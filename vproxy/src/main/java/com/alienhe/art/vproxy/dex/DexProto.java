package com.alienhe.art.vproxy.dex;

import java.util.List;
import java.util.Objects;

/**
 * @author alienhe
 */
public class DexProto extends DexItem {

    public final DexString shorty;

    public final DexType returnType;

    public final List<DexType> argumentTypes;

    DexProto(final DexString shorty, final DexType returnType, final List<DexType> argumentTypes) {
        this.shorty = shorty;
        this.returnType = returnType;
        this.argumentTypes = argumentTypes;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DexProto proto = (DexProto) o;
        return Objects.equals(shorty, proto.shorty) &&
                Objects.equals(returnType, proto.returnType) &&
                Objects.equals(argumentTypes, proto.argumentTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shorty, returnType, argumentTypes);
    }
}