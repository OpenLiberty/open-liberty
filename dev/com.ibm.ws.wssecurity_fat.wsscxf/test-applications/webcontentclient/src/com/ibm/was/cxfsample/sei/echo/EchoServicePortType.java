/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.was.cxfsample.sei.echo;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * This class was generated by Apache CXF 2.6.2
 * 2013-02-27T11:59:38.691-06:00
 * Generated source version: 2.6.2
 * 
 */
@WebService(targetNamespace = "http://com/ibm/was/cxfsample/sei/echo/", name = "EchoServicePortType")
@XmlSeeAlso({ObjectFactory.class})
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface EchoServicePortType {

    @WebResult(name = "echoStringResponse", targetNamespace = "http://com/ibm/was/cxfsample/sei/echo/", partName = "parameter")
    @WebMethod(action = "echoOperation")
    public EchoStringResponse echoOperation(
        @WebParam(partName = "parameter", name = "echoStringInput", targetNamespace = "http://com/ibm/was/cxfsample/sei/echo/")
        EchoStringInput parameter
    );
}
