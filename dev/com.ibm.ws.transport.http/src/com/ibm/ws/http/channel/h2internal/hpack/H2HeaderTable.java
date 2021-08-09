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

public class H2HeaderTable {

    private final DynamicTable dynamicTable;

    public H2HeaderTable() {
        dynamicTable = new DynamicTable();
    }

    public H2HeaderTable(int tableSize) {
        dynamicTable = new DynamicTable(tableSize);
    }

    public H2HeaderField getHeaderEntry(int index) {
        if (index <= StaticTable.STATIC_TABLE.size()) {
            return getFromStaticTable(index);
        } else {
            //Make sure to reduce the static table size from the index size
            //such that the address space location starts at 0 from the
            //perspective of the dynamic table.
            return dynamicTable.get(index - StaticTable.STATIC_TABLE.size() - 1);
        }
    }

    public H2HeaderField getHeaderEntry(String name, String value) {

        int hashName = name.hashCode();
        int hashValue = value.hashCode();

        H2HeaderField staticResult = null;
        H2HeaderField dynamicResult = null;

        staticResult = findInStaticTable(hashName, hashValue);

        if (staticResult != null) {
            if (staticResult.getValueHash() == hashValue) {
                //Fully matches return
                return staticResult;
            }
        }

        dynamicResult = dynamicTable.findInList(hashName, hashValue);

        if (dynamicResult != null) {
            if (dynamicResult.getValueHash() == hashValue) {
                return dynamicResult;
            }
        }
        //If it get here, there is not a complete match. Return the
        //result from the static table if it isn't null. Otherwise,
        //return the result from the dynamic table. If no results
        //were found in either table, this will return null.

        return staticResult != null ? staticResult : dynamicResult;
    }

    private static H2HeaderField getFromStaticTable(int index) {
        return StaticTable.STATIC_TABLE.get(index - 1);
    }

    private static H2HeaderField findInStaticTable(int name, int value) {
        Iterator<H2HeaderField> it = StaticTable.STATIC_TABLE.iterator();
        H2HeaderField result = null;
        H2HeaderField current = null;

        while (it.hasNext()) {
            current = it.next();
            if (current.getNameHash() == name) {
                //Store the first partial match. This is the one that will be
                //returned unless a complete match is found.
                if (result == null) {
                    result = current;
                }
                if (current.getValueHash() == value) {
                    return current;
                }
            }
        }
        return result;

    }

    public void addHeaderEntry(H2HeaderField entry) {
        this.dynamicTable.addDynamicEntry(entry);
    }

    public void updateTableSize(int size) {
        this.dynamicTable.updateDynamicTableSize(size);
    }

    public int getDynamicTableUsedAddressSpace() {
        return this.dynamicTable.usedAddressSpace();
    }

    public String printDynamicTable() {
        return (this.dynamicTable.toString());
    }

    public void setDynamicTableValidity(boolean isValid) {
        this.dynamicTable.setValidity(isValid);
    }

    public boolean isDynamicTableValid() {
        return this.dynamicTable.isValid();
    }

    public int getDynamicEntryCount() {
        return this.dynamicTable.amountOfEntries();
    }

}
