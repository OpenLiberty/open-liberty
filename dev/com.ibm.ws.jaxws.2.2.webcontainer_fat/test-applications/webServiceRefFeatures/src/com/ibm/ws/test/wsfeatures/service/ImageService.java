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

import javax.activation.DataHandler;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

@WebService(name = "ImageService", targetNamespace = "http://jaxws.service/")
public interface ImageService {

    @WebMethod(operationName = "uploadImage", action = "urn:UploadImage")
    void uploadImage(@WebParam(name = "arg0") String id, @WebParam(name = "arg1") DataHandler image);
}
