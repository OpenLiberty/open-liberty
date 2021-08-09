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
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;

@Embeddable
public class SetEnumeratedEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public enum SetEnumeratedEnum {
        ONE, TWO, THREE
    }

    public static final Set<SetEnumeratedEnum> INIT = new HashSet<SetEnumeratedEnum>(Arrays.asList(SetEnumeratedEnum.ONE, SetEnumeratedEnum.THREE));
    public static final Set<SetEnumeratedEnum> UPDATE = new HashSet<SetEnumeratedEnum>(Arrays.asList(SetEnumeratedEnum.ONE, SetEnumeratedEnum.THREE, SetEnumeratedEnum.TWO));

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "SetEnum", joinColumns = @JoinColumn(name = "parent_id"))
    @Column(name = "value")
    @Enumerated(EnumType.STRING)
    private Set<SetEnumeratedEnum> setEnumerated;

    public SetEnumeratedEmbed() {
    }

    public SetEnumeratedEmbed(Set<SetEnumeratedEnum> setEnumerated) {
        this.setEnumerated = setEnumerated;
    }

    public Set<SetEnumeratedEnum> getSetEnumerated() {
        return this.setEnumerated;
    }

    public void setSetEnumerated(Set<SetEnumeratedEnum> setEnumerated) {
        this.setEnumerated = setEnumerated;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof SetEnumeratedEmbed))
            return false;
        return (((SetEnumeratedEmbed) otherObject).setEnumerated.equals(setEnumerated)); // Can't use hash b/c not sorted.
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (setEnumerated != null) {
            // EclipseLink wraps order column Lists in an IndirectSet implementation and overrides toString()
            Set<SetEnumeratedEnum> temp = new HashSet<SetEnumeratedEnum>(setEnumerated);
            sb.append("setEnumerated=" + temp.toString());
        } else {
            sb.append("setEnumerated=null");
        }
        return sb.toString();
    }
}
