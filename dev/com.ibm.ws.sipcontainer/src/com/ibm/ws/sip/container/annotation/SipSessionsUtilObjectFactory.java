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

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.osgi.service.component.annotations.Component;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.servlets.SipSessionsUtilImpl;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.factory.ResourceInfo;
import com.ibm.wsspi.injectionengine.factory.ResourceInfoRefAddr;

@Component(service = { ObjectFactory.class, SipSessionsUtilObjectFactory.class })
public class SipSessionsUtilObjectFactory implements ObjectFactory{
    
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipSessionsUtilObjectFactory.class);

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
		// -----------------------------------------------------------------------
		// Create a Date based on annotation attribute to verify it was processed
		// -----------------------------------------------------------------------
		String appName = info.getComponent();
		
		if (appName != null) {
			return new SipSessionsUtilImpl(appName);
		}
		
		if (c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug("Could not locate application name, returning default sip session util.");
		}    	
		return new SipSessionsUtilImpl(null);
	}
	
}
