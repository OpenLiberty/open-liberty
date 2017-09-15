/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.client.jaas.modules;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.clientcontainer.metadata.CallbackHandlerProvider;
import com.ibm.ws.security.client.internal.jaas.JAASClientConfigurationImpl;
import com.ibm.ws.security.client.internal.jaas.JAASClientService;
import com.ibm.ws.security.jaas.common.modules.CommonLoginModule;

/**
 * <p>
 * WebSphere login module to authenticate the principal using JAAS authentication
 * mechanism. This login module honors the <code>CallbackHandler</code> specified
 * in the Client Container deployment descriptor. If there is a <code>CallbackHandler</code>
 * specified in the Client Container deployment descriptor, it will be used by this
 * login module to gather information and ignore the one passed as argument in the
 * <code>LoginContext</code>.
 * </p>
 * 
 * <p>
 * This login module is very similar to the <code>WSLoginModuleImpl</code>, except
 * it enforces the requirement of the Client Container. The requirements on callbacks
 * is the same as <code>WSLoginModuleImpl</code>. Similar, a <code>WSCredential</code>
 * and <code>WSPrincipal</code> are created and associated with the Subject after
 * successful authentication.
 * </p>
 * 
 * @author IBM Corporation
 * @version 1.0
 * @since 1.0
 */
public class WSClientLoginModuleImpl extends CommonLoginModule {

    private static final TraceComponent tc = Tr.register(WSClientLoginModuleImpl.class);

    /**
     * <p>Initialize this login module.</p>
     * 
     * <p>
     * This is called by the <code>LoginContext</code> after this login module is
     * instantiated. The relevant information is passed from the <code>LoginContext</code>
     * to this login module. If the login module does not understand any of the data
     * stored in the <code>sharedState</code> and <code>options</code> parameters,
     * they can be ignored.
     * </p>
     * 
     * @param subject The subject to be authenticated.
     * @param callbackandler A <code>CallbackHandler</code> for communicating with the end user to gather login information (e.g., username and password).
     *            N.B.: this callbackhandler is ignored if the client application specifies one in its DD (application-client.xml) and this login module
     *            was not specified on the WSLogin login context entry.
     * @param sharedState The state shared with other configured login modules.
     * @param options The options specified in the login configuration for this particular login module.
     */
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandlerFromLoginContextEntry, Map<String, ?> sharedState, Map<String, ?> options) {
        CallbackHandler callbackHandlerFromDD = null;
        CallbackHandler callbackHandler = null;
        Boolean ignoreCallbackFromDD = false;

        //get option to determine whether to look for callback handler in the DD
        if (options != null) {
            ignoreCallbackFromDD = (Boolean) options.get(JAASClientConfigurationImpl.WAS_IGNORE_CLIENT_CONTAINER_DD);
        }
        if (ignoreCallbackFromDD != null && ignoreCallbackFromDD) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "CallbackHandler passed from LoginContext constructor is used because ignore option is set on the login module.");
            }
            callbackHandler = callbackHandlerFromLoginContextEntry;
        }
        else {
            CallbackHandlerProvider cbhProvider = JAASClientService.getCallbackHandlerProvider();
            if (cbhProvider != null) {
                callbackHandlerFromDD = cbhProvider.getCallbackHandler();
            }
            if (callbackHandlerFromDD != null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Client Container Deployment Descriptor's CallbackHandler is used: " + callbackHandlerFromDD);
                }
                callbackHandler = callbackHandlerFromDD;
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc,
                             "CallbackHandler passed from LoginContext constructor is used because no CallbackHanlder was specified in the Client Container Deployment Descriptor.");
                }
                callbackHandler = callbackHandlerFromLoginContextEntry;
            }
        }
        super.initialize(subject, callbackHandler, sharedState, options);
    }

    /** {@inheritDoc} */
    @Override
    public boolean login() throws LoginException {
        Subject basicAuthSubject = JAASClientService.getClientAuthenticationService().authenticate(callbackHandler, subject);

        setUpSubject(basicAuthSubject);

        setAlreadyProcessed();
        return true;
    }
}