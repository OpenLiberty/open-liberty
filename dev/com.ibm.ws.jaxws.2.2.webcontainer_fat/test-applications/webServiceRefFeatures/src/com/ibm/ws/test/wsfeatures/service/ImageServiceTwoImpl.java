/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.test.wsfeatures.service;

import java.io.FileOutputStream;
import java.io.IOException;

import javax.activation.DataHandler;
import javax.jws.WebService;

@WebService(targetNamespace = "http://jaxws.service/",
            endpointInterface = "com.ibm.ws.test.wsfeatures.service.ImageServiceTwo",
            portName = "ImageServiceImplPortTwo",
            serviceName = "ImageServiceImplServiceTwo",
            wsdlLocation = "wsdl/image.wsdl")
public class ImageServiceTwoImpl implements ImageServiceTwo {

    @Override
    public void uploadImage(String id, DataHandler image) {
        try {
            FileOutputStream out = new FileOutputStream(id + ".jpg");
            image.writeTo(out);
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
