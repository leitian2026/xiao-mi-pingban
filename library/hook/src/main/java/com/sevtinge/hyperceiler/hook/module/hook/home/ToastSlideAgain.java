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
package com.sevtinge.hyperceiler.hook.module.hook.home;

import android.view.MotionEvent;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class ToastSlideAgain extends BaseHook {
    public XC_MethodHook.Unhook unhook = null;
    // 手势回调执行期间 > 0。原来每个触摸事件都要安装再撤销一次 Toast.show 钩子（很重，还会让相关代码的编译缓存失效），
    // 现在钩子只在 init 里挂一次，靠这个计数决定是否拦截
    private volatile int mBlockDepth = 0;

    private void endBlock() {
        if (mBlockDepth > 0) mBlockDepth--;
    }

    @Override
    public void init() {
        hookToast();
        findAndHookMethod("com.miui.home.recents.NavStubView",
            "onPointerEvent", MotionEvent.class,
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    mBlockDepth++;
                    // logI("im hook onPointerEvent");
                }

                @Override
                protected void after(MethodHookParam param) {
                    endBlock();
                }
            }
        );

        findAndHookMethod("com.miui.home.recents.GestureModeApp",
            lpparam.classLoader, "onStartGesture", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    mBlockDepth++;
                    // logI("im hook onStartGesture");
                }

                @Override
                protected void after(MethodHookParam param) {
                    endBlock();
                }
            }
        );
    }

    public XC_MethodHook.Unhook hookToast() {
        return XposedHelpers.findAndHookMethod(findClassIfExists("android.widget.Toast"),
            "show", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (mBlockDepth > 0) param.setResult(null);
                    // logI("im hook Toast show");
                }
            }
        );
    }

    public void unHook(XC_MethodHook.Unhook unhook) {
        if (unhook != null) {
            unhook.unhook();
            // logI("the unhook is: " + unhook);
        }  // logE("the unhook is: null");

    }
}
