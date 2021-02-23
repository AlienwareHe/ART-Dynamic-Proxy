package com.alienhe.art.vproxy.dex.writer;

import com.alienhe.art.vproxy.dex.Dex;
import com.alienhe.art.vproxy.dex.DexClassDef;
import com.alienhe.art.vproxy.dex.DexCode;
import com.alienhe.art.vproxy.dex.DexField;
import com.alienhe.art.vproxy.dex.DexFieldDef;
import com.alienhe.art.vproxy.dex.DexMethod;
import com.alienhe.art.vproxy.dex.DexMethodDef;
import com.alienhe.art.vproxy.dex.DexProto;
import com.alienhe.art.vproxy.dex.DexString;
import com.alienhe.art.vproxy.dex.DexType;
import com.alienhe.art.vproxy.instruction.DexInstruction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.zip.Adler32;

/**
 * @author alienhe
 */
public class DexWriter {

    private static final int NO_INDEX = 0xffffffff;

    private static final int ENDIAN_CONSTANT = 0x12345678;

    private int stringsOffset;

    private int typesOffset;

    private int protosOffset;

    private int fieldsOffset;

    private int methodsOffset;

    private int classDefsOffset;

    private int dataOffset;

    private int dataSize;

    private int mapOffset;

    private final DexMap map = new DexMap();

    private DexWriter() {
    }

    public static void write(final Dex dex, final File file) throws IOException {
        new DexWriter().writeInternal(dex, file);
    }

    private void writeInternal(final Dex dex, final File file) throws IOException {
        prepareFile(file);

        try (OutputStream os = new FileOutputStream(file)) {
            try (DexOutputStream stream = new DexOutputStream()) {
                writeFakeHeader(stream);
                writeFakeIdsSection(dex, stream);

                // DATA SECTION
                dataOffset = stream.getPosition();
                int[] stringOffsets = writeStringsData(dex.strings, stream);
                int[] protosOffsets = writeProtoArgumentsData(dex.protos, stream);
                int[] interfaceOffset = writeClassInterfaces(dex.classDefs, stream);
                CodeItemOffsets[] codeItemOffsets = writeClassCodeItems(dex.classDefs, stream);
                int[] classDataOffsets = writeClassDefsData(dex.classDefs, codeItemOffsets, stream);
                mapOffset = stream.getPosition();

                // IDS SECTION
                stream.setPosition(0x70);
                stringsOffset = writeStringsIds(stringOffsets, stream);
                typesOffset = writeTypesIds(dex.types, stream);
                protosOffset = writeProtosIds(dex.protos, protosOffsets, stream);
                fieldsOffset = writeFieldsIds(dex.fields, stream);
                methodsOffset = writeMethodsIds(dex.methods, stream);
                classDefsOffset = writeClassDefs(dex.classDefs, classDataOffsets, interfaceOffset, stream);

                stream.setPosition(mapOffset);
                mapOffset = writeMap(stream);
                dataSize = stream.getPosition() - dataOffset;

                stream.setPosition(0x0);
                writeHeader(dex, stream);

                os.write(stream.toByteArray());
            }
            os.flush();
        }
    }

    private void prepareFile(final File file) throws IOException {
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }

        final File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                throw new IOException("Cannot create parent for file " + file);
            }
        }
    }

    private void writeFakeHeader(final DexOutputStream file) {
        map.put(EntryType.TYPE_HEADER_ITEM, 1, 0);
        file.write(new byte[0x70]);
    }

    private void writeFakeIdsSection(final Dex dex, final DexOutputStream file) {
        int size = dex.strings.size() * 4
                + dex.types.size() * 4
                + dex.protos.size() * 12
                + dex.fields.size() * 8
                + dex.methods.size() * 8
                + dex.classDefs.size() * 32;
        file.write(new byte[size]);
    }

    private void writeHeader(final Dex dex, final DexOutputStream file) {
        file.write(new byte[]{0x64, 0x65, 0x78, 0x0a, 0x30, 0x33, (byte) dex.version, 0x00});

        final int checksumOffset = file.getPosition();
        file.writeInt(0); // checksum
        final int signatureOffset = file.getPosition();
        file.write(new byte[20]); // signature
        file.writeInt(file.getSize()); // signature
        file.writeInt(0x70); // head size
        file.writeInt(ENDIAN_CONSTANT); // endian_tag
        file.writeInt(0); // link_size
        file.writeInt(0); // link_off
        file.writeInt(mapOffset); // map_off
        file.writeInt(dex.strings.size()); // string_ids_size
        file.writeInt(stringsOffset); // string_ids_off
        file.writeInt(dex.types.size()); // type_ids_size
        file.writeInt(typesOffset); // type_ids_off
        file.writeInt(dex.protos.size()); // proto_ids_size
        file.writeInt(protosOffset); // proto_ids_off
        file.writeInt(dex.fields.size()); // field_ids_size
        file.writeInt(fieldsOffset); // field_ids_off
        file.writeInt(dex.methods.size()); // method_ids_size
        file.writeInt(methodsOffset); // method_ids_off
        file.writeInt(dex.classDefs.size()); // class_defs_size
        file.writeInt(classDefsOffset); // class_defs_off
        file.writeInt(dataSize); // data_size
        file.writeInt(dataOffset); // data_off
        final int endOffset = file.getPosition();

        byte[] content = file.toByteArray();
        file.setPosition(signatureOffset);
        file.write(toSHA1(content, 16, content.length - 16));

        content = file.toByteArray();
        final Adler32 checksum = new Adler32();
        checksum.update(content, 12, content.length - 12);
        file.setPosition(checksumOffset);
        file.writeInt((int) checksum.getValue());

        file.setPosition(endOffset);
    }

    private static byte[] toSHA1(byte[] data, int start, int length) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(data, start, length);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private int[] writeStringsData(final List<DexString> strings, final DexOutputStream stream) throws IOException {
        final int[] offsets = new int[strings.size()];
        map.put(EntryType.TYPE_STRING_DATA_ITEM, strings.size(), stream.getPosition());

        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = stream.getPosition();
            final String string = strings.get(i).value;
            stream.writeUleb128(string.length());
            stream.write(Mutf8.encode(string));
            stream.writeByte(0);
        }
        return offsets;
    }

    private int writeStringsIds(final int[] offsets, final DexOutputStream stream) {
        final int stringsOffset = stream.getPosition();
        map.put(EntryType.TYPE_STRING_ID_ITEM, offsets.length, stringsOffset);

        for (final int offset : offsets) {
            stream.writeInt(offset);
        }

        return stringsOffset;
    }

    private int writeTypesIds(final List<DexType> types, final DexOutputStream stream) {
        final int typesOffset = stream.getPosition();
        map.put(EntryType.TYPE_TYPE_ID_ITEM, types.size(), typesOffset);

        for (DexType type : types) {
            stream.writeInt(type.descriptor.index);
        }

        return typesOffset;
    }

    private int writeTypeListData(final List<DexType> typeList, final DexOutputStream stream) {
        int offset = align4Bytes(stream);

        map.put(EntryType.TYPE_TYPE_LIST, 1, offset);

        stream.writeInt(typeList.size());
        for (DexType argType : typeList) {
            stream.writeShort(argType.index);
        }

        return offset;
    }

    private int[] writeProtoArgumentsData(final List<DexProto> protos, final DexOutputStream stream) {
        final int[] offsets = new int[protos.size()];
        for (int i = 0; i < protos.size(); i++) {
            final DexProto proto = protos.get(i);
            if (proto.argumentTypes.isEmpty()) {
                offsets[i] = 0;
            } else {
                offsets[i] = writeTypeListData(proto.argumentTypes, stream);
            }
        }
        return offsets;
    }

    private int writeProtosIds(final List<DexProto> protos, final int[] offsets, final DexOutputStream stream) {
        final int protosOffset = stream.getPosition();
        map.put(EntryType.TYPE_PROTO_ID_ITEM, protos.size(), protosOffset);

        for (int i = 0; i < protos.size(); i++) {
            final DexProto proto = protos.get(i);
            stream.writeInt(proto.shorty.index);
            stream.writeInt(proto.returnType.index);
            stream.writeInt(offsets[i]);
        }

        return protosOffset;
    }

    private int writeFieldsIds(final List<DexField> fields, final DexOutputStream stream) {
        if (fields.isEmpty()) {
            return 0;
        }
        final int fieldOffset = stream.getPosition();
        map.put(EntryType.TYPE_FIELD_ID_ITEM, fields.size(), fieldOffset);

        for (DexField field : fields) {
            stream.writeShort(field.definer.index);
            stream.writeShort(field.type.index);
            stream.writeInt(field.name.index);
        }

        return fieldOffset;
    }

    private int writeMethodsIds(final List<DexMethod> methods, final DexOutputStream stream) {
        final int methodsOffset = stream.getPosition();
        map.put(EntryType.TYPE_METHOD_ID_ITEM, methods.size(), methodsOffset);

        for (DexMethod method : methods) {
            stream.writeShort(method.definer.index);
            stream.writeShort(method.proto.index);
            stream.writeInt(method.name.index);
        }

        return methodsOffset;
    }

    private int[] writeClassInterfaces(final List<DexClassDef> classDefs, final DexOutputStream stream) {
        final int[] interfaceOffsets = new int[classDefs.size()];
        for (int i = 0; i < classDefs.size(); i++) {
            final DexClassDef classDef = classDefs.get(i);
            if (classDef.interfaces.isEmpty()) {
                interfaceOffsets[i] = 0;
            } else {
                interfaceOffsets[i] = writeTypeListData(classDef.interfaces, stream);
            }
        }
        return interfaceOffsets;
    }

    private CodeItemOffsets[] writeClassCodeItems(final List<DexClassDef> classDefs, final DexOutputStream stream) {
        final CodeItemOffsets[] codeItemOffsets = new CodeItemOffsets[classDefs.size()];

        for (int i = 0; i < codeItemOffsets.length; i++) {
            final DexClassDef classDef = classDefs.get(i);
            codeItemOffsets[i] = new CodeItemOffsets(classDef);

            for (int j = 0; j < classDef.directMethods.size(); j++) {
                final DexMethodDef method = classDef.directMethods.get(j);
                final int offset = writeCodeItem(method.code, stream);
                codeItemOffsets[i].directMethodCodeOffsets[j] = offset;
                map.put(EntryType.TYPE_CODE_ITEM, 1, offset);
            }

            for (int j = 0; j < classDef.virtualMethods.size(); j++) {
                final DexMethodDef method = classDef.virtualMethods.get(j);
                final int offset = writeCodeItem(method.code, stream);
                codeItemOffsets[i].virtualMethodCodeOffsets[j] = offset;
                map.put(EntryType.TYPE_CODE_ITEM, 1, offset);
            }
        }

        return codeItemOffsets;
    }

    private int[] writeClassDefsData(final List<DexClassDef> classDefs, final CodeItemOffsets[] codeItemOffsets, final DexOutputStream stream) {
        final int[] classDataOffsets = new int[classDefs.size()];
        map.put(EntryType.TYPE_CLASS_DATA_ITEM, classDefs.size(), stream.getPosition());
        for (int i = 0; i < classDataOffsets.length; i++) {
            classDataOffsets[i] = stream.getPosition();
            final DexClassDef classDef = classDefs.get(i);
            stream.writeUleb128(0); // TODO static_fields_size
            stream.writeUleb128(classDef.instanceFields.size()); // instance_fields_size
            stream.writeUleb128(classDef.directMethods.size()); // direct_methods_size
            stream.writeUleb128(classDef.virtualMethods.size()); // virtual_methods_size

            writeClassFields(classDef.instanceFields, stream);
            writeClassMethods(classDef.directMethods, codeItemOffsets[i].directMethodCodeOffsets, stream);
            writeClassMethods(classDef.virtualMethods, codeItemOffsets[i].virtualMethodCodeOffsets, stream);
        }

        return classDataOffsets;
    }

    private int writeCodeItem(final DexCode code, final DexOutputStream stream) {
        int offset = align4Bytes(stream);

        stream.writeShort(code.registersSize);
        stream.writeShort(code.insSize);
        stream.writeShort(code.outsSize);
        stream.writeShort(0); // tries_size
        stream.writeInt(0); // debug_info_off

        int count = 0;
        for (DexInstruction instruction : code.instructions) {
            count += instruction.getByteCode().length;
        }

        stream.writeInt(count);
        for (DexInstruction instruction : code.instructions) {
            for (int s : instruction.getByteCode()) {
                stream.writeShort(s);
            }
        }

        return offset;
    }

    private void writeClassFields(final List<DexFieldDef> fields, final DexOutputStream stream) {
        final ItemIndex index = new ItemIndex();
        for (DexFieldDef field : fields) {
            stream.writeUleb128(index.next(field.field.index));
            stream.writeUleb128(field.accessFlags.value);
        }
    }

    private void writeClassMethods(final List<DexMethodDef> methods, final int[] codeOffsets,
                                   final DexOutputStream stream) {
        final ItemIndex index = new ItemIndex();
        for (int i = 0; i < methods.size(); i++) {
            final DexMethodDef method = methods.get(i);
            stream.writeUleb128(index.next(method.method.index));
            stream.writeUleb128(method.accessFlags.value);
            stream.writeUleb128(codeOffsets[i]);
        }
    }

    private int writeClassDefs(final List<DexClassDef> classDefs,
                               final int[] classDataOffsets,
                               final int[] interfaceOffsets,
                               final DexOutputStream stream) {
        final int classDefsOffset = stream.getPosition();
        map.put(EntryType.TYPE_CLASS_DEF_ITEM, classDefs.size(), classDefsOffset);

        for (int i = 0; i < classDefs.size(); i++) {
            final DexClassDef classDef = classDefs.get(i);
            stream.writeInt(classDef.type.index);
            stream.writeInt(classDef.accessFlags.value);
            stream.writeInt(classDef.superClass.index);
            stream.writeInt(interfaceOffsets[i]); // interfaces_off
            stream.writeInt(NO_INDEX); // source_file_idx
            stream.writeInt(0); // annotations_off
            stream.writeInt(classDataOffsets[i]); // class_data_off
            stream.writeInt(0); // static_values_off
        }

        return classDefsOffset;
    }

    private int writeMap(final DexOutputStream stream) {
        int offset = align4Bytes(stream);

        map.put(EntryType.TYPE_MAP_LIST, 1, offset);
        final List<DexMapEntry> entries = map.geSortedEntries();

        stream.writeInt(entries.size());
        for (DexMapEntry entry : entries) {
            stream.writeShort(entry.type.value);
            stream.writeShort(0); // unused
            stream.writeInt(entry.count);
            stream.writeInt(entry.offset);
        }

        return offset;
    }

    private int align4Bytes(final DexOutputStream stream) {
        int offset = stream.getPosition();
        final int byteToSkip = (offset % 4 == 0) ? 0 : 4 - offset % 4;
        for (int i = 0; i < byteToSkip; i++) {
            stream.writeByte(0); // padding
            offset++;
        }
        return offset;
    }

    private static class CodeItemOffsets {

        private final int[] directMethodCodeOffsets;
        private final int[] virtualMethodCodeOffsets;

        CodeItemOffsets(final DexClassDef classDef) {
            directMethodCodeOffsets = new int[classDef.directMethods.size()];
            virtualMethodCodeOffsets = new int[classDef.virtualMethods.size()];
        }
    }

    private static class ItemIndex {
        int prevIndex;

        int next(int index) {
            final int diff = index - prevIndex;
            prevIndex = index;
            return diff;
        }
    }
}
