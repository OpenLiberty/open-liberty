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
package com.ibm.ws.sip.container.tu;

import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.servlets.SipServletMessageImpl.MessageType;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;

/**
 * Class that represents a key for unigue dialog ID.
 * @author anat
 * @update moti 29/1/2009 fix memory leak 
 *
 */
	public class TUKey implements Cloneable
	{
	
	/**
	 * Class Logger.
	 */
	private static final transient LogMgr c_logger = Log.get(TUKey.class);
	
	/**
	 * Empty String constant
	 */
	private static final String EMPTY_STRING = "";
    
	/**
	 * String that represents tag_1 of the dialog.
	 * In UAS and UAC will be a localTag and in Proxy mode
	 * will represent remoteFrom tag of the INITIAL request.
	 */
	String _tag_1 = null;
	
	/**
	 * String that represents tag_2 of the dialog.
	 * In UAS and UAC will be a remoteTag and in Proxy mode
	 * will represent remoteTo tag of the INITIAL request.
	 */
	String _tag_2 = null;
	
	
	/**
	 * This string is usually representing CallId, but in Proxy mode
	 * it will be a ibmsid string from RecordRoute header.
	 */
	String _key = null;
	
	/**
	 * Will be true when we don't know which one is local tag : ToTag or FromTag 
	 */
	boolean _isExchangable = false;
	
	/**
	 * Helper method which sets the parameters
	 * @param tag_1
	 * @param tag_2
	 * @param key Can represent a CallId or RouteHeader app acts as a Proxy.
	 */
	public void setup(String tag_1,String tag_2,String key , boolean isExchangable){
		_tag_1 = tag_1;
		_tag_2 = tag_2;
		_key = key;	
		_isExchangable = isExchangable;
		
		if(c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug(null, "setup", "New key tag_1=" + _tag_1 + 
												" tag_2=" + _tag_2 +
												" key=" + key +
												" isExchangable =" + isExchangable);
		}
	}
	
	/**
	 * Method which cleans this TUKey object and returns it 
	 * back to the pool
	 */
	protected void cleanKey() {
		_tag_1 = null;
		_tag_2 = null;
		_key = null;
		_isExchangable = false;		
	}

	/**
	 * Method used for Proxy mode for INCOMING request
	 * @param request
	 * @param type
	 * @param sessionId session ID extraced from the Route header
	 */
	public void setParams(SipServletRequestImpl request, String sessionId, MessageType type){
		Request req = request.getRequest();
		if(type == MessageType.INCOMING_REQUEST){
			setup(req.getToHeader().getTag(),req.getFromHeader().getTag(),sessionId,true);
		}
		else{
			setup(req.getFromHeader().getTag(),req.getToHeader().getTag(),sessionId,true);
		}
	}
	
	/**
	 * Method used for Proxy mode for INCOMING responses
	 * @param request
	 * @param type
	 * @param sessionId session ID extraced from the Route header
	 */
	public void setParams(Response resp, String key ,boolean isProxy){
		if(isProxy){
			setup(resp.getFromHeader().getTag(),resp.getToHeader().getTag(),key,true);
		}
		else{
			setup(resp.getFromHeader().getTag(),resp.getToHeader().getTag(),key,false);
		}
	}
	
	/**
	 * Method used for UAC/UAS mode for INCOMING request
	 * @param request
	 * @param type
	 */
	public void setParams(SipServletRequestImpl request, MessageType type){
		Request req = request.getRequest();
		if(type == MessageType.INCOMING_REQUEST){
			setup(req.getToHeader().getTag(),req.getFromHeader().getTag(),request.getCallId(),false);
		}
		else if(type == MessageType.OUTGOING_REQUEST){
			setup(req.getToHeader().getTag(),req.getFromHeader().getTag(),request.getCallId(),false);
		}
	}
	
	/**
	 * Method used for UAC/UAS mode for INCOMING request
	 * @param request
	 * @param type
	 */
	public void setParams(Response response, MessageType type){
		if(type == MessageType.OUTGOING_RESPONSE){
			setup(response.getToHeader().getTag(),response.getFromHeader().getTag(),response.getCallIdHeader().getCallId(),false);
		}
		else if(type == MessageType.INCOMING_RESPONSE){
			setup(response.getFromHeader().getTag(),response.getToHeader().getTag(),response.getCallIdHeader().getCallId(),false);
		}
	}
	
	
	/**
	 * Hash code overide.
	 */
	public int hashCode()
	{
		int result = 0 ;
		//a note from Moti: Strings hash codes are valurnable.
		// in java they don't have uniform distribution.
		// which result that s1.hashcode() sometimes equals s2.hashcode()
		
		if (_tag_1 != null) result ^= _tag_1.hashCode();
		if (_tag_2 != null) result ^= _tag_2.hashCode();
		if (_key != null) result ^= _key.hashCode();
		return result;
	}
	
	 /**
	  *  @see java.lang.Object#equals(java.lang.Object)
	  */
	public boolean equals(Object obj){
		if (this == obj){
			return true;
		}
		
		if(obj == null){
			return false;
		}
		
		TUKey other = (TUKey)obj;
		
		if(!_key.equals(other._key)){
			return false;
		} 
		
		if(_isExchangable != other._isExchangable){
			return false;
		}
		
		if(_isExchangable ){
			//the second tag can be null in proxy when it is taken from the 
			//TU destination member (the remote To tag)and it is not set yet. 
			//in this case we still want to compare the TUkey so we will set it to
			//empty String to avoid null comparison
			String tag2 = (_tag_2 == null) ? EMPTY_STRING : _tag_2;
			String otherTag2 = (other._tag_2 == null) ? EMPTY_STRING : other._tag_2;
			
			String tag1 = (_tag_1 == null) ? EMPTY_STRING : _tag_1;
			String otherTag1 = (other._tag_1 == null) ? EMPTY_STRING : other._tag_1; 
			
			//In case when Proxy mode - we should check both sides.
			if(	tag1.equals(otherTag1) && tag2.equals(otherTag2) ||
				tag1.equals(otherTag2) && tag2.equals(otherTag1)) {
				return true;
			}
		}else{
			if(_tag_1 != null && !_tag_1.equals(other._tag_1)) {
				return false;
			} else if (_tag_1 == null && other._tag_1 != null){
				return false;
			}
					
			// There are some cases when we can ask to find TUKey when
			// remote is null But in this case both should be nulls
			if(_tag_2 == null && other._tag_2 != null || 
					other._tag_2 == null && _tag_2 != null){
				return false;
			}else if(_tag_2 == null && other._tag_2 == null){
				return true;
			}else if(!_tag_2.equals(other._tag_2)){
				return false;
			} 
		}
		
		return true;
	 }

	/**
	 * Set Tag_2  = sets remote tag.
	 * @param tag_2
	 */
	public void setTag_2(String tag_2) {
		this._tag_2 = tag_2;
	}

	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append('[');
		buff.append(_tag_1);
		buff.append(',');
		buff.append(_tag_2);
		buff.append(',');
		buff.append(_key);
		buff.append(',');
		buff.append(_isExchangable);
		buff.append(']');
		return buff.toString();
	}

	public String get_tag_2() {
		return _tag_2;
	}

	public TUKey clone() throws CloneNotSupportedException {
		TUKey newkey = new TUKey();
		newkey._isExchangable = _isExchangable;
		newkey._key = _key;
		newkey._tag_1 = _tag_1;
		newkey._tag_2 = _tag_2;
		return newkey;
	}
	
	
}
