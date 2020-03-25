/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.oauth20.internal.LibertyOAuth20Provider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(service = OAuth20ClientMetatypeService.class, name = "oauth20ClientMetatypeService", immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = "service.vendor=IBM")
public class OAuth20ClientMetatypeService {

    private static TraceComponent tc = Tr.register(OAuth20ClientMetatypeService.class);

    public static final String RESPONSE_KEY_ID = "id";
    public static final String RESPONSE_KEY_NAME = "name";
    public static final String RESPONSE_KEY_DESCRIPTION = "description";
    public static final String RESPONSE_KEY_TYPE = "type";
    public static final String RESPONSE_KEY_DEFAULT = "default";
    public static final String RESPONSE_KEY_OPTIONS = "options";
    public static final String RESPONSE_KEY_OPTIONS_LABEL = "label";
    public static final String RESPONSE_KEY_OPTIONS_VALUE = "value";
    public static final String RESPONSE_KEY_CARDINALITY = "cardinality";
    public static final String RESPONSE_KEY_REQUIRED = "required";
    public static final String RESPONSE_KEY_SEPARATION_CHAR = "separationChar";
    public static final String RESPONSE_KEY_ALLOW_USER_PROVIDED_VALUE = "allowUserProvidedValue";
    public static final String RESPONSE_KEY_REQUEST_PARAMETER_NAME = "requestParameterName";

    public static final String KEY_METATYPE_SERVICE = "metaTypeService";
    protected final AtomicServiceReference<MetaTypeService> metaTypeServiceRef = new AtomicServiceReference<MetaTypeService>(KEY_METATYPE_SERVICE);

    private Bundle thisBundle = null;
    private JsonObject allMetatypeData = new JsonObject();

    @Reference(service = MetaTypeService.class, name = KEY_METATYPE_SERVICE, policy = ReferencePolicy.DYNAMIC)
    protected void setMetaTypeService(ServiceReference<MetaTypeService> ref) {
        metaTypeServiceRef.setReference(ref);
    }

    protected void unsetMetaTypeService(ServiceReference<MetaTypeService> ref) {
        metaTypeServiceRef.unsetReference(ref);
    }

    @Activate
    protected void activate(ComponentContext cc) {
        metaTypeServiceRef.activate(cc);

        thisBundle = cc.getBundleContext().getBundle();
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        metaTypeServiceRef.deactivate(cc);
    }

    public void sendClientMetatypeData(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!isSupportedHttpMethod(request.getMethod())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        JsonArray metadataJson = getOAuthClientMetatypeData(getRequestLocales(request));
        writeMetatypeDataToResponse(response, metadataJson);
    }

    JsonArray getOAuthClientMetatypeData(List<String> locales) {
        MetaTypeInformation metatypeInfo = getMetaTypeInformation();
        if (metatypeInfo == null) {
            return null;
        }
        allMetatypeData = createMetatypeJson(metatypeInfo, locales);
        return putMetatypeDataInAppropriateOrder();
    }

    void writeMetatypeDataToResponse(HttpServletResponse response, JsonArray metadataJson) throws IOException {
        try {
            if (metadataJson == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Null metatype object was supplied; error response will be returned");
                }
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
            response.setHeader("Content-Type", "application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
            PrintWriter writer = response.getWriter();
            writer.println(new String(metadataJson.toString().getBytes(StandardCharsets.UTF_16.toString()), StandardCharsets.UTF_16));
            writer.flush();
            writer.close();
        } catch (IOException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isSupportedHttpMethod(String requestMethod) {
        return "GET".equalsIgnoreCase(requestMethod);
    }

    private List<String> getRequestLocales(HttpServletRequest request) {
        List<String> localesSet = new ArrayList<String>();
        Enumeration<Locale> locales = request.getLocales();
        if (locales != null) {
            while (locales.hasMoreElements()) {
                localesSet.add(locales.nextElement().toString());
            }
        }
        return localesSet;
    }

    private MetaTypeInformation getMetaTypeInformation() {
        MetaTypeService metatypeService = metaTypeServiceRef.getService();
        if (metatypeService == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to find MetaTypeService");
            }
            return null;
        }
        if (thisBundle == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Missing bundle information");
            }
            return null;
        }
        MetaTypeInformation metatypeInfo = metatypeService.getMetaTypeInformation(thisBundle);
        if (metatypeInfo == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to find MetaTypeInformation for this bundle (" + thisBundle.getBundleId() + " - " + thisBundle.getSymbolicName() + ")");
            }
            return null;
        }
        return metatypeInfo;
    }

    private JsonObject createMetatypeJson(MetaTypeInformation metatypeInfo, List<String> locales) {
        JsonObject metatypeJson = null;
        for (String thisLocale : locales) {
            // TODO - any way to get ExtendedObjectClassDefinition?
            ObjectClassDefinition ocd = metatypeInfo.getObjectClassDefinition("com.ibm.ws.security.oauth20.client", thisLocale);
            if (ocd == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Did not find translated metatype information for locale [" + thisLocale + "]");
                }
                continue;
            }
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Creating metatype information based on locale [" + thisLocale + "]");
            }
            metatypeJson = createMetatypeJsonForObjectClassDefinition(metatypeInfo, ocd);
            break;
        }
        return metatypeJson;
    }

    // TODO - decide on whether to improve efficiency by separately getting required and optional attributes
    private JsonObject createMetatypeJsonForObjectClassDefinition(MetaTypeInformation metatypeInfo, ObjectClassDefinition ocd) {
        AttributeDefinition[] allAttributeDefs = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
        if (allAttributeDefs == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to find any attribute definitions for OCD [" + ocd.getID() + "]");
            }
            return null;
        }
        List<String> requiredAttributeIds = getRequiredAttributeIds(ocd);
        JsonObject metatypeJson = new JsonObject();
        for (AttributeDefinition attrDef : allAttributeDefs) {
            if (isAttributeToIgnore(attrDef)) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Attribute [" + attrDef.getID() + "] will not be included");
                }
                continue;
            }
            JsonObject attrJson = createJsonForConfigAttribute(attrDef, requiredAttributeIds);
            if (attrJson != null) {
                metatypeJson.add(attrDef.getID(), attrJson);
            }
        }
        return metatypeJson;
    }

    private List<String> getRequiredAttributeIds(ObjectClassDefinition ocd) {
        List<String> requiredAttributes = new ArrayList<String>();
        AttributeDefinition[] requiredAttrDefs = ocd.getAttributeDefinitions(ObjectClassDefinition.REQUIRED);
        if (requiredAttrDefs == null) {
            return requiredAttributes;
        }
        for (AttributeDefinition attrDef : requiredAttrDefs) {
            requiredAttributes.add(attrDef.getID());
        }
        return requiredAttributes;
    }

    @Trivial
    private boolean isAttributeToIgnore(AttributeDefinition attrDef) {
        return "internal".equals(attrDef.getName()) || isUnsupportedRegistrationAttribute(attrDef);
    }

    @Trivial
    private boolean isUnsupportedRegistrationAttribute(AttributeDefinition attrDef) {
        String attributeId = attrDef.getID();
        // LibertyOAuth20Provider.KEY_CLIENT_TRUSTED_URI_PREFIXES.equals(attributeId)
        return LibertyOAuth20Provider.KEY_CLIENT_RESOURCE_IDS.equals(attributeId)
                || LibertyOAuth20Provider.KEY_CLIENT_ENABLED.equals(attributeId)
                || OIDCConstants.OIDC_SESSION_MANAGED.equals(attributeId);
    }

    private JsonObject createJsonForConfigAttribute(AttributeDefinition attrDef, List<String> requiredAttributeIds) {
        JsonObject attributeJson = new JsonObject();
        addApiRequiredEntriesForConfigAttribute(attrDef, attributeJson);
        addApiOptionalEntriesForConfigAttribute(attrDef, attributeJson, requiredAttributeIds);
        addApiSpecificEntriesForConfigAttribute(attrDef, attributeJson);
        return attributeJson;
    }

    private void addApiRequiredEntriesForConfigAttribute(AttributeDefinition attrDef, JsonObject attributeJson) {
        attributeJson.addProperty(RESPONSE_KEY_ID, attrDef.getID());
        attributeJson.addProperty(RESPONSE_KEY_NAME, attrDef.getName());
        attributeJson.addProperty(RESPONSE_KEY_DESCRIPTION, attrDef.getDescription());
        attributeJson.addProperty(RESPONSE_KEY_TYPE, getAttributeTypeString(attrDef));
    }

    private String getAttributeTypeString(AttributeDefinition attrDef) {
        int typeNum = attrDef.getType();
        if (typeNum == AttributeDefinition.STRING) {
            return "String";
        }
        if (typeNum == AttributeDefinition.BOOLEAN) {
            return "Boolean";
        }
        if (typeNum == AttributeDefinition.BYTE) {
            return "Byte";
        }
        if (typeNum == AttributeDefinition.CHARACTER) {
            return "Character";
        }
        if (typeNum == AttributeDefinition.DOUBLE) {
            return "Double";
        }
        if (typeNum == AttributeDefinition.FLOAT) {
            return "Float";
        }
        if (typeNum == AttributeDefinition.INTEGER) {
            return "Integer";
        }
        if (typeNum == AttributeDefinition.LONG) {
            return "Long";
        }
        if (typeNum == AttributeDefinition.PASSWORD) {
            return "Password";
        }
        if (typeNum == AttributeDefinition.SHORT) {
            return "Short";
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Encountered unknown type value [" + typeNum + "]");
        }
        return null;
    }

    private void addApiOptionalEntriesForConfigAttribute(AttributeDefinition attrDef, JsonObject attributeJson, List<String> requiredAttributeIds) {
        addAttributeEntryForDefault(attrDef, attributeJson);
        addAttributeEntryForOptions(attrDef, attributeJson);
        addAttributeEntryForCardinality(attrDef, attributeJson);
        addAttributeEntryForRequired(attrDef, attributeJson, requiredAttributeIds);
        addAttributeEntryForRequestParameterName(attrDef, attributeJson);
    }

    private void addAttributeEntryForDefault(AttributeDefinition attrDef, JsonObject attributeJson) {
        String[] defaults = attrDef.getDefaultValue();
        if (defaults == null) {
            return;
        }
        JsonArray defaultArray = new JsonArray();
        for (String def : defaults) {
            defaultArray.add(new JsonPrimitive(def));
        }
        attributeJson.add(RESPONSE_KEY_DEFAULT, defaultArray);
    }

    private void addAttributeEntryForOptions(AttributeDefinition attrDef, JsonObject attributeJson) {
        String[] optionLabels = attrDef.getOptionLabels();
        String[] optionValues = attrDef.getOptionValues();
        if (optionLabels != null && optionValues != null) {
            if (optionLabels.length != optionValues.length) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Number of option labels " + Arrays.toString(optionLabels) + " did not match number of option values " + Arrays.toString(optionValues));
                }
                return;
            }
            JsonArray optionsArray = new JsonArray();
            for (int i = 0; i < optionLabels.length; i++) {
                JsonObject optionJson = new JsonObject();
                optionJson.addProperty(RESPONSE_KEY_OPTIONS_LABEL, optionLabels[i]);
                optionJson.addProperty(RESPONSE_KEY_OPTIONS_VALUE, optionValues[i]);
                optionsArray.add(optionJson);
            }
            attributeJson.add(RESPONSE_KEY_OPTIONS, optionsArray);
        }
    }

    private void addAttributeEntryForCardinality(AttributeDefinition attrDef, JsonObject attributeJson) {
        int cardinality = attrDef.getCardinality();
        if (cardinality != 0) {
            attributeJson.addProperty(RESPONSE_KEY_CARDINALITY, cardinality);
        }
    }

    private void addAttributeEntryForRequired(AttributeDefinition attrDef, JsonObject attributeJson, List<String> requiredAttributeIds) {
        if (requiredAttributeIds.contains(attrDef.getID())) {
            attributeJson.addProperty(RESPONSE_KEY_REQUIRED, true);
        }
    }

    private void addAttributeEntryForRequestParameterName(AttributeDefinition attrDef, JsonObject attributeJson) {
        String registrationApiReqParamName = getRegistrationApiRequestParameterName(attrDef.getID());
        if (registrationApiReqParamName != null) {
            attributeJson.addProperty(RESPONSE_KEY_REQUEST_PARAMETER_NAME, registrationApiReqParamName);
        }
    }

    private String getRegistrationApiRequestParameterName(String attributeId) {
        Map<String, String> nameMap = getConfigAttributeNameToRegistrationApiParameterNameMap();
        return nameMap.get(attributeId);
    }

    /**
     * Configuration attribute names don't perfectly map to the request parameter name used for that configuration option by the
     * OAuth client registration API. For example, the "name" configuration attribute corresponds to the "client_id" registration
     * request parameter. This returns the appropriate mapping for each configuration attribute.
     */
    private Map<String, String> getConfigAttributeNameToRegistrationApiParameterNameMap() {
        Map<String, String> nameMap = new HashMap<String, String>();
        nameMap.put(LibertyOAuth20Provider.KEY_CLIENT_ID, OidcBaseClient.SN_CLIENT_ID);
        nameMap.put(LibertyOAuth20Provider.KEY_CLIENT_SECRET, OidcBaseClient.SN_CLIENT_SECRET);
        nameMap.put(LibertyOAuth20Provider.KEY_CLIENT_DISPLAYNAME, OidcBaseClient.SN_CLIENT_NAME);
        nameMap.put(LibertyOAuth20Provider.KEY_CLIENT_REDIRECT, OidcBaseClient.SN_REDIRECT_URIS);
        nameMap.put(LibertyOAuth20Provider.KEY_CLIENT_SCOPE, LibertyOAuth20Provider.KEY_CLIENT_SCOPE);
        nameMap.put(LibertyOAuth20Provider.KEY_CLIENT_GRANT_TYPES, OidcBaseClient.SN_GRANT_TYPES);
        nameMap.put(LibertyOAuth20Provider.KEY_CLIENT_RESPONSE_TYPES, OidcBaseClient.SN_RESPONSE_TYPES);
        nameMap.put(LibertyOAuth20Provider.KEY_CLIENT_TOKEN_EP_AUTH_METHOD, OidcBaseClient.SN_TOKEN_ENDPOINT_AUTH_METHOD);
        nameMap.put(LibertyOAuth20Provider.KEY_CLIENT_PREAUTHORIZED_SCOPE, OidcBaseClient.SN_PREAUTHORIZED_SCOPE);
        nameMap.put(LibertyOAuth20Provider.KEY_CLIENT_APP_TYPE, OidcBaseClient.SN_APPLICATION_TYPE);
        nameMap.put(LibertyOAuth20Provider.KEY_CLIENT_POST_LOGOUT_REDIRECT_URIS, OidcBaseClient.SN_POST_LOGOUT_REDIRECT_URIS);
        nameMap.put(LibertyOAuth20Provider.KEY_CLIENT_SUBJECT_TYPE, OidcBaseClient.SN_SUBJECT_TYPE);
        nameMap.put(LibertyOAuth20Provider.KEY_CLIENT_FUNCTIONAL_USER_ID, OidcBaseClient.SN_FUNCTIONAL_USER_ID);
        nameMap.put(LibertyOAuth20Provider.KEY_CLIENT_FUNCTIONAL_USER_GROUPIDS, OidcBaseClient.SN_FUNCTIONAL_USER_GROUP_IDS);
        nameMap.put(LibertyOAuth20Provider.KEY_CLIENT_INTROSPECT_TOKENS, OidcBaseClient.SN_INTROSPECT_TOKENS);
        nameMap.put(LibertyOAuth20Provider.KEY_CLIENT_allowRegexpRedirects, OidcBaseClient.SN_ALLOW_REGEXP_REDIRECTS);
        nameMap.put(LibertyOAuth20Provider.KEY_CLIENT_TRUSTED_URI_PREFIXES, OidcBaseClient.SN_TRUSTED_URI_PREFIXES);
        return nameMap;
    }

    private void addApiSpecificEntriesForConfigAttribute(AttributeDefinition attrDef, JsonObject attributeJson) {
        addAttributeEntryForValidation(attrDef, attributeJson);
        addAttributeEntryForSeparationChar(attrDef, attributeJson);
        addAttributeEntryForAllowUserProvidedValue(attrDef, attributeJson);
    }

    private void addAttributeEntryForValidation(AttributeDefinition attrDef, JsonObject attributeJson) {
        String attributeId = attrDef.getID();
        String validationRegex = null;
        String validationErrorMsg = null;
        // TODO
        // if (validationRegex != null) {
        // attributeJson.addProperty("validation", validationRegex);
        // }
        // if (validationErrorMsg != null) {
        // attributeJson.addProperty("validationErrMsg", validationErrorMsg);
        // }
    }

    private void addAttributeEntryForSeparationChar(AttributeDefinition attrDef, JsonObject attributeJson) {
        String attributeId = attrDef.getID();
        String seperationChar = null;
        if (isSpaceSeparatedAttribute(attributeId)) {
            seperationChar = " ";
        }
        if (seperationChar != null) {
            attributeJson.addProperty(RESPONSE_KEY_SEPARATION_CHAR, seperationChar);
        }
    }

    private boolean isSpaceSeparatedAttribute(String attributeId) {
        return (LibertyOAuth20Provider.KEY_CLIENT_SCOPE.equals(attributeId) || LibertyOAuth20Provider.KEY_CLIENT_PREAUTHORIZED_SCOPE.equals(attributeId));
    }

    private void addAttributeEntryForAllowUserProvidedValue(AttributeDefinition attrDef, JsonObject attributeJson) {
        String attributeId = attrDef.getID();
        if (isAttributeThatAllowsUserProvidedValues(attributeId)) {
            attributeJson.addProperty(RESPONSE_KEY_ALLOW_USER_PROVIDED_VALUE, true);
        }
    }

    private boolean isAttributeThatAllowsUserProvidedValues(String attributeId) {
        return (LibertyOAuth20Provider.KEY_CLIENT_SCOPE.equals(attributeId) || LibertyOAuth20Provider.KEY_CLIENT_PREAUTHORIZED_SCOPE.equals(attributeId));
    }

    private JsonArray putMetatypeDataInAppropriateOrder() {
        JsonArray responseJson = new JsonArray();
        responseJson.addAll(getPrioritizedMetatypeData());
        responseJson.addAll(getUnprioritizedMetatypeData());
        return responseJson;
    }

    /**
     * Adds to the response the metatype entries that are most likely to be of interest and most likely to be configured by a
     * server admin.
     */
    private JsonArray getPrioritizedMetatypeData() {
        JsonArray responseJson = new JsonArray();
        List<String> prioritizedEntries = getPrioritizedEntryOrder();
        for (String prioritizedEntryKey : prioritizedEntries) {
            JsonObject responseEntry = allMetatypeData.getAsJsonObject(prioritizedEntryKey);
            if (responseEntry == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed to find data corresponding to expected config attribute [" + prioritizedEntryKey + "]");
                }
                continue;
            }
            responseJson.add(processMetatypeDataEntryForResponse(prioritizedEntryKey, responseEntry));
        }
        return responseJson;
    }

    private JsonArray getUnprioritizedMetatypeData() {
        JsonArray responseJson = new JsonArray();
        List<String> unprioritizedMetatypeKeys = getMetatypeJsonObjectKeys();
        // Remove entries for the keys that have been given priority and should already be present in the JSON response
        unprioritizedMetatypeKeys.removeAll(getPrioritizedEntryOrder());

        // Sort and add the remaining metatype entries
        Collections.sort(unprioritizedMetatypeKeys);
        for (String unprioritizedKey : unprioritizedMetatypeKeys) {
            JsonObject responseEntry = allMetatypeData.getAsJsonObject(unprioritizedKey);
            if (responseEntry == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed to find data corresponding to expected config attribute [" + unprioritizedKey + "]");
                }
                continue;
            }
            responseJson.add(processMetatypeDataEntryForResponse(unprioritizedKey, responseEntry));
        }
        return responseJson;
    }

    private List<String> getMetatypeJsonObjectKeys() {
        // Oddly enough, JsonObject doesn't have any kind of method for getting just the keys in the object
        List<String> jsonObjectKeys = new ArrayList<String>();
        for (Entry<String, JsonElement> entry : allMetatypeData.entrySet()) {
            jsonObjectKeys.add(entry.getKey());
        }
        return jsonObjectKeys;
    }

    /**
     * Determines the order in which some metatype entries should ultimately appear in the API response JSON data.
     */
    private List<String> getPrioritizedEntryOrder() {
        List<String> responseEntryOrder = new ArrayList<String>();
        responseEntryOrder.add(LibertyOAuth20Provider.KEY_CLIENT_ID);
        responseEntryOrder.add(LibertyOAuth20Provider.KEY_CLIENT_SECRET);
        responseEntryOrder.add(LibertyOAuth20Provider.KEY_CLIENT_DISPLAYNAME);
        responseEntryOrder.add(LibertyOAuth20Provider.KEY_CLIENT_REDIRECT);
        responseEntryOrder.add(LibertyOAuth20Provider.KEY_CLIENT_SCOPE);
        responseEntryOrder.add(LibertyOAuth20Provider.KEY_CLIENT_GRANT_TYPES);
        responseEntryOrder.add(LibertyOAuth20Provider.KEY_CLIENT_RESPONSE_TYPES);
        responseEntryOrder.add(LibertyOAuth20Provider.KEY_CLIENT_TOKEN_EP_AUTH_METHOD);
        responseEntryOrder.add(LibertyOAuth20Provider.KEY_CLIENT_PREAUTHORIZED_SCOPE);
        return responseEntryOrder;
    }

    private JsonObject processMetatypeDataEntryForResponse(String attributeId, JsonObject entry) {
        updateSpecialCaseEntries(attributeId, entry);
        return entry;
    }

    private void updateSpecialCaseEntries(String attributeId, JsonObject entry) {
        if (LibertyOAuth20Provider.KEY_CLIENT_SCOPE.equals(attributeId)) {
            updateEntriesUniqueToScope(entry);
        }
        if (LibertyOAuth20Provider.KEY_CLIENT_GRANT_TYPES.equals(attributeId)) {
            updateEntriesUniqueToGrantTypes(entry);
        }
        if (LibertyOAuth20Provider.KEY_CLIENT_RESPONSE_TYPES.equals(attributeId)) {
            updateEntriesUniqueToResponseTypes(entry);
        }
    }

    private void updateEntriesUniqueToScope(JsonObject entry) {
        JsonArray defaults = entry.getAsJsonArray(RESPONSE_KEY_DEFAULT);
        if (defaults == null) {
            defaults = new JsonArray();
        }
        // The "openid" scope is required for OpenID Connect functionality to work. Since this code is written to support
        // registering a new OAuth client within an OpenID Connect provider, it makes sense to automatically add "openid"
        // as a default scope.
        defaults.add(new JsonPrimitive(OIDCConstants.SCOPE_OPENID));
        entry.add(RESPONSE_KEY_DEFAULT, defaults);
    }

    private void updateEntriesUniqueToGrantTypes(JsonObject entry) {
        // The metatype.xml includes all of the possible options as entries in the default list. However the client
        // registration API actually uses only authorization_code as the default.
        JsonArray defaults = new JsonArray();
        defaults.add(new JsonPrimitive(OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE));
        entry.add(RESPONSE_KEY_DEFAULT, defaults);
    }

    private void updateEntriesUniqueToResponseTypes(JsonObject entry) {
        // The metatype.xml includes all of the possible options as entries in the default list. However the client
        // registration API actually uses only code as the default.
        JsonArray defaults = new JsonArray();
        defaults.add(new JsonPrimitive(OAuth20Constants.RESPONSE_TYPE_CODE));
        entry.add(RESPONSE_KEY_DEFAULT, defaults);
    }

}
