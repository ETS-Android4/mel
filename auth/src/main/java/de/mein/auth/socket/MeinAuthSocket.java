package de.mein.auth.socket;

import de.mein.auth.data.MeinRequest;
import de.mein.auth.data.db.Certificate;
import de.mein.auth.jobs.AConnectJob;
import de.mein.auth.service.MeinAuthService;
import de.mein.auth.socket.process.auth.MeinAuthProcess;
import de.mein.auth.socket.process.imprt.MeinCertRetriever;
import de.mein.auth.socket.process.reg.MeinRegisterProcess;
import de.mein.auth.socket.process.transfer.MeinIsolatedProcess;
import de.mein.auth.socket.process.val.MeinValidationProcess;
import de.mein.auth.tools.NoTryRunner;
import de.mein.auth.tools.Hash;
import de.mein.core.serialize.SerializableEntity;
import de.mein.core.serialize.deserialize.entity.SerializableEntityDeserializer;
import de.mein.core.serialize.exceptions.JsonSerializationException;
import de.mein.sql.SqlQueriesException;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URISyntaxException;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Created by xor on 10.08.2016.
 */
@SuppressWarnings("Duplicates")
public class MeinAuthSocket extends MeinSocket implements MeinSocket.MeinSocketListener {
    private static Logger logger = Logger.getLogger(MeinAuthSocket.class.getName());
    protected MeinProcess process;
    protected Certificate partnerCertificate;

    public MeinAuthSocket(MeinAuthService meinAuthService) {
        super(meinAuthService);
        setListener(this);
    }

    public Certificate getPartnerCertificate() {
        return partnerCertificate;
    }

    MeinAuthSocket setProcess(MeinProcess process) {
        this.process = process;
        return this;
    }

    public String getAddressString() {
        return MeinAuthSocket.getAddressString(socket.getInetAddress(), socket.getPort());
    }

    public static String getAddressString(InetAddress address, int port) {
        return address.getHostAddress() + ":" + port;
    }

    public MeinAuthSocket(MeinAuthService meinAuthService, Socket socket) {
        super(meinAuthService, socket);
        setListener(this);
    }

    public MeinAuthSocket allowIsolation() {
        this.allowIsolation = true;
        return this;
    }

    public InetAddress getAddress() {
        return socket.getInetAddress();
    }

    @Override
    public void onIsolated() {
        ((MeinIsolatedProcess) process).onIsolated();
    }

    @Override
    public void onMessage(MeinSocket meinSocket, String msg) {
        try {
            System.out.println(meinAuthService.getName() + ".got: " + msg);
            SerializableEntityDeserializer deserializer = new SerializableEntityDeserializer();
            SerializableEntity deserialized = SerializableEntityDeserializer.deserialize(msg);
            if (process != null) {
                process.onMessageReceived(deserialized, this);
            } else if (deserialized instanceof MeinRequest) {
                MeinRequest request = (MeinRequest) deserialized;
                if (request.getServiceUuid().equals(MeinAuthService.SERVICE_NAME) &&
                        request.getIntent().equals(MeinAuthService.INTENT_REGISTER)) {
                    MeinRegisterProcess meinRegisterProcess = new MeinRegisterProcess(this);
                    process = meinRegisterProcess;
                    meinRegisterProcess.onMessageReceived(deserialized, this);
                } else if (request.getServiceUuid().equals(MeinAuthService.SERVICE_NAME) &&
                        request.getIntent().equals(MeinAuthService.INTENT_AUTH)) {
                    MeinAuthProcess meinAuthProcess = new MeinAuthProcess(this);
                    process = meinAuthProcess;
                    meinAuthProcess.onMessageReceived(deserialized, this);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onOpen() {

    }

    @Override
    public void onError(Exception ex) {

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println(meinAuthService.getName() + "." + getClass().getSimpleName() + ".onClose");
    }

    @Override
    public void onBlockReceived(byte[] block) {
        // this shall only work with isolated processes
        ((MeinIsolatedProcess) process).onBlockReceived(block);
    }

    /**
     * from MeinAuthService
     */

    public Promise<MeinValidationProcess, Exception, Void> connect(AConnectJob job) throws URISyntaxException, InterruptedException, UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException, InvalidKeyException, IllegalAccessException, NoSuchPaddingException, BadPaddingException, SqlQueriesException, IllegalBlockSizeException, ClassNotFoundException, JsonSerializationException {
        final Long remoteCertId = job.getCertificateId();
        final String address = job.getAddress();
        final Integer port = job.getPort();
        final Integer portCert = job.getPortCert();
        final boolean regOnUnknown = job.getRegOnUnknown();

        System.out.println("MeinAuthSocket.connect(id=" + remoteCertId + " addr=" + address + " port=" + port + " portCert=" + portCert + " reg=" + regOnUnknown + ")");
        DeferredObject result = job.getPromise();
        NoTryRunner runner = new NoTryRunner(e -> {
            result.reject(e);
        });
        DeferredObject<Void, Exception, Void> firstAuth = this.auth(job);
        firstAuth.done(result1 -> {
            result.resolve(result1);
        }).fail(except -> runner.runTry(() -> {
            if (regOnUnknown) {
                if (except instanceof ShamefulSelfConnectException) {
                    result.reject(except);
                } else if (remoteCertId == null) {
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
                                    this.auth(job);
                                });

                            }).fail(exception -> {
                                        // it won't compile otherwise. don't know why.
                                        // compiler thinks exception is an Object instead of Exception
                                        ((Exception) exception).printStackTrace();
                                        result.reject(exception);
                                    }
                            );
                        });
                    }).fail(ee -> {
                        ee.printStackTrace();
                        result.reject(ee);
                    });
                }
            } else {
                if (!(except instanceof ShamefulSelfConnectException)) {
                    result.reject(new CannotConnectException(except, address, port));
                } else {
                    result.reject(except);
                }
            }
        }));

        /*else {
            System.out.println("MeinAuthSocket.connect.NOT.IMPLEMENTED.YET");
            this.auth(result, remoteCertId, address, port, portCert);
        }*/
        return result;
    }

    public void importCertificate(DeferredObject<de.mein.auth.data.db.Certificate, Exception, Object> deferred, String address, int port, int portCert) throws URISyntaxException, InterruptedException {
        MeinCertRetriever retriever = new MeinCertRetriever(meinAuthService);
        retriever.retrieveCertificate(deferred, address, port, portCert);
    }

    private Promise<Certificate, Exception, Void> register(DeferredObject<Certificate, Exception, Void> result, Certificate certificate, String address, Integer port) throws IllegalAccessException, SqlQueriesException, URISyntaxException, InvalidKeyException, NoSuchAlgorithmException, JsonSerializationException, CertificateException, KeyStoreException, ClassNotFoundException, KeyManagementException, BadPaddingException, UnrecoverableKeyException, NoSuchPaddingException, IOException, IllegalBlockSizeException, InterruptedException {
        MeinRegisterProcess meinRegisterProcess = new MeinRegisterProcess(this);
        return meinRegisterProcess.register(result, certificate.getId().v(), address, port);
    }

   /* private Promise<Integer, Exception, Void> auth(Certificate certificate, String wss, String ws, int sslPort, int listenerPort) throws NoSuchPaddingException, IllegalAccessException, SqlQueriesException, URISyntaxException, InvalidKeyException, NoSuchAlgorithmException, KeyManagementException, CertificateException, KeyStoreException, ClassNotFoundException, BadPaddingException, UnrecoverableKeyException, JsonSerializationException, IOException, IllegalBlockSizeException, InterruptedException {
        MeinAuthProcess meinAuthProcess = new MeinAuthProcess(meinAuthService);
        return meinAuthProcess.authenticate(certificate.getId().v(), wss);
    }*/

    private DeferredObject<Void, Exception, Void> auth(AConnectJob job) {
        final Long remoteCertId = job.getCertificateId();
        final String address = job.getAddress();
        final Integer port = job.getPort();
        final Integer portCert = job.getPortCert();
        DeferredObject<Void, Exception, Void> deferred = new DeferredObject<>();

        NoTryRunner runner = new NoTryRunner(e -> {
            e.printStackTrace();
            deferred.reject(e);
        });
        runner.runTry(() -> {
            MeinAuthProcess meinAuthProcess = new MeinAuthProcess(this);
            Promise<Void, Exception, Void> authPromise = meinAuthProcess.authenticate(job);
            authPromise.fail(ex -> {
                ex.printStackTrace();
                deferred.reject(ex);
            });
        });
        return deferred;
    }

    public Promise<Void, Exception, Void> connectSSL(String address, Integer port) throws InterruptedException, UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, IOException, URISyntaxException {
        DeferredObject<Void, Exception, Void> deferredObject = new DeferredObject<>();
        Socket socket = meinAuthService.getCertificateManager().createSocket();
        MeinSocket meinSocket = new MeinSocket(meinAuthService);
        meinSocket.setSocket(socket).setAddress(address);
        return deferredObject;
    }

    public void connectSSL(Long certId, String address, int port) throws SqlQueriesException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        if (certId != null)
            partnerCertificate = meinAuthService.getCertificateManager().getTrustedCertificateById(certId);
        Socket socket = meinAuthService.getCertificateManager().createSocket();
        socket.connect(new InetSocketAddress(address, port));
        //stop();
        setSocket(socket);
        start();
    }

    public Certificate getTrustedPartnerCertificate() throws IOException, CertificateEncodingException, SqlQueriesException, ShamefulSelfConnectException {
        if (partnerCertificate == null) {
            SSLSocket sslSocket = (SSLSocket) socket;
            java.security.cert.Certificate cert = sslSocket.getSession().getPeerCertificates()[0];
            byte[] certBytes = cert.getEncoded();
            String hash = Hash.sha256(certBytes);
            partnerCertificate = meinAuthService.getCertificateManager().getTrustedCertificateByHash(hash);
            if (partnerCertificate == null) {
                if (Arrays.equals(meinAuthService.getCertificateManager().getPublicKey().getEncoded(), cert.getPublicKey().getEncoded())) {
                    throw new ShamefulSelfConnectException();
                }
            }
        }
        return partnerCertificate;
    }


    public void sendBlock(byte[] block) throws IOException {
        assert block.length == MeinSocket.BLOCK_SIZE;
        out.write(block);
        out.flush();
    }

    public void disconnect() throws IOException {
        socket.close();
    }

    public boolean isValidated() {
        return (process != null && process instanceof MeinValidationProcess);
    }

    @Override
    protected void onSocketClosed(Exception e) {
        meinAuthService.onSocketClosed(this);
    }

    public MeinProcess getProcess() {
        return process;
    }
}
