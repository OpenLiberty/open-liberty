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

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;
import javax.persistence.Lob;

@Embeddable
@Access(AccessType.PROPERTY)
public class LobPropertyAccessEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static final Collection<LobPropertyAccessEmbed> COLLECTION_INIT = new ArrayList<LobPropertyAccessEmbed>(Arrays
                    .asList(new LobPropertyAccessEmbed("InitPA2"), new LobPropertyAccessEmbed("InitPA3"), new LobPropertyAccessEmbed("InitPA1")));
    public static final Collection<LobPropertyAccessEmbed> COLLECTION_UPDATE = new ArrayList<LobPropertyAccessEmbed>(Arrays
                    .asList(new LobPropertyAccessEmbed("UpdatePA2"), new LobPropertyAccessEmbed("UpdatePA4"), new LobPropertyAccessEmbed("UpdatePA3"),
                            new LobPropertyAccessEmbed("UpdatePA1")));

    public static final Map<LobPropertyAccessEmbed, LobPropertyAccessEmbed> MAP_INIT;
    public static final Map<LobPropertyAccessEmbed, LobPropertyAccessEmbed> MAP_UPDATE;
    static {
        Map<LobPropertyAccessEmbed, LobPropertyAccessEmbed> map = new HashMap<LobPropertyAccessEmbed, LobPropertyAccessEmbed>();
        map.put(new LobPropertyAccessEmbed("Init1a"), new LobPropertyAccessEmbed("Init1b"));
        map.put(new LobPropertyAccessEmbed("Init2a"), new LobPropertyAccessEmbed("Init2b"));
        MAP_INIT = Collections.unmodifiableMap(map);

        map = new HashMap<LobPropertyAccessEmbed, LobPropertyAccessEmbed>();
        map.put(new LobPropertyAccessEmbed("Update3a"), new LobPropertyAccessEmbed("Update3b"));
        map.put(new LobPropertyAccessEmbed("Update2a"), new LobPropertyAccessEmbed("Update2b"));
        map.put(new LobPropertyAccessEmbed("Update1a"), new LobPropertyAccessEmbed("Update1b"));
        MAP_UPDATE = Collections.unmodifiableMap(map);
    }

    private String clobValuePA;

    public LobPropertyAccessEmbed() {
    }

    public LobPropertyAccessEmbed(String clobValuePA) {
        this.clobValuePA = clobValuePA;
    }

    @Lob
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
        if (!(otherObject instanceof LobPropertyAccessEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "clobValuePA=" + getClobValuePA();
    }

}
