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


package test.libertyfat.caller;

import javax.xml.ws.WebServiceProvider;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.Service;

import test.libertyfat.caller.SAMLCallerUtil;

@WebServiceProvider(targetNamespace="http://caller.libertyfat.test/contract",
                    serviceName="FatSamlC01Service", portName="SamlCallerToken01",
                    wsdlLocation="WEB-INF/samlcallertoken.wsdl")

@ServiceMode(value = Service.Mode.MESSAGE)

/**
 */
public class SAMLCaller_bac01 implements javax.xml.ws.Provider<SOAPMessage> {

    /* (non-Javadoc)
     * @see javax.xml.ws.Provider#invoke(java.lang.Object)
     */
    @Override
    public SOAPMessage invoke(SOAPMessage request) {
        String PrincipalUserID = SAMLCallerUtil.getPrincipalUserID();
        String respMsg = new String(
                               "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                               "<SOAP-ENV:Body><provider><message>Liberty Fat SamlCaller bac01(" + PrincipalUserID +
                               ")</message></provider></SOAP-ENV:Body>" + "</SOAP-ENV:Envelope>"  );
        return SAMLCallerUtil.invoke( request, respMsg, getClass().getName() );
    }
}
