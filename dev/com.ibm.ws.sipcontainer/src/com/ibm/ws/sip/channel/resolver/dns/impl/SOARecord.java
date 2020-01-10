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
 * Class to represent a DNS Start of Authority (SOA) resource record
 * <p>
 * See RFC 1035
 * <p>
 * The field members of this class represent the RDATA portion
 * of a resource record
 */
 public class SOARecord extends ResourceRecord{
	 
	private Name _mname;
	private Name _rname;
	private int  _serial;
	private int  _refresh;
	private int  _retry;
	private int  _expire;
	private int  _minTTL;
	
	protected SOARecord(){
		_mname = null;
		_rname = null;
		_serial = 0;
		_refresh = 0;
		_retry = 0;
		_expire = 0;
		_minTTL =0;
	}
	protected SOARecord(WsByteBuffer buffer){
		
		super(buffer);
		_mname   = new Name(buffer);
		_rname   = new Name(buffer);
		_serial  = buffer.getInt();
		_refresh = buffer.getInt();
		_retry   = buffer.getInt();
		_expire  = buffer.getInt();
		_minTTL  = buffer.getInt();
		
	}
	
	protected void toBuffer(WsByteBuffer buffer){
		super.toBuffer(buffer);
		
		_mname.toBuffer(buffer);
		_rname.toBuffer(buffer);
		buffer.putInt(_serial);
		buffer.putInt(_refresh);
		buffer.putInt(_retry);
		buffer.putInt(_expire);
		buffer.putInt(_minTTL);
	}
	
	public void setMname(Name n){
		_mname = n;
	}
	
	public void setRname(Name n){
		_rname = n;
	}
	
	public void setSerial(int i){
		_serial = i;
	}
	
	public void setRefresh(int i){
		_refresh = i;
	}
	
	public void setRetry(int i){
		_retry = i;
	}
	
	public void setExpire(int i){
		_expire = i;
	}
	
	public void setMinTTL(int i){
		_minTTL = i;
	}
	
	public short calcrdLength(){
		int length = 0;	
		length = _mname.length() + _rname.length() + 
				 Integer.SIZE/8 + Integer.SIZE/8 + Integer.SIZE/8 + Integer.SIZE/8 + Integer.SIZE/8;
		return (short)length;
	}
	
	public Name getMname(){
		return _mname;
	}
	
	public Name getRname(){
		return _rname;
	}
	
	public String toString() {
		String s = new String();
		s = super.toString();
		s += "      mname: " + _mname.toString() + "\n" +
		     "      rname: " + _rname.toString() + "\n" +
		     "      serial: " + _serial + "\n" +
		     "      refresh: " + _refresh + "\n" +
		     "      retry: " + _retry + "\n" +
		     "      expire: " + _expire + "\n" +
		     "      minimum TTL: " + _minTTL + "\n";
		return s;
	}

}
