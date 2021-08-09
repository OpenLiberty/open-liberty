/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.ejbcdi.client.normal;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "NormalService", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface NormalService {


    /**
     * 
     * @param arg1
     * @param arg0
     * @return
     *     returns java.lang.String
     * @throws SQLException_Exception
     * @throws NamingException_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "normalSayHelloToOther", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.normal.NormalSayHelloToOther")
    @ResponseWrapper(localName = "normalSayHelloToOtherResponse", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.normal.NormalSayHelloToOtherResponse")
    public String normalSayHelloToOther(
        @WebParam(name = "arg0", targetNamespace = "")
        String arg0,
        @WebParam(name = "arg1", targetNamespace = "")
        String arg1)
        throws NamingException_Exception, SQLException_Exception
    ;

}
