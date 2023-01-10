/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.org.apache.myfaces40.fat.externalContext.addResponseCookie.bean;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

/**
 * Add Cookies to the Response using the ExternalContext addResponseCookie API.
 */
@Named
@RequestScoped
public class ExternalContextAddResponseCookieBean {

    public void addCookies() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();

        // Add a Cookie with the samesite attribute set
        Map<String, Object> sameSiteCookieProperties = new HashMap<>();
        sameSiteCookieProperties.put("samesite", "lax");
        externalContext.addResponseCookie("sameSiteCookieName", "sameSiteCookieValue", sameSiteCookieProperties);

        // Add a Cookie with the httpOnly defined Cookie attribute.
        Map<String, Object> httpOnlyCookieProperties = new HashMap<>();
        httpOnlyCookieProperties.put("httpOnly", true);
        externalContext.addResponseCookie("httpOnlyCookieName", "httpOnlyCookieValue", httpOnlyCookieProperties);

        // Add a Cookie with an undefined Cookie attribute
        Map<String, Object> undefinedCookieProperties = new HashMap<>();
        undefinedCookieProperties.put("undefinedAttributeName", "undefinedAttributeValue");
        externalContext.addResponseCookie("undefinedCookieName", "undefinedCookieValue", undefinedCookieProperties);
    }
}
