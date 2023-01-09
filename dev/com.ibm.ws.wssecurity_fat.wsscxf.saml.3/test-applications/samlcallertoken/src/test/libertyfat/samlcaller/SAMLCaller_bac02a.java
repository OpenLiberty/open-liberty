/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package test.libertyfat.samlcaller;
//2/01/2021 renamed the package; it's not matched to the actual folder name 'samlcaller'
//orig from CL:
//package test.libertyfat.caller;
//2/01/2021 renamed the package;

import javax.xml.ws.WebServiceProvider;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.Service;

//2/01/2021 comment out
//compilatilon: error: package edu.emory.mathcs.backport.java.util does not exist
//import edu.emory.mathcs.backport.java.util.Arrays;
import test.libertyfat.samlcaller.SAMLCallerUtil;
//2/01/2021 renamed the package; it's not matched to the actual folder name 'samlcaller'
//orig from CL:
//import test.libertyfat.caller.SAMLCallerUtil;

@WebServiceProvider(targetNamespace="http://caller.libertyfat.test/contract",
                    serviceName="FatSamlC02aService", portName="SamlCallerToken02a",
                    wsdlLocation="WEB-INF/samlcallertoken.wsdl")

@ServiceMode(value = Service.Mode.MESSAGE)

/**
 */
public class SAMLCaller_bac02a implements javax.xml.ws.Provider<SOAPMessage> {

    /* (non-Javadoc)
     * @see javax.xml.ws.Provider#invoke(java.lang.Object)
     */
    @Override
    public SOAPMessage invoke(SOAPMessage request) {
        String PrincipalUserID = SAMLCallerUtil.getPrincipalUserID();
        String RealmName = SAMLCallerUtil.getRealmName();
        String GroupNames = SAMLCallerUtil.getGroups();
        String respMsg = new String(
                               "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                               "<SOAP-ENV:Body><provider><message>Liberty Fat SAMLCaller bac02a(" + "realm name: " + RealmName + 
                               " PrincipalUserID: " + PrincipalUserID +
                               " Groups: " + GroupNames +
                               ")</message></provider></SOAP-ENV:Body>" + "</SOAP-ENV:Envelope>"  );
        return SAMLCallerUtil.invoke( request, respMsg, getClass().getName()  );
    }
}
