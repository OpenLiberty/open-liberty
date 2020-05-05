/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.query.entities.interfaces;

import java.io.Serializable;

public class SerializableClass implements ISerializableClass, Serializable {
    private static final long serialVersionUID = -5132930216544441864L;

    private String someString;
    private int someInt;
    private byte[] someBytes;

    public SerializableClass() {

    }

    @Override
    public byte[] getSomeBytes() {
        return someBytes;
    }

    @Override
    public void setSomeBytes(byte[] someBytes) {
        this.someBytes = someBytes;
    }

    @Override
    public int getSomeInt() {
        return someInt;
    }

    @Override
    public void setSomeInt(int someInt) {
        this.someInt = someInt;
    }

    @Override
    public String getSomeString() {
        return someString;
    }

    @Override
    public void setSomeString(String someString) {
        this.someString = someString;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SerializableClass)) {
            return false;
        } else {
            SerializableClass sc = (SerializableClass) o;
            if (sc.getSomeInt() != someInt)
                return false;

            if (!sc.getSomeString().equals(someString))
                return false;

            byte bytes[] = sc.getSomeBytes();
            if (bytes == null && someBytes != null)
                return false;
            for (int index = 0; index < bytes.length; index++) {
                if (someBytes[index] != bytes[index])
                    return false;
            }

            return true;
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("SerializableClass: ");
        sb.append(someInt).append('/').append(someString).append('/');
        if (someBytes != null) {
            sb.append('[');
            for (int index = 0; index < someBytes.length; index++) {
                sb.append(someBytes[index]).append(',');
            }
            sb.setLength(sb.length() - 1);
            sb.append(']');
        } else {
            sb.append("[ no bytes ]");
        }
        return new String(sb);
    }
}
