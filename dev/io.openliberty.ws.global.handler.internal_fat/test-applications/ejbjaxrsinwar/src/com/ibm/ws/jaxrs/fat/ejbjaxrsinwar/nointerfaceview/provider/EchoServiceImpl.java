/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.provider;

import javax.jws.WebService;

@WebService(endpointInterface = "com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.provider.EchoServiceInterface")
public class EchoServiceImpl implements EchoServiceInterface {

    @Override
    public EchoStringResponse invoke(EchoString parameters) {
        EchoStringResponse response = new EchoStringResponse();
        response.setEchoStringResult(parameters.getInarg());
        return response;
    }

}
