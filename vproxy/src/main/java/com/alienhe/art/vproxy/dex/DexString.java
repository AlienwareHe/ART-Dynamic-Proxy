package com.alienhe.art.vproxy.dex;

import java.util.Objects;

/**
 * @author alienhe
 */
public class DexString extends DexItem {

    public final String value;

    DexString(final String value) {
        this.value = value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DexString string = (DexString) o;
        return Objects.equals(value, string.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}