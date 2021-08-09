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

import java.util.Arrays;
import java.util.List;

public class XMLIntegerAttributeOverridesEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static final List<XMLIntegerAttributeOverridesEmbed> LIST_INIT = Arrays.asList(new XMLIntegerAttributeOverridesEmbed(new Integer(2)),
                                                                                          new XMLIntegerAttributeOverridesEmbed(new Integer(3)),
                                                                                          new XMLIntegerAttributeOverridesEmbed(new Integer(1)));
    public static final List<XMLIntegerAttributeOverridesEmbed> LIST_UPDATE = Arrays.asList(new XMLIntegerAttributeOverridesEmbed(new Integer(2)),
                                                                                            new XMLIntegerAttributeOverridesEmbed(new Integer(4)),
                                                                                            new XMLIntegerAttributeOverridesEmbed(new Integer(3)),
                                                                                            new XMLIntegerAttributeOverridesEmbed(new Integer(1)));

    private Integer notIntegerValue;

    public XMLIntegerAttributeOverridesEmbed() {
    }

    public XMLIntegerAttributeOverridesEmbed(int notIntegerValue) {
        this.notIntegerValue = new Integer(notIntegerValue);
    }

    public Integer getNotIntegerValue() {
        return this.notIntegerValue;
    }

    public void setNotIntegerValue(Integer notIntegerValue) {
        this.notIntegerValue = notIntegerValue;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLIntegerAttributeOverridesEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "notIntegerValue=" + notIntegerValue;
    }

}
