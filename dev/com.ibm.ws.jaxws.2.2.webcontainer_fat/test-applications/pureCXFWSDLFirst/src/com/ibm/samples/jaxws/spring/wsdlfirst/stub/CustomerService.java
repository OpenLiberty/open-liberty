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
package com.ibm.samples.jaxws.spring.wsdlfirst.stub;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(targetNamespace = "http://customerservice.example.com/", name = "CustomerService")
@XmlSeeAlso({ ObjectFactory.class })
public interface CustomerService {

    @Oneway
    @RequestWrapper(localName = "updateCustomer", targetNamespace = "http://customerservice.example.com/", className = "com.example.customerservice.UpdateCustomer")
    @WebMethod
    public void updateCustomer(
                               @WebParam(name = "customer", targetNamespace = "") com.ibm.samples.jaxws.spring.wsdlfirst.stub.Customer customer) throws Exception;

    @WebResult(name = "return", targetNamespace = "")
    @RequestWrapper(localName = "getCustomersByName", targetNamespace = "http://customerservice.example.com/", className = "com.example.customerservice.GetCustomersByName")
    @WebMethod
    @ResponseWrapper(localName = "getCustomersByNameResponse", targetNamespace = "http://customerservice.example.com/",
                     className = "com.example.customerservice.GetCustomersByNameResponse")
    public java.util.List<com.ibm.samples.jaxws.spring.wsdlfirst.stub.Customer> getCustomersByName(
                                                                                                   @WebParam(name = "name",
                                                                                                             targetNamespace = "") java.lang.String name) throws NoSuchCustomerException;
}
