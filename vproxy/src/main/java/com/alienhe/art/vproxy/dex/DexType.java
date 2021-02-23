package com.alienhe.art.vproxy.dex;

import java.util.Objects;

/**
 * @author alienhe
 */
public class DexType extends DexItem {

    public final DexString descriptor;

    DexType(final DexString descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DexType type = (DexType) o;
        return Objects.equals(descriptor, type.descriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(descriptor);
    }
}