/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.ola;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.resource.cci.MappedRecord;

public class MappedRecordImpl implements MappedRecord {
	
	private static final long serialVersionUID = 1;
	
	/**
	 * The name of the RAD RecordBytes interface.
	 */
	private static final String RECORD_BYTES_CLASSNAME = "com.ibm.etools.marshall.RecordBytes";
	
	/**
	 * The name of this record.
	 */
	private String recordName;
	
	/**
	 * The description for this record.
	 */
	private String recordDescription;
	
	/**
	 * The HashMap containing all of the pairs of Container names and data that belongs in them.
	 */
	private HashMap<String, byte[]> containerMap;

	/**
	 * The number of bytes of data currently in the containerMap
	 */
	private int dataSize;
	
	/**
	 * Public Constructor
	 */
	public MappedRecordImpl() {
        containerMap = new HashMap<String, byte[]>();
        dataSize = 0;
	}
    
    /**
     * Public Constructor
     * @param newRecordName The record name for this Mapped Record.
     * @param newRecordDescription The short description for this Mapped Record.
     * @param newMap A Map filled with any values to prepopulate the Mapped Record with.
     */
    public MappedRecordImpl(String newRecordName, String newRecordDescription, HashMap newMap) {
        recordName = newRecordName;
        recordDescription = newRecordDescription;
        containerMap = new HashMap<String, byte[]>();
        containerMap.putAll(newMap);
    }
	
	/**
	 * Copy constructor
	 */
	private MappedRecordImpl(MappedRecordImpl base) {
		this.recordName = base.getRecordName();
		this.recordDescription = base.getRecordShortDescription();
		this.dataSize = base.getDataSize();
		
		this.containerMap.putAll(base.containerMap);
	}
	
	/**
	 * Get the current record name.
	 * @return The name of the record.
	 */
	public String getRecordName() {
		return recordName;
	}
	
	/**
	 * Get the current description
	 * @return The description of the record.
	 */
	public String getRecordShortDescription() {
		return recordDescription;
	}
	
	/**
	 * Get the number of bytes of data currently stored.
	 * @return Number of bytes
	 */
	public int getDataSize() {
		return dataSize;
	}
	
	/**
	 * Set the current record name.
	 * @param name The name of the record.
	 */
	public void setRecordName(String name) {
		recordName = name;
	}
	
	/**
	 * Set the current description
	 * @param description The description of the record.
	 */
	public void setRecordShortDescription(String description) {
		recordDescription = description;
	}
	
	/**
	 * Clears the record
	 */
	public void clear() {
		containerMap.clear();
		dataSize = 0;
	}
	
	/**
	 * Determines if the record contains a specific key.
	 * @param key The key to check for.
	 * @return True if the record contains the key.
	 */
	public boolean containsKey(Object key) {
		return containerMap.containsKey(key);
	}
	
	/**
	 * Determines if the record contains a specific value.
	 * @param value The value to check for.
	 * @return True if the record contains the value.
	 */
	public boolean containsValue(Object value) {
		return containerMap.containsValue(value);
	}
	
	/**
	 * Answers a Set of the mappings contained in this record.
	 * @return A Set of the mappings.
	 */
	public Set<Entry<String, byte[]>> entrySet() {
		return containerMap.entrySet();
	}
	
	/**
	 * Returns the value that is mapped to the specified key.
	 * @param key The key to use.
	 * @return The value which is mapped to the specified key.
	 */
	public byte[] get(Object key) {
		return containerMap.get(key);
	}
	
	/**
	 * Checks to see if the record is currently empty.
	 * @return True if the record is empty.
	 */
	public boolean isEmpty() {
		return containerMap.isEmpty();
	}
	
	/**
	 * Returns a Set view of the keys contained in this record.
	 * @return A Set of the keys.
	 */
	public Set<String> keySet() {
		return containerMap.keySet();
	}
	
    /**
     * Tells us whether this Record is an instance of the RecordBytes interface (meaning it was
     * generated from RAD).
     * 
     * Since we require applications in Liberty to package the RecordBytes interface and supporting
     * code (marshall.jar) inside their application, we can't just do an instanceof check here to
     * see if we have an instance of RecordBytes.  The RecordBytes instance that we are checking
     * against will have been loaded by a different classloader, and the check will fail.
     * 
     * Instead, we must use the classloader which loaded/created the Record class, or just query
     * the class for the interfaces it implements and see if the string name of the RecordBytes
     * interface is one of those.
     */
    private boolean isRecordBytes(Object o) {
    	try {
    		Class<?> recordClass = o.getClass();
    		ClassLoader cl = recordClass.getClassLoader();
    		Class<?> recordBytesClass = cl.loadClass(RECORD_BYTES_CLASSNAME);
    		return recordBytesClass.isAssignableFrom(recordClass);
    	} catch (Throwable t) {
    		/* FFDC */
    	}

    	return false;
    }

	/**
	 * Adds a mapping to the record.
	 * @param key The key.
	 * @param value The value.
	 * @return The previous value for this mapping (or null if there was no previous mapping).
	 */
	public byte[] put(Object key, Object value) {
		byte[] inputBytes = null;
		if(value instanceof byte[]) {
			inputBytes = (byte[])value;
		}
		else if(isRecordBytes(value)) {
			try {
        		Class<?> c = value.getClass();
        		Method m = c.getMethod("getBytes");
        		inputBytes = (byte[]) m.invoke(value, (Object[])null);
			} catch (Throwable t) {
				throw new IllegalArgumentException("Unable to store RecordBytes into MappedRecord", t);
			}
		}
		else
			throw new IllegalArgumentException();
		
		if( !(key instanceof String) )
			throw new IllegalArgumentException();
		
		byte[] returnBytes = containerMap.put((String)key, inputBytes);
		
        if (returnBytes != null)
            dataSize -= returnBytes.length;
		dataSize += inputBytes.length;
		return returnBytes;
	}
	
	/**
	 * Copies all the mappings from the given Map to the current record.
	 * @param t The Map to copy from
	 */
	public void putAll(Map t) {
		/*
		 * This logic abuses the fact that to actually add the parameter to our internal
		 * Map we're simply calling the internal Maps putAll(...) function.  The logic
		 * assumes that the call to putAll(...) will perform the type check on our parameter,
		 * and at all points afterwards it can be assumed if an error didn't get thrown that
		 * our parameter is a Map<String, byte[]>.
		 */
		containerMap.putAll(t);
	
		// Increment the data size for each byte[] just added
		Set<String> keys = (Set<String>)t.keySet();
		int dataSizeToAdd = 0;
		for(String currentKey : keys) {
			dataSizeToAdd += ((byte[])t.get(currentKey)).length;
		}
		dataSize += dataSizeToAdd;
	}
	
	/**
	 * Removes an mapping with a specific key from the record.
	 * @param key The key of the mapping to be removed.
	 * @return The value of the removed mapping.
	 */
	public byte[] remove(Object key) {
		byte[] returnBytes = containerMap.remove(key);
		if( returnBytes != null )
			dataSize -= returnBytes.length;
		
		return returnBytes;
	}
	
	/**
	 * Returns the number of records that are in the record.
	 * @return The number of elements in the record.
	 */
	public int size() {
		return containerMap.size();
	}
	
	/**
	 * Returns a Collection of all the values in the record.
	 * @return The collection of all the values.
	 */
	public Collection<byte[]> values() {
		return containerMap.values();
	}
	
	/**
	 * Makes a copy of this MappedRecord
	 * @return A copy of this Mapped Record
	 */
	public Object clone() {
		return new MappedRecordImpl(this);
	}
}
