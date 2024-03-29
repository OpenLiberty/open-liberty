/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

package test.wssecfvt.endsuptokens;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

/**
 * This class was generated by Apache CXF 2.6.2
 * 2013-02-15T08:47:39.543-06:00
 * Generated source version: 2.6.2
 *
 */
@WebServiceClient(name = "EndSupTokensService4",
                  wsdlLocation = "EndSupTokens.wsdl",
                  targetNamespace = "http://endsuptokens.wssecfvt.test")
public class EndSupTokensService4 extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://endsuptokens.wssecfvt.test", "EndSupTokensService4");
    public final static QName EndSupTokensUNTEndorsingPort = new QName("http://endsuptokens.wssecfvt.test", "EndSupTokensUNTEndorsingPort");
    static {
        URL url = EndSupTokensService4.class.getResource("EndSupTokens.wsdl");
        if (url == null) {
            java.util.logging.Logger.getLogger(EndSupTokensService4.class.getName()).log(java.util.logging.Level.INFO,
                                                                                         "Can not initialize the default wsdl from {0}", "EndSupTokens.wsdl");
        }
        WSDL_LOCATION = url;
    }

    public EndSupTokensService4(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public EndSupTokensService4(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public EndSupTokensService4() {
        super(WSDL_LOCATION, SERVICE);
    }

    /**
     *
     * @return
     *         returns EndSupTokensPortType
     */
    @WebEndpoint(name = "EndSupTokensUNTEndorsingPort")
    public EndSupTokensPortType getEndSupTokensUNTEndorsingPort() {
        return super.getPort(EndSupTokensUNTEndorsingPort, EndSupTokensPortType.class);
    }

    /**
     *
     * @param features
     *                     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy. Supported features not in the <code>features</code> parameter will have their
     *                     default values.
     * @return
     *         returns EndSupTokensPortType
     */
    @WebEndpoint(name = "EndSupTokensUNTEndorsingPort")
    public EndSupTokensPortType getEndSupTokensUNTEndorsingPort(WebServiceFeature... features) {
        return super.getPort(EndSupTokensUNTEndorsingPort, EndSupTokensPortType.class, features);
    }

}
