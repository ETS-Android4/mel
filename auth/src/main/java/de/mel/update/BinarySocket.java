package de.mel.update;

import de.mel.Lok;
import de.mel.auth.MelStrings;
import de.mel.auth.file.AFile;
import de.mel.auth.socket.MelSocket;
import de.mel.sql.Hash;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BinarySocket extends SimpleSocket {
    private final VersionAnswer.VersionEntry versionEntry;
    private final File target;
    private final Updater updater;

    public BinarySocket(Updater updater, Socket socket, VersionAnswer.VersionEntry versionEntry, File target) throws IOException {
        super(socket);
        this.versionEntry = versionEntry;
        this.target =target;
        this.updater = updater;
    }

    @Override
    public void runImpl() {
        String question = MelStrings.update.QUERY_FILE + versionEntry.getHash();
        try {
            FileOutputStream fos =new FileOutputStream(target);
            MessageDigest messageDigest = Hash.createSHA256MessageDigest();
            out.writeUTF(question);
            byte[] bytes;
            bytes = new byte[MelSocket.BLOCK_SIZE];
            int read;
            Long sum = 0L;
            updater.onSocketProgress(sum,versionEntry.getLength());
            do {
                read = in.read(bytes);
                if (read > 0) {
                    fos.write(bytes, 0, read);
                    messageDigest.update(bytes, 0, read);
                    sum+=read;
                    updater.onSocketProgress(sum,versionEntry.getLength());
                }
            } while (read > 0);
            String receivedHash = Hash.bytesToString(messageDigest.digest());
            if (receivedHash.equals(versionEntry.getHash()))
                updater.onUpdateReceived(versionEntry, target);
            else
                Lok.error("expected hash: " + versionEntry.getHash() + ", received: " + receivedHash);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}