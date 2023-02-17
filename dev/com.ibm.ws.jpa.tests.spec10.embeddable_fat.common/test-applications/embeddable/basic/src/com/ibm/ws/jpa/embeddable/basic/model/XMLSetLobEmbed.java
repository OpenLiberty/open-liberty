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

public class XMLSetLobEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static final Set<String> INIT = new HashSet<String>(Arrays.asList("Init1", "Init2"));
    public static final Set<String> UPDATE = new HashSet<String>(Arrays.asList("Update1", "Update2", "Update3"));

    private Set<String> setLob;

    public XMLSetLobEmbed() {
    }

    public XMLSetLobEmbed(Set<String> setLob) {
        this.setLob = setLob;
    }

    public Set<String> getSetLob() {
        return this.setLob;
    }

    public void setSetLob(Set<String> setLob) {
        this.setLob = setLob;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLSetLobEmbed))
            return false;
        return (((XMLSetLobEmbed) otherObject).setLob.equals(setLob)); // Can't use hash b/c not sorted.
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (setLob != null) {
            // EclipseLink wraps order column Lists in an IndirectSet implementation and overrides toString()
            Set<String> temp = new HashSet<String>(setLob);
            sb.append("setLob=" + temp.toString());
        } else {
            sb.append("setLob=null");
        }
        return sb.toString();
    }
}
