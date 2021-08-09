/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins.jose4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.claims.UserClaims;
import com.ibm.ws.security.oauth20.TraceConstants;
import com.ibm.ws.security.oauth20.util.ConfigUtils;
import com.ibm.ws.security.wim.VMMService;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;
import com.ibm.wsspi.security.registry.RegistryHelper;
import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.IdentifierType;
import com.ibm.wsspi.security.wim.model.PersonAccount;
import com.ibm.wsspi.security.wim.model.PropertyControl;
import com.ibm.wsspi.security.wim.model.Root;

/**
 * User claims for the id token, token introspection, identity assertion, and resource authorization.
 */
public class OidcUserClaims extends com.ibm.ws.security.common.claims.UserClaims {
    private static final TraceComponent tc = Tr.register(OidcUserClaims.class,
            TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);

    // public static final String USER_CLAIMS_UNIQUE_SECURITY_NAME = "uniqueSecurityName";
    // public static final String USER_CLAIMS_REALM_NAME = "realmName";

    // protected Map<String, Object> claimsMap;
    // protected String groupIdentifier;
    // protected String userName;
    public OidcUserClaims(UserClaims oauthClaims) {
        super(oauthClaims.asMap(), oauthClaims.getUserName(), oauthClaims.getGroupIdentifier());
    }

    /**
     * @param oidcServerConfig
     */
    public void addExtraClaims(OidcServerConfig oidcServerConfig) {
        if (oidcServerConfig.isCustomClaimsEnabled()) {
            Set<String> extraCustomClaims = oidcServerConfig.getCustomClaims();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "extraCustomClaims length: " + extraCustomClaims.size());
            }
            if (extraCustomClaims == null || extraCustomClaims.size() <= 0)
                return;
            Map<String, Object> vmmInfoMap = new HashMap<String, Object>();
            try {
                vmmInfoMap = getUserinfoFromRegistryMap(oidcServerConfig, extraCustomClaims, vmmInfoMap, false);
            } catch (Exception e) {
                // TODO error handling
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "extraCustomClaims get unexpected Exception", e);
                }
            }
            claimsMap.putAll(vmmInfoMap);
        }
    }

    /**
     * Get the JSONObject that will be returned for userinfo endpoint from the user registry
     * @param oidcServerConfig The OidcServerConfig
     * @param claims The claims for this granted access
     * @param inputMap
     * @throws Exception
     * @throws IOException
     *
     */
    public Map<String, Object> getUserinfoFromRegistryMap(OidcServerConfig oidcServerConfig,
            Set<String> claims,
            Map<String, Object> inputMap,
            boolean isJson) throws Exception {
        Map<String, Object> result = inputMap;

        String userName = this.userName;

        VMMService vmmService = ConfigUtils.getVMMService();
        if (vmmService != null) {
            PropertyControl vmmServiceProps = new PropertyControl();
            Properties claimsToVMMProperties = new Properties();
            if (!claims.isEmpty()) {
                Properties claimsToVMMProps = oidcServerConfig.getClaimToUserRegistryMap();
                for (String claim : claims) {
                    String vmmProperty = claimsToVMMProps.getProperty(claim);
                    if (vmmProperty == null) {
                        vmmProperty = claim;
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "claim: " + claim + "  is not mapped to a vmm property, using the claim name as the vmm property name");
                        }
                    } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "claim: " + claim + "  mapped to vmmProperty: " + vmmProperty);
                    }
                    claimsToVMMProperties.put(claim, vmmProperty);
                    vmmServiceProps.getProperties().add(vmmProperty);
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "claimsToVMMProperties: " + claimsToVMMProperties);
                Tr.debug(tc, "getting VMM properties: " + vmmServiceProps.getProperties());
            }
            if (!vmmServiceProps.getProperties().isEmpty()) {
                // Call VMM to get the user's properties
                IdentifierType id = new IdentifierType();
                String uniqueId = RegistryHelper.getUserRegistry(null).getUniqueUserId(userName);
                id.setUniqueName(uniqueId);
                Entity entity = new Entity();
                entity.setIdentifier(id);
                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(vmmServiceProps);
                root = vmmService.get(root);
                PersonAccount person = (PersonAccount) root.getEntities().get(0);
                // Now add the claims/values to the JSON
                for (Entry<Object, Object> e : claimsToVMMProperties.entrySet()) {
                    String claim = (String) e.getKey();
                    String vmmProperty = (String) e.getValue();
                    Object value = person.get(vmmProperty);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "get for claim: " + claim + "  vmmProperty: " + vmmProperty + ", returned: " + value);
                    }
                    String strValue = vmmPropertyToString(value);
                    if (strValue != null && !strValue.isEmpty()) {
                        if (isJson && claim.equals("address")) {
                            JSONObject addressJSON = new JSONObject();
                            addressJSON.put("formatted", strValue);
                            result.put(claim, addressJSON);
                        } else
                            result.put(claim, strValue);
                    }
                }
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "VMM service not available - returning sub and groupIds claims");
            }
        }

        return result;
    }

    /**
     * Get the JSONObject that will be returned for userinfo endpoint from the user registry
     * @param claims The claims for this granted access
     */
    public JSONObject getUserinfoFromRegistry(OidcServerConfig oidcServerConfig,
            JSONObject inputJSON,
            HttpServletRequest request,
            HttpServletResponse response,
            HashSet<String> claims) throws IOException {
        JSONObject responseJSON = inputJSON;
        Map<String, Object> vmmInfoMap = new HashMap<String, Object>();
        try {
            vmmInfoMap = getUserinfoFromRegistryMap(oidcServerConfig, claims, vmmInfoMap, true);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Tr.error(tc, "OIDC_SERVER_USERINFO_INTERNAL_ERROR", new Object[] { e.getMessage(), request.getRequestURI() });
        }

        return mapToJson(vmmInfoMap, responseJSON);
    }

    /**
     * @param vmmInfoMap
     * @param responseJSON
     * @return
     */
    public JSONObject mapToJson(Map<String, Object> map, JSONObject jsonObject) {
        Set<Entry<String, Object>> entries = map.entrySet();
        for (Entry<String, Object> entry : entries) {
            jsonObject.put(entry.getKey(), entry.getValue());
        }
        return jsonObject;
    }

    /**
     * Convert the object to a String. If it's a list create a
     * String of the elements as Strings delimited by blanks.
     *
     * @param value
     * @return
     */
    @SuppressWarnings("rawtypes")
    public String vmmPropertyToString(Object value) {
        String result = null;
        if (value == null || value instanceof String) {
            result = (String) value;
        } else if (value instanceof List) {
            StringBuffer strBuff = null;
            for (Object element : (List) value) {
                String elem = element.toString();
                if (elem != null) {
                    if (strBuff == null) {
                        strBuff = new StringBuffer();
                    } else {
                        strBuff.append(" ");
                    }
                    strBuff.append(elem);
                }
            }
            if (strBuff != null) {
                result = strBuff.toString();
            }
        } else {
            result = value.toString();
        }
        return result;
    }

}
