package de.mel.android.file;

import android.content.Context;
import android.database.Cursor;
import android.provider.DocumentsContract;
import androidx.documentfile.provider.DocumentFile;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import de.mel.auth.file.AbstractFile;

/**
 * maybe faster than the other find method. needs some benchmarking. if the other way is fast enough this can be removed.
 * does not work yet. probably never will.
 */
@Deprecated
public class DFileRecursiveIterator implements Iterator<AbstractFile> {

    private final AbstractFile pruneDir;
    private Cursor cursor;
    private final Context context;
    private final AbstractFile currentDir;
    private Queue<AbstractFile> currentSubDirs = new LinkedList<>();
    private DFileRecursiveIterator subIterator;

    public DFileRecursiveIterator(Context context, AbstractFile rootDirectory, AbstractFile pruneDir)  {
        this.context = context;
        this.pruneDir = pruneDir;
        this.currentDir = rootDirectory;
        DocumentFile rootDoc = null;
        try {
            rootDoc = new AndroidFile(rootDirectory.getAbsolutePath()).createDocFile();
        } catch (SAFAccessor.SAFException e) {
            e.printStackTrace();
        }
        String rootId = DocumentsContract.getDocumentId(rootDoc.getUri());
//        DocumentsContract.build(rootDoc.getUri(),rootId);
//        Uri childrenUri = rootDirectory.buildChildrenUri();
//        this.cursor = context.getContentResolver().query(childrenUri, new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_DOCUMENT_ID}, null, null, null);

    }



    @Override
    public boolean hasNext() {
        boolean next = !cursor.isAfterLast() || (subIterator != null && subIterator.hasNext());
        if (!next)
            cursor.close();
        return next;
    }

    @Override
    public AbstractFile next() {
        if (hasNext()) {
            if (cursor.isAfterLast()) {
                cursor.close();
                if (subIterator != null)
                    return subIterator.next();
                while (!currentSubDirs.isEmpty()) {
                    AbstractFile subDir = currentSubDirs.poll();
                    subIterator = new DFileRecursiveIterator(context, currentDir, null);
                    if (subIterator.hasNext())
                        return subIterator.next();

                }
            } else {
                cursor.moveToNext();
                String name = cursor.getString(0);
                String mime = cursor.getString(1);
                String id = cursor.getString(2);
                AbstractFile dFile = AbstractFile.instance(currentDir, name);
                if (dFile.isDirectory()) {
                    currentSubDirs.add(dFile);
                }
                return dFile;
            }
        }
        return null;
    }
}
