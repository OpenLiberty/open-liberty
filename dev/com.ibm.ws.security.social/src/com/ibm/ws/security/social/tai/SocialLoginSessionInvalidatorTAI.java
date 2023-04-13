/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.tai;

import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WebTrustAssociationException;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.OidcSessionInfo;
import com.ibm.ws.security.openidconnect.clients.common.OidcSessionUtils;
import com.ibm.ws.security.social.Constants;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.OidcLoginConfigImpl;
import com.ibm.ws.security.social.internal.utils.SocialTaiRequest;
import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;

/**
 * This TAI is ran before SSO and is used only to check if the oidc session 
 * has been invalidated for the social login flow.
 * 
 * If the oidc session has been invalidated, it will log out the session.
 * 
 */
public class SocialLoginSessionInvalidatorTAI implements TrustAssociationInterceptor {

    public static final TraceComponent tc = Tr.register(SocialLoginSessionInvalidatorTAI.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    
    TAIWebUtils taiWebUtils = new TAIWebUtils();
    TAIRequestHelper taiRequestHelper = new TAIRequestHelper();
    
    private static boolean issuedBetaMessage = false;

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) {
        // Do nothing for now.
    }

    @Modified
    protected void modified(ComponentContext cc, Map<String, Object> props) {
        // Do nothing for now.
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        // Do nothing for now.
    }

    /**
     * The logic to check and logout invalidated sessions is
     * put into this method, so none of the side effects of TAI's are run.
     * 
     * i.e., the TAI result is not relevant for this use case,
     * but we need to setup a TAI for the social login flow to work.
     */
    @Override
    public boolean isTargetInterceptor(HttpServletRequest req) throws WebTrustAssociationException {
        if (!isRunningBetaMode()) {
            return false;
        }
        SocialTaiRequest socialTaiRequest = taiRequestHelper.createSocialTaiRequestAndSetRequestAttribute(req);
        taiRequestHelper.requestShouldBeHandledByTAI(req, socialTaiRequest);
        logoutIfSessionInvalidated(req);
        return false;
    }
    
    boolean isRunningBetaMode() {
        if (!ProductInfo.getBetaEdition()) {
            return false;
        } else {
            // Running beta exception, issue message if we haven't already issued one for this class
            if (!issuedBetaMessage) {
                Tr.info(tc, "BETA: A beta method has been invoked for the class " + this.getClass().getName() + " for the first time.");
                issuedBetaMessage = !issuedBetaMessage;
            }
            return true;
        }
    }

    private void logoutIfSessionInvalidated(HttpServletRequest request) {
        SocialTaiRequest socialTaiRequest = (SocialTaiRequest) request.getAttribute(Constants.ATTRIBUTE_TAI_REQUEST);
        if (socialTaiRequest == null) {
            // Should not be null
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Request is missing " + Constants.ATTRIBUTE_TAI_REQUEST + " attribute.");
            }
            return;
        }

        SocialLoginConfig clientConfig = null;
        try {
            clientConfig = socialTaiRequest.getTheOnlySocialLoginConfig();
        } catch (SocialLoginException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "A unique social login config wasn't found for this request. Exception was " + e.getMessage());
            }
            return;
        }
        if (clientConfig == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Client config for request could not be found. An error must have occurred initializing this request.");
            }
            return;
        }
        if (!(clientConfig instanceof ConvergedClientConfig)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Client config not an instance of ConvergedClientConfig.");
            }
            return;
        }
        
        OidcSessionInfo sessionInfo = OidcSessionInfo.getSessionInfo(request, (ConvergedClientConfig) clientConfig);
        if (sessionInfo == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Session info could not be retrieved from client cookies.");
            }
            return;
        }

        if (clientConfig instanceof OidcLoginConfigImpl) {
            OidcSessionUtils.logoutIfSessionInvalidated(request, sessionInfo, (OidcLoginConfigImpl) clientConfig);
        }
    }
    
    @Override
    public TAIResult negotiateValidateandEstablishTrust(HttpServletRequest request, HttpServletResponse response) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public int initialize(Properties props) throws WebTrustAssociationFailedException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getVersion() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void cleanup() {
        // TODO Auto-generated method stub
    }

}
