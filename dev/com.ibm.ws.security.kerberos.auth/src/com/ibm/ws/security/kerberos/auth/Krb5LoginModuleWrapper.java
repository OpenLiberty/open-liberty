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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import com.ibm.ws.security.authentication.AuthenticationException;

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
    private static final boolean isIBMJdk8 = (JavaInfo.vendor() == Vendor.IBM || isIBMLoginModuleAvailable())
                                             && JavaInfo.majorVersion() <= 8;
    private static final Class<?>[] noparams = {};

    public CallbackHandler callbackHandler;
    public Subject subject;
    public Map<String, Object> sharedState;
    public Map<String, Object> options;
    public Subject temporarySubject;

    private final Class<?> krb5LoginModuleClass;
    private final Object krb5loginModule;
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
        options = new HashMap<>();
        options.putAll(opts);

        if (!isIBMJdk8)
            useKeytabValue = options.get("useKeyTab");

        if (isIBMJdk8) {
            // Sanitize any OpenJDK-only config options
            if (options.containsKey("isInitiator")) {
                String isInitiator = (String) options.remove("isInitiator");
                if ("true".equalsIgnoreCase(isInitiator)) {
                    options.put("credsType", "both");
                }
            }
            if (options.containsKey("doNotPrompt")) {
                options.remove("doNotPrompt");
            }
            if (options.containsKey("refreshKrb5Config")) {
                options.remove("refreshKrb5Config");
            }
            if (options.containsKey("keyTab")) {
                String keytab = (String) options.remove("keyTab");
                options.remove("useKeyTab");
                options.put("useKeytab", keytab);
            }
        }

        if (useKeytabValue != null && useKeytabValue.equals("true") && options.get("keyTab") == null) {
            options.put("keyTab", getSystemProperty("KRB5_KTNAME"));
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            options.put("debug", "true");
        }

        try {
            Method initializeMethod = krb5LoginModuleClass.getDeclaredMethod("initialize",
                                                                             new Class<?>[] { Subject.class, CallbackHandler.class, Map.class, Map.class });
            initializeMethod.invoke(krb5loginModule, subject, null, sharedState, this.options);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException)
                throw (RuntimeException) cause;
            else
                throw new RuntimeException(cause);
        }
    }

    @Override
    public boolean login() throws LoginException {
        if (krb5LoginModuleClass != null) {
            inVokeMethod("login", noparams);
            login_called = true;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean commit() throws LoginException {
        if (login_called)
            inVokeMethod("commit", noparams);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean abort() throws LoginException {
        if (login_called)
            inVokeMethod("abort", noparams);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean logout() throws LoginException {
        if (login_called)
            inVokeMethod("logout", noparams);
        return true;
    }

    private void inVokeMethod(String methodName, Class<?>[] params) throws LoginException {
        if (krb5LoginModuleClass != null) {
            try {
                Method method = krb5LoginModuleClass.getDeclaredMethod(methodName, params);
                method.invoke(krb5loginModule);
            } catch (InvocationTargetException e) {
                Exception cause = e;
                if (e.getCause() instanceof Exception)
                    cause = (Exception) e.getCause();
                throw new AuthenticationException(e.getCause().getLocalizedMessage(), cause);
            } catch (Exception e) {
                throw new AuthenticationException(e.getLocalizedMessage(), e);
            }
        }
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

    private static Class<?> getClassForName(String tg) {
        try {
            return Class.forName(tg);
        } catch (ClassNotFoundException e) {
            Tr.error(tc, "Exception performing class for name.", e.getLocalizedMessage());
            throw new IllegalStateException(e);
        }
    }
}
