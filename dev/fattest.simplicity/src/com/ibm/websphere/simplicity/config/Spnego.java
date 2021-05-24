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
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Represents the <kerberos> element in server.xml
 */
public class Spnego extends ConfigElement {

    @XmlAttribute
    public String krb5Keytab;

    @XmlAttribute
    public String krb5Config;

    @XmlAttribute
    public String servicePrincipalNames;

    @XmlAttribute
    public String canonicalHostName;

    @XmlAttribute
    public String skipForUnprotectedURI;

    @XmlAttribute
    public String trimKerberosRealmNameFromPrincipal;

    @XmlAttribute
    public String spnegoAuthenticationErrorPageURL;

    @XmlAttribute
    public String spnegoNotSupportedErrorPageURL;

    @XmlAttribute
    public String ntlmTokenReceivedErrorPageURL;

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        // attributes
        buf.append("id=\"" + (getId() == null ? "" : getId()) + "\" ");
        buf.append("canonicalHostName=").append(canonicalHostName).append(",\n");
        buf.append("servicePrincipalNames=").append(servicePrincipalNames).append(",\n");
        buf.append("skipForUnprotectedURI=").append(skipForUnprotectedURI).append(",\n");
        buf.append("trimKerberosRealmNameFromPrincipal=").append(trimKerberosRealmNameFromPrincipal).append(",\n");
        buf.append("krb5Keytab=").append(krb5Keytab).append(",\n");
        buf.append("krb5Config=").append(krb5Config).append(",\n");
        buf.append("spnegoAuthenticationErrorPageURL=").append(spnegoAuthenticationErrorPageURL).append(",\n");
        buf.append("spnegoNotSupportedErrorPageURL=").append(spnegoNotSupportedErrorPageURL).append(",\n");
        buf.append("ntlmTokenReceivedErrorPageURL=").append(ntlmTokenReceivedErrorPageURL).append(",\n");
        buf.append('}');
        return buf.toString();
    }
}
