package com.ibm.ws.security.common.config;

import java.util.ArrayList;
import java.util.Map;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.TraceConstants;
import com.ibm.ws.security.common.crypto.HashUtils;

public class DiscoveryConfigUtils {
    
    public static final TraceComponent tc = Tr.register(DiscoveryConfigUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    
    private JSONObject discoveryjson;
    private String tokenEndpointAuthMethod;
    private String scope;
    private String signatureAlgorithm;
    private String id;
    private String discoveryURL;

    private CommonConfigUtils configUtils = new CommonConfigUtils();

    private String discoveryDocumentHash;

    private long discoveryPollingRate;
    
    public static final String OPDISCOVERY_AUTHZ_EP_URL = "authorization_endpoint";
    public static final String OPDISCOVERY_TOKEN_EP_URL = "token_endpoint";
    public static final String OPDISCOVERY_INTROSPECTION_EP_URL = "introspection_endpoint";
    public static final String OPDISCOVERY_JWKS_EP_URL = "jwks_uri";
    public static final String OPDISCOVERY_USERINFO_EP_URL = "userinfo_endpoint";
    public static final String OPDISCOVERY_ISSUER = "issuer";
    public static final String OPDISCOVERY_TOKEN_EP_AUTH = "token_endpoint_auth_methods_supported";
    public static final String OPDISCOVERY_SCOPES = "scopes_supported";
    public static final String OPDISCOVERY_IDTOKEN_SIGN_ALG = "id_token_signing_alg_values_supported";
    
    public static final String CFG_KEY_SCOPE = "scope";
    public static final String CFG_KEY_TOKEN_ENDPOINT_AUTH_METHOD = "tokenEndpointAuthMethod";
    public static final String CFG_KEY_SIGNATURE_ALGORITHM = "signatureAlgorithm";
    public static final String KEY_authorizationEndpoint = "authorizationEndpoint";
    public static final String KEY_tokenEndpoint = "tokenEndpoint";
    public static final String KEY_USERINFO_ENDPOINT = "userInfoEndpoint";
    public static final String KEY_jwksUri = "jwksUri";
    public static final String KEY_ISSUER = "issuer";
    public static final String KEY_DISCOVERY_ENDPOINT = "discoveryEndpoint";
       
    public DiscoveryConfigUtils() {
        
    }

    public DiscoveryConfigUtils initialConfig(String configId, String ep, long discoveryRate) {
        id = configId;
        this.discoveryURL = ep;
        this.discoveryPollingRate = discoveryRate;
        return this;
    }
    
    public DiscoveryConfigUtils discoveredConfig(String alg, String tokenepAuthMethod, String scope) {
        this.signatureAlgorithm = alg;
        this.tokenEndpointAuthMethod = tokenepAuthMethod;
        this.scope = scope;
        return this;
    }
    
    public DiscoveryConfigUtils discoveryDocumentHash(String discoveryHash) {    
        this.discoveryDocumentHash = discoveryHash;
        return this;
    }
    
    public DiscoveryConfigUtils discoveryDocumentResult(JSONObject json) {    
        this.discoveryjson = json;
        return this;
    }

    public String adjustTokenEndpointAuthMethod() {
        ArrayList<String> discoveryTokenepAuthMethod = discoverOPConfig(discoveryjson.get(OPDISCOVERY_TOKEN_EP_AUTH));
        if (isRPUsingDefault("authMethod") && !opHasRPDefault("authMethod", discoveryTokenepAuthMethod)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "See if we need to adjusted the token endpoint authmethod. The original is : " + tokenEndpointAuthMethod);
            }
            String supported  = rpSupportsOPConfig("authMethod", discoveryTokenepAuthMethod);
            if (supported != null) {
                Tr.info(tc,  "OIDC_CLIENT_DISCOVERY_OVERRIDE_DEFAULT", this.tokenEndpointAuthMethod, CFG_KEY_TOKEN_ENDPOINT_AUTH_METHOD, supported, getId());
                this.tokenEndpointAuthMethod = supported;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "The adjusted value is : " + tokenEndpointAuthMethod);
                }
            }           
        }
        return this.tokenEndpointAuthMethod;
    }
    
    private String getId() {
        return id;
    }

    /**
     * @return 
     * 
     */
    public String adjustScopes() {
        ArrayList<String> discoveryScopes = discoverOPConfig(discoveryjson.get(OPDISCOVERY_SCOPES));
        if (isRPUsingDefault("scope") && !opHasRPDefault("scope", discoveryScopes)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "See if we need to adjusted the scopes. The original is : " + this.scope);
            }
            String supported  = rpSupportsOPConfig("scope", discoveryScopes);
            if (supported != null) {
                Tr.info(tc,  "OIDC_CLIENT_DISCOVERY_OVERRIDE_DEFAULT", this.scope, CFG_KEY_SCOPE, supported, getId());
                this.scope = supported;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "The adjusted value is : " + this.scope);
                }
            }           
        }
        return this.scope;
    }
    
    /**
     * @param discoveryTokenepAuthMethod
     * @return
     */
    private String rpSupportsOPConfig(String key, ArrayList<String> values) {

        String rpSupportedSignatureAlgorithms = "HS256 RS256";
        String rpSupportedTokenEndpointAuthMethods = "post basic";
        String rpSupportedScopes = "openid profile";

        if ("alg".equals(key) && values != null) {
            for (String value : values) {
                if (rpSupportedSignatureAlgorithms.contains(value)) {
                    return value;
                }
            }
        }

        if ("authMethod".equals(key) && values != null) {
            for (String value : values) {
                value = matchingRPValue(value);
                if (rpSupportedTokenEndpointAuthMethods.contains(value)) {
                    return value;
                }
            }
        }

        if ("scope".equals(key) && values != null) {
            String scopes = null;
            for (String value : values) {
                if (rpSupportedScopes.contains(value)) {
                    if (scopes == null) {
                        scopes = value;
                    }
                    else {
                        scopes = scopes + " " + value;
                    }
                }
            }
            return scopes;
        }
        return null;
    }
    
    /**
     * @param value
     * @return
     */
    private String matchingRPValue(String value) {
        if ("client_secret_post".equals(value)) {
            return "post";
        } else if ("client_secret_basic".equals(value)) {
            return "basic";
        }
        return value;
    }
    
    /**
     * @param object
     */
    private ArrayList<String> discoverOPConfig(Object obj) {
        return jsonValue(obj);
    }

    /**
     * @param obj
     * @return
     */
    private ArrayList<String> jsonValue(Object obj) {
        ArrayList<String> str = new ArrayList<String>();
        int index = 0;
        if (obj != null) {
            if (obj instanceof String) {
                str.add(index, (String) obj);
                return str;
            } else if (obj instanceof JSONArray) {
                return parseJsonArray((JSONArray)obj);
            }
        }
        return null;
    }

    /**
     * @param obj
     * @return
     */
    private ArrayList<String> parseJsonArray(JSONArray jsonArrayOfStrings) {
        ArrayList<String> jsonString = new ArrayList<String>();
        int index = 0;

        if (jsonArrayOfStrings != null) {
            for (Object strObj : jsonArrayOfStrings) {
                if (strObj instanceof String) {
                    jsonString.add(index, (String) strObj);
                    index++;
                }
            }
        }
       
        return jsonString;
    }

    
    /**
     * @param string
     * @return
     */
    private boolean opHasRPDefault(String key, ArrayList<String> opconfig) {

        if ("authMethod".equals(key)) {
            return matches("client_secret_post", opconfig);
        } else if ("alg".equals(key)) {
            return matches("HS256", opconfig);
        } else if ("scope".equals(key)) {
            return matches("openid", opconfig) && matches("profile", opconfig);
        }
        return false;
    }

    private boolean matches(String rpdefault, ArrayList<String> opconfig) {
        for (String str : opconfig) {
            if (rpdefault.equals(str)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(String rpdefault, String rpconfig) {
        return rpconfig.equals(rpdefault);
    }

    /**
     * @param string
     * @return
     */
    private boolean isRPUsingDefault(String key) {
        if ("authMethod".equals(key)) {
            return matches("post", this.tokenEndpointAuthMethod);
        } else if ("alg".equals(key)) {
            return matches("HS256", this.signatureAlgorithm);
        } else if ("scope".equals(key)) {
            return matches("openid profile", this.scope);
        }
        return false;
    }
    
    /**
     * @param object
     * @return
     */
    public String discoverOPConfigSingleValue(Object object) {
       
        return jsonValue(object).get(0);
    }

    /**
     * @param props
     */
    public void logDiscoveryWarning(Map<String, Object> props) {
        String endpoints = "";
        String ep = null;
        if ((ep = configUtils .trim((String) props.get(KEY_authorizationEndpoint))) != null) {
            endpoints = buildDiscoveryWarning(endpoints, KEY_authorizationEndpoint);
        }
        if((ep = configUtils.trim((String) props.get(KEY_tokenEndpoint))) != null) {
            endpoints = buildDiscoveryWarning(endpoints, KEY_tokenEndpoint);
        }
        if ((ep = configUtils.trim((String) props.get(KEY_USERINFO_ENDPOINT))) != null) {
            endpoints = buildDiscoveryWarning(endpoints, KEY_USERINFO_ENDPOINT);
        }
        if ((ep = configUtils.trim((String) props.get(KEY_jwksUri))) != null) {
            endpoints = buildDiscoveryWarning(endpoints, KEY_jwksUri);
        } 
        if (!endpoints.isEmpty()) {
            logWarning("OIDC_CLIENT_DISCOVERY_OVERRIDE_EP", endpoints);
        }
        
        if ((ep = configUtils.trim((String) props.get(KEY_ISSUER))) != null) {
            logWarning("OIDC_CLIENT_DISCOVERY_OVERRIDE_ISSUER", KEY_ISSUER);
        }
        
    }

    /**
     * @param endpoints
     */
    private void logWarning(String key, String endpoints) {
           
        Tr.warning(tc, key, KEY_DISCOVERY_ENDPOINT, endpoints, getId());
        
    }

    /**
     * @param endpoints
     * @param ep
     * @return
     */
    private String buildDiscoveryWarning(String endpoints, String ep) { 
        return endpoints.concat(ep).concat(", ");
    }
    
    /**
     * @param string
     */
    public void logDiscoveryMessage(String key, String nlsMessage, String defaultMessage) {
        //String defaultMessage = "Error processing discovery request";

        if (nlsMessage != null) {
            Tr.info(tc, nlsMessage);
        } else {
            Tr.info(tc, getNlsMessage(key, defaultMessage));
        }     
    }

    private String getNlsMessage(String key, String defaultMessage) {
        String message = defaultMessage;
        String bundleName = "com.ibm.ws.security.common.internal.resources.SSOCommonMessages";
        message = TraceNLS.getFormattedMessage(getClass(),
                bundleName, key,
                new Object[] { getId(), this.discoveryURL }, defaultMessage);
             
        return message;
    }

    public boolean calculateDiscoveryDocumentHash(JSONObject json) {
        String latestDiscoveryHash = HashUtils.digest(json.toString());
        String OIDC_CLIENT_DISCOVERY_UPDATED_CONFIG="CWWKS6111I: The client [{" + getId() + "}] configuration has been updated with the new information received from the discovery endpoint URL [{"+ this.discoveryURL + "}].";
        boolean updated = false;
        if (this.discoveryDocumentHash == null || !this.discoveryDocumentHash.equals(latestDiscoveryHash)) {
            if (this.discoveryDocumentHash != null) {
                logDiscoveryMessage("OIDC_CLIENT_DISCOVERY_UPDATED_CONFIG", null, OIDC_CLIENT_DISCOVERY_UPDATED_CONFIG);
            }
            updated = true;
            this.discoveryDocumentHash = latestDiscoveryHash;
        } else if (this.discoveryDocumentHash != null && this.discoveryDocumentHash.equals(latestDiscoveryHash)) {
            String OIDC_CLIENT_DISCOVERY_NOT_UPDATED_CONFIG="CWWKS6112I: The client [{" + getId() + "}] configuration is consistent with the information from the discovery endpoint URL [{"+ this.discoveryURL + "}], so no configuration updates are needed.";
            logDiscoveryMessage("OIDC_CLIENT_DISCOVERY_NOT_UPDATED_CONFIG", null, OIDC_CLIENT_DISCOVERY_NOT_UPDATED_CONFIG);
        }
        return updated;
    }
    
    public String getDiscoveryDocumentHash() {
        return this.discoveryDocumentHash;
    }
    
}
