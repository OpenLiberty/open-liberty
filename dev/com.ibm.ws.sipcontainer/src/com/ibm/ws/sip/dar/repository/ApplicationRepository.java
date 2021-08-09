/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.dar.repository;

import java.util.List;

import com.ibm.ws.sip.dar.selector.ApplicationSelector;

/**
 * In all application composition selection strategies 
 * there is an application repository that should implement 
 * that interface, the application repoitory holds all applications
 * was started in the SIP container.
 * 
 * @author Roman Mandeleil
 */
public interface ApplicationRepository {
	
	/**
	 * Application started
	 * 
	 * @param newlyDeployedApplicationNames
	 */
    void 	applicationDeployed(List<String> newlyDeployedApplicationNames);
    
    /**
     * Application stopped
     * 
     * @param undeployedApplicationNames
     */
    void 	applicationUndeployed(List<String> undeployedApplicationNames);
    
    /**
     * @return Application selector for current repository
     */
    public  ApplicationSelector getApplicationSelector();
}
