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
package com.ibm.ws.sip.properties;

public class SipPropertyEntry {
	
	/**
	 * The Custom Property Name
	 */
	protected String _key = "";
	
	/**
	 * The property value
	 */
	protected Object _value = "";
	
	/**
	 * The property source
	 */
	protected CustPropSource _source = CustPropSource.DEFAULT;
	
	/**
	 * The WCCM Attribute Name, if any
	 */
	protected String _wccmAttrName = "";
	
	public SipPropertyEntry(String key, Object value, CustPropSource source, String wccmAttrName) {
		super();
		this._key = key;
		this._value = value;
		this._source = source;
		this._wccmAttrName = wccmAttrName;
	}

	public Object getValue() {
		return _value;
	}
	
	public void setValue(Object value) {
		_value = value;
	}
	
	public CustPropSource getSource() {
		return _source;
	}
	
	public String getWccmAttrName() {
		return _wccmAttrName;
	}

	public String getKey() {
		return _key;
	}

	public void setSource(CustPropSource source) {
		_source = source;
	}

	@Override
	public String toString() {
		StringBuffer buff = new StringBuffer();
		
		buff.append("Custom property name ");
		buff.append('[' + _key + ']' + '\t');
		buff.append("value ");
		buff.append('[' + _value.toString() + ']' + '\t');
		if(!_wccmAttrName.equals("")){
			buff.append("WCCM attribute name ");
			buff.append('[' + _wccmAttrName + ']' + '\t');
		}
		buff.append("source ");
		buff.append('[' + _source.toString() + ']');		
		return buff.toString();
	}
	
}

