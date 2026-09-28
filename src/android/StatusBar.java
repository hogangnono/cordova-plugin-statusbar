/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/
package org.apache.cordova.statusbar;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import java.util.Arrays;

public class StatusBar extends CordovaPlugin {
    private static final String TAG = "StatusBar";

    /**
     * 마지막으로 JS 에서 요청한 스타일.
     *
     * cordova-android 15 의 SystemBarPlugin 이 onResume·configuration 변경 때마다
     * BackgroundColor preference 기준으로 아이콘 색을 되돌려버린다.
     * 그래서 이 값을 들고 있다가 onResume 뒤에 다시 적용한다.
     */
    private String lastRequestedStyle = null;

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    @Override
    public void initialize(final CordovaInterface cordova, CordovaWebView webView) {
        LOG.v(TAG, "StatusBar: initialization");
        super.initialize(cordova, webView);

        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Clear flag FLAG_FORCE_NOT_FULLSCREEN which is set initially
                // by the Cordova.
                Window window = cordova.getActivity().getWindow();
                window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

                // Read 'StatusBarBackgroundColor' from config.xml, default is #000000.
                setStatusBarBackgroundColor(preferences.getString("StatusBarBackgroundColor", "#000000"));

                // Read 'StatusBarStyle' from config.xml, default is 'lightcontent'.
                setStatusBarStyle(preferences.getString("StatusBarStyle", "lightcontent"));
            }
        });
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  True if the action was valid, false otherwise.
     */
    @Override
    public boolean execute(final String action, final CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        LOG.v(TAG, "Executing action: " + action);
        final Activity activity = this.cordova.getActivity();
        final Window window = activity.getWindow();

        if ("_ready".equals(action)) {
            boolean statusBarVisible = (window.getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) == 0;
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, statusBarVisible));
            return true;
        }

        // [workaround] 안드로이드 같은 경우에 스테이터스바 조작을 할 경우에 전체를 가려버리고, 스크롤 오버플로우가 제대로 동작하지 않는 문제가 있어서, 해당 부분만 주석처리한다.
        // if ("show".equals(action)) {
        //     this.cordova.getActivity().runOnUiThread(new Runnable() {
        //         @Override
        //         public void run() {
        //             // SYSTEM_UI_FLAG_FULLSCREEN is available since JellyBean, but we
        //             // use KitKat here to be aligned with "Fullscreen"  preference
        //             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        //                 int uiOptions = window.getDecorView().getSystemUiVisibility();
        //                 uiOptions &= ~View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        //                 uiOptions &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;

        //                 window.getDecorView().setSystemUiVisibility(uiOptions);
        //             }

        //             // CB-11197 We still need to update LayoutParams to force status bar
        //             // to be hidden when entering e.g. text fields
        //             window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //         }
        //     });
        //     return true;
        // }

        // if ("hide".equals(action)) {
        //     this.cordova.getActivity().runOnUiThread(new Runnable() {
        //         @Override
        //         public void run() {
        //             // SYSTEM_UI_FLAG_FULLSCREEN is available since JellyBean, but we
        //             // use KitKat here to be aligned with "Fullscreen"  preference
        //             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        //                 int uiOptions = window.getDecorView().getSystemUiVisibility()
        //                         | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        //                         | View.SYSTEM_UI_FLAG_FULLSCREEN;

        //                 window.getDecorView().setSystemUiVisibility(uiOptions);
        //             }

        //             // CB-11197 We still need to update LayoutParams to force status bar
        //             // to be hidden when entering e.g. text fields
        //             window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //         }
        //     });
        //     return true;
        // }

        if ("backgroundColorByHexString".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        setStatusBarBackgroundColor(args.getString(0));
                    } catch (JSONException ignore) {
                        LOG.e(TAG, "Invalid hexString argument, use f.i. '#777777'");
                    }
                }
            });
            return true;
        }

        if ("overlaysWebView".equals(action)) {
            if (Build.VERSION.SDK_INT >= 21) {
                this.cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            setStatusBarTransparent(args.getBoolean(0));
                        } catch (JSONException ignore) {
                            LOG.e(TAG, "Invalid boolean argument");
                        }
                    }
                });
                return true;
            }
            else return args.getBoolean(0) == false;
        }

        if ("styleDefault".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setStatusBarStyle("default");
                }
            });
            return true;
        }

        if ("styleLightContent".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setStatusBarStyle("lightcontent");
                }
            });
            return true;
        }

        if ("styleBlackTranslucent".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setStatusBarStyle("blacktranslucent");
                }
            });
            return true;
        }

        if ("styleBlackOpaque".equals(action)) {
            this.cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setStatusBarStyle("blackopaque");
                }
            });
            return true;
        }

        return false;
    }

    private void setStatusBarBackgroundColor(final String colorPref) {
        if (Build.VERSION.SDK_INT >= 21) {
            if (colorPref != null && !colorPref.isEmpty()) {
                final Window window = cordova.getActivity().getWindow();
                // Method and constants not available on all SDKs but we want to be able to compile this code with any SDK
                window.clearFlags(0x04000000); // SDK 19: WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(0x80000000); // SDK 21: WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                try {
                    // Using reflection makes sure any 5.0+ device will work without having to compile with SDK level 21
                    window.getClass().getMethod("setStatusBarColor", int.class).invoke(window, Color.parseColor(colorPref));
                } catch (IllegalArgumentException ignore) {
                    LOG.e(TAG, "Invalid hexString argument, use f.i. '#999999'");
                } catch (Exception ignore) {
                    // this should not happen, only in case Android removes this method in a version > 21
                    LOG.w(TAG, "Method window.setStatusBarColor not found for SDK level " + Build.VERSION.SDK_INT);
                }
            }
        }
    }

    private void setStatusBarTransparent(final boolean transparent) {
        if (Build.VERSION.SDK_INT >= 21) {
            final Window window = cordova.getActivity().getWindow();
            if (transparent) {
                window.getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                window.setStatusBarColor(Color.TRANSPARENT);
            }
            else {
                window.getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_VISIBLE);
            }
        }
    }

    /**
     * 상태바 아이콘(시계·배터리) 색을 바꾼다.
     *
     * 원본은 View#setSystemUiVisibility 와 SYSTEM_UI_FLAG_LIGHT_STATUS_BAR 를 썼지만,
     * 이 API 는 API 30 에서 deprecated 되고 targetSdk 35+ 에서는 아무 일도 하지 않는다.
     *
     * appearanceLight = true  -> 어두운 아이콘 (밝은 배경용, style "default")
     * appearanceLight = false -> 흰 아이콘   (어두운 배경용, style "lightcontent")
     */
    private void setStatusBarStyle(final String style) {
        if (style == null || style.isEmpty()) {
            return;
        }

        String[] darkContentStyles = {
            "default",
        };

        String[] lightContentStyles = {
            "lightcontent",
            "blacktranslucent",
            "blackopaque",
        };

        String normalized = style.toLowerCase();
        boolean appearanceLight;

        if (Arrays.asList(darkContentStyles).contains(normalized)) {
            appearanceLight = true;
        } else if (Arrays.asList(lightContentStyles).contains(normalized)) {
            appearanceLight = false;
        } else {
            LOG.e(TAG, "Invalid style, must be either 'default', 'lightcontent' or the deprecated 'blacktranslucent' and 'blackopaque'");
            return;
        }

        lastRequestedStyle = normalized;
        applyAppearanceLightStatusBars(appearanceLight);
    }

    private void applyAppearanceLightStatusBars(final boolean appearanceLight) {
        Window window = cordova.getActivity().getWindow();
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(appearanceLight);
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);

        if (lastRequestedStyle == null) {
            return;
        }

        // SystemBarPlugin 이 runOnUiThread 로 예약한 updateSystemBars 뒤에 실행되도록 큐에 넣는다.
        final Window window = cordova.getActivity().getWindow();
        window.getDecorView().post(new Runnable() {
            @Override
            public void run() {
                setStatusBarStyle(lastRequestedStyle);
            }
        });
    }
}
