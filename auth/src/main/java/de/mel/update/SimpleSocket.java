package de.mel.update;

import de.mel.DeferredRunnable;
import de.mel.auth.tools.N;
import org.jdeferred.Promise;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public abstract class SimpleSocket extends DeferredRunnable {
    protected final Socket socket;
    protected final DataOutputStream out;
    protected final DataInputStream in;

    public SimpleSocket(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        runImpl();
        onShutDown();
    }


    @Override
    public Promise<Void, Void, Void> onShutDown() {
        N.s(socket::close);
        N.s(in::close);
        N.s(out::close);
        return DeferredRunnable.ResolvedDeferredObject();
    }

    @Override
    public String getRunnableName() {
        return getClass().getSimpleName();
    }

}
