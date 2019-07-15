/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
public class NSRecord extends ResourceRecord{
	
	private Name _NSname;
	
	protected NSRecord(){
		_NSname = null;
	}
	protected NSRecord(WsByteBuffer buffer){
		
		super(buffer);
		_NSname = new Name(buffer);
		
	}
	
	protected void toBuffer(WsByteBuffer buffer){
		super.toBuffer(buffer);
		_NSname.toBuffer(buffer);		
	}
	
	public void setNSname(Name name){
		_NSname = name;
	}
	
	public Name getNSname(){
		return _NSname;
	}
	
	public short calcrdLength(){
		int length = 0;
		length = _NSname.length();
		return (short)length;	
	}
	
	public String toString(){
		return super.toString() + "      name: " +_NSname.toString() +"\n";
	}

}
