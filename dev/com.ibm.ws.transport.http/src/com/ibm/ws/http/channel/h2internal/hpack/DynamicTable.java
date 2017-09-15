/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal.hpack;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * 2.3.2 - Consists of a list of header fields maintained in
 * first-in, first-out order. The first and newest entry is
 * at the lowest index, and the oldest entry is at the
 * highest index.
 *
 * The table is initially empty. Entries are added as each
 * header block is decompressed. It can contain duplicate
 * entries.
 *
 * Encoder decides how to update the dynamic table and as
 * such can control how much memory is used by the dynamic
 * table. To limit the memory requirements of the decoder,
 * dynamic table size is strictly bounded.
 *
 * Decoder updates the dynamic table during processing of
 * a list of header field representations.
 *
 */
public class DynamicTable {

    //private static DynamicTableEntry[] entries;

    private final LinkedList<H2HeaderField> entries = new LinkedList<H2HeaderField>();

    private int tableAddressSpace = HpackConstants.INITIAL_SETTINGS_HEADER_TABLE_SIZE;
    private int freeAddressSpace = tableAddressSpace;
    //tracks state of the DynamicTable. Upon a decompression error, this flag is set to false
    private boolean isValid = true;

    public DynamicTable() {

    }

    public DynamicTable(int size) {
        this.tableAddressSpace = size;
        this.freeAddressSpace = tableAddressSpace;
    }

    public void addDynamicEntry(H2HeaderField entry) {
        int entrySize = entry.getSize();
        /*
         * If newEntry is <= available address space, add it as the
         * is no conflicts.
         */
        if (entrySize <= freeAddressSpace) {
            this.entries.addFirst(entry);
            freeAddressSpace -= entrySize; // update free address space.
        }

        else {
            /*
             * New entry will result in the table size being larger than the
             * maximum size. First verify if the entry size is larger than
             * the table max size. If true, this causes the table to be
             * emptied of all existing values and results in an empty
             * table.
             */
            if (entrySize > tableAddressSpace) {
                clearDynamicTable();
            } else {
                /*
                 * If entry would fit on table if there was enough address
                 * space for it. Evict entries until there is enough
                 * free space to fit the new entry.
                 */
                while (entrySize > freeAddressSpace) {
                    evictDynamicEntry();
                }
                this.entries.addFirst(entry);
                freeAddressSpace -= entrySize; // update free address space.
            }

        }

    }

    private void evictDynamicEntry() {
        // Dequeue last element of the list and update the free
        // address space that it was occupying.
        freeAddressSpace += entries.getLast().getSize();
        entries.removeLast();
    }

    public void updateDynamicTableSize(int size) {

        /* If new size is 0, clear the table */
        if (size == 0) {
            this.tableAddressSpace = size;
            clearDynamicTable();
            return;
        }

        /*
         * If maximum size is reduced, entries are evicted from the
         * end of the dynamic table until the size of the dynamic
         * table is less than or equal to the maximum size.
         */
        if (this.tableAddressSpace > size) {
            while (tableAddressSpace - freeAddressSpace >= size) {
                evictDynamicEntry();
            }
        }
        this.freeAddressSpace -= (tableAddressSpace - size);
        this.tableAddressSpace = size;

    }

    public H2HeaderField get(int index) {
        /*
         * Will start at index 0. HPack specifications dictate that the
         * static table address Space is 1 through s; dynamic table
         * address space is s+1 thru s+k. Therefore, caller of this
         * needs to account that the starting position s+1 gets deducted
         * from the requested index, such that s+1 equates to index 0
         * of this dynamic table.
         */
        try {
            return entries.get(index);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public H2HeaderField findInList(int name, int value) {
        H2HeaderField result = null;
        H2HeaderField current = null;
        Iterator<H2HeaderField> i = entries.iterator();
        int indexAddressSpace = StaticTable.STATIC_TABLE.size() + 1;
        while (i.hasNext()) {
            current = i.next();
            if (current.getNameHash() == name) {
                //Store the first partial match. This is the one that will be
                //returned unless a complete match is found.
                if (result == null) {
                    result = current;
                    //current index may have changed due to updates in the table
                    //set new index location.
                    result.setCurrentIndex(indexAddressSpace);
                }

                if (current.getValueHash() == value) {

                    return result;
                }
            }
            indexAddressSpace++;

        }
        return result;
    }

    private void clearDynamicTable() {
        entries.clear();
        freeAddressSpace = tableAddressSpace;
    }

    public int tableAddressSpace() {
        return this.tableAddressSpace;
    }

    //TODO: consider renaming
    public int amountOfEntries() {
        return this.entries.size();
    }

    public int usedAddressSpace() {
        return this.tableAddressSpace - this.freeAddressSpace;
    }

    public int freeSpace() {
        return this.freeAddressSpace;
    }

    public boolean isValid() {
        return this.isValid;
    }

    public void setValidity(boolean isValid) {
        this.isValid = isValid;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        H2HeaderField current;
        int i = 0;
        Iterator<H2HeaderField> it = entries.iterator();
        while (it.hasNext()) {
            i++;
            current = it.next();
            result.append("[  " + i + "] (s = " + current.getSize() + ") " + current.getName() + ": " + current.getValue() + "\n");
        }
        result.append("Table size: " + this.usedAddressSpace());
        return result.toString();
    }

}
