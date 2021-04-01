/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.tai;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WebTrustAssociationException;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;
import com.ibm.wsspi.security.token.AttributeNameConstants;

public class OidcTAI implements TrustAssociationInterceptor {
    private static final TraceComponent tc = Tr.register(OidcTAI.class);
    static final String CFG_KEY_PROPERTIES_PID = "properties";
    static final String KEY_CONFIGURATION_ADMIN = "configurationAdmin";
    private final AtomicServiceReference<ConfigurationAdmin> configAdminRef = new AtomicServiceReference<ConfigurationAdmin>(KEY_CONFIGURATION_ADMIN);
    private final Properties properties = new Properties();
    static final String client_id = "client_id";
    static String okClientId = null;

    public static final String Authorization_Header = "Authorization";
    public static final String AUTHORIZATION_ENCODING = "Authorization-Encoding";
    public static final String BasicAuthEncoding = System.getProperty("com.ibm.websphere.security.BasicAuthEncoding", "UTF-8");

    static final String WSCREDENTIAL_CACHE_KEY_INTERNAL_ASSERTION = "com.ibm.ws.authentication.internal.key.assertion";
    static final String INTERNAL_ASSERTION_KEY = "com.ibm.ws.authentication.internal.assertion";

    protected static final Pattern OIDC_PATTERN = Pattern.compile("/(oidc|oauth2)/(endpoint|providers)/[\\w_]+/(authorize|registration)");
    protected static final String OAUTH_APPLICATION = "com.ibm.ws.security.oauth20";
    protected static final String OIDC_APPLICATION = "com.ibm.ws.security.openidconnect.server";
    protected static final Pattern CONSENT_PATTERN = Pattern.compile("/(oidc|oauth2)/template.html");

    /** Default Constructor **/
    public OidcTAI() {}

    /**
     * <customTAI id="myCustomTAI">
     * <properties scenario="nonZero"/>
     * </customTAI>
     **/
    protected void activate(ComponentContext componentContext, Map<String, Object> newProperties) {
        configAdminRef.activate(componentContext);
        modified(newProperties);
    }

    protected void modified(Map<String, Object> newProperties) {
        properties.clear();
        processProperties(newProperties.get(CFG_KEY_PROPERTIES_PID));
        if (!properties.isEmpty()) {
            try {
                this.initialize(properties);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void deactivate() {
        properties.clear();
    }

    protected void setConfigurationAdmin(ServiceReference<ConfigurationAdmin> ref) {
        configAdminRef.setReference(ref);
    }

    protected void unsetConfigurationAdmin(ServiceReference<ConfigurationAdmin> ref) {
        configAdminRef.unsetReference(ref);
    }

    @Override
    public void cleanup() {}

    @Override
    public String getType() {
        return "OidcTAI";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public int initialize(Properties props)
                    throws WebTrustAssociationFailedException {
        // Initialize TAI according to the configuration values
        return 0;
    }

    @Override
    public boolean isTargetInterceptor(HttpServletRequest req)
                    throws WebTrustAssociationException {
        Enumeration<String> attrKeys = req.getAttributeNames();
        while (attrKeys.hasMoreElements()) {
            String attrKey = attrKeys.nextElement();
            Object obj = req.getAttribute(attrKey);
            System.out.println("key:" + attrKey + " value:" + obj);
        }

        if (!isOidcAuthorize(req)) {
            System.out.println("This is not a request for oidc or oauth2");
            return false;
        }

        String clientId = getClientId(req);
        okClientId = properties.getProperty(client_id);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "properties okClientId:" + okClientId + " client_id:" + clientId);
        }

        System.out.println("Yes, this is a request for OpenID Connect or OAuth");
        return true;
    }

    @Override
    public TAIResult negotiateValidateandEstablishTrust(HttpServletRequest req, HttpServletResponse rsp)
                    throws WebTrustAssociationFailedException {
        String result = properties.getProperty("result");
        int iResult = result == null ? 1 : Integer.valueOf(result);;
        Subject subject = new Subject();
        String user = properties.getProperty("username");
        String realm = properties.getProperty("realm");
        String username = (realm != null && user != null) ? "user:" + realm + "/" + user : "user:BasicRealm/testuser";
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "return " + iResult + " userName:" + username);
        }
        if (iResult == 0) {
            return TAIResult.create(HttpServletResponse.SC_OK, username);
        } else if (iResult == 1) {

            Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
            hashtable.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, username);
            hashtable.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, user);
            hashtable.put(AttributeNameConstants.WSCREDENTIAL_REALM, realm);
            //hashtable.put(WSCREDENTIAL_CACHE_KEY_INTERNAL_ASSERTION, Boolean.TRUE);
            subject.getPublicCredentials().add(hashtable);
            return TAIResult.create(HttpServletResponse.SC_OK, user, subject);
        } else if (iResult == 2) {
            return TAIResult.create(HttpServletResponse.SC_FORBIDDEN);
        } else if (iResult == 3) {
            return TAIResult.create(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } else {
            return TAIResult.create(HttpServletResponse.SC_CONTINUE);
        }
    }

    /**
     * @param configAdmin
     * @param obj
     */
    private void processProperties(Object o) {
        properties.clear();
        if (o == null)
            return;

        String pid = (String) o;
        ConfigurationAdmin configAdmin = configAdminRef.getServiceWithException();

        Configuration[] configList = null;
        try {
            // We do not want to create a missing pid, only find one that we were told exists
            configList = configAdmin.listConfigurations(FilterUtils.createPropertyFilter("service.pid", pid));
        } catch (InvalidSyntaxException e) {
        } catch (IOException e) {
        }

        if (configList != null && configList.length > 0) {
            // Just get the first one (there should only ever be one.. )
            Dictionary<String, ?> cProps = configList[0].getProperties();
            Enumeration<String> keys = cProps.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                // Skip certain keys
                if (key.startsWith(".")
                    || key.startsWith("config.")
                    || key.startsWith("service.")
                    || key.equals("id")) {
                    continue;
                }
                Object value = cProps.get(key);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "properties key:" + key + " value:" + value);
                }
                properties.put(key, value);
            }
        }
    }

    String getClientId(HttpServletRequest req) {
        String clientId = null;
        //String clientSecret = null;
        String hdrValue = req.getHeader(Authorization_Header);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, Authorization_Header + "=" + hdrValue);
        }
        if (hdrValue == null || !hdrValue.startsWith("Basic ")) {
            //clientSecret = req.getParameter("client_secret");
            clientId = req.getParameter("client_id");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "None-Basic clientID:" + clientId);
            }
            if (clientId == null) {
                String queryString = req.getQueryString();
                if (queryString != null) {
                    String[] params = queryString.split("&");
                    for (int iI = 0; iI < params.length; iI++) {
                        if (params[iI].startsWith("client_id=")) {
                            clientId = params[iI].substring(10);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Query clientID:" + clientId);
                            }
                        }
                    }
                }
            }
        } else {
            String encoding = req.getHeader(AUTHORIZATION_ENCODING);
            hdrValue = decodeAuthorizationHeader(hdrValue, encoding);
            int idx = hdrValue.indexOf(':');
            if (idx < 0) {
                clientId = hdrValue;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "()Basic clientID:" + clientId);
            } else {
                clientId = hdrValue.substring(0, idx);
                //clientSecret = hdrValue.substring(idx + 1);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "[]Basic clientID:" + clientId);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Use authentication data from Authentication head for client:" + clientId);

        return clientId;
    }

    private final static String base64chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    public static String decodeAuthorizationHeader(String hdrValue, String encoding) {
        if (hdrValue == null) {
            return null;
        }
        String source = hdrValue.substring(6);
        // remove/ignore any characters not in the base64 characters list
        // or the pad character -- particularly newlines
        source = source.replaceAll("[^" + base64chars + "=]", "");

        // replace any incoming padding with a zero pad (the 'A' character is
        // zero)
        String pad = (source.charAt(source.length() - 1) == '=' ?
                        (source.charAt(source.length() - 2) == '=' ? "AA" : "A") : "");
        String result = "";
        source = source.substring(0, source.length() - pad.length()) + pad;

        // increment over the length of this encoded string, four characters
        // at a time
        for (int cnt = 0; cnt < source.length(); cnt += 4) {

            // each of these four characters represents a 6-bit index in the
            // base64 characters list which, when concatenated, will give the
            // 24-bit number for the original 3 characters
            int n = (base64chars.indexOf(source.charAt(cnt)) << 18)
                    + (base64chars.indexOf(source.charAt(cnt + 1)) << 12)
                    + (base64chars.indexOf(source.charAt(cnt + 2)) << 6)
                    + base64chars.indexOf(source.charAt(cnt + 3));

            // split the 24-bit number into the original three 8-bit (ASCII)
            // characters
            result += "" + (char) ((n >>> 16) & 0xFF) + (char) ((n >>> 8) & 0xFF)
                      + (char) (n & 0xFF);
        }

        // remove any zero pad that was added to make this a multiple of 24 bits
        return result.substring(0, result.length() - pad.length());
    }

    private boolean isOidcAuthorize(HttpServletRequest req) {
        boolean isOidcAuthorize = false;

        String uri = req.getRequestURI();
        Matcher matcher = OIDC_PATTERN.matcher(uri);
        isOidcAuthorize = matcher.matches();
        return isOidcAuthorize;
    }

}
