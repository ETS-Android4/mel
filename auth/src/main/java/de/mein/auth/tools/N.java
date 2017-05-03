package de.mein.auth.tools;

import de.mein.auth.socket.ShamefulSelfConnectException;

import java.util.ArrayList;
import java.util.List;

/**
 * Syntactic sugar. Saves you lots of try/catches. calls e.stacktrace() per default.
 */
public class N {
    public interface INoTryRunnable {
        void run() throws Exception, ShamefulSelfConnectException;
    }

    public interface NoTryExceptionConsumer {
        void accept(Exception e);
    }


    private static N runner = new N(new NoTryExceptionConsumer() {
        @Override
        public void accept(Exception e) {
            e.printStackTrace();
        }
    });

    public static void r(N.INoTryRunnable noTryRunnable) {
        N.runner.runTry(noTryRunnable);
    }

    private NoTryExceptionConsumer consumer;

    public N(NoTryExceptionConsumer consumer) {
        this.consumer = consumer;
    }

    public NoTryExceptionConsumer getConsumer() {
        return consumer;
    }

    public N runTry(INoTryRunnable noTryRunnable) {
        try {
            noTryRunnable.run();
        } catch (Exception e) {
            consumer.accept(e);
        }
        return this;
    }

    public N runTry(INoTryRunnable noTryRunnable, NoTryExceptionConsumer consumer) {
        try {
            noTryRunnable.run();
        } catch (Exception e) {
            consumer.accept(e);
        }
        return this;
    }

    public static void main(String[] args) {
        N runner = new N(e -> System.out.println("NoTryRunner.main." + e.getMessage()));
        runner.runTry(() -> {
            List<String> list = (ArrayList) ((Object) 12);
            System.out.println(list);
        });
        System.out.println("NoTryRunner.main.end");
    }
}
