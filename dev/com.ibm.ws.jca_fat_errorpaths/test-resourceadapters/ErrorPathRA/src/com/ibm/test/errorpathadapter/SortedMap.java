/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.test.errorpathadapter;

import java.util.TreeMap;

public class SortedMap extends TreeMap<String, Object> {
    private static final long serialVersionUID = -7846577209878501012L;

    public Byte getByteProp1() {
        return (Byte) get("ByteProp1");
    }

    public Character getCharProp1() {
        return (Character) get("CharProp1");
    }

    public Double getDoubleProp1() {
        return (Double) get("DoubleProp1");
    }

    public Float getFloatProp1() {
        return (Float) get("FloatProp1");
    }

    public Long getLongProp1() {
        return (Long) get("LongProp1");
    }

    public Integer getIntProp1() {
        return (Integer) get("IntProp1");
    }

    public Short getShortProp1() {
        return (Short) get("ShortProp1");
    }

    public String getStringProp1() {
        return (String) get("StringProp1");
    }

    public void setByteProp1(Byte value) {
        put("ByteProp1", value);
    }

    public void setCharProp1(Character value) {
        put("CharProp1", value);
    }

    public void setDoubleProp1(Double value) {
        put("DoubleProp1", value);
    }

    public void setFloatProp1(Float value) {
        put("FloatProp1", value);
    }

    public void setLongProp1(Long value) {
        put("LongProp1", value);
    }

    public void setIntProp1(Integer value) {
        put("IntProp1", value);
    }

    public void setShortProp1(Short value) {
        put("ShortProp1", value);
    }

    public void setStringProp1(String value) {
        put("StringProp1", value);
    }
}
