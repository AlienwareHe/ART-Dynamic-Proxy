package com.alienhe.art.vproxy.dex;

/**
 * @author alienhe
 */
public class DexMethod extends DexItem {

    public final DexType definer;
    public final DexString name;
    public final DexProto proto;

    DexMethod(final DexType definer, final DexString name, final DexProto proto) {
        this.definer = definer;
        this.name = name;
        this.proto = proto;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final DexMethod dexMethod = (DexMethod) o;

        if (!definer.equals(dexMethod.definer)) return false;
        if (!name.equals(dexMethod.name)) return false;
        return proto.equals(dexMethod.proto);
    }

    @Override
    public int hashCode() {
        int result = definer.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + proto.hashCode();
        return result;
    }
}