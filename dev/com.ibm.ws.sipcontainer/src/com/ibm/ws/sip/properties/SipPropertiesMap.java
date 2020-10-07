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

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;

/**
 * The <code>SipPropertiesMap</code> class wrap a java.util.Properties class
 * and insert some useful methods, specific for Sip Custom properties 
 */
public class SipPropertiesMap {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2474724823492890831L;
	
	/**
	 * Class Logger. 
	 */
	private static final transient LogMgr c_logger = Log
			.get(SipPropertiesMap.class);
	
	/**
     * Hold properties
     */
    protected Properties _properties = null;
    
    /**
     * CTOR
     */
    public SipPropertiesMap() {
		_properties = new Properties();
	}
			
	/**
	 * @param key
	 * @param value
	 * @param source
	 * @param wccmAttrName
	 */
	private void setEntry(String key, Object value,CustPropSource source, String wccmAttrName){
		SipPropertyEntry entry = new SipPropertyEntry(key,value,source,wccmAttrName);
		_properties.put(key, entry);
	}
	
	/**
	 * @param key
	 * @param value
	 * @param source
	 * @param wccmAttrName
	 */
	public final SipPropertyEntry getEntry(String key){
		return (SipPropertyEntry)_properties.get(key);
	}
	
	/**
	 * @param key
	 * @param value
	 * @param source
	 */
	public void setString(String key, String value, CustPropSource source){
		setEntry(key,value,source,"");
	}
	
	/**
	 * @param key
	 * @param value
	 * @param wccmAttrName
	 */
	public void setString(String key, String value,String wccmAttrName){
		setEntry(key,value,CustPropSource.WCCM,wccmAttrName);
	}
	
	/**
	 * @param key
	 * @param value
	 * @param wccmAttrName
	 */
	public void setObject(String key, Object value, CustPropSource source,String wccmAttrName){
		setEntry(key,value,source,wccmAttrName);
	}
	
	/**
	 * @param key
	 * @param value
	 * @param wccmAttrName
	 */
	public void setObject(String key, Object value, CustPropSource source){
		setEntry(key,value,source,"");
	}
	
	/**
	 * @param key
	 * @param value
	 * @param source
	 */
	public void setInt(String key, int value, CustPropSource source){
		setEntry(key,new Integer(value),source,"");
	}
	
	/**
	 * @param key
	 * @param value
	 * @param wccmAttrName
	 */
	public void setShort(String key, short value,String wccmAttrName){
		setEntry(key,new Short(value),CustPropSource.WCCM,wccmAttrName);
	}
	
	/**
	 * @param key
	 * @param value
	 * @param source
	 */
	public void setShort(String key, short value, CustPropSource source){
		setEntry(key,new Short(value),source,"");
	}
	
	/**
	 * @param key
	 * @param value
	 * @param wccmAttrName
	 */
	public void setInt(String key, int value,String wccmAttrName){
		setEntry(key,new Integer(value),CustPropSource.WCCM,wccmAttrName);
	}
	
	/**
	 * @param key
	 * @param value
	 * @param source
	 */
	public void setBoolean(String key, boolean value, CustPropSource source){
		setEntry(key,new Boolean(value),source,"");
	}
	
	/**
	 * @param key
	 * @param value
	 * @param wccmAttrName
	 */
	public void setBoolean(String key, boolean value,String wccmAttrName){
		setEntry(key,new Boolean(value),CustPropSource.WCCM,wccmAttrName);
	}
	
	/**
	 * @param key
	 * @param value
	 * @param source
	 */
	public void setLong(String key, long value, CustPropSource source){
		setEntry(key,new Long(value),source,"");
	}
	
	/**
	 * Return int property value
	 * @param key
	 * @return the value, or -1 by default
	 */
	public int getInt(String key){
		SipPropertyEntry entry = (SipPropertyEntry)_properties.get(key);
		int retValue = -1;
		if(entry!=null){
			try{
			Object oVal = entry.getValue();
			if (oVal instanceof String){
				retValue = Integer.parseInt( (String)oVal );
			}else {				
				Integer value = (Integer)oVal;
				retValue = value.intValue();
			}
			}catch( Exception exp ){
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "getInt", 
					"got Exception, retrieving value: " + retValue + " " + exp);
				}
			}
		} else{
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getInt", "could not find value for: "+
				key + " ,retrieving value: " + retValue);
			}
		}
		return retValue;
	}
	
	/**
	 * Return short property value
	 * @param key
	 * @return the value, or -1 by default
	 */
	public short getShort(String key){	
		SipPropertyEntry entry = (SipPropertyEntry)_properties.get(key);
		short retValue = -1;
		if(entry!=null){
			try{
			Object oVal = entry.getValue();
			if (oVal instanceof String){
				retValue = Short.parseShort( (String)oVal );
			}else {				
				Short value = (Short)entry.getValue();
				retValue = value.shortValue();
			}
			}catch( Exception exp ){
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "getShort", 
					"got Exception, retrieving value: " + retValue + " " + exp);
				}
			}
		} else{
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getShort", "could not find value for: "+
				key + " ,retrieving value: " + retValue);
			}
		}
		return retValue;
	}
	
	/**
	 * Return long property value
	 * @param key
	 * @return the value, or -1 by default
	 */
	public long getLong(String key){		
		SipPropertyEntry entry = (SipPropertyEntry)_properties.get(key);
		long retValue = -1;
		if(entry!=null){
			try{
			Object oVal = entry.getValue();
			if (oVal instanceof String){
				retValue = Long.parseLong( (String)oVal );
			}else {				
				Long value = (Long)entry.getValue();
				retValue = value.longValue();
			}
			}catch( Exception exp ){
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "getLong", 
					"got Exception, retrieving value: " + retValue + " " + exp);
				}
			}
		} else{
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getLong", "could not find value for: "+
				key + " ,retrieving value: " + retValue);
			}
		}
		return retValue;
	}
	
	/**
	 * Return duration property value
	 * @param key
	 * @return the value, or -1 by default
	 */
	public int getDuration(String key){		
		SipPropertyEntry entry = (SipPropertyEntry)_properties.get(key);
		int retValue = -1;
		
		if (entry != null){
			try {
				long longValue;
				
				Object oVal = entry.getValue();
				if (oVal instanceof String){
					longValue = Long.parseLong( (String)oVal );
				} else {
					Long value = (Long)entry.getValue();
					longValue = value.longValue();
				}
				
				if (longValue <= Integer.MAX_VALUE) {
					retValue = (int)longValue;
				} else {
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "getDuration", 
						"the duration for " + key +" is greater than max integer, " +
								"retrieving value: " + retValue);
					}
				}
			} catch (Exception exp) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "getDuration", 
					"got Exception, retrieving value: " + retValue + " " + exp);
				}
			}
		} else {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getDuration", "could not find value for: "+
				key + " ,retrieving value: " + retValue);
			}
		}
		return retValue;
	}
	
	/**
	 * Return String property value
	 * @param key
	 * @return the value, or "" String by default
	 */
	public String getString(String key){
		return getString(key,false);
	}
	
	/**
	 * @param key
	 * @param forceString flag that indicate if the required returned value
	 * 	must be String
	 * 
	 * @return String value or "" String, if the property doesn't exist
	 */
	public String getString(String key, boolean forceString){
		// TODO do we want to read first the property value from the System ?
		
		SipPropertyEntry entry = (SipPropertyEntry)_properties.get(key);
		String strValue = "";
		if(entry!=null){
			Object object = entry.getValue();
			
			if(object instanceof String){
				strValue = (String) object;
			} else if (forceString){
				if (c_logger.isTraceEntryExitEnabled()) {
					Object[] params = { key, forceString };
					c_logger.traceEntry(SipPropertiesMap.class.getName(), "getString",
							params);
				}
				
				// create String value from any type
				strValue = object.toString();
			}
		}
		return strValue;
	}
	
	/**
	 * Return boolean property value
	 * @param key
	 * @return the value, or false by default
	 */
	public boolean getBoolean(String key){
		SipPropertyEntry entry = (SipPropertyEntry)_properties.get(key);
		boolean retValue = false;
		if(entry!=null){
			try{
			Object oVal = entry.getValue();
			if (oVal instanceof String){
				retValue = Boolean.valueOf( (String)oVal ).booleanValue();
			}else {				
				Boolean value = (Boolean)entry.getValue();
				retValue = value.booleanValue();
			}
			} catch( Exception exp ){
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "getBoolean", 
					"got Exception, retrieving value: " + retValue + " " + exp);
				}
			}
		} else{
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getBoolean", "could not find value for: "+
				key + " ,retrieving value: " + retValue);
			}
		}
		return retValue;
	}
	
	/**
	 * @param key
	 * @return
	 */
	public Object getObject(String key){
		SipPropertyEntry entry = (SipPropertyEntry)_properties.get(key);
		if(entry == null){
			return null;
		}
		return entry.getValue();
	}
	
	/**
	 * @param key
	 * @param value
	 * @param source
	 */
	public void remove(String key){
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { key };
			c_logger.traceEntry(SipPropertiesMap.class.getName(), "remove",
					params);
		}
		_properties.remove(key);
	}
	
	/**
	 * Print out the custom properties according to the given source
	 * @param source
	 */
	public String logProprs(CustPropSource source){
		StringBuffer buff = new StringBuffer(1000);
		for (Iterator iter = _properties.keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			SipPropertyEntry element = (SipPropertyEntry)_properties.get(key);

			if(element.getSource()==source){
				buff.append('\n');
				buff.append(element.toString());
			}
		}
		
		return buff.toString();
	}

	/**
	 * Copy all properties from the given properties to this SipPropertiesMap.
	 * The type that will be used is according to the default entry, if exists.
	 * By default the value would be String.
	 * 
	 * @param prop
	 * @param The CustPropSource
	 */
	public void putAll(Properties prop, CustPropSource source) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(SipPropertiesMap.class.getName(), "putAll");
		}
		
		for (Iterator iter = prop.keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			String newValue = prop.getProperty(key);
			
			set(key, newValue,source);
		}
	}

	/**
	 * Set new value to existing type if exist.
	 * By default String value would be created.
	 * 
	 * @param key
	 * @param newValue
	 * @param custom
	 */
	public void set(String key, String newValue, CustPropSource custom) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { key, newValue, custom };
			c_logger
					.traceEntry(SipPropertiesMap.class.getName(), "set", params);
		}
		
		SipPropertyEntry entry = (SipPropertyEntry)_properties.get(key);
		if(entry!=null){
			// replace original value for this entry
			Object obj = entry.getValue();
			
			if(obj instanceof Integer) {
				try {						
					Integer integerVal = Integer.parseInt(newValue);
					
					// set new value as Integer
					entry.setValue(integerVal);
					entry.setSource(CustPropSource.CUSTOM);
					
				} catch (NumberFormatException e) {
					// if it is not number print error
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "putAll", 
						"cant't load Integer value property: " + key + " value: " + newValue);
					}
					return;
				}
			}else if(obj instanceof Long) {
				try {						
					Long longVal = Long.parseLong(newValue);
					
					// set new value as Long
					entry.setValue(longVal);
					entry.setSource(CustPropSource.CUSTOM);
					
				} catch (NumberFormatException e) {
					// if it is not number print error
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "putAll", 
						"cant't load Long value property: " + key + " value: " + newValue);
					}
					return;
				}
			} else if(obj instanceof Short) {
				try {						
					Short shortVal = Short.parseShort(newValue);
					
					// set new value as Long
					entry.setValue(shortVal);
					entry.setSource(CustPropSource.CUSTOM);
					
				} catch (NumberFormatException e) {
					// if it is not number print error
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "putAll", 
						"cant't load Short value property: " + key + " value: " + newValue);
					}
					return;
				}
			} else if(obj instanceof Boolean) {
				Boolean boolVal = true;
				if(newValue.equalsIgnoreCase("false")){
					boolVal = false;
				} else if(!newValue.equalsIgnoreCase("true")){
					//if it is not true and not false print error
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "putAll", 
						"cant't load Boolean value property: " + key + " value: " + newValue);
					}
					return;
				}

				// set new value as Boolean
				entry.setValue(boolVal);
				entry.setSource(CustPropSource.CUSTOM);
				
			} else {
				// by default set new value as String
				entry.setValue(newValue);
				entry.setSource(CustPropSource.CUSTOM);
			}
			
		} else {
			// create new entry
			entry = new SipPropertyEntry(key, newValue, CustPropSource.CUSTOM, "");
			_properties.put(key, entry);
		}
	}

	/**
	 * Create new properties instance and copy all properties with String values 
	 */
	public Properties copyProps() {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(SipPropertiesMap.class.getName(), "copyProps");
		}
		
		Properties prop = new Properties();
		
		for (Iterator iter = _properties.keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			SipPropertyEntry element = (SipPropertyEntry)_properties.get(key);
			
			if(!element.getValue().equals("")){
				prop.put(key, element.getValue().toString());
			}
		}
		
		return prop;
	}
	
	/**
	 * This method verify property value is one of several values. If not, it fixes it to a specified value.
	 * 
	 * @param key property key
	 * @param values array of legal values for property (null is not a valid value)
	 * @param fixValue set this value in case an illegal value is found.
	 */
	public void validateAndFixProperty(String key, String[] values, String fixValue) {
		if (values == null || values.length == 0) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "validateAndFixProperty", "Skipping validation for key: " + key);
			}
			return;
		}
		String value = getString(key);

		if (value == null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "validateAndFixProperty", "key wans't found.");
			}
			return;
		}

		boolean found = false;

		for (int i = 0; i < values.length && !found; i++) {
			if (values[i] == null) {
				throw new NullPointerException("Illegal value specified.");
			}
			if (value.equals(values[i])) {
				found = true;
			}
		}

		if (!found) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "validateAndFixProperty", "Invalid value retrieved: " + value + " replacing with: " + fixValue);
			}		   

			setString(key, fixValue,CustPropSource.DEFAULT);
		}
	}
	
	/**
	 * Updating the container properties with those received in a declarative service 
	 * @param properties DS configuration properties
	 * @return
	 */
	//TODO Liberty this needs to be thread safe
	public DeclarativeServiceProperties updateProperties( Map<String, Object> properties){
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "updateProperties", properties);
		}
		DeclarativeServiceProperties pHolder = new DeclarativeServiceProperties(properties);
		
		for(Iterator<String> itr = properties.keySet().iterator(); itr.hasNext(); ){
			String key = itr.next();
			Object oldValue = getObject(key);
			Object value = properties.get(key);
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "updateProperties", "Debug: Key="+ key + 
							" value=" + value + " replacing : " + oldValue);
			}
			if(oldValue != null && !oldValue.equals(value)){
				setObject(key, value, CustPropSource.CONFIG_FILE);
				pHolder.storeChangedFromValue(key, oldValue);
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "updateProperties", 
							"key: " + key + 
							" modified from: " + oldValue + 
							" to:" + value);
				}
			}
		}
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "updateProperties", pHolder);
		}
		return pHolder;
	}
}