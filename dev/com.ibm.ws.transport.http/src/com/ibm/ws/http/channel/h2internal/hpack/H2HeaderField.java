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

public class H2HeaderField {

    private String name = null;
    private String value = null;
    private int nameHash;
    private int valueHash;
    private int size;
    private int currentIndex = -1;

    public H2HeaderField(String name, String value) {
        this.name = name;
        this.value = value;
        init();
    }

    public H2HeaderField(String name, String value, int index) {
        this.name = name;
        this.value = value;
        this.currentIndex = index;
        init();

    }

    private void init() {
        this.nameHash = name.hashCode();
        this.valueHash = value.hashCode();
        setSize();
    }

    private void setSize() {
        /*
         * The size of an entry is the sum of its name's length in octets (as defined in Section 5.2),
         * its value's length in octets, and 32.
         */
        //TODO: Put 32 on constants.
        this.size = this.name.length() + this.value.length() + 32;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setValue(String value) {
        this.value = value;
        this.valueHash = value.hashCode();
        setSize();
    }

    public void setName(String name) {
        this.name = name;
        this.nameHash = name.hashCode();
        setSize();
    }

    public void setCurrentIndex(int index) {
        this.currentIndex = index;
    }

    public int getCurrentIndex() {
        return this.currentIndex;
    }

    public int getSize() {
        return this.size;
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    public int getNameHash() {
        return this.nameHash;
    }

    public int getValueHash() {
        return this.valueHash;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof H2HeaderField)
            if (getName().equals(((H2HeaderField) o).getName()) && getValue().equals(((H2HeaderField) o).getValue()))
                return true;

        return false;
    }

    @Override
    public String toString() {
        return name + ": " + value;
    }

}
