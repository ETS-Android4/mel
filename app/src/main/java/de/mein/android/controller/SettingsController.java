package de.mein.android.controller;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import de.mein.R;
import de.mein.android.MeinActivity;
import de.mein.android.service.AndroidService;
import de.mein.auth.data.MeinAuthSettings;
import de.mein.auth.service.power.PowerManager;

/**
 * Created by xor on 9/19/17.
 */

public class SettingsController extends GuiController {
    private PowerManager powerManager;
    private Button btnStartStop, btnApply;
    private EditText txtPort, txtCertPort, txtName;


    public SettingsController(MeinActivity activity, LinearLayout content) {
        super(activity, content, R.layout.content_settings);
        txtCertPort = rootView.findViewById(R.id.txtCertPort);
        txtName = rootView.findViewById(R.id.txtName);
        txtPort = rootView.findViewById(R.id.txtPort);
        // action listeners
        btnStartStop = rootView.findViewById(R.id.btnStart);
        btnApply = rootView.findViewById(R.id.btnApply);
        btnStartStop.setOnClickListener(v1 -> {
            //service
            Intent serviceIntent = new Intent(rootView.getContext(), AndroidService.class);
            ComponentName name = rootView.getContext().startService(serviceIntent);
            System.out.println("InfoController.InfoController.service.started: " + name.getClassName());
        });
        btnApply.setOnClickListener(v1 -> applyInputs());
    }

    private void showAll() {
        // fill values
        if (androidService != null) {
            activity.runOnUiThread(() -> {
                MeinAuthSettings meinAuthSettings = androidService.getMeinAuthSettings();
                txtPort.setText(meinAuthSettings.getPort().toString());
                txtName.setText(meinAuthSettings.getName());
                txtCertPort.setText(meinAuthSettings.getDeliveryPort().toString());
            });
        }
    }

    private void applyInputs() {
        try {
            int port = Integer.parseInt(txtPort.getText().toString());
            int certPort = Integer.parseInt(txtCertPort.getText().toString());
            String name = txtName.getText().toString();
            if (name.trim().isEmpty())
                throw new Exception("No name entered");
            MeinAuthSettings meinAuthSettings = androidService.getMeinAuthSettings();
            meinAuthSettings.setName(name).setDeliveryPort(certPort).setPort(port);
            meinAuthSettings.save();
        } catch (Exception e) {
            Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public String getTitle() {
        return "Settings";
    }

    @Override
    public void onAndroidServiceAvailable() {
        this.powerManager = androidService.getMeinAuthService().getPowerManager();
        CheckBox cbWorkWhenPlugged = rootView.findViewById(R.id.cbWorkWhenPlugged);
        cbWorkWhenPlugged.setChecked(powerManager.getHeavyWorkWhenPlugged());
        cbWorkWhenPlugged.setOnCheckedChangeListener((compoundButton, isChecked) -> powerManager.setHeavyWorkWhenPlugged(isChecked));
        CheckBox cbWorkWhenOffline = rootView.findViewById(R.id.cbWorkWhenOffline);
        cbWorkWhenOffline.setOnCheckedChangeListener((compoundButton, isChecked) -> powerManager.setHeavyWorkWhenOffline(isChecked));
        cbWorkWhenOffline.setChecked(powerManager.getHeavyWorkWhenOffline());
        showAll();
    }

    @Override
    public void onAndroidServiceUnbound(AndroidService androidService) {

    }

    @Override
    public void onDestroy() {
        powerManager = null;
    }
}