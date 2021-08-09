/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.registry;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * This class contains utility methods related to LDAP
 */
public final class LDAPUtils {
    private static final TraceComponent tc = Tr.register(LDAPUtils.class);
    private static final int START_CN_OFFSET = 3;

    /**
     * Enforce static-only use via private constructor.
     */
    private LDAPUtils() {}

    /**
     * Extract the common name from a distinguished name, i.e.:
     * input "cn=Paul,ou=WebSphere,o=ibm" will return "Paul".
     * If it's an invalid dn return null.
     * 
     * @param dn distinguished name
     * @return common name
     */
    @FFDCIgnore(InvalidNameException.class)
    public static String getCNFromDN(String dn) {
        String cn = null;
        try {
            LdapName ldapDN = new LdapName(dn);
            for (int i = ldapDN.size() - 1; i > -1; i--) {
                //for (int i = 0; i < ldapDN.size(); i++) {
                if (ldapDN.get(i).toLowerCase().startsWith("cn=")) {
                    cn = ldapDN.get(i).substring(START_CN_OFFSET);
                    break;
                }
            }
        } catch (InvalidNameException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to getCNFromDN: " + e.getMessage(), e);
            }
        }
        return cn;
    }
}
