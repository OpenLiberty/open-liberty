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
package com.ibm.ws.sip.stack.transaction.util;

import jain.protocol.ip.sip.ListeningPoint;
import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.header.HeaderIterator;
import jain.protocol.ip.sip.header.HeaderParseException;
import jain.protocol.ip.sip.header.RecordRouteHeader;
import jain.protocol.ip.sip.header.RouteHeader;
import jain.protocol.ip.sip.message.Message;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.ListeningPointImpl;
import com.ibm.ws.jain.protocol.ip.sip.SipJainFactories;
import com.ibm.ws.jain.protocol.ip.sip.header.HeaderImpl;
import com.ibm.ws.sip.parser.CharArray;
import com.ibm.ws.sip.parser.HeaderCreator;
import com.ibm.ws.sip.parser.util.CharsBuffer;
import com.ibm.ws.sip.parser.util.InetAddressCache;
import com.ibm.ws.sip.properties.StackProperties;
import com.ibm.ws.sip.stack.transaction.SIPTransactionConstants;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection;

/**
 * @author amirk
 * 
 * utility class for sip operations
 */
public class SIPStackUtil
{

	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(SIPStackUtil.class);

	/**
	* cache constants maybe overridden by some properties file
	*/
	public static  int s_ipCacheInitSize = StackProperties.IP_CACHE_INIT_SIZE_DEFAULT;
	
	/**
	 * when the LRU cache reach this size , we shall begin
	 * throwing the eldest entry.
	 */
	public static  int s_ipCacheMaxSize = StackProperties.IP_CACHE_MAX_SIZE_DEFAULT;
	
	private static float IP_CACHE_LOAD_FACTOR = 0.75f; 
	
	/**
	 * a LRU cache of SIPURLImpl classes...
	 * The reason: minimize String allocation made by InetAddress.getHostName(...)
	 * Moti: I'm not sure about the synchronized here but
	 * I put it anyway , to be safe. We shall see how this affects
	 * performance. 
	 */
	private static Map m_host2IP = null;

	/** work buffer for assembling the IBM-Client-Address value */
	private static ThreadLocal<CharsBuffer> m_ibmClientAddressBuffers = new ThreadLocal<CharsBuffer>();

	/** the IBM-Client-Address header name */
	private static final String IBM_CLIENT_ADDRESS = "IBM-Client-Address";

	
	private SIPStackUtil(){};
	
	/** return a List from the headers iterator */
	public static List headerIteratorToList( HeaderIterator iter )
	{
		
		List retVal = new Vector(3);
		
		if( iter!=null  )
		{
			while( iter.hasNext() )
			{
				try
				{
					retVal.add( iter.next());
				}
				catch (HeaderParseException e)
				{
					if(c_logger.isTraceDebugEnabled())
					{
					c_logger.traceDebug(null,"headerIteratorToList",e.getMessage());
					}
				}
				catch (NoSuchElementException e)
				{
					if(c_logger.isTraceDebugEnabled())
					{
					c_logger.traceDebug(null,"headerIteratorToList",e.getMessage());
					}
				}
			}
		}
		return retVal;
	}
	
	
	/**
	 * 
	 * @param msg
	 * @return
	 * @throws SipParseException
	 */
	public static List getRouteHeaders( Message msg ) throws  SipParseException 
	{
		//if we have a record route headers , create record route ones
		List routeHeaders;
		if (msg.getRecordRouteHeaders() != null && msg.getRecordRouteHeaders().hasNext())
		{
			routeHeaders = new ArrayList(3);
			//this means this was a response that created a dialog , that has a record route headers
			HeaderIterator iter = msg.getRecordRouteHeaders();
			while (iter.hasNext())
			{
				RecordRouteHeader recordRoute = (RecordRouteHeader)iter.next();
				RouteHeader route =  SipJainFactories.getInstance().getHeaderFactory().createRouteHeader( recordRoute.getNameAddress());
				routeHeaders.add(route);
			}
		}
		else
		{
			//this is a request that has routes
			HeaderIterator iter = msg.getHeaders( RouteHeader.name );
			routeHeaders = SIPStackUtil.headerIteratorToList( iter );
		}
		return routeHeaders;
	}
	
	
	
	
	public static String generateRandomString()
	{		
		return String.valueOf(Math.random()).substring(2);		
	}
	
	
	/**
	 * follow section 20.8 to generate a global CallId
	 * @param address
	 * @return
	 */
	public static String  generateCallIdentifier(String address) 
	{
		if (address == null || address.trim().length() == 0) {
			return generateRandomString();
		}
		return generateRandomString() + "@" + address;
	}
	
	//
	static char[] hexChar = 
		{ '0' , '1' , '2' , '3' , 
		  '4' , '5' , '6' , '7' , 
		  '8' , '9' , 'a' , 'b' , 
		  'c' , 'd' , 'e' , 'f' } ; 
	
	public static String getAsHexString ( byte[] b ) 
	{ 
		StringBuffer sb = new StringBuffer( b.length * 2 ); 
		for ( int i=0 ; i<b.length ; i++ ) 
		{  
			sb.append( hexChar [ ( b[ i] & 0xf0 ) >>> 4 ] ) ;  
			sb.append( hexChar [ b[ i] & 0x0f ] ) ; 
		} 
		return sb.toString() ; 
	}
	
	
	/**
	 * implementation of section 19.3 for generating Tags 
	 * @return tag generated
	 */
	public static String generateTag() 
	{		
		 return generateRandomString();
	}
	
	/**
	 * create the branch id for indicating the transaction 
	 * implementing section 8.1.1.7
	 * @return the branch Id
	 */
	public static String generateBranchId() 
	{
		final int randomSize = 15; // number of random digits
		final double exp = 1000000000000000.0; // 10 to the power of 15
		long random = Math.round(Math.random() * exp);
		
		StringBuffer branchId = new StringBuffer(
			SIPTransactionConstants.BRANCH_MAGIC_COOKIE_SIZE
			+ randomSize);
		branchId.append(SIPTransactionConstants.BRANCH_MAGIC_COOKIE);
		branchId.append(random);

		return branchId.toString();
	}
	
	public static String toHexString ( byte[] b ) 
	{ 
		StringBuffer sb = new StringBuffer( b.length * 2 ); 
		for ( int i=0 ; i<b.length ; i++ ) 
		{  
			sb.append( hexChar [ ( b[ i] & 0xf0 ) >>> 4 ] ) ;  
			sb.append( hexChar [ b[ i] & 0x0f ] ) ; 
		} 
		return sb.toString() ; 
	}
	
	/**
	 * Determines the IP address of a host, given the host's name.
	 * @param host -  host  the specified host, or <code>null</code> for the
     *                 local host. 
	 * @return String -  an IP address for the given host name.
	 */
	public static String getHostAddress(String host) 
	{
        try {
            return InetAddressCache.getHostAddress(host);
        }
        catch (UnknownHostException e) {
        	return host;
        }
    }
	
	/**
	 * converts numeric IP address to String
	 * 
	 * @param address host address
	 * @return address as String in dotted format
	 */
	public static String getHostAddress(InetAddress address) {
        return InetAddressCache.getHostAddress(address);
	}

	
	/**
	 * Utility function to check whether the specified host name represent
	 * the same ip address
	 * 
	 * the functions replaces any parameter which is represented as IP to host name
	 * and then compare literally between them.
	 * 
	 * @param host - the host name or the IP address to be compared with. 
	 * @param otherhost - other host name or IP address to be compared with.
	 * 
	 * @return true whether the both parameters represent the same host.
	 */
	public static boolean isSameHost(String host, String otherhost) {

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(SIPStackUtil.class, "isSameHost", new String[]{
				String.valueOf(host), String.valueOf(otherhost)});
		}
		
		boolean rc = false;

		String normalizedHost = normalizedHostName(host);
		String normalizedOtherHost = normalizedHostName(otherhost);

		if(normalizedHost != null && normalizedOtherHost != null) {

			rc = normalizedHost.equals(normalizedOtherHost);
			
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(SIPStackUtil.class, "isSameHost", 
						"is " + normalizedHost + " == " + normalizedOtherHost + " ?");
			}
		}
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(SIPStackUtil.class, "isSameHost", rc);
		}

		return rc; 
	}
	
	
	/**
	 * Normalizes the given host name.
	 * whether the String is the host name or the IP address
	 * this methods returns the canonical host name.
	 * 
	 * @param host - the hostname or the ip address to normalized
	 * @return canonical hostname or null in case of exception.
	 */
	private static String normalizedHostName(String host) {

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(SIPStackUtil.class, "normalizedHostName", String.valueOf(host));
		}
		
		String returnValue = null;

		try {
			InetAddress address = InetAddressCache.getByName(host);
			
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(SIPStackUtil.class, "normalizedHostName", 
						"Address Cache returned : " + address);
			}
			
			returnValue = address.getCanonicalHostName();
			
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(SIPStackUtil.class, "normalizedHostName", 
						"The canonical host name is : " + returnValue);
			}

		} catch (UnknownHostException e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(SIPStackUtil.class, "normalizedHostName", 
						"Failed to get canonical host address : " + host);
			}
		}
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(SIPStackUtil.class, "normalizedHostName", returnValue);
		}
		
		return returnValue;

	}
	
	
	/**
	 * parses the name adress read from configuration parameters.
	 * The format of the parameter should be "ipaddress:port/transport" 
	 * i.e. 129.1.22.333:5060/UDP
	 * @param nameAdress
	 * @return
	 */
	public static SipURL parseNameAdressFromConfig( String nameAdress )
		throws SipParseException
	{
		SipURL retVal;
		
		try
		{			
			String host = null;
			String port = null;
			String transport = null;			
			if( nameAdress!=null && nameAdress.length() > 0 )
			{
				int doublequete = nameAdress.lastIndexOf(":");
				int rightSlash = nameAdress.lastIndexOf("/");
				
				if( doublequete==-1 )
				{
					if( rightSlash ==-1 )
					{
						host = nameAdress;
					}
					else
					{						
						host = nameAdress.substring( 0 , rightSlash  );
						transport = transport = nameAdress.substring( rightSlash + 1 , nameAdress.length());
					}					
				}
				else
				{
					if( rightSlash ==-1 )
					{
						//has ":" , no "/" , means "host:port" 
						host = nameAdress.substring( 0 , doublequete );
						port = nameAdress.substring( doublequete + 1 , nameAdress.length()  );						 
					}
					else
					{
						//has ":" and "/" , means  "host:port/transport"
						host = nameAdress.substring( 0 , doublequete );
						port = nameAdress.substring( doublequete + 1 , rightSlash );
						transport = nameAdress.substring( rightSlash + 1 , nameAdress.length());						 						
					}
				}
			}
			else
			{
				//create default listening , no parameters
				host = getLocalHost();
			}
			
			retVal = SipJainFactories.getInstance().getAddressFactory().createSipURL(host);
			if( port!=null )
			{
				retVal.setPort( Integer.parseInt(port));
			}
			if( transport!=null )
			{
				// Assaf: TODO: do this properly.
				if (transport.equalsIgnoreCase("tls"))
				{
					retVal.setScheme("sips");
				}
				retVal.setTransport( transport );
			}
			return retVal;			
		}
		catch( Throwable t )
		{
			throw new SipParseException("could not parse listening point");
		}
		
	}
	
	public static ListeningPointImpl parseLPNameAdressFromConfig( String nameAdress )
		throws SipParseException
	{
		try
		{			
			String host = "";
			String port = "";
			String transport = "";			
			ListeningPointImpl retVal;
			if( nameAdress!=null && nameAdress.length() > 0 )
			{
				int doublequete = nameAdress.lastIndexOf(":");
				int rightSlash = nameAdress.lastIndexOf("/");
				
				if( doublequete==-1 )
				{
					if( rightSlash ==-1 )
					{
						
						//only one parameter , host , port or transport
						try
						{
							Integer.parseInt( nameAdress );
							//if no exception , this is the port
							host = getLocalHost();
							port = nameAdress;
							transport = ListeningPointImpl.TRANSPORT_UDP;							
						}
						catch( NumberFormatException exp )
						{
							//host or transport 
							if( nameAdress.equalsIgnoreCase(ListeningPointImpl.TRANSPORT_UDP) || 
								nameAdress.equalsIgnoreCase(ListeningPointImpl.TRANSPORT_TCP) ||
								nameAdress.equalsIgnoreCase(ListeningPointImpl.TRANSPORT_TLS) )
								{
									host = getLocalHost();
									port = "5060";
									transport = nameAdress;																
								}
							else
							{
								host = nameAdress;
								port = "5060";
								transport = ListeningPointImpl.TRANSPORT_UDP;																
							}
						}
					}
					else
					{
						
						String param = nameAdress.substring( 0 , rightSlash  );
						try
						{
							Integer.parseInt( param );
							//if no exception , this is the port
							host = getLocalHost();
							port = param;														
						}
						catch( NumberFormatException exp )
						{
							//this is the host
							host = param;
							port = "5060";
						}
						transport = transport = nameAdress.substring( rightSlash + 1 , nameAdress.length());
					}
					
				}
				else
				{
					if( rightSlash ==-1 )
					{
						//has ":" , no "/" , means "host:port" 
						host = nameAdress.substring( 0 , doublequete );
						port = nameAdress.substring( doublequete + 1 , nameAdress.length()  );
						transport = ListeningPointImpl.TRANSPORT_UDP;						 
					}
					else
					{
						//has ":" and "/" , means  "host:port/transport"
 						host = nameAdress.substring( 0 , doublequete );
						port = nameAdress.substring( doublequete + 1 , rightSlash );
						transport = nameAdress.substring( rightSlash + 1 , nameAdress.length());						 						
					}
				}
			}
			else
			{
				//create default listening , no parameters
				host = getLocalHost();
				port = "5060";
				transport = ListeningPointImpl.TRANSPORT_UDP;																							
			}
			
			retVal = new ListeningPointImpl( host , Integer.parseInt(port) , transport);
			return retVal;			
		}
		catch( Throwable t )
		{
			throw new SipParseException("could not parse listenning point");
		}
	}
	
	public static String getLocalHost()
	{
		//create default listening , no parameters
		try 
		{
			return InetAddressCache.getHostAddress(InetAddress.getLocalHost());
		} 
		catch (UnknownHostException e) 
		{
			return "127.0.0.1";
		}					
		
	}

	/**
	 * adds the proprietary "IBM-Client-Address" header if needed
	 * @param message incoming message
	 * @param listeningConnection the local listening point
	 * @throws SipParseException 
	 * @throws IllegalArgumentException 
	 * @throws SipParseException 
	 */
	public static void addIbmClientAddressHeader(Message message, 
			ListeningPoint lpoint) 
	throws IllegalArgumentException, SipParseException{
		
		addIbmClientAddressHeader(message, null, lpoint);
	}
    /**
	 * adds the proprietary "IBM-Client-Address" header if needed
	 * @param message incoming message
	 * @param connection the connection that carried the message here
	 * @param listeningConnection the local listening point
	 * @throws SipParseException 
	 */
	public static void addIbmClientAddressHeader(Message message,
		SIPConnection connection, ListeningPoint lpoint)
		throws IllegalArgumentException, SipParseException
	{
		CharsBuffer ibmClientAddressBuffer = m_ibmClientAddressBuffers.get();
		
		if (ibmClientAddressBuffer == null){
			ibmClientAddressBuffer = new CharsBuffer();
			m_ibmClientAddressBuffers.set(ibmClientAddressBuffer);
		}

		// IBM-Client-Address: r.e.m.ote:5555;local-address=l.o.c.al:5060
		if (message.getHeader(IBM_CLIENT_ADDRESS, true) != null) {
			// already added by the SLSP
			if( c_logger.isTraceDebugEnabled())
			{
				c_logger.traceDebug(SIPStackUtil.class,"SIPStackUtil addIbmClientAddressHeader",
						"IBM-Client-Address already added");
			}
			return;
		}
		String localHost = lpoint.getHost();
		int localPort = lpoint.getPort();
		
		String remoteHost;
		int remotePort;
		if (connection != null){
			remoteHost = connection.getRemoteHost();
			remotePort = connection.getRemotePort();
		}else{
			remoteHost = localHost;
			remotePort = localPort;
		}
		ibmClientAddressBuffer.reset();
		ibmClientAddressBuffer.append(remoteHost).append(':').append(remotePort);
		ibmClientAddressBuffer.append(";local-address=");
		ibmClientAddressBuffer.append(localHost).append(':').append(localPort);
		char[] buffer = ibmClientAddressBuffer.getCharArray();
		int length = ibmClientAddressBuffer.getCharCount();
		CharArray array = CharArray.getFromPool(buffer, 0, length);
		HeaderImpl ibmClientAddressHeader = HeaderCreator.createHeader(IBM_CLIENT_ADDRESS);
		ibmClientAddressHeader.setValue(array);
		ibmClientAddressHeader.parse(); // returns the CharArray back to the pool
		message.addHeader(ibmClientAddressHeader, true);
		
		if( c_logger.isTraceDebugEnabled())
		{
			c_logger.traceDebug(SIPStackUtil.class,"SIPStackUtil addIbmClientAddressHeader",
					"IBM-Client-Address: " + ibmClientAddressHeader);
		}
	}

}
