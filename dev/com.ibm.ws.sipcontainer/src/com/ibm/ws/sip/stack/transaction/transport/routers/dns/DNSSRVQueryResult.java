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

import java.util.StringTokenizer;

/**
 *	Represents one SRV record read from the DNS.
 */
public class DNSSRVQueryResult implements Comparable
{
	//service typr
	String serviceType;
	
	//transport type
	String protocol;

	//host name
	String hostName;

	//record priority
	int priority;

	//record weight
	int weight;

	//port number
	int port;

	
	long order;
	
	/**
	 *	constructor
	 */
	DNSSRVQueryResult( String serviceType ,  String protocol , String dnsRecord )
	{
		// Parser to pull apart the DNS record
		this.serviceType = serviceType;
		this.protocol = protocol;
		StringTokenizer recordParser = new StringTokenizer(dnsRecord);
		priority = Integer.parseInt(recordParser.nextToken());
		weight = Integer.parseInt(recordParser.nextToken());
		port = Integer.parseInt(recordParser.nextToken());
		hostName = recordParser.nextToken();		
	}
		
	public int compareTo(Object o)
	{
		DNSSRVQueryResult otherRes = (DNSSRVQueryResult)o;
		return otherRes.priority - priority;
	}
	
	public String toString()
	{
		StringBuffer buf = new StringBuffer("_");
		buf.append(serviceType);
		buf.append("._" );
		buf.append(protocol);
		buf.append("._");
		buf.append(hostName);
		buf.append("\t");
		buf.append(priority);
		buf.append(" ");
		buf.append(weight);
		buf.append(" ");
		buf.append(port);		
		return  buf.toString();  
	}
}
