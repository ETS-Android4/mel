package de.mein.android.controller;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;


import java.util.ArrayList;
import java.util.List;

import de.mein.R;
import de.mein.auth.service.BootLoader;
import de.mein.auth.service.MeinAuthService;
import de.mein.android.boot.AndroidBootLoader;
import de.mein.android.service.AndroidService;
import de.mein.auth.tools.N;
import de.mein.android.MainActivity;

/**
 * Created by xor on 2/20/17.
 */
public class CreateServiceController extends GuiController {
    private final Spinner spinner;
    private LinearLayout embedded;
    private Button btnCreate;
    private AndroidBootLoader bootLoader;
    private AndroidServiceCreatorGuiController currentController;
    private MainActivity mainActivity;

    public CreateServiceController(MainActivity activity, LinearLayout content ) {
        super(activity, content,R.layout.content_create_service);
        this.mainActivity = activity;
        this.spinner = rootView.findViewById(R.id.spin_bootloaders);
        this.embedded = rootView.findViewById(R.id.embedded);
        this.btnCreate = rootView.findViewById(R.id.btnCreate);

    }

    private void showSelected() {
        N.r(() -> {
            bootLoader = (AndroidBootLoader) spinner.getSelectedItem();
            currentController = bootLoader.inflateEmbeddedView(embedded, activity, androidService.getMeinAuthService(), null);
            System.out.println("CreateServiceController.showSelected");
        });

    }


    @Override
    public String getTitle() {
        return "Create Service";
    }

    @Override
    public void onAndroidServiceAvailable() {
        activity.runOnUiThread(() -> {
            MeinAuthService meinAuthService = androidService.getMeinAuthService();
            List<BootLoader> bootLoaders = new ArrayList<>();
            for (Class<? extends BootLoader> bootloaderClass : meinAuthService.getMeinBoot().getBootloaderClasses()){
                N.r(() -> {
                    BootLoader bootLoader = bootloaderClass.newInstance();
                    bootLoaders.add(bootLoader);
                    //MeinDriveServerService serverService = new DriveCreateController(meinAuthService).createDriveServerService("server service", testdir1.getAbsolutePath());
                    System.out.println("CreateServiceController.CreateServiceController");
                });
            }

            // Create an ArrayAdapter using the string array and a default spinner layout
            ArrayAdapter<BootLoader> adapter = new ArrayAdapter<>(rootView.getContext(), R.layout.support_simple_spinner_dropdown_item, bootLoaders);
            // Specify the layout to use when the list of choices appears
            // Apply the adapter to the spinner
            spinner.setAdapter(adapter);
            showSelected();
            btnCreate.setOnClickListener(view -> {
                if (bootLoader != null) {
                    bootLoader.createService(activity, meinAuthService, currentController);
                    mainActivity.showMenuServices();
                }
            });
        });
    }

    @Override
    public void onAndroidServiceUnbound(AndroidService androidService) {

    }
}
