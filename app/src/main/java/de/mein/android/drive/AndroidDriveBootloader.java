package de.mein.android.drive;

import android.Manifest;
import android.app.Activity;
import android.view.ViewGroup;

import org.jdeferred.Promise;

import de.mein.R;
import de.mein.android.MeinActivity;
import de.mein.android.controller.AndroidServiceCreatorGuiController;
import de.mein.android.Threadder;
import de.mein.android.drive.controller.RemoteDriveServiceChooserGuiController;
import de.mein.android.drive.controller.AndroidDriveEditGuiController;
import de.mein.auth.service.IMeinService;
import de.mein.auth.service.MeinAuthService;
import de.mein.android.boot.AndroidBootLoader;
import de.mein.drive.DriveBootLoader;
import de.mein.drive.DriveCreateController;
import de.mein.drive.service.MeinDriveClientService;

/**
 * Created by xor on 2/25/17.
 */

public class AndroidDriveBootloader extends DriveBootLoader implements AndroidBootLoader {
    private static final int PERMISSION_WRITE = 666;

    @Override
    public void createService(Activity activity, MeinAuthService meinAuthService, AndroidServiceCreatorGuiController currentController) {
        RemoteDriveServiceChooserGuiController driveCreateGuiController = (RemoteDriveServiceChooserGuiController) currentController;

        // create the actual MeinDrive service
        DriveCreateController driveCreateController = new DriveCreateController(meinAuthService);
        if (driveCreateGuiController.isValid())
            Threadder.runNoTryThread(() -> {
                String name = driveCreateGuiController.getName();
                String path = driveCreateGuiController.getPath();
                if (driveCreateGuiController.isServer()) {
                    driveCreateController.createDriveServerService(name, path);
                } else {
                    Long certId = driveCreateGuiController.getSelectedCertId();
                    String serviceUuid = driveCreateGuiController.getSelectedService().getUuid().v();
                    Promise<MeinDriveClientService, Exception, Void> promise = driveCreateController.createDriveClientService(name, path, certId, serviceUuid);
                    //promise.done(meinDriveClientService -> N.r(() -> meinDriveClientService.syncThisClient()));
                }
            });
    }

    @Override
    public AndroidServiceCreatorGuiController createGuiController(MeinAuthService meinAuthService, MeinActivity activity, ViewGroup rootView, IMeinService runningInstance) {
        // check for permission if necessary
        activity.annoyWithPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return new RemoteDriveServiceChooserGuiController(meinAuthService, activity, rootView);
    }

    @Override
    public AndroidServiceCreatorGuiController inflateEmbeddedView(ViewGroup embedded, MeinActivity activity, MeinAuthService meinAuthService, IMeinService runningInstance) {
        activity.annoyWithPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (runningInstance == null) {
            return new RemoteDriveServiceChooserGuiController(meinAuthService, activity, embedded);
        } else {
            return new AndroidDriveEditGuiController(meinAuthService, activity, runningInstance, embedded);
        }

    }

    @Override
    public int getMenuIcon() {
        return R.drawable.icon_folder;
    }

}
