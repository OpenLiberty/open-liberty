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
package com.ibm.ws.security.oauth20.error.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.util.JSONUtil;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.oauth20.util.TemplateRetriever;

public class ErrorPageRenderer {

    public static final String ATTR_OAUTH_ERROR = "oauthError";
    public static final String ERROR = "error";
    public static final String ERROR_DESCRIPTION = "error_description";
    public static final String ERROR_URI = "error_uri";
    public static final String ATTR_OAUTH_FORM_CLIENT = "oauthClient";
    public static final String ATTR_OAUTH_FORM_NONCE = "oauthNonce";
    public static final String ATTR_OAUTH_FORM_ATTRIBUTES = "oauthAttributes";

    private static final TemplateRetriever retriever = new TemplateRetriever();

    public void renderErrorPage(OAuthException exception,
            String templateUrl,
            String acceptLanguage,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String encoding = request.getCharacterEncoding() != null ? request.getCharacterEncoding() : "utf-8";
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(ERROR, exception.getError());
        map.put(ERROR_DESCRIPTION, exception.formatSelf(request.getLocale(), encoding));
        StringBuilder data = JSONUtil.getJSON(map);

        TemplateRetriever.Item template = retriever.getTemplate(templateUrl, acceptLanguage);
        response.setContentType(template.getContentType());
        OutputStream os = response.getOutputStream();
        os.write(template.getContent());
        StringBuilder sb = new StringBuilder();
        sb.append("<script language=\"javascript\">")
                .append("var ").append(ATTR_OAUTH_ERROR).append("=").append(data)
                .append(";")
                .append("</script>");
        os.write(Base64Coder.getBytes(sb.toString()));
        os.close();
    }

}
