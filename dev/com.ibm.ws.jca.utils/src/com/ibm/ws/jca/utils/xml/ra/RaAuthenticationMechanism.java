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

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jca.utils.xml.ra.v10.Ra10AuthenticationMechanism;

/**
 * ra.xml authentication-mechanism element
 */
@Trivial
@XmlType(propOrder = { "description", "authenticationMechanismType", "credentialInterface" })
public class RaAuthenticationMechanism {

    private static final String PasswordCredential = "javax.resource.spi.security.PasswordCredential";
    private static final String GSSCredential = "org.ietf.jgss.GSSCredential";
    private static final String GenericCredential = "javax.resource.spi.security.GenericCredential";
    private static final Set<String> credentialInterfaceTypes;

    private enum AuthenticationMechanismType {
        BasicPassword,
        Kerbv5
    };

    static {
        Set<String> types = new HashSet<String>();
        types.add(PasswordCredential);
        types.add(GSSCredential);
        types.add(GenericCredential);
        credentialInterfaceTypes = Collections.unmodifiableSet(types);
    }

    @XmlElement(name = "description")
    private final List<RaDescription> description = new LinkedList<RaDescription>();
    private String credentialInterface;
    private String authenticationMechanismType;
    private String id;

    public List<RaDescription> getDescription() {
        return description;
    }

    public String getCredentialInterface() {
        return credentialInterface;
    }

    @XmlElement(name = "credential-interface", required = true)
    public void setCredentialInterface(String credType) {
        if (credType == null || !credentialInterfaceTypes.contains(credType))
            throw new IllegalArgumentException("credential-interface: " + credentialInterface);
        credentialInterface = credType;
    }

    /**
     * @return the authenticationMechanismType
     */
    public String getAuthenticationMechanismType() {
        return authenticationMechanismType;
    }

    /**
     * Set the authentication mechanism type
     * 
     * @param the authentication mechanism type
     */
    @XmlElement(name = "authentication-mechanism-type", required = true)
    public void setAuthenticationMechanismType(String authMech) {
        AuthenticationMechanismType type = AuthenticationMechanismType.valueOf(authMech);
        authenticationMechanismType = type.name();
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    @XmlID
    @XmlAttribute(name = "id")
    public void setId(String id) {
        this.id = id;
    }

    public void copyRa10Settings(Ra10AuthenticationMechanism mechanism) {
        if (mechanism.getDescription() != null) {
            RaDescription desc = new RaDescription();
            desc.setValue(mechanism.getDescription());
            description.add(desc);
        }
        authenticationMechanismType = mechanism.getAuthMechType();
        credentialInterface = mechanism.getCredentialInterface();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RaAuthenticationMechanism{");
        sb.append("credential-interface='");
        if (credentialInterface != null)
            sb.append(credentialInterface);
        sb.append("'}");
        return sb.toString();
    }
}
