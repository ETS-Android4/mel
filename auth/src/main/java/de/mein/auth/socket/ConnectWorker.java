package de.mein.auth.socket;

import de.mein.Lok;
import de.mein.MeinRunnable;
import de.mein.auth.data.db.Certificate;
import de.mein.auth.jobs.AConnectJob;
import de.mein.auth.jobs.ConnectJob;
import de.mein.auth.jobs.Job;
import de.mein.auth.service.MeinAuthService;
import de.mein.auth.service.MeinWorker;
import de.mein.auth.socket.process.imprt.MeinCertRetriever;
import de.mein.auth.tools.CountWaitLock;
import de.mein.auth.tools.CountdownLock;
import de.mein.auth.tools.N;
import de.mein.core.serialize.exceptions.JsonSerializationException;
import de.mein.sql.SqlQueriesException;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.SocketFactory;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URISyntaxException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Objects;

/**
 * Connects to other instances on the network.
 */
@SuppressWarnings("Duplicates")
public class ConnectWorker extends MeinWorker {

    private final AConnectJob connectJob;
    private final MeinAuthService meinAuthService;
    private MeinAuthSocket meinAuthSocket;

    public ConnectWorker(MeinAuthService meinAuthService, AConnectJob connectJob) {
        Objects.requireNonNull(connectJob);
        this.meinAuthService = meinAuthService;
        this.connectJob = connectJob;
        addJob(connectJob);
    }


    @Override
    public String getRunnableName() {
        String line = "Connecting to: " + connectJob.getAddress() + ":" + connectJob.getPort() + "/" + connectJob.getPortCert();
        return line;
    }


    private DeferredObject<Void, Exception, Void> auth(AConnectJob job) {
        DeferredObject<Void, Exception, Void> deferred = new DeferredObject<>();
        N runner = new N(e -> {
            e.printStackTrace();
            deferred.reject(e);
        });
        runner.runTry(() -> {
            meinAuthSocket = new MeinAuthSocket(meinAuthService, job);
//            Socket socket = meinAuthService.getCertificateManager().createSocket();
//            socket.connect(new InetSocketAddress(job.getAddress(), job.getPort()));
            MeinAuthProcess meinAuthProcess = new MeinAuthProcess(meinAuthSocket);
            Promise<Void, Exception, Void> authPromise = meinAuthProcess.authenticate(job);
            authPromise.fail(ex -> {
                ex.printStackTrace();
                deferred.reject(ex);
            });
        });
        return deferred;
    }

    /**
     * connects and blocks
     *
     * @param job
     * @return
     */
    Promise<MeinValidationProcess, Exception, Void> connect(AConnectJob job) {
        final CountdownLock lock = new CountdownLock(1);
        final Long remoteCertId = job.getCertificateId();
        final String address = job.getAddress();
        final Integer port = job.getPort();
        final Integer portCert = job.getPortCert();
        final boolean regOnUnknown = N.result(() -> {
            if (job instanceof ConnectJob)
                return ((ConnectJob) job).getRegOnUnknown();
            return false;
        });

        Lok.debug("MeinAuthSocket.connect(id=" + remoteCertId + " addr=" + address + " port=" + port + " portCert=" + portCert + " reg=" + regOnUnknown + ")");
        meinAuthService.getPowerManager().wakeLock(this);
        DeferredObject result = job.getPromise();
        N runner = new N(e -> {
            result.reject(e);
            meinAuthService.getPowerManager().releaseWakeLock(this);
            stop();
            lock.unlock();
        });
        DeferredObject<Void, Exception, Void> firstAuth = this.auth(job);
        firstAuth.done(result1 -> {
            result.resolve(result1);
            meinAuthService.getPowerManager().releaseWakeLock(this);
        }).fail(except -> runner.runTry(() -> {
            if (except instanceof ShamefulSelfConnectException) {
                result.reject(except);
                meinAuthService.getPowerManager().releaseWakeLock(this);
                stop();
            } else if (except instanceof ConnectException) {
                Lok.error(getClass().getSimpleName() + " for " + meinAuthService.getName() + ".connect.HOST:NOT:REACHABLE");
                result.reject(except);
                meinAuthService.getPowerManager().releaseWakeLock(this);
                stop();
            } else if (regOnUnknown && remoteCertId == null) {
                // try to register
                DeferredObject<Certificate, Exception, Object> importPromise = new DeferredObject<>();
                DeferredObject<Certificate, Exception, Void> registered = new DeferredObject<>();
                this.importCertificate(importPromise, address, port, portCert);
                importPromise.done(importedCert -> {
                    runner.runTry(() -> {
                        job.setCertificateId(importedCert.getId().v());
                        this.register(registered, importedCert, address, port);
                        registered.done(registeredCert -> {
                            runner.runTry(() -> {
                                //connection is no more -> need new socket
                                // create a new job that is not allowed to register.
                                ConnectJob secondJob = new ConnectJob(connectJob.getCertificateId(), connectJob.getAddress(), connectJob.getPort(), connectJob.getPortCert(), false);
                                secondJob.getPromise().done(result::resolve).fail(result::reject);
                                this.addJob(secondJob);
                            });

                        }).fail(exception -> {
                                    // it won't compile otherwise. don't know why.
                                    // compiler thinks exception is an Object instead of Exception
                                    ((Exception) exception).printStackTrace();
                                    result.reject(exception);
                                    meinAuthService.getPowerManager().releaseWakeLock(this);
                                    stop();
                                }
                        );
                    });
                }).fail(ee -> {
                    ee.printStackTrace();
                    result.reject(ee);
                    meinAuthService.getPowerManager().releaseWakeLock(this);
                    stop();
                });
            } else {
                if (!(except instanceof ShamefulSelfConnectException)) {
                    result.reject(new CannotConnectException(except, address, port));
                } else {
                    result.reject(except);
                }
                meinAuthService.getPowerManager().releaseWakeLock(this);
                stop();
            }
        }));

        /*else {
            Lok.debug("MeinAuthSocket.connect.NOT.IMPLEMENTED.YET");
            this.auth(result, remoteCertId, address, port, portCert);
        }*/
        return result;
    }

    private void stop() {
        if (meinAuthSocket != null)
            meinAuthSocket.stop();
    }

    public void importCertificate(DeferredObject<de.mein.auth.data.db.Certificate, Exception, Object> deferred, String address, int port, int portCert) throws URISyntaxException, InterruptedException {
        MeinCertRetriever retriever = new MeinCertRetriever(meinAuthService);
        retriever.retrieveCertificate(deferred, address, port, portCert);
    }

    private Promise<Certificate, Exception, Void> register(DeferredObject<Certificate, Exception, Void> result, Certificate certificate, String address, Integer port) throws IllegalAccessException, SqlQueriesException, URISyntaxException, InvalidKeyException, NoSuchAlgorithmException, JsonSerializationException, CertificateException, KeyStoreException, ClassNotFoundException, KeyManagementException, BadPaddingException, UnrecoverableKeyException, NoSuchPaddingException, IOException, IllegalBlockSizeException, InterruptedException {
        meinAuthSocket = new MeinAuthSocket(meinAuthService);
//        Socket socket = meinAuthService.getCertificateManager().createSocket();
//        socket.connect(new InetSocketAddress(address,port));
        MeinRegisterProcess meinRegisterProcess = new MeinRegisterProcess(meinAuthSocket);
        return meinRegisterProcess.register(result, certificate.getId().v(), address, port);
    }

    @Override
    protected void workWork(Job job) throws Exception {
        if (job instanceof ConnectJob) {
            Lok.debug("Connecting to: " + connectJob.getAddress() + ":" + connectJob.getPort() + "/" + connectJob.getPortCert());
//        MeinAuthSocket meinAuthSocket = new MeinAuthSocket(meinAuthService);
//        Socket socket = createSocket();
//        N.oneLine(() -> meinAuthSocket.connect(connectJob));
            connect(connectJob);
        }

    }
}
