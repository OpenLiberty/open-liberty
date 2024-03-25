/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package io.openliberty.checkpoint.testapp.jaxws.props.client.stub;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "ImageServiceTwo", targetNamespace = "http://jaxws.service/")
@XmlSeeAlso({
              ObjectFactory.class
})
public interface ImageServiceTwo {

    @RequestWrapper(localName = "uploadImage", targetNamespace = "http://jaxws.service/", className = "io.openliberty.checkpoint.testapp.jaxws.props.client.stub.UploadImage")
    @WebMethod(action = "urn:UploadImage")
    @ResponseWrapper(localName = "uploadImageResponse", targetNamespace = "http://jaxws.service/", className = "io.openliberty.checkpoint.testapp.jaxws.props.client.stub.UploadImageResponse")
    public void uploadImage(
                            @WebParam(name = "arg0", targetNamespace = "") java.lang.String arg0,
                            @WebParam(name = "arg1", targetNamespace = "") javax.activation.DataHandler arg1);

}
