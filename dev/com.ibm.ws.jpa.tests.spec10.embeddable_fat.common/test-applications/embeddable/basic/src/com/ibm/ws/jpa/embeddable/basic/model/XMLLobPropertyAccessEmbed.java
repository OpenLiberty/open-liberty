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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class XMLLobPropertyAccessEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static final Collection<XMLLobPropertyAccessEmbed> COLLECTION_INIT = new ArrayList<XMLLobPropertyAccessEmbed>(Arrays
                    .asList(new XMLLobPropertyAccessEmbed("InitPA2"), new XMLLobPropertyAccessEmbed("InitPA3"), new XMLLobPropertyAccessEmbed("InitPA1")));
    public static final Collection<XMLLobPropertyAccessEmbed> COLLECTION_UPDATE = new ArrayList<XMLLobPropertyAccessEmbed>(Arrays
                    .asList(new XMLLobPropertyAccessEmbed("UpdatePA2"), new XMLLobPropertyAccessEmbed("UpdatePA4"), new XMLLobPropertyAccessEmbed("UpdatePA3"),
                            new XMLLobPropertyAccessEmbed("UpdatePA1")));

    public static final Map<XMLLobPropertyAccessEmbed, XMLLobPropertyAccessEmbed> MAP_INIT;
    public static final Map<XMLLobPropertyAccessEmbed, XMLLobPropertyAccessEmbed> MAP_UPDATE;
    static {
        Map<XMLLobPropertyAccessEmbed, XMLLobPropertyAccessEmbed> map = new HashMap<XMLLobPropertyAccessEmbed, XMLLobPropertyAccessEmbed>();
        map.put(new XMLLobPropertyAccessEmbed("Init1a"), new XMLLobPropertyAccessEmbed("Init1b"));
        map.put(new XMLLobPropertyAccessEmbed("Init2a"), new XMLLobPropertyAccessEmbed("Init2b"));
        MAP_INIT = Collections.unmodifiableMap(map);

        map = new HashMap<XMLLobPropertyAccessEmbed, XMLLobPropertyAccessEmbed>();
        map.put(new XMLLobPropertyAccessEmbed("Update3a"), new XMLLobPropertyAccessEmbed("Update3b"));
        map.put(new XMLLobPropertyAccessEmbed("Update2a"), new XMLLobPropertyAccessEmbed("Update2b"));
        map.put(new XMLLobPropertyAccessEmbed("Update1a"), new XMLLobPropertyAccessEmbed("Update1b"));
        MAP_UPDATE = Collections.unmodifiableMap(map);
    }

    private String clobValuePA;

    public XMLLobPropertyAccessEmbed() {
    }

    public XMLLobPropertyAccessEmbed(String clobValuePA) {
        this.clobValuePA = clobValuePA;
    }

    public String getClobValuePA() {
        return this.clobValuePA;
    }

    public void setClobValuePA(String clobValuePA) {
        this.clobValuePA = clobValuePA;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLLobPropertyAccessEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "clobValuePA=" + getClobValuePA();
    }

}
