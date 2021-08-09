/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.metadata;

import java.util.List;

/**
 * The SecurityConstraint represents a single security-constraint element in the web.xml.
 * <p/>
 * <pre>
 * &lt;security-constraint&gt;
 * &lt;web-resource-collection&gt;
 * &lt;web-resource-name&gt;web-resource-name&lt;/web-resource-name&gt;
 * &lt;url-pattern&gt;url-pattern&lt;/url-pattern&gt;
 * &lt;http-method&gt;http-method&lt;/http-method&gt;
 * &lt;http-method-omission&gt;http-method-omission&lt;/http-method-omission&gt;
 * &lt;/web-resource-collection&gt;
 * &lt;auth-constraint&gt;
 * &lt;role-name&gt;role-name&lt;/role-name&gt;
 * &lt;/auth-constraint&gt;
 * &lt;user-data-constraint&gt;
 * &lt;transport-guarantee&gt;
 * transport-guarantee
 * &lt;/transport-guarantee&gt;
 * &lt;/user-data-constraint&gt;
 * &lt;/security-constraint&gt;
 * </pre>
 */
public class SecurityConstraint {

    private final List<WebResourceCollection> webResourceCollections;
    private final List<String> roles;
    private final boolean sslRequired;
    private final boolean accessPrecluded;
    private final boolean fromHttpConstraint;
    private final boolean accessUncovered;

    /**
     * Constructs a SecurityContraint object. List parameters are guaranteed to contain values or be empty lists.
     * 
     * @param webResourceCollections The list of WebResourceCollection objects. Cannot be <code>null</code>.
     * @param roles The list of roles. Cannot be <code>null</code>.
     * @param sslRequired The flag that indicates if SSL is required.
     * @param accessPrecluded The flag that indicates if access is precluded.
     */
    public SecurityConstraint(List<WebResourceCollection> webResourceCollections, List<String> roles, boolean sslRequired, boolean accessPrecluded, boolean fromHttpConstraint,
                              boolean accessUncovered) {
        if (accessPrecluded == true && roles != null && roles.size() > 0) {
            throw new IllegalArgumentException("The roles must be empty when access is precluded.");
        }
        this.webResourceCollections = webResourceCollections;
        this.roles = roles;
        this.sslRequired = sslRequired;
        this.accessPrecluded = accessPrecluded;
        this.fromHttpConstraint = fromHttpConstraint;
        this.accessUncovered = accessUncovered;
    }

    public List<WebResourceCollection> getWebResourceCollections() {
        return webResourceCollections;
    }

    public List<String> getRoles() {
        return roles;
    }

    public boolean isSSLRequired() {
        return sslRequired;
    }

    public boolean isAccessPrecluded() {
        return accessPrecluded;
    }

    public boolean isFromHttpConstraint() {
        return fromHttpConstraint;
    }

    public boolean isAccessUncovered() {
        return accessUncovered;
    }
}
