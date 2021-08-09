/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.tai;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.security.auth.Subject;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.common.crypto.HashUtils;
import com.ibm.ws.security.mp.jwt.MicroProfileJwtConfig;
import com.ibm.ws.security.mp.jwt.TraceConstants;
import com.ibm.ws.security.mp.jwt.error.MpJwtProcessingException;
import com.ibm.ws.security.mp.jwt.impl.utils.JwtPrincipalMapping;
import com.ibm.wsspi.security.token.AttributeNameConstants;

public class TAIMappingHelper {

    private static TraceComponent tc = Tr.register(TAIMappingHelper.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    String INTERNAL_DISABLE_SSO_LTPA_CACHE = "com.ibm.ws.authentication.internal.sso.disable.ltpa.cache";

    @Sensitive
    String decodedTokenPayload = null;
    String username = null;
    String realm = null;
    JwtPrincipalMapping claimToPrincipalMapping = null;
    MicroProfileJwtConfig config = null;
    JsonWebToken jwtPrincipal = null;
    Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
    protected static final String CCK_CLAIM = "sid";
    protected static final String APR_CLAIM = "apr"; // custom auth provider

    TAIJwtUtils taiJwtUtils = new TAIJwtUtils();
    //boolean addJwtPrincipalToSubject = false;

    public TAIMappingHelper(@Sensitive String decodedPayload) throws MpJwtProcessingException {
        decodedTokenPayload = decodedPayload;
        config = null;
        //addJwtPrincipalToSubject = true;
        if (decodedTokenPayload != null) {
            claimToPrincipalMapping = new JwtPrincipalMapping(decodedTokenPayload, "upn", "groups", false);
            setUsername();
            setRealm();
        }
    }

    public TAIMappingHelper(@Sensitive String decodedPayload, MicroProfileJwtConfig clientConfig) throws MpJwtProcessingException {
        String methodName = "<init>";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, decodedPayload, clientConfig);
        }
        decodedTokenPayload = decodedPayload;
        config = clientConfig;
        if (decodedTokenPayload != null) {
            claimToPrincipalMapping = new JwtPrincipalMapping(decodedTokenPayload, config.getUserNameAttribute(), config.getGroupNameAttribute(), config.getMapToUserRegistry());
            setUsername();
            setRealm();
        }
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName);
        }
    }

    /**
     *
     */
    private void setRealm() {
        this.realm = claimToPrincipalMapping.getMappedRealm();

    }

    public void createJwtPrincipalAndPopulateCustomProperties(@Sensitive JwtToken jwtToken, boolean addJwtPrincipal) throws MpJwtProcessingException {
        String methodName = "createJwtPrincipalAndPopulateCustomProperties";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, jwtToken);
        }
        jwtPrincipal = createJwtPrincipal(jwtToken);
        String issuer = getIssuer(jwtPrincipal);
        customProperties = populateCustomProperties(issuer, getmaptoURconfig(), addJwtPrincipal);
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName);
        }
    }

    /**
     * @return
     */
    private boolean getmaptoURconfig() {

        if (config != null) {
            return config.getMapToUserRegistry();
        }
        return false;
    }

    public Subject createSubjectFromCustomProperties(boolean addJwtPrincipalToSubject) {
        String methodName = "createSubjectFromCustomProperties";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, addJwtPrincipalToSubject);
        }
        Subject subject = new Subject();
        //        if (jwt != null) {
        //            subject.getPrivateCredentials().add(jwt);
        //        }

        if (addJwtPrincipalToSubject) {
            subject.getPrincipals().add(jwtPrincipal);
            customProperties.put(AuthenticationConstants.INTERNAL_ASSERTION_KEY, Boolean.TRUE);
        }
        customProperties.put(AuthenticationConstants.INTERNAL_JSON_WEB_TOKEN, jwtPrincipal);

        //        subject.getPrivateCredentials().add(jwtPrincipal);
        subject.getPrivateCredentials().add(customProperties);

        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, subject);
        }
        return subject;
    }

    public String getUsername() {
        return username;
    }

    public Hashtable<String, Object> getCustomProperties() {
        return customProperties;
    }

    public JsonWebToken getJwtPrincipal() {
        return jwtPrincipal;
    }

    void setUsername() throws MpJwtProcessingException {
        if (claimToPrincipalMapping != null) {
            username = claimToPrincipalMapping.getMappedUser();
        }
        if (username == null) {
            String msg = Tr.formatMessage(tc, "USERNAME_NOT_FOUND");
            Tr.error(tc, msg);
            throw new MpJwtProcessingException(msg);
        }
    }

    JsonWebToken createJwtPrincipal(@Sensitive JwtToken jwtToken) {
        String methodName = "createJwtPrincipal";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, jwtToken);
        }
        if (claimToPrincipalMapping == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Claim to principal mapping object not initialized");
            }
            if (tc.isDebugEnabled()) {
                Tr.exit(tc, methodName, null);
            }
            return null;
        }
        JsonWebToken token = taiJwtUtils.createJwtPrincipal(username, claimToPrincipalMapping.getMappedGroups(), jwtToken);
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, token);
        }
        return token;
    }

    String getIssuer(@Sensitive JsonWebToken jwtPrincipal) throws MpJwtProcessingException {
        String methodName = "getIssuer";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, jwtPrincipal);
        }
        if (jwtPrincipal == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "jwtPrincipal is null");
            }
            if (tc.isDebugEnabled()) {
                Tr.exit(tc, methodName, null);
            }
            return null;
        }
        return jwtPrincipal.getIssuer();
    }

    Hashtable<String, Object> populateCustomProperties(String issuer, boolean mapToUR, boolean addJwtPrincipal) {
        String methodName = "populateCustomProperties";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, issuer);
        }
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();

        if (mapToUR) {
            customProperties.put(AuthenticationConstants.INTERNAL_ASSERTION_KEY, Boolean.TRUE);
            customProperties.put(AttributeNameConstants.WSCREDENTIAL_USERID, username);
        } else {
            if (realm == null && issuer != null) {
                realm = getRealm(issuer);
            }
            String uniqueID = getUniqueId(realm);
            List<String> groupswithrealm = getGroupsWithRealm(realm);

            customProperties.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, uniqueID);
            if (realm != null && !realm.isEmpty()) {
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_REALM, realm);
            }
            if (!groupswithrealm.isEmpty()) {
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_GROUPS, groupswithrealm);
            }
            customProperties.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, username);

        }

        if (!addJwtPrincipal) {
            // add this only in the mpjwt tai flow
            addCustomCacheKey(customProperties);
        } else {
            // jwtsso flow
            // copy cache key and amr claims from the token to the custom properties, so that the new subject
            // will have these values.
            // TODO look at realm
            addCustomClaimsFromToken(customProperties);
        }

        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, customProperties);
        }

        return customProperties;
    }

    public void addDisableSsoLtpaCacheProp() {
        customProperties.put(INTERNAL_DISABLE_SSO_LTPA_CACHE, true);
    }

    /**
     * @param customProperties
     */
    private void addCustomClaimsFromToken(Hashtable<String, Object> customProperties) {
        String cck = getCustomCacheKey();
        if (cck != null) {
            customProperties.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, cck);
        }
        String apr = getCustomAuthProvider();
        if (apr != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "apr claim is set in the token : ", apr);
            }
            customProperties.put(AuthenticationConstants.INTERNAL_AUTH_PROVIDER, apr);
        }
    }

    /**
     * @return
     */
    private String getCustomAuthProvider() {
        if (jwtPrincipal != null) {
            Object apr = jwtPrincipal.getClaim(APR_CLAIM);
            if (apr != null && apr instanceof String) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "apr claim is set in the token : ", (String) apr);
                }
                return (String) apr;
            }
        }
        return null;
    }

    /**
     * @param customProperties
     */
    private void addCustomCacheKey(Hashtable<String, Object> customProperties) {

        if (jwtPrincipal != null) {
            String customCacheKey = HashUtils.digest(jwtPrincipal.getRawToken());
            customProperties.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, customCacheKey);
        }
    }

    /**
     * @param jwtPrincipal
     */
    private String getCustomCacheKey() {

        if (jwtPrincipal != null) {
            Object cck = jwtPrincipal.getClaim(CCK_CLAIM);
            if (cck != null) {
                return (String) cck;
            }
        }
        return null;
    }

    String getRealm(String issuer) {
        String methodName = "getRealm";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, issuer);
        }
        // Default realm to the issuer
        String realm = issuer;
        if (isRealmEndsWithSlash(realm)) {
            realm = updateRealm(realm);
        }

        //        if (realm == null) {
        //            // runtime default
        //            realm = defaultRealm(clientConfig);
        //        }
        //        if (realm == null) {
        //            Tr.error(tc, "REALM_NOT_FOUND", new Object[] {});
        //            return sendToErrorPage(res, TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED));
        //        }
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, realm);
        }
        return realm;
    }

    /**
     * @param realm
     * @return
     */
    private boolean isRealmEndsWithSlash(String realm) {
        return (realm != null && realm.length() > 1 && realm.endsWith("/"));
    }

    /**
     * @param realm
     * @return
     */
    private String updateRealm(String realm) {
        // remove the trailing slash
        return realm.substring(0, realm.length() - 1);
    }

    String getUniqueId(String realm) {
        String methodName = "getUniqueId";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, realm);
        }
        String uniqueUser = null;
        if (claimToPrincipalMapping != null) {
            uniqueUser = claimToPrincipalMapping.getMappedUser();
        }
        String uniqueId = new StringBuffer("user:").append(realm).append("/").append(uniqueUser).toString();
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, uniqueId);
        }
        return uniqueId;
    }

    List<String> getGroupsWithRealm(String realm) {
        String methodName = "getGroupsWithRealm";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, realm);
        }
        List<String> groups = null;
        if (claimToPrincipalMapping != null) {
            groups = claimToPrincipalMapping.getMappedGroups();
        }
        List<String> groupsWithRealm = new ArrayList<String>();
        if (groups != null) {
            for (String groupEntry : groups) {
                String group = new StringBuffer("group:").append(realm).append("/").append(groupEntry).toString();
                groupsWithRealm.add(group);
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, groupsWithRealm);
        }
        return groupsWithRealm;
    }
}
