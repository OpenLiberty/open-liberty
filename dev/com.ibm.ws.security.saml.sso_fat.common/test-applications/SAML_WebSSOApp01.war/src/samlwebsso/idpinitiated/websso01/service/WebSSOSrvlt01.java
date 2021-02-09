/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package samlwebsso.idpinitiated.websso01.service;

import javax.xml.ws.WebServiceProvider;

@WebServiceProvider(targetNamespace = "http://samlwebsso.idpinitiated.samlwebsso.service",
        serviceName = "WebSSOSrvlt01Service", portName = "UrnWebSSOSrvlt01",
        wsdlLocation = "WEB-INF/WebSSOSrvlt01.wsdl")
/**
 * Server side implementation of Web Services Security tests.
 * Contains invoke method called by clients which returns
 * WssecfvtConst.TEST_STRING_OUT.
 * @author Syed Wadood
 */
public class WebSSOSrvlt01 implements javax.xml.ws.Provider<String> {
    /**
     * @param s
     *            Not used at this time but needed to implement the
     *            Provider interface.
     * @return Returns the string referenced by WssecfvtConst.TEST_STRING_OUT.
     */
    public String invoke(String s) {
        return "SAML Test App 1";
    }
}
