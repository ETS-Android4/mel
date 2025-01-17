package de.mel.android.controller.intro;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import de.mel.R;
import de.mel.android.MainActivity;
import de.mel.android.MelActivity;
import de.mel.android.Notifier;
import de.mel.android.service.AndroidService;
import de.mel.android.service.AndroidServiceBind;

public class IntroWrapper extends RelativeLayout implements AndroidServiceBind {

    public interface IntroDoneListener {
        void introDone();
    }

    protected MainActivity melActivity;
    private LinearLayout container;
    private AppCompatImageButton btnForward, btnPrevious;
    protected TextView lblTitle;
    protected TextView lblIndex;
    protected IntroPageController pageController;
    protected int index = 1;
    protected int maxIndex = 4;
    protected IntroDoneListener introDoneListener;

    public IntroWrapper(MainActivity melActivity) {
        super(melActivity.getApplicationContext());
        this.melActivity = melActivity;
        init(melActivity.getApplicationContext());
    }


    private void init(Context context) {
        inflate(context, R.layout.intro_wrapper, this);
        lblTitle = findViewById(R.id.lblTitle);
        container = findViewById(R.id.container);
        btnForward = findViewById(R.id.btnForward);
        btnPrevious = findViewById(R.id.btnPrevious);
        lblIndex = findViewById(R.id.lblIndex);
        btnPrevious.setVisibility(INVISIBLE);
        btnForward.setOnClickListener(v -> {
            if (pageController.getError() == null) {
                btnForward.setEnabled(true);
                btnPrevious.setEnabled(true);
                btnPrevious.setVisibility(VISIBLE);
                if (index < maxIndex) {
                    index++;
                    showPage();
                } else {
                    if (introDoneListener != null) {
                        introDoneListener.introDone();
                    }
                    return;
                }
                if (index == maxIndex) {
                    melActivity.runOnUiThread(() -> {
                        btnForward.setImageResource(R.drawable.icon_finish);
                    });
                }
            } else {
                Notifier.toast(melActivity, pageController.getError());
            }
        });
        btnPrevious.setOnClickListener(v -> {
            btnPrevious.setEnabled(true);
            btnForward.setEnabled(true);
            if (index > 1) {
                index--;
                showPage();
            }
            if (index == 1) {
                btnPrevious.setEnabled(false);
                btnPrevious.setVisibility(INVISIBLE);
            } else if (index < maxIndex) {
                melActivity.runOnUiThread(() -> {
                    btnForward.setImageResource(R.drawable.icon_next);
                });
            }
        });
        showPage();
    }

    public void setIntroDoneListener(IntroDoneListener introDoneListener) {
        this.introDoneListener = introDoneListener;
    }

    protected void showPage() {
        if (pageController != null) {
            pageController.onDestroy();
            pageController.onAndroidServiceUnbound();
        }
        switch (index) {
            case 1:
                pageController = new FirstPage(this);
                break;
            case 2:
                pageController = new SecondPage(this);
                break;
            case 3:
                pageController = new ThirdPage(this);
                break;
            case 4:
                pageController = new FourthPage(this);
                break;
        }
        if (melActivity.getAndroidService() != null) {
            pageController.onAndroidServiceAvailable(melActivity.getAndroidService());
        }
        lblTitle.setText(pageController.getTitle());
        lblIndex.setText(index + "/" + 4);
    }

    public MainActivity getMelActivity() {
        return melActivity;
    }

    public ViewGroup getContainer() {
        return container;
    }

    public void setBtnForwardActive(boolean active) {
        btnForward.setEnabled(active);
    }

    @Override
    public void onAndroidServiceAvailable(AndroidService androidService) {
        pageController.onAndroidServiceAvailable(androidService);
        melActivity.runOnUiThread(() -> btnForward.setVisibility(VISIBLE));
    }

    @Override
    public void onAndroidServiceUnbound() {
        pageController.onAndroidServiceUnbound();
    }
}
