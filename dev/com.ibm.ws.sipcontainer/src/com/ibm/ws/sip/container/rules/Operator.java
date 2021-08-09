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
package com.ibm.ws.sip.container.rules;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.TelURL;
import javax.servlet.sip.URI;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;

/**
 * @author Amir Perlman, Jun 25, 2003
 * 
 * Base class for logical operator.
 */
public abstract class Operator implements Condition
{
    /**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(Operator.class);

    private static final String PHONE = "phone";
    private static final String SCHEME_SIP = "sip";
    private static final String SCHEME_TEL = "tel";
    private static final String URI = ".uri";
    private static final String SCHEME = ".scheme";
    private static final String USER = ".user";
    private static final String HOST = ".host";
    private static final String PORT = ".port";
    private static final String TEL = ".tel";
    private static final String PARAM = ".param";
    private static final String DISPLAY_NAME = ".display-name";
    private static final String REQUEST_TO = "request.to";
    private static final String REQUEST_FROM = "request.from";
    private static final String REQUEST_URI = "request.uri";
    private static final String REQUEST_METHOD = "request.method";
    private static final String REQUEST_HEADER_PREFIX = "request.header.";    
    private static final String REQUEST_ADDRESS_PREFIX = "request.address.";
    
	/**
	 * Argument that its contents is checked. 
	 */
	private final String m_var;
	
	/**
	 * constructor
	 * @param var Argument that its contents is checked
	 */
	protected Operator(String var) {
		m_var = var;
	}
	
	/**
	 * @return Argument that its contents is checked
	 */
	protected String getVariable() {
		return m_var;
	}

    /**
     * Helper functions. Extracts the variable's value from the given request.
     * 
     * @param varParam
     *            Indicate the variable type description.
     * @param req
     *            The request that the value is extracted from.
     * @return The variable's value in a general Object format if available
     *         otherwise null. The returned value can be of any type specificly
     *         suitable for the requested parameter.
     */
    protected Object getObjectForVar(String varParam, SipServletRequest req)
    {
        if (c_logger.isTraceEntryExitEnabled())
        {
            Object[] params =
            { varParam, req.getMethod() };
            c_logger.traceEntry(this, "getObjectForVar", params);
        }

        Object rValue = null;
        //Comparison is not case sensative. 
        String var = varParam.toLowerCase();

        if (var.equals(REQUEST_METHOD))
        {
            rValue = req.getMethod();
        }
        else if (var.startsWith(REQUEST_URI))
        {
            String suffix = var.substring(REQUEST_URI.length());
            rValue = getObjectForVar(req.getRequestURI(), suffix, varParam);
        }
        else if (var.startsWith(REQUEST_FROM))
        {
            String suffix = var.substring(REQUEST_FROM.length());
            rValue = getObjectForVar(req.getFrom(), suffix, varParam);
        }
        else if (var.startsWith(REQUEST_TO))
        {
            String suffix = var.substring(REQUEST_TO.length());
            rValue = getObjectForVar(req.getTo(), suffix, varParam);
        }
        else if (var.startsWith(REQUEST_ADDRESS_PREFIX))
        {
        	// proprietary extension to match address headers
        	// that are not defined in the jsr
        	// format: request.address.<top/any>.<header>[.uri-component]
        	boolean top; // true = top, false = any
        	String headerName;
        	String componentName;
        	String suffix = var.substring(REQUEST_ADDRESS_PREFIX.length());
        	int dot = suffix.indexOf('.');
        	int end = suffix.length();
        	if (dot < 1) {
                if (c_logger.isTraceDebugEnabled()) {
                    c_logger.traceDebug("error in servlet descriptor - "
                    	+ "no scope specified in variable [" + var + ']');
                }
                return null;
        	}
    		String scopeSuffix = suffix.substring(0, dot);
    		if (scopeSuffix.equalsIgnoreCase("top")) {
    			top = true;
    		}
    		else if (scopeSuffix.equalsIgnoreCase("any")) {
    			top = false;
    		}
    		else {
                if (c_logger.isTraceDebugEnabled()) {
                    c_logger.traceDebug("error in servlet descriptor - "
                    	+ "unknown scope specified in variable [" + var + ']');
                }
                return null;
    		}
        	suffix = suffix.substring(dot+1, end);
        	dot = suffix.indexOf('.');
        	end = suffix.length();
        	if (dot < 1) {
        		// no component name - match full address
        		headerName = suffix;
        		componentName = "";
        	}
        	else {
        		headerName = suffix.substring(0, dot);
        		componentName = suffix.substring(dot, end);
        	}
            try {
	        	if (top) {
	        		Address header = req.getAddressHeader(headerName);
	        		if (header == null) {
	        			rValue = null;
	        		}
	        		else {
	        			rValue = getObjectForVar(header, componentName, varParam);
	        		}
	        	}
	        	else {
	        		// match any header
	            	LinkedList addresses;
	            	ListIterator iHeader = req.getAddressHeaders(headerName);
	            	if (iHeader.hasNext()) {
		            	addresses = new LinkedList();
		                while (iHeader.hasNext()) {
		                	Address header = (Address)iHeader.next();
		                	Object address = getObjectForVar(header, componentName, varParam);
		                	addresses.add(address);
		                }
	            	}
	            	else {
	            		addresses = null;
	            	}
	                rValue = addresses;
	        	}
            }
            catch (ServletParseException e) {
                if (c_logger.isTraceDebugEnabled()) {
                    c_logger.traceDebug(this, "getObjectForVar", "ServletParseException", e);
                }
            }
        }
        else if (var.startsWith(REQUEST_HEADER_PREFIX))
        {
        	// proprietary extension to match headers
        	// that are not defined in the jsr.
            // evaluation is done on the whole header as a string
            String headerSuffix = var.substring(REQUEST_HEADER_PREFIX.length());
            rValue = req.getHeader(headerSuffix);
        }                
        else
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args =
                { var };
                c_logger.error("error.invalid.operator.var",
                        Situation.SITUATION_REQUEST, args);
            }
        }

        if (c_logger.isTraceDebugEnabled())
        {
            StringBuffer buffer = new StringBuffer(16);
            buffer.append("Var: ");
            buffer.append(var);
            buffer.append(", Value:");
            buffer.append(rValue);

            c_logger.traceDebug(this, "getObjectForVar", buffer.toString());
        }

        return rValue;
    }

    /**
     * Helper functions. Extracts the variable's value from the given address
     * header of the request (either from or to).
     * 
     * @param suffix
     *            Indicate the suffix variable type description.
     * @param address
     *            The address part of the request that the value is extracted
     *            from.
     * @param fullVar
     *            Full description of the variable. Needed for logging.
     * @return The variable's value in a general Object format if available
     *         otherwise null. The returned value can be of any type specificly
     *         suitable for the requested parameter.
     */
    private Object getObjectForVar(Address address, String suffix,
            String fullVar)
    {
        Object rValue = null;
        if (suffix.length() == 0)
        {
            rValue = address.toString();
        }
        else if (suffix.equals(DISPLAY_NAME))
        {
            rValue = address.getDisplayName();
        }
        else if (suffix.startsWith(URI))
        {
            String suffix2 = suffix.substring(URI.length());
            rValue = getObjectForVar(address.getURI(), suffix2, fullVar);
        }
        else
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args =
                { fullVar };
                c_logger.error("error.invalid.operator.var",
                        Situation.SITUATION_REQUEST, args);
            }
        }

        return rValue;
    }

    /**
     * Helper functions. Extracts the variable's value from the given uri of the
     * request (either from or to or request uri).
     * 
     * @param suffix
     *            Indicate the suffix variable type description.
     * @param address
     *            The address part of the request that the value is extracted
     *            from.
     * @param fullVar
     *            Full description of the variable. Needed for logging.
     * @return The variable's value in a general Object format if available
     *         otherwise null. The returned value can be of any type specificly
     *         suitable for the requested parameter.
     */
    private Object getObjectForVar(URI requestURI, String suffix, String fullVar)
    {
        Object rValue = null;

        if (suffix.length() == 0)
        {
            rValue = requestURI.toString();
        }
        else if (suffix.equals(SCHEME))
        {
        	// According to JSR 116, section 11.2
        	//  for SipURL: scheme: a literal string - either "sip" or "sips"
        	//  for TelURL: scheme: always the literal string "tel"
        	// So I convert it to lower case so it will always be sip, sips or tel
            rValue = requestURI.getScheme().toLowerCase();
        }
        else if (suffix.equals(USER))
        {
            if (requestURI.isSipURI())
            {
                rValue = ((SipURI) requestURI).getUser();
            }
        }
        else if (suffix.equals(HOST))
        {
            if (requestURI.isSipURI())
            {
                rValue = ((SipURI) requestURI).getHost();
            }
        }
        else if (suffix.equals(PORT))
        {
            rValue = getPort(requestURI);
        }
        else if (suffix.equals(TEL))
        {
            rValue = getTelephoneNumber(requestURI);
        }
        else if (suffix.startsWith(PARAM))
        {
            String param = suffix.substring(PARAM.length() + 1);
            rValue = getParam(requestURI, param);
        }
        else
        {
            if (c_logger.isErrorEnabled())
            {
                Object[] args =
                { fullVar };
                c_logger.error("error.invalid.operator.var",
                        Situation.SITUATION_REQUEST, args);
            }
        }

        return rValue;
    }

    /**
     * Extracts the param from the specified URI
     * 
     * @param uri
     * @return The param value if available otherwise null
     */
    private String getParam(URI uri, String param)
    {
        String rValue = null;
        if (uri.isSipURI())
        {
            rValue = ((SipURI) uri).getParameter(param);
        }
        else if (uri.getScheme().equalsIgnoreCase(SCHEME_TEL))
        {
            rValue = ((TelURL) uri).getParameter(param);
        }

        return rValue;
    }

    /**
     * Helper method. Gets the port number from the specified URI. For Sip uri
     * if the port is not specified then the default for sip (5060) and sips
     * (5061) are used.
     * 
     * @param uri
     * @return The port number if available otherwise null.
     */
    private String getPort(URI uri)
    {
        String rValue = null;
        if (uri.isSipURI())
        {
            int port = ((SipURI) uri).getPort();

            if (port < 0)
            {
                rValue = (uri.getScheme().equalsIgnoreCase(SCHEME_SIP) ? "5060"
                        : "5061");
            }
            else
            {
                rValue = Integer.toString(port);
            }
        }

        return rValue;
    }

    /**
     * Helper function. Extract Telphone from either a Sip Uri or a Tel Uri.
     * 
     * @param uri
     * @return Phone Comparison class for comparing tel numbers correctly
     */
    private PhoneComparison getTelephoneNumber(URI uri)
    {
        if (uri.isSipURI())
        {
            SipURI sipURI = (SipURI) uri;
            if (PHONE.equals(sipURI.getUserParam()))
            {
                try{
                    return new PhoneComparison(sipURI.getUser());
                }
                catch (IllegalArgumentException e){
                   return null; 
                }
            }
        }
        else if (uri instanceof TelURL)
        {
            return new PhoneComparison(((TelURL) uri).getPhoneNumber());
        }

        return null;
    }

    /**
     * @see com.ibm.ws.sip.container.rules.Condition#evaluate(javax.servlet.sip.SipServletRequest)
     */
    public boolean evaluate(SipServletRequest request) {
        //Value returned is an Object in order to enable specific handling
        //for different types other than String. Var types will need to overide
        //the default equals behavior and provide adequate comparison function. 
        Object value = getObjectForVar(m_var, request);
        return evaluate(value); 
    }

    /**
     * evaluates an element within a request
     * @param value the element to evaluate
     * @return true if evaluation passed, false if does not match
     */
    private boolean evaluate(Object value) {
        if (value == null) {
			if (c_logger.isTraceDebugEnabled()) {
            	c_logger.traceDebug(
            		this,
            		"evaluate" ,
					"null value evaluated to false");
            }
        	return false;
        }
        if (value instanceof Collection) {
            // evaluate a collection of values.
            // return true if any value matches
        	Collection values = (Collection)value;
        	Iterator i = values.iterator();

        	while (i.hasNext()) {
        		Object o = i.next();
        		if (evaluate(o)) {
        			// one positive evaluation is all we need
        			return true;
        		}
        	}
        	return false;
        }
        if (value instanceof PhoneComparison) {
        	PhoneComparison phone = (PhoneComparison)value;
            return evaluate(phone);
        }
       	String string = value.toString();
        return evaluate(string);
    }

    /**
     * evaluates the operator as a string
     * @param value the string value to evaluate
     */
    protected abstract boolean evaluate(String value);

    /**
     * evaluates the operator as a phone number
     * @param value the phone number value to evaluate
     */
    protected abstract boolean evaluate(PhoneComparison value);
}
