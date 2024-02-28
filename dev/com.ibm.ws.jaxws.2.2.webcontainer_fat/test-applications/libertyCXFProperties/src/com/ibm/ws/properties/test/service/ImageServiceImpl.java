/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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
package com.ibm.ws.properties.test.service;

import java.io.FileOutputStream;
import java.io.IOException;

import javax.activation.DataHandler;
import javax.jws.WebService;
import javax.xml.ws.soap.Addressing;

import com.ibm.ws.properties.test.client.stub.ImageService;

@WebService(targetNamespace = "http://jaxws.service/", endpointInterface = "com.ibm.ws.properties.test.service.ImageService", portName = "ImageServiceImplPort",
            serviceName = "ImageServiceImplService") // 27755 WSDL location is removed that might be causing NPE on z/OS
@Addressing
public class ImageServiceImpl implements ImageService {

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
