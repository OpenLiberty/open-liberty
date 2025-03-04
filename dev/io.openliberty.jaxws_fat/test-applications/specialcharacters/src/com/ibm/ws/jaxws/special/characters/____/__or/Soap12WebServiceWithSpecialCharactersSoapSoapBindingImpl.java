/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jaxws.special.characters.____.__or;

import com.ibm.ws.jaxws.special.characters.____$__or.ObjectFactory;


@javax.jws.WebService (endpointInterface="com.ibm.ws.jaxws.special.characters.____.__or.WebServiceWithSpecialCharactersPortType", targetNamespace="http://characters.special.jaxws.ws.ibm.com/:.!?$()or.=*#,", serviceName="WebServiceWithSpecialCharacters", portName="soap12WebServiceWithSpecialCharactersSoap", wsdlLocation="WEB-INF/wsdl/WebServiceWithSpecialCharacters.wsdl")
@javax.xml.ws.BindingType (value=javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
public class Soap12WebServiceWithSpecialCharactersSoapSoapBindingImpl{

    public String sc(String input1) {
        // TODO Auto-generated method stub
        return "Hello " + input1;
    }

}