package github.tornaco.xposedmoduletest.ui.activity.green2;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.List;

import github.tornaco.android.common.Collections;
import github.tornaco.android.common.Consumer;
import github.tornaco.xposedmoduletest.R;
import github.tornaco.xposedmoduletest.compat.os.AppOpsManagerCompat;
import github.tornaco.xposedmoduletest.loader.ComponentLoader;
import github.tornaco.xposedmoduletest.model.CommonPackageInfo;
import github.tornaco.xposedmoduletest.ui.activity.common.CommonPackageInfoListActivity;
import github.tornaco.xposedmoduletest.ui.adapter.common.CommonPackageInfoAdapter;
import github.tornaco.xposedmoduletest.ui.adapter.common.CommonPackageInfoViewerChooserAdapter;
import github.tornaco.xposedmoduletest.ui.widget.SwitchBar;
import github.tornaco.xposedmoduletest.xposed.app.XAshmanManager;

/**
 * Created by guohao4 on 2017/11/18.
 * Email: Tornaco@163.com
 */
@SuppressLint("Registered")
public class PackageViewerActivity extends CommonPackageInfoListActivity {

    private boolean mShowSystemApp;

    private int colorGreen, colorDefault, colorSet;

    @Override
    protected void initView() {
        super.initView();
        fab.hide();

        colorDefault = ContextCompat.getColor(getContext(), R.color.grey);
        colorGreen = ContextCompat.getColor(getContext(), R.color.green_dark);
        colorSet = ContextCompat.getColor(getContext(), R.color.blue);
    }

    @Override
    protected void onInitSwitchBar(SwitchBar switchBar) {
        switchBar.hide();
    }

    @Override
    protected int getSummaryRes() {
        return R.string.summary_greening_app;
    }

    @Override
    protected void onRequestClearItemsInBackground() {
        super.onRequestClearItemsInBackground();

        final List<CommonPackageInfo> choosed = new ArrayList<>();
        Collections.consumeRemaining(getCommonPackageInfoAdapter().getCommonPackageInfos(),
                new Consumer<CommonPackageInfo>() {
                    @Override
                    public void accept(CommonPackageInfo info) {
                        if (info.isChecked()) choosed.add(info);
                    }
                });
        runOnUiThreadChecked(new Runnable() {
            @Override
            public void run() {
                showExtraPermSettingDialogInBatch(choosed);
            }
        });
    }

    @Override
    public void onLeaveChoiceMode() {
        fab.hide();
    }

    @Override
    public void onEnterChoiceMode() {
        fab.setImageResource(R.drawable.ic_mode_edit_black_24dp);
        fab.show(new FloatingActionButton.OnVisibilityChangedListener() {
            @Override
            public void onHidden(FloatingActionButton fab) {
                super.onHidden(fab);
            }
        });
    }

    @Override
    protected CommonPackageInfoAdapter onCreateAdapter() {
        CommonPackageInfoViewerChooserAdapter adapter = new CommonPackageInfoViewerChooserAdapter(this) {

            @Override
            protected int getTemplateLayoutRes() {
                return R.layout.app_list_item_2;
            }

            @Override
            public void onBindViewHolder(CommonPackageInfoAdapter.CommonViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);

                final CommonPackageInfo packageInfo = getCommonPackageInfos().get(position);

                String summary = "";

                holder.getExtraIndicator().setVisibility(View.VISIBLE);

                boolean cut = packageInfo.isAllExtraPermAllowed()
                        || packageInfo.isAllExtraPermDisabled();
                if (cut) {
                    if (packageInfo.isAllExtraPermAllowed()) {
                        summary = "默认状态";
                        holder.getExtraIndicator().setBackgroundColor(colorDefault);
                    } else if (packageInfo.isAllExtraPermDisabled()) {
                        summary = "纯绿色化";
                        holder.getExtraIndicator().setBackgroundColor(colorGreen);
                    }
                } else {
                    holder.getExtraIndicator().setBackgroundColor(colorSet);
                    if (packageInfo.isServiceOpAllowed()) {
                        summary += "服务\t\t";
                    }
                    if (packageInfo.isWakelockOpAllowed()) {
                        summary += "唤醒锁\t\t";
                    }
                    if (packageInfo.isAlarmOpAllowed()) {
                        summary += "唤醒定时器";
                    }
                }

                holder.getLineTwoTextView().setText(summary);
            }
        };

        adapter.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CommonPackageInfo info = getCommonPackageInfoAdapter().getCommonPackageInfos().get(position);
                showExtraPermSettinDialog(info);
            }
        });
        return adapter;
    }

    @Override
    protected List<CommonPackageInfo> performLoading() {
        return ComponentLoader.Impl.create(this).loadInstalledAppsWithOp(mShowSystemApp,
                ComponentLoader.Sort.byOp());
    }

    private static void updateOpState(CommonPackageInfo info) {
        int modeService = XAshmanManager.get()
                .getPermissionControlBlockModeForPkg(
                        AppOpsManagerCompat.OP_START_SERVICE, info.getPkgName());
        int modeWakelock = XAshmanManager.get()
                .getPermissionControlBlockModeForPkg(
                        AppOpsManagerCompat.OP_WAKE_LOCK, info.getPkgName());
        int modeAlarm = XAshmanManager.get()
                .getPermissionControlBlockModeForPkg(
                        AppOpsManagerCompat.OP_SET_ALARM, info.getPkgName());
        info.setServiceOpAllowed(modeService == AppOpsManagerCompat.MODE_ALLOWED);
        info.setAlarmOpAllowed(modeAlarm == AppOpsManagerCompat.MODE_ALLOWED);
        info.setWakelockOpAllowed(modeWakelock == AppOpsManagerCompat.MODE_ALLOWED);
    }


    private void showExtraPermSettinDialog(final CommonPackageInfo packageInfo) {

        final String[] items = {"服务", "唤醒锁", "唤醒定时器"};
        new AlertDialog.Builder(getActivity()).setCancelable(false)
                .setTitle("绿化设置")
                .setMultiChoiceItems(items, new boolean[]{
                                packageInfo.isServiceOpAllowed(),
                                packageInfo.isWakelockOpAllowed(),
                                packageInfo.isAlarmOpAllowed()},
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                int op = -1;
                                if (which == 0) op = AppOpsManagerCompat.OP_START_SERVICE;
                                if (which == 1) op = AppOpsManagerCompat.OP_WAKE_LOCK;
                                if (which == 2) op = AppOpsManagerCompat.OP_SET_ALARM;
                                int mode = isChecked ? AppOpsManagerCompat.MODE_ALLOWED : AppOpsManagerCompat.MODE_IGNORED;
                                XAshmanManager.get().setPermissionControlBlockModeForPkg(op, packageInfo.getPkgName(), mode);
                            }
                        })
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialoginterface, int i) {
                                dialoginterface.dismiss();

                                // Retrieve new state.
                                updateOpState(packageInfo);
                                getCommonPackageInfoAdapter().notifyDataSetChanged();
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showExtraPermSettingDialogInBatch(final List<CommonPackageInfo> packageInfo) {

        final String[] items = {"服务", "唤醒锁", "唤醒定时器"};

        final int[] modeService = new int[1];
        final int[] modeAlarm = new int[1];
        final int[] modeWakelock = new int[1];

        new AlertDialog.Builder(getActivity()).setCancelable(false)
                .setTitle("批量设置" + packageInfo.size() + "个应用")
                .setMultiChoiceItems(items, new boolean[]{
                                true,
                                true,
                                true},
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                final int mode = isChecked ? AppOpsManagerCompat.MODE_ALLOWED : AppOpsManagerCompat.MODE_IGNORED;
                                if (which == 0) {
                                    modeService[0] = mode;
                                }
                                if (which == 1) {
                                    modeWakelock[0] = mode;
                                }
                                if (which == 2) {
                                    modeAlarm[0] = mode;
                                }
                            }
                        })
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialoginterface, int i) {

                                Collections.consumeRemaining(packageInfo, new Consumer<CommonPackageInfo>() {
                                    @Override
                                    public void accept(CommonPackageInfo info) {
                                        XAshmanManager.get().setPermissionControlBlockModeForPkg(
                                                AppOpsManagerCompat.OP_START_SERVICE, info.getPkgName(), modeService[0]);
                                        XAshmanManager.get().setPermissionControlBlockModeForPkg(
                                                AppOpsManagerCompat.OP_SET_ALARM, info.getPkgName(), modeAlarm[0]);
                                        XAshmanManager.get().setPermissionControlBlockModeForPkg(
                                                AppOpsManagerCompat.OP_WAKE_LOCK, info.getPkgName(), modeWakelock[0]);


                                        updateOpState(info);
                                    }
                                });

                                dialoginterface.dismiss();
                                // Retrieve new state.
                                getCommonPackageInfoAdapter().notifyDataSetChanged();
                            }
                        })

                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.green_viewer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.show_system_app) {
            mShowSystemApp = !mShowSystemApp;
            item.setChecked(mShowSystemApp);
            startLoading();
        }
        return super.onOptionsItemSelected(item);
    }
}
