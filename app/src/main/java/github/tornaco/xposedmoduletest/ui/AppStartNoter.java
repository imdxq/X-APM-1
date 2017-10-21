package github.tornaco.xposedmoduletest.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.andrognito.pinlockview.IndicatorDots;
import com.andrognito.pinlockview.PinLockListener;
import com.andrognito.pinlockview.PinLockView;

import junit.framework.Assert;

import org.newstand.logger.Logger;

import java.util.Observable;
import java.util.Observer;

import github.tornaco.android.common.Holder;
import github.tornaco.xposedmoduletest.ICallback;
import github.tornaco.xposedmoduletest.R;
import github.tornaco.xposedmoduletest.camera.CameraManager;
import github.tornaco.xposedmoduletest.x.XEnc;
import github.tornaco.xposedmoduletest.x.XMode;
import github.tornaco.xposedmoduletest.x.XSettings;

/**
 * Created by guohao4 on 2017/10/17.
 * Email: Tornaco@163.com
 */

@SuppressWarnings("ConstantConditions")
public class AppStartNoter {

    private Handler mUiHandler;
    private Context mContext;

    private boolean mTakePhoto, mFullscreen;

    private Animation mErrorAnim;

    private final Holder<String> mPsscode = new Holder<>();

    public AppStartNoter(Handler uiHandler, Context context) {
        this.mUiHandler = uiHandler;
        this.mContext = context;
        Assert.assertTrue(
                "MainLopper is needed",
                this.mUiHandler.getLooper() == Looper.getMainLooper());
        this.mTakePhoto = XSettings.get().takenPhotoEnabled(context);
        this.mFullscreen = XSettings.get().fullScreenNoter(context);
        this.mPsscode.setData(XSettings.getPassCodeEncrypt(context));//FIXME Enc-->NoneEnc
        this.mErrorAnim = AnimationUtils.loadAnimation(context, R.anim.shake);
        registerObserver();
    }

    private void registerObserver() {
        XSettings.get().addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                mTakePhoto = XSettings.get().takenPhotoEnabled(mContext);
                mFullscreen = XSettings.get().fullScreenNoter(mContext);
                mPsscode.setData(XSettings.getPassCodeEncrypt(mContext));//FIXME Enc-->NoneEnc
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void note(
            final String callingAppName,
            final String targetPkg,
            final String appName,
            final ICallback callback) {
        mUiHandler.post(new LockDialog(callingAppName, targetPkg, appName, callback));
    }

    private class LockDialog implements Runnable {

        final String callingAppName;
        final String targetPkg;
        final String appName;
        final ICallback callback;

        LockDialog(String callingAppName, String targetPkg, String appName, ICallback callback) {
            this.callingAppName = callingAppName;
            this.targetPkg = targetPkg;
            this.appName = appName;
            this.callback = callback;
        }

        @Override
        public void run() {
            try {
                // Check if our passcode has been set.
                if (!XEnc.isPassCodeValid(mPsscode.getData())) {
                    Logger.w("Pass code not valid, ignoring...");
                    Toast.makeText(mContext, R.string.summary_setup_passcode_none_set, Toast.LENGTH_SHORT).show();
                    onPass(callback);
                    return;
                }

                Logger.v("Init note dialog, mFullscreen:" + mFullscreen);

                @SuppressLint("InflateParams") final View container = LayoutInflater.from(mContext)
                        .inflate(mFullscreen ? R.layout.app_noter_fullscreen : R.layout.app_noter, null, false);

                PinLockView pinLockView = (PinLockView) container.findViewById(R.id.pin_lock_view);
                IndicatorDots indicatorDots = (IndicatorDots) container.findViewById(R.id.indicator_dots);
                pinLockView.attachIndicatorDots(indicatorDots);

                TextView labelView = (TextView) container.findViewById(R.id.label);
                labelView.setText(appName);

                final Dialog md =
                        new AlertDialog.Builder(mContext,
                                mFullscreen ? R.style.NoterLightFullScreen : R.style.NoterLight)
                                .setView(container)
                                .setCancelable(true)
                                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        onFail(callback);
                                    }
                                })
                                .create();

                md.getWindow().setType(
                        WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

                pinLockView.setPinLockListener(new PinLockListener() {
                    @Override
                    public void onComplete(String pin) {
                        if (XEnc.isPassCodeCorrect(mPsscode.getData(), pin)) {
                            md.dismiss();
                            onPass(callback);
                        } else {
                            container.clearAnimation();
                            container.startAnimation(mErrorAnim);

                            if (mTakePhoto) {
                                CameraManager.get().captureSaveAsync(new CameraManager.PictureCallback() {
                                    @Override
                                    public void onImageReady(String path) {
                                        Logger.v("onImageReady:" + path);
                                    }

                                    @Override
                                    public void onFail(Exception e) {
                                        Logger.v("onImageFail:" + e);
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onEmpty() {

                    }

                    @Override
                    public void onPinChange(int pinLength, String intermediatePin) {

                    }
                });

                Logger.d("Show note dialog...");
                md.show();

                // Setup camera preview.
                View softwareCameraPreview = container.findViewById(R.id.surface);
                if (softwareCameraPreview != null)
                    softwareCameraPreview.setVisibility(mTakePhoto ? View.VISIBLE : View.GONE);
            } catch (Exception e) {
                Logger.e("Can not show dialog:" + Logger.getStackTraceString(e));
                Toast.makeText(mContext, "FATAL- Fail show lock dialog:\n" + Logger.getStackTraceString(e),
                        Toast.LENGTH_LONG).show();
                // We should tell the res here.
                try {
                    callback.onRes(XMode.MODE_IGNORED); // BYPASS.
                } catch (RemoteException e1) {
                    Logger.e(Logger.getStackTraceString(e1));
                }
            }
        }
    }

    private void onPass(ICallback callback) {
        try {
            callback.onRes(XMode.MODE_ALLOWED);
        } catch (RemoteException e) {
            Logger.e(Logger.getStackTraceString(e));
        }
    }

    private void onFail(ICallback callback) {
        try {
            callback.onRes(XMode.MODE_DENIED);
        } catch (RemoteException e) {
            Logger.e(Logger.getStackTraceString(e));
        }
    }
}
