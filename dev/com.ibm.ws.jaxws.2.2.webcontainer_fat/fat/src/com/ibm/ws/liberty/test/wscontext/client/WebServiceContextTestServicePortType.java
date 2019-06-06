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
package com.ibm.ws.liberty.test.wscontext.client;

import javax.jws.WebMethod;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "WebServiceContextTestServicePortType", targetNamespace = "http://wscontext.test.liberty.ws.ibm.com")
@XmlSeeAlso({ ObjectFactory.class })
public interface WebServiceContextTestServicePortType {

    /**
     *
     * @return
     *         returns boolean
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "isMessageContextNull", targetNamespace = "http://wscontext.test.liberty.ws.ibm.com",
                    className = "com.ibm.ws.liberty.test.wscontext.client.IsMessageContextNull")
    @ResponseWrapper(localName = "isMessageContextNullResponse", targetNamespace = "http://wscontext.test.liberty.ws.ibm.com",
                     className = "com.ibm.ws.liberty.test.wscontext.client.IsMessageContextNullResponse")
    public boolean isMessageContextNull();

    /**
     *
     * @return
     *         returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "getServletContextParameter", targetNamespace = "http://wscontext.test.liberty.ws.ibm.com",
                    className = "com.ibm.ws.liberty.test.wscontext.client.GetServletContextParameter")
    @ResponseWrapper(localName = "getServletContextParameterResponse", targetNamespace = "http://wscontext.test.liberty.ws.ibm.com",
                     className = "com.ibm.ws.liberty.test.wscontext.client.GetServletContextParameterResponse")
    public String getServletContextParameter();

    /**
     *
     * @return
     *         returns boolean
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "isDefaultJndiLookupInstanceNull", targetNamespace = "http://wscontext.test.liberty.ws.ibm.com",
                    className = "com.ibm.ws.liberty.test.wscontext.client.IsDefaultJndiLookupInstanceNull")
    @ResponseWrapper(localName = "isDefaultJndiLookupInstanceNullResponse", targetNamespace = "http://wscontext.test.liberty.ws.ibm.com",
                     className = "com.ibm.ws.liberty.test.wscontext.client.IsDefaultJndiLookupInstanceNullResponse")
    public boolean isDefaultJndiLookupInstanceNull();

    /**
     *
     * @return
     *         returns boolean
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "isSelfDefinedJndiLookupInstanceNull", targetNamespace = "http://wscontext.test.liberty.ws.ibm.com",
                    className = "com.ibm.ws.liberty.test.wscontext.client.IsSelfDefinedJndiLookupInstanceNull")
    @ResponseWrapper(localName = "isSelfDefinedJndiLookupInstanceNullResponse", targetNamespace = "http://wscontext.test.liberty.ws.ibm.com",
                     className = "com.ibm.ws.liberty.test.wscontext.client.IsSelfDefinedJndiLookupInstanceNullResponse")
    public boolean isSelfDefinedJndiLookupInstanceNull();

    /**
     *
     * @return
     *         returns boolean
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "isInjectionInstanceNull", targetNamespace = "http://wscontext.test.liberty.ws.ibm.com",
                    className = "com.ibm.ws.liberty.test.wscontext.client.IsInjectionInstanceNull")
    @ResponseWrapper(localName = "isInjectionInstanceNullResponse", targetNamespace = "http://wscontext.test.liberty.ws.ibm.com",
                     className = "com.ibm.ws.liberty.test.wscontext.client.IsInjectionInstanceNullResponse")
    public boolean isInjectionInstanceNull();

    /**
     *
     * @return
     *         returns boolean
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "isServletContextNull", targetNamespace = "http://wscontext.test.liberty.ws.ibm.com",
                    className = "com.ibm.ws.liberty.test.wscontext.client.IsServletContextNull")
    @ResponseWrapper(localName = "isServletContextNullResponse", targetNamespace = "http://wscontext.test.liberty.ws.ibm.com",
                     className = "com.ibm.ws.liberty.test.wscontext.client.IsServletContextNullResponse")
    public boolean isServletContextNull();

}
