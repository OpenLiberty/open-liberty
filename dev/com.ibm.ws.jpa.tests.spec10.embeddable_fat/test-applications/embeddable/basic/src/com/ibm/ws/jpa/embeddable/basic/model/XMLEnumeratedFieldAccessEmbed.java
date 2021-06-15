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

public class XMLEnumeratedFieldAccessEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private XMLEnumeratedFieldAccessEnum enumeratedStringValueFA;
    private XMLEnumeratedFieldAccessEnum enumeratedOrdinalValueFA;

    public enum XMLEnumeratedFieldAccessEnum {
        ONE, TWO, THREE
    }

    public XMLEnumeratedFieldAccessEmbed() {
    }

    public XMLEnumeratedFieldAccessEmbed(XMLEnumeratedFieldAccessEnum enumeratedStringValueFA) {
        this.enumeratedStringValueFA = enumeratedStringValueFA;
        this.enumeratedOrdinalValueFA = enumeratedStringValueFA;
    }

    public XMLEnumeratedFieldAccessEnum getEnumeratedStringValueFA() {
        return this.enumeratedStringValueFA;
    }

    public void setEnumeratedStringValueFA(XMLEnumeratedFieldAccessEnum enumeratedStringValueFA) {
        this.enumeratedStringValueFA = enumeratedStringValueFA;
    }

    public XMLEnumeratedFieldAccessEnum getEnumeratedOrdinalValueFA() {
        return this.enumeratedOrdinalValueFA;
    }

    public void setEnumeratedOrdinalValueFA(XMLEnumeratedFieldAccessEnum enumeratedOrdinalValueFA) {
        this.enumeratedOrdinalValueFA = enumeratedOrdinalValueFA;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLEnumeratedFieldAccessEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "enumeratedStringValueFA=" + enumeratedStringValueFA
               + ", enumeratedOrdinalValueFA=" + enumeratedOrdinalValueFA;
    }

}
