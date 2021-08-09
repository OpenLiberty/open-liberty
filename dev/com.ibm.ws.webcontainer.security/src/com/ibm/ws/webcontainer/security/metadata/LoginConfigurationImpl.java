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

/**
 * Represents a login-config element in web.xml.
 * <p/>
 *
 * <pre>
 * &lt;login-config&gt;
 * &lt;auth-method&gt;auth-method&lt;/auth-method&gt;
 * &lt;realm-name&gt;realm-name&lt;/realm-name&gt;
 * &lt;form-login-config&gt;
 * &lt;form-login-page&gt;form-login-page&lt;/form-login-page&gt;
 * &lt;form-error-page&gt;form-error-page&lt;/form-error-page&gt;
 * &lt;/form-login-config&gt;
 * &lt;/login-config>
 * </pre>
 */
public class LoginConfigurationImpl implements LoginConfiguration {
    /**
     * Client certificate authentication value for &lt;auth-method&gt;.
     */
    public static final String CLIENT_CERT_AUTH_METHOD = "CLIENT-CERT";

    private final String authenticationMethod;
    private final String realmName;
    private final FormLoginConfiguration formLoginConfiguration;
    private boolean authenticationMethodDefaulted = false;

    /**
     * Converts the value from the deployment descriptor into a "common" value.
     * <p>
     * This really means that 'CLIENT-CERT' is converted to 'CLIENT_CERT'
     * as the AUTH_TYPE for the javax.servlet.http.HttpServletRequest expects
     * an underscore, not a dash. Once this value is interpreted, as long as
     * the common constant is used, there is no issue.
     *
     * @param authMethod
     * @return
     */
    private String convertAuthMethod(String authMethod) {
        if (CLIENT_CERT_AUTH_METHOD.equalsIgnoreCase(authMethod)) {
            return CLIENT_CERT;
        } else {
            return authMethod;
        }
    }

    public LoginConfigurationImpl(String authenticationMethod, String realmName, FormLoginConfiguration formLoginConfiguration) {
        this.authenticationMethod = convertAuthMethod(authenticationMethod);
        this.realmName = realmName;
        this.formLoginConfiguration = formLoginConfiguration;
    }

    /** {@inheritDoc} */
    @Override
    public String getAuthenticationMethod() {
        return authenticationMethod;
    }

    /** {@inheritDoc} */
    @Override
    public String getRealmName() {
        return realmName;
    }

    /** {@inheritDoc} */
    @Override
    public FormLoginConfiguration getFormLoginConfiguration() {
        return formLoginConfiguration;
    }

    /**
     *
     */
    public void setAuthenticationMethodDefaulted() {
        authenticationMethodDefaulted = true;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.webcontainer.security.metadata.LoginConfiguration#isAuthenticationMethodDefaulted()
     */
    @Override
    public boolean isAuthenticationMethodDefaulted() {
        return authenticationMethodDefaulted;
    }

}
