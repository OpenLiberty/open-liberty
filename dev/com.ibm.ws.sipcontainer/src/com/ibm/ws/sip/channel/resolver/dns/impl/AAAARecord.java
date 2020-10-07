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
import java.net.Inet6Address;
import java.net.UnknownHostException;

/**
 * Class to represent a DNS AAAA resource record
 * <p>
 * See RFC 3596
 * <p>
 * The field members of this class represent the RDATA portion
 * of a resource record
 */
public class AAAARecord extends ResourceRecord{
	
	private Inet6Address _address;
	
	protected AAAARecord(){
		_address = null;
	}
	protected AAAARecord(WsByteBuffer buffer){
		super (buffer);
		byte [] ba = new byte[16];
		
		for (int i = 0; i < 16; i++){
			ba[i] = buffer.get();
		}
		
		try {	
			_address = (Inet6Address)Inet6Address.getByAddress(ba);
		}
		catch (UnknownHostException uhe) {}
		
	}
	
	protected void toBuffer(WsByteBuffer buffer){
		super.toBuffer(buffer);		
		buffer.put(_address.getAddress());
	}
	
	public void setAddress(Inet6Address address){
		_address = address;
	}
	
	public Inet6Address getAddress() {
		return _address;
	}
	
	public short calcrdLength(){
		return (short)16;
	}
	
	public String toString(){
		String s = new String();
		s = super.toString();
		s += "      address: " + _address.getHostAddress() + "\n";
		return s;
}

}
