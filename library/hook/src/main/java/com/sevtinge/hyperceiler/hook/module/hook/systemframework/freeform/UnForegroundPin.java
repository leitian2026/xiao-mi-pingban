/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.module.hook.systemframework.freeform;

import android.util.Log;
import android.view.SurfaceControl;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class UnForegroundPin extends BaseHook {
    @Override
    public void init() {
        try {
            /*Hyper*/
            getDeclaredMethod("com.android.server.wm.MiuiFreeFormGestureController",
                "needForegroundPin",
                "com.android.server.wm.MiuiFreeFormActivityStack");
            findAndHookMethod("com.android.server.wm.MiuiFreeFormGestureController",
                "needForegroundPin",
                "com.android.server.wm.MiuiFreeFormActivityStack",
                new MethodHook() {
                    @Override
                    protected void before(XC_MethodHook.MethodHookParam param) {
                        param.setResult(true);
                    }
                }
            );
        } catch (Throwable throwable) {
            logE(TAG, "Hyper UnForegroundPin E, if you is Miui don't worry : " + throwable);
            /*Miui*/
            findAndHookMethod("com.android.server.wm.MiuiFreeFormGestureController",
                "moveTaskToBack",
                "com.android.server.wm.MiuiFreeFormActivityStack",
                new MethodHook() {
                    @Override
                    protected void before(XC_MethodHook.MethodHookParam param) {
                        Object stack = param.args[0];
                        if (stack != null) {
                            Object mTask = XposedHelpers.getObjectField(stack, "mTask");
                            if (mTask != null) {
                                SurfaceControl surfaceControl;
                                if (mTask != null && (surfaceControl =
                                    (SurfaceControl) XposedHelpers.getObjectField(mTask,
                                        "mSurfaceControl")) != null
                                    && surfaceControl.isValid()) {
                                    SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
                                    XposedHelpers.callMethod(transaction, "hide", surfaceControl);
                                    XposedHelpers.callMethod(transaction, "apply");
                                    // logE(tag, "moveTaskToBack: s: " + surfaceControl);
                                    param.setResult(null);
                                }
                            }
                        }
                    }
                }
            );

            hookAllConstructors("com.android.server.wm.Task",
                new MethodHook() {
                    @Override
                    protected void after(XC_MethodHook.MethodHookParam param) {
                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "mLastSurfaceVisibility", true);
                        // logE(tag, "Task add mLastSurfaceVisibility");
                    }
                }
            );

            findAndHookMethod("com.android.server.wm.Task",
                "prepareSurfaces", new MethodHook() {
                    // prepareSurfaces 在 system_server 里每次布局都会对每个任务调用，是极热的路径。
                    // 原来无论是不是自由窗口都先做十几次反射；后面所有分支都要求 mffs != null，
                    // 所以先用最少的几次调用判断，不是自由窗口就直接返回。
                    @Override
                    protected void after(XC_MethodHook.MethodHookParam param) {
                        Object task = param.thisObject;
                        int taskId = (int) XposedHelpers.callMethod(task, "getRootTaskId");
                        Object mWmService = XposedHelpers.getObjectField(task, "mWmService");
                        Object mAtmService = XposedHelpers.getObjectField(mWmService, "mAtmService");
                        Object mMiuiFreeFormManagerService = XposedHelpers.getObjectField(mAtmService, "mMiuiFreeFormManagerService");
                        Object mffs = XposedHelpers.callMethod(mMiuiFreeFormManagerService, "getMiuiFreeFormActivityStack", taskId);
                        if (mffs == null) return;

                        SurfaceControl mSurfaceControl = (SurfaceControl) XposedHelpers.getObjectField(task, "mSurfaceControl");
                        if (mSurfaceControl == null) return;

                        boolean inPinMode = (boolean) XposedHelpers.callMethod(mffs, "inPinMode");
                        if (inPinMode) {
                            boolean isAnimating = (boolean) XposedHelpers.callMethod(task, "isAnimating", 7);
                            if (!isAnimating) {
                                SurfaceControl.Transaction transaction = (SurfaceControl.Transaction) XposedHelpers.callMethod(task, "getSyncTransaction");
                                XposedHelpers.callMethod(transaction, "setVisibility", mSurfaceControl, false);
                                XposedHelpers.setAdditionalInstanceField(task, "mLastSurfaceVisibility", false);
                            }
                        } else {
                            Object last = XposedHelpers.getAdditionalInstanceField(task, "mLastSurfaceVisibility");
                            boolean mLastSurfaceVisibility = last == null || (boolean) last;
                            if (!mLastSurfaceVisibility) {
                                SurfaceControl.Transaction transaction = (SurfaceControl.Transaction) XposedHelpers.callMethod(task, "getSyncTransaction");
                                XposedHelpers.callMethod(transaction, "setVisibility", mSurfaceControl, true);
                                XposedHelpers.setAdditionalInstanceField(task, "mLastSurfaceVisibility", true);
                            }
                        }
                    }
                }
            );

            findAndHookMethod("com.android.server.wm.MiuiFreeFormGestureController",
                "moveTaskToFront",
                "com.android.server.wm.MiuiFreeFormActivityStack",
                new MethodHook() {
                    @Override
                    protected void before(XC_MethodHook.MethodHookParam param) {
                        param.setResult(null);
                    }
                }
            );

            findAndHookMethod("com.android.server.wm.MiuiFreeformPinManagerService",
                "lambda$unPinFloatingWindow$0$com-android-server-wm-MiuiFreeformPinManagerService",
                "com.android.server.wm.MiuiFreeFormActivityStack",
                float.class, float.class, boolean.class, "com.android.server.wm.DisplayContent",
                "com.android.server.wm.MiuiFreeFormFloatIconInfo",
                new MethodHook() {
                    @Override
                    protected void before(XC_MethodHook.MethodHookParam param) {
                        Object mffas = param.args[0];
                        Object activityRecord = XposedHelpers.callMethod(XposedHelpers.getObjectField(mffas, "mTask"),
                            "getTopNonFinishingActivity");
                        /*遵循安卓日志*/
                        Log.i("MiuiFreeformPinManagerService",
                            "unPinFloatingWindow mffas: " + mffas + " activityRecord: " + activityRecord);
                        if (activityRecord == null) {
                            param.setResult(null);
                            return;
                        }
                        XposedHelpers.setObjectField(mffas, "mEnterVelocityX", param.args[1]);
                        XposedHelpers.setObjectField(mffas, "mEnterVelocityY", param.args[2]);
                        XposedHelpers.setObjectField(mffas, "mIsEnterClick", param.args[3]);
                        XposedHelpers.setObjectField(mffas, "mIsPinFloatingWindowPosInit", false);
                        XposedHelpers.callMethod(param.thisObject, "setUpMiuiFreeWindowFloatIconAnimation",
                            mffas, activityRecord, param.args[4], param.args[5]);
                        XposedHelpers.callMethod(param.thisObject, "startUnPinAnimation", mffas);
                        param.setResult(null);
                    }
                }
            );

            findAndHookMethod("com.android.server.wm.MiuiFreeFormGestureController",
                "lambda$startFullscreenFromFreeform$2$com-android-server-wm-MiuiFreeFormGestureController",
                "com.android.server.wm.MiuiFreeFormActivityStack",
                new MethodHook() {
                    @Override
                    protected void before(XC_MethodHook.MethodHookParam param) {
                        Object mffas = param.args[0];
                        Object mGestureListener = XposedHelpers.getObjectField(param.thisObject, "mGestureListener");
                        if ((boolean) XposedHelpers.callMethod(mffas, "isInFreeFormMode")) {
                            XposedHelpers.callMethod(mGestureListener, "startFullScreenFromFreeFormAnimation", mffas);
                            Object mTrackManager = XposedHelpers.getObjectField(param.thisObject, "mTrackManager");
                            if (mTrackManager != null) {
                                XposedHelpers.callMethod(mTrackManager, "trackSmallWindowPinedQuitEvent",
                                    XposedHelpers.callMethod(mffas, "getStackPackageName"),
                                    XposedHelpers.callMethod(mffas, "getApplicationName"),
                                    (long) XposedHelpers.getObjectField(mffas, "mPinedStartTime") != 0 ?
                                        ((float) (System.currentTimeMillis() -
                                            (long) XposedHelpers.getObjectField(mffas,
                                                "mPinedStartTime"))) / 1000.0f : 0.0f
                                );
                            }
                        } else if ((boolean) XposedHelpers.callMethod(mffas, "isInMiniFreeFormMode")) {
                            XposedHelpers.callMethod(mGestureListener, "startFullScreenFromSmallAnimation", mffas);
                            Object mTrackManager = XposedHelpers.getObjectField(param.thisObject, "mTrackManager");
                            if (mTrackManager != null) {
                                XposedHelpers.callMethod(mTrackManager, "trackMiniWindowPinedQuitEvent",
                                    XposedHelpers.callMethod(mffas, "getStackPackageName"),
                                    XposedHelpers.callMethod(mffas, "getApplicationName"),
                                    (long) XposedHelpers.getObjectField(mffas, "mPinedStartTime") != 0 ?
                                        ((float) (System.currentTimeMillis() -
                                            (long) XposedHelpers.getObjectField(mffas,
                                                "mPinedStartTime"))) / 1000.0f : 0.0f
                                );
                            }
                        }
                        XposedHelpers.setObjectField(mffas, "mPinedStartTime", 0L);
                        XposedHelpers.callMethod(mffas, "setInPinMode", false);
                        param.setResult(null);
                    }
                }
            );

        }
    }
}
