/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.jaas.modules;

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
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.krb5.Krb5Common;

public class Krb5LoginModuleWrapper implements LoginModule {
    private static final TraceComponent tc = Tr.register(Krb5LoginModuleWrapper.class);

    public CallbackHandler callbackHandler;
    public Subject subject;
    public Map<String, Object> sharedState;
    public Map<String, Object> options;
    public Subject temporarySubject;

    Class<?> krb5LoginModuleClass = null;
    Class noparams[] = {};
    Method method;

    Object krb5loginModule = null;
    boolean login_called = false;

    /**
     * <p>Construct an uninitialized Krb5LoginModuleWrapper object.</p>
     */
    public Krb5LoginModuleWrapper() {
        String targetClass = null;
        if (Krb5Common.isIBMJdk18OrLower)
            targetClass = JaasLoginConfigConstants.COM_IBM_SECURITY_AUTH_MODULE_KRB5LOGINMODULE;
        else if (Krb5Common.isOtherSupportJDKs)
            targetClass = JaasLoginConfigConstants.COM_SUN_SECURITY_AUTH_MODULE_KRB5LOGINMODULE;
        else {
            //TODO: NLS msg
            Tr.error(tc, "Not support JDK vendor and/or version");
        }

        if (targetClass != null) {
            krb5LoginModuleClass = getClassForName(targetClass);
            try {
                krb5loginModule = krb5LoginModuleClass.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        Object useKeytabValue = null;
        this.callbackHandler = callbackHandler;
        this.subject = subject;
        this.sharedState = (Map<String, Object>) sharedState;
        this.options = new HashMap();
        this.options.putAll(options);

        if (Krb5Common.isOtherSupportJDKs)
            useKeytabValue = options.get("useKeyTab");

        if (useKeytabValue != null && useKeytabValue.equals("true") && options.get("keyTab") == null) {
            this.options.put("keyTab", getSystemProperty("KRB5_KTNAME"));
        }
//        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//            this.options.put("debug", "true");
//            Krb5Common.debugKrb5LoginModule(subject, callbackHandler, sharedState, this.options);
//        }

        Class[] params = new Class[4];
        params[0] = Subject.class;
        params[1] = CallbackHandler.class;
        params[2] = Map.class;
        params[3] = Map.class;

        if (krb5LoginModuleClass == null) {
            Tr.error(tc, "Not a supported JDK vendor and/or version in Krb5LoginModuleWrapper");
        }
        try {
            method = krb5LoginModuleClass.getDeclaredMethod("initialize", params);
            method.invoke(krb5loginModule, subject, null, sharedState, this.options);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean login() throws LoginException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Krb5Common.debugKrb5LoginModule(subject, callbackHandler, sharedState, this.options);
        }
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

    @FFDCIgnore({ java.lang.reflect.InvocationTargetException.class })
    private void inVokeMethod(String methodName, Class[] params) throws LoginException {
        if (krb5LoginModuleClass != null) {
            try {
                method = krb5LoginModuleClass.getDeclaredMethod(methodName, params);
                method.invoke(krb5loginModule);
            } catch (NoSuchMethodException e) {
                throw new AuthenticationException(e.getLocalizedMessage(), e);
            } catch (SecurityException e) {
                throw new AuthenticationException(e.getLocalizedMessage(), e);
            } catch (IllegalAccessException e) {
                throw new AuthenticationException(e.getLocalizedMessage(), e);
            } catch (IllegalArgumentException e) {
                throw new AuthenticationException(e.getLocalizedMessage(), e);
            } catch (InvocationTargetException e) {
                throw new AuthenticationException(e.getLocalizedMessage(), e);
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

    private Class<?> getClassForName(String tg) {
        Class<?> cl = null;
        try {
            cl = Class.forName(tg);
        } catch (ClassNotFoundException e) {
            Tr.error(tc, "Exception performing class for name.", e.getLocalizedMessage());
        }
        return cl;
    }
}
