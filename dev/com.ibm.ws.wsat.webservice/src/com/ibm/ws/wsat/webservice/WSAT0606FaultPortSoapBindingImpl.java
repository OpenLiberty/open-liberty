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
package com.ibm.ws.wsat.webservice;

import javax.xml.namespace.QName;

import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.utils.WSATRequestHandler;
import com.ibm.ws.wsat.webservice.client.soap.Detail;

@javax.jws.WebService(endpointInterface = "com.ibm.ws.wsat.webservice.WSAT0606FaultPort", targetNamespace = "http://docs.oasis-open.org/ws-tx/wsat/2006/06",
                      serviceName = "WSAT0606FaultService", portName = "WSAT0606FaultPort")
public class WSAT0606FaultPortSoapBindingImpl {

    public void fault(QName faultcode, String faultstring, String faultactor, Detail detail) throws WSATException {
        WSATRequestHandler.getInstance().handleFaultRequest(faultcode, faultstring, faultactor, detail);
    }

}
