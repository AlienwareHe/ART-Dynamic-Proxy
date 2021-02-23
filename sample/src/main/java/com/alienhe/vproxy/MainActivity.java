package com.alienhe.vproxy;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.alienhe.art.vproxy.ProxyBuilder;
import com.alienhe.art.vproxy.R;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "alienhe";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // hook package manger
        try{
            PackageManager packageManager = getPackageManager();
            Field mPMField = packageManager.getClass().getDeclaredField("mPM");
            mPMField.setAccessible(true);
            Object mPM = mPMField.get(packageManager);
            Object normalPM = Proxy.newProxyInstance(getClassLoader(), mPM.getClass().getInterfaces(), new InvocationHandler() {
                @Override
                public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                    return null;
                }
            });
            mPMField.set(packageManager,normalPM);
            Log.i(TAG,"normal package manager hook,and detect result:" + checkPMProxy());

            // vproxy hook
            Object vproxyPM = ProxyBuilder.newProxyInstance(this, mPM.getClass().getInterfaces()[0], new InvocationHandler() {
                @Override
                public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                    return null;
                }
            });
            mPMField.set(packageManager,vproxyPM);
            Log.i(TAG,"vproxy package manager hook,and detect result:" + checkPMProxy());
        }catch (Exception e){
            Log.e(TAG,"hook error:",e);
        }

    }

    /**
     * 检测 PM 代理
     */
    @SuppressLint("PrivateApi")
    private boolean checkPMProxy() {
        String truePMName = "android.content.pm.IPackageManager$Stub$Proxy";
        String nowPMName = "";
        try {
            // 被代理的对象是 PackageManager.mPM
            PackageManager packageManager = getPackageManager();
            Field mPMField = packageManager.getClass().getDeclaredField("mPM");
            mPMField.setAccessible(true);
            Object mPM = mPMField.get(packageManager);
            nowPMName = mPM.getClass().getName();
            Log.i(TAG, "PackageManager now class name:" + nowPMName + "|" + (mPM instanceof Proxy));
            return !(mPM instanceof Proxy);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}