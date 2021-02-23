# aims
在安卓下通过动态生成DEX，自己实现java.lang.reflect.Proxy的动态代理机制，解决某些风控检查动态代理实现的Hook痕迹，例如VA 的PMS检测，例如：
```
PackageManager packageManager = getPackageManager();
Field mPMField = packageManager.getClass().getDeclaredField("mPM");
mPMField.setAccessible(true);
Object mPM = mPMField.get(packageManager);
nowPMName = mPM.getClass().getName();
return !(mPM instanceof Proxy);
```

# FIX
- support method with return
- compat return primitive type object
- fix long/double wide type params
- ...

# TODO
- multi interfaces


# Thanks
- (Experimental project on generating byte code for Dalvik/ART in runtime for creating dynamic proxies (like java.lang.reflect.Proxy))[https://github.com/int02h/fast-proxy]