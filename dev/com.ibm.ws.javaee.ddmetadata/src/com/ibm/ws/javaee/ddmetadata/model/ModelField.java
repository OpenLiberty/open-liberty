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

import java.util.concurrent.TimeUnit;

public class ModelField {
    public String name;

    /**
     * The modeled type of the return value of the method.
     *
     * @see #list
     */
    public final ModelType type;

    /**
     * The name of the addX method if this field is a List of {@link #type}.
     */
    public String listAddMethodName;

    /**
     * True if this field can be private.
     */
    public final boolean privateAccess;

    private TimeUnit durationTimeUnit;

    private String libertyReference;

    public ModelField(String name, ModelType type, String listAddMethodName, boolean privateAccess) {
        this.name = name;
        this.type = type;
        this.listAddMethodName = listAddMethodName;
        this.privateAccess = privateAccess;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + name + ", " + type + ", list=" + listAddMethodName + ']';
    }

    public String getJavaTypeName() {
        if (listAddMethodName != null) {
            return "DDParser.ParsableListImplements<" + type.getJavaImplTypeName() + ", " + type.getJavaTypeName() + '>';
        }
        return type.getJavaImplTypeName();
    }

    public String getJavaImplTypeName() {
        if (listAddMethodName != null) {
            return type.getJavaListImplTypeName();
        }
        return type.getJavaImplTypeName();
    }

    /**
     * If set, will result in ibm:type=duration(timeUnit)
     */
    public void setDuration(TimeUnit timeUnit) {
        this.durationTimeUnit = timeUnit;
    }

    public TimeUnit getDurationTimeUnit() {
        return this.durationTimeUnit;
    }

    /**
     * If set, will result in ibm:reference="refName"
     */
    public void setLibertyReference(String refName) {
        this.libertyReference = refName;
    }

    public String getLibertyReference() {
        return this.libertyReference;
    }
}
