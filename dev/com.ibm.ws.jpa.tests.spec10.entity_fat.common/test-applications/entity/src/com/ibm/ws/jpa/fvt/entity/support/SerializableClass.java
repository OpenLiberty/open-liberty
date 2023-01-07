/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.entity.support;

import java.io.Serializable;
import java.util.Arrays;

public class SerializableClass implements Serializable {
    private String someString;
    private int someInt;
    private byte[] someBytes;

    public SerializableClass() {

    }

    public byte[] getSomeBytes() {
        return someBytes;
    }

    public void setSomeBytes(byte[] someBytes) {
        this.someBytes = someBytes;
    }

    public int getSomeInt() {
        return someInt;
    }

    public void setSomeInt(int someInt) {
        this.someInt = someInt;
    }

    public String getSomeString() {
        return someString;
    }

    public void setSomeString(String someString) {
        this.someString = someString;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(someBytes);
        result = prime * result + someInt;
        result = prime * result
                 + ((someString == null) ? 0 : someString.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof SerializableClass))
            return false;
        SerializableClass other = (SerializableClass) obj;
        if (!Arrays.equals(someBytes, other.someBytes))
            return false;
        if (someInt != other.someInt)
            return false;
        if (someString == null) {
            if (other.someString != null)
                return false;
        } else if (!someString.equals(other.someString))
            return false;
        return true;
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
