/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.cxf.samltoken2.common;
//orig from CL:
//package com.ibm.ws.wssecurity.fat.cxf.samltoken.common;

import java.io.File;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestServer;
import com.ibm.ws.wssecurity.fat.utils.common.UpdateWSDLPortNum;

public class CXFSAMLCommonUtils {

    private static final Class<?> thisClass = CXFSAMLCommonUtils.class;

    public String processClientWsdl(String clientWsdlName, String servicePort) throws Exception {

        Log.info(thisClass, "processClientWsdl", "clientWsdlName: " + clientWsdlName);
        Log.info(thisClass, "processClientWsdl", "servicePort: " + servicePort);
        String WSDL_PORT_NUM = "8010"; // hard coded port number in client WSDL
        UpdateWSDLPortNum newWsdl = null;
        String clientWsdl = System.getProperty("user.dir") + File.separator + "cxfclient-policies" + File.separator + clientWsdlName;
        // Check if port number in WSDL is same as server's http port number
        if (servicePort.equals(WSDL_PORT_NUM)) {
            return clientWsdl;

        } else { // port number needs to be updated
            String newClientWsdl = System.getProperty("user.dir") + File.separator + "cxfclient-policies" + File.separator + "new_" + clientWsdlName;
            Log.info(thisClass, "processClientWsdl", "newClientWsdl: " + newClientWsdl);
            newWsdl = new UpdateWSDLPortNum(clientWsdl, newClientWsdl);
            newWsdl.updatePortNum(WSDL_PORT_NUM, servicePort);
            return newClientWsdl;
        }

    }

    public void fixServer2Ports(SAMLTestServer testSAMLServer2) throws Exception {

        // we need to override the ports that are stored in the secondary server (first servers ports are set by default)
        testSAMLServer2.setServerHttpPort(testSAMLServer2.getServer().getHttpSecondaryPort());
        testSAMLServer2.setServerHttpsPort(testSAMLServer2.getServer().getHttpSecondarySecurePort());

    }

}