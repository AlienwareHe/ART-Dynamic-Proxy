package com.alienhe.art.vproxy.dex.writer;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author alienhe
 */
class DexMap {

    private final Map<EntryType, DexMapEntry> map = new HashMap<>();

    void put(final EntryType entryType, final int count, final int offset) {
        DexMapEntry entry = map.get(entryType);
        if (entry == null) {
            entry = new DexMapEntry(entryType, count, offset);
            map.put(entryType, entry);
        } else {
            entry.offset = Math.min(entry.offset, offset);
            entry.count += count;
        }
    }

    List<DexMapEntry> geSortedEntries() {
        final List<DexMapEntry> entries = new ArrayList<>(map.values());
        Collections.sort(entries, new Comparator<DexMapEntry>() {
            @Override
            public int compare(final DexMapEntry e1, final DexMapEntry e2) {
                return Integer.compare(e1.offset, e2.offset);
            }
        });
        return entries;
    }
}