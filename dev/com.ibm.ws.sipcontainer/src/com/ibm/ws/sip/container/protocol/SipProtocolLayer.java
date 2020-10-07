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
package com.ibm.ws.sip.container.protocol;

import java.util.Iterator;
import java.util.TooManyListenersException;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.sip.SipURI;

import com.ibm.sip.util.log.*;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jain.protocol.ip.sip.address.AddressFactoryImpl;
import com.ibm.ws.sip.container.internal.SipContainerComponent;
import com.ibm.ws.sip.container.pmi.PerformanceMgr;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.proxy.SipProxyInfo;
import com.ibm.ws.sip.container.router.SipRouter;
import com.ibm.ws.sip.container.servlets.*;
import com.ibm.ws.sip.container.util.SipUtil;
import com.ibm.ws.sip.properties.SipPropertiesMap;
import com.ibm.ws.sip.stack.properties.StackProperties;
import com.ibm.ws.sip.stack.transaction.util.ApplicationProperties;

import jain.protocol.ip.sip.*;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

/**
 * @author Amir Perlman, Jan 26, 2003 
 *
 * Receives Sip Message from the Jain Sip Stack. Messages are wrapped by 
 * appropriate Siplet objects and passed for processing. 
 */
public class SipProtocolLayer implements SipListener
{
	/*
	 * Trace variable
	 */
	private static final TraceComponent tc = Tr.register(SipProtocolLayer.class);

	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(SipProtocolLayer.class);

	/**
	 * The one and only sip stack. 
	 */
	private SipStack m_sipStack;

	/**
	 * The message router to servlets. 
	 */
	private SipRouter m_servletsRouter = SipRouter.getInstance();

	/**
	 * Pointer to Jain stack properties that is passed to messages. 
	 */
	private StackProperties m_stackProperties;


	/**
	 *  if the first request was already received
	 */
	private boolean m_firstRequestReceived = false;
	private final ReentrantLock m_lock = new ReentrantLock();

	private boolean initialized = false;

	/**
	 * Singleton instance returned in a thread-safe manner
	 */
	private static volatile SipProtocolLayer instance;
	public static SipProtocolLayer getInstance(){
		SipProtocolLayer theInstance = instance;
		if(theInstance == null){
			synchronized (SipProtocolLayer.class) {
				theInstance = instance;
				if(theInstance == null){
					instance = theInstance = new SipProtocolLayer();
				}
			}			
		}
		return theInstance;
	}
	
	
	public boolean isInitialized() {
		return initialized;
	}


	/**
	 * Initialize the required sip components. 
	 * @throws SipPeerUnavailableException
	 */
	public void init() throws SipPeerUnavailableException
	{
		// Create the SIP stack
		try
		{
			// Set the path so we'll create the correct stack 
			SipFactory.getInstance().setPathName("com.ibm.ws");
			SipPropertiesMap properties = PropertiesStore.getInstance().getProperties();
			try
			{
				// Add properties for the SIP stack
				ApplicationProperties.setProperties(properties);

				// Create the SIP stack
				SipContainerComponent.activateSipStack();
				m_sipStack = SipContainerComponent.getSipStackService().getService();
				if(m_sipStack == null){
					if (c_logger.isTraceDebugEnabled())
					{
						c_logger.traceDebug( this, "init", "SipStack service was not set");
					}
				}
			}
			catch (Throwable e)
			{
				if (c_logger.isErrorEnabled())
				{
					c_logger.error(
							"error.sip.stack.exception",
							Situation.SITUATION_CREATE,
							null,
							e);
				}
				throw new SipPeerUnavailableException();
			}


			// Create the jain sip factory instance and set it's member
			SipFactory jainSipfactory = SipFactory.getInstance();
			m_stackProperties = StackProperties.getInstance();
			m_stackProperties.setFactories(
					jainSipfactory.createMessageFactory(),
					jainSipfactory.createHeaderFactory(),
					jainSipfactory.createAddressFactory());

			if (c_logger.isTraceDebugEnabled())
			{
				c_logger.traceDebug(
						this,
						"init",
						"Sip Protocol Layer Using Sip Stack:"
								+ m_sipStack.getStackName());

			}
			
			SipContainerComponent.startGenericEndpoints();
			
			PerformanceMgr perfMgr = PerformanceMgr.getInstance();
			if (perfMgr != null) {
				perfMgr.init(properties);
			}
			SipContainerComponent.activatePerfManager();
			
			initialized = true;
			
			// Call init on service activation
			// PerformanceMgr.getInstance().init(false,properties);

		}
		catch (SipPeerUnavailableException e)
		{
			if (c_logger.isErrorEnabled())
			{
				c_logger.error(
						"error.sip.stack.exception",
						Situation.SITUATION_CREATE,
						null,
						e);
			}

			throw e;
		}

	}

	/**
	 * Initialize the 
	 */
	public void initNewInterfaces(ListeningPoint listeningPoint){
		createSipListeners(listeningPoint);
		initOutboundIfaceList(listeningPoint);
	}
	
	/**
	 * this method should be called when SIP container runs in standalone.
	 * If SIP container is fronted with SIP Proxy then we should empty
	 * SipProxyInfo content (and should be filled with info from SIP proxy).
	 */
	private void initOutboundIfaceList(ListeningPoint listeningPoint) {
		if (c_logger.isTraceEntryExitEnabled())
		{
			c_logger.traceEntry(this, "initOutboundIfaceList");
		}

		String host = listeningPoint.getHost();
		int port = listeningPoint.getPort();
		String transport =  listeningPoint.getTransport();
		SipURL url;
		try {
			url = AddressFactoryImpl.createSipURL(null,null,null,host,port,
					null,null,null,transport);
			SipURI newUri = new SipURIImpl(url);

			if (newUri.getTransportParam().equalsIgnoreCase("udp") == true)
				SipProxyInfo.getInstance().addUdpInterface(newUri);
			else if (newUri.getTransportParam().equalsIgnoreCase("tcp") == true)
				SipProxyInfo.getInstance().addTcpInterface(newUri);
			else if (newUri.getTransportParam().equalsIgnoreCase("tls") == true)
				SipProxyInfo.getInstance().addTlsInterface(newUri);

		} catch (SipParseException e) {
			if (c_logger.isErrorEnabled())
			{
				c_logger.error(
						"error.sip.stack.exception",
						Situation.SITUATION_CREATE,
						null,
						e);
			}

		}
	}

	/**
	 * Helper function. Creates Sip Providers for all available Sip Listening
	 * Points and adds Sip Listeners to these providers. 
	 */
	private void createSipListeners(ListeningPoint listeningPoint)
	{

		if (c_logger.isTraceEntryExitEnabled())
		{
			c_logger.traceEntry(this, "createSipListeners");
		}

		SipProvider provider;
		if (c_logger.isInfoEnabled())
		{
			Object[] args =
				{   listeningPoint.getHost(),
					new Integer(listeningPoint.getPort()),
					listeningPoint.getTransport()};
			c_logger.info( "info.listening.point", Situation.SITUATION_START, args);
		}
		Exception exp = null;
		try
		{
			provider = m_sipStack.createSipProvider(listeningPoint);
			provider.addSipListener(this);

			m_stackProperties.addProvider(provider);
		}
		catch (ListeningPointUnavailableException e)
		{
			exp = e;
			throw new RuntimeException();
		}
		catch (SipListenerAlreadyRegisteredException e)
		{
			exp = e;
		}
		catch (TooManyListenersException e)
		{
			exp = e;
		}
		finally{
			if(exp != null){
				if (c_logger.isErrorEnabled())
				{
					c_logger.error(
							"error.sip.stack.exception",
							Situation.SITUATION_CONFIGURE,
							null,
							exp);

				}
			}
			if (c_logger.isTraceEntryExitEnabled())
			{
				c_logger.traceExit(this, "createSipListeners");
			}
		}

	}

	/**
	 * Processes a Response received on one of the SipListener's ListeningPoints.
	 * @param <var>responseReceivedEvent</var> SipEvent received because Response was received
	 */
	public void processResponse(SipEvent responseReceivedEvent)
	{
		Response response = (Response) responseReceivedEvent.getMessage();
		long transactionId = responseReceivedEvent.getTransactionId();

		if (c_logger.isTraceDebugEnabled())
		{
			StringBuffer b = new StringBuffer(64);
			b.append("Transaction: ");
			b.append(transactionId);
			b.append("\r\n");
			b.append(response);

			c_logger.traceDebug(this, "processResponse", b.toString());
		}

		SipProvider provider = (SipProvider) responseReceivedEvent.getSource();

		//Stray responses are recieved without any transaction context. 
		if(transactionId == -1)
		{
			m_servletsRouter.handleStrayResponses(response, provider);
		}
		else
		{

			//Create a Sip Servlet Response
			SipServletResponseImpl sipResponse =
					new IncomingSipServletResponse(response, transactionId, provider);

            if(responseReceivedEvent.getEventId() == SipEvent.ERROR_RESPONSE_CREATED_INTERNALLY){
            	SipUtil.setIntenalResponseFlag(sipResponse);
            }
			//Route message to Servlets. 
			m_servletsRouter.handleResponse(sipResponse);
		}

	}

	/**
	 * Processes the time out of a transaction specified by
	 * the transactionId.
	 * @param <var>transactionTimeOutEvent</var> SipEvent received because transaction timed out
	 */
	public void processTimeOut(SipEvent transactionTimeOutEvent)
	{
		if (c_logger.isTraceDebugEnabled())
		{
			c_logger.traceDebug(
					this,
					"processTimeOut",
					"Received a Sip Timeout\n"
							+ transactionTimeOutEvent.getTransactionId());
		}

		long transactionId = transactionTimeOutEvent.getTransactionId();
		m_servletsRouter.handleTimeout(transactionId);
	}

	/**
	 * Processes a Request received on one of the SipListener's ListeningPoints.
	 * @param <var>requestReceivedEvent</var> SipEvent received because Request was received
	 */
	public void processRequest(SipEvent requestReceivedEvent)
	{
		Request request = (Request) requestReceivedEvent.getMessage();
		long transactionId = requestReceivedEvent.getTransactionId();
		SipProvider provider = (SipProvider) requestReceivedEvent.getSource();

		if (c_logger.isTraceDebugEnabled())
		{
			StringBuffer b = new StringBuffer(64);
			b.append("Transaction: ");
			b.append(transactionId);
			b.append("\r\n");
			b.append(requestReceivedEvent.getMessage());

			c_logger.traceDebug(this, "processRequest", b.toString());
		}

		//Create a Sip Servlet Request
		SipServletRequestImpl sipRequest =
				new IncomingSipServletRequest(request, transactionId, provider);

		//if its the first request we need to initialize the SIP application router
		m_lock.lock();
		if (!m_firstRequestReceived) {
			SipContainerComponent.activateSipApplicationRouter();
			m_firstRequestReceived = true;
		}
		m_lock.unlock();

		//Route message to Servlets. 
		m_servletsRouter.handleRequest(sipRequest);
	}

	/**
	 * Stop and clean up. 
	 */
	public void stop()
	{
		// TODO Auto-generated method stub

	}

	/**
	 * Get all listening points from the Stack
	 * @return
	 */
	public Iterator getListeningPoints(){
		return m_sipStack.getListeningPoints();
	}
}

