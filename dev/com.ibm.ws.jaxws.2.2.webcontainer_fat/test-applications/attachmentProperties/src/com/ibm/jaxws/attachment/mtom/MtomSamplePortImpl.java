/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.jaxws.attachment.mtom;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.activation.DataHandler;
import javax.annotation.Resource;
import javax.jws.HandlerChain;
import javax.xml.ws.BindingType;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.soap.SOAPBinding;

@javax.jws.WebService(endpointInterface = "com.ibm.jaxws.attachment.mtom.MtomSample",
                      targetNamespace = "http://com/ibm/jaxws/attachment/mtom/",
                      serviceName = "MtomSampleService",
                      portName = "MtomSamplePort",
                      wsdlLocation = "wsdl/ImageDepot.wsdl")
@BindingType(value = SOAPBinding.SOAP11HTTP_MTOM_BINDING)
@HandlerChain(file = "soaphandlers.xml")
public class MtomSamplePortImpl {

    @Resource
    WebServiceContext wsc;

    public ImageDepot sendImage(ImageDepot data) {
        try {
            // read all data
            List<DataHandler> dhList = data.getImageData();
            for (DataHandler dh : dhList) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                dh.writeTo(bos);
                bos = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }
}
