package com.OcrTranslator.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.DisplayCutout;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class UIUtil {

    // 刘海屏  陷进去
    public static void fullINScreen(Window window) {
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//        window.setStatusBarColor(Color.TRANSPARENT);
        //设置页面全屏显示
        WindowManager.LayoutParams lp = window.getAttributes();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            //设置页面延伸到刘海区显示
            window.setAttributes(lp);
        }

    }


    // 判断是否是刘海屏
    public static boolean hasNotchScreen(Activity activity) {
        try {

            if (getInt(activity) == 1 || // 小米刘海屏判断
                    hasNotchAtHuawei(activity) || // 华为刘海屏判断
                    hasNotchAtOPPO(activity) || // OPPO刘海屏判断
                    hasNotchAtVivo(activity) || // Vivo刘海屏判断
                    isAndroidP(activity) != null || // Android P版本刘海屏判断
                    getBarHeight(activity) >= 80 // 一般状态栏高度超过80可以认为就是刘海屏
                //TODO 各种品牌
            ) {
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Android P 刘海屏判断
    @TargetApi(28)
    private static DisplayCutout isAndroidP(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        if (decorView != null && Build.VERSION.SDK_INT >= 28) {
            WindowInsets windowInsets = decorView.getRootWindowInsets();
            if (windowInsets != null)
                return windowInsets.getDisplayCutout();
        }
        return null;
    }


    // 获取状态栏高度
    public static int getBarHeight(Activity activity) {
        int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return activity.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private static boolean isXiaomi() {
        return "Xiaomi".equals(Build.MANUFACTURER);
    }

    // 小米刘海屏判断
    @SuppressWarnings({"unchecked"})
    private static int getInt(Activity activity) {
        int result = 0;
        if (isXiaomi()) {
            try {
                ClassLoader classLoader = activity.getClassLoader();
                @SuppressWarnings("rawtypes")
                Class SystemProperties = classLoader.loadClass("android.os.SystemProperties");
                //参数类型
                @SuppressWarnings("rawtypes")
                Class[] paramTypes = new Class[2];
                paramTypes[0] = String.class;
                paramTypes[1] = int.class;
                Method getInt = SystemProperties.getMethod("getInt", paramTypes);
                //参数
                Object[] params = new Object[2];
                params[0] = new String("ro.miui.notch");
                params[1] = 0;
                result = (Integer) getInt.invoke(SystemProperties, params);

            } catch (ClassNotFoundException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    // 华为刘海屏判断
    @SuppressWarnings({"finally", "unchecked"})
    private static boolean hasNotchAtHuawei(Context context) {
        boolean ret = false;
        try {
            ClassLoader classLoader = context.getClassLoader();
            Class HwNotchSizeUtil = classLoader.loadClass("com.huawei.android.util.HwNotchSizeUtil");
            Method get = HwNotchSizeUtil.getMethod("hasNotchInScreen");
            ret = (Boolean) get.invoke(HwNotchSizeUtil);
        } catch (Exception ignored) {
        } finally {
            return ret;
        }
    }

    private static final int VIVO_NOTCH = 0x00000020;//是否有刘海
    private static final int VIVO_FILLET = 0x00000008;//是否有圆角

    // VIVO刘海屏判断
    @SuppressWarnings({"finally", "unchecked"})
    private static boolean hasNotchAtVivo(Context context) {
        boolean ret = false;
        try {
            ClassLoader classLoader = context.getClassLoader();
            Class FtFeature = classLoader.loadClass("android.util.FtFeature");
            Method method = FtFeature.getMethod("isFeatureSupport", int.class);
            ret = (Boolean) method.invoke(FtFeature, VIVO_NOTCH);
        } catch (Exception ignored) {
        } finally {
            return ret;
        }
    }

    // OPPO刘海屏判断
    private static boolean hasNotchAtOPPO(Context context) {
        return context.getPackageManager().hasSystemFeature("com.oppo.feature.screen.heteromorphism");
    }

}
