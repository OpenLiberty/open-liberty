/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.registry;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.jwt.config.JwtConfig;
import com.ibm.ws.security.jwt.utils.JwtUtils;
import com.ibm.ws.security.wim.VMMService;
import com.ibm.wsspi.security.registry.RegistryHelper;
import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.IdentifierType;
import com.ibm.wsspi.security.wim.model.PersonAccount;
import com.ibm.wsspi.security.wim.model.PropertyControl;
import com.ibm.wsspi.security.wim.model.Root;

/**
 * Extra claims that can be fetched from registry for JWT tokens
 */
public class RegistryClaims {
    private static final TraceComponent tc = Tr.register(RegistryClaims.class,
            TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);

    // public static final String USER_CLAIMS_UNIQUE_SECURITY_NAME = "uniqueSecurityName";
    // public static final String USER_CLAIMS_REALM_NAME = "realmName";

    // protected Map<String, Object> claimsMap;
    // protected String groupIdentifier;
    private final String userName;

    // private final Map<String, Object> claimsMap;

    public RegistryClaims(String userName) {
        this.userName = userName;
        // claimsMap = new ConcurrentHashMap<String, Object>();
        // super( oauthClaims.asMap(), oauthClaims.getUserName(), oauthClaims.getGroupIdentifier() );
    }

    public Object fetchClaim(String claim) throws Exception {
        VMMService vmmService = JwtUtils.getVMMService();
        if (vmmService != null) {
            PropertyControl vmmServiceProps = new PropertyControl();
            vmmServiceProps.getProperties().add(claim);
            if (!vmmServiceProps.getProperties().isEmpty()) {
                PersonAccount person = getUser(vmmService, vmmServiceProps);
                Object value = person.get(claim);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "get for claim: " + claim + ", returned: " + value);
                }
                return value;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "get for claim: " + claim + ", returned: null");
        }
        return null;
    }

    /**
     * @param jwtConfig
     */
    public Map<String, Object> fetchExtraClaims(JwtConfig jwtConfig) {
        // if (oidcServerConfig.isCustomClaimsEnabled()) {
        List<String> claimsList = jwtConfig.getClaims();
        Map<String, Object> vmmInfoMap = new HashMap<String, Object>();
        if (!claimsList.isEmpty()) {
            Set<String> extraCustomClaims = new HashSet<String>(claimsList);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "extraCustomClaims length: " + extraCustomClaims.size());
            }
            if (extraCustomClaims == null || extraCustomClaims.size() <= 0) {
                return null;
            }

            try {
                vmmInfoMap = getUserinfoFromRegistryMap(extraCustomClaims, vmmInfoMap, false);
            } catch (Exception e) {
                // TODO error handling
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "extraCustomClaims get unexpected Exception", e);
                }
            }
            // claimsMap.putAll(vmmInfoMap);
        }
        return vmmInfoMap;
    }

    private PersonAccount getUser(VMMService vmmService, PropertyControl vmmServiceProps) throws Exception {
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
        return person;
    }

    /**
     * Get the JSONObject that will be returned for userinfo endpoint from the user registry
     * 
     * @param jwtConfig
     *            The JwtConfig
     * @param claims
     *            The claims for this granted access
     * @param inputMap
     * @throws Exception
     * @throws IOException
     * 
     */
    private Map<String, Object> getUserinfoFromRegistryMap(Set<String> claims,
            Map<String, Object> inputMap,
            boolean isJson) throws Exception {
        Map<String, Object> result = inputMap;

        VMMService vmmService = JwtUtils.getVMMService();
        if (vmmService != null) {
            PropertyControl vmmServiceProps = new PropertyControl();
            Properties claimsToVMMProperties = new Properties();
            if (!claims.isEmpty()) {
                // Properties claimsToVMMProps = jwtConfig.getClaimToUserRegistryMap(); //TODO
                for (String claim : claims) {
                    String vmmProperty = claim;// claimsToVMMProps.getProperty(claim);
                    // if (vmmProperty == null) {
                    // vmmProperty = claim;
                    // if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    // Tr.debug(tc, "claim: " + claim + "  is not mapped to a vmm property, using the claim name as the vmm property name");
                    // }
                    // }
                    // else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    // Tr.debug(tc, "claim: " + claim + "  mapped to vmmProperty: " + vmmProperty);
                    // }

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
                // IdentifierType id = new IdentifierType();
                // String uniqueId = RegistryHelper.getUserRegistry(null).getUniqueUserId(userName);
                // id.setUniqueName(uniqueId);
                // Entity entity = new Entity();
                // entity.setIdentifier(id);
                // Root root = new Root();
                // root.getEntities().add(entity);
                // root.getControls().add(vmmServiceProps);
                // root = vmmService.get(root);
                // PersonAccount person = (PersonAccount) root.getEntities().get(0);
                PersonAccount person = getUser(vmmService, vmmServiceProps);
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
                        result.put(claim, strValue);
                        // if (isJson && claim.equals("address")) {
                        // JSONObject addressJSON = new JSONObject();
                        // addressJSON.put("formatted", strValue);
                        // result.put(claim, addressJSON);
                        // } else {
                        // result.put(claim, strValue);
                        // }
                    }
                }
            }
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "VMM service not available - not returning any extra claims");
            }
        }

        return result;
    }

    /**
	 * Convert the object to a String. If it's a list create a String of the
	 * elements as Strings delimited by blanks.
     * 
     * @param value
     * @return
     */
    @SuppressWarnings("rawtypes")
    public String vmmPropertyToString(Object value) {
        String result = null;
        if (value == null || value instanceof String) {
            result = (String) value;
        }
        else if (value instanceof List) {
            StringBuffer strBuff = null;
            for (Object element : (List) value) {
                String elem = element.toString();
                if (elem != null) {
                    if (strBuff == null) {
                        strBuff = new StringBuffer();
                    }
                    else {
                        strBuff.append(" ");
                    }
                    strBuff.append(elem);
                }
            }
            if (strBuff != null) {
                result = strBuff.toString();
            }
        }
        else {
            result = value.toString();
        }
        return result;
    }

}
