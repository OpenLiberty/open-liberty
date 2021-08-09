/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package test.wssecfat;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

/**
 * This class was generated by Apache CXF 2.6.2
 * 2012-11-27T09:50:44.893-06:00
 * Generated source version: 2.6.2
 *
 */
@WebServiceClient(name = "NoPassService",
                  wsdlLocation = "nopassunt1.wsdl",
                  targetNamespace = "http://wssecfat.test")
public class NoPassService extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://wssecfat.test", "NoPassService");
    public final static QName UrnNoPassUNT = new QName("http://wssecfat.test", "UrnNoPassUNT");
    static {
        URL url = NoPassService.class.getResource("nopassunt1.wsdl");
        if (url == null) {
            java.util.logging.Logger.getLogger(NoPassService.class.getName()).log(java.util.logging.Level.INFO,
                                                                                  "Can not initialize the default wsdl from {0}", "nopassunt1.wsdl");
        }
        WSDL_LOCATION = url;
    }

    public NoPassService(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public NoPassService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public NoPassService() {
        super(WSDL_LOCATION, SERVICE);
    }

    /**
     *
     * @return
     *         returns NoPass
     */
    @WebEndpoint(name = "UrnNoPassUNT")
    public NoPass getUrnNoPassUNT() {
        return super.getPort(UrnNoPassUNT, NoPass.class);
    }

    /**
     *
     * @param features
     *                     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy. Supported features not in the <code>features</code> parameter will have their
     *                     default values.
     * @return
     *         returns NoPass
     */
    @WebEndpoint(name = "UrnNoPassUNT")
    public NoPass getUrnNoPassUNT(WebServiceFeature... features) {
        return super.getPort(UrnNoPassUNT, NoPass.class, features);
    }

}
