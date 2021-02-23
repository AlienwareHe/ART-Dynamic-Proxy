package com.alienhe.art.vproxy.dex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author alienhe
 */
public class DexBuilder {

    private final int version;

    private final Map<String, DexString> stringMap = new HashMap<>();

    private final Map<String, DexType> typeMap = new HashMap<>();

    private final Map<ProtoKey, DexProto> protoMap = new HashMap<>();

    private final Map<FieldKey, DexField> fieldMap = new HashMap<>();

    private final Map<MethodKey, DexMethod> methodMap = new HashMap<>();

    private final List<DexClassDef> classDefList = new ArrayList<>();

    public DexBuilder(final int version) {
        this.version = version;
    }

    public DexString addString(final String value) {
        DexString string = stringMap.get(value);
        if (string == null) {
            string = new DexString(value);
            stringMap.put(value, string);
        }
        return string;
    }

    public DexType addType(final Class<?> clazz) {
        return addType(getTypeDescription(clazz));
    }

    public DexType addType(final String typeDescription) {
        DexType dexType = typeMap.get(typeDescription);
        if (dexType == null) {
            dexType = new DexType(addString(typeDescription));
            typeMap.put(typeDescription, dexType);
        }
        return dexType;
    }

    public DexProto addProto(final DexType returnType, final List<DexType> argumentTypes) {
        final ProtoKey key = new ProtoKey(returnType, argumentTypes);
        DexProto proto = protoMap.get(key);
        if (proto == null) {
            StringBuilder shorty = new StringBuilder(getShortyTypeDescription(returnType));
            final List<DexType> dexArgumentTypes = new ArrayList<>(argumentTypes.size());
            for (DexType argType : argumentTypes) {
                dexArgumentTypes.add(argType);
                shorty.append(getShortyTypeDescription(argType));
            }
            proto = new DexProto(addString(shorty.toString()), returnType, dexArgumentTypes);
            protoMap.put(key, proto);
        }
        return proto;
    }

    public DexField addField(final DexType definer, final DexString name, final DexType type) {
        final FieldKey key = new FieldKey(definer, name, type);
        DexField field = fieldMap.get(key);
        if (field == null) {
            field = new DexField(definer, type, name);
            fieldMap.put(key, field);
        }
        return field;
    }

    public DexMethod addMethod(final DexType definer, final DexString name, final DexProto proto) {
        final MethodKey key = new MethodKey(definer, name, proto);
        DexMethod method = methodMap.get(key);
        if (method == null) {
            method = new DexMethod(definer, name, proto);
            methodMap.put(key, method);
        }
        return method;
    }

    public DexClassDef.Builder addClass() {
        return new DexClassDef.Builder(this);
    }

    public Dex build() {
        // order of sorting matters because order of types depends on order of strings and so on
        final List<DexString> strings = SortingHelper.sortString(stringMap.values());
        final List<DexType> types = SortingHelper.sortTypes(typeMap.values());
        final List<DexProto> protos = SortingHelper.sortProtos(protoMap.values());
        final List<DexField> fields = SortingHelper.sortFields(fieldMap.values());
        final List<DexMethod> methods = SortingHelper.sortMethods(methodMap.values());
        return Dex.newBuilder()
                .version(version)
                .strings(strings)
                .types(types)
                .protos(protos)
                .fields(fields)
                .methods(methods)
                .classDefs(classDefList)
                .build();
    }

    DexClassDef addClassInternal(final DexClassDef dexClassDef) {
        classDefList.add(dexClassDef);
        return dexClassDef;
    }

    private static String getTypeDescription(final Class<?> type) {
        if (void.class == type) {
            return "V";
        }
        if (boolean.class == type) {
            return "Z";
        }
        if (byte.class == type) {
            return "Z";
        }
        if (short.class == type) {
            return "S";
        }
        if (char.class == type) {
            return "C";
        }
        if (int.class == type) {
            return "I";
        }
        if (long.class == type) {
            return "J";
        }
        if (float.class == type) {
            return "F";
        }
        if (double.class == type) {
            return "D";
        }
        if (type.isArray()) {
            return "[" + getTypeDescription(type.getComponentType());
        }
        return "L" + type.getName().replace('.', '/') + ";";
    }

    private static String getShortyTypeDescription(final DexType type) {
        String value = type.descriptor.value;
        if (value.startsWith("L") || value.startsWith("[")) {
            return "L";
        }
        return value;
    }

    private static class ProtoKey {
        final DexType returnType;

        final List<DexType> argumentTypes;

        ProtoKey(final DexType returnType, final List<DexType> argumentTypes) {
            this.returnType = returnType;
            this.argumentTypes = argumentTypes;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final ProtoKey key = (ProtoKey) o;
            return Objects.equals(returnType, key.returnType) &&
                    Objects.equals(argumentTypes, key.argumentTypes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(returnType, argumentTypes);
        }
    }

    private static class FieldKey {
        final DexType definer;
        final DexString name;
        final DexType type;

        FieldKey(final DexType definer, final DexString name, final DexType type) {
            this.definer = definer;
            this.name = name;
            this.type = type;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final FieldKey fieldKey = (FieldKey) o;
            return Objects.equals(definer, fieldKey.definer) &&
                    Objects.equals(name, fieldKey.name) &&
                    Objects.equals(type, fieldKey.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(definer, name, type);
        }
    }

    private static class MethodKey {
        final DexType definer;
        final DexString name;
        final DexProto proto;

        MethodKey(final DexType definer, final DexString name, final DexProto proto) {
            this.definer = definer;
            this.name = name;
            this.proto = proto;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final MethodKey key = (MethodKey) o;
            return Objects.equals(definer, key.definer) &&
                    Objects.equals(name, key.name) &&
                    Objects.equals(proto, key.proto);
        }

        @Override
        public int hashCode() {
            return Objects.hash(definer, name, proto);
        }
    }
}
