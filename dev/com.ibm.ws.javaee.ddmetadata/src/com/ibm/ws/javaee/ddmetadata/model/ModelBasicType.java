/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmetadata.model;

import com.ibm.ws.javaee.ddmodel.BooleanType;
import com.ibm.ws.javaee.ddmodel.IntegerType;
import com.ibm.ws.javaee.ddmodel.LongType;
import com.ibm.ws.javaee.ddmodel.ProtectedStringType;
import com.ibm.ws.javaee.ddmodel.StringType;

public enum ModelBasicType implements ModelType {
    Boolean(boolean.class, BooleanType.class, "false", "getBooleanValue"),
    Int(int.class, IntegerType.class, "0", "getIntValue"),
    Long(long.class, LongType.class, "0", "getLongValue"),
    String(String.class, StringType.class, null, "getValue"),
    ProtectedString(String.class, ProtectedStringType.class, null, "getValue");

    public final Class<?> type;
    public final Class<?> fieldType;
    public final String defaultValue;
    public final String valueMethodName;

    ModelBasicType(Class<?> type, Class<?> fieldType, String defaultValue, String valueMethodName) {
        this.type = type;
        this.fieldType = fieldType;
        this.defaultValue = defaultValue;
        this.valueMethodName = valueMethodName;
    }

    @Override
    public String getJavaTypeName() {
        return type.getName();
    }

    @Override
    public String getJavaImplTypeName() {
        return fieldType.getName();
    }

    @Override
    public String getJavaListImplTypeName() {
        return fieldType.getName() + ".ListType";
    }

    @Override
    public String getDefaultValue(String string) {
        if (string != null) {
            throw new UnsupportedOperationException();
        }
        return defaultValue;
    }

    private String getPlainTypeName() {
        String simpleName = fieldType.getSimpleName();
        return simpleName.substring(0, simpleName.length() - "Type".length());
    }

    public String getParseAttributeMethodName() {
        return "parse" + getPlainTypeName() + "AttributeValue";
    }

    public String getParseListAttributeMethodName() {
        return "parse" + getPlainTypeName() + "ListAttributeValue";
    }
}
