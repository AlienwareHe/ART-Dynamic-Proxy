package com.alienhe.art.vproxy.dex;

/**
 * @author alienhe
 */
public class DexField extends DexItem {

    public final DexType definer;

    public final DexType type;

    public final DexString name;

    DexField(final DexType definer, final DexType type, final DexString name) {
        this.definer = definer;
        this.type = type;
        this.name = name;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final DexField dexField = (DexField) o;

        if (!definer.equals(dexField.definer)) return false;
        if (!type.equals(dexField.type)) return false;
        return name.equals(dexField.name);
    }

    @Override
    public int hashCode() {
        int result = definer.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }
}
