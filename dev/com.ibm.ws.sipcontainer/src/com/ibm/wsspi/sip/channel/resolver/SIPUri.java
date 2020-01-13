/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.sip.channel.resolver;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * @author mjohnson
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class SIPUri
{
	/**
	 * RAS Trace Component.
	 */
	/*static final TraceComponent	tc				= Tr
														.register(
																SIPUri.class,
																SIPLogKeys.TR_GROUP,
																SIPLogKeys.TR_MSGS);
	*//**
	 * RAS Trace Component.
	 */
	static final TraceComponent	tc				= Tr
														.register(
																SIPUri.class);
	
	   static {
	        if (tc.isDebugEnabled()) {
	            Tr.debug(tc, "version : ", "1.14 ");
	        }
	   }
	   
	public static final String SIP_SCHEME = "sip:";
	public static final String SIPS_SCHEME = "sips:";
	
	private		String	URI = null;
	private		String	scheme = null;
	private		String	userInfo = null;
	private		String	host = null;
	private		String	port = null;
	private     String  mAddr = null;
	private     String  transport = null;
	protected	String 	additionalParameters = null;
	
	boolean	changed = false;
	boolean	parsed = false;
	private boolean ipV6addr = false;
	
	/**
	 */
	public static SIPUri createSIPUri(String originalUri)
	{
		SIPUri sipUri = new SIPUri();
		sipUri.setURI(originalUri);

		return sipUri;
	}
	
    public boolean equals( Object o ) {
    	if (o == null)
    		return (false);
    	
    	if(this == o) {  // Step 1: Perform an == test
    		return (true);
    	}
    	
    	if(!(o instanceof SIPUri)) {  // Step
    		return (false);
    	}
    	
    	parseUri();
    	SIPUri uri = (SIPUri) o; //

    	//	Check for matching scheme
        if (scheme == null){
        	if (uri.getScheme() != null){
        		return (false);
        	}
        }
        else if (uri.getScheme() == null){
        	return (false);
        }
        else if(uri.getScheme().equals(scheme) == false){
        	return (false);
        }
        
    	//	Check for matching host
        if (host == null){
        	if (uri.getHost() != null){
        		return (false);
        	}
        }
        else if (uri.getHost() == null){
        	return (false);
        }
        else if(uri.getHost().equals(host) == false){
        	return (false);
        }
        
    	//	Check for matching port
        if (port == null){
        	if (uri.getPort() != null){
        		return (false);
        	}
        }
        else if (uri.getPort() == null){
        	return (false);
        }
        else if(uri.getPort().equals(port) == false){
        	return (false);
        }
        		
    	//	Check for matching mAddr
        if (mAddr == null){
        	if (uri.getMaddr() != null){
        		return (false);
        	}
        }
        else if (uri.getMaddr() == null){
        	return (false);
        }
        else if(uri.getMaddr().equals(mAddr) == false){
        	return (false);
        }

    	//	Check for matching transport
        if (transport == null){
        	if (uri.getTransport() != null){
        		return (false);
        	}
        }
        else if (uri.getTransport() == null){
        	return (false);
        }
        else if(uri.getTransport().equals(transport) == false){
        	return (false);
        }
        
        return (true);
    }

    public int hashCode()
    {
    	int hashCode = 0;
    	
    	if (scheme != null)
    		hashCode += scheme.hashCode();
    	
    	if (host != null)
    		hashCode += host.hashCode();
    	
    	if (port != null)
    		hashCode += port.hashCode();
    	
    	if (mAddr != null)
    		hashCode += mAddr.hashCode();

    	if (transport != null)
    		hashCode += transport.hashCode();

    	return (hashCode);
    }

	public String getURI()
	{
		if (changed == true)
		{
			changed = false;
			
			URI = scheme;
			if (userInfo != null)
				URI = URI + userInfo + "@";
			URI = URI + host;
			if (port != null)
				URI = URI + ":" + port;
			URI = URI + additionalParameters;
		}
		return URI;
	}
	
	/*
	 * Note that parsing is performed in a lazy fashion for performance reasons.
	 */
	public void  setURI(String	uri)
	{
		URI = uri;
		changed = false;
		parsed = false;
		
		//	Initialize all the local instance variables to null to reflect the unparsed state.
		scheme = null;
		userInfo = null;
		host = null;
		port = null;
		mAddr = null;
		transport = null;
		additionalParameters = null;
		
	}
	
	protected void parseUri()
	{
		if ((parsed == false) && (URI != null))
		{
			parsed = true;
			String	tempString = URI;
	
			//
			// Look for the < character
			//
			int index = tempString.indexOf("<");
			if (index != -1) {
				tempString = tempString.substring(index + 1);
			}
			/*
			 * Grab the sip scheme
			 */			
			index = tempString.indexOf(SIP_SCHEME);
			if (index == -1)
			{
				index = tempString.indexOf(SIPS_SCHEME);
				if (index == -1)
					scheme = new String(tempString.substring(0,tempString.indexOf(":")+1));
				else
					scheme = SIPS_SCHEME;
			}
			else
			{
				scheme = SIP_SCHEME;
			}
			tempString = tempString.substring((scheme.length()));
	
			/*
			 * Grab the user info
			 */
			index = tempString.indexOf("@");
			if (index != -1)
			{
				userInfo = tempString.substring(0, index);
				tempString = tempString.substring(index+1);
			}
	
			/*
			 * Grab the host name
			 */
			String	hostPort = null;			
			index = tempString.indexOf(";");
			if (index != -1)
				hostPort = tempString.substring(0, index);
			else
			{
				hostPort = tempString;
				index = tempString.length();
			}
			//
			// Check to see if it is an IPv6 Address/port combo first.
			// sip:[2001:db8::10]:5070
			//
			int leftSquareBracketIndex = hostPort.indexOf("[");
			if (leftSquareBracketIndex != -1) {
				int rightSquareBracketIndex = hostPort.indexOf("]");
				if (rightSquareBracketIndex != -1) {
					ipV6addr = true;
					host = hostPort.substring(leftSquareBracketIndex+1, rightSquareBracketIndex);
					int colonIndex = hostPort.indexOf(":",rightSquareBracketIndex);
					if (colonIndex != -1) {
						port = hostPort.substring(colonIndex+1, index);
					}
					else {
						port = new String("5060");
					}
				}
				else {
					//
					// What to do here...
					//
				}
			}
			else {			
				int colonIndex = hostPort.indexOf(":");
				int bracketIndex = hostPort.indexOf(">");
				int semiIndex = hostPort.indexOf(";");
	
				if (bracketIndex != -1) {
					index = bracketIndex;
				}
				if (semiIndex != -1 && semiIndex < bracketIndex) {
					index = semiIndex;
				}
				if (colonIndex != -1)
				{
					host = hostPort.substring(0, colonIndex);
					port = hostPort.substring(colonIndex+1, index);
				}
				else
				{
					host = hostPort.substring(0, index);
					port = new String("5060");
				}
			}
			tempString = tempString.substring(hostPort.length());
				
			additionalParameters = tempString;
			
			parseAdditionalParameters(additionalParameters);
		}
	}

	/**
	 * @return
	 */
	private void parseAdditionalParameters(String additionalParameters)
	{
		// RWH DCUT LIDB4564-02
		if (additionalParameters.length() > 1)
		{
			try
			{
				StringTokenizer tokenizer = new StringTokenizer(additionalParameters,";=");
				
				while (true)
				{
					String token = tokenizer.nextToken();
					
					if (token.compareToIgnoreCase("maddr") == 0)
						mAddr = tokenizer.nextToken();
					else if (token.compareToIgnoreCase("transport") == 0)
						transport = tokenizer.nextToken();
					else
						tokenizer.nextToken(); //	Skip the next element for now
				}
			}
			catch (NoSuchElementException e)
			{
				// This is a no-op. Just mean we are at the end of the tokens.
			}
		}
	}

	
	/**
	 * @return
	 */
	public String getHost() 
	{
		parseUri();
		return host;
	}

	/**
	 * @return
	 */
	public String getPort() 
	{
		parseUri();
		return port;
	}

	/**
	 * @return
	 */
	public String getScheme() 
	{
		parseUri();
		return scheme;
	}

	/**
	 * @return
	 */
	public String getUserInfo() 
	{
		parseUri();
		return userInfo;
	}

	/**
	 * @param string
	 */
	public void setHost(String string) 
	{
		parseUri();
		changed = true;
		host = string;
	}

	/**
	 * @param string
	 */
	public void setPort(String string) 
	{
		parseUri();
		changed = true;
		port = string;
	}

	/**
	 * @param string
	 */
	public void setScheme(String string) 
	{
		parseUri();
		changed = true;
		scheme = string;
	}

	/**
	 * @param string
	 */
	public void setUserInfo(String string) 
	{
		parseUri();
		changed = true;
		userInfo = string;
	}
	
	/**
	 * @return
	 */
	public String getBaseSIPUri() 
	{
		parseUri();

		String baseUri = new String();
		
		baseUri = scheme;
		if (userInfo != null)
			baseUri = baseUri + userInfo + "@";
		
		baseUri += getHostnamePortCombo();
		return (baseUri);
	}
	
	/**
	 * @return
	 */
	public String getHostnamePortCombo() 
	{
		parseUri();
		String hostnamePort = new String();
		
		hostnamePort = host;
		
		if (port != null)
			hostnamePort = hostnamePort + ":" + port;
		
		if (ipV6addr) {
			hostnamePort = "[" + host + "]";
		}
		else {
			hostnamePort =  host;
		}
		
		if (port != null)
			hostnamePort = hostnamePort + ":" + port;				
		
		return (hostnamePort);
	}
	
	
	/**
	 * @return
	 */
	public String getBaseSIPUriWithoutSchema() 
	{
		parseUri();
		String baseUri = new String();
		
		if (userInfo != null)
			baseUri = baseUri + userInfo + "@";
		baseUri += getHostnamePortCombo();
		return (baseUri);
	}
	
	/**
	 * @return
	 */
	public String getAdditionalParms() 
	{
		parseUri();
		return additionalParameters;
	}

	/**
	 * @param string
	 */
	public void setAdditionalParms(String string) 
	{
		parseUri();
		changed = true;
		additionalParameters = string;
	}
	
	public void setPortInt (int p){
		parseUri();
		changed = true;
		port = Integer.toString(p);
	}
	
	public int getPortInt(){
		parseUri();
		if (port != null){
		return Integer.parseInt(port);
		}
		else {
			return -1;
		}
	}
	
	public void setTransport(String s){
		parseUri();
		changed = true;
		transport = s;
	}
	
	public String getTransport(){
		parseUri();
		return transport;
	}
	
	public void setMaddr(String s){
		parseUri();
		changed = true;
		mAddr = s;
	}

	public String getMaddr(){
		parseUri();
		return mAddr;
	}
}
