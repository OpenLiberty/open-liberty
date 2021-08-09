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
package com.ibm.ws.sip.stack.transaction.transport.routers.dns;

import jain.protocol.ip.sip.SipException;
import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.header.NameAddressHeader;
import jain.protocol.ip.sip.header.RouteHeader;
import jain.protocol.ip.sip.message.Request;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.ListeningPointImpl;
import com.ibm.ws.sip.stack.transaction.transport.Hop;
import com.ibm.ws.sip.stack.transaction.transport.routers.Router;
import com.ibm.ws.sip.stack.transaction.util.ApplicationProperties;

/**
 * @author Amirk
 */
public class DNSSRVRouter implements Router
{
	
	private static final String s_dnsServerPrefix = "com.ibm.sip.dnsserver";
	
	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(DNSSRVRouter.class);
	
	
	//list of servers to do a lookup for 
	private List m_dnsServers;
	
	
	//context of the dns servers
	private DirContext m_context;
	
	//srv record record
	private static final String QTYPE_SRV = "SRV";
	
	//srv record record
	private static final String QTYPE_ADDRESS = "A";
	
	//Sip service prefix
	private static final String SIP_SERVICE = "sip";


	/**
	 * 
	 *
	 */
	public DNSSRVRouter()
	{
		try
        {
            init();
        }
        catch (NamingException e)
        {
        	if( c_logger.isTraceDebugEnabled())
        	{
        		c_logger.traceDebug(this,"DNSSRVRouter",e.getMessage());
        	}
        }
	}
	
	/**
	 * init the context
	 * @throws NamingException
	 */
	private void init() throws NamingException
	{
		Hashtable env = new Hashtable();
		m_dnsServers = readDNSServersFromConfig();
		//TODO - check if com.sun.jndi.dns.DnsContextFactory is supported by 1.3,1.4 JVM
		//also - what should I call in IBM JVM?
		env.put("java.naming.factory.initial","com.sun.jndi.dns.DnsContextFactory");
		env.put("java.naming.provider.url",getDNSServersAsProviderString() );
		m_context = new InitialDirContext(env);		
	}
	

	/**
	 * init dns servers
	 * @return - list of servers as Strings
	 */
	private List readDNSServersFromConfig()
	{
		List dnsServers = new ArrayList(2);
		
		//iterate Over the listening points and create them
		int lpNum = 1;
		//iterate until there are listening points , then break
		while( true )
		{
			String dnsServer = ApplicationProperties.getProperties().getString(
						s_dnsServerPrefix +  "." + lpNum );
			if( dnsServer.equals("") )
			{ 
				break;
			}
											 
			lpNum++;
								
			dnsServers.add(dnsServer);
		}
		
		return dnsServers;	
		
	}
	


	/**
	 * @return - list of DNS servers to create DNS query
	 */
	private String getDNSServersAsProviderString()
	{
		StringBuffer retval = new StringBuffer("");
		for(int i=0;i<m_dnsServers.size();i++)
		{
			if( i> 0)
			{
				retval.append(" "); 
			}
			retval.append("dns://" + m_dnsServers.get(i));			 
		}
		return retval.toString();		
	}
		
	/**
	 * get the next Hop
	 * @param sipRequest
	 * @throws SipParseException
	 */
	public List getNextHops(Request sipRequest)
	{
		List retval = new ArrayList(3);
		
		try
		{
			SipURL uriToFind; 
							
			//get the URI to which we should do a DNS lookup for
			if( sipRequest.hasRouteHeaders() )
			{
				NameAddressHeader routeHeader =  ( NameAddressHeader )sipRequest.getHeader( RouteHeader.name , true);
				uriToFind = (SipURL) routeHeader.getNameAddress().getAddress();				
			}
			else
			{ 
				uriToFind = (SipURL)sipRequest.getRequestURI();
			}
			
			//OK
			//we got the host to connect to
			//is this a real IP adress or a DNS record to query?
			//solution ( I think a bad one , but I could not find another ):
			// try a "A" ( Address)  query - if there is an Answer , great , this is a computer
			// else
			// try a "SRV" query - if there are answers , greate , get them
			// else
			//return empty list
									
			try
			{
				//not an IP
				//try do find domain by regular "A" name
				retval.addAll( getHostAddressesTypeResults( uriToFind ) );					
			}
			catch( UnknownHostException exp )
			{
				//do nothing - not a bug , just try DNS Srv
				try
                {
                    retval.addAll( getDNSSrvTypeResults( uriToFind ));
                }
                catch (UnknownHostException e)
                {
                	if( c_logger.isTraceDebugEnabled())
                	{
                	c_logger.traceDebug(this,"getNextHops",exp.getMessage());
                	}
                }
			}
				
		}
		catch( SipException exp  )
		{
        	if( c_logger.isTraceDebugEnabled())
        	{
			c_logger.traceDebug(this,"getNextHops",exp.getMessage(),exp);
        	}
		}
		
		return retval;	
											
	}
	
	
	
	/**
	 * find by: if this is an IP , return it , and if not , try a DNS A Type query
	 * @param uriToFind - the uri to lookup
	 * @return - results from query - can be more than one ( like in the case of rotating DNS )  
	 * @throws UnknownHostException - result not found
	 */
	private List getHostAddressesTypeResults( SipURL uriToFind ) throws UnknownHostException
	{		
		List retVal = new ArrayList(3);
		//call DNS with A type
		String tempHost = uriToFind.hasMAddr() ? uriToFind.getMAddr() : uriToFind.getHost();
		
		List hosts;
		if(Character.isDigit(tempHost.charAt(0)))
		{
			//this is an IP , just add it
			hosts = new ArrayList(3);
			hosts.add( tempHost );
		}
		else
		{
			//not an IP , get the dns A type results
			hosts = getDNSATypeResult( tempHost );			
		}
		
		
		//no exception , A type OK , create a Hop
			
		int port;
		String transport;
		
		if (uriToFind.hasPort() ) 
		{
			port = uriToFind.getPort();
		} 
		else 
		{
			port = 5060;
		}
		
		transport = uriToFind.getTransport();
		if (transport == null) 
			transport = ListeningPointImpl.TRANSPORT_UDP;
		else
		{
			if( uriToFind.getScheme().equalsIgnoreCase("sips"))
			{
				transport = ListeningPointImpl.TRANSPORT_TLS;
			}
		}
		
		//create Hops to all servers
		for (Iterator iter = hosts.iterator(); iter.hasNext();)
        {
            String host = (String)iter.next();
			Hop hop = new Hop( transport , host , port );
           	retVal.add( hop );
        }
			
		return retVal;
		
	}
	
	private List getDNSSrvTypeResults( SipURL uriToFind ) throws UnknownHostException
	{
		List retval = new ArrayList(2);
		
		//the domain to query
		String domain = uriToFind.getHost();
			
		//transport
		String transport = uriToFind.getTransport();
		if (transport == null)
		{ 
			transport = ListeningPointImpl.TRANSPORT_UDP;
		}
		else
		{
			if( uriToFind.getScheme().equalsIgnoreCase( "sips" ))
			{
				transport = ListeningPointImpl.TRANSPORT_TLS;
			}
		}				
	
		//create the lookup string for the query
		StringBuffer buf = new StringBuffer("_");
		buf.append(SIP_SERVICE);
		buf.append("._" );
		buf.append(transport);
		buf.append(".");
		buf.append(domain);
		String qname = buf.toString();
		
		//DNS SRV request
		try
		{
			Attributes attr = m_context.getAttributes(qname, new String[] { QTYPE_SRV });
				
			//get the result
			Attribute srvRes = (Attribute)attr.getAll().next();
				
			//iterate on the result
			if( srvRes == null || srvRes.size() == 0 )
			{
				throw new UnknownHostException( uriToFind.toString() );
			}

			if( srvRes.size()==1 )
			{
				//TODO - check this , did not understand
			}
			
			List resultsList =  new ArrayList( attr.size() );
			//get all records
			NamingEnumeration na = srvRes.getAll();
			while (na.hasMore())
			{
				DNSSRVQueryResult res = new DNSSRVQueryResult(SIP_SERVICE, transport , (String) na.next()  );
				resultsList.add( res );
			}
				
			//sort results
			Collections.sort( resultsList );
				
			for (Iterator iter = resultsList.iterator();iter.hasNext();)
			{
				DNSSRVQueryResult res = (DNSSRVQueryResult) iter.next();
				//create Hops and put them in the return List
				Hop srvHop = new Hop( transport , res.hostName , res.port );
				retval.add(srvHop);								
			}																				
		}
		catch( NamingException ne )
		{
			throw new UnknownHostException( uriToFind.toString() );			
		}
		return retval;		
	}
	
	
	public List getDNSATypeResult( String host  ) throws UnknownHostException
	{
				
		List retVal = new ArrayList(3);
		//DNS SRV request
		try
		{
			Attributes attr = m_context.getAttributes(host, new String[] { QTYPE_ADDRESS });
				
			//get the result
			Attribute aRes = (Attribute)attr.get( QTYPE_ADDRESS );
				

			if( aRes==null || aRes.size()==0) 
			{
				throw new UnknownHostException( host );
			}
			//iterate on the result and get all records 
			NamingEnumeration na = aRes.getAll();
			while (na.hasMore())
			{
				//this will add the host result to the list
				retVal.add(na.next()); 
			}
									
		}
		catch( NamingException ne )
		{
			//do nothing , not neserally a bug
			throw new UnknownHostException( host );
		}
		return retVal;				
	}
	
	
	

	public Hop getOutboundProxy()
	{
		return null;
	}

	public void processRequest(Request req)
		throws SipParseException
	{
	}

	public void removeConnectionHop(Hop value)
	{
	}

	public void setOutboundProxy(Hop proxy)
	{
	}	
}
