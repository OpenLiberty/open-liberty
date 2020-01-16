/*******************************************************************************
 * Copyright (c) 1997, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.oauth.core.internal.oauth20.granttype.OAuth20GrantTypeHandlerFactory;
import com.ibm.oauth.core.internal.oauth20.responsetype.OAuth20ResponseTypeHandlerFactory;
import com.ibm.oauth.core.internal.oauth20.tokentype.OAuth20TokenTypeHandler;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.common.claims.UserClaimsRetrieverService;
import com.ibm.ws.security.oauth20.TraceConstants;
import com.ibm.ws.security.oauth20.internal.LibertyOAuth20Provider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.wim.VMMService;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.oauth20.JwtAccessTokenMediator;
import com.ibm.wsspi.security.openidconnect.IDTokenMediator;

@Component(service = ConfigUtils.class,
        name = "com.ibm.ws.security.oauth20.util.ConfigUtils",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.IGNORE,
        property = "service.vendor=IBM")
public class ConfigUtils {

    private static TraceComponent tc = Tr.register(ConfigUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    protected static final List<OidcBaseClient> clientsList = Collections.synchronizedList(new ArrayList<OidcBaseClient>());
    protected static final Map<String, List<OAuth20Parameter>> providerConfigMap = Collections.synchronizedMap(new HashMap<String, List<OAuth20Parameter>>());
    protected static final Map<String, ClassLoader> providerPluginClassLoaderMap =
            Collections.synchronizedMap(new HashMap<String, ClassLoader>());
    private static Map<String, Object[]> jdbcCredentialsMap =
            Collections.synchronizedMap(new HashMap<String, Object[]>());
    private static UserClaimsRetrieverService userClaimsRetrieverService;
    public static final String KEY_JWT_MEDIATOR = "jwtAccessTokenMediator";
    private static ConcurrentServiceReferenceMap<String, JwtAccessTokenMediator> jwtMediatorRef =
            new ConcurrentServiceReferenceMap<String, JwtAccessTokenMediator>(KEY_JWT_MEDIATOR);
    public static final String KEY_IDTOKEN_MEDIATOR = "idTokenMediator";
    private static ConcurrentServiceReferenceMap<String, IDTokenMediator> idTokenMediatorRef =
            new ConcurrentServiceReferenceMap<String, IDTokenMediator>(KEY_IDTOKEN_MEDIATOR);

    public static final String BUILTIN_DB_PROVIDER_CLASS = "com.ibm.ws.security.oauth20.plugins.db.CachedDBOidcClientProvider";
    public static final String BUILTIN_DB_TOKEN_STORE_CLASS = "com.ibm.ws.security.oauth20.plugins.db.CachedDBOidcTokenStore";
    public static final String BUILTIN_BASE_PROVIDER_CLASS = "com.ibm.ws.security.oauth20.plugins.OidcBaseClientProvider";
    public static final String BUILTIN_BASE_TOKEN_STORE_CLASS = "com.ibm.ws.security.oauth20.plugins.BaseCache";
    public static final String BUILTIN_BASE_TOKEN_HANDLER_CLASS = "com.ibm.ws.security.oauth20.plugins.BaseTokenHandler";
    public static final String BUILTIN_BASE_ID_TOKEN_HANDLER_CLASS = OIDCConstants.DEFAULT_ID_TOKEN_HANDLER_CLASS; // oidc10
    public static final String BUILTIN_GRANT_TYPE_HANDLER_FACTORY_CLASS = OIDCConstants.DEFAULT_OIDC_GRANT_TYPE_HANDLER_FACTORY_CLASSNAME; // oidc10
    public static final String BUILTIN_RESPONSE_TYPE_HANDLER_FACTORY_CLASS = OIDCConstants.DEFAULT_OIDC10_RESPONSE_TYPE_HANDLER_FACTORY_CLASSNAME; // oidc10
    public static final String BUILTIN_MEDIATOR_CLASS = "com.ibm.oauth.core.internal.oauth20.mediator.impl.OAuth20MediatorDefaultImpl";
    public static final String BUILTIN_SAMPLE_MEDIATOR_CLASS = "com.ibm.ws.security.oauth20.mediator.ResourceOwnerValidationMediator";

    private static Map<String, SecurityService> mapSecurityService =
            Collections.synchronizedMap(new HashMap<String, SecurityService>());

    public static final String KEY_ID = "id";
    public static final String KEY_OIDC_SERVER_CONFIG = "oidcServerConfig";
    private static final String KEY_VMM_SERVICE = "vmmService";
    static AtomicServiceReference<VMMService> vmmServiceRef =
            new AtomicServiceReference<VMMService>(KEY_VMM_SERVICE);

    private final static ConcurrentServiceReferenceMap<String, OidcServerConfig> oidcServerConfigRef =
            new ConcurrentServiceReferenceMap<String, OidcServerConfig>(KEY_OIDC_SERVER_CONFIG);
    private static boolean bOidcUpdated = false;
    private static HashMap<String, OidcServerConfig> oidcMap = new HashMap<String, OidcServerConfig>();

    private static final String KEY_OIDC_IDTOKEN_HANDLER = "IDTokenHandler";
    private volatile OAuth20TokenTypeHandler oidcIDTokenHandler = null;
    private static final String KEY_OIDC_GRANT_TYPE_HANDLER_FACTORY = "OAuth20GrantTypeHandlerFactory";
    private volatile OAuth20GrantTypeHandlerFactory oidcGrantTypeHandlerFactory = null;
    private static final String KEY_OIDC_RESPONSE_TYPE_HANDLER_FACTORY = "OAuth20ResponseTypeHandlerFactory";
    private volatile OAuth20ResponseTypeHandlerFactory oidcResponseTypeHandlerFactory = null;

    @Activate
    public void activate(ComponentContext cc) {
        synchronized (oidcServerConfigRef) {
            oidcServerConfigRef.activate(cc);
            bOidcUpdated = true;
        }
        vmmServiceRef.activate(cc);
    }

    @Deactivate
    public void deactivate(ComponentContext cc) {
        synchronized (oidcServerConfigRef) {
            oidcServerConfigRef.deactivate(cc);
            bOidcUpdated = true;
        }
        vmmServiceRef.deactivate(cc);
    }

    @Reference(service = OidcServerConfig.class, name = KEY_OIDC_SERVER_CONFIG,
            policy = ReferencePolicy.DYNAMIC,
            cardinality = ReferenceCardinality.MULTIPLE,
            policyOption = ReferencePolicyOption.GREEDY)
    public void setOidcServerConfig(ServiceReference<OidcServerConfig> ref) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setOidcServerConfig", new Object[] { ref });
        synchronized (oidcServerConfigRef) {
            oidcServerConfigRef.putReference((String) ref.getProperty(KEY_ID), ref);
            bOidcUpdated = true;
        }
    }

    public void unsetOidcServerConfig(ServiceReference<OidcServerConfig> ref) {
        synchronized (oidcServerConfigRef) {
            oidcServerConfigRef.removeReference((String) ref.getProperty(KEY_ID), ref);
            bOidcUpdated = true;
        }
    }

    @Reference(service = VMMService.class,
            name = KEY_VMM_SERVICE,
            policy = ReferencePolicy.DYNAMIC)
    public void setVmmService(ServiceReference<VMMService> ref) {
        vmmServiceRef.setReference(ref);
    }

    protected void unsetVmmService(ServiceReference<VMMService> ref) {
        vmmServiceRef.unsetReference(ref);
    }

    /*
     * This is a singleton instance.
     * Do not use it for multiple instance
     */
    @Reference(name = KEY_OIDC_IDTOKEN_HANDLER,
            service = OAuth20TokenTypeHandler.class)
    protected void setOidcIDTokenTypeHandler(OAuth20TokenTypeHandler handler) {
        oidcIDTokenHandler = handler;
        LibertyOAuth20Provider.setOidcIDTokenTypeHandler(handler);
    }

    protected void unsetOidcIDTokenTypeHandler(OAuth20TokenTypeHandler handler) {
        oidcIDTokenHandler = null;
        LibertyOAuth20Provider.setOidcIDTokenTypeHandler(null);
    }

    /*
     * This is a singleton instance.
     * Do not use it for multiple instance
     */
    @Reference(name = KEY_OIDC_GRANT_TYPE_HANDLER_FACTORY,
            service = OAuth20GrantTypeHandlerFactory.class)
    protected void setOidcGrantTypeHandlerFactory(OAuth20GrantTypeHandlerFactory handler) {
        oidcGrantTypeHandlerFactory = handler;
        LibertyOAuth20Provider.setOidcGrantTypeHandlerFactory(handler);
    }

    protected void unsetOidcGrantTypeHandlerFactory(OAuth20GrantTypeHandlerFactory handler) {
        oidcGrantTypeHandlerFactory = null;
        LibertyOAuth20Provider.setOidcGrantTypeHandlerFactory(null);
    }

    /*
     * This is a singleton instance.
     * Do not use it for multiple instance
     */
    @Reference(name = KEY_OIDC_RESPONSE_TYPE_HANDLER_FACTORY,
            service = OAuth20ResponseTypeHandlerFactory.class)
    protected void setOidcResponseTypeHandlerFactory(OAuth20ResponseTypeHandlerFactory handler) {
        oidcResponseTypeHandlerFactory = handler;
        LibertyOAuth20Provider.setOidcResponseTypeHandlerFactory(handler);
    }

    protected void unsetOidcResponseTypeHandlerFactory(OAuth20ResponseTypeHandlerFactory handler) {
        oidcResponseTypeHandlerFactory = null;
        LibertyOAuth20Provider.setOidcResponseTypeHandlerFactory(null);
    }

    /**
     * Gets the OIDC server config for the OIDC provider that uses the given OAuth20 provider name.
     * @param oauth20providerName The OAuth20 provider name used by the OIDC provider.
     * @return The OidcServerConfig object.
     */
    public static OidcServerConfig getOidcServerConfigForOAuth20Provider(String oauth20providerName) {
        OidcServerConfig oidcServerConfig = null;
        synchronized (oidcServerConfigRef) {
            if (bOidcUpdated) {
                ConfigUtils configUtils = new ConfigUtils();
                oidcMap = configUtils.checkDuplicateOAuthProvider(oidcServerConfigRef);
                bOidcUpdated = false;
            }
        }
        Set<String> oidcServerConfigIds = oidcMap.keySet();
        for (String oidcServerConfigId : oidcServerConfigIds) {
            OidcServerConfig entry = oidcMap.get(oidcServerConfigId);
            String providerName = entry.getOauthProviderName();
            if (oauth20providerName.equals(providerName)) {
                oidcServerConfig = entry;
                break;
            }
        }
        return oidcServerConfig;
    }

    /**
     * @param name
     * @param value
     * @return a string of name=value
     */
    public static String nameAndValueProperty(String name, String value) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "nameAndValueProperty", new Object[] { name, value });
        String result = null;
        StringBuffer sb = new StringBuffer();
        if (name != null && value != null) {
            sb.append("\"");
            sb.append(name);
            sb.append("=");
            sb.append(value);
            sb.append("\"");
            result = sb.toString();
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "nameAndValueProperty", result);
        return result;
    }

    /**
     * @param prefix
     * @param key
     * @param spProps
     * @return key value as a string
     */
    @SuppressWarnings("unchecked")
    public static String getPropertyValue(String prefix, String key, Map<String, String> spProps) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getPropertyValue", new Object[] { prefix, key, spProps });
        String value = null;
        if (prefix != null && prefix.length() > 0 &&
                key != null && key.length() > 0 &&
                spProps != null && !spProps.isEmpty()) {
            String propName = prefix + key;
            for (Iterator<?> k = spProps.entrySet().iterator(); k.hasNext();) {
                Entry<String, String> entry = (Entry<String, String>) k.next();
                String name = (String) entry.getKey();
                if (name != null && name.equalsIgnoreCase(propName)) {
                    value = (String) entry.getValue();
                    break;
                }
            }
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "getPropertyValue", new Object[] { value });
        return value;
    }

    public static boolean isCustomPropStringGood(String propString) throws Exception
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "isCustomPropStringGood", propString);

        try {
            if (propString != null && propString.length() > 0)
            {
                if (!propString.startsWith("\"") && !propString.endsWith("\""))
                {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "isCustomPropStringGood", false);
                    return false;
                }

                // StringTokenizer tokenizer = new StringTokenizer(propString, "(\"\")+");
                StringTokenizer tokenizer = new StringTokenizer(propString, "\""); // PK84743

                while (tokenizer.hasMoreTokens())
                {
                    String token = tokenizer.nextToken();
                    if (token.indexOf("=") < 0)
                    {
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "isCustomPropStringGood", false);
                        return false;
                    }

                    if (tokenizer.hasMoreTokens())
                    {
                        String getComma = tokenizer.nextToken();
                        if (getComma != null && getComma.trim().equals(",") && tokenizer.hasMoreTokens())
                        {
                            continue;
                        }
                        else
                        {
                            if (tc.isEntryEnabled())
                                Tr.exit(tc, "isCustomPropStringGood", false);
                            return false;
                        }
                    }
                }
            }
        } catch (Exception e)
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception while tokenizing custom property string " + e.getMessage());
            throw e;
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "isCustomPropStringGood", true);

        return true;
    }

    public static void clearClientAndProviderConfigs() {
        clientsList.clear();
        providerConfigMap.clear();
        providerPluginClassLoaderMap.clear();
    }

    public static void setClients(List<OidcBaseClient> clients) {
        clientsList.addAll(clients);
    }

    public static List<OidcBaseClient> getClients() {
        return clientsList;
    }

    public static void setProviderConfigMap(Map<String, List<OAuth20Parameter>> configMap) {
        providerConfigMap.putAll(configMap);
    }

    public static Map<String, List<OAuth20Parameter>> getProviderConfigMap() {
        return providerConfigMap;
    }

    public static Map<String, ClassLoader> getProviderPluginClassLoaderMap() {
        return providerPluginClassLoaderMap;
    }

    public static Map<String, Object[]> getProviderJdbcCredentialsMap() {
        return jdbcCredentialsMap;
    }

    public static boolean isBuiltinClass(String className) {
        boolean isBuiltin =
                className.equals(BUILTIN_DB_PROVIDER_CLASS) ||
                        className.equals(BUILTIN_DB_TOKEN_STORE_CLASS) ||
                        className.equals(BUILTIN_BASE_PROVIDER_CLASS) ||
                        className.equals(BUILTIN_BASE_TOKEN_HANDLER_CLASS) ||
                        className.equals(BUILTIN_BASE_ID_TOKEN_HANDLER_CLASS) || // oidc10
                        className.equals(BUILTIN_GRANT_TYPE_HANDLER_FACTORY_CLASS) || // oidc10
                        className.equals(BUILTIN_RESPONSE_TYPE_HANDLER_FACTORY_CLASS) || // oidc10
                        className.equals(BUILTIN_MEDIATOR_CLASS) ||
                        className.equals(BUILTIN_SAMPLE_MEDIATOR_CLASS) ||
                        className.equals(BUILTIN_BASE_TOKEN_STORE_CLASS);
        // System.out.println("isBuiltinClass:" + isBuiltin + ":" + className);
        return isBuiltin;
    }

    public static boolean deleteClients(String providerId) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "deleteClients " + providerId);

        synchronized (clientsList) {
            for (Iterator<OidcBaseClient> it = clientsList.iterator(); it.hasNext();) {
                OidcBaseClient client = it.next();
                if (client.getComponentId().equals(providerId)) {
                    it.remove();
                }
            }
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "deleteClients");

        return true;
    }

    public static synchronized void setUserClaimsRetrieverService(UserClaimsRetrieverService claimsRetrieverService) {
        userClaimsRetrieverService = claimsRetrieverService;
    }

    public static synchronized UserClaimsRetrieverService getUserClaimsRetrieverService() {
        return userClaimsRetrieverService;
    }

    public static synchronized VMMService getVMMService() {
        return vmmServiceRef.getService();
    }

    public static synchronized void setJwtAccessTokenMediatorService(ConcurrentServiceReferenceMap<String, JwtAccessTokenMediator> jwtAccessTokenMediatorServiceRef) {
        jwtMediatorRef = jwtAccessTokenMediatorServiceRef;
    }

    public static synchronized ConcurrentServiceReferenceMap<String, JwtAccessTokenMediator> getJwtAccessTokenMediatorService() {
        return jwtMediatorRef;
    }

    public static synchronized void setIdTokenMediatorService(ConcurrentServiceReferenceMap<String, IDTokenMediator> idTokenMediatorServiceRef) {
        idTokenMediatorRef = idTokenMediatorServiceRef;
    }

    public static synchronized ConcurrentServiceReferenceMap<String, IDTokenMediator> getIdTokenMediatorService() {
        return idTokenMediatorRef;
    }

    public static void addSecurityService(String providerId, SecurityService securityService) {
        mapSecurityService.put(providerId, securityService);
    }

    public static void removeSecurityService(String providerId) {
        mapSecurityService.remove(providerId);
    }

    public static SecurityService getSecurityService(String providerId) {
        return mapSecurityService.get(providerId);
    }

    /**
     * 
     */
    public HashMap<String, OidcServerConfig> checkDuplicateOAuthProvider(ConcurrentServiceReferenceMap<String, OidcServerConfig> oidcServerConfigRef) {
        HashMap<String, OidcServerConfig> result = new HashMap<String, OidcServerConfig>();
        HashMap<String, String> mapProviderId = new HashMap<String, String>();
        HashMap<String, String> mapConfigId = new HashMap<String, String>();
        Set<String> configIDs = (Set<String>) oidcServerConfigRef.keySet();
        for (String configId : configIDs) {
            OidcServerConfig oidcServerConfig = oidcServerConfigRef.getService(configId);
            String oidcProviderId = oidcServerConfig.getProviderId();
            String oauthProviderName = oidcServerConfig.getOauthProviderName();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "oidcConfigId: " + oidcProviderId + " oauthProviderName: " + oauthProviderName);
            }
            String oldOidcProviderId = mapProviderId.get(oauthProviderName);
            if (oldOidcProviderId != null) {
                Tr.error(tc, "OIDC_SERVER_MULTI_OIDC_TO_ONE_OAUTH", new Object[] { oldOidcProviderId, oidcProviderId, oauthProviderName });
                String oldConfigId = mapConfigId.get(oauthProviderName);
                result.remove(oldConfigId);
            } else {
                mapProviderId.put(oauthProviderName, oidcProviderId);
                mapConfigId.put(oauthProviderName, configId);
                result.put(configId, oidcServerConfig);
            }
        }
        return result;
    }
}