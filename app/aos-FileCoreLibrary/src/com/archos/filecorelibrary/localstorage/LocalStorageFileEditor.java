// Copyright 2017 Archos SA
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.archos.filecorelibrary.localstorage;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.provider.DocumentFile;

import com.archos.filecorelibrary.ExtStorageManager;
import com.archos.filecorelibrary.FileEditor;
import com.archos.filecorelibrary.MetaFile2;
import com.archos.environment.ArchosUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.Channels;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;


public class LocalStorageFileEditor extends FileEditor {
    private static Constructor<?> hackyContructor;

    private final Context mContext;

    private static final String PICTURES_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath();
    private static final String USBHOST_PTP_PATH = ExtStorageManager.getExternalStorageUsbHostPtpDirectory().getPath();
    private static final String VIDEO_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getPath();
    private static final String MUSIC_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath();
    private static final String DCIM_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath();
    private static final File STORAGE = Environment.getExternalStorageDirectory();

    /**
     * These directories cannot be deleted, renamed, moved, ...
     */
    public static final String[] DO_NOT_TOUCH = {
            DCIM_PATH, (new File(STORAGE, "Android")).getPath(), MUSIC_PATH, PICTURES_PATH, VIDEO_PATH, USBHOST_PTP_PATH
    };

    public LocalStorageFileEditor(Uri uri, Context context) {
        super(uri);
        if (LocalStorageFileEditor.hackyContructor == null) {

            try {
                Class<?> c = Class.forName("android.support.v4.provider.TreeDocumentFile");
                LocalStorageFileEditor.hackyContructor = c.getDeclaredConstructor(DocumentFile.class, Context.class, Uri.class);
                LocalStorageFileEditor.hackyContructor.setAccessible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }


        }

        mContext = context;
    }

    public InputStream getInputStream() throws FileNotFoundException {
        return new FileInputStream(new File(mUri.getPath()));
    }

    // Our FileEditor API throw an exception when the delete fails.
    // Since the java.File API does not do it (returns false instead) we're using this home-made exception instead
    public static class DeleteFailException extends Exception {
    }

    public static boolean checkIfShouldNotTouchFolder(Uri uri) {
        String path = uri.getPath();
        if (path == null) {
            return false;
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return Arrays.asList(DO_NOT_TOUCH).contains(path);
    }

    @Override
    public InputStream getInputStream(long from) throws Exception {
        RandomAccessFile raf = new RandomAccessFile(new File(mUri.getPath()), "r");
        if (raf != null) {
            raf.seek(from);
            return Channels.newInputStream(raf.getChannel());
        }
        return null;
    }

    public OutputStream getOutputStream() throws IOException {
        OutputStream fos = null;
        try {
            List<UriPermission> permissions;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                permissions = mContext.getContentResolver().getPersistedUriPermissions();
                UriPermission sdPermission = permissions.get(0);
                Uri sdUri = sdPermission.getUri();
                DocumentFile sdDcoumentFile = DocumentFile.fromTreeUri(mContext, sdUri);
                Stack<DocumentFile> stack = new Stack<>();
                String[] parts = mUri.toString().split("\\/");
                DocumentFile documentFile = sdDcoumentFile;
                stack.push(sdDcoumentFile);
                for (int i = 5; i < parts.length; i++) {
                    String part = parts[i];
                    documentFile = getSubDocFile(documentFile, part);
                    stack.push(documentFile);
                }
                DocumentFile lastDoc = stack.peek();
                fos = mContext.getContentResolver().openOutputStream(lastDoc.getUri(), "w");
                fos = new FileOutputStream(new File(lastDoc.getUri().getPath()));
            } else
                fos = new FileOutputStream(new File(mUri.getPath()));
        } catch (FileNotFoundException e) {
            // when permission error, try externalsdfilewriter
            if (mContext != null) {
                ExternalSDFileWriter external = new ExternalSDFileWriter(mContext.getContentResolver(), new File(mUri.getPath()));
                fos = external.write();
            } else {
                throw e;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return fos;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private DocumentFile getSubDocFile(DocumentFile documentFile, String name) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        final Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(documentFile.getUri(),
                DocumentsContract.getDocumentId(documentFile.getUri()));
        Cursor cursor = mContext.getContentResolver().query(childrenUri, new String[]{
                DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String docId = cursor.getString(0);
                String docName = cursor.getString(1);
                if (docName.equals(name)) {
                    final Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(documentFile.getUri(),
                            docId);
                    DocumentFile res = (DocumentFile) LocalStorageFileEditor.hackyContructor.newInstance(null, mContext, documentUri);
                    return res;
                }
            }
            cursor.close();
        }
        return null;
    }

    @Override
    public boolean touchFile() {
        try {
            boolean create = new File(mUri.getPath()).createNewFile();
            if (create && mContext != null) {
                scanFile(mUri, false);
            }
            return create;
        } catch (IOException e) {
            return false;
        }
    }

    private void scanFile(Uri toIndex, boolean isAddingDirectory) {
        if (isAddingDirectory) {
            ExternalSDFileWriter external = new ExternalSDFileWriter(mContext.getContentResolver(), new File(toIndex.getPath()));
            external.addFolderToDB();
            return;
        }
        if (toIndex.getScheme() == null) {
            toIndex = Uri.parse("file://" + toIndex.toString());
        }
        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scanIntent.setPackage(ArchosUtils.getGlobalContext().getPackageName());
        scanIntent.setData(toIndex);
        mContext.sendBroadcast(scanIntent);
    }

    @Override
    public boolean mkdir() {
        if (!new File(mUri.getPath()).mkdir()) {
            //when permission error, try externalsdfilewriter
            if (mContext != null) {
                ExternalSDFileWriter external = new ExternalSDFileWriter(mContext.getContentResolver(), new File(mUri.getPath()));
                return external.mkdir();
            }
        } else if (mContext != null) {
            scanFile(mUri, true);
            return true;
        }
        return false;
    }

    @Override
    public void delete() throws Exception {
        if (checkIfShouldNotTouchFolder(mUri)) {
            // when some folders are protected
            throw new DeleteFailException();
        }
        File fileToDelete = new File(mUri.getPath());
        if (fileToDelete.isDirectory()) {
            deleteFolder(mUri);
        } else {
            deleteFile(fileToDelete);
        }
    }

    private void deleteFromDatabase(File file) {
        if (mContext != null) {
            Uri uri = MediaStore.Files.getContentUri("external");
            String where = MediaStore.MediaColumns.DATA + "=?";
            String[] selectionArgs = {file.getAbsolutePath()};
            mContext.getContentResolver().delete(uri, where, selectionArgs);
        }
    }

    private void deleteFile(File file) throws DeleteFailException {
        if (!file.delete()) {
            if (mContext != null) {
                ExternalSDFileWriter external = new ExternalSDFileWriter(mContext.getContentResolver(), file);
                try {
                    if (!external.delete()) {
                        throw new DeleteFailException();
                    }
                    return;
                } catch (IOException e1) {
                }
            } else {
                throw new DeleteFailException();
            }
        } else {
            deleteFromDatabase(file);
        }
    }

    private void deleteFolder(Uri uri) throws Exception {
        LocalStorageRawLister lsrl = new LocalStorageRawLister(uri);
        List<MetaFile2> children = lsrl.getFileList();
        if (children != null) {
            for (MetaFile2 child : children) {
                child.getFileEditorInstance(mContext).delete();
            }
        }
        deleteFile(new File(uri.getPath()));
    }

    /**
     * @param context
     * @throws DeleteFailException if database AND file delete fails
     */
    public void deleteFileAndDatabase(Context context) throws DeleteFailException {
        if (checkIfShouldNotTouchFolder(mUri)) {
            // when some folders are protected
            throw new DeleteFailException();
        }
        ContentResolver cr = context.getContentResolver();
        Uri uri = MediaStore.Files.getContentUri("external");
        String where = MediaStore.MediaColumns.DATA + "=?";
        String[] selectionArgs = {mUri.getPath()};
        cr.delete(uri, where, selectionArgs);
        boolean delete = false;
        try {
            delete();
            delete = true;
        } catch (Exception e) {
        }

        if (!(delete || !exists())) {
            // in case delete fail because file has already been deleted by cr.delete
            throw new DeleteFailException();
        }
    }

    @Override
    public boolean rename(String newName) {
        if (checkIfShouldNotTouchFolder(mUri)) {
            // when some folders are protected
            return false;
        }
        File newFile = new File(new File(mUri.getPath()).getParentFile(), newName);
        boolean success = new File(mUri.getPath()).renameTo(newFile);
        if (success) {
            deleteFromDatabase(new File(mUri.getPath()));
            scanFile(Uri.fromFile(newFile), newFile.isDirectory());
        }
        return success;
    }

    @Override
    public boolean move(Uri uri) {
        if (checkIfShouldNotTouchFolder(mUri)) {
            // when some folders are protected
            return false;
        }
        if (uri.getScheme().equals(mUri.getScheme())) {
            return new File(mUri.getPath()).renameTo(new File(uri.getPath()));
        }
        return false;
    }

    @Override
    public boolean exists() {
        String path = mUri.getPath();
        return path != null && new File(path).exists();
    }

}
