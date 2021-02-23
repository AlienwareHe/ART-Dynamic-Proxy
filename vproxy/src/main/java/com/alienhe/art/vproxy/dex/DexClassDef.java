package com.alienhe.art.vproxy.dex;

import java.util.ArrayList;
import java.util.List;

/**
 * @author alienhe
 */
public class DexClassDef {

    public final DexType type;

    public final AccessFlags accessFlags;

    public final DexType superClass;

    public final List<DexType> interfaces;

    // TODO support annotations_off

    // TODO support static_values_off

    public final List<DexFieldDef> instanceFields;

    public final List<DexMethodDef> directMethods;

    public final List<DexMethodDef> virtualMethods;

    private DexClassDef(final Builder builder) {
        type = builder.type;
        accessFlags = builder.accessFlags;
        superClass = builder.superClass;
        interfaces = builder.interfaces;
        instanceFields = builder.instanceFields;
        directMethods = builder.directMethods;
        virtualMethods = builder.virtualMethods;
    }

    public static final class Builder {
        private DexType type;
        private AccessFlags accessFlags;
        private DexType superClass;
        private final List<DexType> interfaces = new ArrayList<>();
        private final List<DexFieldDef> instanceFields = new ArrayList<>();
        private final List<DexMethodDef> directMethods = new ArrayList<>();
        private final List<DexMethodDef> virtualMethods = new ArrayList<>();
        private final DexBuilder dexBuilder;

        Builder(final DexBuilder dexBuilder) {
            this.dexBuilder = dexBuilder;
        }

        public Builder type(final DexType type) {
            this.type = type;
            return this;
        }

        public Builder accessFlags(final AccessFlags accessFlags) {
            this.accessFlags = accessFlags;
            return this;
        }

        public Builder superClass(final DexType superClass) {
            this.superClass = superClass;
            return this;
        }

        public Builder implementedInterface(final DexType implementedInterface) {
            this.interfaces.add(implementedInterface);
            return this;
        }

        public Builder instanceField(final DexField field, final AccessFlags accessFlags) {
            instanceFields.add(new DexFieldDef(field, accessFlags));
            return this;
        }

        public Builder directMethod(final DexMethod method, final AccessFlags accessFlags, final DexCode code) {
            directMethods.add(new DexMethodDef(method, accessFlags, code));
            return this;
        }

        public Builder virtualMethod(final DexMethod method, final AccessFlags accessFlags, final DexCode code) {
            virtualMethods.add(new DexMethodDef(method, accessFlags, code));
            return this;
        }

        public DexClassDef build() {
            return dexBuilder.addClassInternal(new DexClassDef(this));
        }
    }


}

