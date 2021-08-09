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
package com.ibm.websphere.sip;

import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

///** TODO Liberty: use this Java Doc when Story 159478 is delivered - and this API is going to be exported.
// * This interface adds SIP-Web Services functionality API for converged applications.
// * 
// * @author Nitzan Nissim
// *@ibm-api
// */
/**
 * This interface adds SIP-Web Services functionality API for converged applications.
 * 
 * @author Nitzan Nissim
 */
public interface WSApplicationSession {
	
	
	/**
	  * Create an EndpointReference (EPR) and update it with the application session ID  
	  * which can be used by converged applications using web services.  The resulting 
	  * EPR can be passed to a web services client and used to make future requests with 
	  * affinity.  That will ensure the request comes back to the correct session.
	  * 
	  * This method is different from the other one with a similar name.  It is more tied
	  * specifically to the CEA web service.
	  * 
	  * @param appSessionId - the SIP application session ID to be placed int the EPR
	  *                                                   which will be used for affinity.
	  * @return - an EndPointReference with affinity based on the appSessionId
	  * @throws Exception - if an unexpected error occurs
	  */
	public W3CEndpointReference createEPR(String appSessionId) throws Exception;
	
	/**
	  * This is a more generic method to create an EPR for any converged web service.
	  *  
	  * Create an EndpointReference (EPR) and update it with the application session ID  
	  * which can be used by converged applications using web services.  The resulting 
	  * EPR can be passed to a web services client and used to make future requests with 
	  * affinity.  That will ensure the request comes back to the correct session.
	  * @param appSessionId - the SIP application session ID to be placed int the EPR
	  *                                                   which will be used for affinity.
	  * @param portName - The QName which will identify the reference parameter used for affinity
	  * @param serviceName - The QName representing the service that the newly created EPR will represent
	  * @param context - The web service context associated with the calling code
	  * @return - an EndPointReference with affinity based on the appSessionId
	  * @throws Exception - if an unexpected error occurs
	  */
	public W3CEndpointReference createEPR(String appSessionId, 
			                              QName portName, 
			                              QName serviceName,
			                              WebServiceContext context) throws Exception;

}
