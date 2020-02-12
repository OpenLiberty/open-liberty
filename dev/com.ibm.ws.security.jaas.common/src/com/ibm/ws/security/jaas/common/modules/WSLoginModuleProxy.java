/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jaas.common.modules;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.classloading.ClassProvider;
import com.ibm.ws.kernel.boot.security.LoginModuleProxy;
import com.ibm.ws.security.jaas.common.internal.JAASLoginModuleConfigImpl;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;

/**
 * <p>
 * The proxy uses the shared library class loader to load the JAAS custom login module. If
 * there is no shared library class loader, it uses the class loader.
 * </p>
 * 
 * <p>
 * The delegate class is specified in the login module by the option
 * Note: This login module proxy is used only by the custom login module.
 * <code>delegate</code>. An Example:
 * <br>
 * <code>com.ibm.ws.security.authentication.jaas.modules.WSLoginModuleProxy</code>
 * <code>required delegate=mycompany.example.CustomLoginModule;</code>
 * </p>
 * 
 * <p>
 * The proxy will use the thread context class loader if there is one
 * present. Otherwise, it uses the proxy's class loader.
 * </p>
 * 
 * @author IBM Corporation
 * @version 1.0
 * @since 1.0
 */
public class WSLoginModuleProxy extends CommonLoginModule implements LoginModule {
    static final TraceComponent tc = Tr.register(WSLoginModuleProxy.class);
    private LoginModule loginModule;

    /**
     * <p>Constructs a login module proxy.</p>
     */
    public WSLoginModuleProxy() {}

    /**
     * <p>
     * Initialize proxy login module, loads the delegate custom login module
     * </p>
     * 
     * @param subject The subject to be authenticated.
     * @param callbackHandler
     *            A <code>CallbackHandler</code> for communicating with the
     *            end user to gather login information (e.g., username and password).
     * @param sharedState
     *            The state shared with other configured login modules.
     * @param options The options specified in the login configuration for this
     *            particular login module.
     */
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String, ?> sharedState, Map<String, ?> options) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        checkForNoOptions(options);

        String delegateProviderPid = (String) options.get(JAASLoginModuleConfigImpl.DELEGATE_PROVIDER_PID);
        Class<?> target = null;
        if (delegateProviderPid == null) {
            // loaded from shared library
            target = (Class<?>) options.get(JAASLoginModuleConfigImpl.DELEGATE);
        } else {
            // load login module from resourceAdapter or application
            String loginModuleClassName = (String) options.get(JAASLoginModuleConfigImpl.DELEGATE);
            String classProviderFilter = FilterUtils.createPropertyFilter("service.pid", delegateProviderPid);
            BundleContext bundleContext = FrameworkUtil.getBundle(WSLoginModuleProxy.class).getBundleContext();
            ServiceReference<?>[] refs;
            try {
                refs = bundleContext.getServiceReferences("com.ibm.wsspi.application.Application", classProviderFilter);
            } catch (InvalidSyntaxException x) {
                throw new RuntimeException(classProviderFilter, x); // internal error - filter above must be bad
            }
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "found?", classProviderFilter, Arrays.toString(refs));
            if (refs == null) {
                // TODO better error message?
                throw new IllegalStateException(classProviderFilter);
            }
            String classProviderId = (String) refs[0].getProperty("id");
            String extendsFactoryPid = (String) refs[0].getProperty("ibm.extends.source.factoryPid");
            if ("com.ibm.ws.jca.resourceAdapter".equals(extendsFactoryPid)) {
                // load from resource adapter
                classProviderFilter = FilterUtils.createPropertyFilter("id", classProviderId);
                Collection<ServiceReference<ClassProvider>> classProviderRefs;
                try {
                    classProviderRefs = bundleContext.getServiceReferences(ClassProvider.class, classProviderFilter);
                } catch (InvalidSyntaxException x) {
                    throw new RuntimeException(classProviderFilter, x); // internal error - filter above must be bad
                }
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "found resource adapter?", classProviderFilter, Arrays.toString(refs));
                if (classProviderRefs.isEmpty()) {
                    // TODO better error message?
                    throw new IllegalStateException("resourceAdapter " + classProviderId);
                }
                ClassProvider classProvider = bundleContext.getService(classProviderRefs.iterator().next());
                ClassLoader loader = classProvider.getDelegateLoader();
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "about to load " + loginModuleClassName + " with " + loader + " from " + classProvider);
                try {
                    target = loader.loadClass(loginModuleClassName);
                } catch (ClassNotFoundException x) {
                    // TODO better error message?
                    throw new IllegalArgumentException(x);
                }
            } else {
                // load from application
                // TODO
            }
        }

        if (target == null) {
            throw new IllegalArgumentException(Tr.formatMessage(tc, "JAAS_WSLOGIN_MODULE_PROXY_DELEGATE_NOT_SET"));

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "target class: " + target);
        }

        try {
            loginModule = (LoginModule) target.newInstance();
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Un-expect exception:", e);
            }
            throw new RuntimeException(e.getMessage());
        }
        Map<String, Object> origOptions = excludeInternalOption(options);
        checkForNoOptions(options);
        loginModule.initialize(subject, callbackHandler, sharedState, excludeInternalOption(origOptions));
    }

    /**
     * @param origOptions
     */
    private void checkForNoOptions(Map<String, ?> opts) {
        // We add one or two options for custom login module
        if ((opts == null) || (opts.isEmpty())) {
            throw new IllegalArgumentException(Tr.formatMessage(tc, "JAAS_WSLOGIN_MODULE_PROXY_NULL_OPTIONS"));
        }
    }

    /**
     * @param options
     * @return
     */
    private Map<String, Object> excludeInternalOption(Map<String, ?> options) {
        Map<String, Object> cutomLoginModuleOptions = new HashMap<String, Object>();
        cutomLoginModuleOptions.putAll(options);
        cutomLoginModuleOptions.remove(JAASLoginModuleConfigImpl.DELEGATE);
        cutomLoginModuleOptions.remove(JAASLoginModuleConfigImpl.DELEGATE_PROVIDER_PID);
        cutomLoginModuleOptions.remove(LoginModuleProxy.KERNEL_DELEGATE);
        cutomLoginModuleOptions.remove(JAASLoginModuleConfigImpl.WAS_LM_SHARED_LIB);
        return cutomLoginModuleOptions;
    }

    /**
     * <p>
     * Call the delegate object <code>login()</code> method.
     * </p>
     * 
     * @return <code><b>true</b></code> if the authentication succeeded, or <code><b>false</b></code>
     *         if this login module should be ignored
     * @exception LoginException
     *                If the authentication fails.
     */
    @Override
    public boolean login() throws LoginException {
        return loginModule.login();
    }

    /**
     * <p>
     * Call the delegate object <code>commit()</code> method.
     * </p>
     * 
     * @return <code><b>true</b></code> if the commit succeeded, or <code><b>false</b></code>
     *         if this login module should be ignored.
     * @exception LoginException
     *                If the commit fails.
     */
    @Override
    public boolean commit() throws LoginException {
        return loginModule.commit();
    }

    /**
     * <p>
     * Call the delegate object <code>abort()</code> method.
     * </p>
     * 
     * @return <code><b>true</b></code> if the abort succeeded, or <code><b>false</b></code>
     *         if this login module should be ignored.
     * @exception LoginException
     *                If the abort fails.
     */
    @Override
    public boolean abort() throws LoginException {
        return loginModule.abort();
    }

    /**
     * <p>
     * Call the delegate object <code>logout()</code> method.
     * </p>
     * 
     * @return <code><b>true</b></code> if the logout succeeded, or <code><b>false</b></code>
     *         if this logout module should be ignored.
     * @exception LoginException
     *                If the logout fails.
     */
    @Override
    public boolean logout() throws LoginException {
        return loginModule.logout();
    }

    /** {@inheritDoc} */
    protected Callback[] getRequiredCallbacks(CallbackHandler callbackHandler) throws IOException, UnsupportedCallbackException {
        return null;
    }

}
