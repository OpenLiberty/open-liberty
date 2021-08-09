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
package com.ibm.ws.sip.security.auth;

import java.util.Properties;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.properties.PropertiesStore;
//TODO Liberty replace the current imports
//import com.ibm.ws.security.config.DynamicTAI;
//import com.ibm.ws.security.config.DynamicTAIConfig;
//import com.ibm.ws.security.config.SecurityObjectLocator;

/**
 * A 'security manager' class. For now, the sole purpose of this class is to
 * dynamically load the digest TAI code. The digest code is implemented as a 
 * TAI, but since there is now (WAS 7) a requirement to hide it from the TAI 
 * list, we load it dynamically at runtime. An additional benefit of this is 
 * that the TAI gets loaded only when it is first needed. 
 *  
 * @author dedi
 *
 */
public class SipSecurityManager {

    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipSecurityManager.class);

	/**
	 * The digest TAI classname. Does this mean we need to move it to 
	 * this project?
	 */
	private final static String DIGEST_TAI_NAME = 
		"com.ibm.ws.sip.security.digest.DigestTAI";
	
	/**
	 * Propeties to initialize the TAI with.
	 */
	private Properties m_taiProperties;
	
	/**
	 * Sip container started notification - dynamically load the TAI.
	 */
	public void onContainerStarted()
	{
        if (c_logger.isTraceEntryExitEnabled())
        {
            c_logger.traceEntry(this, "onContainerStarted");
        }
		m_taiProperties = getTAIProperties();

		// Could be we nNeed to run in a priviledged block when running with J2
		// security. Since the current version of WASX doesn't work with J2
		// security, I couldn't make sure. Anyway, here's the code, 
		// commented out:
//		AccessController.doPrivileged(
//				new PrivilegedAction()
//				{
//					public Object run() {
//						loadDigestTai();
//						return null;
//					}
//				});
		loadDigestTai();

		if (c_logger.isTraceEntryExitEnabled())
        {
            c_logger.traceExit(this, "onContainerStarted");
        }

	}

	/**
	 * Load the digest TAI code.
	 */
	private void loadDigestTai() {
        if (c_logger.isTraceEntryExitEnabled())
        {
            c_logger.traceEntry(this, "loadDigestTai");
        }

        /*TODO Liberty boolean firstPass = false;  
		DynamicTAI dynamicTAI = 
			new DynamicTAI(m_taiProperties, DIGEST_TAI_NAME, firstPass);
		DynamicTAIConfig taiConfig = SecurityObjectLocator.getDynamicTAIConfig(); 
		taiConfig.addInterceptor(dynamicTAI);*/

        if (c_logger.isTraceEntryExitEnabled())
        {
            c_logger.traceExit(this, "loadDigestTai");
        }
	}

	/**
	 * Get the TAI Properties from the configuration.
	 * @return a Properties object,with the SIP container properties.
	 */
	private Properties getTAIProperties() 
	{
        if (c_logger.isTraceEntryExitEnabled())
        {
            c_logger.traceEntry(this, "readTAIProperties");
        }
        
        Properties properties = PropertiesStore.getInstance().getProperties().copyProps();
        
        if (c_logger.isTraceEntryExitEnabled())
        {
            c_logger.traceExit(this, "readTAIProperties");
        }

		return properties;
	}
}
