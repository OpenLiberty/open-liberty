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
 * Represents a form-login-config element in web.xml.
 * <p/>
 * <pre>
 * &lt;form-login-config&gt;
 * &lt;form-login-page&gt;form-login-page&lt;/form-login-page&gt;
 * &lt;form-error-page>form-error-page&lt;/form-error-page&gt;
 * &lt;/form-login-config&gt;
 * </pre>
 */
public interface FormLoginConfiguration {

    /**
     * Gets the form login page.
     */
    public String getLoginPage();

    /**
     * Gets the form error page.
     */
    public String getErrorPage();
}
