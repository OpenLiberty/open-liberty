/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.utils.xml.ra;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * ra.xml inbound-resourceadapter element
 */
@Trivial
@XmlType(propOrder = { "messageAdapter" })
public class RaInboundResourceAdapter {
    private RaMessageAdapter messageAdapter;
    private String id;

    @XmlElement(name = "messageadapter")
    public void setMessageAdapter(RaMessageAdapter messageAdapter) {
        this.messageAdapter = messageAdapter;
    }

    public RaMessageAdapter getMessageAdapter() {
        return messageAdapter;
    }

    @Override
    public String toString() {
        return "RaInboundResourceAdapter{" + messageAdapter.toString() + "}";
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    @XmlID
    @XmlAttribute(name = "id")
    public void setId(String id) {
        this.id = id;
    }

}
