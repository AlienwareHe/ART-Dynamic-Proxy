package com.alienhe.art.vproxy.dex.writer;

import java.util.Objects;

/**
 * @author alienhe
 */
class DexMapEntry {

    EntryType type;

    int count;

    int offset;

    DexMapEntry(final EntryType type, final int count, final int offset) {
        this.type = type;
        this.count = count;
        this.offset = offset;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DexMapEntry entry = (DexMapEntry) o;
        return type == entry.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }
}