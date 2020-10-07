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
package com.ibm.ws.sip.container.util;

import jain.protocol.ip.sip.ListeningPoint;
import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.SipProvider;
import jain.protocol.ip.sip.address.AddressFactory;
import jain.protocol.ip.sip.address.NameAddress;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.header.NameAddressHeader;
import jain.protocol.ip.sip.header.ParametersHeader;
import jain.protocol.ip.sip.header.RouteHeader;
import jain.protocol.ip.sip.message.Message;
import jain.protocol.ip.sip.message.MessageFactory;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.TelURL;
import javax.servlet.sip.URI;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.ListeningPointImpl;
import com.ibm.ws.jain.protocol.ip.sip.address.AddressFactoryImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.ReasonHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.GenericNameAddressHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.message.RequestImpl;
import com.ibm.ws.jain.protocol.ip.sip.message.SipResponseCodes;
import com.ibm.ws.sip.container.events.ContextEstablisher;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.servlets.IncomingSipServletResponse;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;
import com.ibm.ws.sip.container.servlets.SipServletMessageImpl;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.servlets.SipServletResponseImpl;
import com.ibm.ws.sip.container.servlets.SipServletsFactoryImpl;
import com.ibm.ws.sip.container.servlets.SipSessionImplementation;
import com.ibm.ws.sip.container.servlets.SipURIImpl;
import com.ibm.ws.sip.container.tu.DialogUsageKey;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.parser.Coder;
import com.ibm.ws.sip.parser.Separators;
import com.ibm.ws.sip.parser.util.CharsBuffer;
import com.ibm.ws.sip.parser.util.CharsBuffersPool;
import com.ibm.ws.sip.properties.CoreProperties;
import com.ibm.ws.sip.stack.properties.StackProperties;
import com.ibm.ws.sip.stack.transaction.util.SIPStackUtil;
import com.ibm.ws.sip.stack.util.SipStackUtil;

/**
 * 
 * @author Anat Fradin, Jul 10, 2006
 *
 * This is a helper class that will contain all the common SIP information.
 */
public class SipUtil {
	
	 /**
	 * This error message is thrown when there are multiple Reason headers with the same protocol.
	 * See RFC 3326.
	 */
	public static final String REASON_PROTOCOL_MULTIPLE = "The Reason header includes more than one of the same protocol. Please use different protocols when there are multiple Reason headers.";
	
	 /**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(SipUtil.class);
	
	/**
	 * Defines the header name for the client address.  This will be appended to any request / response going to the container. 
	 * This header will not be on any request / response coming from the container.
	 */
	public final static String IBM_CLIENT_ADDRESS = "IBM-Client-Address"; 

	/** Parameter added to the top route header
	 * in outgoing requests, to indicate that the next hop is
	 * a strict router, and therefore the message should be
	 * routed to the request-uri instead of the top route,
	 * and the final destination is the bottom route, instead
	 * of the request-uri.
	 * the component sending the request should remove this
	 */
	public static final String STRICT_ROUTING_PARAM = "ibmsr";

	public static final String REFER_HEADER_NAME = "Refer-To";
	
	public static final String EARLY_ONLY_TAG = "early-only";
	public static final String TO_TAG = "to-Tag";
	public static final String FROM_TAG = "from-Tag";
	public static final String CALL_ID = "call-id";
	
	public static final String EVENT_HEADER = "Event";
	public static final String EVENT_HEADER_ID_PARAM = "id";

	public final static String PATH_PARAM = "path";

	public final static String DESTINATION_URI = SipStackUtil.DESTINATION_URI;
	
	public static final String SIP_SCHEME = SipStackUtil.SIP_SCHEME;
	public static final String SIPS_SCHEME = SipStackUtil.SIPS_SCHEME;
	
	public static final String TLS_TRANSPORT = SipStackUtil.TLS_TRANSPORT;
	public static final int DEFAULT_TLS_PORT = SipStackUtil.DEFAULT_TLS_PORT;

	
	public static final String SUBSCRIPTION_STATE = "Subscription-State";
	public static final String TERMINATED_STATE = "terminated";
	
	//parameter identifier to the route header that indicates that this route header
	//was added according to the application router ROUTE BACK mechanism - JSR289 15.14.1
	public static final String IBM_ROUTE_BACK_PARAM = "ibmrb";
	
	//the sip application session attribute name that stores the AR stateInfo
	public static final String IBM_STATE_INFO_ATTR = "com.ibm.ws.sip.ibm.state.info";

	 /**
     * This attribute will be set on the SipServletMessage when response
     * was generated locally.
     */
    private static final String INTERNAL_RESPONSE_ATTR = "com.ibm.websphere.sip.container.internal.message";
	/**
	 * array of dialog-related methods.
	 * @see #getDialogRelatedMethod(String)
	 * @see #getDialogRelatedMethod(int)
	 * @see #getDialogRelatedMethodId(String)
	 */
	private static final String[] dialogRelatedMethods = {
		Request.INVITE,
		RequestImpl.SUBSCRIBE
	};
	 
	/**
	 * Helper method that identify if sipMethod is method that creates a 
	 * dialog or not.
	 * @param method
	 * @return
	 */
	public static boolean isDialogInitialRequest(String method){
		if(method.equals(Request.INVITE) || 
				method.equals(RequestImpl.SUBSCRIBE)||
				method.equals(RequestImpl.REFER))
        {
            return true; 
        }
		return false;
	}
	
	/**
	 * According to the rfc not all responses should contain Contact Header.
	 * Only 1xx-3xx and 485.
	 * @param method
	 * @return
	 */
	public static boolean shouldAddContact(int error){
		if((error > 100 && error < 400) || error == 485)
        {
            return true; 
        }
		return false;
	}
	
	/**
	 * Helper method that matches a method to a type of dialog usage
	 *  
	 * @param method - the method in question
	 * 
	 * @return the method representing this dialog type
	 */
	public static String getDialogRelatedMethod(String method){
		// if adding new methods in the return value, need to add
		// also a new method ID in the dialogRelatedMethods array
		if(method.equals(Request.INVITE) || 
				method.equals(RequestImpl.UPDATE)||
				method.equals(RequestImpl.PRACK)||
				method.equals(Request.ACK)||
				method.equals(Request.CANCEL)||
				method.equals(Request.BYE)||
				method.equals(RequestImpl.INFO)){
			return Request.INVITE;
		}
		if (method.equals(RequestImpl.SUBSCRIBE)||
				method.equals(RequestImpl.NOTIFY)||
				method.equals(RequestImpl.REFER)){
            return RequestImpl.SUBSCRIBE;
        }
		return null;
	}

	/**
	 * Helper method to return a dialog usage method identifier,
	 * given a method.
	 * @param method - the method that was returned from a call to
	 *  {@link #getDialogRelatedMethod(String)} 
	 * @return the identifier, or -1 if not a dialog message
	 */
	public static int getDialogRelatedMethodId(String method) {
		int length = dialogRelatedMethods.length;
		for (int i = 0; i < length; i++) {
			if (method.equals(dialogRelatedMethods[i])) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Helper method to return a dialog usage method,
	 * given the unique identifier.
	 * @param id - the identifier that was returned from a call to
	 *  {@link #getDialogRelatedMethodId(String)} 
	 * @return the method, or null if no such dialog method ID
	 */
	public static String getDialogRelatedMethod(int id) {
		return dialogRelatedMethods[id];
	}

	/**
	 * Helper method that checks if the request with provided sip method 
	 * should contain ContactHeader
	 * @param method
	 * @return
	 */
	public static boolean shouldContainContact(String method){
		if(isDialogInitialRequest(method)||
				method.equals(RequestImpl.NOTIFY) ||
	        	method.equals(Request.REGISTER) ||
	        	//Moti: 28/8 defect 460274 FVT found UPDATE is missing contact header. 
	        	method.equals(RequestImpl.UPDATE) 
	        	)
        {
//			TODO in the REGISTER requests application MAY decide
//			if / which CONTACT header to put. Should REGISTER to be removed from
//			this method ?
			return true; 
        }
		return false;
	}
	/**
	 * Helper method that checks if the method belongs to the subsequent request
	 * received on the dialog that was already created
	 * @param method
	 * @return
	 */
	public static boolean isSubsequestRequest(String method){
		if(method.equals(Request.BYE) || 
				method.equals(RequestImpl.PRACK)||
				method.equals(RequestImpl.UPDATE)||
				method.equals(RequestImpl.NOTIFY)//||
				//TODO commented out just because all FVT tests are based wrongly 
				//on initial INFO
				//method.equals(RequestImpl.INFO))
				)
        {
            return true; 
        }
		
		return false;
	}
	
	/**
	 * Helper method that checks if the method is a target refresh request
	 * defined in RFC 5057 5.4
	 * @param method
	 * @return
	 */
	public static boolean isTargetRefreshRequest(String method){
		if(method.equals(Request.INVITE) || 
				method.equals(RequestImpl.UPDATE)||
				method.equals(RequestImpl.SUBSCRIBE)||
				method.equals(RequestImpl.NOTIFY) ||
				method.equals(RequestImpl.REFER)
				)
        {
            return true; 
        }
		
		return false;
	}
	
	/**
     * Checks whether the specified response is in the 2xx range.
     * 
     * @param status
     */
    public static boolean is2xxResponse(int status) {
        if (status >= 200 && status < 300) {
            return true;
        }
        return false;
    }
	
	/**
     * Checks whether the specified response is in the 2xx range.
     * 
     * @param status
     */
    public static boolean isErrorResponse(int status) {
        if (status >= 400 && status < 700) {
            return true;
        }
        return false;
    }

	/**
	 * Checks if this method can be sent or received on EARLY dialog
	 * @param method
	 * @param transactionUser
	 * @return
	 */
	public static boolean canReceiveOnDialog(String method,TransactionUserWrapper transactionUser){
		
		boolean rc = false;
		
		if (transactionUser.getState()== SipSessionImplementation.State.EARLY) {
			
			if (method.equals(RequestImpl.PRACK) || 
				method.equals(Request.CANCEL)||
				method.equals(RequestImpl.INFO)) {
				rc = true;
			}
			else if (method.equals(Request.BYE)){
				if(transactionUser.isProxying()){
					rc = true;
				}
				else if(transactionUser.isServerTransaction()){
						rc = true;
				}
			}
		}
		return rc;
	}

	 /**
     * Return true in case the method is allowed on Early dialog
     * @param method The method namw
     * @return true in case the the method is allowed on early dialog
     */
    public static boolean canSendOnDialog(String method, TransactionUserWrapper tUser) {
    	
    	boolean rc = false;
    	
    	switch (tUser.getState()) {
	
    	case EARLY:
			if (method.equals(RequestImpl.PRACK)|| 
				method.equals(RequestImpl.UPDATE)|| 
				method.equals(RequestImpl.INFO)) {
				rc = true;
			}
			// The caller's UA MAY send a BYE for either
			// confirmed or early dialogs, and the callee's UA MAY send a BYE on
			// confirmed dialogs, but MUST NOT send a BYE on early dialogs.
			else if (method.equals(Request.BYE)
					&& !tUser.isServerTransaction()) {
				rc = true;
			}
			break;
		
		case INITIAL:
			// ANAT: the Application cannot send reSUBSCRIBE before final response / Notify received, and dialog becomes to 
			// CONFIMRED state.
			if (!method.equals(RequestImpl.SUBSCRIBE)){
					rc = true;
			}
			break;
		case CONFIRMED:
			rc = true;
			break;

		default:
			break;
		}
    	
   		return rc;
	}
	
	/**
	 * Tests if the domain of the sipUri is numeric.
	 * @param sipUrlToSend
	 * @return
	 */
	public static boolean isNumericIP(SipURI sipUrlToSend) {
		 
		String host = sipUrlToSend.getHost();

		boolean isIP = false;
		
		if (host.indexOf(':') != -1) {
			//IPV6
			isIP = true;
		}		
		else{
			isIP = isNumIPV4Ip(host);
		}
		// Check if the string host contains : - it is IPV6 format.
		// Note: this is not testing the rules of the IPV6 - only format
		// definition.		
		return isIP;
	}

	
	/**
	 * Helper method that checks if this is an IP in IPV4 format.
	 * ddd.ddd.ddd.ddd
	 * @param host
	 * @return
	 */
	private static boolean isNumIPV4Ip(String host){
		int dotsNum = 0;
		boolean isIp = true;
		int digitsNum = 0;
		
		// Convert host to char array
		char [] hostChar = host.toCharArray();

		for (int i = 0; isIp && i < host.length() && digitsNum < 4; i++) {
			// '.'
			if (hostChar[i] == 46) {
				// If this is a . - check if number of dots is already > 2
				// or number digits until now is 0 - it is not correct IP
				if (dotsNum > 2 || digitsNum == 0) {
					isIp = false;
				}
				dotsNum++;
				// digitsNum calculated between 2 dots. Nullify it after
				// each found dot
				digitsNum = 0;
			} 
			else {
				// If it is not a number - it is not correct IP
				if ((int) hostChar[i] < 48 || (int) hostChar[i] > 57) {
					isIp = false;
				} 
				else {
					// If already find more than 2 digits - it is not correct IP
					if (digitsNum > 2) {
						isIp = false;
					} 
					else {
						digitsNum++;
					}
				}
			}
		}
		// If number of dots is different than 3 - it is not correct IP
		if(dotsNum != 3){
			isIp = false;	
		}
		return isIp;		
	}
	
	
	/**
	 * Helper method that sets the target Header in the request. This header
	 * contains the actual address where stack should send the request.
	 * 
	 * @param request
	 * @param targetAddressUri
	 *            The actual address where the request should be sent.
	 * 
	 */
	public static void setDestinationHeader(SipURL targetAddressUri,Message message)
		throws SipParseException
	{
		// specify transport and port number if those are not explicitly set by now
		String scheme = targetAddressUri.getScheme();
		if (scheme != null) {
			String transport = targetAddressUri.getTransport();
			int port = targetAddressUri.getPort();
			
			if (scheme.equals(SIP_SCHEME)) {
				if (transport == null) {
					targetAddressUri.setTransport(ListeningPoint.TRANSPORT_UDP);
					transport = ListeningPoint.TRANSPORT_UDP;
				}

				if (port < 0) {
					//in case that the transport is tls we should use the tls default transport
					if (transport.equalsIgnoreCase(TLS_TRANSPORT)){
						targetAddressUri.setPort(DEFAULT_TLS_PORT);
					}else{
						targetAddressUri.setPort(ListeningPoint.DEFAULT_PORT);
					}
				}
			}
			else if (scheme.equals(SIPS_SCHEME)) {
				// The IBM-Destination header is different than other headers
				// that contain a URI. It does not need to comply with the RFC, and its
				// transport parameter is used for specifying the transport protocol.
				// In other words, transport=tls is legal in the IBM-Destination header,
				// while deprecated in standard URIs.

				if (transport == null || !transport.equalsIgnoreCase(TLS_TRANSPORT)) {
					targetAddressUri.setTransport(TLS_TRANSPORT);
				}
				if (port < 0) {
					targetAddressUri.setPort(DEFAULT_TLS_PORT);
				}
			}
		}
		NameAddressHeader ibmDestination =
			(NameAddressHeader)message.getHeader(DESTINATION_URI, true);
		if (ibmDestination == null) {
            AddressFactory addressFactory = StackProperties.getInstance().getAddressFactory();
			NameAddress address = addressFactory.createNameAddress(targetAddressUri);
			ibmDestination = new GenericNameAddressHeaderImpl(DESTINATION_URI);
			ibmDestination.setNameAddress(address);
			message.setHeader(ibmDestination, true);
		}
		else {
			// change existing header
			NameAddress address = ibmDestination.getNameAddress();
			address.setAddress(targetAddressUri);
		}
	}
	
	/**
	 * creating the container-generated timeout response 
	 * for the cases where the server wasnt reached.
	 * 
	 * @param req - the original request
	 * @return the timeout response (408)
	 * @throws IllegalArgumentException
	 * @throws SipParseException
	 */
	public static IncomingSipServletResponse createResponse(int errorCode, SipServletRequestImpl req) throws IllegalArgumentException, SipParseException{
		//get the jain request and the message factory
        Request jainReq = req.getRequest();
        MessageFactory msgFactory = StackProperties.getInstance()
                .getMessageFactory();

        //Create a dummy Jain SIP Response from the factory
        Response jainRes = msgFactory
                .createResponse(SipServletResponse.SC_REQUEST_TIMEOUT,
                                jainReq);

        //add generated to-tag only if there is no tag already e.g. error response for initial request
        if (jainRes.getToHeader().getTag() == null || jainRes.getToHeader().getTag().length() == 0){
        	jainRes.getToHeader().setTag(String.valueOf( SIPStackUtil.generateTag()));
        }
        
        //this part is used for determining the ibm client address
        SipProvider sipProvider = req.getSipProvider();
    	ListeningPoint lp = null;
    	if (sipProvider != null){
    		lp = sipProvider.getListeningPoint();
    	}
		if (lp != null){
    		SIPStackUtil.addIbmClientAddressHeader(jainRes,lp); 
		}

        //Wrap the Jain Response in Sip Servlet Response
        IncomingSipServletResponse response = new IncomingSipServletResponse(
                jainRes, req.getTransactionId(), req.getSipProvider());

        SipUtil.setIntenalResponseFlag(response);

          //setting the request and tu on the response
        response.setRequest(req);
        response.setTransactionUser(req.getTransactionUser());

		return response;
	}

	/**
	 * Method which decides if this response can cause DerivedSession be created
	 * @param response
	 * @return
	 */
	public static boolean canCreateDerivedSession(SipServletResponseImpl response){
		
		if(response.getMethod().equals(RequestImpl.INVITE) || response.getMethod().equals(RequestImpl.SUBSCRIBE)){
			return true;
		}
		return false;
	}

	/**
	 * get the canonical form of the Uri without params and unescaped.
	 * 
	 * @param uri - original URI
	 * 
	 * @return the canonical form of the Uri without params and unescaped.
	 */
	public static String getCanonicalURI(URI uri){
		try{
			if (uri == null){
				return null;
			}
			if (uri instanceof SipURI){
				return getCanonicalURI((SipURI)uri);
			}
			if (uri instanceof TelURL){
				return getCanonicalURI((TelURL)uri);
			}
			return uri.toString();
		}catch (SipParseException e) {
			return null;
		}
	}

	/**
	 * get the canonical form of the Sip Uri without params and unescaped.
	 * 
	 * @param sipURI original sip URI
	 * 
	 * @return the canonical form of the Sip Uri without params and unescaped.
	 * 
	 * @throws SipParseException
	 */
	public static String getCanonicalURI(SipURI sipURI) throws SipParseException {
        CharsBuffer buffer = CharsBuffersPool.getBuffer();
		buffer.append(sipURI.getScheme());
		buffer.append(Separators.COLON);
		// User info.
		if(sipURI.getUser() != null){
			buffer.append(Coder.decode(sipURI.getUser()));
			if(sipURI.getUserPassword() != null){
				buffer.append(Separators.COLON);			
				buffer.append(Coder.decode(sipURI.getUserPassword()));
			}
			buffer.append(Separators.AT);
		}
		
		// Host and port.
		boolean ipv6 = sipURI.getHost().indexOf(':') != -1;
		if (ipv6) {
			buffer.append('[');
		}
		buffer.append(sipURI.getHost());
		if (ipv6) {
			buffer.append(']');
		}
		if(sipURI.getPort() != -1){
			buffer.append(Separators.COLON);
			buffer.append(sipURI.getPort());
		}
		
		String value = buffer.toString();
        CharsBuffersPool.putBufferBack(buffer);
        
        return value;
	}
	
	/**
	 * get the canonical form of the Tel URL without params.
	 *  
	 * @param telURL original Tel URL
	 * 
	 * @return the canonical form of the Tel URL
	 */
	public static String getCanonicalURI(TelURL telURL) {
        CharsBuffer buffer = CharsBuffersPool.getBuffer();
		buffer.append(telURL.getScheme());
		buffer.append(Separators.COLON);
		// User info.
		if(telURL.getPhoneNumber() != null){
			buffer.append(telURL.getPhoneNumber());
		}
		
		String value = buffer.toString();
        CharsBuffersPool.putBufferBack(buffer);
        
        return value;
	}
	
	/**
	 * a method that extract the dialog usage key from a dialog message.
	 * 
	 * @param sipMessage - the sip message from which we extract 
	 * 				the dialog usage key.
	 * 
	 * @return the dialog usage key.
	 */
	public static DialogUsageKey getDialogUsageKey(SipServletMessageImpl sipMessage) {
		//get the method
		String method = sipMessage.getMethod();
		
		//if this is not a method that creates dialogs, we need not continue
		method = getDialogRelatedMethod(method);
		if (method == null) {
			return null;
		}

		//prepare variable for secondary key, if such exists
		String usageSecondaryKey = null;
		
		//if this is a subscribe or refer message
		//we will need the event id, because there can be multiple
		//subscription over one single dialog (RFC 3265 section 3.1.2)
		if(method.equals(RequestImpl.SUBSCRIBE)|| method.equals(RequestImpl.REFER)) {
			ParametersHeader eventHeader = null;
			try {
				//get the 'Event' header
				eventHeader = (ParametersHeader)sipMessage.getMessage().getHeader(EVENT_HEADER, true);
				if (eventHeader == null) {
					if (sipMessage instanceof SipServletResponseImpl) { 
						//If this is a response, look for the Event header in the request
						SipServletRequestImpl req = (SipServletRequestImpl)((SipServletResponseImpl)sipMessage).getRequest();
						eventHeader = (ParametersHeader)req.getMessage().getHeader(EVENT_HEADER, true);
					}
				}
			} catch (Exception e) {
			}
			if (eventHeader != null) {
				//get the value of the 'id' parameter of the 'Event' header
				usageSecondaryKey = eventHeader.getParameter(EVENT_HEADER_ID_PARAM);
				if (usageSecondaryKey == null) {
					//If there's no id parameter, use the Event header itself as an identifier
					//of the specific subscription within a dialog.
					usageSecondaryKey = eventHeader.getValue();
				}
			} 
		}
		return DialogUsageKey.instance(method, usageSecondaryKey);
	}
	
	/**
	 * a method that determines if a certain response code should terminate
	 * the whole dialog (all usages) or if it applies just to the usage 
	 * it belongs to.  (see RFC 5057 section 5.1)
	 * 
	 * @param responseCode - the response code in question
	 * 
	 * @return true if all dialog usages should be terminated, othewise, false
	 */
	
	public static boolean shouldTerminateAllDialogUsages(int responseCode){
		switch (responseCode){
		case SipResponseCodes.CLIENT_NOT_FOUND:  
		case SipResponseCodes.CLIENT_GONE:
		case SipResponseCodes.CLIENT_UNSUPPORTED_URI_SCHEME:
		case SipResponseCodes.CLIENT_LOOP_DETECTED:         
		case SipResponseCodes.CLIENT_TOO_MANY_HOPS:         
		case SipResponseCodes.CLIENT_ADDRESS_INCOMPLETE:    
		case SipResponseCodes.CLIENT_AMBIGUOUS:             
		case SipResponseCodes.SERVER_BAD_GATEWAY:           
		case SipResponseCodes.GLOBAL_DOES_NOT_EXIST_ANYWHERE:
			return true;
		default:
			return false;
		}
	}

	/**
	 * check if the message contained a response code that should terminate all 
	 * message usages, or just this one. 
	 *  
	 * @param sipMessage - the message in question
	 * 
	 * @return true if all dialog usages should be terminated, othewise, false
	 */
	public static boolean shouldTerminateAllDialogUsages(SipServletMessageImpl sipMessage){
		//if this came without a message, it means it was done by app initiative
		//this means invalidation, therefore we should return true
		if (sipMessage == null){
			return true;
		}
		//we should check if this is a response
		//if it is we need to check status code.
		if (sipMessage instanceof SipServletResponse){
			SipServletResponse response = (SipServletResponse)sipMessage;
			int statusCode = response.getStatus();
			return shouldTerminateAllDialogUsages(statusCode);
		}
		return false;
	}

	/**
	 * This method determines if the given request should terminate
	 * a dialog usage (see RFC 5057 section 4).
	 * 
	 * @param msg the request in question
	 * 
	 * @return <tt>true</tt> - if the given request should terminate a dialog usage, 
	 * 		   <tt>false</tt> - otherwise.
	 * 
	 * @throws IllegalArgumentException
	 * @see {@link SipServletRequest}
	 */
	public static boolean isUsageTerminatingRequest(SipServletRequest msg) throws IllegalArgumentException {
		if (msg.getMethod().equals((Request.BYE))) {
			return true;
		}
		if (msg.getMethod().equals((RequestImpl.NOTIFY))) {
			String stateHeader = msg.getHeader(SUBSCRIPTION_STATE);
			if (stateHeader != null) {
				if (stateHeader.indexOf(TERMINATED_STATE) != -1) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * This method determines if the given response code should terminate
	 * a/all dialog usage/s (see RFC 5057 section 5.1).
	 * 
	 * @param responseCode the response code in question
	 * 
	 * @return <tt>true</tt> - if a/all dialog usage/s should be terminated, 
	 * 		   <tt>false</tt> - otherwise.
	 */
	public static boolean isUsageTerminatingResponse(int responseCode,SipServletResponse response) {
		
		if (shouldTerminateAllDialogUsages(responseCode)) {
			return true;
		}
		switch (responseCode) {
		case SipResponseCodes.CLIENT_METHOD_NOT_ALLOWED:
		case SipResponseCodes.SERVER_NOT_IMPLEMENTED:
			if(response.getMethod().equals("INFO")){
				return false;
			}
			return true;
	
		case SipResponseCodes.CLIENT_TEMPORARILY_UNAVAILABLE:
		case SipResponseCodes.CLIENT_CALL_OR_TRANSACTION_DOES_NOT_EXIST:
			return true;
		default:
			return false;
		}
	}


	/**
	 * The method looks for sip application session according encoded uri specification.
	 *
	 * @param request - incoming request;
	 */
	public static SipApplicationSessionImpl getApplicationSessionAccordingToEncodedUri(SipServletRequest request){
		
		SipApplicationSessionImpl appSession = null;
        String appSessionId = null;
        URI uri;

        // first look for the encoded URI in the Route header

        try {
        	Address route = request.getAddressHeader(RouteHeader.name);
        	if (route != null) {
        		uri = route.getURI();
                if (uri.isSipURI()){
                    appSessionId = ((SipURI)uri)
                        .getParameter(SipApplicationSessionImpl.ENCODED_APP_SESSION_ID);
                }    
        	}
		}
		catch (ServletParseException e) {
		}

    	// then try to look for encoded URI in RequestURI
		if (null == appSessionId){
	        uri = request.getRequestURI();
	        
	        if(uri.isSipURI()){
	        	//only Sip and Sips URIs will can contain encoded session Ids.
	            appSessionId = ((SipURI) uri)
	                .getParameter(SipApplicationSessionImpl.ENCODED_APP_SESSION_ID);
	        }    
		}

        if (null == appSessionId){
        	// The JSR 116 wasn't clear about where to look for encoded URI
        	// in To or in RequestURI. This is more logically to look for
        	// it in RequestUri. But the following code (look for encoded URI
        	// in ToHeader) will be remain for backward compatibility.
	        uri = request.getTo().getURI();
	        if (uri.isSipURI()) {
	            //only Sip and Sips URIs will can contain encoded session Ids.
	            appSessionId = ((SipURI) uri)
	                .getParameter(SipApplicationSessionImpl.ENCODED_APP_SESSION_ID);
	        }
        }

        
        if (null != appSessionId) {
            appSession = SipApplicationSessionImpl.getAppSession(appSessionId);
        }
        
        return appSession;
	}
	
    /**
     * get key base target session id
     * 
     * @param appDesc application descriptor
     * @param request Sip servlet request
     * @return the sip application id associated with this key base targeting key, null if there isn't any.
     */
	public static String getKeyBaseTargetingKey(SipAppDesc appDesc, SipServletRequestImpl request) {
		String returnValue = null;
		
		if (!appDesc.hasApplicationKeyMethod())
			return null;

		// call annotated method
		ContextEstablisher contextEstablisher = appDesc.getContextEstablisher();
		
		ClassLoader currentThreadClassLoader = Thread.currentThread().getContextClassLoader();
		
		try {
			contextEstablisher.establishContext();
			
			// calling to method defined by @SipApplicationKey annotation
			returnValue =  appDesc.getApplicationKeyHelper().generateApplicationKey(request);

		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			if (contextEstablisher != null && currentThreadClassLoader != null) {
				contextEstablisher.removeContext(currentThreadClassLoader);
			}
			
		}
		

		if (returnValue == null || returnValue == "") {
			return null;
		}

		return SipUtil.composeSessionKeyBaseKey(appDesc.getApplicationName(), returnValue);
	}
	
	public static final String composeSessionKeyBaseKey(String appName, String appKey) {
		StringBuilder sb = new StringBuilder(appName.length() + appKey.length() + 1);

		
		sb.append(appName)
		  .append("_")
		  .append(appKey);
		
		return sb.toString();		
	}
	
	/**
	 * create the route header for the ROUTE BACK mechanism (JSR289 15.14.1)
	 * the stateInfo is saved locally on a new SipApplicationSession, this SAS is
	 * encoded in the route header so the request will be handled by the correct cluster member
	 * that has the stateInfo information. 
	 * 
	 * @param request - initial request 
	 * @param stateInfo - the state info object from the application router, 
	 *                    will be used by the AR when the requests will be routed back to the container
	 * @return - the route header URI
	 */
	public static final SipURI creatLocalRouteHeader(SipServletRequestImpl request, Serializable stateInfo){
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(SipUtil.class, "creatLocalRouteHeader", new Object[] {request, stateInfo});
		}
		
		SipProvider provider = request.getSipProvider();
		ListeningPoint lp = provider.getListeningPoint();
		String host = null;
		int port = -1;
		String transport = null;
	    
		host = lp.getSentBy();
		port = lp.getPort();

		// Take transport from listening point to make sure they match
	    transport = ((ListeningPointImpl) lp).isSecure() ? "TLS" : lp.getTransport();

	    SipURI routeUri = null;
		try {
			SipURL url = AddressFactoryImpl.createSipURL(null, null, null, host, port, null, null, null, transport);
			routeUri = new SipURIImpl(url);
			
			//set the route header parameters
			routeUri.setParameter(SipURIImpl.LR, "");
			routeUri.setParameter(IBM_ROUTE_BACK_PARAM, "");
			
			if (stateInfo != null){
				SipApplicationSessionImpl sas = (SipApplicationSessionImpl) SipServletsFactoryImpl.getInstance().createApplicationSession();
				
				//we do not need this SAS to use the invalidate when ready mechanism, we will invalidate it when the request is routed back to the container
				sas.setInvalidateWhenReady(false);
				
				//add the encoded uri parameter so the request that is routed back will be routed to this specific container, this is because 
				//this container has the SAS with the stateInfo information
				routeUri.setParameter(SipApplicationSessionImpl.ENCODED_APP_SESSION_ID, sas.getId());
				
				//put the infoState as a sip app session attribute for future use when the request is routed back to this container
				sas.setAttribute(IBM_STATE_INFO_ATTR, stateInfo);
			}
		} catch (SipParseException e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(SipUtil.class, "creatLocalRouteHeader", "failed to create the route back route header", e);
    		}
		}
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(SipUtil.class, "creatLocalRouteHeader", routeUri);
		}
	    
	    return routeUri;
	}
	
    /**
     * Checks whether two SipURLs use the same transport
     * @param url1
     * @param url2
     */
    public static boolean isSameTransport(SipURI firstURI, SipURI secondURI)
    {
    	String firstURLTransport = getTransport(firstURI);
    	String secondURLTransport = getTransport(secondURI);
    	return firstURLTransport.equalsIgnoreCase(secondURLTransport);
    }

    /**
     * Checks whether two SIP URLs have the same host 
     * @param url1
     * @param url2
     */
    public static boolean isSameHost(SipURI url1, SipURI url2)
    {
    	boolean retValue = true;
    	if (url1 != null && url2 != null) {
        	String host1 = url1.getHost();
        	String host2 = url2.getHost();
        	
        	retValue = host1.equalsIgnoreCase(host2);
    	}
    	return retValue;
    }
    
    /**
     * Returns the actual transport a SipURL is using
     * @param url
     * @return
     */
    public static String getTransport(SipURI uri) {   	
    	//if sips scheme the transport is tls
    	if("sips".equalsIgnoreCase(uri.getScheme())) {
    		return "tls";
    	}
    	else if (uri.getTransportParam() != null) {
    		return uri.getTransportParam();
    	}
    	//default sip transport is udp
    	else return "udp";
    }
    /**
     * Return true if To and From tag persists in the request.
     * @param req
     * @return
     */
    public static boolean hasBothTags(Request req){
    	
    	if(req.getToHeader().getTag() == null){
    		return false;
    	}
    	if(req.getFromHeader().getTag() == null){
    		return false;
    	}
    	return true;
    }
    
    
    /**
     * The function parses an array of reason headers in their string representation and returns a collection of ReasonHeaders
     * @param reasons the reasons to be parsed
     * @return a list of reason headers that can be used in an outgoing message
     * @throws SipParseException if there is more than one reason with the same protocol
     */
    public static List<ReasonHeaderImpl> parseReasons(String [] reasons) throws SipParseException {
    	
    	HashSet<String> uniqueProtocols = new HashSet<String>();
    	ArrayList<ReasonHeaderImpl> reasonHeaders = new ArrayList<ReasonHeaderImpl>();
    	for (String reason : reasons){
    		// each reason header may contain multiple values separated by ','
    		// moreReason indicates whether there's more reason to parse
    		boolean moreReason = true;
			while(moreReason){	
				// reasonHeaderLength holds the length of each value inside a "Reason:" header (until the ',')
				int reasonHeaderLength = 0;
				moreReason = false;
				int cause = 0;
				String text = "";
				// Split "Reason:" header by ';', as each parameter is separated by ';'
				String [] reasonParam = reason.split(";");
				
				// parse the protocol parameter
				String protocol = reasonParam[0].trim();
				reasonHeaderLength += reasonParam[0].length()+1; // +1 for the ';'
				
				for(int j=1 ; j < reasonParam.length ; j++){
					String [] keyVal = reasonParam[j].split("=");
					// parse the "cause" parameter if exists
					if(keyVal[0].trim().equals( "cause")){
						cause = Integer.parseInt(keyVal[1].trim());
						reasonHeaderLength += keyVal[0].length() + keyVal[1].length()+2;// +2 for the '=' and the ';'
					}
					else {
						//parse the "text" parameter
						if(keyVal[0].trim().equals( "text")){
							String restReason = keyVal[1].trim();
							int indexOfSecondQute = restReason.indexOf('\"', restReason.indexOf('\"')+1);
							text = restReason.substring(1, indexOfSecondQute);
							reasonHeaderLength += keyVal[0].length() + indexOfSecondQute +1; // for the '='
							// remove the parsed reason value from the reason string, and checks whether there's an additional value
							// (if there's a subsequent ',')
							reason = reason.substring(reasonHeaderLength);
							if (reason.indexOf(',') > 0) {
								reason = reason.substring(reason.indexOf(',') + 1).trim();
								moreReason = true;
								break;
							}
						}
					}
				}
				if(uniqueProtocols.contains(protocol)){
					throw new SipParseException(REASON_PROTOCOL_MULTIPLE, reason);
				}
				else{
					reasonHeaders.add(new ReasonHeaderImpl(protocol, cause, text));
				}
			}
		}
    	
		return reasonHeaders;
	
    }

    /**
     * Sets the INTERNAL_RESPONSE_ATTR attribute on the response.
     * @return
     */
    public static void setIntenalResponseFlag(SipServletResponse resp) {
    	// Check if we need to add property about internal response.
    	boolean markInternalMessage = PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.MARK_INTERNAL_ERROR_RESPONSE);
    	
    	
    	if(markInternalMessage == true){
    		// mark response as internal response.
        	if(resp != null){
        		resp.setAttribute(INTERNAL_RESPONSE_ATTR, Boolean.TRUE);
        	}
    	}
    	
    	if (resp != null) {
    		((IncomingSipServletResponse)resp).setInternal(true);
    	}
    }

}