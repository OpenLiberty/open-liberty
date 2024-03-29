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

package test.libertyfat.caller.contract;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

/**
 * This class was generated by Apache CXF 2.6.2
 * 2013-01-31T16:42:42.994-06:00
 * Generated source version: 2.6.2
 *
 */
@WebServiceClient(name = "FatBAC06Service",
                  wsdlLocation = "calltoken.wsdl",
                  targetNamespace = "http://caller.libertyfat.test/contract")
public class FatBAC06Service extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://caller.libertyfat.test/contract", "FatBAC06Service");
    public final static QName UrnCallerToken06 = new QName("http://caller.libertyfat.test/contract", "UrnCallerToken06");
    static {
        URL url = FatBAC06Service.class.getResource("calltoken.wsdl");
        if (url == null) {
            java.util.logging.Logger.getLogger(FatBAC06Service.class.getName()).log(java.util.logging.Level.INFO,
                                                                                    "Can not initialize the default wsdl from {0}", "calltoken.wsdl");
        }
        WSDL_LOCATION = url;
    }

    public FatBAC06Service(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public FatBAC06Service(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public FatBAC06Service() {
        super(WSDL_LOCATION, SERVICE);
    }

    /**
     *
     * @return
     *         returns FVTVersionBAC
     */
    @WebEndpoint(name = "UrnCallerToken06")
    public FVTVersionBAC getUrnCallerToken06() {
        return super.getPort(UrnCallerToken06, FVTVersionBAC.class);
    }

    /**
     *
     * @param features
     *                     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy. Supported features not in the <code>features</code> parameter will have their
     *                     default values.
     * @return
     *         returns FVTVersionBAC
     */
    @WebEndpoint(name = "UrnCallerToken06")
    public FVTVersionBAC getUrnCallerToken06(WebServiceFeature... features) {
        return super.getPort(UrnCallerToken06, FVTVersionBAC.class, features);
    }

}
