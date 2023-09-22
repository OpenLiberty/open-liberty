/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.checkpoint.testapp.jaxws.props.service;

import java.io.FileOutputStream;
import java.io.IOException;

import javax.activation.DataHandler;
import javax.jws.WebService;

import io.openliberty.checkpoint.testapp.jaxws.props.client.stub.ImageServiceTwo;

@WebService(targetNamespace = "http://jaxws.service/",
            endpointInterface = "io.openliberty.checkpoint.testapp.jaxws.props.service.ImageServiceTwo",
            portName = "ImageServiceImplPortTwo",
            serviceName = "ImageServiceImplServiceTwo",
            wsdlLocation = "WEB-INF/wsdl/service-image.wsdl")
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
