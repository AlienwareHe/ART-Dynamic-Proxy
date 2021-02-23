package com.alienhe.art.vproxy;

import android.content.Context;
import android.os.Build;

import com.alienhe.art.vproxy.dex.AccessFlags;
import com.alienhe.art.vproxy.dex.DexBuilder;
import com.alienhe.art.vproxy.dex.DexClassDef;
import com.alienhe.art.vproxy.dex.DexCode;
import com.alienhe.art.vproxy.dex.DexField;
import com.alienhe.art.vproxy.dex.DexMethod;
import com.alienhe.art.vproxy.dex.DexProto;
import com.alienhe.art.vproxy.dex.DexType;
import com.alienhe.art.vproxy.dex.writer.DexWriter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import dalvik.system.DexClassLoader;

import static com.alienhe.art.vproxy.instruction.DexInstructions.agetObject;
import static com.alienhe.art.vproxy.instruction.DexInstructions.aputObject;
import static com.alienhe.art.vproxy.instruction.DexInstructions.checkCast;
import static com.alienhe.art.vproxy.instruction.DexInstructions.const16;
import static com.alienhe.art.vproxy.instruction.DexInstructions.const4;
import static com.alienhe.art.vproxy.instruction.DexInstructions.igetObject;
import static com.alienhe.art.vproxy.instruction.DexInstructions.invokeDirect;
import static com.alienhe.art.vproxy.instruction.DexInstructions.invokeInterface;
import static com.alienhe.art.vproxy.instruction.DexInstructions.invokeStatic;
import static com.alienhe.art.vproxy.instruction.DexInstructions.invokeVirtual;
import static com.alienhe.art.vproxy.instruction.DexInstructions.iputObject;
import static com.alienhe.art.vproxy.instruction.DexInstructions.moveResult;
import static com.alienhe.art.vproxy.instruction.DexInstructions.moveResultObject;
import static com.alienhe.art.vproxy.instruction.DexInstructions.newArray;
import static com.alienhe.art.vproxy.instruction.DexInstructions.returnObject;
import static com.alienhe.art.vproxy.instruction.DexInstructions.returnPrimitive;
import static com.alienhe.art.vproxy.instruction.DexInstructions.returnVoid;

/**
 * @author alienhe
 */
public class ProxyBuilder<T> {

    private static final int DEX_VERSION = 0x35;
    private static final String PROXY_CLASS_NAME_PREFIX = "_Proxy_";

    private final DexBuilder dexBuilder = new DexBuilder(DEX_VERSION);
    private final Class<T> interfaceClass;
    private final InvocationHandler invocationHandler;

    private DexType proxyType;
    private DexClassDef.Builder proxyClassBuilder;
    private DexField handlerField;
    private DexField methodsField;
    private Method[] methods;

    private ProxyBuilder(final Class<T> interfaceClass, final InvocationHandler invocationHandler) {
        this.interfaceClass = interfaceClass;
        this.invocationHandler = invocationHandler;
    }

    public static <T> T newProxyInstance(final Context context,
                                         final Class<T> interfaceClass,
                                         final InvocationHandler invocationHandler) {
        final ProxyBuilder<T> builder = new ProxyBuilder<>(interfaceClass, invocationHandler);
        builder.prepare();
        builder.generatedFields();
        builder.generateConstructor();
        builder.generateMethods();

        builder.proxyClassBuilder.build();
        builder.writeDex(context);
        return builder.createProxyInstance(context);
    }

    private void prepare() {
        this.proxyType = dexBuilder.addType(getProxyTypeDescription());

        final DexType objectType = dexBuilder.addType(Object.class);
        this.proxyClassBuilder = dexBuilder.addClass()
                .type(proxyType)
                .accessFlags(AccessFlags.fromValue(AccessFlags.ACC_PUBLIC))
                .superClass(objectType)
                .implementedInterface(dexBuilder.addType(interfaceClass));
    }

    private void generatedFields() {
        handlerField = dexBuilder.addField(proxyType,
                dexBuilder.addString("handler"),
                dexBuilder.addType(InvocationHandler.class));
        proxyClassBuilder.instanceField(handlerField, AccessFlags.fromValue(AccessFlags.ACC_PRIVATE));

        methodsField = dexBuilder.addField(proxyType,
                dexBuilder.addString("methods"),
                dexBuilder.addType(Method[].class));
        proxyClassBuilder.instanceField(methodsField, AccessFlags.fromValue(AccessFlags.ACC_PRIVATE));
    }

    private void generateConstructor() {
        final DexType objectType = dexBuilder.addType(Object.class);
        final DexType voidType = dexBuilder.addType("V");
        final DexMethod ctr = dexBuilder.addMethod(proxyType,
                dexBuilder.addString("<init>"),
                dexBuilder.addProto(voidType, Arrays.asList(
                        dexBuilder.addType(InvocationHandler.class),
                        dexBuilder.addType(Method[].class))));
        final DexMethod objectCtr = dexBuilder.addMethod(objectType, dexBuilder.addString("<init>"),
                dexBuilder.addProto(voidType, Collections.<DexType>emptyList()));
        proxyClassBuilder.directMethod(ctr, AccessFlags.fromValue(AccessFlags.ACC_PUBLIC, AccessFlags.ACC_CONSTRUCTOR), DexCode.newBuilder()
                .registersSize(3)
                .insSize(3)
                .outsSize(1)
                .instruction(invokeDirect(0, objectCtr))
                .instruction(iputObject(1, 0, handlerField))
                .instruction(iputObject(2, 0, methodsField))
                .instruction(returnVoid())
                .build());
    }

    private void generateMethods() {
        methods = interfaceClass.getMethods();

        for (int i = 0; i < methods.length; i++) {
            final Method method = methods[i];
            generateMethodCode(method, i);
        }
    }

    private boolean isWide(Class<?> classType) {
        return classType == long.class || classType == Long.class || classType == double.class || classType == Double.class;
    }

    private int calculateRegisterSize(int begin, Class<?>[] params) {
        int size = begin + 1;
        for (Class<?> clazz : params) {
            if (isWide(clazz)) {
                size++;
            }
            size++;
        }
        return size;
    }

    private int calculateArgCount(Class<?>[] params){
        int size = 1;
        for (Class<?> clazz : params) {
            if (isWide(clazz)) {
                // long/double count as two
                size++;
            }
            size++;
        }
        return size;
    }

    private void generateMethodCode(final Method method, final int methodIndex) {
        // TODO do it once
        final DexMethod invokeMethod = dexBuilder.addMethod(dexBuilder.addType(InvocationHandler.class),
                dexBuilder.addString("invoke"),
                dexBuilder.addProto(dexBuilder.addType(Object.class), Arrays.asList(
                        dexBuilder.addType(Object.class),
                        dexBuilder.addType(Method.class),
                        dexBuilder.addType(Object[].class)
                )));

        final DexMethod dexMethod = dexBuilder.addMethod(proxyType, dexBuilder.addString(method.getName()), getDexProto(method));

        final int rHandler = 0;
        final int rMethod = 1;
        final int rMethodArray = 2;
        final int rIndex = 3;
        final int rArgArray = 4;
        final int rArrSize = 5;
        final int rBoxedArg = 6;
        final int rThis = 7;
        final int registersSize = calculateRegisterSize(rThis, method.getParameterTypes());
        final int insSize = calculateArgCount(method.getParameterTypes());
        final DexCode.Builder builder = DexCode.newBuilder()
                .registersSize(registersSize)
                .insSize(insSize)
                .outsSize(4)
                .instruction(igetObject(rHandler, rThis, handlerField))
                .instruction(igetObject(rMethodArray, rThis, methodsField))
                .instruction(const16(rIndex, methodIndex))
                .instruction(agetObject(rMethod, rMethodArray, rIndex))
                .instruction(const4(rArrSize, method.getParameterTypes().length))
                .instruction(newArray(rArgArray, rArrSize, dexBuilder.addType(Object[].class)));

        int rArg = rThis + 1;
        for (int i = 0; i < method.getParameterTypes().length; i++, rArg++) {
            final Class<?> argType = method.getParameterTypes()[i];
            builder.instruction(const4(rIndex, i));
            if (argType.isPrimitive()) {
                if (isWide(argType)) {
                    // long/double 占用两个寄存器
                    // http://aospxref.com/android-10.0.0_r47/xref/art/runtime/verifier/method_verifier.cc
                    builder.instruction(invokeStatic(rArg, ++rArg, getValueOfMethod(argType)))
                            .instruction(moveResultObject(rBoxedArg))
                            .instruction(aputObject(rBoxedArg, rArgArray, rIndex));
                } else {
                    builder.instruction(invokeStatic(rArg, getValueOfMethod(argType)))
                            .instruction(moveResultObject(rBoxedArg))
                            .instruction(aputObject(rBoxedArg, rArgArray, rIndex));
                }
            } else {
                builder.instruction(aputObject(rArg, rArgArray, rIndex));
            }
        }

        if (method.getReturnType() == void.class) {
            builder.instruction(invokeInterface(rHandler, rThis, rMethod, rArgArray, invokeMethod))
                    .instruction(returnVoid());
        } else if (method.getReturnType().isPrimitive()) {
            builder.instruction(invokeInterface(rHandler, rThis, rMethod, rArgArray, invokeMethod))
                    .instruction(moveResultObject(rHandler))
                    .instruction(checkCast(rHandler, dexBuilder.addType(getBoxedType(method.getReturnType()))))
                    .instruction(invokeVirtual(rHandler, convertValueOfMethod(method.getReturnType())))
                    .instruction(moveResult(rHandler))
                    .instruction(returnPrimitive(rHandler));
        } else {
            builder.instruction(invokeInterface(rHandler, rThis, rMethod, rArgArray, invokeMethod))
                    .instruction(moveResultObject(rHandler))
                    .instruction(checkCast(rHandler, dexBuilder.addType(method.getReturnType())))
                    .instruction(returnObject(rHandler));
        }

        proxyClassBuilder.virtualMethod(dexMethod, AccessFlags.fromValue(AccessFlags.ACC_PUBLIC), builder.build());
    }

    private Class<?> getBoxedType(final Class<?> primitiveType) {
        if (int.class == primitiveType) {
            return Integer.class;
        } else if (boolean.class == primitiveType) {
            return Boolean.class;
        } else if (float.class == primitiveType) {
            return Float.class;
        } else if (long.class == primitiveType) {
            return Long.class;
        } else if (short.class == primitiveType) {
            return Short.class;
        } else if (double.class == primitiveType) {
            return Double.class;
        } else if (byte.class == primitiveType) {
            return Byte.class;
        } else if (char.class == primitiveType) {
            return Character.class;
        } else {
            throw new UnsupportedOperationException("Unsupported primitive type " + primitiveType);
        }
    }

    /**
     * 将包装类转化为基本类型
     *
     * @param argType primitive type
     * @return the method of cast
     */
    private DexMethod convertValueOfMethod(final Class<?> argType) {
        DexType boxedClass;
        DexType primitiveClass;
        String methodName;
        if (int.class == argType) {
            boxedClass = dexBuilder.addType(Integer.class);
            primitiveClass = dexBuilder.addType(int.class);
            methodName = "intValue";
        } else if (boolean.class == argType) {
            boxedClass = dexBuilder.addType(Boolean.class);
            primitiveClass = dexBuilder.addType(boolean.class);
            methodName = "booleanValue";
        } else if (float.class == argType) {
            boxedClass = dexBuilder.addType(Float.class);
            primitiveClass = dexBuilder.addType(float.class);
            methodName = "floatValue";
        } else if (long.class == argType) {
            boxedClass = dexBuilder.addType(Long.class);
            primitiveClass = dexBuilder.addType(long.class);
            methodName = "longValue";
        } else if (short.class == argType) {
            boxedClass = dexBuilder.addType(Short.class);
            primitiveClass = dexBuilder.addType(short.class);
            methodName = "shortValue";
        } else if (double.class == argType) {
            boxedClass = dexBuilder.addType(Double.class);
            primitiveClass = dexBuilder.addType(double.class);
            methodName = "doubleValue";
        } else if (byte.class == argType) {
            boxedClass = dexBuilder.addType(Byte.class);
            primitiveClass = dexBuilder.addType(byte.class);
            methodName = "byteValueValue";
        } else if (char.class == argType) {
            boxedClass = dexBuilder.addType(Character.class);
            primitiveClass = dexBuilder.addType(char.class);
            methodName = "charValue";
        } else {
            throw new UnsupportedOperationException("Unsupported primitive type " + argType);
        }

        return dexBuilder.addMethod(boxedClass,
                dexBuilder.addString(methodName),
                dexBuilder.addProto(primitiveClass, new ArrayList<DexType>()));
    }

    private DexMethod getValueOfMethod(final Class<?> argType) {
        DexType boxedClass;
        DexType primitiveClass;

        if (int.class == argType) {
            boxedClass = dexBuilder.addType(Integer.class);
            primitiveClass = dexBuilder.addType(int.class);
        } else if (boolean.class == argType) {
            boxedClass = dexBuilder.addType(Boolean.class);
            primitiveClass = dexBuilder.addType(boolean.class);
        } else if (float.class == argType) {
            boxedClass = dexBuilder.addType(Float.class);
            primitiveClass = dexBuilder.addType(float.class);
        } else if (long.class == argType) {
            boxedClass = dexBuilder.addType(Long.class);
            primitiveClass = dexBuilder.addType(long.class);
        } else if (short.class == argType) {
            boxedClass = dexBuilder.addType(Short.class);
            primitiveClass = dexBuilder.addType(short.class);
        } else if (double.class == argType) {
            boxedClass = dexBuilder.addType(Double.class);
            primitiveClass = dexBuilder.addType(double.class);
        } else if (byte.class == argType) {
            boxedClass = dexBuilder.addType(Byte.class);
            primitiveClass = dexBuilder.addType(byte.class);
        } else if (char.class == argType) {
            boxedClass = dexBuilder.addType(Character.class);
            primitiveClass = dexBuilder.addType(char.class);
        } else {
            throw new UnsupportedOperationException("Unsupported primitive type " + argType);
        }
        return dexBuilder.addMethod(boxedClass,
                dexBuilder.addString("valueOf"),
                dexBuilder.addProto(boxedClass,
                        Collections.singletonList(primitiveClass)));
    }

    private DexProto getDexProto(final Method method) {
        final List<DexType> dexArgTypes = new ArrayList<>(method.getParameterTypes().length);
        for (Class<?> argType : method.getParameterTypes()) {
            dexArgTypes.add(dexBuilder.addType(argType));
        }
        final DexType returnDexType = dexBuilder.addType(method.getReturnType());
        return dexBuilder.addProto(returnDexType, dexArgTypes);
    }

    private void writeDex(final Context context) {
        try {
            DexWriter.write(dexBuilder.build(), getProxyDexFile(context));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private T createProxyInstance(final Context context) {
        final DexClassLoader classLoader = new DexClassLoader(getProxyDexFile(context).getAbsolutePath(),
                getCodeCacheDir(context).getAbsolutePath(),
                null,
                ProxyBuilder.class.getClassLoader());
        try {
            Class<?> proxyClass = classLoader.loadClass(getProxyClassName());
            Object proxy = proxyClass.getConstructor(InvocationHandler.class, Method[].class)
                    .newInstance(invocationHandler, methods);
            return interfaceClass.cast(proxy);
        } catch (ClassNotFoundException
                | NoSuchMethodException
                | IllegalAccessException
                | InstantiationException
                | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private String getProxyClassName() {
        final String packageName = interfaceClass.getPackage() == null
                ? ""
                : interfaceClass.getPackage().getName() + ".";
        return packageName + PROXY_CLASS_NAME_PREFIX + interfaceClass.getSimpleName();
    }

    private String getProxyTypeDescription() {
        final String packageName = interfaceClass.getPackage() == null
                ? ""
                : interfaceClass.getPackage().getName().replace('.', '/') + "/";
        final String proxyName = PROXY_CLASS_NAME_PREFIX + interfaceClass.getSimpleName() + ";";
        return "L" + packageName + proxyName;
    }

    private File getProxyDexFile(final Context context) {
        File proxyDir = new File(getCodeCacheDir(context), "proxies");
        return new File(proxyDir, interfaceClass.getName() + ".generated.dex");
    }

    private static File getCodeCacheDir(final Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return context.getCodeCacheDir();
        }
        return new File(context.getFilesDir(), "code_cache");
    }
}
