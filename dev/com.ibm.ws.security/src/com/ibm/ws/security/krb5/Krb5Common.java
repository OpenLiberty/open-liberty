/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.krb5;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;

import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.kernel.service.util.JavaInfo;

/**
 * Krb5Common
 * - Common constants and methods that use for Kerberos and SPNEGO features
 */
public class Krb5Common {
    private static final TraceComponent tc = Tr.register(Krb5Common.class);
    // SPNEGO mechanism OID
    static public Oid SPNEGO_MECH_OID;
    // Kerberos mechanism OID
    static public Oid KRB5_MECH_OID;

    static public boolean IBM_KRB5_LOGIN_MODULE_AVAILABLE = JavaInfo.isSystemClassAvailable("com.ibm.security.auth.module.Krb5LoginModule");

    static public boolean OTHER_KRB5_LOGIN_MODULE_AVAILABLE = JavaInfo.isSystemClassAvailable("com.sun.security.auth.module.Krb5LoginModule");

    // Kerberos KDC host name
    static public final String KRB5_KDC = "java.security.krb5.kdc";
    // Kerberos Realm name
    static public final String KRB5_REALM = "java.security.krb5.realm";
    // Kerberos configuration file
    static public final String KRB5_CONF = "java.security.krb5.conf";
    // Kerberos keytab file
    static public final String KRB5_KTNAME = "KRB5_KTNAME";

    static public final String USE_SUBJECT_CREDS_ONLY = "javax.security.auth.useSubjectCredsOnly";

    static public final String KRB5_NAME = "javax.security.auth.login.name";
    static public final String KRB5_PWD = "javax.security.auth.login.password";
    static public final String IBM_KRB5_PRINCIPAL = "com.ibm.security.krb5.principal";
    static public final String SUN_KRB5_PRINCIPAL = "sun.security.krb5.principal";
    static public String KRB5_PRINCIPAL = IBM_KRB5_PRINCIPAL;

    /**
     * This method set the system property if the property is null or property value is not the same with the new value
     *
     * @param propName
     * @param propValue
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static String setPropertyAsNeeded(final String propName, final String propValue) {

        String previousPropValue = (String) java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {
            @Override
            public String run() {
                String oldPropValue = System.getProperty(propName);
                if (propValue == null) {
                    System.clearProperty(propName);
                } else if (!propValue.equalsIgnoreCase(oldPropValue)) {
                    System.setProperty(propName, propValue);
                }
                return oldPropValue;
            }
        });
        if (tc.isDebugEnabled())
            Tr.debug(tc, propName + " property previous: " + ((previousPropValue != null) ? previousPropValue : "<null>") + " and now: " + propValue);

        return previousPropValue;
    }

    /**
     * This method restore the property value to the original value.
     *
     * @param propName
     * @param oldPropValue
     * @param newPropValue
     */
    public static void restorePropertyAsNeeded(final String propName, final String oldPropValue, final String newPropValue) {
        java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<Object>() {
            @Override
            public Object run() {
                if (oldPropValue == null) {
                    System.clearProperty(propName);
                } else if (!oldPropValue.equalsIgnoreCase(newPropValue)) {
                    System.setProperty(propName, oldPropValue);
                }
                return null;
            }
        });
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Restore property " + propName + " to previous value: " + oldPropValue);
    }

    @Trivial
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static String getSystemProperty(final String propName) {
        String value = (String) java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {
            @Override
            public Object run() {
                return System.getProperty(propName);
            }
        });

        return value;
    }

    @Trivial
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void setSystemProperty(final String propName, final String propValue) {
        java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {
            @Override
            public Object run() {
                if (propValue == null)
                    System.clearProperty(propName);
                else
                    System.setProperty(propName, propValue);

                return null;
            }
        });
    }

    public static void debugKrb5LoginModule(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        String NULL = "null";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Krb5LoginModule ==> ",
                     "       subject: " + (subject == null ? NULL : subject.toString()),
                     "       sharedState: " + (sharedState == null ? NULL : sharedState.toString()),
                     "       options: " + (options == null ? NULL : options.toString()),
                     "       " + KRB5_NAME + ": " + getSystemProperty(KRB5_NAME),
                     "       " + KRB5_PRINCIPAL + ": " + getSystemProperty(KRB5_PRINCIPAL),
                     "       " + USE_SUBJECT_CREDS_ONLY + ": " + getSystemProperty(USE_SUBJECT_CREDS_ONLY),
                     "       " + KRB5_KDC + ": " + getSystemProperty(KRB5_KDC),
                     "       " + KRB5_REALM + ": " + getSystemProperty(KRB5_REALM),
                     "       " + KRB5_CONF + ": " + getSystemProperty(KRB5_CONF),
                     "       " + KRB5_KTNAME + ": " + getSystemProperty(KRB5_KTNAME));
        }
    }

    static {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Jdk vendor: " + JavaInfo.vendor() + " and major version: " + JavaInfo.majorVersion());
        }
        if (OTHER_KRB5_LOGIN_MODULE_AVAILABLE) {
            KRB5_PRINCIPAL = SUN_KRB5_PRINCIPAL;
        }

        try {
            KRB5_MECH_OID = new Oid("1.2.840.113554.1.2.2");
            SPNEGO_MECH_OID = new Oid("1.3.6.1.5.5.2");
        } catch (GSSException ex) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Unexpected GSSExecption: " + ex);
            }
        }
    }
}
