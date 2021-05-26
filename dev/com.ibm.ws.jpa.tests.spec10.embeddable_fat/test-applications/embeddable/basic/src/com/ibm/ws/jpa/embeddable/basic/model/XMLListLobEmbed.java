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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class XMLListLobEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static final List<String> INIT = new ArrayList<String>(Arrays.asList("Init1", "Init2"));
    public static final List<String> UPDATE = new ArrayList<String>(Arrays.asList("Update1", "Update2", "Update3"));

    private List<String> listLob;

    public XMLListLobEmbed() {
    }

    public XMLListLobEmbed(List<String> listLob) {
        this.listLob = listLob;
    }

    public List<String> getListLob() {
        return this.listLob;
    }

    public void setListLob(List<String> listLob) {
        this.listLob = listLob;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLListLobEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (listLob != null) {
            // EclipseLink wraps order column Lists in an IndirectList implementation and overrides toString()
            List<String> temp = new Vector<String>(listLob);
            sb.append("listLob=" + temp.toString());
        } else {
            sb.append("listLob=null");
        }
        return sb.toString();
    }
}
