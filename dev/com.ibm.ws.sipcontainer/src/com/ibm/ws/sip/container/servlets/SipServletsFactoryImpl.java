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
package com.ibm.ws.sip.container.servlets;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.SipProvider;
import jain.protocol.ip.sip.address.NameAddress;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.header.CSeqHeader;
import jain.protocol.ip.sip.header.CallIdHeader;
import jain.protocol.ip.sip.header.ContactHeader;
import jain.protocol.ip.sip.header.ContentLengthHeader;
import jain.protocol.ip.sip.header.ContentTypeHeader;
import jain.protocol.ip.sip.header.FromHeader;
import jain.protocol.ip.sip.header.Header;
import jain.protocol.ip.sip.header.MaxForwardsHeader;
import jain.protocol.ip.sip.header.NameAddressHeader;
import jain.protocol.ip.sip.header.RecordRouteHeader;
import jain.protocol.ip.sip.header.RouteHeader;
import jain.protocol.ip.sip.header.ToHeader;
import jain.protocol.ip.sip.header.ViaHeader;
import jain.protocol.ip.sip.message.Request;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

import javax.servlet.sip.Address;
import javax.servlet.sip.AuthInfo;
import javax.servlet.sip.Parameterable;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.jain.protocol.ip.sip.header.GenericNameAddressHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.GenericParametersHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.ParametersHeaderImpl;
import com.ibm.ws.sip.container.asynch.AsynchronousWorkTask;
import com.ibm.ws.sip.container.failover.repository.SessionRepository;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.parser.SipServletDesc;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.router.SipAppDescManager;
import com.ibm.ws.sip.container.tu.TransactionUserBase;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.container.util.SipUtil;
import com.ibm.ws.sip.container.was.ThreadLocalStorage;
import com.ibm.ws.sip.properties.CoreProperties;
import com.ibm.ws.sip.security.auth.AuthInfoFactory;
import com.ibm.ws.sip.stack.properties.StackProperties;
import com.ibm.ws.sip.stack.transaction.util.SIPStackUtil;
import com.ibm.ws.sip.stack.util.SipStackUtil;

/**
 * @author Amir Perlman, Feb 18, 2003
 *
 * Implementation for the Sip Servlets - Sip Factory API. 
 */
public class SipServletsFactoryImpl implements SipFactory
{
	

	private static final char GREATER_THEN = '>';

	public static final String UNKNOWN_APPLICATION = "Unknown application";

	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger =
		Log.get(SipServletsFactoryImpl.class);

	/**
	 * The single instance of the factory, not associated with any specific 
	 * SIP App.  
	 */
	private static SipServletsFactoryImpl c_sipFactory;

	/**
	 * The SIP App Descriptor associated with this Factory. 
	 */
	private SipAppDesc m_sipAppDesc;

	/**
	 * Constructs a new Sip Factory associated with the given SIP App. 
	 */
	private SipServletsFactoryImpl(SipAppDesc appDesc)
	{
		m_sipAppDesc = appDesc;
	}

	/**
	 * Constructs a new Sip Factory NOT associated with any SIP App. 
	 */
	private SipServletsFactoryImpl()
	{
	}

	/**
	 * @see javax.servlet.sip.SipFactory#createAddress(String)
	 */
	public Address createAddress(String sipAddress)
	throws ServletParseException
	{
		if (c_logger.isTraceEntryExitEnabled())
		{
			Object[] params = { sipAddress };
			c_logger.traceEntry(this, "createAddress", params);
		}

		Address address = null;

		if (sipAddress.equals("*"))
		{
			//Create the wildcard address object
			address = new WildcardNameAddress();
		}
		else
		{
			StackProperties sp = StackProperties.getInstance();
			try
			{
				//Patch: To avoid doing the parsing ourself or use the parser 
				//directly, we create a dummy TO header and pass it to the address
				//impl.
        		boolean parseAddressAccordingToJSr = PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.SIP_JSR289_PARSE_ADDRESS);
        		String headerTemplate = RouteHeader.name;
        		
        		//if we don't want to parse the address correctly we will use the route header template
        		//in To/From/Contact headers parameters outside of <> are belong to the header and not 
        		//to the URL. in all other headers parameters belongs to the url
        		if (parseAddressAccordingToJSr){
        			if (c_logger.isTraceDebugEnabled()){
        				c_logger.traceDebug(this, "createAddress", "To header is used as a template, the address will be parsed according to the JSR");
        			}
        			headerTemplate = ToHeader.name;
        		}
        		
				NameAddressHeader header =
					(NameAddressHeader) sp.getHeadersFactory().createHeader(
							headerTemplate,
							sipAddress);
				address = new AddressImpl(header);
			}
			catch (SipParseException e)
			{
				throw new ServletParseException(
						e.getMessage() + "\n" + e.toString());
			}
		}

		if (c_logger.isTraceDebugEnabled())
		{
			c_logger.traceDebug(this, "createAddress", "Address = " + address);
		}

		return address;
	}

	/**
	 * @see javax.servlet.sip.SipFactory#createParameterable(String)
	 */
	public Parameterable createParameterable(String paramString)
	throws ServletParseException
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { paramString };
			c_logger.traceEntry(this, "createParameterable", params);
		}

		if (paramString == null) {
			throw new IllegalArgumentException("null parameterable string");
		}

		// try to create the header as an address header. if it doesn't parse
		// as an address, try as a generic parameters header.
		ParametersHeaderImpl parametersHeader =
			new GenericNameAddressHeaderImpl("IBM-GenericAddressHeader");
		try {
			parametersHeader.setValue(paramString);
			parametersHeader.parse();
		}
		catch (SipParseException dontPrintMe) {
			parametersHeader = new GenericParametersHeaderImpl("IBM-GenericParametersHeader");
			try {
				parametersHeader.setValue(paramString);
				parametersHeader.parse();
			}
			catch (SipParseException e) {
				// not a parameters header. give up.
				throw new ServletParseException("bad parameterable syntax ["
						+ paramString + ']');
			}
		}

		// wrap the parameters header with a new parameterable
		ParameterableImpl parameterable = new ParameterableImpl(parametersHeader);

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "createParameterable",
					"parameterable created [" + parameterable + ']');
		}
		return parameterable;
	}

	/**
	 * Helper method to measure left-trim for field name from the field string
	 * @param fieldString
	 * @param fieldNameBegin
	 * @return
	 */
	private int measureFieldLeftTrim(String fieldString, int fieldNameBegin) {

		// measure left-trim for field name
		int delimiter = fieldString.indexOf(":");
		int i = delimiter;
		while (i-- > fieldNameBegin) {
			char c = fieldString.charAt(i);

			if ((c == ' ' || c == '\t') && fieldNameBegin == 0) {
				// right-trim header name
				delimiter = i;
			} else {
				break;
			}
		}
		return delimiter;
	}

	/**
	 * Helper method to measure right-trim for field name from the field string
	 * 
	 * @param fieldString - the field string
	 * @return the delimiter to start reading the field name
	 */
	private int measureFieldRightTrim(String fieldString) {
		int fieldNameBegin = 0;
		int i = 0;

		//       measure right-trim for field name
		while (i < fieldString.length()) {
			char c = fieldString.charAt(i);
			if (c == ':') {
				break;
			}
			i++;

			if ((c == ' ' || c == '\t') && fieldNameBegin == 0) {
				// right-trim header name
				fieldNameBegin = i;
			}else {
				break;
			}
		}
		return fieldNameBegin;
	}

	/**
	 * @see javax.servlet.sip.SipFactory#createAddress(URI, String)
	 */
	public Address createAddress(URI uri, String displayName)
	{
		if (c_logger.isTraceEntryExitEnabled())
		{
			Object[] params = { uri, displayName };
			c_logger.traceEntry(this, "createAddress", params);
		}

		Address address = null;

		try
		{
			jain.protocol.ip.sip.address.URI jainSipURI = null;
			StackProperties sp = StackProperties.getInstance();

			if (uri instanceof URIImpl)
			{
				jainSipURI = ((URIImpl)uri).getJainURI();
			} 
			else
			{
				String local = uri.toString();
				local = local.substring(local.indexOf(':') + 1);

				jainSipURI =
					sp.getAddressFactory().createURI(uri.getScheme(), local);
			}

			NameAddress nameAddress = (displayName == null)?
					sp.getAddressFactory().createNameAddress(jainSipURI):
						sp.getAddressFactory().createNameAddress(displayName, jainSipURI);

					//Use a from header as the base name address header is abstract
					//In any case the name of the headers should be discarded 
					NameAddressHeader header = 
						sp.getHeadersFactory().createFromHeader(nameAddress);
					address = new AddressImpl(header);
		}
		catch (IllegalArgumentException e)
		{
			if (c_logger.isErrorEnabled())
			{
				Object[] args = { uri, "not exist" };
				c_logger.error(
						"error.create.address",
						Situation.SITUATION_REQUEST,
						args,
						e);
			}
		}
		catch (SipParseException e)
		{
			if (c_logger.isErrorEnabled())
			{
				Object[] args = { uri, "not exist" };
				c_logger.error(
						"error.create.address",
						Situation.SITUATION_REQUEST,
						args,
						e);
			}
		}

		if (c_logger.isTraceEntryExitEnabled())
		{
			c_logger.traceExit(this, "createAddress", address);
		}
		return address;
	}

	/**
	 * @see javax.servlet.sip.SipFactory#createAddress(URI)
	 */
	public Address createAddress(URI uri)
	{
		return createAddress(uri, null);
	}

	/**
	 * @see javax.servlet.sip.SipFactory#createApplicationSession()
	 */
	public SipApplicationSession createApplicationSession()
	{
		// This part is for asynch invocation feature
		if (m_sipAppDesc == null) {
			m_sipAppDesc = new SipAppDesc(UNKNOWN_APPLICATION, UNKNOWN_APPLICATION); 
			m_sipAppDesc.addSipServlet(new SipServletDesc(m_sipAppDesc, "AsynchWorkSiplet", "none"));               
		}
		return createApplicationSession(null);
	}

	/**
	 * Creates SIP application session for a specific logical name, if logical name is null it's ignored.
	 * @param logicalName
	 * @return
	 */
	public SipApplicationSession createApplicationSession(String logicalName)
	{
		SipApplicationSessionImpl appSession = (SipApplicationSessionImpl) WASXSipApplicationSessionFactory.getInstance().createSipApplicationSession(
				TransactionUserBase.createNextApplicationSessionId(logicalName));

		appSession.setSynchronizer(new Object());
		appSession.setServiceSynchronizer(new Object());
		appSession.setSipApp(m_sipAppDesc,true);
		appSession.addToApplicationSessionsTable();
		return appSession;
	}

	/**
	 * @see javax.servlet.sip.SipFactory#createRequest(SipApplicationSession, 
	 *                                                                                          String, Address, Address)
	 */
	public SipServletRequest createRequest(
			SipApplicationSession appSession,
			String method,
			Address from,
			Address to)
	{

		return createRequest(appSession, method, from , to, true);
	}
	/**
	 * @see javax.servlet.sip.SipFactory#createRequest(SipApplicationSession, 
	 *                                                                                          String, Address, Address)
	 *  @param requiresCloning Indicates whether address objects are safe to 
	 * use or require cloning. 
	 */
	private SipServletRequest createRequest(
			SipApplicationSession appSession,
			String method,
			Address fromParam,
			Address toParam, 
			boolean requiresCloning)
	{
		Address from = fromParam;
		Address to = toParam;

		if (c_logger.isTraceEntryExitEnabled())
		{
			Object[] params = { appSession, method, from, to };
			c_logger.traceEntry(this, "createRequest", params);
		}

		if (method.equalsIgnoreCase(Request.ACK)
				|| method.equalsIgnoreCase(Request.CANCEL))
		{
			throw new IllegalArgumentException("ACK/CANCEL can not be created by this method. See documentation");
		}

		if(from.isWildcard() || to.isWildcard())
		{
			throw new IllegalArgumentException("From/To address can not be a wildcard address");
		}
		// MML 587682 - null appSession
		if (null == appSession || !appSession.isValid()) 
		{
			throw new IllegalArgumentException("Sip Application Session does not exists or had been invalidated.");
		}

		if(requiresCloning)
		{
			from = ((Address) ((AddressImpl)from).clone(true));
			to = ((Address) ((AddressImpl)to).clone(true));
		}

		Address sipUri = (Address)to.clone();

		cleanSIPUri(to);
		cleanSIPUri(from);

		//Make sure that we remove all existing tags, later we will create
		//our own. 
		((AddressImpl) from).removeTag();
		((AddressImpl) to).removeTag();
		
		OutgoingSipServletRequest request =
			new OutgoingSipServletRequest(
					method,
					from,
					to,
					createCallId(),
					null,
					(SipApplicationSessionImpl) appSession,
					null);

		request.setRequestURI(sipUri.getURI());

		SipAppDesc appDesc =
			((SipApplicationSessionImpl) appSession).getAppDescriptor();

		if (null != appDesc)
		{
			String servletName = ThreadLocalStorage.getSipServletName();
			SipServletDesc sipletDesc = appDesc.getSipServlet(servletName);

			if (!appDesc.hasMainServlet()){

				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug("Creating request from jsr116 application");
				}

				//Assign the default handler Siplet from the same application 
				//to the new request.
				if(sipletDesc ==  null){
					sipletDesc = appDesc.getDefaultSiplet();
				}
				
				request.getTransactionUser().setSipServletDesc(sipletDesc);

				// Set indication for application router 
				// intent supporting JSR 116 backward compatibility
				if (!appDesc.hasMainServlet())
					request.setAppInvokedName(appDesc.getApplicationName());

			} else {

				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug("Creating request from jsr289 application");
				}

				if (sipletDesc != null){

					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug("Set handler to be - [" + sipletDesc + "]");
					}

					request.getTransactionUser().setSipServletDesc(sipletDesc);
				}
				else{

					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug("Set handler to be - [" + appDesc.getMainSiplet() + "]");
					}

					request.getTransactionUser().setSipServletDesc(appDesc.getMainSiplet());
				}
			} 
			
			//now we should check if the SAS/SS needs to get created, this must be done after the sip servlet desc is set on the TU
			if(!method.equals(AsynchronousWorkTask.SIP_METHOD)){
				request.getTransactionUser().createSessionsWhenListenerExists();
	        }
		}

		return request;
	}


	/**
	 * Removes forbidden paramethers from SIPURL
	 * @param addr
	 */
	private void cleanSIPUri(Address addr){
		if(addr.getURI() instanceof SipURIImpl){
			SipURIImpl uri = (SipURIImpl)addr.getURI();

			//          clean according to the RFC 3261 19.1.2
			//uri.removePort();
			uri.removeHeaders();

			uri.removeParameter(SipURIImpl.LR);
			uri.removeParameter(SipURIImpl.MADDR);
			uri.removeParameter(SipURIImpl.METHOD);
			uri.removeParameter(SipURIImpl.TRANSPORT);
			uri.removeParameter(SipURIImpl.TTL);
		}
		else
		{
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, 
						"cleanSIPUri", "Can not clean non SIP URI: " + 
						addr.getURI());
			}
		}
	}

	/**
	 * @see javax.servlet.sip.SipFactory#createRequest(SipApplicationSession, 
	 *                                                                                                  String, String, String)
	 */
	public SipServletRequest createRequest(
			SipApplicationSession appSession,
			String method,
			String from,
			String to)
	throws ServletParseException
	{
		if (c_logger.isTraceEntryExitEnabled())
		{
			Object[] params = { appSession, method, from, to };
			c_logger.traceEntry(this, "createRequest", params);
		}

		return createRequest(
				appSession,
				method,
				createAddress(from),
				createAddress(to), 
				false);
	}

	/**
	 * @see javax.servlet.sip.SipFactory#createRequest(SipApplicationSession, 
	 *                                                                                              String, URI, URI)
	 */
	public SipServletRequest createRequest(
			SipApplicationSession appSession,
			String method,
			URI from,
			URI to)
	{
		if (c_logger.isTraceEntryExitEnabled())
		{
			Object[] params = { appSession, method, from, to };
			c_logger.traceEntry(this, "createRequest", params);
		}

		return createRequest(
				appSession,
				method,
				createAddress((URI)((BaseURI)from).clone(false)),  //Must be cloned see - SPR #RDUH6EAT9E
				createAddress((URI)((BaseURI)to).clone(false)),         //Same as above
				false);
	}

	/**
	 * @see javax.servlet.sip.SipFactory#createRequest(SipServletRequest, 
	 *                                                                                             boolean)
	 */
	public SipServletRequest createRequest(
			SipServletRequest origRequest,
			boolean sameCallId)
	{
		if (c_logger.isTraceEntryExitEnabled())
		{
			Object[] params = { origRequest, new Boolean(sameCallId)};
			c_logger.traceEntry(this, "createRequest", params);
		}

		OutgoingSipServletRequest request;
		String callId = null;

		// Do we need to create a new callId
		if (sameCallId)
		{
			callId = origRequest.getCallId();
		}
		else
		{
			callId = createCallId();
		}

		//Reset the from and to tags to empty values. From tag will be generated
		//by the stack when request is sent
		AddressImpl from = (AddressImpl) ((AddressImpl)origRequest.getFrom()).clone(true);
		from.removeTag();
		
		AddressImpl to = (AddressImpl) ((AddressImpl)origRequest.getTo()).clone(true);
		to.removeTag();

		// There is a possibility that Application Session wasn't created yet
		// The creation of the app Session is added to fix a customer problem.
		// If no AppSession was created before this Ctor was called - the new
		// TU was not connected to the TU from original request - meaning 
		// that B2b was not performed as 2 legs that are connected to the same App session
		// This fix should be removed and replaced with different fix that will not
		// create AppSession in this time but will connect between 2 legs later
		// when application will call to the getApplicationSession(true)
		TransactionUserWrapper  tu = ((SipServletRequestImpl)origRequest).getTransactionUser();
		SipApplicationSession appSession = null;
		if (tu != null) {
			appSession = tu.getApplicationSession(true);
		}

		// Create the request
		request =
			new OutgoingSipServletRequest(
					origRequest.getMethod(),
					from,
					to,
					callId,
					((SipServletRequestImpl) origRequest).getSipProvider(),
					(SipApplicationSessionImpl) appSession,
					(SipServletRequestImpl) origRequest);

		// Copy application composition state
		Serializable stateInfo = 
			((SipServletRequestImpl)origRequest).getStateInfo();

		String nextApp = 
			((SipServletRequestImpl)origRequest).getNextApplication();

		SipApplicationRoutingDirective appDir = 
			((SipServletRequestImpl)origRequest).getDirective();

		SipApplicationRoutingRegion routingRegion = 
			((SipServletRequestImpl)origRequest).getRegion();

		request.setStateInfo(stateInfo);
		request.setNextApplication(nextApp);
		request.setDirective(appDir);
		request.setRoutingRegion(routingRegion);

		// Copy content
		try
		{
			if (origRequest.getContentLength() > 0)
			{
				Object content = origRequest.getRawContent();
				if (null != content)
				{
					request.setContent(
							origRequest.getContent(),
							origRequest.getContentType());
				}
			}

		}
		catch (UnsupportedEncodingException e)
		{
			if (c_logger.isErrorEnabled())
			{
				Object[] args = { request };
				c_logger.error(
						"error.create.request",
						Situation.SITUATION_REQUEST,
						args,
						e);
			}
		}
		catch (IOException e)
		{
			if (c_logger.isErrorEnabled())
			{
				Object[] args = { request };
				c_logger.error(
						"error.create.request",
						Situation.SITUATION_REQUEST,
						args,
						e);
			}
		}

		// Copy the request URI
		request.setRequestURI(origRequest.getRequestURI());

		// Copy headers from original Request to the new request
		copyNonSystemHeaders(origRequest, request);

		//Assign the new request the same Siplet for processing response 
		//as the original request.
		SipServletDesc sipletDesc =
			((SipServletMessageImpl) origRequest).getTransactionUser().getSipServletDesc();
		((SipServletMessageImpl) request).getTransactionUser().setSipServletDesc(sipletDesc);

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug("Creating request from jsr289 application - " +  sipletDesc.getSipApp().hasMainServlet());
		}


		// Set indication for application router 
		// intent supporting JSR 116 backward compatability
		if (!sipletDesc.getSipApp().hasMainServlet())
			request.setAppInvokedName(sipletDesc.getSipApp().getApplicationName());

		//now we should check if the SAS/SS needs to get created, this must be done after the sip servlet desc is set on the TU
		if(!origRequest.getMethod().equals(AsynchronousWorkTask.SIP_METHOD)){
			request.getTransactionUser().createSessionsWhenListenerExists();
        }

		return request;
	}

	/**
	 * copy the none system headers from the original request to the other request
	 * this method is helpful for b2b applications
	 * @param origRequest
	 * @param request
	 */
	public void copyNonSystemHeaders(SipServletRequest origRequest,
			OutgoingSipServletRequest request) {
		
		Iterator iterator =
			((SipServletRequestImpl) origRequest).getJainSipHeaders();
		if (iterator != null)
		{
			Request jainOutReq = request.getRequest();
			Header header;
			while (iterator.hasNext())
			{
				header = (Header) iterator.next();

				// Dont copy the following headers: Via, Record-Route, CSeq,
				// To, From, CallId. Contact is copied only in REGISTER requests
				if (!header.getName().equals(ViaHeader.name)
						&& !header.getName().equals(RecordRouteHeader.name)
						&& !header.getName().equals(CSeqHeader.name)
						&& !header.getName().equals(ToHeader.name)
						&& !header.getName().equals(FromHeader.name)
						&& !header.getName().equals(CallIdHeader.name)
						&& !header.getName().equals(MaxForwardsHeader.name)
						&& !header.getName().equals(ContentTypeHeader.name)
						&& !header.getName().equals(ContentLengthHeader.name)
						&& !header.getName().equals(SipStackUtil.DESTINATION_URI)
						&& !header.getName().equals(ContactHeader.name)
						|| (header.getName().equals(ContactHeader.name)
								&& request.getMethod().equals(Request.REGISTER)))
				{
					jainOutReq.addHeader(header, false);
				}
			}
		}
	}

	/**
	 * @see javax.servlet.sip.SipFactory#createSipURI(String, String)
	 */
	public SipURI createSipURI(String user, String host)
	{
		if (c_logger.isTraceEntryExitEnabled())
		{
			Object[] params = { user, host };
			c_logger.traceEntry(this, "createSipURI", params);
		}

		SipURI sipURI = null;

		SipURL jainSipURL;
		try
		{
			StackProperties sp = StackProperties.getInstance();
			jainSipURL = sp.getAddressFactory().createSipURL(user, host);
			sipURI = new SipURIImpl(jainSipURL);
		}
		catch (IllegalArgumentException e)
		{
			if (c_logger.isErrorEnabled())
			{
				Object[] args = { user, host };
				c_logger.error(
						"error.create.sip.uri",
						Situation.SITUATION_REQUEST,
						args,
						e);
			}
		}
		catch (SipParseException e)
		{
			if (c_logger.isErrorEnabled())
			{
				Object[] args = { user, host };
				c_logger.error(
						"error.create.sip.uri",
						Situation.SITUATION_REQUEST,
						args,
						e);
			}
		}

		if (c_logger.isTraceDebugEnabled())
		{
			c_logger.traceDebug(this, "createSipURI", "SIP Uri = " + sipURI);
		}

		return sipURI;
	}

	/**
	 * @see javax.servlet.sip.SipFactory#createURI(String)
	 */
	public URI createURI(String str) throws ServletParseException
	{
		if (c_logger.isTraceEntryExitEnabled())
		{
			Object[] params = { str };
			c_logger.traceEntry(this, "createURI", params);
		}
		// MML 559441 - null string 
		if (null == str) 
		{
			throw new ServletParseException("The URI was not specified.");
		}
		//There shouldn't be any brackets in a uri - if there are brackets then it is address 
		int gtIndex = str.indexOf(GREATER_THEN);
		if(gtIndex >-1) {
			throw new ServletParseException("Bad URI.");
		}

		URI uri = null;

		try
		{
			//assume the following structure scheme:data
			//take the prefix up to the ':' as the scheme parameter
			String scheme;
			int index = str.indexOf(':');

			if (index == -1)
			{
				if (c_logger.isErrorEnabled())
				{
					Object[] args = { uri };
					c_logger.error(
							"error.create.uri",
							Situation.SITUATION_REQUEST,
							args);
				}

				throw new ServletParseException("Invalid URI scheme");
			}

			
			scheme = str.substring(0, index);

			String uriString = str.substring(index+1);
			StackProperties sp = StackProperties.getInstance();
			jain.protocol.ip.sip.address.URI jainURI;

			if (SipURIImpl.isSchemeSupported(scheme))
			{
				jainURI = sp.getAddressFactory().createURI(scheme, uriString);
				uri = new SipURIImpl((SipURL) jainURI);
			}
			else if (TelURLImpl.isSchemeSupported(scheme))
			{
				//adding > to force the parser to parse the parameters as uri parameters and not as header
				//parameters
				jainURI = sp.getAddressFactory().createURI(scheme, uriString+GREATER_THEN);
				uri = new TelURLImpl(jainURI, true);
			}
			else
			{
				jainURI = sp.getAddressFactory().createURI(scheme, uriString);
				uri = new URIImpl(jainURI);
			}

		}
		catch (IllegalArgumentException e)
		{
			if (c_logger.isTraceDebugEnabled())
			{
				c_logger.traceDebug(
						"error.create.uri",
						Situation.SITUATION_REQUEST,
						uri == null? null : uri.toString(),
								e);
			}

			throw new ServletParseException(e.toString());
		}
		catch (SipParseException e)
		{
			if (c_logger.isTraceDebugEnabled())
			{
				c_logger.traceDebug(
						"error.create.uri",
						Situation.SITUATION_REQUEST,
						uri == null? null : uri.toString(),
								e);
			}

			throw new ServletParseException(e.toString());
		}

		if (c_logger.isTraceDebugEnabled())
		{
			c_logger.traceDebug(this, "createURI", "Uri = " + uri);
		}

		return uri;
	}

	/**
	 * Currenttly implemented as a singleton. 
	 * @param appName Name of the SIP Application. Each SIP Application will 
	 * be associated with a single factory. 
	 */
	public synchronized static SipFactory getInstance(String appName)
	{
		if (c_logger.isTraceEntryExitEnabled())
		{
			Object[] params = { appName };
			c_logger.traceEntry("SIP Servlets Factory", "getInstance", params);
		}

		SipFactory factory = null;

		//Try to get the SIP Application descriptor for the given SIP app name. 
		SipAppDesc appDesc = SipAppDescManager.getInstance().getSipAppDescByAppName(appName);
		
		if(appDesc == null){
			
			appDesc = SipAppDescManager.getInstance().getSipAppDesc(appName);
			if (c_logger.isTraceDebugEnabled())
			{
				c_logger.traceDebug(
						"SIP Servlets Factory",
						"getInstance",
						"getApp by WebAppName, result"
						+ appDesc);
			}
		}

		if (null != appDesc)
		{
			factory = appDesc.getSipFactory();
			if (null == factory)
			{
				//First time only associate a factory with the SIP App
				factory = new SipServletsFactoryImpl(appDesc);
				appDesc.setSipFactory(factory);
			}
		}
		else
		{
			//Use the default SIP Factory which is not associated with any SIP
			//application. 
			factory = getInstance();

			if (c_logger.isTraceDebugEnabled())
			{
				c_logger.traceDebug(
						"SIP Servlets Factory",
						"getInstance",
						"Using global factory, Unknown SIP App descriptor : "
						+ appName);
			}
		}

		if (c_logger.isTraceEntryExitEnabled())
		{
			c_logger.traceExit("SIP Servlets Factory", "getInstance", factory);
		}

		return factory;
	}

	/**
	 * Gets the single instance of the global factory instance. 
	 * @return
	 */
	public synchronized static SipServletsFactoryImpl getInstance()
	{
		if (null == c_sipFactory)
		{
			c_sipFactory = new SipServletsFactoryImpl();
		}

		return c_sipFactory;
	}

	/**
	 * Create a unique call ID
	 * 
	 * @return String
	 */
	private String createCallId()
	{
		SipProvider provider = StackProperties.getInstance().getFirstProvider();
		// PM61939 - hide local IP in Call-ID
    	// getCallIdValue() returns the value of the new custom property com.ibm.ws.sip.callid.value if set
		// if not set returns a local IP
		return SIPStackUtil.generateCallIdentifier(provider.getListeningPoint().getCallIdValue());
	}

	/**
	 * Internal Utility function for genearting javax.servlet.sip.URI from 
	 * a Jain URI
	 */
	public URI generateURI(jain.protocol.ip.sip.address.URI jainUri)
	{
		URI uri = null;
		String scheme = jainUri.getScheme();
		if (SipURIImpl.isSchemeSupported(scheme))
		{
			uri = new SipURIImpl((SipURL) jainUri);
		}
		else if (TelURLImpl.isSchemeSupported(scheme))
		{
			uri = new TelURLImpl(jainUri);
		}
		else
		{
			uri = new URIImpl(jainUri);
		}

		return uri;
	}

	/**
	 * Removed from javax - hide 289 API
	 * Create a new authinfo object, for authenticating a SIP request.
	 */
	public AuthInfo createAuthInfo() {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "createAuthInfo");
		}

		AuthInfo authInfo = AuthInfoFactory.createAuthInfo(); 

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "createAuthInfo", authInfo);
		}
		return authInfo;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.sip.SipFactory#createApplicationSessionByKey(java.lang.String)
	 */
	public SipApplicationSession createApplicationSessionByKey(String applicationSessionKey){
		return createApplicationSessionByKey(applicationSessionKey, true);           
	}
	
	/**
	 * return Sip application Session according to the key, if SAS is not found
	 * and the create flag is true create a new SAS otherwise return null 
	 * 
	 * @param applicationSessionKey
	 * @param create
	 * @return
	 */
	public SipApplicationSession createApplicationSessionByKey(String applicationSessionKey, boolean create){
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "createApplicationSessionByKey", new Object[] {applicationSessionKey, create});
		}
		
		if (applicationSessionKey == null) {
			throw new NullPointerException("applicationSessionKey is null");
		}
		String appName = m_sipAppDesc != null ? m_sipAppDesc.getApplicationName() : "";

		String appKey = SipUtil.composeSessionKeyBaseKey(appName, applicationSessionKey);
		String sipAppID = SessionRepository.getInstance().getKeyBaseAppSession(appKey);

		SipApplicationSession sipAppSession = null;
		if (sipAppID != null) {
			sipAppSession = SipApplicationSessionImpl.getAppSession(sipAppID);
		} else if (create){
			// create application session and attach it to the key
			sipAppSession = createApplicationSession();
			SessionRepository.getInstance().setSessionKeyBase(appKey, sipAppSession.getId());
			((SipApplicationSessionImpl) sipAppSession).setSessionKeyBaseTargeting(appKey);
		}

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "createApplicationSessionByKey", sipAppSession);
		}
		return sipAppSession;           
	}
}
