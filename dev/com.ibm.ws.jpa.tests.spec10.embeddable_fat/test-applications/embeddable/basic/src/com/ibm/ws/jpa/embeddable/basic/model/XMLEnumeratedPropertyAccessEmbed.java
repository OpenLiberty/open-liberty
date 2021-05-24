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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XMLEnumeratedPropertyAccessEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public enum XMLEnumeratedPropertyAccessEnum {
        ONE, TWO, THREE
    }

    public static final List<XMLEnumeratedPropertyAccessEmbed> LIST_INIT = Arrays.asList(new XMLEnumeratedPropertyAccessEmbed(XMLEnumeratedPropertyAccessEnum.TWO),
                                                                                         new XMLEnumeratedPropertyAccessEmbed(XMLEnumeratedPropertyAccessEnum.ONE));
    public static final List<XMLEnumeratedPropertyAccessEmbed> LIST_INIT_ORDERED = Arrays.asList(new XMLEnumeratedPropertyAccessEmbed(XMLEnumeratedPropertyAccessEnum.ONE),
                                                                                                 new XMLEnumeratedPropertyAccessEmbed(XMLEnumeratedPropertyAccessEnum.TWO));
    public static final List<XMLEnumeratedPropertyAccessEmbed> LIST_UPDATE = Arrays.asList(new XMLEnumeratedPropertyAccessEmbed(XMLEnumeratedPropertyAccessEnum.TWO),
                                                                                           new XMLEnumeratedPropertyAccessEmbed(XMLEnumeratedPropertyAccessEnum.ONE),
                                                                                           new XMLEnumeratedPropertyAccessEmbed(XMLEnumeratedPropertyAccessEnum.THREE));
    public static final List<XMLEnumeratedPropertyAccessEmbed> LIST_UPDATE_ORDERED = Arrays.asList(new XMLEnumeratedPropertyAccessEmbed(XMLEnumeratedPropertyAccessEnum.ONE),
                                                                                                   new XMLEnumeratedPropertyAccessEmbed(XMLEnumeratedPropertyAccessEnum.TWO),
                                                                                                   new XMLEnumeratedPropertyAccessEmbed(XMLEnumeratedPropertyAccessEnum.THREE));

    public static final Map<XMLEnumeratedPropertyAccessEnum, XMLEnumeratedPropertyAccessEmbed> MAP_INIT;
    public static final Map<XMLEnumeratedPropertyAccessEnum, XMLEnumeratedPropertyAccessEmbed> MAP_UPDATE;
    static {
        Map<XMLEnumeratedPropertyAccessEnum, XMLEnumeratedPropertyAccessEmbed> map = new HashMap<XMLEnumeratedPropertyAccessEnum, XMLEnumeratedPropertyAccessEmbed>();
        map.put(XMLEnumeratedPropertyAccessEnum.TWO, new XMLEnumeratedPropertyAccessEmbed(XMLEnumeratedPropertyAccessEnum.ONE));
        map.put(XMLEnumeratedPropertyAccessEnum.ONE, new XMLEnumeratedPropertyAccessEmbed(XMLEnumeratedPropertyAccessEnum.TWO));
        MAP_INIT = Collections.unmodifiableMap(map);

        map = new HashMap<XMLEnumeratedPropertyAccessEnum, XMLEnumeratedPropertyAccessEmbed>();
        map.put(XMLEnumeratedPropertyAccessEnum.THREE, new XMLEnumeratedPropertyAccessEmbed(XMLEnumeratedPropertyAccessEnum.TWO));
        map.put(XMLEnumeratedPropertyAccessEnum.TWO, new XMLEnumeratedPropertyAccessEmbed(XMLEnumeratedPropertyAccessEnum.ONE));
        map.put(XMLEnumeratedPropertyAccessEnum.ONE, new XMLEnumeratedPropertyAccessEmbed(XMLEnumeratedPropertyAccessEnum.THREE));
        MAP_UPDATE = Collections.unmodifiableMap(map);
    }

    private XMLEnumeratedPropertyAccessEnum enumeratedStringValuePA;
    private XMLEnumeratedPropertyAccessEnum enumeratedOrdinalValuePA;

    public XMLEnumeratedPropertyAccessEmbed() {
    }

    public XMLEnumeratedPropertyAccessEmbed(XMLEnumeratedPropertyAccessEnum enumeratedStringValuePA) {
        this.enumeratedStringValuePA = enumeratedStringValuePA;
        this.enumeratedOrdinalValuePA = enumeratedStringValuePA;
    }

    public XMLEnumeratedPropertyAccessEnum getEnumeratedStringValuePA() {
        return this.enumeratedStringValuePA;
    }

    public void setEnumeratedStringValuePA(XMLEnumeratedPropertyAccessEnum enumeratedStringValuePA) {
        this.enumeratedStringValuePA = enumeratedStringValuePA;
    }

    public XMLEnumeratedPropertyAccessEnum getEnumeratedOrdinalValuePA() {
        return this.enumeratedOrdinalValuePA;
    }

    public void setEnumeratedOrdinalValuePA(XMLEnumeratedPropertyAccessEnum enumeratedOrdinalValuePA) {
        this.enumeratedOrdinalValuePA = enumeratedOrdinalValuePA;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLEnumeratedPropertyAccessEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "enumeratedStringValuePA=" + getEnumeratedStringValuePA() + ", enumeratedOrdinalValuePA=" + getEnumeratedOrdinalValuePA();
    }

}
