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

package com.ibm.was.wssample.sei.echo;

@javax.jws.WebService(endpointInterface = "com.ibm.was.wssample.sei.echo.EchoServicePortType", targetNamespace = "http://com/ibm/was/wssample/sei/echo/",
                      serviceName = "EchoService", portName = "EchoServicePort")
public class EchoSOAPImpl {

    public EchoStringResponse echoOperation(EchoStringInput parameter) {
        String inputString = "Failed";
        if (parameter != null) {
            try {
                inputString = parameter.getEchoInput();
                System.out.println(">> SERVICE: SOAP11 Echo Input String '" + inputString + "'");
            } catch (Exception e) {
                System.out.println(">> SERVICE: SOAP11 Echo ERROR Exception " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println(">> SERVICE: ERROR - SOAP11 Echo Missing Input String");
        }
        EchoStringResponse response = new ObjectFactory().createEchoStringResponse();
        response.setEchoResponse("SOAP11==>>" + inputString);
        return response;
    }
}