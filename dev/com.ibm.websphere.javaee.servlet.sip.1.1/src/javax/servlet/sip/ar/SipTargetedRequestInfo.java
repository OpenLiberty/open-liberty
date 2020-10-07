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

/**
 * 
 * The SipTargetedRequestInfo class encapsulates the information that the container 
 * provides to the application router when the container calls the 
 * SipApplicationRouter.getNextApplication() method and the initial request is a targeted one. 
 *
 */
public class SipTargetedRequestInfo {
	
	/**
	 * Creates a SipTargetedRequestInfo object containing information necessary 
	 * to help the application router make its application selection decision when 
	 * the request is targeted. This information includes the type of targeted request 
	 * and the name of the targeted application.
	 *  
	 * @param targetedRequestType - Targeted request type {ENCODED_URI, JOIN, REPLACES}
	 * @param applicationName - The name of the application targeted by the request.
	 */
    public SipTargetedRequestInfo(SipTargetedRequestType targetedRequestType, String applicationName) {
        this.targetedRequestType = targetedRequestType;
        this.applicationName = applicationName;
    }

    /**
     * @return name of next application selected. If the route is external 
     * 		   then this returns null.
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * @return The type of the targeted request, one of: 
     * 		   {JOIN, REPLACES, ENCODED_URI}
     */
    public SipTargetedRequestType getType() {
        return targetedRequestType;
    }

    private final SipTargetedRequestType targetedRequestType;
    private final String applicationName;
}
