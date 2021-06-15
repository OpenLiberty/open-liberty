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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class XMLMapKeyEnumeratedValueEnumeratedEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public enum XMLMapKeyEnumeratedValueEnumeratedEnum {
        ONE, TWO, THREE
    }

    public static final Map<XMLMapKeyEnumeratedValueEnumeratedEnum, XMLMapKeyEnumeratedValueEnumeratedEnum> INIT;
    public static final Map<XMLMapKeyEnumeratedValueEnumeratedEnum, XMLMapKeyEnumeratedValueEnumeratedEnum> UPDATE;
    static {
        Map<XMLMapKeyEnumeratedValueEnumeratedEnum, XMLMapKeyEnumeratedValueEnumeratedEnum> map = new HashMap<XMLMapKeyEnumeratedValueEnumeratedEnum, XMLMapKeyEnumeratedValueEnumeratedEnum>();
        map.put(XMLMapKeyEnumeratedValueEnumeratedEnum.TWO, XMLMapKeyEnumeratedValueEnumeratedEnum.ONE);
        map.put(XMLMapKeyEnumeratedValueEnumeratedEnum.ONE, XMLMapKeyEnumeratedValueEnumeratedEnum.TWO);
        INIT = Collections.unmodifiableMap(map);

        map = new HashMap<XMLMapKeyEnumeratedValueEnumeratedEnum, XMLMapKeyEnumeratedValueEnumeratedEnum>();
        map.put(XMLMapKeyEnumeratedValueEnumeratedEnum.THREE, XMLMapKeyEnumeratedValueEnumeratedEnum.TWO);
        map.put(XMLMapKeyEnumeratedValueEnumeratedEnum.TWO, XMLMapKeyEnumeratedValueEnumeratedEnum.ONE);
        map.put(XMLMapKeyEnumeratedValueEnumeratedEnum.ONE, XMLMapKeyEnumeratedValueEnumeratedEnum.THREE);
        UPDATE = Collections.unmodifiableMap(map);
    }

    private Map<XMLMapKeyEnumeratedValueEnumeratedEnum, XMLMapKeyEnumeratedValueEnumeratedEnum> mapKeyEnumeratedValueEnumerated;

    public XMLMapKeyEnumeratedValueEnumeratedEmbed() {
    }

    public XMLMapKeyEnumeratedValueEnumeratedEmbed(Map<XMLMapKeyEnumeratedValueEnumeratedEnum, XMLMapKeyEnumeratedValueEnumeratedEnum> mapKeyEnumeratedValueEnumerated) {
        this.mapKeyEnumeratedValueEnumerated = mapKeyEnumeratedValueEnumerated;
    }

    public Map<XMLMapKeyEnumeratedValueEnumeratedEnum, XMLMapKeyEnumeratedValueEnumeratedEnum> getMapKeyEnumeratedValueEnumerated() {
        return this.mapKeyEnumeratedValueEnumerated;
    }

    public void setMapKeyEnumeratedValueEnumerated(Map<XMLMapKeyEnumeratedValueEnumeratedEnum, XMLMapKeyEnumeratedValueEnumeratedEnum> mapKeyEnumeratedValueEnumerated) {
        this.mapKeyEnumeratedValueEnumerated = mapKeyEnumeratedValueEnumerated;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLMapKeyEnumeratedValueEnumeratedEmbed))
            return false;
        return (((XMLMapKeyEnumeratedValueEnumeratedEmbed) otherObject).mapKeyEnumeratedValueEnumerated.equals(mapKeyEnumeratedValueEnumerated)); // Can't use hash b/c not sorted.
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (mapKeyEnumeratedValueEnumerated != null) {
            // EclipseLink wraps order column Lists in an IndirectMap implementation and overrides toString()
            Map<XMLMapKeyEnumeratedValueEnumeratedEnum, XMLMapKeyEnumeratedValueEnumeratedEnum> temp = new HashMap<XMLMapKeyEnumeratedValueEnumeratedEnum, XMLMapKeyEnumeratedValueEnumeratedEnum>(mapKeyEnumeratedValueEnumerated);
            sb.append("mapKeyEnumeratedValueEnumerated=" + temp.toString());
        } else {
            sb.append("mapKeyEnumeratedValueEnumerated=null");
        }
        return sb.toString();
    }
}
