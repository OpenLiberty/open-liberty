/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.boot.security.LoginModuleProxy;
import com.ibm.ws.security.jaas.common.TraceConstants;
import com.ibm.ws.security.jaas.common.internal.JAASLoginModuleConfigImpl;

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

        checkForNoOptions(options);

        Class<?> target = (Class<?>) options.get(JAASLoginModuleConfigImpl.DELEGATE);
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
