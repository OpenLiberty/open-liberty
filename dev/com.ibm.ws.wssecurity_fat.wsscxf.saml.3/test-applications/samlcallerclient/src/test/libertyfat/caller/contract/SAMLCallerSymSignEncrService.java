/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package test.libertyfat.caller.contract;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.Service;

/**
 * This class was generated by Apache CXF 2.6.2
 * 2015-09-28T08:30:59.109-05:00
 * Generated source version: 2.6.2
 * 
 */
@WebServiceClient(name = "SAMLCallerSymSignEncrService", 
                  wsdlLocation = "samlcallertoken.wsdl",
                  targetNamespace = "http://caller.libertyfat.test/contract") 
public class SAMLCallerSymSignEncrService extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://caller.libertyfat.test/contract", "SAMLCallerSymSignEncrService");
    public final static QName SAMLCallerSymSignEncrPort = new QName("http://caller.libertyfat.test/contract", "SAMLCallerSymSignEncrPort");
    static {
        URL url = SAMLCallerSymSignEncrService.class.getResource("samlcallertoken.wsdl");
        if (url == null) {
            java.util.logging.Logger.getLogger(SAMLCallerSymSignEncrService.class.getName())
                .log(java.util.logging.Level.INFO, 
                     "Can not initialize the default wsdl from {0}", "samlcallertoken.wsdl");
        }       
        WSDL_LOCATION = url;
    }

    public SAMLCallerSymSignEncrService(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public SAMLCallerSymSignEncrService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public SAMLCallerSymSignEncrService() {
        super(WSDL_LOCATION, SERVICE);
    }
    
    /**
     *
     * @return
     *     returns FVTVersionSamlC
     */
    @WebEndpoint(name = "SAMLCallerSymSignEncrPort")
    public FVTVersionSamlC getSAMLCallerSymSignEncrPort() {
        return super.getPort(SAMLCallerSymSignEncrPort, FVTVersionSamlC.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns FVTVersionSamlC
     */
    @WebEndpoint(name = "SAMLCallerSymSignEncrPort")
    public FVTVersionSamlC getSAMLCallerSymSignEncrPort(WebServiceFeature... features) {
        return super.getPort(SAMLCallerSymSignEncrPort, FVTVersionSamlC.class, features);
    }

}
