/*******************************************************************************
 * Copyright (c) 2003,2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jain.protocol.ip.sip;

import java.io.IOException;
import java.util.*;

import org.osgi.service.component.annotations.*;

import com.ibm.sip.util.log.*;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.properties.SipPropertiesMap;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;

import jain.protocol.ip.sip.*;

/**
 * 
 * @author Amirk
 *
 * A declarative services component.
 * The component is responsible of SipStack implementation.
 * Is injected when a initializing sipProtocolLayer.
 * 
 */
@Component(service = SipStack.class,
configurationPolicy = ConfigurationPolicy.OPTIONAL,
configurationPid = "com.ibm.ws.jain.protocol.ip.sip",
name = "com.ibm.ws.jain.protocol.ip.sip",
property = {"service.vendor=IBM"} )
public class SipStackImpl
	implements SipStack
{

	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(SipStackImpl.class);
	
	/** 
	 * the stack's default name 
	 */
	private static final String STACK_NAME = "TEMP_NAME";
	
	/** 
	 * the stack's name 
	 */
	private String m_stackName;
	
	/** 
	 * list of stack providers 
	 */
	private List<SipProvider> m_providers;
	
	/** 
	 * trace variable 
	 */
	private static final TraceComponent tc = Tr.register(SipStackImpl.class);
	
	
	/** 
	 * the transaction stack 
	 */
	private SIPTransactionStack m_stack;
	

	
	/**
	 * DS method to activate this component.
	 * 
	 * @param 	properties 	: Map containing service & config properties
	 *            populated/provided by config admin
	 */
	public void activate(Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "SipStackImpl activated", properties);
      
        SipPropertiesMap props = PropertiesStore.getInstance().getProperties();
        props.updateProperties(properties);
        init();
    }
	
	/**
	 * DS method to modify this component.
	 * 
	 * @param 	properties 	: Map containing service & config properties
	 *            populated/provided by config admin
	 * @throws IOException 
	 */
	@Modified
	public void modified(Map<String, Object> properties){
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "SipStackImpl modified", properties);
        

    }
	
	/**
	 * DS method to deactivate this component.
	 * 
	 * @param reason int - representation of reason the component is stopping
	 */
	public void deactivate(int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "SipStackImpl deactivated, reason="+reason);
        

    }
	
	private void init()
	{
		if( c_logger.isTraceDebugEnabled())
		{
			c_logger.traceDebug(this,"SipStackImpl","trying to load sip stack");
		}

		try{
			m_providers = new ArrayList<SipProvider>(16);
			m_stack = SIPTransactionStack.instance();
			if( c_logger.isTraceDebugEnabled())
			{
				c_logger.traceDebug(this,"SipStackImpl","sip stack loaded!!!");
			}
		}
		catch( Throwable t )
		{
			if(c_logger.isErrorEnabled())
            {
                c_logger.error("error.exception.stack", Situation.SITUATION_CREATE, 
                        		null, t);
            }
			throw new RuntimeException("SIP stack failed to start", t);
		}
	}
	
	/** 
	 * creates a jain provider and starts listening
	 * on the listening point 
	 */
	public SipProvider createSipProvider(ListeningPoint listeningPoint)
		throws IllegalArgumentException, ListeningPointUnavailableException
	{
		ListeningPointImpl lp = (ListeningPointImpl)listeningPoint;
		if(! m_stack.getTransportCommLayerMgr().getListeningPoints().contains( lp ))
		{
			//if the listening point is a new one , create it
			try
			{
				 m_stack.getTransportCommLayerMgr().createSIPListenningConnection(lp);
			}
			catch (IOException e)
			{
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "createSipProvider", e
							.getMessage(), e);
				}
				throw new ListeningPointUnavailableException("cannot listen on " + lp );
			}			
		}
		//create only if we can listen
		SipProviderImpl provider = new SipProviderImpl();
		provider.setListeningPoint( lp );
		provider.setSipStack( this );
		lp.setProvider( provider );		
		m_providers.add( provider );
		
		return provider;
	}

	/** 
	 * stop and delete the provider 
	 **/
	public void deleteSipProvider(SipProvider sipProvider)
		throws UnableToDeleteProviderException, IllegalArgumentException
	{
		SipProviderImpl providerImpl = ( SipProviderImpl )sipProvider;
		providerImpl.stop();
		m_providers.remove( providerImpl  );
	}

	/** returns the Listening points */ 
	public Iterator getListeningPoints()
	{
		return  m_stack.getTransportCommLayerMgr().getListeningPoints().iterator();
	}

	/** return allproviders */
	public Iterator getSipProviders()
	{
		return m_providers.iterator();
	}

	/** stack name */ 
	public String getStackName()
	{
		return m_stackName == null ?  STACK_NAME : m_stackName ;
	}

	/** set stack name */
	public void setStackName(String stackName)
	{
		if( stackName == null )
		{
			throw new IllegalArgumentException("stack namecannot be null");
		}
		m_stackName = stackName;
	}
	
	/** return the transaction stack */
	public SIPTransactionStack getTransactionStack()
	{
		return m_stack;
	}
	
	/**
	 * get The underlyn Timer ObjectO
	 * @return - the stack's Timer
	 */
	public Timer getTimer()
	{
		return m_stack.getTimer();
	}
	

}
