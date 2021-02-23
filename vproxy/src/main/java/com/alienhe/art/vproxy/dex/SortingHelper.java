package com.alienhe.art.vproxy.dex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author alienhe
 */
class SortingHelper {

    static List<DexString> sortString(final Collection<DexString> strings) {
        return fillIndices(sort(strings, new Comparator<DexString>() {
            @Override
            public int compare(final DexString o1, final DexString o2) {
                return o1.value.compareTo(o2.value);
            }
        }));
    }

    static List<DexType> sortTypes(final Collection<DexType> types) {
        return fillIndices(sort(types, new Comparator<DexType>() {
            @Override
            public int compare(final DexType t1, final DexType t2) {
                return Integer.compare(t1.descriptor.index, t2.descriptor.index);
            }
        }));
    }

    static List<DexProto> sortProtos(final Collection<DexProto> protos) {
        return fillIndices(sort(protos, new Comparator<DexProto>() {
            @Override
            public int compare(final DexProto p1, final DexProto p2) {
                int majorOrder = Integer.compare(p1.returnType.index, p2.returnType.index);
                if (majorOrder != 0) {
                    return majorOrder;
                }
                int n = Math.min(p1.argumentTypes.size(), p2.argumentTypes.size());
                for (int i = 0; i < n; i++) {
                    int cmp = Integer.compare(p1.argumentTypes.get(i).index, p2.argumentTypes.get(i).index);
                    if (cmp != 0) {
                        return cmp;
                    }
                }
                return Integer.compare(p1.argumentTypes.size(), p2.argumentTypes.size());
            }
        }));
    }

    static List<DexField> sortFields(final Collection<DexField> fields) {
        return fillIndices(sort(fields, new Comparator<DexField>() {
            @Override
            public int compare(final DexField f1, final DexField f2) {
                int majorOrder = Integer.compare(f1.definer.index, f2.definer.index);
                if (majorOrder != 0) {
                    return majorOrder;
                }
                int intermediateOrder = Integer.compare(f1.name.index, f2.name.index);
                if (intermediateOrder != 0) {
                    return intermediateOrder;
                }
                return Integer.compare(f1.type.index, f2.type.index);
            }
        }));
    }

    static List<DexMethod> sortMethods(final Collection<DexMethod> methods) {
        return fillIndices(sort(methods, new Comparator<DexMethod>() {
            @Override
            public int compare(final DexMethod m1, final DexMethod m2) {
                int majorOrder = Integer.compare(m1.definer.index, m2.definer.index);
                if (majorOrder != 0) {
                    return majorOrder;
                }
                int intermediateOrder = Integer.compare(m1.name.index, m2.name.index);
                if (intermediateOrder != 0) {
                    return intermediateOrder;
                }
                return Integer.compare(m1.proto.index, m2.proto.index);
            }
        }));
    }

    private static <T extends DexItem> List<T> fillIndices(final List<T> items) {
        for (int i = 0; i < items.size(); i++) {
            items.get(i).index = i;
        }
        return items;
    }

    private static <T extends DexItem> List<T> sort(final Collection<T> items,
                                                    final Comparator<T> comparator) {
        final List<T> sorted = new ArrayList<>(items);
        Collections.sort(sorted, comparator);
        return sorted;
    }
}

