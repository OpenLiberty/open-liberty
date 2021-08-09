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
 * Class to represent a DNS OPT pseudo resource record
 * <p>
 * See RFC 2671
 * <p>
 * The field members of this class represent the RDATA portion
 * of a resource record
 */
public class OPTRecord extends ResourceRecord{
	
	/** senders UDP payload size */
	private short _UDPpayloadSize;
	/** first 8 MSBs of the RR TTL  */
	private byte _extendedRCODE;
	/** second 8 MSBs of the RR TTL */
	private byte _version;
	/** final 16 bits of the RR TTL */
	private short _Z;
	
	/** variable portion of OPT record contained in RDATA */
	private short _optionCode;
	private short _optionLength;
	private byte [] _optionData;
	
	/** Standard MAX Ethernet MTU 1500 octets (does not include the ethernet header 14 octets and 4 octets CRC) */
	/** 1500 - IPv4 Header 20 octets (no options) - 8 octet UDP Header = 1472                                   */
	/** 1500 - IPv6 Header 40 octets (no options  - 8 octet UDP Header = 1452                                   */
	/** recommendation by rfc 2671 is for a lower value to prevent ICMP from intermediate gateways              */
	public static final short DEFAULT_UDP_PAYLOAD_SIZE = 1280; 
	
	protected OPTRecord(){
		
		_UDPpayloadSize = DEFAULT_UDP_PAYLOAD_SIZE;
		
		_extendedRCODE = 0;
		_version = 0;
		_Z = 0;
		
		_optionCode = 0;
		_optionLength = 0;
		_optionData = new byte [0];
		
		/** senders UDP payload size */
		super.setName(new Name(""));
		super.setClassType((short)_UDPpayloadSize);
		/** no options */
		super.setrdLength((short)0);
	}
	
	protected OPTRecord(WsByteBuffer buffer){
		
		super(buffer);
		
		/** payload size is overlayed in the RR class field */
		_UDPpayloadSize = super.getClassType();
		
		_extendedRCODE = (byte)((super.getTTL() & 0xff000000) >>> 24);
		_version = (byte)((super.getTTL() & 0x00ff000000) >>> 16);
		
		
		if (super.getrdLength() > 0) {
			_optionCode = buffer.getShort();
			_optionLength = buffer.getShort();
			_optionData = new byte [_optionLength];
		}
		else {
			_optionCode = 0;
			_optionLength = 0;
			_optionData = null;
		}
		
	}
	
	protected void toBuffer(WsByteBuffer buffer){
		super.toBuffer(buffer);
		if (super.getrdLength() > 0) {
			buffer.putShort(_optionCode);
			buffer.putShort(_optionLength);
			if (_optionLength > 0)
				buffer.put(_optionData);
		}
	}
	
	public short calcrdLength(){
		int length = 0;
		length = Short.SIZE/8 + Short.SIZE/8 + _optionData.length;
		return (short)length;	
	}
	
	public void setUdpPayloadSize(short size) {
		_UDPpayloadSize = size;
		super.setClassType(_UDPpayloadSize);
	}
	
	public String toString(){
		String s = new String();
		s =    "    Resource Record: " + Dns.TYPESTRING[Dns.OPT] + "\n" + 
		       "      domain name: " + super.getName().toString() + "\n";
		s +=   "      udp payload size:  "   + _UDPpayloadSize +"\n" +
		       "      extended RCODE: " + _extendedRCODE +"\n" +
		       "      version: "   +_version + "\n" + 
		       "      Z: " + _Z +"\n" +
		       "      rdlength: " + super.getrdLength() + "\n";
		
		if (_optionLength > 0){
			s += "      option code: " +_optionCode + "\n" +
			     "      option length: " + _optionLength + "\n" + 
			     "      option data : " + _optionData.toString() + "\n";	
		}
		     
		return s;
	}

}
