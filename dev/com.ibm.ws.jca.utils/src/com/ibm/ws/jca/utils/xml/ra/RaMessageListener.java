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

import java.util.HashMap;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaMessageListener;

/**
 * ra.xml messagelistener element
 */
@Trivial
@XmlType(propOrder = { "messageListenerType", "activationSpec" })
public class RaMessageListener {
    public static final HashMap<String, String> parentPids = new HashMap<String, String>();
    static {
        parentPids.put("javax.jms.MessageListener", "com.ibm.ws.jca.jmsActivationSpec");
        parentPids.put(null, "com.ibm.ws.jca.activationSpec");
    }

    private String messageListenerType;
    private RaActivationSpec activationSpec;
    private String id;

    // wlp-ra.xml settings
    @XmlTransient
    private String aliasSuffix;
    @XmlTransient
    private String wlp_messageListenerType;
    @XmlTransient
    private String wlp_nlsKey;
    @XmlTransient
    private String wlp_name;
    @XmlTransient
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

    @XmlElement(name = "messagelistener-type", required = true)
    public void setMessageListenerType(String messageListenerType) {
        this.messageListenerType = messageListenerType;
    }

    public String getMessageListenerType() {
        return messageListenerType != null ? messageListenerType : wlp_messageListenerType;
    }

    @XmlElement(name = "activationspec", required = true)
    public void setActivationSpec(RaActivationSpec activationSpec) {
        this.activationSpec = activationSpec;
    }

    public RaActivationSpec getActivationSpec() {
        return activationSpec;
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

    public String getMessageListenerParentPid() {
        String parentPid = parentPids.get(messageListenerType);
        if (parentPid != null)
            return parentPid;
        else
            return parentPids.get(null);
    }

    public void copyWlpSettings(WlpRaMessageListener messageListener) {
        aliasSuffix = messageListener.getAliasSuffix();
        wlp_nlsKey = messageListener.getNLSKey();
        wlp_name = messageListener.getName();
        wlp_description = messageListener.getDescription();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RaMessageListener{messagelistener-type='");
        if (messageListenerType != null)
            sb.append(messageListenerType);
        else
            sb.append(wlp_messageListenerType);
        return sb.append("'}").toString();
    }

    public boolean useSpecializedConfig() {
        return parentPids.get(messageListenerType) != null;
    }
}
