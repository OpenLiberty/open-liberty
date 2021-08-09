/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.form;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import com.ibm.oauth.core.api.attributes.Attribute;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.oauth20.client.OAuth20Client;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.util.JSONUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.oauth20.util.Nonce;
import com.ibm.ws.security.oauth20.util.TemplateRetriever;

public class FormRenderer {
    private static TraceComponent tc = Tr.register(FormRenderer.class,
            "OAuth20Provider", "com.ibm.ws.security.oauth20.resources.ProviderMsgs");

    private static final String JS_PATH = "/scripts/oauthForm.js";

    public static final String ATTR_OAUTH_FORM_DATA = "oauthFormData";
    public static final String FORM_AUTHORIZATION_URL = "authorizationUrl";
    public static final String FORM_NONCE = "consentNonce";
    public static final String FORM_CLIENT_DISPLAYNAME = "clientDisplayName";
    public static final String FORM_EXTENDED_PROPERTIES = "extendedProperties";

    private static final TemplateRetriever retriever = new TemplateRetriever();
    private static final Set<String> requiredAttributes = Collections
            .unmodifiableSet(new HashSet<String>(Arrays.asList(new String[] {
                    OAuth20Constants.CLIENT_ID,
                    OAuth20Constants.CLIENT_SECRET,
                    OAuth20Constants.RESPONSE_TYPE,
                    OAuth20Constants.REDIRECT_URI,
                    OAuth20Constants.STATE,
                    OAuth20Constants.SCOPE
            })));

    public void renderForm(OAuth20Client client, String templateUrl, String contextPath,
            String authorizationUrl, Nonce nonce, AttributeList attributes, String acceptLanguage,
            HttpServletResponse response) throws IOException {

        renderForm(client, templateUrl, contextPath, authorizationUrl, nonce, attributes, acceptLanguage,
                response, null);
    }

    public void renderForm(OAuth20Client client, String templateUrl, String contextPath,
            String authorizationUrl, Nonce nonce, AttributeList attributes, String acceptLanguage,
            HttpServletResponse response, byte[] defaultAuthorizationFormTemplatecontent) throws IOException {

        // if using default auth form, don't load it over the network, which fails with proxy. Load direct instead.
        TemplateRetriever.Item template = null;
        if (defaultAuthorizationFormTemplatecontent == null) {
            template = retriever.getTemplate(templateUrl, acceptLanguage);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Using authorization form from memory");
            }
            template = new TemplateRetriever.Item(defaultAuthorizationFormTemplatecontent, "text/html; charset=UTF-8");
        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.put(FORM_NONCE, nonce.getValue());
        map.put(OAuth20Constants.CLIENT_ID, attributes.getAttributeValueByName(OAuth20Constants.CLIENT_ID));
        map.put(OAuth20Constants.RESPONSE_TYPE, attributes.getAttributeValueByName(OAuth20Constants.RESPONSE_TYPE));
        map.put(OAuth20Constants.REDIRECT_URI, attributes.getAttributeValueByName(OAuth20Constants.REDIRECT_URI));
        // map.put(OAuth20Constants.STATE, attributes.getAttributeValueByName(OAuth20Constants.STATE));

        String strState = attributes.getAttributeValueByName(OAuth20Constants.STATE);
        if (strState != null && strState.length() > 0) {
            String encoding = response.getCharacterEncoding();
            String encodedState = URLEncoder.encode(strState, encoding != null ? encoding : "UTF-8");
            map.put(OAuth20Constants.STATE, encodedState);
        } else {
            map.put(OAuth20Constants.STATE, strState);
        }

        map.put(OAuth20Constants.SCOPE, attributes.getAttributeValuesByName(OAuth20Constants.SCOPE));
        map.put(FORM_CLIENT_DISPLAYNAME, client.getClientName());

        if (attributes.getAttributeValuesByName(OAuth20Constants.RESOURCE) != null) {
            map.put(OAuth20Constants.RESOURCE, attributes.getAttributeValuesByName(OAuth20Constants.RESOURCE));
        }
        String[] resources = attributes.getAttributeValuesByName(OAuth20Constants.RESOURCE);
        if (resources != null) {
            String resource = arrayToString(resources);
            // resource = URLEncoder.encode(resource, encoding != null ? encoding : "UTF-8");
            map.put(OAuth20Constants.RESOURCE, resource);
        }

        Map<String, Object> extendedProperties = new HashMap<String, Object>();
        for (Attribute attr : attributes.getAllAttributes()) {
            String name = attr.getName();
            if (!requiredAttributes.contains(name)) {
                String[] values = attr.getValuesArray();
                if (values == null || values.length == 0) {
                    extendedProperties.put(name, null);
                } else if (values.length > 1) {
                    extendedProperties.put(name, values);
                } else {
                    extendedProperties.put(name, values[0]);
                }
            }
        }

        map.put(FORM_EXTENDED_PROPERTIES, extendedProperties);

        // set other client provided request parameters
        StringBuilder data = JSONUtil.getJSON(map);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "oauth form data is " + data.toString());
        }

        response.setContentType(template.getContentType());
        OutputStream os = response.getOutputStream();
        os.write(template.getContent());
        StringBuilder sb = new StringBuilder();

        sb.append("<script type=\"text/javascript\" src=\"")
                .append(contextPath)
                .append(JS_PATH)
                .append("\"></script>")
                .append("<script language=\"javascript\">")
                .append("var ").append(ATTR_OAUTH_FORM_DATA).append("=").append(data)
                .append(";")
                .append(";var loc=document.location;loc=loc.href.substring(0,loc.href.indexOf(loc.pathname))+loc.pathname;") // @bj1
                .append(ATTR_OAUTH_FORM_DATA).append(".").append("authorizationUrl").append("=loc;") // @bj1
                .append("</script>");
        os.write(Base64Coder.getBytes(sb.toString()));
        os.close();
    }

    /**
     * @param values
     * @return
     */
    private String arrayToString(String[] values) {
        if (values != null) {
            String result = "";
            for (int iI = 0; iI < values.length; iI++) {
                if (iI > 0)
                    result = result.concat(" ");
                result = result.concat(values[iI]);
            }
            return result;
        }
        return null;
    }
}