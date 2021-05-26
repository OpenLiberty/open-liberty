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

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Embeddable
@Access(AccessType.PROPERTY)
public class EnumeratedPropertyAccessEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public enum EnumeratedPropertyAccessEnum {
        ONE, TWO, THREE
    }

    public static final List<EnumeratedPropertyAccessEmbed> LIST_INIT = Arrays.asList(new EnumeratedPropertyAccessEmbed(EnumeratedPropertyAccessEnum.TWO),
                                                                                      new EnumeratedPropertyAccessEmbed(EnumeratedPropertyAccessEnum.ONE));
    public static final List<EnumeratedPropertyAccessEmbed> LIST_INIT_ORDERED = Arrays.asList(new EnumeratedPropertyAccessEmbed(EnumeratedPropertyAccessEnum.ONE),
                                                                                              new EnumeratedPropertyAccessEmbed(EnumeratedPropertyAccessEnum.TWO));
    public static final List<EnumeratedPropertyAccessEmbed> LIST_UPDATE = Arrays.asList(new EnumeratedPropertyAccessEmbed(EnumeratedPropertyAccessEnum.TWO),
                                                                                        new EnumeratedPropertyAccessEmbed(EnumeratedPropertyAccessEnum.ONE),
                                                                                        new EnumeratedPropertyAccessEmbed(EnumeratedPropertyAccessEnum.THREE));
    public static final List<EnumeratedPropertyAccessEmbed> LIST_UPDATE_ORDERED = Arrays.asList(new EnumeratedPropertyAccessEmbed(EnumeratedPropertyAccessEnum.ONE),
                                                                                                new EnumeratedPropertyAccessEmbed(EnumeratedPropertyAccessEnum.TWO),
                                                                                                new EnumeratedPropertyAccessEmbed(EnumeratedPropertyAccessEnum.THREE));

    public static final Map<EnumeratedPropertyAccessEnum, EnumeratedPropertyAccessEmbed> MAP_INIT;
    public static final Map<EnumeratedPropertyAccessEnum, EnumeratedPropertyAccessEmbed> MAP_UPDATE;
    static {
        Map<EnumeratedPropertyAccessEnum, EnumeratedPropertyAccessEmbed> map = new HashMap<EnumeratedPropertyAccessEnum, EnumeratedPropertyAccessEmbed>();
        map.put(EnumeratedPropertyAccessEnum.TWO, new EnumeratedPropertyAccessEmbed(EnumeratedPropertyAccessEnum.ONE));
        map.put(EnumeratedPropertyAccessEnum.ONE, new EnumeratedPropertyAccessEmbed(EnumeratedPropertyAccessEnum.TWO));
        MAP_INIT = Collections.unmodifiableMap(map);

        map = new HashMap<EnumeratedPropertyAccessEnum, EnumeratedPropertyAccessEmbed>();
        map.put(EnumeratedPropertyAccessEnum.THREE, new EnumeratedPropertyAccessEmbed(EnumeratedPropertyAccessEnum.TWO));
        map.put(EnumeratedPropertyAccessEnum.TWO, new EnumeratedPropertyAccessEmbed(EnumeratedPropertyAccessEnum.ONE));
        map.put(EnumeratedPropertyAccessEnum.ONE, new EnumeratedPropertyAccessEmbed(EnumeratedPropertyAccessEnum.THREE));
        MAP_UPDATE = Collections.unmodifiableMap(map);
    }

    private EnumeratedPropertyAccessEnum enumeratedStringValuePA;
    private EnumeratedPropertyAccessEnum enumeratedOrdinalValuePA;

    public EnumeratedPropertyAccessEmbed() {
    }

    public EnumeratedPropertyAccessEmbed(EnumeratedPropertyAccessEnum enumeratedStringValuePA) {
        this.enumeratedStringValuePA = enumeratedStringValuePA;
        this.enumeratedOrdinalValuePA = enumeratedStringValuePA;
    }

    @Enumerated(EnumType.STRING)
    public EnumeratedPropertyAccessEnum getEnumeratedStringValuePA() {
        return this.enumeratedStringValuePA;
    }

    public void setEnumeratedStringValuePA(EnumeratedPropertyAccessEnum enumeratedStringValuePA) {
        this.enumeratedStringValuePA = enumeratedStringValuePA;
    }

    @Enumerated(EnumType.ORDINAL)
    public EnumeratedPropertyAccessEnum getEnumeratedOrdinalValuePA() {
        return this.enumeratedOrdinalValuePA;
    }

    public void setEnumeratedOrdinalValuePA(EnumeratedPropertyAccessEnum enumeratedOrdinalValuePA) {
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
        if (!(otherObject instanceof EnumeratedPropertyAccessEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "enumeratedStringValuePA=" + getEnumeratedStringValuePA() + ", enumeratedOrdinalValuePA=" + getEnumeratedOrdinalValuePA();
    }

}
