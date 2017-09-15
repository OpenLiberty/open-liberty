/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.cache;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A ConfigEntry is a struct object that holds the
 * cache policies specifying how to cache an entry.
 * @ibm-spi 
 */
public class ConfigEntry {
   
	private com.ibm.ws.cache.config.ConfigEntry configEntry = null;
	
	public ConfigEntry(com.ibm.ws.cache.config.ConfigEntry ce){
		configEntry = ce;
	}
	
	/**
     * This method returns the name of cache policy definition.
     * The class name can be "command", "servlet", "webservice", "JAXRPCClient", 
     * "static" or "portlet".
     * 
	 * @return The class name 
	 */
	public String getClassName(){
		return configEntry.className;
	}
	
	/**
     * This method returns all the names of cacheable objects.
     * 
	 * @return The class name 
	 */
	public HashSet getAllNames(){
		return configEntry.allNames;
	}
	
	/**
     * This method returns the sharing policy.
     * 
	 * @return The sharing policy  
	 */
	public int getSharingPolicy(){
		return configEntry.sharingPolicy;
	}
	
	/**
     * This method returns a clone of this config entry.
     * 
	 * @return The config entry
	 */
	public Object clone(){
		return new ConfigEntry((com.ibm.ws.cache.config.ConfigEntry)configEntry.clone());
	}
	
	/**
     * This method returns a list of properties defined on a cache-entry
     * 
	 * @return A list of properties
	 */
	public ArrayList getProperties(){
		ArrayList properties = new ArrayList();
		if (configEntry.properties != null){
			Iterator it = configEntry.properties.values().iterator();
			while (it.hasNext()){
				Property property = new Property((com.ibm.ws.cache.config.Property)it.next());
				properties.add(property);
			}
		}
		return properties;
	}
	
	/**
     * This method returns an array of CacheId objects that contain cache ID generation rules 
     * used to produce a valid cache ID.
     * 
	 * @return Array of CacheId objects
	 */
	public CacheId[] getCacheIds(){
		CacheId[] cacheIds = new CacheId[configEntry.cacheIds.length];
		for ( int i=0; i<configEntry.cacheIds.length; i++ ){
			cacheIds[i] = new CacheId(configEntry.cacheIds[i]);
		}
		return cacheIds;
	}
	
	/**
     * This method returns an array of DependencyId objects that specified addditional cache 
     * indentifers that associated multiple cache entries to the same group identiifier. 
     * 
	 * @return Array of DependencyId objects
	 */
	public DependencyId[] getDependencyIds(){
		DependencyId[] depIds = new DependencyId[configEntry.dependencyIds.length];
		for ( int i=0; i<configEntry.dependencyIds.length; i++ ){
			depIds[i] = new DependencyId(configEntry.dependencyIds[i]);
		}
		return depIds;
	}
	
	/**
     * This method returns an array of Invalidation objects that written custom Java code or through  
     * rules that are defined in the cache policy of each entry.
     *  
	 * @return Array of Invalidation objects
	 */
	public Invalidation[] getInvalidations(){
		Invalidation[] invalidations = new Invalidation[configEntry.invalidations.length];
		for ( int i=0; i<configEntry.invalidations.length; i++ ){
			invalidations[i] = new Invalidation(configEntry.invalidations[i]);
		}
		return invalidations;
	}
	
	
	/**
	 * Property is defined optionally on a cacheable object. It provides a description of the 
	 * configured servlet, portlet or Web services. 
	 * It maps to a property definition for a cache-entry in the cachespec.xml. 
	 *
	 */
	public class Property {
		
		private com.ibm.ws.cache.config.Property property = null;
		
		public Property(com.ibm.ws.cache.config.Property prop){
			property = prop;
		}
		
		public String getName(){
			return property.name;
		}
		
		public String getValue(){
			return property.value;
		}
		
		public String[] getExcludeList(){
			return property.excludeList;
		}
		
	}
	
	/**
	 * The CacheId defines a rule for caching an object and is composed of the sub-elements component, timeout,
	 * inactivity, priority, property, idgenerator, and metadatagenerator. 
	 * It maps to a cache-id definition for a cache-entry in the cachespec.xml. 
	 */
	public class CacheId {
		
		private com.ibm.ws.cache.config.CacheId cacheId = null;
		
		public CacheId(com.ibm.ws.cache.config.CacheId ci){
			cacheId = ci;
		}
		
		public int getTimeout(){
			return cacheId.timeout;
		}
		
		public int getPriority(){
			return cacheId.priority;
		}
		
		public int getInactivity(){
			return cacheId.inactivity;
		}
		
		public String getIdGenerator(){
			return cacheId.idGenerator;
		}
		
		public String getMetaDataGenerator(){
			return cacheId.metaDataGenerator;
		}
		
		public ArrayList getProperties(){
			ArrayList properties = new ArrayList();
			if (cacheId.properties != null){
				Iterator it = cacheId.properties.values().iterator();
				while (it.hasNext()){
					Property property = new Property((com.ibm.ws.cache.config.Property)it.next());
					properties.add(property);
				}
			}
			return properties;
		}
		
		public Component[] getComponents(){
			Component[] components = new Component[cacheId.components.length];
			for ( int i=0; i<cacheId.components.length; i ++ ){
				components[i] = new Component(cacheId.components[i]);
			}
			return components;
		}
		
	}
	
	/**
	 * Component is a subelement to generate a portion of the cache ID. It consists of the 
	 * attribute ID, type and ignore-vlaue and the elements index, method, field, required, value,
	 * and not-value.
	 * It maps to a component definition for a cache-entry in the cachespec.xml. 
	 */
	public class Component{
		
		private com.ibm.ws.cache.config.Component component = null;
		
		public Component(com.ibm.ws.cache.config.Component c){
			component = c;
		}
		
		public String getId(){
			return component.id;
		}
		
		public String getType(){
			return component.type;
		}
		
		public boolean getIgnoreValue(){
			return component.ignoreValue;
		}
		
		public String getMethod(){
			com.ibm.ws.cache.config.Method method = component.method;
			if (method != null){
				return method.toString();
			}else {
				return null;
			}
		}
		
		public String getField(){
			com.ibm.ws.cache.config.Field field = component.field;
			if (field != null){
				return field.toString();
			}else {
				return null;
			}
		}
		
		public boolean getRequired(){
			return component.required;
		}
		
		public int getIndex(){
			return component.index;
		}
		
		public boolean getMultipleIds(){
			return component.multipleIds;
		}
		
		public ArrayList getValues(){
			ArrayList values = new ArrayList();
			if (component.values != null){
				Iterator it = component.values.values().iterator();
				while (it.hasNext()){
					com.ibm.ws.cache.config.Value v = (com.ibm.ws.cache.config.Value)it.next();
					values.add(v.value);
				}
			}
			return values;
		}
		
		public ArrayList getNotValues(){
			ArrayList notValues = new ArrayList();
			if (component.notValues != null){
				Iterator it = component.notValues.values().iterator();
				while (it.hasNext()){
					com.ibm.ws.cache.config.NotValue nv = (com.ibm.ws.cache.config.NotValue)it.next();
					notValues.add(nv.notValue);
				}
			}
			return notValues;
		}
		
		public ArrayList getValueRanges(){
			ArrayList valueRanges = new ArrayList();
			if (component.valueRanges != null){
				Iterator it = component.valueRanges.iterator();
				while (it.hasNext()){
					com.ibm.ws.cache.config.Range range = (com.ibm.ws.cache.config.Range)it.next();
					valueRanges.add("range low: "+range.low+ " high: "+range.high);
				}
			}
			return valueRanges;
		}
		
		public ArrayList getNotValueRanges(){
			ArrayList notValueRanges = new ArrayList();
			if (component.notValueRanges != null){
				Iterator it = component.notValueRanges.iterator();
				while (it.hasNext()){
					com.ibm.ws.cache.config.Range range = (com.ibm.ws.cache.config.Range)it.next();
					notValueRanges.add("range low: "+range.low+ " high: "+range.high);
				}
			}
			return notValueRanges;
		}
	}
	
	/**
	 * The DependencyId object specifies additional cache identifiers that associate multiple cache entries
	 * to the same group identifier. The value of the dependency-id element is generated by concatenating 
	 * the dependency ID base string with the values that are returned by its component elements. If a 
	 * required component returns a null value, the entire dependency does not generate and is not used. 
	 * Validate the dependency IDs explicitly through the dynamic cache API, or use the invalidation element. 
	 * Multiple dependency ID rules can exist in one cache-entry element. All dependency rules run separately. 
	 * It maps to a dependency-id definition for a cache-entry in the cachespec.xml. 
	 */
	public class DependencyId {
			
		private com.ibm.ws.cache.config.DependencyId depId = null;
			
		public DependencyId(com.ibm.ws.cache.config.DependencyId di){
			depId = di;
		}
			
		public String getBaseName(){
			return depId.baseName;
		}
		
		public Component[] getComponents(){
			Component[] components = new Component[depId.components.length];
			for ( int i=0; i<depId.components.length; i ++ ){
				components[i] = new Component(depId.components[i]);
			}
			return components;
		}
	}
	
	/**
	 * To invalidate cached objects, the application server must generate unique invalidation IDs.
	 * Build invalidation IDs by writing custom Java code or through rules that are defined in the
	 * cache policy of each cache entry. 
	 * It maps to a invalidation definition for a cache-entry in the cachespec.xml. 
	 */
	public class Invalidation {
		
		private com.ibm.ws.cache.config.Invalidation invalidation = null;
			
		public Invalidation(com.ibm.ws.cache.config.Invalidation i){
			invalidation = i;
		}
		
		public String getBaseName(){
			return invalidation.baseName;
		}
		
		public String getInvalidationGenerator(){
			return invalidation.invalidationGenerator;
		}
			
		public Component[] getComponents(){
			Component[] components = new Component[invalidation.components.length];
			for ( int i=0; i<invalidation.components.length; i ++ ){
				components[i] = new Component(invalidation.components[i]);
			}
			return components;
		}
	}
	
}