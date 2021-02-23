package com.alienhe.art.vproxy.dex;

/**
 * @author alienhe
 */
public class DexMethodDef {

    public final DexMethod method;

    public final AccessFlags accessFlags;

    public final DexCode code;

    DexMethodDef(final DexMethod method, final AccessFlags accessFlags, final DexCode code) {
        this.method = method;
        this.accessFlags = accessFlags;
        this.code = code;
    }
}