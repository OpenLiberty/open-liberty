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
package com.ibm.ws.sip.properties;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.container.properties.PropertiesStore;

/**
 * This class is a data structure for holding a specific DS properties. 
 * It provides helper methods that will return the global default (as set on CoreProperties of StackProperties) when no value was set on server.xml, and will provide a way to tell if a certain 
 * property has changed by the user (either from the default on when in DS 'activate()' or from what was previously set by the user when 'modified()' is called), thus helping the DS decide what action needs to be taken.    
 * Note that this class is not meant to be thread safe
 * 
 * @author Nitzan Nissim
 */
public class DeclarativeServiceProperties {
	private static final TraceComponent tc = Tr.register(DeclarativeServiceProperties.class);
	
	/**
	 * Configuration properties received for this service
	 */
	private Map<String, Object> properties;
	
	/**
	 * Changes set from defaults
	 */
	private Map<String, Object> changes;
	
	/**
	 * Global property store
	 */
	private SipPropertiesMap sipPropertyMap = PropertiesStore.getInstance().getProperties();
	
	/**
	 * Ctor
	 * @param dsProperties the declarative service properties received on the Activate or Modify methods
	 */
	public DeclarativeServiceProperties(Map<String, Object> dsProperties){
		this.properties = dsProperties;
	}
	
	/**
	 * Check whether this property was set
	 * @param key
	 * @return
	 */
	private Object validateProperty(String key){
		Object prop = properties.get(key);
		if(prop == null){
			 if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
		            Tr.debug(tc, "DeclarativeServiceProperties validateProperty. property key="+key +" not found", properties);
		}
		
		return prop;
	}
	
	/**
	 * get a string property. 
	 * @param key
	 * @param useGlobalDefault
	 * @return the property value in the DS configuration, or if not exists its 
	 * default as set on the global store
	 */
	public String getString( String key, boolean useGlobalDefault){
		return get(key, sipPropertyMap.getString(key), useGlobalDefault);
	}
	
	/**
	 * get a integer property. 
	 * @param key
	 * @param useGlobalDefault
	 * @return the property value in the DS configuration, or if not exists its 
	 * default as set on the global store
	 */
	public int getInt( String key, boolean useGlobalDefault){
		return getInt(key, sipPropertyMap.getInt(key), useGlobalDefault);
	}
	
	/**
	 * get a long property. 
	 * @param key
	 * @param useGlobalDefault
	 * @return the property value in the DS configuration, or if not exists its 
	 * default as set on the global store
	 */
	public long getLong( String key, boolean useGlobalDefault){
		return getLong(key, sipPropertyMap.getLong(key), useGlobalDefault);
	}
	
	/**
	 * get a boolean property. 
	 * @param key
	 * @param useGlobalDefault
	 * @return the property value in the DS configuration, or if not exists its 
	 * default as set on the global store
	 */
	public boolean getBoolean( String key, boolean useGlobalDefault){
		return getBoolean(key, sipPropertyMap.getBoolean(key), useGlobalDefault);
	}
	

	/**
	 * get a string property. 
	 * @param key
	 * @param useDefault
	 * @return the property value in the DS configuration, or the default passed
	 * if not exists
	 */
	private String get( String key, String defaultValue, boolean useDefault){
		Object value = validateProperty(key);
		return (value == null && useDefault) ? defaultValue : value.toString(); 
	}
	
	/**
	 * get a integer property. 
	 * @param key
	 * @param useDefault
	 * @return the property value in the DS configuration, or the default passed
	 * if not exists
	 */
	private int getInt( String key, int defaultValue, boolean useDefault){
		Object value = validateProperty(key);
		return (value == null && useDefault) ? defaultValue : (Integer)value; 
	}
	

	/**
	 * get a long property. 
	 * @param key
	 * @param useDefault
	 * @return the property value in the DS configuration, or the default passed
	 * if not exists
	 */
	private long getLong( String key, long defaultValue, boolean useDefault){
		Object value = validateProperty(key);
		return (value == null && useDefault) ? defaultValue : (Long)value; 
	}
	

	/**
	 * get a boolean property. 
	 * @param key
	 * @param useDefault
	 * @return the property value in the DS configuration, or the default passed
	 * if not exists
	 */
	private boolean getBoolean( String key, boolean defaultValue, boolean useDefault){
		Object value = validateProperty(key);
		return (value == null && useDefault) ? defaultValue : (Boolean)value;		 
	}
	
		
	/**
	 * Store changes in changes set
	 * @param key
	 * @param oldValue
	 */
	void storeChangedFromValue(String key, Object oldValue){
		if( changes == null){
			changes = new HashMap<String, Object>(); 
		}
		changes.put(key, oldValue);
	}
	
	/**
	 * Returns iterable set of modified properties
	 * @return
	 */
	public Set<String> getUpdatedProperties(){
		return changes.keySet();
	}
	
	/**
	 * Check whether the property has changed from what was set previously (or the global default)
	 * @param key
	 * @return
	 */
	public boolean wasChanged(String key){
		return changes.get(key) != null;
	}
	
	/**
	 * The value of that property that was set previously, or null if no change was done
	 * @param key
	 * @return
	 */
	public Object previousValue(String key){
		return changes.get(key);
	}
}
