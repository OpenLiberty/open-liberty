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
package com.ibm.ws.cache.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.servlet.CacheProxyRequest.Attribute;
import com.ibm.ws.cache.util.SerializationUtility;

/**
 * This is a struct object containing the information describing
 * a link to a contained entry from within a FragmentComposer.
 * It includes the template of the contained entry as well as the
 * request attributes required to execute the entry.
 */
/* package */
class MementoEntry implements Serializable {
    private static final long serialVersionUID = 1342185474L;
	private static TraceComponent tc = Tr.register(MementoEntry.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

	String cacheName;
	
	/**
	 * This is the template of the contained entry.
	 */
	String template = null;
	Object contents[] = null;
	
	/**
	 * This is the serialized Hashtable containing the request attribute
	 * name-value pairs as they were in the request object just prior to
	 * execution of the contained entry.
	 */
	byte[] attributeTableBytes = null;
	CacheProxyRequest.Attribute attributeTable[] = null;
	
	byte[] endAttributeTableBytes = null;
	CacheProxyRequest.Attribute endAttributeTable[] = null;

	/**
	 * This indicates whether this JSP was called via the include call.
	 * A false indicates it was a forward.
	 */
	boolean include = false;
	
	/**
	 * This indicates whether this used a NamedDispatcher
	 * A false indicates it was a RequestDispatcher
	 */
	boolean namedDispatch = false;

	String contextPath = null;

	boolean doNotConsume = false;
	
	String servletClassName = null;

	int outputStyle = FragmentComposer.NONE;

	boolean externallyCacheable = false;
	
	boolean async =false;
	
	/**
	 * Constructor with parameters.
	 *
	 * @param template The Template of the contained entry.
	 * @param attributeTable The serialized Hashtable containing the
	 * request attribute name-value pairs as they were in the request
	 * object just prior to execution of the contained entry.
	 * @param include True indicates this JSP was called via the include
	 * call instead of the forward call.
	 */
	MementoEntry(String cName, String template, byte[] attributeTableBytes, byte[] endAttributeTableBytes,boolean include, boolean namedDispatch, String contextPath) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.debug(tc, "Building new Memento Entry for " + template + ", attributeTableBytes, endAttributeTableBytes, include = " + include + ", contextPath is " + contextPath);
		this.cacheName = cName;
		this.template = template;
		this.contextPath = contextPath;
		this.attributeTableBytes = attributeTableBytes;
		this.endAttributeTableBytes = endAttributeTableBytes;
		this.include = include;
		this.namedDispatch = namedDispatch;
	}

	MementoEntry(String cName, String template, CacheProxyRequest.Attribute attributeTable[], CacheProxyRequest.Attribute endAttributeTable[], boolean include, boolean namedDispatch, String contextPath) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.debug(tc, "Building new Memento Entry for " + template + ", attributeTable = " + prettyPrintAttributeTable(attributeTable) + ", endAttributeTable = " + prettyPrintAttributeTable(endAttributeTable)+ ", include = " + include + ", contextPath is " + contextPath);
		
		this.cacheName = cName;
		this.contextPath = contextPath;
		this.template = template;
		this.attributeTable = attributeTable;
		this.endAttributeTable = endAttributeTable;
		this.include = include;
		this.namedDispatch = namedDispatch;
	}

	void addContents(Object contents[]){
		this.contents = contents;
	}
	
	Object[] getContents(){
		return contents;
	}

	/**
	 * This gets the Template of the contained entry.
	 *
	 * @return The Template of the contained entry.
	 */
	String getTemplate() {
		return template;
	}

	/**
	 * This get the Hashtable containing the
	 * request attribute name-value pairs as they were in the request
	 * object just prior to execution of the contained entry.
	 *
	 * @return The Hashtable containing the request attributes.
	 */
	CacheProxyRequest.Attribute[] getAttributeTable() {
		if (attributeTable != null) {
			return attributeTable;
		}

		if (attributeTableBytes == null) {
			//then no attributes were changed when this memento was called from its parent
			return null;
		}
		try {
			return (CacheProxyRequest.Attribute[]) SerializationUtility.deserialize(attributeTableBytes, cacheName);
		} catch (Exception ex) {
			com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.servlet.MementoEntry.getAttributeTable", "134", this);
			return null;
		}
	}

	/**
	 * @return The attributeTable variable.
	 */
	byte[] getAttributeTableBytes() {
		return attributeTableBytes;
	}
	
	/**
	 * @return The endAttributeTable variable.
	 */
	CacheProxyRequest.Attribute[] getEndAttributeTable() {
		if (endAttributeTable != null) {
			return endAttributeTable;
		}

		if (endAttributeTableBytes == null) {
			//then no attributes were changed when this memento was called from its parent
			return null;
		}
		try {
			return (CacheProxyRequest.Attribute[]) SerializationUtility.deserialize(endAttributeTableBytes, null);
		} catch (Exception ex) {
			com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.servlet.MementoEntry.getEndAttributeTable", "134", this);
			return null;
		}
	}

	/**
	 * @return The endAttributeTable variable.
	 */
	byte[] getEndAttributeTableBytes() {
		return endAttributeTableBytes;
	}

	/**
	 * @return The include variable.
	 */
	/* package */
	boolean getInclude() {
		return include;
	}
	
	/**
	 * @return The namedDispatch variable.
	 */
	/* package */
	boolean getNamedDispatch() {
		return namedDispatch;
	}

	/**
	 * @return The include variable.
	 */
	/* package */
	String getContextPath() {
		return contextPath;
	}
	
	/**				
	 * This gets the doNotConsume variable.
	 *
	 * @return The doNotConsume variable.
	 */
	public boolean getDoNotConsume() {
		return doNotConsume;
	}

	/**
	 * This sets the doNotConsume variable.
	 *
	 * @para, The doNotConsume variable.
	 */
	public void setDoNotConsume(boolean doNotConsume) {
		this.doNotConsume = doNotConsume;
	}
	
	/**				
	 * This gets the servletClassName variable.
	 *
	 * @return The servletClassName variable.
	 */
	public String getServletClassName() {
		return servletClassName;
	}
	
	/**
	 * This sets the servletClassName variable.
	 *
	 * @para, The servletClassName variable.
	 */
	public void setServletClassName(String servletClassName) {
		this.servletClassName = servletClassName;
	}

	public void setOutputStyle(int outputStyle){
	    this.outputStyle = outputStyle;
	}

	public void setExternallyCacheable(boolean externallyCacheable){
	    this.externallyCacheable = externallyCacheable;
	}

	public boolean isExternallyCacheable(){
	    return externallyCacheable;
	}
	
	public boolean isAsync() {
		return async;
	}

	public void setAsync(boolean a) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.debug(tc, "template="+template+ " async="+a);
		this.async = a;
	}

	public byte[] toByteArray(String charEnc) throws IOException {
	     if (!isExternallyCacheable()) {
		        throw new IllegalStateException("Cannot obtain byteArray since caching was not enabled for this fragment");
	      }
	     ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	     OutputStreamWriter osw = null;
	     byte[] array = null;

	     for (int i = 0; i < contents.length; i++) {
		   Object object = contents[i];
		   if (object instanceof byte[]) {		      
		        if (outputStyle != FragmentComposer.BYTE)
				throw new IllegalStateException("MementoEntry " + this +" with outputStyle " + outputStyle + " cannot contain byte arrays");
			outputStream.write((byte[]) object, 0, ((byte[]) object).length);
			outputStream.flush();
			continue;		      
		   } else if (object instanceof char[]) {		      
			    if (outputStyle != FragmentComposer.CHAR)
			       	throw new IllegalStateException("MementoEntry " + this +" with outputStyle " + outputStyle + " cannot contain char arrays");
			    if (osw == null) {
					try {
						osw = new OutputStreamWriter(outputStream, charEnc);
					} catch (UnsupportedEncodingException uex) {
						com.ibm.ws.ffdc.FFDCFilter.processException(uex, "com.ibm.ws.cache.servlet.FragmentComposer.toByteArray", "671", this);
						osw = new OutputStreamWriter(outputStream);
					}
				}
				osw.write((char[]) object, 0, ((char[]) object).length);
				osw.flush();
				continue;
		   } else if (object instanceof String) {
		      continue;
		   } else if (object instanceof ResponseSideEffect) {
		      continue;
		   }
		   if (!(object instanceof MementoEntry)) {
				throw new IllegalStateException("MementoEntry should only contain " + "other MementoEntries, " + "Byte Arrays, Char Arrays, " + "and ResponseSideEffects");
			}
			//FragmentComposer
		     MementoEntry mementoEntry = (MementoEntry) object;
		     array = mementoEntry.toByteArray(charEnc);
		     outputStream.write(array, 0, array.length);
	     }
	     return outputStream.toByteArray();
			
	}

    /**
     * This overrides the method in Object of MementoEntry.
     * It returns the hashCode of the contents.
     *
     * @return The hashCode.
     */
	@Override
	public int hashCode()  {
	    int hc = 0;
	    if (isExternallyCacheable()) {
		    for (int i = 0; i < contents.length; i++) {
		         Object object = contents[i];
				 if (object instanceof byte[]) {	
				     hc += object.hashCode();
					 continue;
				 } else if (object instanceof char[]) {		      
				     hc += object.hashCode();	
				     continue;
				 } else if (object instanceof String) {
				     continue;
				 } else if (object instanceof ResponseSideEffect) {
				     continue;
				 } else if (object instanceof MementoEntry) {
				     //FragmentComposer
				     MementoEntry mementoEntry = (MementoEntry) object;
				     hc += mementoEntry.hashCode();
				 }
			 }
	     }
	     return hc;
	}
	
	private String prettyPrintAttributeTable(Attribute[] attrs) {
              StringBuffer sb = new StringBuffer();
              if (null != attrs) {
                     for (int i = 0; i < attrs.length; i++) {
                            Attribute attr = attrs[i];
                            sb.append(System.getProperty("line.separator"));
                            sb.append(attr);
                     }
              } else {
                     sb.append("null");
              }
              return sb.toString();
      }   
}
