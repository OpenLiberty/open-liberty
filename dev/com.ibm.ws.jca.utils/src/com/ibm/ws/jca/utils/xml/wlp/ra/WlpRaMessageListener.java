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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * ra.xml messagelistener element
 */
@Trivial
@XmlType
public class WlpRaMessageListener {

    private WlpRaActivationSpec activationSpec;

    @XmlAttribute(name = "aliasSuffix")
    private String aliasSuffix;
    @XmlAttribute(name = "messagelistener-type")
    private String wlp_messageListenerType;
    @XmlAttribute(name = "nlsKey")
    private String wlp_nlsKey;
    @XmlAttribute(name = "name")
    private String wlp_name;
    @XmlAttribute(name = "description")
    private String wlp_description;

    public String getName() {
        return wlp_name;
    }

    public String getDescription() {
        return wlp_description;
    }

    public String getNLSKey() {
        return wlp_nlsKey;
    }

    public String getAliasSuffix() {
        return aliasSuffix;
    }

    public String getMessageListenerType() {
        return wlp_messageListenerType;
    }

    @XmlElement(name = "activationspec")
    public void setActivationSpec(WlpRaActivationSpec activationSpec) {
        this.activationSpec = activationSpec;
    }

    public WlpRaActivationSpec getActivationSpec() {
        return activationSpec;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("WlpRaMessageListener{messagelistener-type='");
        sb.append(wlp_messageListenerType);
        return sb.append("'}").toString();
    }
}
