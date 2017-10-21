package github.tornaco.xposedmoduletest.x;

import android.os.Build;
import android.support.annotation.Keep;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by guohao4 on 2017/10/19.
 * Email: Tornaco@163.com
 */
@Keep
public class XModuleDelegate extends XModule {

    private XModule mImpl;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        mImpl.handleLoadPackage(lpparam);
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        mImpl.initZygote(startupParam);
    }

    public XModuleDelegate() {
        int sdkInt = Build.VERSION.SDK_INT;

        switch (sdkInt) {
            case Build.VERSION_CODES.N_MR1:
                mImpl = new XModuleImpl25();
                break;
            case Build.VERSION_CODES.N:
                mImpl = new XModuleImpl24();
                break;
            case Build.VERSION_CODES.M:
                mImpl = new XModuleImpl23();
                break;
            case Build.VERSION_CODES.LOLLIPOP_MR1:
            case Build.VERSION_CODES.LOLLIPOP:
                mImpl = new XModuleImpl22();
                break;
            default:
                mImpl = new XModuleNotSupport();
                break;
        }

        XposedBridge.log(String.format("Init XModuleDelegate with SDK: %s, impl %s:", sdkInt, mImpl));
    }
}
