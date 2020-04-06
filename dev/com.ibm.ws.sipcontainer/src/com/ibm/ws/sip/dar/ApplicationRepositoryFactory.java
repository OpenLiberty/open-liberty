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
package com.ibm.ws.sip.dar;

import java.io.File;
import java.io.IOException;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.dar.repository.ApplicationRepository;
import com.ibm.ws.sip.dar.repository.impl.PropertyApplicationRepository;
import com.ibm.ws.sip.dar.repository.impl.StartOrderApplicationRepository;
import com.ibm.ws.sip.properties.CoreProperties;

/**
 * Application repository factory creates repository 
 * for define application selection strategy
 * 
 * @author Roman Mandeleil
 */
public class ApplicationRepositoryFactory {
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(ApplicationRepositoryFactory.class);

    
	/**
	 * The factory should choose betwean two strategies for 
	 * repository management.
	 * If the system property javax.servlet.sip.dar is exist 
	 * the repository will be managed by property file, otherwise 
	 * by application start order. 
	 * @return
	 */
	public static ApplicationRepository createApplicationRepository(){
		
		ApplicationRepository applicationRepository = null;
		
		String darConfigLocation = 	PropertiesStore.getInstance().
			getProperties().getString(CoreProperties.DAR_CONFIG_LOCATION);
		
		if (!darConfigLocation.equals("")){
			
			darConfigLocation = darConfigLocation.replace('\\', '/');
			darConfigLocation = darConfigLocation.replace(" ", "%20");
			
			Object[] params = {darConfigLocation};
			try {
				
				File propFile = new File(darConfigLocation);
				applicationRepository = 
					new PropertyApplicationRepository(propFile);
				
                if( c_logger.isInfoEnabled()){
                	c_logger.info("info.dar.init.4", 
                			Situation.SITUATION_START, 
                			params);
                }
				
			} catch (IOException e) {
				if (c_logger.isErrorEnabled()){
	    			c_logger.error("error.dar.no.config",null, params);
	    		}
			}
		if (c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug("Loading default application router, property file " +  
						darConfigLocation);
    		}
		} 
		
		// Define start order strategy, the repository
		// will be defined as WAS listener  
		if (applicationRepository == null){
			
         
			 if( c_logger.isInfoEnabled()){
				 c_logger.info("info.dar.init.5", 
            			Situation.SITUATION_START);
            }

            try {
				
				applicationRepository = 
					new StartOrderApplicationRepository();
            	
	            
            	if( c_logger.isInfoEnabled()){
            				 c_logger.info("info.dar.start.order",null);
	    		}
	            
			} catch (Exception e) {
				
				c_logger.error("error.dar.repository.create.1", 
						Situation.SITUATION_CONFIGURE, e.getMessage());
			}
		}
		return applicationRepository;
	}

}
