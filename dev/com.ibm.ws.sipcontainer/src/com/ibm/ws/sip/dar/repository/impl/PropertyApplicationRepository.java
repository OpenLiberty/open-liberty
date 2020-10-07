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
package com.ibm.ws.sip.dar.repository.impl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.servlet.sip.ar.SipApplicationRouterInfo;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.dar.parser.PropertyFileParser;
import com.ibm.ws.sip.dar.repository.ApplicationRepository;
import com.ibm.ws.sip.dar.selector.ApplicationSelector;
import com.ibm.ws.sip.dar.selector.impl.PropertyApplicationSelector;

/**
 * The class holds configuration of application composition
 * as defined in JSR 289 Appendix C 
 * @author Roman Mandeleil 
 */
public class PropertyApplicationRepository implements ApplicationRepository{
	
	private static final LogMgr c_logger = Log.get(PropertyApplicationRepository.class);

	private PropertyFileParser propertyFileParser = null;
	private HashMap<String, List<SipApplicationRouterInfo>> methodForApplicationMap;
	private PropertyApplicationSelector applicationSelector = null;
	
	/**
	 * Repository for application composition defined in 
	 * property file 
	 * @param propFile 
	 * @throws IOException
	 */
	public PropertyApplicationRepository(File propFile) throws IOException{
		
		this.methodForApplicationMap = 
			new HashMap<String, List<SipApplicationRouterInfo>>();
		
		this.propertyFileParser = new PropertyFileParser(propFile, 
				this.methodForApplicationMap);

		
		this.applicationSelector = 
			new PropertyApplicationSelector(methodForApplicationMap);

	}

	/**
	 * when the application is deployed, 
	 * property file should be reloaded.
	 * the list of application is useless in that case because
	 * application names are loaded from the property file 
	 */
	public void applicationDeployed(List<String> newlyDeployedApplicationNames) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug("CWSCT0415I: Default application router, property strategy, property file has been reloaded.");
		}
		this.propertyFileParser.reload();
	}



	/**
	 * applicationUndeployed - when the application is deployed, 
	 * property file should be reloaded.
	 * the list of application is useless in that case because
	 * application names are loaded from the property file 
	 */
	public void applicationUndeployed(List<String> undeployedApplicationNames) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug("CWSCT0415I: Default application router, property strategy, property file has been reloaded.");
		}
		this.propertyFileParser.reload();
	}

	/**
	 * return strategy to select applications in that repository.
	 */
	public ApplicationSelector getApplicationSelector(){
		return this.applicationSelector;
	}

}
