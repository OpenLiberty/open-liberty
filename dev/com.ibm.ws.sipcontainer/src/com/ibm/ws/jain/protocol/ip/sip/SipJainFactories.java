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

import jain.protocol.ip.sip.SipFactory;
import jain.protocol.ip.sip.SipPeerUnavailableException;
import jain.protocol.ip.sip.address.AddressFactory;
import jain.protocol.ip.sip.header.HeaderFactory;
import jain.protocol.ip.sip.message.MessageFactory;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;

/**
 * @author amirk
 *  holds jain factories
 */
public class SipJainFactories
{
	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(SipJainFactories.class);


	
	/** jain factories */
	private HeaderFactory m_headerFactory;
	private MessageFactory m_messsageFactory;
	private AddressFactory m_addressFactory; 
	 
	/** the singletone */
	private static SipJainFactories m_singletone;
	
	 
	private SipJainFactories()
	{
		try
		{
			m_headerFactory = SipFactory.getInstance().createHeaderFactory();
			m_messsageFactory = SipFactory.getInstance().createMessageFactory();
			m_addressFactory = SipFactory.getInstance().createAddressFactory();
		}
		catch( SipPeerUnavailableException exp )
		{
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "SipJainFactories", exp.getMessage(),
						exp);
			}
		}
	}
	
	
	public static SipJainFactories getInstance()
	{
		if( m_singletone == null )
		{
			m_singletone = new SipJainFactories();
		}
		return m_singletone;
	}
	
	
	
	/** @return AddressFactory */
	public AddressFactory getAddressFactory()
	{
		return m_addressFactory;
	}

	/** @return HeaderFactory */
	public HeaderFactory getHeaderFactory()
	{
		return m_headerFactory;
	}

	/** @return MessageFactory */
	public MessageFactory getMesssageFactory()
	{
		return m_messsageFactory;
	}

}
