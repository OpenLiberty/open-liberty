/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.security.krb5;

import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.ws.kernel.service.util.JavaInfo.Vendor;

/**
 * SpnegoTokenHelper
 * - utilities to help create a SPNEGO Token as Authorization header for outbound authentication purposes
 *
 * @author IBM Corporation
 * @version 1.1
 * @since 1.0
 * @ibm-api
 *
 */
public class Krb5Common {
    private static final TraceComponent tc = Tr.register(Krb5Common.class);

    // Is IBM JDK 1.8 or lower
    static public boolean isIBMJdk18Lower = (JavaInfo.vendor() == Vendor.IBM && JavaInfo.majorVersion() <= 8);
    // Is IBM, Oracle and Open JDK 11 or higher
    static public boolean isJdk11Up = JavaInfo.majorVersion() >= 11;
    // SPNEGO support IBM JDK 8 and lower and JDK 11 or higher
    static public boolean isSupportJDK = isIBMJdk18Lower || isJdk11Up;

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
    static public final String KRB5_PRINCIPAL = "sun.security.krb5.principal";

    // SPNEGO mechanism OID
    static public Oid SPNEGO_MECH_OID;

    // Kerberos mechanism OID
    static public Oid KRB5_MECH_OID;

    public Krb5Common() {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Jdk vendor: " + JavaInfo.vendor());
            Tr.debug(tc, "Jdk major version: " + JavaInfo.majorVersion());
        }
    }

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
                String oldValue = System.getProperty(propName);
                if (oldValue == null || !oldValue.equalsIgnoreCase(propValue)) {
                    System.setProperty(propName, propValue);
                }
                return oldValue;
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void setSystemProperty(final String propName, final String propValue) {
        java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {
            @Override
            public Object run() {
                return System.setProperty(propName, propValue);
            }
        });
    }

    static {
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
