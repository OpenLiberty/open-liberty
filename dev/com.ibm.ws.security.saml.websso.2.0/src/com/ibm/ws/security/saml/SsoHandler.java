/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.security.saml.error.SamlException;

public interface SsoHandler {

    /*
     * 
     * @return the SAML Version the instance id handling
     */
    public Constants.SamlSsoVersion getSamlVersion();

    static String SAML_CONFIG = SsoConfig.class.getName();

    /*
     * All the parameters are stored in the parameters
     */
    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response,
                              SsoRequest samlRequest,
                              Map<String, Object> parameters) throws SamlException;
}
