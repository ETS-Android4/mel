package de.miniserver.socket;

import de.mein.DeferredRunnable;
import de.mein.MeinRunnable;
import de.mein.auth.tools.N;
import de.miniserver.MiniServer;
import de.miniserver.data.FileRepository;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class BinarySocketOpener extends DeferredRunnable {
    private final int port;
    private final MiniServer miniServer;
    private final FileRepository fileRepository;
    private ServerSocket serverSocket;

    public BinarySocketOpener(int port, MiniServer miniServer, FileRepository fileRepository) {
        this.port = port;
        this.miniServer = miniServer;
        this.fileRepository = fileRepository;
    }


    @Override
    public String getRunnableName() {
        return getClass().getSimpleName();
    }

    @Override
    public void onShutDown() {
        N.s(() -> serverSocket.close());
    }



    @Override
    public void runImpl() {
        N.r(() -> {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(port));
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                SendBinarySocket sendBinarySocket = new SendBinarySocket(socket, fileRepository);
                miniServer.execute(sendBinarySocket);
            }
        });
        onShutDown();
    }
}
