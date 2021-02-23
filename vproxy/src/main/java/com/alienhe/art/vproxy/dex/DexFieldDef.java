package com.alienhe.art.vproxy.dex;

/**
 * @author alienhe
 */
public class DexFieldDef {

    public final DexField field;

    public final AccessFlags accessFlags;

    DexFieldDef(final DexField field, final AccessFlags accessFlags) {
        this.field = field;
        this.accessFlags = accessFlags;
    }
}
