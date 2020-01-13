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
package com.ibm.ws.cache.servlet;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

import javax.servlet.AsyncContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.ibm.websphere.command.CommandCaller;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.cache.ServletCacheRequest;
import com.ibm.ws.cache.util.SerializationUtility;
import com.ibm.wsspi.webcontainer.servlet.DummyRequest;
import com.ibm.wsspi.webcontainer.servlet.IServletRequest;

/**
 * This class is a proxy for the WebSphere request object.
 * It has features added to enable caching.
 * @ibm-private-in-use
 */
public class CacheProxyRequest extends HttpServletRequestWrapper implements CommandCaller, ServletCacheRequest{

   private static TraceComponent tc = Tr.register(CacheProxyRequest.class, 
		   "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

   static DummyRequest DUMMY_REQUEST = new DummyRequest(); 

   /**
    * The current FragmentComposer for this cache entry.
    */
   private FragmentComposer fragmentComposer = null;

   /**
    * The serialized form of attributeTable.
    */
   private byte[] attributeTableBytes = null;
   private Attribute attributeTableUnReadied[] = null;

   /**
    * A buffer for storing changes to the request attribute table
    */
   private HashMap <String, Object> setTable = null;

   /**
    * flag specifying whether the CPR is currently servicing a cacheable fragment
    */
   private boolean caching = false;

   /**
    * The struct object containing the caching metadata for this entry.
    */
   private FragmentInfo fragmentInfo = new FragmentInfo();

   /**
    * True indicates that the include call was used to
    * create this fragment.  False indicates that the forward call was used.
    */
   private boolean include = false;
   
   /**
    * True indicates that a NamedDispatcher was used.
    * False indicates that a RequestDispatcher was used.
    */
   private boolean namedDispatch = false;

   /**
    * 1 indicates that this is a ServletRequest
    * 2 indicates that this is a Portlet RenderRequest
    * 3 indicates that this is a Portlet ActionRequest
    */
   private int portletMethod = FragmentComposer.SERVLET_REQUEST;
   
   /**
    * URI for this request
    */
   private String relativeUri = null;
   private String absoluteUri = null;
   private String contextPath = null;
   private String servletName = null;
   private String servletClassName = null;
   
   
   /* Is this a top level request */
   private boolean nestedDispatch = false;
   
   /**
    * generatingId is used to keep track of whether or not we should
    * buffer the inputstream, incase the inputstream is used to generate
    * the cache id
    */
   private boolean generatingId = false;

   CacheProxyInputStream _cpis = null;
   CacheProxyReader _cpr = null;

   /**
    * Constructor with parameter.
    *
    * @param proxiedRequest The WebSphere request being proxied.
    */
   public CacheProxyRequest(HttpServletRequest proxiedRequest) {	   
       super(proxiedRequest);
   }
   
   public CacheProxyRequest() {
       super(DUMMY_REQUEST);   
   }

   /**
    * reset - resets this CacheProxyRequest for reuse
    */
   public void reset() {
 
	  if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
		  Tr.entry(tc, "reset", this); 
	   
	  attributeTableBytes = null;
      attributeTableUnReadied = null;
      setTable = null;
      caching = false;
      include = false;
      namedDispatch = false;
      
      portletMethod = FragmentComposer.SERVLET_REQUEST;
      generatingId = false;
      _cpis = null;
      _cpr = null;
      contextPath = relativeUri = absoluteUri = null;
      servletName = null;
      servletClassName = null;
      fragmentInfo.reset();
      fragmentComposer = null;
      
      nestedDispatch = false;
      
      setRequest(DUMMY_REQUEST);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
    	  Tr.exit(tc, "reset");
   }

   public void setGeneratingId(boolean b) {
      generatingId = b;
      if (!b) {
         //if proxied streams/reader have been created,
         //reset these streams so they are ready for use by the called servlet.
         if (_cpis != null) {
            try {
               _cpis.setCanMark(true);
               _cpis.reset();
               if (_cpr != null) {
                  _cpr.setCanMark(true);
                  _cpr.reset();
               }
            } catch (IOException e) {
               com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.servlet.CacheProxyRequest.setGeneratingId", "116", this);
               throw new IllegalStateException("failed to reset the inputstream proxy for normal use: " + e.getMessage());
            }
         }
      }
   }

   public boolean isGeneratingId() {
      return generatingId;
   }

   /* if generatingId is true, create a new proxied inputstream
    * and proxied reader
    */
   public ServletInputStream getInputStream() throws IOException {
      if (_cpis != null) {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getInputStream called, returning proxied CPIS");
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "returning " + _cpis);
         return _cpis;
      }
      if (isGeneratingId()) {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getInputStream called, generatingId is true, generating proxied CPIS");
         
         int cl = getContentLength();
         InputStream sourceStream = getRequest().getInputStream();
         
         if ("chunked".equalsIgnoreCase(getHeader("Transfer-Encoding"))) {
             if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                 Tr.debug(tc, "getInputStream Transfer-Encoding: chunked, available: " + sourceStream.available());

             // Because this is chunked Transfer-Encoding, there's no
             // Content-Length header which we need for the CacheProxyInputStream
             // to work (because of its rewindable mark/reset behavior).
             // So we just read the InputStream and count the bytes.
             int blockSize = 512;
             byte[] block = new byte[blockSize];
             ByteArrayOutputStream baos = new ByteArrayOutputStream(blockSize);
             int bytesRead;
             while ((bytesRead = sourceStream.read(block, 0, blockSize)) != -1) {
            	 baos.write(block, 0, bytesRead);
             }
             byte[] requestBody = baos.toByteArray();
             cl = requestBody.length;
             sourceStream = new ByteArrayInputStream(requestBody);
         }
         
         if (cl == -1)
            throw new IllegalStateException("request must know content length to use inputstream while generating Id");

         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
             Tr.debug(tc, "getInputStream underlying data size: " + cl);
         
         cl++;
         _cpis = new CacheProxyInputStream(sourceStream, cl); //NK2
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "returning " + _cpis);
         return _cpis;
      } else {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "returning " + getRequest().getInputStream()); //NK2
         return getRequest().getInputStream(); //NK2
      }
   }

   public BufferedReader getReader() throws IOException {
      if (_cpr != null) {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getReader called, returning proxied CPReader");
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "returning " + _cpr);
         return _cpr;
      }
      if (_cpis != null) {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getReader called, generating CPReader from CPIS");
         String enc = getCharacterEncoding();
         if (enc == null) {
            _cpr = new CacheProxyReader(new InputStreamReader(_cpis), _cpis.buffer);
         } else {
            try {
               _cpr = new CacheProxyReader(new InputStreamReader(_cpis, enc), _cpis.buffer);
            } catch (UnsupportedEncodingException e) {
               com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.servlet.CacheProxyRequest.getReader", "178", this);
               _cpr = new CacheProxyReader(new InputStreamReader(_cpis), _cpis.buffer);
            }
         }
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "returning " + _cpr);
         return _cpr;
      }
      if (isGeneratingId()) {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getReader called, generating proxied CPReader and CPIS");
         int cl = getContentLength();
         if (cl == -1)
            throw new IllegalStateException("request must know content length to use reader while generating Id");
         cl = cl + 1;
         _cpis = new CacheProxyInputStream(getRequest().getInputStream(), cl);
         String enc = getCharacterEncoding();
         if (enc == null) {
            _cpr = new CacheProxyReader(new InputStreamReader(_cpis), _cpis.buffer);
         } else {
            try {
               _cpr = new CacheProxyReader(new InputStreamReader(_cpis, enc), _cpis.buffer);
            } catch (UnsupportedEncodingException e) {
               com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.servlet.CacheProxyRequest.getReader", "201", this);
               _cpr = new CacheProxyReader(new InputStreamReader(_cpis), _cpis.buffer);
            }
         }
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "returning " + _cpr);
         return _cpr;
      } else {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "returning " + getRequest().getReader()); 
         return getRequest().getReader(); 
      }
   }

   /**
    * This sets the FragmentComposer for this response object.
    *
    */
   public void setFragmentComposer(FragmentComposer fragmentComposer) {
      this.fragmentComposer = fragmentComposer;
   }

   /**
    * This returns the FragmentInfo for this entry,
    * which contains the caching metadata for the entry.
    *
    * @return The caching metadata for this entry.
    */
   public com.ibm.websphere.servlet.cache.FragmentInfo getFragmentInfo() {
      return fragmentInfo;
   }

   //Satisfy CommandCaller interface
   public void unionDependencies(com.ibm.websphere.cache.EntryInfo entryInfo) {
      if (fragmentInfo == null) {
         return;
      }
      if (entryInfo == null) {
         throw new IllegalArgumentException("entryInfo is null");
      }
      fragmentInfo.unionDependencies(entryInfo);
   }

   /**
    * This sets the FragmentInfo for this entry,
    * which contains the caching metadata for the entry.
    *
    * @param fragmentInfo The caching metadata for this entry.
    */
   public void _setFragmentInfo(FragmentInfo fragmentInfo) {
      this.fragmentInfo = fragmentInfo;
      caching = fragmentInfo != null && fragmentInfo.getId() != null;
   }

   /**
    * This gets the include variable.
    *
    * @return True indicates that the include call was used to
    * create this fragment.  False indicates that the forward call was used.
    */
   public boolean getInclude() {
      return include;
   }

   /**
    * This sets the include variable.
    *
    * @param include True indicates that the include call was used to
    * create this fragment.  False indicates that the forward call was used.
    */
   public void setInclude(boolean include) {
      this.include = include;
   }
   
   /**
    * This gets the namedDispatch variable.
    *
    * @return True indicates that a NamedDispatcher was used.
    * False indicates that a RequestDispatcher was used.
    */
   public boolean getNamedDispatch() {
      return namedDispatch;
   }
   
   /**
    * This sets the namedDispatch variable.
    *
    * @param include True indicates that a NamedDispatcher was used.
    * False indicates that a RequestDispatcher was used.
    */
   public void setNamedDispatch(boolean namedDispatch) {
      this.namedDispatch = namedDispatch;
   }
   
   public int getPortletMethod() {
      return portletMethod;
   }
   
   public void setPortletMethod(int portletMethod) {
      this.portletMethod = portletMethod;
   }
   
   public void setRelativeUri(String relativeUri) {
      this.relativeUri = relativeUri;
   }

   public String getRelativeUri() {
      return relativeUri;
   }

   public void setAbsoluteUri(String absoluteUri) {
      this.absoluteUri = absoluteUri;
   }

   public String getAbsoluteUri() {
      return absoluteUri;
   }

   /* methods to get the real context path for
      this particular include/forward rather than
      for the original request
    */
   public void _setContextPath(String contextPath) {
      this.contextPath = contextPath;
   }

   public String _getContextPath() {
      return contextPath;
   }

   public void setServletName(String servletName) {
   	  this.servletName = servletName;
   }

   public String getServletName() {
      return servletName;
   }
   
   public void setServletClassName(String servletClassName) {
	  this.servletClassName = servletClassName;
   }

   public String getServletClassName() {
 	  return servletClassName;
   }
   
   public boolean getCaching() {
      return caching;
   }

   public void setCaching(boolean b) {
      caching = b;
   }

   /**
     * This sets the page to be uncachebale
     *
     * @param value True if the page to be set as uncacheable
     */

   public void setUncacheable(boolean uncacheable) {
       fragmentInfo.setUncacheable(uncacheable);
       fragmentInfo.setDoNotCache(uncacheable);
   }

   /**
     * This returns true if the page uncacheable
     *
     * @return True indicates that the fragment is uncacheable and
     * false indicates that the fragment is cacheable.
     */

   public boolean isUncacheable() {
       return fragmentInfo.isUncacheable();
   }
	
	public AsyncContext startAsync() {
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			Tr.entry(tc, "startAsync");
	    
		AsyncContext context = startAsyncOverriden();
	    
	    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	    	Tr.exit(tc, "startAsync", context);
	    
		return context;
	}
	
	public AsyncContext startAsync(ServletRequest req, ServletResponse resp) {
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			Tr.entry(tc, "startAsync", new Object[]{req, resp});
	    
		AsyncContext context = startAsyncOverriden();
	    
	    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	    	Tr.exit(tc, "startAsync", context);
	    
		return context;
	}
	

	private AsyncContext startAsyncOverriden() {
		
		AsyncContext context = super.startAsync(this, fragmentComposer.getResponse());
		
		Servlet30AsyncListener dynServlet30Listener = new Servlet30AsyncListener();
	    context.addListener(dynServlet30Listener, this, fragmentComposer.getResponse());
	    if (tc.isDebugEnabled())
	    	Tr.debug(tc, context + " associated with:"+ dynServlet30Listener+":"+dynServlet30Listener.hashCode());
		
	    fragmentComposer.setAsyncDispatch(true);
	    if (tc.isDebugEnabled())
			Tr.debug(tc, this+" ASYNC=TRUE ");
		
		return context;
	}	

   public Hashtable getAttributeTable() {
      throw new IllegalStateException("getAttributeTable is no longer a valid method");
   }

   //only clone the table if you're still going to use the settable afterwards, otherwise let the lazy initialization
   //build you a new one if this request is used again.
   public Attribute[] getChangedAttributes() {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
         Tr.debug(tc, "getting Changed Attributes: " + setTable);

      if (setTable == null)
         return null;

      HashMap tmp = setTable;
      setTable = null;
      Attribute attrs[] = new Attribute[tmp.size()];
      Iterator it = tmp.keySet().iterator();
      int i=0;
      while (it.hasNext()) {
           attrs[i] = new Attribute();
           attrs[i].key = (String) it.next();
           attrs[i].value = tmp.get(attrs[i].key);
           i++;
      }
      return attrs;
   }

   protected void clearSetTable() {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
         Tr.debug(tc, "clearing setTable");
      
      if (setTable != null)
         setTable.clear();
   }

   /**
    * This sets the attributeTableUnReadied variable.
    * @param attributeTable The request attribute table
    */

   public void setAttributeTableUnReadied(Attribute unready[]) {
      attributeTableUnReadied = unready;
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
         Tr.debug(tc, "setAttributeTableUnReadied: ",  (Object) attributeTableUnReadied);
   }

   public Attribute[] removeAttributeTableUnReadied() {
      Attribute tmp[] = attributeTableUnReadied;
      attributeTableUnReadied = null;
      return tmp;
   }

   /**
    * This sets the attributeTableBytes variable.
    * @param attributeTableBytes The request attribute table in its
    * byte array form.
    */
   public void setAttributeTableBytes(byte[] attributeTableBytes) {
      this.attributeTableBytes = attributeTableBytes;
   }

   public byte[] removeAttributeTableBytes() {
      byte[] tmp = attributeTableBytes;
      attributeTableBytes = null;
      return tmp;
   }

   public boolean isUnReadied() {
      return attributeTableUnReadied != null || attributeTableBytes != null;
   }

   /**
    * This overrides the method in the WebSphere request.
    * It returns the request attribute with the specified key.
    *
    * @param key The attribute key.
    * @return The attribute value.
    */
   public Object getAttribute(String key) {
      return getRequest().getAttribute(key);  
   }

   /**
    * This overrides the method in the WebSphere request.
    * It returns sets request attribute key-value pair.
    *
    * @param key The attribute key.
    * @param value The attribute value.
    */
   public void setAttribute(String key, Object value) {
      
      //skip-cache-attribute
      String skipCacheAttribute = fragmentInfo.getSkipCacheAttribute();
      if ( key.equals(skipCacheAttribute))
    	  CacheHook.setSkipCache(true);
    
      if ((caching || fragmentInfo.getHasCacheableConsumingParent()) && 
    		  fragmentComposer.currentChild == null){
         
    	  if (setTable == null)
            setTable = new HashMap <String, Object>();
         
         setTable.put(key, value);
      }
      
      getRequest().setAttribute(key, value); 
   }

   /**
    * This converts the byte array version of the request attributes
    * table to the hashtable version.
    */
   public void readyAttributes() {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
         Tr.entry(tc, "readyAttributes");
      if (attributeTableBytes == null && attributeTableUnReadied == null) {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "attributeTableBytes == null && attributeTableUnReadied == null");
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "readyAttributes");
         return;
      }
      Attribute tmp[] = null;
      if (attributeTableBytes != null) {
         try {
            tmp = (Attribute[]) SerializationUtility.deserialize(attributeTableBytes, fragmentInfo.getInstanceName());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
               Tr.debug(tc, "used serialized attributes, setting on the request...");
         } catch (Exception ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.servlet.CacheProxyRequest.readyAttributes", "416", this);
            throw new IllegalStateException("Exception occurred while deserializing attributeTableBytes");
         }
      } else {
         tmp = attributeTableUnReadied;
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "used unreadied attributes, setting on the request...");
      }

      for (int i=0;i<tmp.length;i++)
         setAttribute(tmp[i].key,tmp[i].value);

      attributeTableBytes = null;
      attributeTableUnReadied = null;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
         Tr.exit(tc, "readyAttributes");
   }

   /* used for storing request attribute */
   static public class Attribute implements Serializable {
       	  private static final long serialVersionUID = -2623484169069387491L;
          public String key;
          public Object value;
          public String toString() {
             return "key: "+key+" value: "+value;
          }
   }

   public Object clone() throws CloneNotSupportedException {
	 
	   CacheProxyRequest request = (CacheProxyRequest) super.clone();
	   
	   javax.servlet.ServletRequest inner = request.getRequest();
	   if (inner instanceof IServletRequest) {
		   request.setRequest((ServletRequest)((IServletRequest)inner).clone());
	   } else {
		   throw new CloneNotSupportedException("Unable to clone root request of class " + inner.getClass().getName());
	   }
	   
	   if(this.attributeTableBytes != null) {
		   request.attributeTableBytes = (byte[])attributeTableBytes.clone();
	   }
	   if(this.attributeTableUnReadied != null) {
		   request.attributeTableUnReadied = (Attribute[])attributeTableUnReadied.clone();
	   }
	   if(this.setTable != null) {
		   request.setTable = (HashMap)setTable.clone();
	   }
	   
       return request;
   }

   /**
    * getFragmentComposer - returns the fragment composer for this response object.
    * @return returns the current fragment composer for this response object.
    * @ibm-private-in-use
    */
	public FragmentComposer getFragmentComposer() {
		return fragmentComposer;
	}
	
	public boolean isNestedDispatch() {
		return nestedDispatch;
	}

	public void setNestedDispatch(boolean nestedDispatch) {
		this.nestedDispatch = nestedDispatch;
	}


}

class CacheProxyInputStream extends ServletInputStream {
   BufferedInputStream bis = null;
   protected int buffer = -1;
   boolean canMark = false;

   public CacheProxyInputStream(InputStream is, int s) {
      bis = new BufferedInputStream(is, s);
      bis.mark(s);
      buffer = s;
   }
   protected void setCanMark(boolean b) {
      canMark = b;
   }
   //we've marked this, so don't let the user erase our mark..
   public boolean markSupported() {
      return canMark;
   }

   public int available() throws IOException {
      return bis.available();
   }
   public void close() throws IOException {
   }
   public void mark(int readlimit) {
      if (!canMark)
         throw new IllegalStateException("ServletInputStream/CacheProxyInputStream does not support mark");
      else
         bis.mark(readlimit);
   }
   public int read() throws IOException {
      return bis.read();
   }
   public int read(byte[] b, int off, int len) throws IOException {
      return bis.read(b, off, len);
   }
   public void reset() throws IOException {
      if (!canMark)
         throw new IllegalStateException("ServletInputStream/CacheProxyInputStream does not support mark/reset");
      else
         bis.reset();
   }
   public long skip(long n) throws IOException {
      return bis.skip(n);
   }

}

class CacheProxyReader extends BufferedReader {
   boolean canMark = true;

   public CacheProxyReader(Reader in) {
      super(in);
   }

   public CacheProxyReader(Reader in, int sz) {
      super(in, sz);
      canMark = true;
      try {
         mark(sz);
      } catch (IOException e) {
         com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.servlet.CacheProxyRequest.CacheProxyReader", "496", this);
         throw new IllegalStateException("mark must succeed to init CacheProxyReader while computing cacheId: " + e.getMessage());
      }
      canMark = false;
   }

   protected void setCanMark(boolean b) {
      canMark = b;
   }

   public void mark(int readAheadLimit) throws IOException {
      if (!canMark)
         throw new IllegalStateException("CacheProxyReader does not support mark");
      else
         super.mark(readAheadLimit);
   }
   public boolean markSupported() {
      return canMark;
   }
   public void reset() throws IOException {
      if (!canMark)
         throw new IllegalStateException("CacheProxyReader does not support mark/reset");
      else
         super.reset();
   }
}
