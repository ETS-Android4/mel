package de.mel.auth.tools;

import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import de.mel.Lok;
import de.mel.core.serialize.exceptions.JsonSerializationException;
import de.mel.sql.RWLock;

/**
 * Created by xor on 2/26/16.
 */
public class Timor {
    private RWLock lock = new RWLock();
    private Timer timer;

    public Timor() {

    }

    public void start(long milliseconds) {
        timer = new Timer();
        lock.lockWrite();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                lock.unlockWrite();
            }
        }, milliseconds);
    }

    public void waite() {
        lock.lockWrite();
        lock.unlockWrite();
    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static void main(String[] args) throws JsonSerializationException, IllegalAccessException {
        InputStream is = String.class.getResourceAsStream("/de/mel/auth/sql.sql");
        URL url = String.class.getResource("foo.txt");
        String text = new Scanner(String.class.getResourceAsStream("/de/mel/auth/sql.sql"), "UTF-8").useDelimiter("\\A").next();
        String r = convertStreamToString(is);
        Lok.debug(r);
        //.getClassLoader().getResource("de/mel/auth/service/register.fxml"));
    }


}
