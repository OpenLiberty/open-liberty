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

package com.ibm.was.wssample.sei.ping;

@javax.jws.WebService(endpointInterface = "com.ibm.was.wssample.sei.ping.PingServicePortType", targetNamespace = "http://com/ibm/was/wssample/sei/ping/",
                      serviceName = "PingService", portName = "PingServicePort")
public class PingSOAPImpl {

    public void pingOperation(PingStringInput parameter) {
        if (parameter != null) {
            try {
                System.out.println(">> SERVICE: SOAP11 Ping Input String '" + parameter.getPingInput() + "'");
            } catch (Exception e) {
                System.out.println(">> SERVICE: SOAP11 Ping ERROR Exception " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println(">> SERVICE: ERROR - SOAP12 Ping Missing Input String");
        }
    }

}