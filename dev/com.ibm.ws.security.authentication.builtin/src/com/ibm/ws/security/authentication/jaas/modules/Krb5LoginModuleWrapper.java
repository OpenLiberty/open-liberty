package com.ibm.ws.security.authentication.jaas.modules;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.ws.kernel.service.util.JavaInfo.Vendor;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;

public class Krb5LoginModuleWrapper implements LoginModule {
    private static final TraceComponent tc = Tr.register(Krb5LoginModuleWrapper.class);

    static private boolean isIBMJdk18Lower = (JavaInfo.vendor() == Vendor.IBM && JavaInfo.majorVersion() <= 8);
    static private boolean isJdk11Up = JavaInfo.majorVersion() >= 11;

    public CallbackHandler callbackHandler;
    public Subject subject;
    public Map<String, Object> sharedState;
    public Map<String, ?> options;
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
        if (isIBMJdk18Lower)
            targetClass = JaasLoginConfigConstants.COM_IBM_SECURITY_AUTH_MODULE_KRB5LOGINMODULE;
        else if (isJdk11Up)
            targetClass = JaasLoginConfigConstants.COM_SUN_SECURITY_AUTH_MODULE_KRB5LOGINMODULE;
        cls = getClassForName(targetClass);

        try {
            krb5loginModule = cls.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.callbackHandler = callbackHandler;
        this.subject = subject;
        this.sharedState = (Map<String, Object>) sharedState;
        this.options = options;
        Class[] params = new Class[4];
        params[0] = Subject.class;
        params[1] = CallbackHandler.class;
        params[2] = Map.class;
        params[3] = Map.class;

        try {
            method = cls.getDeclaredMethod("initialize", params);
            method.invoke(krb5loginModule, subject, null, sharedState, options);
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
//        String SUN_KRB5_LOGIN_MODULE = "com.sun.security.auth.module.Krb5LoginModule";
//        //Class<?> cls = getClassForName(SUN_KRB5_LOGIN_MODULE);
//        //Krb5LoginModule krb5 = new Krb5LoginModule();
//        Map<String, String> options = new HashMap<String, String>();
//        Map<String, Object> sharedState = new HashMap<String, Object>();
//
//        //TODO need to handle old IBM JDK version and Oracle JDK
//        options.put("isInitiator", "true");
//        options.put("refreshKrb5Config", "true");
//        options.put("doNotPrompt", "true");
//        options.put("storeKey", "true");
//        options.put("principal", delegateSpn);
//        // TODO: get the keytab file fron SPNEGO configuration
//        options.put("useKeyTab", "true");
//        options.put("keyTab", getSystemProperty("KRB5_KTNAME"));
//        if (tc.isDebugEnabled()) {
//            options.put("debug", "true");
//        }
//        sharedState.put("javax.security.auth.login.name", delegateSpn);
//        Class noparams[] = {};
//        Class[] params = new Class[4];
//        params[0] = Subject.class;
//        params[1] = CallbackHandler.class;
//        params[2] = Map.class;
//        params[3] = Map.class;

//        Method method;
        try {
            //Object krb5loginModule = cls.newInstance();

            method = cls.getDeclaredMethod("login", noparams);
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

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean commit() throws LoginException {
        try {
            method = cls.getDeclaredMethod("commit", noparams);
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

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean abort() throws LoginException {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean logout() throws LoginException {
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

    private Class<?> getClassForName(String tg) {
        Class<?> cl = null;
        ClassLoader contextClassLoader = null;
        try {
//            ClassLoader bundleClassLoader = Krb5LoginModuleWrapper.class.getClassLoader();
//            contextClassLoader = classLoadingService.createThreadContextClassLoader(bundleClassLoader);
//            cl = Class.forName(tg, true, contextClassLoader);
            cl = Class.forName(tg);
        } catch (ClassNotFoundException e) {
            //TODO consider different warning/error
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception performing class for name.", e);
            }
        } finally {
//            classLoadingService.destroyThreadContextClassLoader(contextClassLoader);
        }
        return cl;
    }
}
