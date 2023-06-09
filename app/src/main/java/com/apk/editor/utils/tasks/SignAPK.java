package com.apk.editor.utils.tasks;

import android.app.Activity;
import android.content.Intent;

import com.apk.editor.R;
import com.apk.editor.activities.APKTasksActivity;
import com.apk.editor.utils.APKData;
import com.apk.editor.utils.APKEditorUtils;
import com.apk.editor.utils.Common;

import java.io.File;

import in.sunilpaulmathew.sCommon.Utils.sExecutor;
import in.sunilpaulmathew.sCommon.Utils.sPackageUtils;
import in.sunilpaulmathew.sCommon.Utils.sUtils;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 28, 2023
 */
public class SignAPK extends sExecutor {

    private final Activity mActivity;
    private File mBackUpPath = null, mBuildDir = null, mExportPath = null, mTMPZip = null;

    public SignAPK(Activity activity) {
        mActivity = activity;
    }

    @Override
    public void onPreExecute() {
        mExportPath = new File(mActivity.getCacheDir(), Common.getAppID());
        mTMPZip = new File(mActivity.getCacheDir(), "tmp.apk");
        Common.setFinishStatus(false);
        Common.isCancelled(false);
        Common.isBuilding(true);
        Common.setStatus(null);
        Intent apkTasks = new Intent(mActivity, APKTasksActivity.class);
        mActivity.startActivity(apkTasks);
        Common.setStatus(mActivity.getString(R.string.preparing_apk, Common.getAppID()));

        mBuildDir = new File(mExportPath, ".aeeBuild");
        mBackUpPath = new File(mExportPath, ".aeeBackup");
        if (mBuildDir.exists()) {
            sUtils.delete(mBuildDir);
        }
        sUtils.mkdir(mBuildDir);

        if (mTMPZip.exists()) {
            sUtils.delete(mTMPZip);
        }
    }

    @Override
    public void doInBackground() {
        Common.setStatus(mActivity.getString(R.string.preparing_source));
        APKData.prepareSource(mBuildDir, mExportPath, mBackUpPath, mActivity);
        if (Common.getError() > 0) {
            return;
        }
        APKEditorUtils.zip(mBuildDir, mTMPZip);
        File mParent;
        if (sPackageUtils.isPackageInstalled(Common.getAppID(), mActivity) && APKData.isAppBundle(sPackageUtils
                .getSourceDir(Common.getAppID(), mActivity))) {
            mParent = new File(APKData.getExportAPKsPath(mActivity), Common.getAppID() + "_aee-signed");
            if (mParent.exists()) {
                sUtils.delete(mParent);
            }
            sUtils.mkdir(mParent);
            for (String mSplits : APKData.splitApks(sPackageUtils.getSourceDir(Common.getAppID(), mActivity))) {
                if (!new File(mSplits).getName().equals("base.apk")) {
                    Common.setStatus(mActivity.getString(R.string.signing, new File(mSplits).getName()));
                    APKData.signApks(new File(mSplits), new File(mParent, new File(mSplits).getName()), mActivity);
                }
            }
            Common.setStatus(mActivity.getString(R.string.signing, "base.apk"));
            APKData.signApks(mTMPZip, new File(mParent, "base.apk"), mActivity);
        } else {
            mParent = new File(APKData.getExportAPKsPath(mActivity), Common.getAppID() + "_aee-signed.apk");
            if (mParent.exists()) {
                sUtils.delete(mParent);
            }
            Common.setStatus(mActivity.getString(R.string.signing, mParent.getName()));
            APKData.signApks(mTMPZip, mParent, mActivity);
        }
        if (Common.isCancelled()) {
            sUtils.delete(mParent);
        }
    }

    @Override
    public void onPostExecute() {
        sUtils.delete(mTMPZip);
        sUtils.delete(mBuildDir);
        if (!Common.isFinished()) {
            Common.setFinishStatus(true);
        }
        mActivity.finish();
    }

}