/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * @version 1.0.0
 */
package io.openliberty.restfulWS.client.security;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxrs20.client.MpJwtPropagation;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(service = OAuthPropagationHelper.class, name = "OAuthPropagationHelper", immediate = true, property = "service.vendor=IBM")
public class OAuthPropagationHelper {
    private static final TraceComponent tc = Tr.register(OAuthPropagationHelper.class);
    public static final String ISSUED_JWT_TOKEN = "issuedJwt"; // new jwt token

    public static final String MP_JSON_WEB_TOKEN_PROPAGATION = "MpJwtPropagation";
    protected final static AtomicServiceReference<MpJwtPropagation> MpJsonWebTokenUtilRef = new AtomicServiceReference<MpJwtPropagation>(MP_JSON_WEB_TOKEN_PROPAGATION);

    @Reference(service = MpJwtPropagation.class, name = MP_JSON_WEB_TOKEN_PROPAGATION, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setMpJwtPropagation(ServiceReference<MpJwtPropagation> ref) {
        MpJsonWebTokenUtilRef.setReference(ref);
    }

    protected void unsetMpJwtPropagation(ServiceReference<MpJwtPropagation> ref) {
        MpJsonWebTokenUtilRef.unsetReference(ref);
    }

    @Activate
    protected void activate(ComponentContext cc) {
        MpJsonWebTokenUtilRef.activate(cc);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "MpJwtPropagation service is activated");
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "OAuthPropagationHelper service is activated");
        }
    }

    @Modified
    protected void modified(Map<String, Object> props) {}

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        MpJsonWebTokenUtilRef.deactivate(cc);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "MpJwtPropagation service is deactivated");
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "OAuthPropagationHelper service is activated");
        }
    }

    public static String getMpJsonWebToken() {
        if (MpJsonWebTokenUtilRef.getService() != null) {
            return MpJsonWebTokenUtilRef.getService().getJsonWebTokenPrincipal(getRunAsSubject());
        }
        else {
            String msg = Tr.formatMessage(tc, "warn_mpjwt_prop_service_notavail");
            Tr.warning(tc, msg);
        }
        return null;
    }

    /**
     * Get the type of access token which the runAsSubject authenticated
     *
     * @return the Type of Token, such as: Bearer
     */
    public static String getAccessTokenType() {
        return getSubjectAttributeString("token_type", true);
    }

    public static String getAccessToken() {
        return getSubjectAttributeString("access_token", true);
    }

//    public static String getJwtToken() {
//        return getSubjectAttributeString("id_token", true);
//    }

    public static String getJwtToken() throws Exception {
        String jwt = getIssuedJwtToken();
        if (jwt == null) {
            jwt = getAccessToken(); // the one that the client received
            if (!isJwt(jwt)) {
                jwt = null;
            }
        }
        return jwt;
    }

    private static boolean isJwt(String jwt) {
        if (jwt != null && jwt.indexOf(".") >= 0) {
            return true;
        }
        return false;
    }

    public static String getIssuedJwtToken() throws Exception {
        return getSubjectAttributeString(ISSUED_JWT_TOKEN, true); // the newly issued token
    }

    public static String getScopes() {
        return getSubjectAttributeString("scope", true);
    }

    static Subject getRunAsSubject() {
        try {
            return WSSubject.getRunAsSubject();
        } catch (WSSecurityException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception while getting runAsSubject:", e.getCause());
            }
            // OIDC_FAILED_RUN_AS_SUBJCET=CWWKS1772W: An exception occurred while attempting to get RunAsSubject. The exception was: [{0}]
            Tr.warning(tc, "failed_run_as_subject", e.getLocalizedMessage());
        }
        return null;
    }

    /**
     * @param string
     * @return
     */
    static String getSubjectAttributeString(String attribKey, boolean bindWithAccessToken) {
        Subject runAsSubject = getRunAsSubject();
        if (runAsSubject != null) {
            return getSubjectAttributeObject(runAsSubject, attribKey, bindWithAccessToken);
        }
        return null;
    }

    /**
     * @param runAsSubject
     * @param attribKey
     * @return object
     */
    @FFDCIgnore({ PrivilegedActionException.class })
    static String getSubjectAttributeObject(Subject subject, String attribKey, boolean bindWithAccessToken) {
        try {
            Set<Object> publicCredentials = subject.getPublicCredentials();
            String result = getCredentialAttribute(publicCredentials, attribKey, bindWithAccessToken, "publicCredentials");
            if (result == null || result.isEmpty()) {
                Set<Object> privateCredentials = subject.getPrivateCredentials();
                result = getCredentialAttribute(privateCredentials, attribKey, bindWithAccessToken, "privateCredentials");
            }
            return result;
        } catch (PrivilegedActionException e) {
            // TODO do we need an error handling in here?
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Did not find a value for the attribute (" + attribKey + ")");
            }
        }
        return null;
    }

    static String getCredentialAttribute(final Set<Object> credentials, final String attribKey, final boolean bindWithAccessToken, final String msg) throws PrivilegedActionException {
        // Since this is only for jaxrs client internal usage, it's OK to override java2 security
        Object obj = AccessController.doPrivileged(
                        new PrivilegedExceptionAction<Object>() {
                            @Override
                            public Object run() throws Exception
                            {
                                int iCnt = 0;
                                for (Object credentialObj : credentials) {
                                    iCnt++;
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                        Tr.debug(tc, msg + "(" + iCnt + ") class:" + credentialObj.getClass().getName());
                                    }
                                    if (credentialObj instanceof Map) {
                                        if (bindWithAccessToken) {
                                            Object accessToken = ((Map<?, ?>) credentialObj).get("access_token");
                                            if (accessToken == null)
                                                continue; // on credentialObj
                                        }
                                        Object value = ((Map<?, ?>) credentialObj).get(attribKey);
                                        if (value != null)
                                            return value;
                                    }
                                }
                                return null;
                            }
                        });
        if (obj != null)
            return obj.toString();
        else
            return null;
    }

}
