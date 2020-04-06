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

import java.nio.BufferUnderflowException;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;

/**
 * Class to represent a DNS message as denoted by RFC 1035
 */
 public class DnsMessage {
	
	/** fixed 12 byte header */ 
	private Header _header;
	/** variable length section of questions */
	private Vector<Question> _questions;
	/** variable length section of answers resource records */
	private Vector<ResourceRecord> _answers;
	/** variable length section of authority resource records */
	private Vector<ResourceRecord> _authority;
	/** variable length section of authority resource records */
	private Vector<ResourceRecord> _additional;
	
	private static final AtomicInteger _nextId = new AtomicInteger(1);
	
	private static final char[] HEX  = {'0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
	
	/**
	 * contructor used to create dns queries 
	 * @param type type of query
	 * @param qname name to query
	 */
	public DnsMessage(short type, String qname){
		/** construct the header */
		_header = new Header();
		/** setup the header */
		_header.identification = nextId();
		
		_header.numQuestions = 1;
		_header.numAnswers = 0;
		_header.numAuthority = 0;
		_header.numAdditional = 0;
		
		/** recursion desired */
		_header.flags = 0x0100;
		_header.bitSetFlags.set(7);
		
		/** construct the query name */
		Name n = new Name(qname);
		/** construct the question */
		Question q = new Question(type,n);
		
		/** create the question vector */
		_questions = new Vector<Question>(1);
		
		/** add the question to the vector */
		_questions.add(q);
		
		_answers = new Vector<ResourceRecord>(0);
		_additional = new Vector<ResourceRecord>(1);
		_authority = new Vector<ResourceRecord>(0);		
	}
	
	/**
	 * gets the next query ID, and increments the ID counter
	 * @return the next query ID
	 */
	private int nextId() {
		int oldId, newId;
		boolean done;

		do {
			oldId = _nextId.get();
			newId = (oldId + 1) & 0xffff; // limit to 16 bit
			done = _nextId.compareAndSet(oldId, newId);
		} while (!done);

		return oldId;
	}	
	
	/**
	 * contructor used to create dns queries 
	 * @param type type of query
	 * @param qname name to query
	 * @param eDNS boolean to indicate whether extended DNS support is available 
	 */
	public DnsMessage(short type, String qname, boolean eDNS){
		this(type,qname);
		
		/** construct the OPTRecord which contains the eDNS information */
		ResourceRecord record = ResourceRecord.createRecord(Dns.OPT);
		
		_additional.add(record);	
		_header.numAdditional++;
		
	}
	
	public int getId(){
		return _header.identification;
	}
	
	public short getQclass() {
		return _questions.firstElement().qclass;
	}
	
	public short getQtype() {
		return _questions.firstElement().qtype;
	}
	
	public WsByteBuffer toBuffer(WsByteBufferPoolManager wsByteBufferPoolManager){
		WsByteBuffer buffer = wsByteBufferPoolManager.allocate(1024);
		
		/** mark the beginning of the buffer */
		buffer.mark();
		
		/** place the header in the buffer */
		//System.out.println("*** identification = " + _header.identification);
		//X CR 4564 
		buffer.put((byte)((_header.identification >>> 8) & 0x000000ff));
		buffer.put((byte)(_header.identification & 0x000000ff));
		_header.bitSetToShort();
		buffer.putShort(_header.flags);
		buffer.putShort(_header.numQuestions);
		buffer.putShort(_header.numAnswers);
		buffer.putShort(_header.numAuthority);
		buffer.putShort(_header.numAdditional);
		
		/** place the question(s) in the buffer */
		for (Enumeration e = _questions.elements(); e.hasMoreElements();){
			Question q = (Question)e.nextElement();
			q.qname.toBuffer(buffer);
			buffer.putShort(q.qtype);
			buffer.putShort(q.qclass);
		}
		
		/** place the answer(s) in the buffer */
		for (Enumeration e = _answers.elements(); e.hasMoreElements();){
			ResourceRecord rr  = (ResourceRecord)e.nextElement();
			try {
				Class clazz = rr.className();
				clazz.cast(rr);
				rr.toBuffer(buffer);
			}
			catch (Exception ex ){
			}
		}
		
		/** place the authority(s) in the buffer */
		for (Enumeration e = _authority.elements(); e.hasMoreElements();){
			SOARecord rr  = (SOARecord)e.nextElement();
			rr.toBuffer(buffer);
		}
		
		/** place the addtional(s) in the buffer */
		for (Enumeration e = _additional.elements(); e.hasMoreElements();){
			ResourceRecord rr  = (ResourceRecord)e.nextElement();
			try {
				Class clazz = rr.className();
				clazz.cast(rr);
				rr.toBuffer(buffer);
			}
			catch (Exception ex ){
			}
		}
		
		/** mark the end of the buffer */
		buffer.limit(buffer.position());
		/** reset position back to the beginning */
		buffer.reset();
		
		return buffer;
	}
	
	/**
	 * used to construct a Dns Response
	 * @param buffer
	 * @throws Exception
	 */
	public DnsMessage(DnsMessage m) throws Exception {
		
		try {
			
			_questions  = new Vector<Question>(1);
			_answers    = new Vector<ResourceRecord>(4);
			_authority  = new Vector<ResourceRecord>(4);
			_additional = new Vector<ResourceRecord>(4);
			
			_header = new Header();
			_header.identification = m._header.identification;
			
			/** set the Response and AA bits */
			_header.bitSetFlags.set(0);
			_header.bitSetFlags.set(5);
			/** set as a name error initially */
			_header.bitSetFlags.set(14);
			_header.bitSetFlags.set(15);
			
			Question question = new Question();
			question.qname = new Name(m._questions.firstElement().qname.toString());
			question.qtype = m._questions.firstElement().qtype;
			question.qclass = m._questions.firstElement().qclass;
						
			_questions.add(question);
			_header.numQuestions++;
		}	
		catch (Exception e ){
			System.out.println("DnsMessage:  Exception = " + e);
		throw e;
		}
		
	}

	public DnsMessage(WsByteBuffer buffer) throws Exception {
		
		try {
			
			_header = new Header();
			_header.parse(buffer);
			
			_questions  = new Vector<Question>(_header.numQuestions);
			_answers    = new Vector<ResourceRecord>(_header.numAnswers);
			_authority  = new Vector<ResourceRecord>(_header.numAuthority);
			_additional = new Vector<ResourceRecord>(_header.numAdditional);
			
			/** typically there is only 1 question */
			for (int i = 0; i < _header.numQuestions; i++){
				Question question = new Question(buffer);
				_questions.add(question);
			}
			
			ResourceRecord rr = null;
			/** Construct the RR's for the answer section */
			for (int j = 0; j < _header.numAnswers; j++){
				rr = ResourceRecord.createRecord(buffer);
				/** add the resource record to the vector */
				_answers.add(j,rr);
			}
			
			/** Construct the RR's for the authority section */
			for (int k = 0; k < _header.numAuthority; k++){
				rr = ResourceRecord.createRecord(buffer);
				/** add the resource record to the vector */
				_authority.add(k,rr);
			}
			
			/** Construct the RR's for the additional section */
			for (int l = 0; l < _header.numAdditional; l++){
				rr = ResourceRecord.createRecord(buffer);
				/** add the resource record to the vector */
				_additional.add(l,rr);
			}
		}
		catch (Exception e ){
			//System.out.println("DnsMessage:  Exception = " + e);
		throw e;
		}
		
	}
	
	public void addQuestion(ResourceRecord rr){
		_answers.add(rr);
		_header.numQuestions++;
	}
	
	public void addAnswer(ResourceRecord rr){
		_answers.add(rr);
		_header.numAnswers++;
	}
	
	public void addAuthority(ResourceRecord rr){
		_authority.add(rr);
		_header.numAuthority++;
	}
	
	public void addAdditional(ResourceRecord rr){
		_additional.add(rr);
		_header.numAdditional++;
	}

	public Name getQname(){
		return _questions.firstElement().qname;
	}
	
	public short numAnswers(){
		return _header.numAnswers;
	}
	
	public Vector<ResourceRecord> getAnswers() {
		return _answers;
	}
	
	public Vector<ResourceRecord> getAdditional() {
		return _additional;
	}
	
	protected void flagstoBitSetFlags(){
		/** set the bitSetFlags */
		for (int i = 0; i < 16; i++) {			
			if ((_header.flags & (1 << (15 - i))) != 0)	{
				_header.bitSetFlags.set(i);
	      	}
		}
	}
	protected boolean getQR(){
		if (_header.bitSetFlags.get(Dns.QR)){
			return true;
		}
		
		return false;
	}
	
	protected void setQR(){
		_header.bitSetFlags.set(Dns.QR);
	}
	
	protected boolean getAA(){
		if (_header.bitSetFlags.get(Dns.AA)){
			return true;
		}
		
		return false;
	}
	
	protected void setAA(){
		_header.bitSetFlags.set(Dns.AA);
	}
	
	public boolean getTC(){
		if (_header.bitSetFlags.get(Dns.TC)){
			return true;
		}
		
		return false;
	}
	
	protected void setTC(){
		_header.bitSetFlags.set(Dns.TC);
	}
	
	protected boolean getRD(){
		if (_header.bitSetFlags.get(Dns.RD)){
			return true;
		}
		
		return false;
	}
	
	protected void setRD(){
		_header.bitSetFlags.set(Dns.RD);
	}
	
	protected boolean getRA(){
		if (_header.bitSetFlags.get(Dns.RA)){
			return true;
		}
		
		return false;
	}
	
	protected void setRA(){
		_header.bitSetFlags.set(Dns.RA);
	}
	
	public int getRCODE(){
		int code= 0;
		
		for (int i = 0; i < 4; i++){
			if (_header.bitSetFlags.get(i+ Dns.RCODE0)){
				code = code | (1 << (3 - i));
			}
		}
		return code;
	}
	
	public void setRCODE(byte b){
		for (int i = 0; i < 4; i++){
			if(((int)b & ((int) 1 << (3 - i))) > 0){
				_header.bitSetFlags.set(i + Dns.RCODE0);
			}
			else {
				_header.bitSetFlags.clear(i + Dns.RCODE0);
			}
		}
	}
	
	public String toString(){
		
		String s = new String();
		
		s  = _header.toString();
		s += ((Question)_questions.firstElement()).toString();
		
		Enumeration e = null;
		
		for (e = _answers.elements(); e.hasMoreElements();){
			s += " Answer\n";
			ResourceRecord rr = (ResourceRecord)e.nextElement();
			try {
				Class clazz = rr.className();
				clazz.cast(rr);
				s += rr.toString();
			}
		   catch (ClassNotFoundException cnfe) {
			   System.out.println("DnsMessage::toString ClassNotFoundException" + cnfe.toString());
		   }
		}
		
		for (e = _authority.elements(); e.hasMoreElements();){
			s += " Authority\n";
			ResourceRecord rr = (ResourceRecord)e.nextElement();
			try {
				Class clazz = rr.className();
				clazz.cast(rr);
				s += rr.toString();
			}
		   catch (ClassNotFoundException cnfe) {
			   System.out.println("DnsMessage::toString ClassNotFoundException" + cnfe.toString());
		   }
		}
		
		for (e = _additional.elements(); e.hasMoreElements();){
			s += " Additional\n";
			ResourceRecord rr = (ResourceRecord)e.nextElement();
			try {
				Class clazz = rr.className();
				clazz.cast(rr);
				s += rr.toString();
			}
		   catch (ClassNotFoundException cnfe) {
			   System.out.println("DnsMessage::toString ClassNotFoundException" + cnfe.toString());
		   }
		}
		return s;
	}
	
/*	public String toHexString() {
		 WsByteBuffer buffer = this.toBuffer();
		 String s = new String();
		 for (int i = 0; i < 512; i++){
			byte b = buffer.get();
			char c = HEX[(b & 0xF0) >> 4];
			s += c;
			char d = HEX[(b & 0x0F)];
			s += d;	
		 }
		 return s;
	}
*/	
	public boolean isNameError(){
		
		boolean b = false;
		int rcode = _header.getRCODE();
		
		if (rcode == Dns.NAME_ERROR){
			b = true;
		}
		
		return b;
	}
	
	/**
	 * Class to represent the header portion of a DNS message
	 *
	 */
	 class Header{
		
		private int   identification;
		private short flags;
		private short numQuestions;
		private short numAnswers;
		private short numAuthority;
		private short numAdditional;
		
		private BitSet bitSetFlags = new BitSet(16);
		
		protected Header(){
		}
		
		protected void parse(WsByteBuffer buffer){
			
			try {
				identification  = (int)buffer.getShort() & 0x0000ffff;
				flags 			= buffer.getShort();
				numQuestions 	= buffer.getShort();
				numAnswers 	    = buffer.getShort();
				numAuthority 	= buffer.getShort();
				numAdditional 	= buffer.getShort();
				
				/** set the bitSetFlags */
				for (int i = 0; i < 16; i++) {			
					if ((flags & (1 << (15 - i))) != 0)	{
						bitSetFlags.set(i);
			      	}
				}
			}	
			catch (BufferUnderflowException e){
				throw e;				
			}
		}
		
		protected void bitSetToShort(){
			
			for (int i = 0; i < 16; i++) {			
				if (bitSetFlags.get(i))	{
					flags |= ( 1 << (15 - i));
		      	}
			}	
			
		}
		protected int getRCODE(){
			int code= 0;
			
			for (int i = 0; i < 4; i++){
				if (bitSetFlags.get(i+ Dns.RCODE0)){
					code = code | (1 << (3 - i));
				}
			}
			return code;
		}
		
		public String toString(){
			
			String s = new String();
			
			s = "Dns Message: " + (Dns.TYPESTRING[_questions.firstElement().qtype]).toString() + " ";
			if (bitSetFlags.get(0)) {
				s += "Response\n";
			}	
			else s+= "Query\n";
				
			s += " Header\n" + 
				"  identification: " + (new Integer(identification)).toString() + "\n" +
				"  Flags \n" +
				"    QR: " + bitSetFlags.get(Dns.QR) + "\n";
			
			short sh = 0;
			if (bitSetFlags.get(1)) { sh |= 0x1000; }
			if (bitSetFlags.get(2)) { sh |= 0x0100; }
			if (bitSetFlags.get(3)) { sh |= 0x0010; }
			if (bitSetFlags.get(4)) { sh |= 0x0001; }
			
			s += "    opcode: " + (new Short(sh)).toString() + "\n"  +
				"    AA: " + bitSetFlags.get(Dns.AA) + "\n" +
				"    TC: " + bitSetFlags.get(Dns.TC) + "\n" +
				"    RD: " + bitSetFlags.get(Dns.RD) + "\n" +
				"    RA: " + bitSetFlags.get(Dns.RA) + "\n";
			
			sh = 0;
			if (bitSetFlags.get(9))  { sh |= 0x100; }
			if (bitSetFlags.get(10)) { sh |= 0x010; }
			if (bitSetFlags.get(11)) { sh |= 0x001; }
			
			s += "    zero: " + (new Short(sh)).toString() + "\n";
			
			s += "    rcode: " + (new Integer(this.getRCODE())).toString() + "\n";
			
			s+= "  num Questions : " + (new Short(numQuestions)).toString() + "\n" +
				"  num Answers   : " + (new Short(numAnswers)).toString() + "\n" +
				"  num Authority : " + (new Short(numAuthority)).toString() + "\n" +
				"  num Additional: " + (new Short(numAdditional)).toString() + "\n";
			
			return s;
		}
	 }
	 /**
	  * 
	  * Class to represent the question portion of a Dns message
	  *
	  */
	 class Question {
		 private Name qname;
		 private short qtype;
		 private short qclass;
		 
		 protected Question(short type, Name name){
			 qname  = name;
			 qtype  = type;
			 qclass = Dns.IN;
		 }
		 
		 protected Question(){
			 qname  = null;
			 qtype  = 0;
			 qclass = Dns.IN;
		 }
		 
		 protected Question (WsByteBuffer buffer){
			 qname = new Name (buffer);
			 qtype = buffer.getShort();
			 qclass = buffer.getShort();
		 }
		 
		 public String toString(){
			 String s = new String();
			 s = " Question\n" +
			 	 "  qname: " + qname.toString() + "\n" +
			 	 "  qtype: " + (new Short(qtype)).toString() + 
			 	     "("+ (Dns.TYPESTRING[qtype]).toString() +")" + "\n" +
			 	 "  qclass:" + (new Short(qclass)).toString() + "\n";
			 return s;
		 }
		 
	 }
}
