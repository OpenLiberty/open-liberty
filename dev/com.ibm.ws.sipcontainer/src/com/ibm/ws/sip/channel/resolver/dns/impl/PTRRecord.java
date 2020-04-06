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
 * Class to represent a DNS PTR resource record
 * <p>
 * See RFC 1035
 * <p>
 * The field members of this class represent the rdata portion
 * of a resource record
 * 
 */
public class PTRRecord extends ResourceRecord{
	
	private Name _ptrDname;
	
	protected PTRRecord(){
		_ptrDname = null;
	}
	
	protected PTRRecord(WsByteBuffer buffer){
		
		super(buffer);
		_ptrDname = new Name(buffer);
		
	}
	
	public void setPtrDname(Name name){
		_ptrDname = name;
		
	}
	
	public Name getPtrDname(){
		return _ptrDname;
	}
	
	protected void toBuffer(WsByteBuffer buffer){
		super.toBuffer(buffer);		
		_ptrDname.toBuffer(buffer);
	}
	
	public short calcrdLength(){
		int length = 0;
		length = _ptrDname.length();
		return (short)length;	
	}
	
	public String toString(){
		String s = new String();
		s = super.toString();
		s += "      ptrDname: " + _ptrDname.toString() + "\n";
		return s;
	}

}
