/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.kerberos.auth;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.ws.kernel.service.util.JavaInfo.Vendor;

public class Krb5LoginModuleWrapper implements LoginModule {
    private static final TraceComponent tc = Tr.register(Krb5LoginModuleWrapper.class);

    public static final String COM_IBM_SECURITY_AUTH_MODULE_KRB5LOGINMODULE = "com.ibm.security.auth.module.Krb5LoginModule";
    public static final String COM_SUN_SECURITY_AUTH_MODULE_KRB5LOGINMODULE = "com.sun.security.auth.module.Krb5LoginModule";
    public static final String COM_SUN_SECURITY_JGSS_KRB5_INITIATE = "com.sun.security.jgss.krb5.initiate";
    public static final String COM_SUN_SECURITY_JGSS_KRB5_ACCEPT = "com.sun.security.jgss.krb5.accept";

    @FFDCIgnore(Throwable.class)
    private static boolean isIBMLoginModuleAvailable() {
        try {
            Class.forName(COM_IBM_SECURITY_AUTH_MODULE_KRB5LOGINMODULE);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    // Cannot rely purely on JavaInfo.vendor() because IBM JDK 8 for Mac OS reports vendor = Oracle and only has some IBM API available
    private static final boolean isIBMJdk8 = JavaInfo.majorVersion() <= 8 &&
                                             (JavaInfo.vendor() == Vendor.IBM || isIBMLoginModuleAvailable());

    public CallbackHandler callbackHandler;
    public Subject subject;
    public Map<String, Object> sharedState;
    public Map<String, Object> options;

    private final Class<? extends LoginModule> krb5LoginModuleClass;
    private final LoginModule krb5loginModule;
    private boolean login_called = false;

    /**
     * <p>Construct an uninitialized Krb5LoginModuleWrapper object.</p>
     */
    public Krb5LoginModuleWrapper() {
        String targetClass = isIBMJdk8 //
                        ? COM_IBM_SECURITY_AUTH_MODULE_KRB5LOGINMODULE //
                        : COM_SUN_SECURITY_AUTH_MODULE_KRB5LOGINMODULE;
        if (TraceComponent.isAnyTracingEnabled()) {
            Tr.debug(tc, "Using target class: " + targetClass);
        }

        krb5LoginModuleClass = getClassForName(targetClass);
        try {
            krb5loginModule = krb5LoginModuleClass.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> opts) {
        Object useKeytabValue = null;
        this.callbackHandler = callbackHandler;
        this.subject = subject;
        this.sharedState = (Map<String, Object>) sharedState;
        this.options = new HashMap<>(opts);

        final String IBM_JDK_USE_KEYTAB = "useKeytab"; // URL
        final String OPENJDK_USE_KEYTAB = "useKeyTab"; // boolean

        if (!isIBMJdk8)
            useKeytabValue = options.get(OPENJDK_USE_KEYTAB);

        if (isIBMJdk8) {
            // Sanitize any OpenJDK-only config options

            //Remove OpenJDK-only style options.
            options.remove("clearPass");
            options.remove("isInitiator");
            options.remove("refreshKrb5Config");
            options.remove(OPENJDK_USE_KEYTAB);

            //Remove and save OpenJDK-only style options.
            boolean doNotPrompt = Boolean.valueOf((String) options.remove("doNotPrompt"));
            boolean useTicketCache = Boolean.valueOf((String) options.remove("useTicketCache"));
            String ticketCache = (String) options.remove("ticketCache");
            String keytab = (String) options.remove("keyTab");

            //Enable noninteractive keyTab login to start with (requires useKeytab or useDefaultKeytab to also be set).
            options.put("credsType", "both");

            /*
             * Taken from:
             * https://www.ibm.com/support/knowledgecenter/SSYKE2_8.0.0/com.ibm.java.security.api.80.doc/jgss/com/ibm/security/auth/module/Krb5LoginModule.html
             * The keytab and ccache options take precedence over tryFirstPass.
             *
             * Taken from:
             * https://www.ibm.com/support/knowledgecenter/ssw_ibm_i_71/rzaha/rzahajgssusejaas20.htm
             *
             * The login proceeds noninteractively when you specify the credential type as initiator (credsType=initiator) and
             * you perform one of the following actions:
             *
             * Specify the useCcache option
             * Set the useDefaultCcache option to true
             *
             * The login also proceeds noninteractively when you specify the credential type as acceptor or both (credsType=acceptor or credsType=both)
             * and you perform one of the following actions:
             *
             * Specify the useKeytab option
             * Set the useDefaultKeytab option to true
             *
             * Interactive logins:
             * Other configurations result in the login module prompting for a principal name and password so that it may obtain a TGT from a Kerberos KDC.
             * The login module prompts for only a password when you specify the principal option.
             */
            if (!doNotPrompt) {
                //A password was provided so enable interactive login.
                options.put("useFirstPass", "true");
            } else if (useTicketCache) {
                options.put("credsType", "initiator"); //Enables noninteractive cCache login
                if (ticketCache != null) {
                    // IBM JDK requires they ticketCache option to be a valid URL
                    String ticketCacheURL = coerceToURL(ticketCache);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Coerced ticketCache path from " + ticketCache + " to " + ticketCacheURL);
                    options.put("useCcache", ticketCacheURL);
                } else {
                    options.put("useDefaultCcache", "true");
                }
            } else if (keytab != null) {
                // IBM JDK requires they keytab option to be a valid URL
                String keytabURL = coerceToURL(keytab);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Coerced keytab path from " + keytab + " to " + keytabURL);
                options.put(IBM_JDK_USE_KEYTAB, keytabURL);
            } else {
                // If no keyTab path specified, still set useDefaultKeytab=true because then the
                // default JDK or default OS locations will be checked
                options.put("useDefaultKeytab", "true");
            }

        }

        if (useKeytabValue != null && useKeytabValue.equals("true") && options.get("keyTab") == null) {
            options.put("keyTab", getSystemProperty("KRB5_KTNAME"));
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            options.put("debug", "true");
        }

        krb5loginModule.initialize(subject, callbackHandler, sharedState, options);
    }

    /**
     * If the login module returns false, throws a LoginException. This is to make the IBM Krb5LoginModule behavior
     * match the Sun Krb5LoginModule.<br><br>
     *
     * {@inheritDoc}
     */
    @Override
    public boolean login() throws LoginException {
        if (!krb5loginModule.login())
            throw new LoginException("Kerberos login failed");
        login_called = true;
        return true;
    }

    /**
     * If the login module returns false, throws a LoginException. This is to make the IBM Krb5LoginModule behavior
     * match the Sun Krb5LoginModule.<br><br>
     *
     * {@inheritDoc}
     */
    @Override
    public boolean commit() throws LoginException {
        if (login_called)
            if (!krb5loginModule.commit())
                throw new LoginException("Kerberos login failed (commit)");
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean abort() throws LoginException {
        if (login_called)
            krb5loginModule.abort();
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean logout() throws LoginException {
        if (login_called)
            krb5loginModule.logout();
        return true;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private String getSystemProperty(final String propName) {
        String value = (String) java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {
            @Override
            public Object run() {
                return System.getProperty(propName);
            }
        });

        return value;
    }

    @FFDCIgnore(MalformedURLException.class)
    private static String coerceToURL(String path) {
        try {
            // If this works we already have a valid URL. Return it.
            new URL(path);
            return path;
        } catch (MalformedURLException e1) {
            try {
                return new File(path).toURI().toURL().toString();
            } catch (MalformedURLException e2) {
                // if we cannot return the path as a URL, return the original path
                // to let IBM JDK handle the error messaging
                return path;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends LoginModule> getClassForName(String tg) {
        try {
            return (Class<? extends LoginModule>) Class.forName(tg);
        } catch (ClassNotFoundException e) {
            Tr.error(tc, "Exception performing class for name.", e.getLocalizedMessage());
            throw new IllegalStateException(e);
        }
    }

}
