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
package com.ibm.ws.jca.utils.xml.wlp.ra;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * ra.xml messageadapter element
 */
@Trivial
@XmlType
public class WlpRaMessageAdapter {
    @XmlElement(name = "messagelistener")
    private final List<WlpRaMessageListener> messageListeners = new LinkedList<WlpRaMessageListener>();

    public List<WlpRaMessageListener> getMessageListeners() {
        return messageListeners;
    }

    public WlpRaMessageListener getMessageListenerByType(String messageListenerType) {
        for (WlpRaMessageListener messageListener : messageListeners)
            if (messageListener.getMessageListenerType().equals(messageListenerType))
                return messageListener;

        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("WlpRaMessageAdapter{");
        for (WlpRaMessageListener messageListener : messageListeners) {
            sb.append(messageListener.toString()).append(",");
        }
        sb.append("}");
        return sb.toString();
    }
}
