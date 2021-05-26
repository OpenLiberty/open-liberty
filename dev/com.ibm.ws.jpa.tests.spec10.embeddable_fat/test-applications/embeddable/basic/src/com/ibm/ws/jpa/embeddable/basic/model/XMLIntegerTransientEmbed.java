/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.embeddable.basic.model;

public class XMLIntegerTransientEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private transient Integer transientJavaValue;

    private Integer transientValue;

    public XMLIntegerTransientEmbed() {
    }

    public XMLIntegerTransientEmbed(int transientJavaValue, int transientValue) {
        this.transientJavaValue = new Integer(transientJavaValue);
        this.transientValue = new Integer(transientValue);
    }

    public Integer getTransientJavaValue() {
        return this.transientJavaValue;
    }

    public void setTransientJavaValue(Integer transientJavaValue) {
        this.transientJavaValue = transientJavaValue;
    }

    public Integer getTransientValue() {
        return this.transientValue;
    }

    public void setTransientValue(Integer transientValue) {
        this.transientValue = transientValue;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLIntegerTransientEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "transientJavaValue=" + transientJavaValue + ", transientValue=" + transientValue;
    }

}
