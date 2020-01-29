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
package com.ibm.ws.sip.channel.resolver.dns.impl;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;

/**
 * Class to represent a DNS CNAME resource record
 * <p>
 * See RFC 1035
 * <p>
 * The field members of this class represent the RDATA portion
 * of a resource record
 */
public class CNAMERecord extends ResourceRecord{
	
	private Name _cname;
	
	protected CNAMERecord(){
		_cname = null;
	}
	protected CNAMERecord(WsByteBuffer buffer){
		
		super(buffer);
		_cname = new Name(buffer);
		
	}
	
	protected void toBuffer(WsByteBuffer buffer){
		super.toBuffer(buffer);
		_cname.toBuffer(buffer);		
	}
	
	public void setCname(Name name){
		_cname = name;
	}
	
	public Name getCname(){
		return _cname;
	}
	
	public short calcrdLength(){
		int length = 0;
		length = _cname.length();
		return (short)length;
	}
	
	public String toString(){
		return super.toString() + "      name: " +_cname.toString() +"\n";
	}

}
