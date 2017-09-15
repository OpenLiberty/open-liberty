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

/**
 * Models a Java class type.
 */
public class ModelClassType implements ModelType {
    /**
     * The Java class name.
     */
    protected final String className;

    public ModelClassType(String className) {
        this.className = className;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + className + ']';
    }

    @Override
    public String getJavaTypeName() {
        return className;
    }

    @Override
    public String getJavaImplTypeName() {
        return className;
    }

    @Override
    public String getJavaListImplTypeName() {
        throw new UnsupportedOperationException(toString());
    }

    @Override
    public String getDefaultValue(String string) {
        return null;
    }
}
