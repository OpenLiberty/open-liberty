/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.crypto.certificateutil;

/**
 *
 */
public class DefaultSubjectDN {

    private final String subjectDN;

    /**
     * Creates the default SubjectDN.
     */
    public DefaultSubjectDN() {
        this(null, null);
    }

    /**
     * Create the default SubjectDN based on the host and server names.
     *
     * @param hostName May be {@code null}. If {@code null} an attempt is made to determine it.
     * @param serverName May be {@code null}.
     */
    public DefaultSubjectDN(String hostName, String serverName) {
        if (hostName == null) {
            hostName = getHostName();
        }
        if (serverName == null) {
            subjectDN = "CN=" + hostName;
        } else {
            subjectDN = "CN=" + hostName + ",OU=" + serverName;
        }
    }

    /**
     * @return String the default SubjectDN.
     */
    public String getSubjectDN() {
        return subjectDN;
    }

    /**
     * Get the host name.
     *
     * @return String value of the host name or "localhost" if not able to resolve
     */
    private String getHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getCanonicalHostName();
        } catch (java.net.UnknownHostException e) {
            return "localhost";
        }
    }

}
