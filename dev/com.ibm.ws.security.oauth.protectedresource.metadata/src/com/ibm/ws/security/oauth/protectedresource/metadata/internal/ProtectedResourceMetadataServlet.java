/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.security.oauth.protectedresource.metadata.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.authentication.filter.internal.IAuthenticationFilterInternal;
import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;

/**
 * Servlet that serves OAuth 2.0 Protected Resource Metadata.
 *
 * Endpoint: /.well-known/oauth-protected-resource/<path>
 *
 * Returns metadata for a single protected resource in RFC 9728 format:
 * {
 *   "resource": "https://example.com/protected/path",
 *   "authorization_servers": ["https://auth.example.com"]
 * }
 */
@Component(service = HttpServlet.class, property = {
    "osgi.http.whiteboard.servlet.pattern=/*"
})
public class ProtectedResourceMetadataServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(ProtectedResourceMetadataServlet.class);
    
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private volatile List<OidcClientConfig> oidcClientConfigs;
    
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private volatile List<AuthenticationFilter> authFilters;
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String requestedPath = req.getPathInfo();
        if (requestedPath == null || requestedPath.isEmpty() || "/".equals(requestedPath)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No resource path specified");
            return;
        }
        
        // Remove leading slash for matching
        if (requestedPath.startsWith("/")) {
            requestedPath = requestedPath.substring(1);
        }
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Looking for protected resource metadata for path: " + requestedPath);
        }
        
        // Find matching protected resource
        ProtectedResourceMetadata metadata = findProtectedResource(requestedPath, req);
        
        if (metadata == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No matching protected resource found for path: " + requestedPath);
            }
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No matching protected resource configuration found");
            return;
        }
        
        // Return JSON metadata
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        
        PrintWriter writer = resp.getWriter();
        writer.write(buildJsonResponse(metadata));
        writer.flush();
    }
    
    /**
     * Find a protected resource that matches the requested path.
     */
    private ProtectedResourceMetadata findProtectedResource(String requestedPath, HttpServletRequest req) {
        if (oidcClientConfigs == null || authFilters == null) {
            return null;
        }
        
        for (OidcClientConfig oidcConfig : oidcClientConfigs) {
            String authFilterId = oidcConfig.getAuthFilterId();
            if (authFilterId == null) {
                continue;
            }
            
            AuthenticationFilter matchingFilter = findAuthFilter(authFilterId);
            if (matchingFilter instanceof IAuthenticationFilterInternal) {
                IAuthenticationFilterInternal internal = (IAuthenticationFilterInternal) matchingFilter;
                
                List<Properties> urlPatterns = internal.getRequestUrlPatterns();
                if (urlPatterns != null && matchesPattern(requestedPath, urlPatterns)) {
                    return buildMetadata(requestedPath, oidcConfig, req);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Find an AuthenticationFilter by ID.
     */
    private AuthenticationFilter findAuthFilter(String authFilterId) {
        if (authFilters == null) {
            return null;
        }
        
        for (AuthenticationFilter filter : authFilters) {
            if (filter instanceof IAuthenticationFilterInternal) {
                IAuthenticationFilterInternal internal = (IAuthenticationFilterInternal) filter;
                if (authFilterId.equals(internal.getAuthFilterId())) {
                    return filter;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Check if the requested path matches any of the configured URL patterns.
     */
    private boolean matchesPattern(String requestedPath, List<Properties> urlPatterns) {
        for (Properties props : urlPatterns) {
            String urlPattern = props.getProperty("urlPattern");
            if (urlPattern != null && pathMatches(requestedPath, urlPattern)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Simple pattern matching logic.
     * Supports exact match and "contains" matching.
     */
    private boolean pathMatches(String requestedPath, String pattern) {
        // Normalize paths for comparison
        String normalizedPath = "/" + requestedPath;
        String normalizedPattern = pattern.startsWith("/") ? pattern : "/" + pattern;
        
        // Exact match
        if (normalizedPath.equals(normalizedPattern)) {
            return true;
        }
        
        // Contains match (simple substring check)
        if (normalizedPath.contains(normalizedPattern)) {
            return true;
        }
        
        // Wildcard support: /path/* matches /path/anything
        if (normalizedPattern.endsWith("/*")) {
            String prefix = normalizedPattern.substring(0, normalizedPattern.length() - 2);
            if (normalizedPath.startsWith(prefix)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Build metadata for a matched protected resource.
     */
    private ProtectedResourceMetadata buildMetadata(String path, OidcClientConfig config, HttpServletRequest req) {
        String resourceUrl = buildFullResourceUrl(path, req);
        String authzServer = getAuthorizationServerUrl(config);
        
        if (authzServer == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Could not determine authorization server URL for config: " + config.getId());
            }
            return null;
        }
        
        return new ProtectedResourceMetadata(resourceUrl, authzServer);
    }
    
    /**
     * Build the full resource URL from the request.
     */
    private String buildFullResourceUrl(String path, HttpServletRequest req) {
        String scheme = req.getScheme();
        String serverName = req.getServerName();
        int serverPort = req.getServerPort();
        
        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);
        
        // Only include port if it's not the default for the scheme
        if ((scheme.equals("http") && serverPort != 80) || 
            (scheme.equals("https") && serverPort != 443)) {
            url.append(":").append(serverPort);
        }
        
        url.append("/").append(path);
        
        return url.toString();
    }
    
    /**
     * Get the authorization server URL from the OIDC client configuration.
     * Prefers issuerIdentifier, falls back to deriving from authorization endpoint.
     */
    private String getAuthorizationServerUrl(OidcClientConfig config) {
        // First choice: issuerIdentifier from ConvergedClientConfig
        if (config instanceof ConvergedClientConfig) {
            ConvergedClientConfig converged = (ConvergedClientConfig) config;
            String issuer = converged.getIssuerIdentifier();
            if (issuer != null && !issuer.isEmpty()) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Using issuerIdentifier: " + issuer);
                }
                return issuer;
            }
        }
        
        // Fallback: derive base URL from authorization endpoint
        String authzEndpoint = config.getAuthorizationEndpointUrl();
        if (authzEndpoint != null && !authzEndpoint.isEmpty()) {
            String baseUrl = deriveBaseUrl(authzEndpoint);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Derived base URL from authorization endpoint: " + baseUrl);
            }
            return baseUrl;
        }
        
        return null;
    }
    
    /**
     * Derive the base authorization server URL from the authorization endpoint.
     * Removes common endpoint paths like /authorize, /oauth2/authorize, etc.
     */
    private String deriveBaseUrl(String authzEndpoint) {
        // Remove trailing slash
        String url = authzEndpoint.endsWith("/") ? 
            authzEndpoint.substring(0, authzEndpoint.length() - 1) : authzEndpoint;
        
        // Remove common authorization endpoint paths
        String[] suffixes = {"/authorize", "/oauth2/authorize", "/oidc/authorize"};
        for (String suffix : suffixes) {
            if (url.endsWith(suffix)) {
                return url.substring(0, url.length() - suffix.length());
            }
        }
        
        // If no known suffix, try to remove the last path segment
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash > 7) { // After "http://" or "https://"
            return url.substring(0, lastSlash);
        }
        
        return url;
    }
    
    /**
     * Build the JSON response for the protected resource metadata.
     */
    private String buildJsonResponse(ProtectedResourceMetadata metadata) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"resource\": \"").append(escapeJson(metadata.getResourceUrl())).append("\",\n");
        json.append("  \"authorization_servers\": [\n");
        json.append("    \"").append(escapeJson(metadata.getAuthorizationServer())).append("\"\n");
        json.append("  ]\n");
        json.append("}");
        return json.toString();
    }
    
    /**
     * Escape special characters for JSON.
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
    
    /**
     * Simple data class to hold protected resource metadata.
     */
    private static class ProtectedResourceMetadata {
        private final String resourceUrl;
        private final String authorizationServer;
        
        public ProtectedResourceMetadata(String resourceUrl, String authorizationServer) {
            this.resourceUrl = resourceUrl;
            this.authorizationServer = authorizationServer;
        }
        
        public String getResourceUrl() {
            return resourceUrl;
        }
        
        public String getAuthorizationServer() {
            return authorizationServer;
        }
    }
}

// Made with Bob
