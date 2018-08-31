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
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.krb5.Krb5Common;

public class Krb5LoginModuleWrapper implements LoginModule {
    private static final TraceComponent tc = Tr.register(Krb5LoginModuleWrapper.class);

    public CallbackHandler callbackHandler;
    public Subject subject;
    public Map<String, Object> sharedState;
    public Map<String, Object> options;
    public Subject temporarySubject;

    Class<?> cls = null;
    Class noparams[] = {};
    Method method;

    Object krb5loginModule = null;

    /**
     * <p>Construct an uninitialized Krb5LoginModuleWrapper object.</p>
     */
    public Krb5LoginModuleWrapper() {
        String targetClass = null;
        if (Krb5Common.isIBMJdk18Lower)
            targetClass = JaasLoginConfigConstants.COM_IBM_SECURITY_AUTH_MODULE_KRB5LOGINMODULE;
        else if (Krb5Common.isJdk11Up)
            targetClass = JaasLoginConfigConstants.COM_SUN_SECURITY_AUTH_MODULE_KRB5LOGINMODULE;
        else {
            //TODO: NLS msg
            Tr.error(tc, "Not support JDK vendor and/or version");
        }
        if (targetClass != null) {
            cls = getClassForName(targetClass);

            try {
                krb5loginModule = cls.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        Object useKeytabValue = null;
        this.callbackHandler = callbackHandler;
        this.subject = subject;
        this.sharedState = (Map<String, Object>) sharedState;
        this.options = new HashMap();
        this.options.putAll(options);

//        if (Krb5Common.isIBMJdk18Lower)
//            useKeytabValue = options.get("useKeytab");
        if (Krb5Common.isJdk11Up)
            useKeytabValue = options.get("useKeyTab");

        if (useKeytabValue != null && useKeytabValue.equals("true")) {
            this.options.put("keyTab", getSystemProperty("KRB5_KTNAME"));
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            this.options.put("debug", "true");
            Tr.debug(tc, "Final options: " + this.options.toString());
        }

        Class[] params = new Class[4];
        params[0] = Subject.class;
        params[1] = CallbackHandler.class;
        params[2] = Map.class;
        params[3] = Map.class;

        if (cls == null) {
            //TODO: error msg:
            return;
        }
        try {
            method = cls.getDeclaredMethod("initialize", params);
            method.invoke(krb5loginModule, subject, null, sharedState, this.options);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean login() throws LoginException {
        inVokeMethod("login", noparams);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean commit() throws LoginException {
        inVokeMethod("commit", noparams);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean abort() throws LoginException {
        inVokeMethod("abort", noparams);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean logout() throws LoginException {
        inVokeMethod("logout", noparams);
        return true;
    }

    /**
    *
    */
    private void inVokeMethod(String methodName, Class[] params) {
        if (cls == null) {
            //TODO: error msg:
            return;
        }
        try {
            method = cls.getDeclaredMethod(methodName, params);
            method.invoke(krb5loginModule);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
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
        ClassLoader contextClassLoader = null;
        try {
            cl = Class.forName(tg);
        } catch (ClassNotFoundException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception performing class for name.", e);
            }
        }
        return cl;
    }
}
