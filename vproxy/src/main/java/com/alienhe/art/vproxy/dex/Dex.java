package com.alienhe.art.vproxy.dex;

import java.util.List;

/**
 * @author alienhe
 */
public class Dex {

    public final int version;
    public final List<DexString> strings;
    public final List<DexType> types;
    public final List<DexProto> protos;
    public final List<DexField> fields;
    public final List<DexMethod> methods;
    public final List<DexClassDef> classDefs;

    private Dex(final Builder builder) {
        version = builder.version;
        strings = builder.strings;
        types = builder.types;
        protos = builder.protos;
        fields = builder.fields;
        methods = builder.methods;
        classDefs = builder.classDefs;
    }

    static Builder newBuilder() {
        return new Builder();
    }

    static final class Builder {
        private int version;
        private List<DexString> strings;
        private List<DexType> types;
        private List<DexProto> protos;
        private List<DexField> fields;
        private List<DexMethod> methods;
        private List<DexClassDef> classDefs;

        private Builder() {
        }

        Builder version(final int version) {
            this.version = version;
            return this;
        }

        Builder strings(final List<DexString> strings) {
            this.strings = strings;
            return this;
        }

        Builder types(final List<DexType> types) {
            this.types = types;
            return this;
        }

        Builder protos(final List<DexProto> protos) {
            this.protos = protos;
            return this;
        }

        Builder fields(final List<DexField> fields) {
            this.fields = fields;
            return this;
        }

        Builder methods(final List<DexMethod> methods) {
            this.methods = methods;
            return this;
        }

        Builder classDefs(final List<DexClassDef> classDefs) {
            this.classDefs = classDefs;
            return this;
        }

        Dex build() {
            return new Dex(this);
        }
    }
}
