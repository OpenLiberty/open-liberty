/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package fats.cxf.jaxws22.mtom.server;

import javax.jws.WebService;
import javax.xml.ws.BindingType;
import javax.xml.ws.soap.MTOM;

/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.10
 * Generated source version: 2.2
 *
 */
@MTOM(enabled = true, threshold = 0)
@WebService(portName = "MTOMAnnotationNoMTOMPort", serviceName = "MTOMAnnotationNoMTOMService", targetNamespace = "http://server.mtom.jaxws22.cxf.fats/",
            wsdlLocation = "WEB-INF/wsdl/MTOMAnnotationNoMTOMService.wsdl", endpointInterface = "fats.cxf.jaxws22.mtom.server.MTOMAnnotationNoMTOM")
@BindingType("http://schemas.xmlsoap.org/wsdl/soap/http")
public class MTOMAnnotationNoMTOMService_MTOMAnnotationNoMTOMPortImpl implements MTOMAnnotationNoMTOM {

    public MTOMAnnotationNoMTOMService_MTOMAnnotationNoMTOMPortImpl() {}

    /**
     *
     * @param arg0
     * @return
     *         returns byte[]
     */
    @Override
    public byte[] echobyte(byte[] b) {
        System.out.println("MTOMAnnotationNoMTOM echobyte called");
        return b;
    }

}
