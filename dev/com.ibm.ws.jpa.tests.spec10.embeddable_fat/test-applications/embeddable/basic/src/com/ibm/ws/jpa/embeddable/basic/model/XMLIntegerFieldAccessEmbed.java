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

public class XMLIntegerFieldAccessEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private Integer integerValueFieldAccessColumn;

    public XMLIntegerFieldAccessEmbed() {
    }

    public XMLIntegerFieldAccessEmbed(int integerValueFieldAccessColumn) {
        this.integerValueFieldAccessColumn = new Integer(integerValueFieldAccessColumn);
    }

    public Integer getIntegerValueFieldAccessColumn() {
        return this.integerValueFieldAccessColumn;
    }

    public void setIntegerValueFieldAccessColumn(
                                                 Integer integerValueFieldAccessColumn) {
        this.integerValueFieldAccessColumn = integerValueFieldAccessColumn;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLIntegerFieldAccessEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "integerValueFieldAccessColumn=" + integerValueFieldAccessColumn;
    }

}
