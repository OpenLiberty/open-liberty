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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.activation.DataHandler;
import javax.jws.WebService;
import javax.xml.ws.soap.Addressing;

import com.ibm.ws.properties.test.client.stub.ImageService;

@WebService(targetNamespace = "http://jaxws.service/", endpointInterface = "com.ibm.ws.properties.test.service.ImageService", portName = "ImageServiceImplPort",
            serviceName = "ImageServiceImplService", wsdlLocation = "WEB-INF/wsdl/service-image.wsdl")
@Addressing
public class ImageServiceImpl implements ImageService {

    @Override
    public void uploadImage(String id, DataHandler image) {
        try {

            Path path = Paths.get(id + ".jpg");

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            image.writeTo(output);

            Files.write(path, output.toByteArray());

        } catch (IOException e) {
            // Since these tests are concerned only with whether properties are configured, if there's a problem
            // writing to the file, we can just ignore the failure and continue on with the test.
            System.out.println("Caught an exception trying to write to the file, continuing test.");
        }

    }

}
