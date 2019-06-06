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
package mtomservice.dd;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "MTOMInter", targetNamespace = "http://MTOMService/")
@XmlSeeAlso({
              ObjectFactory.class
})
public interface MTOMInter {

    /**
     *
     * @param arg0
     * @return
     *         returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "sendAttachment", targetNamespace = "http://MTOMService/", className = "mtomservice.dd.SendAttachment")
    @ResponseWrapper(localName = "sendAttachmentResponse", targetNamespace = "http://MTOMService/", className = "mtomservice.dd.SendAttachmentResponse")
    public String sendAttachment(
                                 @WebParam(name = "arg0", targetNamespace = "") byte[] arg0);

    /**
     *
     * @return
     *         returns byte[]
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "getAttachment", targetNamespace = "http://MTOMService/", className = "mtomservice.dd.GetAttachment")
    @ResponseWrapper(localName = "getAttachmentResponse", targetNamespace = "http://MTOMService/", className = "mtomservice.dd.GetAttachmentResponse")
    public byte[] getAttachment();

}
