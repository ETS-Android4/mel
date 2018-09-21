package de.mein.auth.socket;

import de.mein.DeferredRunnable;
import de.mein.Lok;
import de.mein.WaitingDeferredRunnable;
import de.mein.auth.service.MeinAuthService;
import de.mein.auth.tools.N;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by xor on 04.09.2016.
 */
public class MeinAuthSocketOpener extends DeferredRunnable {

    private ServerSocket serverSocket;
    private final MeinAuthService meinAuthService;
    private final int port;

    public MeinAuthSocketOpener(MeinAuthService meinAuthService, int port) {
        this.meinAuthService = meinAuthService;
        this.port = port;
    }

    @Override
    public void onShutDown() {
        N.r(() -> serverSocket.close());
    }

    @Override
    public void runImpl() {
        try {
            serverSocket = meinAuthService.getCertificateManager().createServerSocket();
            serverSocket.bind(new InetSocketAddress(port));
            startedPromise.resolve(this);
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = this.serverSocket.accept();
                MeinSocket meinSocket = new MeinAuthSocket(meinAuthService, socket);
                meinSocket.start();

            }
        } catch (Exception e) {
            if (!isInterrupted()) {
                Lok.error("MeinAuthSocketOpener.runTry.FAAAAAIL!");
                e.printStackTrace();
            }
        } finally {
            try {
                serverSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        Lok.debug("MeinAuthService.runTry.end");
    }

    @Override
    public String getRunnableName() {
        return getClass().getSimpleName() + " for " + meinAuthService.getName();
    }

}
