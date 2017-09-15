/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.util;

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.cache.DistributedNioMapObject;
import com.ibm.websphere.cache.Sizeable;
import com.ibm.ws.cache.CacheEntry;

import com.ibm.wsspi.cache.GenerateContents;

/**
 * Best case effort to determine the size on an Object in the WebSphere JVM
 * The overheads in the calculation below are conceptual. These nos. can change
 * for different JVMs. 
 * 
 * TODO: 64 bit support. Need to handle 32 and 64 bit JVMs
 * Platform JVM peculiarities are NOT considered.
 * 
 */
public class ObjectSizer {
	private static TraceComponent tc = Tr.register(CacheEntry.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
	
	/* all nos here are bytes */
	
	public static final long FASTHASHTABLE_INITIAL_OVERHEAD = 40;
	public static final long FASTHASHTABLE_INITIAL_PER_ENTRY_OVERHEAD = 28;
	public static final long FASTHASHTABLE_PER_ENTRY_OVERHEAD = 64;
	
	public static final long CACHEENTRY_INITIAL_PER_ENTRY_OVERHEAD = 184;
	
	public static final long OBJECT_ARRAY_OVERHEAD = 24;
	public static final long BYTE_ARRAY_OVERHEAD = 16;
	public static final long CHAR_ARRAY_OVERHEAD = 16;
	public static final long STRING_OVERHEAD = 48;
	
	public static final long OBJECT_REF_SIZE = 16;
	public static final long INTEGER_SIZE = 16;
	public static final long FLOAT_SIZE = 16;
	public static final long DOUBLE_SIZE = 24;
	public static final long LONG_SIZE = 24;
	public static final long SHORT_SIZE = 16;
	public static final long BYTE_SIZE = 16;
	public static final long BOOLEAN_SIZE = 16;
	public static final long CHARACTER_SIZE = 16;
	
	public static List<String> unsizeableObjectList = new ArrayList<String>(10);
	
	public static long getSize(Object object) {
		long totalSize = 0, size = 0;
		if (object != null) {
		    
			if (object instanceof String) {
		    	size = ((String)object).length() * 2;
			    totalSize = STRING_OVERHEAD + adjustAlignmentFor8Boundary(size);
			    
            } else if (object instanceof Sizeable) {
            	try {
            		size = ((Sizeable)object).getObjectSize();
            	} catch (Exception ex) {
            		//DYNA0034E=DYNA0034E: Exception invoking method {0} on class {1}.  Exception: {2}
            		Tr.error(tc, "DYNA0034E", new Object[] { "getObjectSize()", object.getClass().getName(), ex.getMessage()} );
            		com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.util.ObjectSizer", "67");
            		size = -1;
            	}
                if (size > 0) {
                	totalSize = adjustAlignmentFor8Boundary(size);
                } else {
                	totalSize = -1;
                }
                
            } else if (object instanceof DistributedNioMapObject) {
            	try {
                    size = ((DistributedNioMapObject)object).getCacheValueSize();
            	} catch (Exception ex) {
            		//DYNA0034E=DYNA0034E: Exception invoking method {0} on class {1}.  Exception: {2}
            		Tr.error(tc, "DYNA0034E", new Object[] { "getCacheValueSize()", object.getClass().getName(), ex.getMessage()} );
            		com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.util.ObjectSizer", "85");
            		size = -1;
            	}
                if (size > 0) {
                	totalSize = adjustAlignmentFor8Boundary(size);
                } else {
                	totalSize = -1;
                }
                
            } else if (object instanceof GenerateContents) {
            	try {
                    size = ((GenerateContents)object).generateContents().length;
                    totalSize = BYTE_ARRAY_OVERHEAD + adjustAlignmentFor8Boundary(size);
            	} catch (Exception ex) {
            		//DYNA0034E=DYNA0034E: Exception invoking method {0} on class {1}.  Exception: {2}
            		Tr.error(tc, "DYNA0034E", new Object[] { "getCacheValueSize()", object.getClass().getName(), ex.getMessage()} );
            		com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.util.ObjectSizer", "102");
            		size = -1;
            		totalSize = -1;
            	}
                
            } else if (object instanceof byte[]) {
                size = ((byte[])object).length;
                totalSize = BYTE_ARRAY_OVERHEAD + adjustAlignmentFor8Boundary(size);
                
            } else if (object instanceof char[]) {
                size = ((char[])object).length;
                totalSize = CHAR_ARRAY_OVERHEAD + adjustAlignmentFor8Boundary(size * 2);
                
		    } else if (object instanceof Integer) {
		    	totalSize = INTEGER_SIZE;
		    	
			} else if (object instanceof Float) {
				totalSize = FLOAT_SIZE;
				
			} else if (object instanceof Double) {
				totalSize = DOUBLE_SIZE;
				
			} else if (object instanceof Long) {
				totalSize = LONG_SIZE;
				
			} else if (object instanceof Short) {
				totalSize = SHORT_SIZE;
				
			} else if (object instanceof Byte) {
				totalSize = BYTE_SIZE;
				
			} else if (object instanceof Boolean) {
				totalSize = BOOLEAN_SIZE;
				
			} else if (object instanceof Character) {
				totalSize = CHARACTER_SIZE;
				
			} else {
            	totalSize = -1;
            	
			}
		}
		if ( tc.isDebugEnabled() && object == null) {
			Tr.debug(tc, "getSize() object is null!" );
	    }
		if (totalSize < 1 && object != null) {
			String className = object.getClass().getName();
			synchronized(unsizeableObjectList) {
				if (!unsizeableObjectList.contains(className)) {
					unsizeableObjectList.add(className);
				}
			}
		}
		return totalSize;
	}
	
	/**
	 * returns the size aligned to 8 byte boundary
	 */
	public static long adjustAlignmentFor8Boundary(long dataSize) {
		if (dataSize % 8 != 0 ) {
			dataSize = (dataSize / 8 + 1) * 8;
		}
		return dataSize;
	}
	
	public static List getUnsizableObjectList() {
	    return unsizeableObjectList;
	}
}
