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
import  java.lang.reflect.Constructor;

/**
 * Class to represent the common resource record portion of a DNS message
 * <p>
 * See RFC 1035
 */
public class ResourceRecord {
	
	private Name   _name;
	private short  _type;
	private short  _class;
	private int    _ttl;
	private short  _rdLength;
	//private String _rData;
	
	/** subclasses of ResourceRecord */
	private static final String ARECORD       = "ARecord";
	private static final String NSRECORD      = "NSRecord";
	private static final String CNAMERECORD   = "CNAMERecord";
	private static final String SOARECORD     = "SOARecord";
	private static final String PTRRECORD     = "PTRRecord";
	private static final String AAAARECORD    = "AAAARecord";
	private static final String SRVRECORD     = "SRVRecord";
	private static final String NAPTRRECORD   = "NAPTRRecord";
	private static final String OPTRECORD     = "OPTRecord";
	
	protected ResourceRecord(){
		_name = null;
		_type = 0;
		_class = 0;
		_ttl = 0;
		_rdLength = 0;
		//_rData = null;
	}
	protected ResourceRecord(WsByteBuffer buffer){
		_class = buffer.getShort();
		_ttl = buffer.getInt();
		_rdLength = buffer.getShort();

		
	}
	
	public static ResourceRecord createRecord(Short type){
		/** get the subclass and instantiate it*/
		try {
			Class clazz = ResourceRecord.className(type);
			Constructor constructor = clazz.getDeclaredConstructor();
			
			ResourceRecord rr = (ResourceRecord)constructor.newInstance();
			
			rr.setType(type);
			rr.setClassType(Dns.IN);
			
			return rr;
			
		}
		catch (Exception e){
			System.out.println("ResourceRecord::createRecord Exception: " + e.getMessage());
			return null;
		}
		
	}
	
	protected static ResourceRecord createRecord(WsByteBuffer buffer){
		/** must read into the buffer to figure out what type of resource record this is */
		Name name     = new Name (buffer);
		short type    = buffer.getShort();
				
		/** get the subclass and instantiate it*/
		try {
			Class clazz = ResourceRecord.className(type);
			ResourceRecord rr = null;
			
			if (type != Dns.SRV){
				Constructor constructor = clazz.getDeclaredConstructor(WsByteBuffer.class);
				rr = (ResourceRecord)constructor.newInstance(new Object[] {buffer});
			}
			/** SRVRecord field members are derived from name */
			else {
				Constructor constructor = clazz.getDeclaredConstructor(WsByteBuffer.class, Name.class);
				rr = (ResourceRecord)constructor.newInstance(new Object[] {buffer, name});
			}
			
			rr.setName(name);
			rr.setType(type);
			
			return rr;
			
		}
		catch (Exception e){
			//System.out.println("ResourceRecord::createRecord Exception: " + e.getMessage());
			return null;
		}
		
	}
	
	
	protected void toBuffer(WsByteBuffer buffer){
		_name.toBuffer(buffer);
		buffer.putShort(_type);
		buffer.putShort(_class);
		buffer.putInt(_ttl);
		buffer.putShort(_rdLength);
	}	
	
	public Name getName(){
		return _name;
	}
	
	public void setName(Name n){
		_name = n;
	}
	
	public short getType(){
		return _type;
	}
	
	public void setType(short s){
		_type = s;
	}
	
	public short getClassType(){
		return _class;
	}
	
	public void setClassType(short s){
		_class =s ;
	}
	
	public int getTTL(){
		return _ttl;
	}
	
	public void setTTL(int i){
		_ttl = i;
	}
	
	public short getrdLength(){
		return _rdLength;
	}
	
	public void setrdLength(short s ){
		_rdLength = s;		
	}
	
	protected static String classNameAsString(Short type) {
		String className = null;
		switch (type){
		case(Dns.A):
			className = ARECORD;
			break;
		case(Dns.NS):
			className = NSRECORD;
			break;
		case(Dns.CNAME):
			className = CNAMERECORD;
			break;
		case(Dns.SOA):
			className = SOARECORD;
			break;
		case(Dns.PTR):
			className = PTRRECORD;
			break;
		case(Dns.AAAA):
			className = AAAARECORD;
			break;
		case(Dns.SRV):
			className = SRVRECORD;
			break;
		case(Dns.NAPTR):
			className = NAPTRRECORD;
			break;
		case(Dns.OPT):
			className = OPTRECORD;
			break;
		default:
			break;
	
		}
		return className;
	}
	protected String classNameAsString() {
		String className = null;
		switch (_type){
		case(Dns.A):
			className = ARECORD;
			break;
		case(Dns.NS):
			className = NSRECORD;
			break;
		case(Dns.CNAME):
			className = CNAMERECORD;
			break;
		case(Dns.SOA):
			className = SOARECORD;
			break;
		case(Dns.PTR):
			className = PTRRECORD;
			break;
		case(Dns.AAAA):
			className = AAAARECORD;
			break;
		case(Dns.SRV):
			className = SRVRECORD;
			break;
		case(Dns.NAPTR):
			className = NAPTRRECORD;
			break;
		case(Dns.OPT):
			className = OPTRECORD;
			break;
		default:
			break;
	
		}
		return className;
	}
	
    public static Class className(Short type) throws ClassNotFoundException {
		
		String className = "com.ibm.ws.sip.channel.resolver.dns.impl.";
		Class clazz = null;
		
		className += ResourceRecord.classNameAsString(type);
		try {
		  clazz = Class.forName(className);
		}
		catch (ClassNotFoundException cnfe){
			System.out.println("className exception" + cnfe.getMessage());
		}
		
		return clazz;
	
	}
	
	protected Class className() throws ClassNotFoundException {
		
		String className = "com.ibm.ws.sip.channel.resolver.dns.impl.";
		className += this.classNameAsString();
		return Class.forName(className);
	}
	
	public String toString(){
		String s = new String();
		s = "    Resource Record: " + Dns.TYPESTRING[_type] + "\n";
		s +="      domain name: " + _name.toString() + "\n" +
			"      type:  " + (new Short(_type)).toString() + "("+ Dns.TYPESTRING[_type]+")" +"\n" +
			"      class:  " + (new Short(_class)).toString() +  "\n" +
			"      ttl:  " + (new Integer(_ttl)).toString() + "\n" +
			"      rdlength:  " + (new Short(_rdLength)).toString() + "\n";
		
		return s;
	}
	
	
	
}
