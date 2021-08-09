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

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * ra.xml messageadapter element
 */
@Trivial
@XmlType(propOrder = { "messageListeners" })
public class RaMessageAdapter {
    @XmlElement(name = "messagelistener", required = true)
    private final List<RaMessageListener> messageListeners = new LinkedList<RaMessageListener>();
    @XmlID
    @XmlAttribute(name = "id")
    private String id;

    public List<RaMessageListener> getMessageListeners() {
        return messageListeners;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    public RaMessageListener getMessageListenerByType(String messageListenerType) {
        for (RaMessageListener messageListener : messageListeners)
            if (messageListener.getMessageListenerType().equals(messageListenerType))
                return messageListener;

        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RaMessageAdapter{");
        for (RaMessageListener messageListener : messageListeners) {
            sb.append(messageListener.toString()).append(",");
        }
        sb.append("}");
        return sb.toString();
    }
}
