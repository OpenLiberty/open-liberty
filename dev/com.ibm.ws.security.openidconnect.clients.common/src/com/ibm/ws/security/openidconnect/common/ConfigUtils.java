/*******************************************************************************
 * Copyright (c) 2013, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.xml.internal.nester.Nester;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.config.CommonConfigUtils;
// import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;

/**
 * This is the config utility class
 */
public class ConfigUtils {
    private static final TraceComponent tc = Tr.register(ConfigUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private final AtomicServiceReference<ConfigurationAdmin> configAdminRef;

    private final CommonConfigUtils commonConfigUtils = new CommonConfigUtils();

    public static final String CFG_KEY_SCOPE_TO_CLAIM_MAP = "scopeToClaimMap";
    public static final String CFG_KEY_CLAIM_TO_UR_MAP = "claimToUserRegistryMap";
    public static final String CFG_KEY_DISCOVERY = "discovery";
    public static final String CFG_VALUES_DELIMITER = ",";

    private static HashMap<String, String[]> defaultDiscoveryProperties = new HashMap<String, String[]>();
    private static boolean defaultDiscoveryClaimsParmSupp;
    private static boolean defaultDiscoveryRequestParmSupp;
    private static boolean defaultDiscoveryRequestUriParmSupp;
    private static boolean defaultDiscoveryRequireRequestUriRegistrationSupp;

    /*
     * the following constants are duplicated from com.ibm.ws.security.oauth20.util.OIDCConstants to avoid a dependency on oauth
     */
    public static final String KEY_OIDC_ISSUER_ID = "issuerIdentifier";
    public static final String KEY_OIDC_AUTHORIZATION_EP = "authorizationEndpoint";
    public static final String KEY_OIDC_TOKEN_EP = "tokenEndpoint";
    public static final String KEY_OIDC_JWKS_URI = "jwksURI";
    public static final String KEY_OIDC_RESPONSE_TYPES_SUPP = "responseTypesSupported";
    public static final String KEY_OIDC_SUB_TYPES_SUPP = "subjectTypesSupported";
    public static final String KEY_OIDC_ID_TOKEN_SIGNING_ALG_VAL_SUPP = "idTokenSigningAlgValuesSupported";
    public static final String KEY_OIDC_USERINFO_EP = "userinfoEndpoint";
    public static final String KEY_OIDC_REGISTRATION_EP = "registrationEndpoint";
    public static final String KEY_OIDC_SCOPES_SUPP = "scopesSupported";
    public static final String KEY_OIDC_CLAIMS_SUPP = "claimsSupported";
    public static final String KEY_OIDC_RESP_MODES_SUPP = "responseModesSupported";
    public static final String KEY_OIDC_GRANT_TYPES_SUPP = "grantTypesSupported";
    public static final String KEY_OIDC_TOKEN_EP_AUTH_METHODS_SUPP = "tokenEndpointAuthMethodsSupported";
    public static final String KEY_OIDC_DISPLAY_VAL_SUPP = "displayValuesSupported";
    public static final String KEY_OIDC_CLAIM_TYPES_SUPP = "claimTypesSupported";
    public static final String KEY_OIDC_CLAIM_PARAM_SUPP = "claimsParameterSupported";
    public static final String KEY_OIDC_REQ_PARAM_SUPP = "requestParameterSupported";
    public static final String KEY_OIDC_REQ_URI_PARAM_SUPP = "requestUriParameterSupported";
    public static final String KEY_OIDC_REQUIRE_REQ_URI_REGISTRATION = "requireRequestUriRegistration";
    public static final String KEY_OIDC_CHECK_SESSION_IFRAME = "checkSessionIframe";
    public static final String KEY_OIDC_END_SESSION_EP = "endSessionEndpoint";
    public static final String KEY_OIDC_INTROSPECTION_EP = "introspectionEndpoint";
    public static final String KEY_OIDC_COVERAGE_MAP_EP = "coverageMapEndpoint";
    public static final String KEY_OIDC_PROXY_EP = "proxyEndpoint";
    public static final String KEY_OIDC_BACKING_IDP_URI_PREFIX = "backingIdpUriPrefix";
    /* end oauth constants */

    private static HashMap<String, String[]> specScopesToClaims = new HashMap<String, String[]>();
    private final static HashSet<String> specDefinedScopes = new HashSet<String>(Arrays.asList("profile", "email", "address", "phone"));
    private static HashMap<String, String> defaultClaimsToVMMProperties = new HashMap<String, String>();
    //    private final static HashSet<String> specDefinedClaims =
    //                    new HashSet<String>(Arrays.asList("name", "family_name", "given_name",
    //                                                      "middle_name", "nickname", "preferred_username",
    //                                                      "profile", "picture", "website", "gender",
    //                                                      "birthdate", "zoneinfo", "locale", "updated_at",
    //                                                      "email", "email_verified", "address",
    //                                                      "phone_number", "phone_number_verified"));
    private final static HashSet<String> supportedSpecDefinedClaims = new HashSet<String>(Arrays.asList("name", "given_name", "picture",
            "email", "address", "phone_number"));
    static {
        specScopesToClaims.put("profile", new String[] { "name", "family_name", "given_name",
                "middle_name", "nickname", "preferred_username",
                "profile", "picture", "website", "gender",
                "birthdate", "zoneinfo", "locale", "updated_at" });
        specScopesToClaims.put("email", new String[] { "email", "email_verified" });
        specScopesToClaims.put("address", new String[] { "address" });
        specScopesToClaims.put("phone", new String[] { "phone_number", "phone_number_verified" });

        defaultClaimsToVMMProperties.put("name", "displayName");
        //defaultClaimsToVMMProperties.put("family_name", null);
        defaultClaimsToVMMProperties.put("given_name", "givenName");
        //defaultClaimsToVMMProperties.put("middle_name", null);
        //defaultClaimsToVMMProperties.put("nickname", null);
        //defaultClaimsToVMMProperties.put("preferred_username", null);
        //defaultClaimsToVMMProperties.put("profile", null);
        defaultClaimsToVMMProperties.put("picture", "photoURL");
        //defaultClaimsToVMMProperties.put("website", null);
        //defaultClaimsToVMMProperties.put("gender", null);
        //defaultClaimsToVMMProperties.put("birthdate", null);
        //defaultClaimsToVMMProperties.put("zoneinfo", null);
        //defaultClaimsToVMMProperties.put("updated_at", null);
        //defaultClaimsToVMMProperties.put("locale", "localityName"); //?
        defaultClaimsToVMMProperties.put("email", "mail");
        //defaultClaimsToVMMProperties.put("email_verified", null);
        defaultClaimsToVMMProperties.put("address", "postalAddress");
        defaultClaimsToVMMProperties.put("phone_number", "telephoneNumber");
        //defaultClaimsToVMMProperties.put("phone_number_verified", null);

        setDefaultDiscoveryProperties();
    }

    //Should model what is set as default in the oidc.server metatype.xml
    private static void setDefaultDiscoveryProperties() {
        defaultDiscoveryProperties.put(KEY_OIDC_RESPONSE_TYPES_SUPP, new String[] { "code", "token", "id_token token" });
        defaultDiscoveryProperties.put(KEY_OIDC_SUB_TYPES_SUPP, new String[] { "public" });
        defaultDiscoveryProperties.put(KEY_OIDC_ID_TOKEN_SIGNING_ALG_VAL_SUPP, new String[] { "HS256" });
        defaultDiscoveryProperties.put(KEY_OIDC_SCOPES_SUPP, new String[] { "openid", "general", "profile", "email", "address", "phone" });
        defaultDiscoveryProperties.put(KEY_OIDC_CLAIMS_SUPP,
                new String[] { "sub", "groupIds", "name", "preferred_username", "picture", "locale", "email", "profile" });
        defaultDiscoveryProperties.put(KEY_OIDC_RESP_MODES_SUPP, new String[] { "query", "fragment", "form_post" });
        defaultDiscoveryProperties.put(KEY_OIDC_GRANT_TYPES_SUPP, new String[] { "authorization_code", "implicit", "refresh_token", "client_credentials", "password",
                "urn:ietf:params:oauth:grant-type:jwt-bearer" });
        defaultDiscoveryProperties.put(KEY_OIDC_TOKEN_EP_AUTH_METHODS_SUPP, new String[] { "client_secret_post", "client_secret_basic" });
        defaultDiscoveryProperties.put(KEY_OIDC_DISPLAY_VAL_SUPP, new String[] { "page" });
        defaultDiscoveryProperties.put(KEY_OIDC_CLAIM_TYPES_SUPP, new String[] { "normal" });
        defaultDiscoveryClaimsParmSupp = false;
        defaultDiscoveryRequestParmSupp = false;
        defaultDiscoveryRequestUriParmSupp = false;
        defaultDiscoveryRequireRequestUriRegistrationSupp = false;
    }

    /**
     * @param configAdminRef
     */
    public ConfigUtils(AtomicServiceReference<ConfigurationAdmin> configAdminRef) {
        this.configAdminRef = configAdminRef;
    }

    public Properties processDiscoveryProps(Map<String, Object> props, String elementNameRef) {
        if (props.get(elementNameRef) == null || ((String) props.get(elementNameRef)).isEmpty()) {
            Properties properties = new Properties();

            for (String key : defaultDiscoveryProperties.keySet()) {
                properties.put(key, defaultDiscoveryProperties.get(key));
            }

            properties.put(KEY_OIDC_CLAIM_PARAM_SUPP, defaultDiscoveryClaimsParmSupp);
            properties.put(KEY_OIDC_REQ_PARAM_SUPP, defaultDiscoveryRequestParmSupp);
            properties.put(KEY_OIDC_REQ_URI_PARAM_SUPP, defaultDiscoveryRequestUriParmSupp);
            properties.put(KEY_OIDC_REQUIRE_REQ_URI_REGISTRATION, defaultDiscoveryRequireRequestUriRegistrationSupp);

            return properties;
        }

        return processProps(props, elementNameRef);
    }

    /*
     * This method processes the elementNameRef and return all the properties
     *
     * @param props
     *
     * @param elementNameRef
     *
     * @return
     */
    public Properties processProps(Map<String, Object> props, String elementNameRef) {
        final Properties properties = new Properties();
        String pid = (String) props.get(elementNameRef);

        if (pid == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "pid null");
            }
        } else {
            ConfigurationAdmin configAdmin = configAdminRef.getServiceWithException();
            Configuration config = null;
            try {
                // We do not want to create a missing pid, only find one that we were told exists
                Configuration[] configList = configAdmin.listConfigurations(FilterUtils.createPropertyFilter("service.pid", pid));
                if (configList != null && configList.length > 0) {
                    //bind the config to this bundle so no one else can steal it
                    config = configAdmin.getConfiguration(pid, configAdminRef.getReference().getBundle().getLocation());
                }
            } catch (Exception e) {
                //do nothing
            }
            if (config != null) {
                // Just get the first one (there should only ever be one.. )
                Dictionary<String, ?> cProps = config.getProperties();
                Enumeration<String> keys = cProps.keys();
                while (keys.hasMoreElements()) {
                    String key = keys.nextElement();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "found key: " + key);
                    }
                    // Skip certain keys
                    if (key.startsWith(".")
                            || key.startsWith("config.")
                            || key.startsWith("service.")
                            || key.equals("id")) {
                        continue;
                    }
                    Object value = cProps.get(key);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "key: " + key + " value: " + value);
                    }
                    // For scope to claims map generate String array from comma delimited string
                    value = getValue(value);
                    properties.put(key, value);
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, elementNameRef + ": " + properties.toString());
        }
        return properties;
    }

    /**
     * This method process the given element name and returns all the user
     * supplied properties on the element and on any <property> subelements
     *
     * @param props
     *            top level config map
     * @param elementName
     *            the name of the element we're processing
     * @return user supplied properties from the config
     */
    public Properties processFlatProps(Map<String, Object> props, String elementName) {
        final Properties properties = new Properties();
        List<Map<String, Object>> listOfPropMaps = Nester.nest(elementName, props);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "listOfPropMaps: " + listOfPropMaps);
        }
        if (!listOfPropMaps.isEmpty()) {
            // Just get the first one (there should only ever be one)
            Map<String, Object> elementProps = listOfPropMaps.get(0);
            if (elementProps != null) {
                // get the properties specified on the parent element
                getConfigProperties(elementProps, properties, elementName);
                // get the properties specified on the <property> sub elements
                listOfPropMaps = Nester.nest("property", elementProps);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "subelement listOfPropMaps: " + listOfPropMaps);
                }
                if (!listOfPropMaps.isEmpty()) {
                    Map<String, Object> subElementProps = new HashMap<String, Object>();
                    for (Map<String, Object> propMap : listOfPropMaps) {
                        subElementProps.put((String) propMap.get("name"), (String) propMap.get("value"));
                    }
                    getConfigProperties(subElementProps, properties, elementName);
                }
            }
        }
        // Fill in defaults for anything not explicitly specified in the config
        if (elementName.equals(CFG_KEY_SCOPE_TO_CLAIM_MAP)) {
            for (String scope : specDefinedScopes) {
                if (!properties.containsKey(scope)) {
                    properties.put(scope, specScopesToClaims.get(scope));
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "adding default claims for scope " + scope + " : " + Arrays.toString(specScopesToClaims.get(scope)));
                    }
                }
            }
        } else if (elementName.equals(CFG_KEY_CLAIM_TO_UR_MAP)) {
            for (String claim : supportedSpecDefinedClaims) {
                if (!properties.containsKey(claim)) {
                    properties.put(claim, defaultClaimsToVMMProperties.get(claim));
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "adding default vmm property for claim " + claim + " : " + defaultClaimsToVMMProperties.get(claim));
                    }
                }
            }
        }
        return properties;
    }

    /**
     * Get user specified properties from the given element and/or it's <property> subelements.
     * Ignore system generated props, add the user props to the given Properties object
     *
     * @param configProps
     *            props from the config
     * @param properties
     *            add properties we're interested in to this collection
     * @param elementName
     *            the element being processed
     */
    private void getConfigProperties(Map<String, Object> configProps, Properties properties, String elementName) {
        Set<Entry<String, Object>> entries = configProps.entrySet();
        for (Entry<String, Object> entry : entries) {
            String key = entry.getKey();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "found key: " + key);
            }
            // Skip certain keys
            if (key.startsWith(".")
                    || key.startsWith("config.")
                    || key.startsWith("service.")
                    || key.startsWith("property.")
                    || key.equals("id")) {
                continue;
            }
            Object value = entry.getValue();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "key: " + key + " value: " + value);
            }
            // For scope to claims map generate String array from comma delimited string
            if (elementName.equals(CFG_KEY_SCOPE_TO_CLAIM_MAP)) {
                String[] valuesArray = ((String) value).split(CFG_VALUES_DELIMITER);
                for (int i = 0; i < valuesArray.length; i++) {
                    valuesArray[i] = valuesArray[i].trim();
                }
                value = valuesArray;
            } else {
                value = getValue(value);
            }
            properties.put(key, value);
        }
    }

    private Object getValue(Object value) {
        if (value != null) {
            if (value instanceof String) {
                return ((String) value).trim();
            } else if (value instanceof String[]) {
                return value;
            } else if (value instanceof Boolean) {
                return value;
            } else if (value instanceof Long) {
                return value;
            }
        }

        return value;
    }

    public List<String> readAndSanitizeForwardLoginParameter(Map<String, Object> props, String configId, String configAttributeName) {
        String[] attributeValue = commonConfigUtils.getStringArrayConfigAttribute(props, configAttributeName);
        if (attributeValue == null) {
            return new ArrayList<String>();
        }
        List<String> configuredForwardAuthzParamList = new ArrayList<String>(Arrays.asList(attributeValue));
        return removeDisallowedForwardAuthzParametersFromConfiguredList(configuredForwardAuthzParamList, configId, configAttributeName);
    }

    List<String> removeDisallowedForwardAuthzParametersFromConfiguredList(List<String> configuredList, String configId, String configAttributeName) {
        if (configuredList == null) {
            return new ArrayList<String>();
        }
        Set<String> configuredDisallowedParameters = new HashSet<String>(configuredList);
        configuredDisallowedParameters.retainAll(getDisallowedForwardAuthzParameterNames());
        if (!configuredDisallowedParameters.isEmpty()) {
            Tr.warning(tc, "DISALLOWED_FORWARD_AUTHZ_PARAMS_CONFIGURED", new Object[] { configId, configuredDisallowedParameters, configAttributeName });
            configuredList.removeAll(configuredDisallowedParameters);
        }
        return configuredList;
    }

    Set<String> getDisallowedForwardAuthzParameterNames() {
        Set<String> disallowedParamNames = new HashSet<String>();
        disallowedParamNames.add(Constants.REDIRECT_URI);
        disallowedParamNames.add(Constants.CLIENT_ID);
        disallowedParamNames.add(Constants.RESPONSE_TYPE);
        disallowedParamNames.add(Constants.NONCE);
        disallowedParamNames.add(Constants.STATE);
        disallowedParamNames.add(Constants.SCOPE);
        return disallowedParamNames;
    }

    /**
     * Populates a map of custom request parameter names and values to add to a certain OpenID Connect request type.
     *
     * @param configAdmin
     *            Config admin that has access to the necessary server configuration properties.
     * @param paramMapToPopulate
     *            Request-specific map of custom parameters to populate (for example, a map of parameters to add to authorization
     *            requests).
     * @param configuredCustomRequestParams
     *            List of configured custom parameter elements for a particular request type.
     * @param configAttrName
     *            Name of the config attribute that specifies the parameter name.
     * @param configAttrValue
     *            Name of the config attribute that specifies the parameter value.
     */
    public void populateCustomRequestParameterMap(ConfigurationAdmin configAdmin, HashMap<String, String> paramMapToPopulate, String[] configuredCustomRequestParams, String configAttrName, String configAttrValue) {
        if (configuredCustomRequestParams == null) {
            return;
        }
        for (String configuredParameter : configuredCustomRequestParams) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Configured custom request param [" + configuredParameter + "]");
            }
            Configuration config = getConfigurationFromConfigAdmin(configAdmin, configuredParameter);
            if (config != null) {
                addCustomRequestParameterValueToMap(config, paramMapToPopulate, configAttrName, configAttrValue);
            }
        }
    }

    Configuration getConfigurationFromConfigAdmin(ConfigurationAdmin configAdmin, String configParameter) {
        Configuration config = null;
        try {
            config = configAdmin.getConfiguration(configParameter, "");
        } catch (IOException e) {
        }
        return config;
    }

    @FFDCIgnore(ClassCastException.class)
    void addCustomRequestParameterValueToMap(Configuration config, HashMap<String, String> paramMapToPopulate, String configAttrName, String configAttrValue) {
        Dictionary<String, Object> configProps = config.getProperties();
        if (configProps == null || configAttrName == null || configAttrValue == null) {
            return;
        }
        String paramName = null;
        String paramValue = null;
        try {
            paramName = (String) configProps.get(configAttrName);
            paramValue = (String) configProps.get(configAttrValue);
        } catch (ClassCastException e) {
            // Do nothing. We expect string values for these props, so leave the values null if they're not strings
        }
        if (paramName != null && paramValue != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Adding parameter name [" + paramName + "] and value [" + paramValue + "] to map");
            }
            if (paramMapToPopulate == null) {
                paramMapToPopulate = new HashMap<String, String>();
            }
            paramMapToPopulate.put(paramName, paramValue);
        }
    }

}
