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
package com.ibm.ws.sip.container.annotation;

import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.osgi.service.component.annotations.Component;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.servlets.SipServletsFactoryImpl;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.factory.ResourceInfo;
import com.ibm.wsspi.injectionengine.factory.ResourceInfoRefAddr;

@Component(service = { ObjectFactory.class, SipFactoryObjectFactory.class })
public class SipFactoryObjectFactory implements ObjectFactory  {
 	
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipFactoryObjectFactory.class);

    /**
     * Represents the sip prefix in the sip factory JNDI name
     * (sip/<appname>/SipFactory).
     */
    private static final String SIP_PREFIX = "sip";
    
    /**
	 * DS method to activate this component.
	 * 
	 * @param 	properties 	: Map containing service & config properties
	 *            populated/provided by config admin
	 */
	public void activate(Map<String, Object> properties) {
        if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("SipFactoryObjectFactory activated", properties);
    }
	
	  /**
     * Represents the SipFactory postfix in the sip factory JNDI name
     * (sip/<appname>/SipFactory).
     */
    private static final String SIP_FACTORY_POSTFIX = "SipFactory";
	
  	public Object getObjectInstance(Object obj, 
			Name name, 
			Context nameCtx,
			Hashtable<?, ?> environment) throws Exception {

	
		// Is obj a Reference?
		if ( !( obj instanceof Reference ) ) {
			throw new InjectionException("Binding object is not a Reference : " + obj );
		}

		Reference ref = (Reference) obj;

		// Is the right factory for this reference?
		if ( !ref.getFactoryClassName().equals( this.getClass().getName() ) ) {
			throw new InjectionException( "Incorrect factory for Reference : " + obj );
		}

		// Is address null?
		RefAddr addr = ref.get( ResourceInfoRefAddr.Addr_Type );
		if ( addr == null ) {
			throw new RuntimeException( "The address for this Reference is empty (null)" );
		}

		// Reference has the right factory and non empty address,
		// so it is OK to generate the object now
		ResourceInfo info = (ResourceInfo) addr.getContent();

		String appName = retrieveAppName(info);
		
		if (appName != null) {
			return SipServletsFactoryImpl.getInstance(appName);
		}
		
		 if (c_logger.isTraceDebugEnabled())
             c_logger.traceDebug("Could not locate application name, returning default factory");
			
		return SipServletsFactoryImpl.getInstance();		
	}
	
	/**
	 * Retrieves the application name from the resource info.
	 * First tries to parse the JNDI name (in the format of 
	 * sip/<appname>/SipFactory), and if that doesn't work -
	 * retrieves it from the component.
	 *  
	 * @param info the resource info
	 * 
	 * @return the application name
	 * 
	 * @see ResourceInfo
	 */
	private String retrieveAppName(ResourceInfo info) {
		String appName = "";
		String jndiName = info.getName();
		
		//First try to parse the JNDI name
		if (jndiName != null) {
			 if (c_logger.isTraceDebugEnabled()){
				 c_logger.traceDebug("retrieveAppName", "jndiName=" + jndiName);
			}
			//Make sure this is a valid JNDI name
			if ((jndiName.startsWith(SIP_PREFIX)) && (jndiName.endsWith(SIP_FACTORY_POSTFIX))) {
				String[] result = jndiName.split("/");
				if (result.length == 3) {
					appName = result[1];
				}
			}
		}
		//Didn't work with the JNDI name, try the component instead
		if (appName == null || appName.equals("")) {
			 if (c_logger.isTraceDebugEnabled()){
	           c_logger.traceDebug("retrieveAppName", "Retrieving app name from component");
	        }
			appName = info.getComponent();
		}
		
		 if (c_logger.isTraceDebugEnabled()){
           c_logger.traceDebug("retrieveAppName", "appName=" + appName);
        }
		return appName;
	}
}
