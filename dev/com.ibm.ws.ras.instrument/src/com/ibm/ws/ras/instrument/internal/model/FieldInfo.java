/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ras.instrument.internal.model;

public class FieldInfo {

    private String fieldName;
    private String fieldDescriptor;
    private boolean loggerField;
    private boolean sensitive;

    public FieldInfo() {}

    public FieldInfo(String name, String descriptor) {
        this.fieldName = name;
        this.fieldDescriptor = descriptor;
    }

    public String getFieldDescriptor() {
        return fieldDescriptor;
    }

    public void setFieldDescriptor(String fieldDescriptor) {
        this.fieldDescriptor = fieldDescriptor;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public boolean isLoggerField() {
        return loggerField;
    }

    public void setLoggerField(boolean loggerField) {
        this.loggerField = loggerField;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public void setSensitive(boolean sensitive) {
        this.sensitive = sensitive;
    }

    public void updateDefaultValuesFromClassInfo(ClassInfo classInfo) {
        if (!isSensitive() && classInfo.isSensitive()) {
            setSensitive(true);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(";fieldName=").append(fieldName);
        sb.append(",fieldDescriptor=").append(fieldDescriptor);
        sb.append(",loggerField=").append(loggerField);
        sb.append(",sensitive=").append(sensitive);
        return sb.toString();
    }
}
