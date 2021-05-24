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

public class XMLMapKeyEnumeratedValueLobEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public enum XMLMapKeyEnumeratedValueLobEnum {
        ONE, TWO, THREE
    }

    public static final Map<XMLMapKeyEnumeratedValueLobEnum, String> INIT;
    public static final Map<XMLMapKeyEnumeratedValueLobEnum, String> UPDATE;
    static {
        Map<XMLMapKeyEnumeratedValueLobEnum, String> map = new HashMap<XMLMapKeyEnumeratedValueLobEnum, String>();
        map.put(XMLMapKeyEnumeratedValueLobEnum.TWO, "Init2");
        map.put(XMLMapKeyEnumeratedValueLobEnum.ONE, "Init1");
        INIT = Collections.unmodifiableMap(map);

        map = new HashMap<XMLMapKeyEnumeratedValueLobEnum, String>();
        map.put(XMLMapKeyEnumeratedValueLobEnum.THREE, "Update3");
        map.put(XMLMapKeyEnumeratedValueLobEnum.TWO, "Update2");
        map.put(XMLMapKeyEnumeratedValueLobEnum.ONE, "Update1");
        UPDATE = Collections.unmodifiableMap(map);
    }

    private Map<XMLMapKeyEnumeratedValueLobEnum, String> mapKeyEnumeratedValueLob;

    public XMLMapKeyEnumeratedValueLobEmbed() {
    }

    public XMLMapKeyEnumeratedValueLobEmbed(Map<XMLMapKeyEnumeratedValueLobEnum, String> mapKeyEnumeratedValueLob) {
        this.mapKeyEnumeratedValueLob = mapKeyEnumeratedValueLob;
    }

    public Map<XMLMapKeyEnumeratedValueLobEnum, String> getMapKeyEnumeratedValueLob() {
        return this.mapKeyEnumeratedValueLob;
    }

    public void setMapKeyEnumeratedValueLob(Map<XMLMapKeyEnumeratedValueLobEnum, String> mapKeyEnumeratedValueLob) {
        this.mapKeyEnumeratedValueLob = mapKeyEnumeratedValueLob;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLMapKeyEnumeratedValueLobEmbed))
            return false;
        return (((XMLMapKeyEnumeratedValueLobEmbed) otherObject).mapKeyEnumeratedValueLob.equals(mapKeyEnumeratedValueLob)); // Can't use hash b/c not sorted.
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (mapKeyEnumeratedValueLob != null) {
            // EclipseLink wraps order column Lists in an IndirectMap implementation and overrides toString()
            Map<XMLMapKeyEnumeratedValueLobEnum, String> temp = new HashMap<XMLMapKeyEnumeratedValueLobEnum, String>(mapKeyEnumeratedValueLob);
            sb.append("mapKeyEnumeratedValueLob=" + temp.toString());
        } else {
            sb.append("mapKeyEnumeratedValueLob=null");
        }
        return sb.toString();
    }
}
