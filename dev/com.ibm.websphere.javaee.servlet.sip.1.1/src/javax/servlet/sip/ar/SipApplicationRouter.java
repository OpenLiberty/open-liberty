/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javax.servlet.sip.ar;

import java.io.Serializable;
import java.util.List;

import javax.servlet.sip.SipServletRequest;



/**
 * The interface between sip container and external 
 * application composition router.  
 * 
 * @author Roman Mandeleil
 */
public interface SipApplicationRouter {
	
	 /**
	  * 
	  *  the method triggered by application container when new application
	  *  is deployed and started.
	  * 
	  * @param newlyDeployedApplicationNames
	  */
	 public void applicationDeployed(List<String> newlyDeployedApplicationNames);

	 /**
	  * 
	  *  the method triggered by application container when an application
	  *  is undeployed or stoped .
	  * 
	  * @param undeployedApplicationNames
	  */
	 public void applicationUndeployed(List<String> undeployedApplicationNames);
	 
	 /**
	  * 
	  * Callback when composition router is unloaded
	  */
	 public void 	destroy();
	 
	 /**
	  * This method is called by the container when a servlet sends or proxies an 
	  * initial SipServletRequest. The application router returns a set of information. 
	  * See SipApplicationRouterInfo  for details.  
	  * @param initialRequest - request which asks for next application. 
	  * @param region         - region which will run the next application.
	  * @param directive      - directive which clarifies the. 
	  * @param targetedRequestInfo - If initialRequest is a targeted request, this object 
	  * 		gives the type of targeted request (ENCODED_URI, JOIN, REPLACES) and the 
	  * 		targeted application name. If the initialRequest is not targeted, this parameter 
	  * 		is null.	   
	  * @param stateInfo      - the state of the application chain.
	  * @return application selection result. If no applications are deployed this 
	  * 		method returns null.
	  *
	  *  @throws java.lang.NullPointerException - if the initialRequest is null 
	  *  @throws java.lang.IllegalStateException - if the application router has not been initialized yet 
	  */
	 public SipApplicationRouterInfo 	getNextApplication(SipServletRequest initialRequest, 
			 SipApplicationRoutingRegion region, 
			 SipApplicationRoutingDirective directive, 
			 SipTargetedRequestInfo targetedRequestInfo,
			 Serializable stateInfo) throws NullPointerException, IllegalStateException ;
	 
	 /**
	  * initialization of application
	  * composition router.
	  */
     public void 	init();
     
     /**
      * Initializes the SipApplicationRouter and passes in initialization properties. 
      * This method is called by the SIP container. The way in which the container obtains 
      * the properties is implementation-dependent.
      * 
      * This method can only be invoked once.
      *  
      * @param properties  - AR initialization properties
      * 
      * @throws IllegalStateException - if invoked more than once
      */
     void init(java.util.Properties properties) throws IllegalStateException;     
	
}
