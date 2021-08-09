/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.cors.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@Component(service = { CorsConfig.class }, configurationPid = "com.ibm.ws.webcontainer.cors", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true, property = { "service.vendor=IBM" })
public class CorsConfig {
    private static final TraceComponent tc = Tr.register(CorsConfig.class);

    public static final String CONFIG_DOMAIN = "domain";
    public static final String CONFIG_ALLOWED_ORIGINS = "allowedOrigins";
    public static final String CONFIG_ALLOWED_METHODS = "allowedMethods";
    public static final String CONFIG_ALLOWED_HEADERS = "allowedHeaders";
    public static final String CONFIG_MAX_AGE = "maxAge";
    public static final String CONFIG_ALLOW_CREDENTIALS = "allowCredentials";
    public static final String CONFIG_EXPOSE_HEADERS = "exposeHeaders";

    private String domain;
    private List<String> allowedOrigins;
    private List<String> allowedMethods;
    private List<String> allowedHeaders;
    private Long maxAge;
    private boolean allowCredentials;
    private String exposeHeaders;

    public CorsConfig() {}

    public CorsConfig(Map<String, Object> properties) {
        processCors(properties);
    }

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        processCors(properties);
    }

    @Modified
    protected void modified(Map<String, Object> properties) throws IOException {
        processCors(properties);
    }

    private void processCors(Map<String, Object> properties) {
        //Process the domain we're setting up CORS for
        //Don't need to check for null because this is a required attribute, so the configuration manager enforces it
        domain = (String) properties.get(CONFIG_DOMAIN);

        //We make sure the configured domain starts with a slash, because RESTRequest.getPath() always gives something that starts with slash, so it will be easier to match.
        if (domain.charAt(0) != '/') {
            domain = "/" + domain;
        }

        //Process allowed origins.  Can be *, a set of origins, or the string null (default)
        String configOrigins = (String) properties.get(CONFIG_ALLOWED_ORIGINS);
        if (configOrigins.contains("*")) {
            allowedOrigins = Arrays.asList("*");
        } else {
            allowedOrigins = Arrays.asList(configOrigins.trim().split("\\s*,\\s*"));
        }

        //Process allowed methods
        String allowedMethodsAsString = (String) properties.get(CONFIG_ALLOWED_METHODS);
        if (allowedMethodsAsString == null || allowedMethodsAsString.isEmpty()) {
            allowedMethods = Collections.emptyList();
        } else {
            allowedMethods = Arrays.asList(allowedMethodsAsString.trim().split("\\s*,\\s*"));
        }

        //Process allowed headers
        String allowedHeadersAsString = (String) properties.get(CONFIG_ALLOWED_HEADERS);
        if (allowedHeadersAsString == null || allowedHeadersAsString.isEmpty()) {
            allowedHeaders = Collections.emptyList();
        } else {
            allowedHeaders = Arrays.asList(allowedHeadersAsString.trim().split("\\s*,\\s*"));
        }

        //Process expose headers
        exposeHeaders = (String) properties.get(CONFIG_EXPOSE_HEADERS);

        //Process max age
        maxAge = (Long) properties.get(CONFIG_MAX_AGE);

        //Process allow credentials
        allowCredentials = Boolean.TRUE.equals(properties.get(CONFIG_ALLOW_CREDENTIALS));

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, toString());
        }
    }

    @Override
    public String toString() {
        return "CorsConfig : domain: " + domain +
               " | allowedOrigins: " + Arrays.toString(allowedOrigins.toArray()) +
               " | allowedMethods: " + Arrays.toString(allowedMethods.toArray()) +
               " | allowedHeaders: " + Arrays.toString(allowedHeaders.toArray()) +
               " | allowCredentials: " + allowCredentials +
               " | maxAge: " + maxAge +
               " | exposeHeaders: " + exposeHeaders;
    }

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {}

    public String getDomain() {
        return domain;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public boolean getAllowCredentials() {
        return allowCredentials;
    }

    public Long getMaxAge() {
        return maxAge;
    }

    public String getExposedHeaders() {
        return exposeHeaders;
    }
}
