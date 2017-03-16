package de.mein.android;

import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TabHost;
import android.widget.TextView;

import java.security.cert.X509Certificate;

import de.mein.auth.data.MeinRequest;
import de.mein.auth.data.access.CertificateManager;
import de.mein.auth.data.db.Certificate;
import de.mein.auth.socket.process.reg.IRegisterHandler;
import de.mein.auth.socket.process.reg.IRegisterHandlerListener;
import de.mein.sql.RWLock;
import mein.de.meindrive.R;

public class CertActivity extends AppCompatActivity {
    private Button btnAccept, btnReject;
    private TextView txtRemote, txtOwn;
    private RegBundle regBundle;
    private TabHost tabHost;
    private RWLock lock = new RWLock();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Incoming Certificate");
        setContentView(R.layout.activity_cert_incoming);
        txtRemote = (TextView) findViewById(R.id.txtRemote);
        txtOwn = (TextView) findViewById(R.id.txtOwn);
        btnAccept = (Button) findViewById(R.id.btnAccept);
        btnReject = (Button) findViewById(R.id.btnReject);
        String regUuid = getIntent().getExtras().getString(AndroidRegHandler.REGBUNDLE_UUID);
        regBundle = AndroidRegHandler.retrieveRegBundle(regUuid);
        regBundle.getAndroidRegHandler().addActivity(regBundle.getRemoteCert(), this);
        showCert(txtRemote, regBundle.getRemoteCert());
        showCert(txtOwn, regBundle.getMyCert());
        btnAccept.setOnClickListener(
                view -> {
                    regBundle.getAndroidRegHandler().onUserAccepted(regBundle);
                    showWaiting();
                }
        );
        btnReject.setOnClickListener(
                view -> Threadder.runNoTryThread(() -> {
                    regBundle.getAndroidRegHandler().onUserRejected(regBundle);
                    showWaiting();
                })
        );
        tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();
        //Tab 1
        TabHost.TabSpec spec = tabHost.newTabSpec("Remote Cert");
        spec.setContent(R.id.tabRemote);
        spec.setIndicator("Remote Indi");
        tabHost.addTab(spec);
        //Tab 2
        spec = tabHost.newTabSpec("Own Cert");
        spec.setContent(R.id.tabOwn);
        spec.setIndicator("Own Indi");
        tabHost.addTab(spec);
    }

    private void showWaiting() {
        System.out.println("CertActivity.showWaiting.NOT:IMPLEMENTED:YET");
        lock.lockWrite();
        lock.lockWrite();
    }

    private void showCert(TextView textView, Certificate cert) {
        try {
            X509Certificate myX509Certificate = CertificateManager.loadX509CertificateFromBytes(cert.getCertificate().v());
            textView.setText(myX509Certificate.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onRegistrationFinished() {
        lock.unlockWrite();
        finish();
    }
}