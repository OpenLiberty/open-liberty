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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.cache.DynamicContentProvider;
import com.ibm.ws.cache.ValueSet;
import com.ibm.ws.cache.util.SerializationUtility;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * There is a FragmentComposer for each call to include an entry (ie, each
 * include or forward call).
 * 
 * The response object keeps a stack of these cooresponding to the current level
 * of entry execution.
 * 
 * @ibm-private-in-use
 */
public class FragmentComposer {

	private static TraceComponent tc = Tr.register(FragmentComposer.class,
			"WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

	public final static int NOT_CACHED = 0; //FragmentComposer's entry should not be cached.
	public final static int WAS_CACHED = 1;  //FragmentComposer's entry was found already in the cache
	/**
	 * This constant indicates that this FragmentComposer's entry was put in the
	 * cached during this call.
	 */
	public final static int POPULATED_CACHE = 2;
	public final static int SERVLET_REQUEST = 1;
	public final static int PORTLET_CACHE_REQUEST = 2;

	/**
	 * This indicates the state of this FragmentComposer's entry. It's possible
	 * values are NOT_CACHED, WAS_CACHED or POPULATED_CACHE.
	 */
	private int cacheType = -1;

	/**
	 * This indicates that the entry is an externally requested page.
	 */
	private boolean externalPage = false;

	/**
	 * Caches the value of toByteArray()
	 */
	protected static final int BYTE = 1;
	protected static final int CHAR = 2;
	protected static final int NONE = 3;
	protected int outputStyle = NONE;

	protected String contentType = null;
	
	protected String characterEncoding = null;

	FragmentComposer parent = null;
	protected FragmentComposer currentChild = null;

	/**
	 * This holds the sequence of entries that make up an entry. Its elements
	 * are either:
	 * <ul>
	 * <li>A byte/char array for HTML static content.
	 * <li>Another FragmentComposer: For a contained entry.
	 * </ul>
	 */
	protected ArrayList contentVector = new ArrayList();

	private int bufferSize = 8192; // buffer size

	private CacheProxyOutputStream cacheProxyOutputStream;

	private CacheProxyWriter cacheProxyWriter;

	/**
	 * The caching metadata for this FragmentComposer's entry.
	 */
	protected FragmentInfo fragmentInfo = null;

	protected boolean isSendRedirect = false; 

	private boolean isJSTLImport = false;

	/**
	 * Do we discard the content of this fragmentComposer 
	 */
	private boolean discardJSPContent = false;

	/**
	 * This is the uri of the entry.
	 */
	private String uri = null;

	private String servletClassName = null;

	private boolean doNotConsume = false;
	private boolean consumeSubfragments = false;
	private boolean hasCacheableConsumingParent = false;

	public CacheProxyRequest.Attribute[] saveAttributeList = null;

	/**
	 * This is a Hashtable containing the request attribute name-value pairs as
	 * they were in the request object just prior to execution of this
	 * FragmentComposer's entry.
	 */
	private CacheProxyRequest.Attribute attributeTable[] = null;

	/**
	 * This is the serialized version of attributeTable.
	 */
	private byte[] attributeTableBytes = null;

	/**
	 * True indicates this fragment was created via the include call instead of
	 * the forward call.
	 */
	private boolean include = false;

	/**
	 * True indicates that a NamedDispatcher was used. False indicates that a
	 * RequestDispatcher was used.
	 */
	private boolean namedDispatch = false;

	/**
	 * The absolute time when this fragment should be invalidated.
	 */
	private long expirationTime = Long.MAX_VALUE;

	/**
	 * Indicates when the fragment was created.
	 */
	private long timeStamp = -1;

	/**
	 * Indicates whether or not the parent fragment was externally cacheable
	 */
	private boolean parentExternallyCacheable = false;

	// the CacheProxyResponse that is coordinating with this composer
	private CacheProxyResponse response = null;

	// the request that matches this composer's response
	private CacheProxyRequest request = null;

	private String contextPath = null;

	// ESI surrogate variables
	private int esiVersion = ESISupport.NO_ESI;

	// Data ids for external cacheable request
	private ValueSet externalDataIds = new ValueSet(5); // ibm.com

	// Templates for external cacheable request
	private ValueSet externalTemplates = new ValueSet(3); // ibm.com
	
	/* was startAsync called on the request */
	private boolean asyncDispatch = false;

	/* Dispatch back to the same servlet that initiated the startAsync*/
	private boolean asyncDoubleDip = false;
	
	/**
	 * Constructor.
	 */
	public FragmentComposer(CacheProxyRequest request, CacheProxyResponse response) 
	{
		this.response = response;
		this.request = request;
		contextPath = request._getContextPath();
	}

	public FragmentComposer() {}

	public void resetRequestAndResponseOnly(CacheProxyRequest request, CacheProxyResponse response) {
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			if (null == uri){
				Tr.debug(tc, "PARTIAL RESET called on "+this);
			} else {
				Tr.debug(tc, "PARTIAL RESET called on "+uri);
			}			
		}			
		this.response = response;
		this.request = request;
	}
		
	/**
	 * reuse this fragment composer for a new response
	 */

	public void reset(CacheProxyRequest request, CacheProxyResponse response) {
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			if (null == uri){
				Tr.debug(tc, "FULL RESET called on "+this);
			} else {
				Tr.debug(tc, "FULL RESET called on "+uri);
			}			
		}	
		
		this.response = response;
		this.request = request;
		contentVector.clear();
		cacheType = -1;
		fragmentInfo = null;
		uri = null;
		servletClassName = null;
		externalPage = false;
		outputStyle = NONE;
		contentType = null;
		characterEncoding = null;
		attributeTable = null;
		include = false;
		namedDispatch = false;
		expirationTime = Long.MAX_VALUE;
		timeStamp = -1;
		parentExternallyCacheable = false;
		parent = null;
		currentChild = null;
		attributeTableBytes = null;
		esiVersion = ESISupport.NO_ESI;
		if (cacheProxyOutputStream != null)
			cacheProxyOutputStream.reset();
		if (cacheProxyWriter != null)
			cacheProxyWriter.reset();
		if (request != null)
			contextPath = request._getContextPath();
		isSendRedirect = false; 
		isJSTLImport = false; 
		discardJSPContent = false;
		hasCacheableConsumingParent = false;
		saveAttributeList = null;
		externalDataIds.clear(); 
		externalTemplates.clear();
		asyncDispatch = false;
		asyncDoubleDip = false;
		
	}

	public void addContents(Object[] contents) {
		for (int i = 0; i < contents.length; i++)
			contentVector.add(contents[i]);
	}

	public void setDoNotConsume(boolean dnc) {
		doNotConsume = dnc;

		// If a Surrogate-Capability header exist the request has come from the
		// edge
		// If EdgeCacheable is set on this fragment we must set doNotConsume =
		// false
		// If EdgeCacheable is set on any parent fragment we must set
		// doNotConsume = false;
		if (doNotConsume) {
			String s = ESISupport.getHeaderDirect(request, "Surrogate-Capability");
			if (s != null) {
				if (getFragmentInfo().isEdgeable()) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
						Tr
								.debug(tc,
										"fragment is EdgeCacheable, setting doNotConsume=false");
					doNotConsume = false;
					fragmentInfo.setConsumeExcludeList(null);
				} else {
					FragmentComposer curFC = getParent();
					while (curFC != null) {
						if (curFC.getFragmentInfo().isEdgeable()) {
							if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
								Tr
										.debug(tc,
												"EdgeCacheable parent found, setting doNotConsume=false");
							doNotConsume = false;
							fragmentInfo.setConsumeExcludeList(null);
							break;
						}
						curFC = curFC.getParent();
					}
				}
			}
		}

		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.debug(tc, "setDoNotConsume: " + uri + " doNotConsume="
					+ doNotConsume);
	}

	public boolean getDoNotConsume() {
		return doNotConsume;
	}

	public void setConsumeSubfragments(boolean csf) {
		consumeSubfragments = csf;

		// If a Surrogate-Capability header exist the request has come from the
		// edge
		// If EdgeCacheable is set on this fragment we must set
		// consumeSubfragments = true
		if (this.isExternalPage() && !consumeSubfragments) {
			String s = ESISupport.getHeaderDirect(request, "Surrogate-Capability");
			if (s != null) {
				if (getFragmentInfo().isEdgeable()) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
						Tr
								.debug(tc,
										"fragment is EdgeCacheable, setting consumeSubfragments=true");
					consumeSubfragments = true;
				}
			}
		}

		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.debug(tc, "setConsumeSubfragments: " + uri
					+ " consumeSubfragments=" + consumeSubfragments);
	}

	public boolean getConsumeSubfragments() {
		return consumeSubfragments;
	}

	/**
	 * getHasCachableConsumingParent - returns boolean to indicate if the fragment has a cacheable consuming parent.
	 * 
	 * @return either true if the fragment has a cacheable consuming parent.
	 * @ibm-private-in-use
	 */
	public boolean getHasCacheableConsumingParent() {
		return hasCacheableConsumingParent;
	}

	public void setHasCacheableConsumingParent(boolean hccp) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.debug(tc, this +" hasCacheableConsumingParent set to " + hccp);
		hasCacheableConsumingParent = hccp;
	}

	/**
	 * setBufferSize Sets the preferred buffer size for the body of the
	 * response. The servlet container will use a buffer at least as large as
	 * the size requested. The actual buffer size used can be found using
	 * getBufferSize.
	 */
	public void setBufferSize(int size) {
		if (size > bufferSize) {
			bufferSize = size;
		}
	}

	/**
	 * getBufferSize Returns the actual buffer size used for the response. If no
	 * buffering is used, this method returns 0.
	 * 
	 */
	public int getBufferSize() {
		return bufferSize;
	}

	/**
	 * resetBuffer Clears the content of the underlying buffer in the response
	 * without clearing headers or status code. If the response has been
	 * committed, this method throws an IllegalStateException.
	 */

	public void resetBuffer() {
		
		if (cacheProxyWriter != null)
			cacheProxyWriter.resetBuffer();
		
		if (cacheProxyOutputStream != null)
			cacheProxyOutputStream.resetBuffer();
	}

	/*
	 * requestFinished called when the request is finished
	 */
	public void requestFinished() {
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Tr.entry(tc, "requestFinished", this);
			Tr.debug(tc, "Flushing: "+uri);
		}
		
		if (cacheProxyWriter != null)
			cacheProxyWriter.flush();

		if (cacheProxyOutputStream != null) {
			try {
				cacheProxyOutputStream.flush();
			} catch (IOException ex) {
                FFDCFilter.processException(ex, this.getClass().getName() + ".requestFinished()", "413");
			}
		}

		saveCachedData();
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Tr.exit(tc, "requestFinished");
		}		
	}

	public boolean shouldCacheOutput() {
		if (cacheType == POPULATED_CACHE) {
			return true;
		} else if (cacheType == WAS_CACHED) {
			// if our parent was externally cacheable or we are externally
			// cacheable then we need to cache our output
			return parentExternallyCacheable || isExternallyCached();
		}
		return false;
	}

	public boolean isExternallyCached() {
		if (isExternalPage()) {
			// fragmentInfo == null means the page is not cached
			if (fragmentInfo != null) {
				String externalCacheId = fragmentInfo.getExternalCacheGroupId();
				return !(externalCacheId == null || externalCacheId.equals(""));
			}
		}
		
		return false;
	}

	public void setParentExternallyCacheable(boolean parentExternal) {
		parentExternallyCacheable = parentExternal;
	}

	// Returns true if 1) current page is externally cacheable OR
	// 2) external page is externally cacheable and CSF=true OR
	// 3) external page is externally cacheable and one of
	// my parent has CSF=true
	public boolean shouldExternalCacheOutput() { 

		if (isExternallyCached())
			return true;
		else {
			FragmentComposer externalFC = getExternalFragmentComposer();
			if (externalFC.isExternallyCached()) {
				if (externalFC.consumeSubfragments)
					return true;
				else if (cacheType == WAS_CACHED
						|| cacheType == POPULATED_CACHE)
					return true;
				else {
					FragmentComposer currentFC = parent;
					while (currentFC != null && !currentFC.consumeSubfragments)
						currentFC = currentFC.parent;
					if (currentFC != null && currentFC.consumeSubfragments)
						return true;
				}
			}

		}

		return false;
	}

	// used to save data-ids and template in the external request if it is
	// externally cacheable
	protected void saveExternalInvalidationIds() {

		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.debug(tc, "saveExternalInvalidationIds uri=" + uri);
		
		if (shouldExternalCacheOutput() && fragmentInfo.getId() != null) {			
			FragmentComposer externalFC = getExternalFragmentComposer();
			externalFC.externalDataIds.union(fragmentInfo.getDataIds());
			externalFC.externalDataIds.add(fragmentInfo.getId());
			externalFC.externalTemplates.union(fragmentInfo.getTemplates());

		}
		// ibm.com end
	}

	
	/**
	 * This adds a FragmentComposer for a contained entry.
	 * 
	 * @param fragmentComposer
	 *            The FragmentComposer to be added.
	 */
	public void startChildFragmentComposer(FragmentComposer fragmentComposer)
			throws IOException {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.debug(tc, "starting fragment composer for "
					+ fragmentComposer.uri + " parent=" + uri);

		saveCachedData();
		contentVector.add(fragmentComposer); 
		
		if (cacheProxyWriter != null)
			cacheProxyWriter.setCaching(false);
		
		if (cacheProxyOutputStream != null)
			cacheProxyOutputStream.setCaching(false);

		// code added for hasCacheableParent
		if ((fragmentComposer.getRequest().getCaching() || 
				fragmentComposer.getHasCacheableConsumingParent())) {
			
			// turn off caching for the child request since it is not cacheable
			// and no parent is consuming subfragments
			if (fragmentComposer.cacheProxyWriter != null)
				fragmentComposer.cacheProxyWriter.setCaching(true);
			
			if (fragmentComposer.cacheProxyOutputStream != null)
				fragmentComposer.cacheProxyOutputStream.setCaching(true);
		}

		// let our child fragment know if we are externally cacheable so it will
		// no whether or not to cache its output
		fragmentComposer.setParentExternallyCacheable(isExternallyCached());
		this.currentChild = fragmentComposer;		
	}
	
	public void endChildFragmentComposer(FragmentComposer child) throws IOException {
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.debug(tc, "ending fragment composer for " +currentChild.uri+ " parent=" + uri);
		
		//For an ARD parent the it is not gauranteed that 
		child.saveCachedData();
		childComposerEnded(child);

		// code added for hasCacheableConsumingParent
		boolean cacheable = request.getCaching() || getHasCacheableConsumingParent();

		if (cacheProxyWriter != null) {
			if (isJSTLImport) {
				cacheProxyWriter.setDelayWrite(true, cacheable);
				setJSTLImport(false);
			} else
				cacheProxyWriter.setCaching(cacheable);
		}

		if (cacheProxyOutputStream != null) {
			if (isJSTLImport) {
				cacheProxyOutputStream.setDelayWrite(true, cacheable);
				setJSTLImport(false);
			} else
				cacheProxyOutputStream.setCaching(cacheable);
		}
		
		if (false == this.isAsyncDispatch()){
			this.currentChild = null; 
		} 
	}

	// this method is called whenever a cacheable child is ended...
	// used to propagate dataids and templates into consuming parents...
	protected void childComposerEnded(FragmentComposer childComposer) {
		if (getConsumeSubfragments()) {
			if (childComposer.fragmentInfo != null
					&& !childComposer.fragmentInfo.getBuildEsiInclude()) {
				// if this fragment is consuming subfragments, we need to
				// add the subfragments' templates to the consuming fragmentInfo
				// object.
				if (!childComposer.getDoNotConsume()) {
					Enumeration templates = childComposer.fragmentInfo
							.getTemplates();
					while (templates.hasMoreElements())
						fragmentInfo.templates.add(templates.nextElement());
					Enumeration dataIds = childComposer.fragmentInfo
							.getDataIds();
					while (dataIds.hasMoreElements())
						fragmentInfo.dataIds.add(dataIds.nextElement());
					String tmp = childComposer.fragmentInfo.getId();
					if (tmp != null)
						fragmentInfo.dataIds.add(tmp);
				}
			}
		}
		// propagate up the chain
		if (parent != null) {
			if (!childComposer.getDoNotConsume() && !getDoNotConsume())
				parent.childComposerEnded(childComposer);
		}
	}

	public void saveCachedData() {
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && null != cacheProxyWriter){
			Tr.debug(tc, "saveCachedData uri=" + uri+" cacheProxyWriter: " + cacheProxyWriter +" caching="+ cacheProxyWriter.isCaching());			
		}
		
		if (null != cacheProxyWriter &&  outputStyle == CHAR && cacheProxyWriter.isCaching()) {
			char[] c = cacheProxyWriter.getCachedData();
			if (c.length > 0){
				contentVector.add(c);
//				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
//					 Tr.debug(tc, "saveCachedData " +cacheProxyWriter + " : [" + new String(c)+"]");
			}
			cacheProxyWriter.resetBuffer();
		} else if (null != cacheProxyOutputStream && outputStyle == BYTE && cacheProxyOutputStream.isCaching()) {
			byte[] b = cacheProxyOutputStream.getCachedData();
			if (b.length > 0){
				contentVector.add(b);
//				 if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
//					  Tr.debug(tc, "saveCachedData " + cacheProxyOutputStream + " : [" + new String(b)+"] ");
			}
			cacheProxyOutputStream.resetBuffer();			
		}
		
//		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
//			Tr.debug(tc, "saveCachedData uri=" + uri+" contentVector: " + contentVector);			
//		}
	}

	private OutputStream obtainOutputStream() throws IOException {
		OutputStream responseOutputStream;
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())			
			Tr.debug(tc, "getting OutputStream from " + response.getResponse() 
					+ ", buffering output = " + response.isBufferingOutput()
					+ ", hasParent == " + (parent != null));
		
		if (response.isBufferingOutput()) {			
			responseOutputStream = response.getBufferedOutputStream();			
		} else {			
			responseOutputStream = ((HttpServletResponse) response.getResponse()).getOutputStream(); 
		}
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.debug(tc, "got OutputStream: " + responseOutputStream);
		
		return responseOutputStream;
	}

	private Writer obtainWriter() throws IOException {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.debug(tc, "getting Writer from " + response.getResponse()
					+ ", buffering output = " + response.isBufferingOutput()
					+ ", hasParent == " + (parent != null)); 
		Writer responsePrintWriter;
		if (response.isBufferingOutput()) {
			responsePrintWriter = response.getBufferedWriter();
		} else {
			responsePrintWriter = ((HttpServletResponse) response.getResponse())
					.getWriter();
		}
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.debug(tc, "got Writer: " + responsePrintWriter);
		return responsePrintWriter;
	}

	private FragmentComposer getExternalFragmentComposer() {
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Tr.debug(tc, "getExternalFragmentComposer");
		}
		FragmentComposer current = this;
		while (null != current && !current.isExternalPage()) {
			current = current.parent;
		}
		return current; 
	}

	/**
	 * This returns the outputStream variable.
	 * 
	 * @return The outputStream variable.
	 */
	public javax.servlet.ServletOutputStream getOutputStream()
			throws IOException
	{
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			Tr.entry(tc, "getOutputStream: " + uri);
		
		outputStyle = BYTE;
		
		if (cacheProxyOutputStream == null) {
			cacheProxyOutputStream = new CacheProxyOutputStream(); 
		}
 
		cacheProxyOutputStream.setOutputStream(obtainOutputStream());
		
		// we need to cache output if a) we are currently executing in our
		// fragment and we should cache or b) we are consuming subfragments
		// boolean consumeSubfragments = fragmentInfo == null ? false :
		// fragmentInfo.getConsumeSubfragments();
		// cacheProxyOutputStream.setCaching((currentChild == null &&
		// shouldCacheOutput()) || consumeSubfragments);
		// cacheProxyOutputStream.setCaching(currentChild == null);
		cacheProxyOutputStream.setCaching(
					( currentChild == null) &&
					( request.getCaching() || getHasCacheableConsumingParent() ) 
				);

		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.debug(tc, "returning " + cacheProxyOutputStream
					+ ", pointing to "
					+ cacheProxyOutputStream.getOutputStream());

		return cacheProxyOutputStream;
	}

	/**
	 * This returns the printWriter variable.
	 * 
	 * @return The printWriter variable.
	 */
	public PrintWriter getPrintWriter() throws IOException {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			Tr.entry(tc, "getPrintWriter: " + uri);
		
		outputStyle = CHAR;
		
		if (cacheProxyWriter == null){
			cacheProxyWriter = new CacheProxyWriter(); //No ARD Possibility
		}  

		cacheProxyWriter.setWriter(obtainWriter());
		
		// we need to cache output if a) we are currently executing in our
		// fragment and we should cache or b) we are consuming subfragments
		// boolean consumeSubfragments = fragmentInfo == null ? false :
		// fragmentInfo.getConsumeSubfragments();
		// cacheProxyWriter.setCaching((currentChild == null &&
		// shouldCacheOutput()) || consumeSubfragments);
		// cacheProxyWriter.setCaching(currentChild == null);
		cacheProxyWriter.setCaching(
				( currentChild == null) && 
				( request.getCaching() || getHasCacheableConsumingParent()));
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.debug(tc, this +" returning " + cacheProxyWriter + ", pointing to "
					+ cacheProxyWriter.getWriter() + " caching=" + cacheProxyWriter.isCaching());
		
		return cacheProxyWriter;
	}

	private Object[] excludeChildFragments(Object contents[], ArrayList excludes) {
		if (contents == null) {
			return contents;
		}
		for (int i = 0; i < contents.length; i++) {
			if (contents[i] instanceof MementoEntry) {
				MementoEntry me = (MementoEntry) contents[i];

				String name = me.getTemplate();
				int j = name.indexOf('?');
				if (j != -1)
					name = name.substring(0, j);

				if (excludes.contains(name)
						|| excludes.contains(me.getServletClassName())) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
						Tr.debug(tc, "Fragment: " + name
								+ " excluded from CacheEntry: " + getURI());
					me.contents = null;
					me.setDoNotConsume(true);
				} else {
					excludeChildFragments(me.contents, excludes);
				}
			}
		}

		return contents;
	}

	/**
	 * This builds and returns a FragmentComposerMemento for this
	 * FragmentComposer's entry.
	 * 
	 * @return The FragmentComposerMemento for this FragmentComposer's entry.
	 */
	public FragmentComposerMemento getMemento(CacheProxyRequest request) throws IOException {
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			Tr.entry(tc, "getMemento: ",new Object[]{uri, this});
		
		if (!shouldCacheOutput()) {
			throw new IllegalStateException(
					"Cannot obtain cache memento since caching was not enabled for this fragment");
		}
		saveCachedData();

		FragmentComposerMemento memento = new FragmentComposerMemento();
		String externalCacheGroupId = fragmentInfo.getExternalCacheGroupId();
		memento.setExternalCacheGroupId(externalCacheGroupId);
		memento.setConsumeSubfragments(getConsumeSubfragments());
		memento.setOutputStyle(outputStyle);
		memento.setContentType(contentType);
        memento.setCharacterEncoding(characterEncoding);

		Object contents[] = contentVector.toArray();

		for (int i = 0; i < contents.length; i++) {
			// we need to convert any fragment composers to mementoentries
			if (contents[i] instanceof FragmentComposer) {
				FragmentComposer fragmentComposer = (FragmentComposer) contents[i];
				
				String cp1 = getContextPath();
				String cp2 = fragmentComposer.getContextPath();
				boolean ignore = cp1 != null && cp2 != null && cp1.equals(cp2);
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
					Tr.debug(tc, "ignore context paths? " + ignore);

				MementoEntry mementoEntry = fragmentComposer.getMementoEntry(ignore);
				mementoEntry.setOutputStyle(fragmentComposer.outputStyle);
				mementoEntry.setExternallyCacheable(fragmentComposer.shouldExternalCacheOutput());
				mementoEntry.setAsync(isAsyncDispatch());
				
				if (getConsumeSubfragments()
						&& !fragmentComposer.getDoNotConsume()
						&& !fragmentComposer.isDiscardJSPContent()) {						
					mementoEntry.addContents(fragmentComposer.contentVector.toArray());
					
				} else if (fragmentComposer.isDiscardJSPContent()){					
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())					
						Tr.debug(tc, "Creating an empty mementoEntry for fragmentComposer -->"+	uri);					
					mementoEntry.addContents(new ArrayList(0).toArray());
				}					
				
				mementoEntry.setServletClassName(fragmentComposer.getServletClassName());
				mementoEntry.setDoNotConsume(fragmentComposer.getDoNotConsume());
				contents[i] = mementoEntry;	

				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
					Tr.debug(tc, "Created MementoEntry for "+ fragmentComposer.getURI());
			}
		}
		String excludeList[] = fragmentInfo.getConsumeExcludeList();
		if (excludeList != null) {
			ArrayList excludes = new ArrayList();
			for (int i = 0; i < excludeList.length; i++) {
				excludes.add(excludeList[i]);
			}
			contents = excludeChildFragments(contents, excludes);
		}
		
		memento.addContents(contents);

		// in addition to the content vector, the request holds any attributes
		// that were set during this servlet's
		// execution. Get them and add them to the memento.
		CacheProxyRequest.Attribute saveList[] = getSaveAttributeList(request); // ST-begin
		if (saveList != null && saveList.length > 0) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
				Tr.debug(tc, "setting changed attrs on memento " + uri + ": "
						+ saveList);
			boolean isShared = false;
			if (fragmentInfo != null)
				isShared = !fragmentInfo.isNotShared();
			if (isShared) {
				String exceptionMessage = null;
				byte[] array = null;
				try {
					array = SerializationUtility.serialize(saveList);
				} catch (Exception e) {
					exceptionMessage = e.toString();
					// com.ibm.ws.ffdc.FFDCFilter.processException(e,
					// "com.ibm.ws.cache.CacheEntry.prepareForSerialization",
					// "206", this);
				}
				if (array != null) {
					memento.addAttributeBytes(array);
				} else {
					memento.addAttributes(saveList);

					Tr.error(tc,"dynacache.error",new Object[] { 
							"NON_SERIALIZABLE_ATTRIBUTES_FOUND. SET_SHARE_POLICY_TO_NOT_SHARED.  uri="
							+ uri + "  exception=" + exceptionMessage });
				}
			} else {
				memento.addAttributes(saveList);
			}
		}

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			Tr.exit(tc, "getMemento: ", Arrays.deepToString(contents));

		return memento;
	}

	/**
	 * This builds and returns a MementoEntry for this FragmentComposer's entry.
	 * 
	 * @return The MementoEntry for this FragmentComposer's entry.
	 */
	private final MementoEntry getMementoEntry(boolean ignoreContextPath) { 
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			Tr.entry(tc, "getMementoEntry: " + ignoreContextPath);

		try {
			
			if (uri == null) {
				throw new IllegalStateException("fragmentComposer.uri is null for "+ this);
			}

			CacheProxyRequest.Attribute endAttributeTable[] = null;
			byte[] endAttributeTableBytes = null;

			if (getHasCacheableConsumingParent() && !getDoNotConsume()) {
				CacheProxyRequest.Attribute saveList[] = saveAttributeList;

				if (saveList != null && saveList.length > 0) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
						Tr.debug(tc, "setting changed attrs on mementoEntry "
								+ uri + ": ", (Object) saveList);
					boolean isShared = false;
					if (fragmentInfo != null)
						isShared = !fragmentInfo.isNotShared();
					if (isShared) {
						String exceptionMessage = null;
						byte[] array = null;
						try {
							array = SerializationUtility.serialize(saveList);
						} catch (Exception e) {
							exceptionMessage = e.toString();
						}
						if (array != null) {
							endAttributeTableBytes = array;
						} else {
							endAttributeTable = saveList;
							Tr.error(tc,"dynacache.error",
									new Object[] { "NON_SERIALIZABLE_ATTRIBUTES_FOUND. SET_SHARE_POLICY_TO_NOT_SHARED.  uri="
									+ uri + "  exception=" + exceptionMessage });
						}
					} else {
						attributeTable = saveList;
					}
				}
			}
			// -----------------------------------------------------------------------
			MementoEntry me = null;
			String cacheName = getFragmentInfo().getInstanceName();
			
			// if we are not saving attributes, then null out the attributes...
			if (attributeTableBytes == null) {
				if (ignoreContextPath)
					me = new MementoEntry(cacheName, uri, attributeTable,
							endAttributeTable, include, namedDispatch, null);
				else
					me = new MementoEntry(cacheName, uri, attributeTable,
							endAttributeTable, include, namedDispatch,
							getContextPath());
			} else {
				if (ignoreContextPath)
					me = new MementoEntry(cacheName, uri, attributeTableBytes,
							endAttributeTableBytes, include, namedDispatch, null);
				else
					me = new MementoEntry(cacheName, uri, attributeTableBytes,
							endAttributeTableBytes, include, namedDispatch,
							getContextPath());
			}
			
			return me;
			
		} finally {
			if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
				Tr.exit(tc, "getMementoEntry");
		}
	}
	
	public void copyContentForParents() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.entry(tc, "copyContentForParents:" + uri);
		
		Object contents[] = contentVector.toArray();

		for (int i = 0; i < contents.length; i++) {			
			
			if (contents[i] instanceof FragmentComposer) { // we need to convert any fragment composers to mementoentries
				FragmentComposer fragmentComposer = (FragmentComposer) contents[i];
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
					Tr.debug(tc, "copyContentForParents: examining " + fragmentComposer);
				
				String cp1 = getContextPath();
				String cp2 = fragmentComposer.getContextPath();
				boolean ignore = cp1 != null && cp2 != null && cp1.equals(cp2);
				MementoEntry contentME = fragmentComposer.getMementoEntry(ignore);
				contentME.setOutputStyle(fragmentComposer.outputStyle);
				contentME.setExternallyCacheable(fragmentComposer.shouldExternalCacheOutput());
				
				if (!fragmentComposer.getDoNotConsume() && !fragmentComposer.isDiscardJSPContent()){
					contentME.addContents(fragmentComposer.contentVector.toArray());

				} else if (fragmentComposer.isDiscardJSPContent()){
					contentME.addContents(new ArrayList(0).toArray());
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
						Tr.debug(tc, "Creating an EMPTY MementoEntry for fragmentComposer -->"
								+	uri + " fragmentComposer.isDiscardJSPContent()-->"+	fragmentComposer.isDiscardJSPContent());
					}				
				}
				
				contentME.setDoNotConsume(fragmentComposer.getDoNotConsume());
				contentVector.set(i, contentME);

				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
					Tr.debug(tc, "Created MementoEntry for "+ fragmentComposer.getURI()+ " with "+ contentVector);
			}
		}

		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.exit(tc, "copyContentForParents", contentVector);
	}

	public String getContextPath() {
		return contextPath;
	}

	/**
	 * This method returns all the attributea that are configured to be stored
	 * in the memento and found on the request object changed attribute list
	 */

	protected CacheProxyRequest.Attribute[] getSaveAttributeList(
			CacheProxyRequest request) { 
		FragmentInfo fi = (FragmentInfo) request.getFragmentInfo();
		CacheProxyRequest.Attribute h[] = request.getChangedAttributes();
		String[] excludeList = fi.getAttributeExcludeList();
		CacheProxyRequest.Attribute saveList[] = null;

		if (!fi.getStoreAttributes() && excludeList == null)
			return null;
		else if (fi.getStoreAttributes() && excludeList == null)
			return h;
		else if (!fi.getStoreAttributes() && excludeList != null) {
			ArrayList tmpList = null;
			for (int i = 0; i < excludeList.length; i++) {
				for (int j = 0; h != null && j < h.length; j++) {
					if (excludeList[i].equals((String) h[j].key)) {
						if (tmpList == null)
							tmpList = new ArrayList();
						tmpList.add(h[j]);
					}
				}
			}
			if (tmpList != null)
				saveList = (CacheProxyRequest.Attribute[]) tmpList
						.toArray(new CacheProxyRequest.Attribute[0]);

		} else if (fi.getStoreAttributes() && excludeList != null) {
			ArrayList tmpList = null;
			for (int i = 0; h != null && i < h.length; i++) {
				boolean exclude = false;
				for (int j = 0; j < excludeList.length; j++) {
					if (excludeList[j].equals((String) h[i].key)) {
						exclude = true;
						break;
					}
				}
				if (!exclude) {
					if (tmpList == null)
						tmpList = new ArrayList();
					tmpList.add(h[i]);
				}

			}
			if (tmpList != null)
				saveList = (CacheProxyRequest.Attribute[]) tmpList
						.toArray(new CacheProxyRequest.Attribute[0]);

		}

		return saveList;
	}

	/**
	 * This does a transitive closure on contained FragmentComposers to fully
	 * expand an entry into a character array. The value will be sent to an
	 * external cache
	 * 
	 * @param charEnc
	 *            the Character encoding to use for the returned byte array
	 * @return A character array containing the fully expanded entry.
	 */
	public byte[] toByteArray(String charEnc) throws IOException {
		if (!shouldExternalCacheOutput()) {
			throw new IllegalStateException(
					"Cannot obtain byteArray since caching was not enabled for this fragment");
		}
		saveCachedData();
		byte[] array = null;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		OutputStreamWriter osw = null;
		int len = contentVector.size();
		for (int i = 0; i < len; i++) {
			Object object = contentVector.get(i);
			if (object instanceof byte[]) {
				if (outputStyle != BYTE)
					throw new IllegalStateException("FragmentComposer " + this
							+ " with outputStyle " + outputStyle
							+ " cannot contain byte arrays");
				outputStream
						.write((byte[]) object, 0, ((byte[]) object).length);
				outputStream.flush();
				continue;
			}
			if (object instanceof char[]) {
				if (outputStyle != CHAR)
					throw new IllegalStateException("FragmentComposer " + this
							+ " with outputStyle " + outputStyle
							+ " cannot contain char arrays");
				if (osw == null) {
					try {
						osw = new OutputStreamWriter(outputStream, charEnc);
					} catch (UnsupportedEncodingException uex) {
						com.ibm.ws.ffdc.FFDCFilter
								.processException(
										uex,
										"com.ibm.ws.cache.servlet.FragmentComposer.toByteArray",
										"671", this);
						osw = new OutputStreamWriter(outputStream);
					}
				}
				osw.write((char[]) object, 0, ((char[]) object).length);
				osw.flush();
				continue;
			}
			if (object instanceof ResponseSideEffect) {
				continue;
			}

			if (object instanceof java.lang.String) { // NK2
				continue;
			}

			if (object instanceof MementoEntry) {
				MementoEntry mementoEntry = (MementoEntry) object;
				byte[] meContents = mementoEntry.toByteArray(charEnc);
				outputStream.write(meContents, 0, meContents.length);
				continue;
			}

			if (!(object instanceof FragmentComposer)) {
				throw new IllegalStateException(
						"FragmentComposer should only contain "
								+ "other FragmentComposers, "
								+ "Byte Arrays, Char Arrays, "
								+ "and ResponseSideEffects");
			}
			// FragmentComposer
			FragmentComposer fragmentComposer = (FragmentComposer) object;
			array = fragmentComposer.toByteArray(charEnc);
			outputStream.write(array, 0, array.length);
		}

		// don't worry about request attribute changes, they don't get sent to
		// external caches

		return outputStream.toByteArray();
	}

	/**
	 * This returns the cacheType variable.
	 * 
	 * @return Either NOT_CACHED, WAS_CACHED or POPULATED_CACHE.
	 * @ibm-private-in-use
	 */
	public int getCacheType() {
		return cacheType;
	}

	/**
	 * This sets the cacheType variable.
	 * 
	 * @param cacheType
	 *            Either NOT_CACHED, WAS_CACHED or POPULATED_CACHE.
	 */
	public void setCacheType(int cacheType) {
		this.cacheType = cacheType;
	}

	/**
	 * getParent - returns the parent fragment composer for this fragment composer.
	 * 
	 * @return returns the parent fragment composer for this fragment composer.
	 * @ibm-private-in-use
	 */
	public FragmentComposer getParent() {
		return parent;
	}

	public void setParent(FragmentComposer fc) {
		this.parent = fc;
	}

	protected void setJSTLImport(boolean flag) // jstl fix begin
	{
		isJSTLImport = flag;
	} // jstl fix end

	/**
	 * This returns the externalPage variable.
	 * 
	 * @return True indicates this FragmentComposer's entry is an externally
	 *         requested page.
	 */
	public boolean isExternalPage() {
		return externalPage;
	}

	/**
	 * This tells this FragmentComposer that its entry is an externally
	 * requested page.
	 * 
	 * @param externalPage
	 *            True indicates this FragmentComposer's entry is an externally
	 *            requested page.
	 */
	public void setExternalPage(boolean externalPage) {
		this.externalPage = externalPage;
	}

	/**
	 * This sets the request attributes in byte array form.
	 * 
	 * @param attributeTableBytes
	 *            The attributes.
	 */
	public void setAttributeTableBytes(byte[] attributeTableBytes) {
		this.attributeTableBytes = attributeTableBytes;
	}

	/**
	 * This gets the attributeTableBytes variable.
	 * 
	 * @return The attributeTableBytes variable.
	 */
	public byte[] getAttributeTableBytes() {
		return attributeTableBytes;
	}

	/**
	 * This sets the request attributes in Hashtable form.
	 * 
	 * @param attributeTable
	 *            The attributes.
	 */
	public void setAttributeTable(CacheProxyRequest.Attribute attributeTable[]) {
		this.attributeTable = attributeTable;
	}

	/**
	 * This gets the attributeTable variable.
	 * 
	 * @return The attributeTable variable.
	 */
	public CacheProxyRequest.Attribute[] getAttributeTable() {
		return attributeTable;
	}

	/**
	 * This returns the fragmentInfo variable.
	 * 
	 * @return The fragmentInfo variable.
	 * @ibm-private-in-use
	 */
	public FragmentInfo getFragmentInfo() {
		return fragmentInfo;
	}

	/**
	 * This sets the fragmentInfo variable.
	 * 
	 * @param fragmentInfo
	 *            The new fragmentInfo.
	 */
	public void setFragmentInfo(FragmentInfo fragmentInfo) {
		this.fragmentInfo = fragmentInfo;
	}

	/**
	 * This returns the uri variable.
	 * 
	 * @return The uri variable.
	 */
	public String getURI() {
		return uri;
	}

	/**
	 * This sets the uri variable.
	 * 
	 * @param uri
	 *            The new uri.
	 */
	public void setURI(String uri) {
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Tr.debug(tc, this + " URI set to :"+uri);
		}
		this.uri = uri;
	}

	/**
	 * This returns the servletClassName variable.
	 * 
	 * @return The servletClassName variable.
	 */
	public String getServletClassName() {
		return servletClassName;
	}

	/**
	 * This sets the uri variable.
	 * 
	 * @param uri
	 *            The new uri.
	 */
	public void setServletClassName(String servletClassName) {
		this.servletClassName = servletClassName;
	}

	protected CacheProxyRequest getRequest() {
		return request;
	}

	protected CacheProxyResponse getResponse() {
		return response;
	}

	/**
	 * This gets the include variable.
	 * 
	 * @return The include variable.
	 */
	public boolean getInclude() {
		return include;
	}

	/**
	 * This sets the include variable.
	 * 
	 * @param include
	 *            The new include value.
	 */
	public void setInclude(boolean include) {
		this.include = include;
	}

	/**
	 * This gets the namedDispatch variable.
	 * 
	 * @return The namedDispatch variable.
	 */
	public boolean getNamedDispatch() {
		return namedDispatch;
	}

	/**
	 * This sets the namedDispatch variable.
	 * 
	 * @param include
	 *            The new namedDispatch value.
	 */
	public void setNamedDispatch(boolean namedDispatch) {
		this.namedDispatch = namedDispatch;
	}

	/**
	 * This sets the expiration time of the entry if it is earlier than the
	 * current setting.
	 * 
	 * @param expirationTime
	 *            The expiration time.
	 */
	public void setExpirationTime(long expirationTime) {
		if ((expirationTime >= 0) && (expirationTime < this.expirationTime)) {
			this.expirationTime = expirationTime;
		}
	}

	/**
	 * This gets the expiration time of the entry.
	 * 
	 * @return The expiration time.
	 */
	public long getExpirationTime() {
		long expirationTime = this.expirationTime;
		long temp = fragmentInfo.getExpirationTime();
		if ((temp >= 0) && (temp < expirationTime)) {
			expirationTime = temp;
		}
		int len = contentVector.size();
		for (int i = 0; i < len; i++) {
			Object object = contentVector.get(i);
			if (!(object instanceof FragmentComposer)) {
				continue;
			}
			FragmentComposer fragmentComposer = (FragmentComposer) object;
			temp = fragmentComposer.getExpirationTime();
			if ((temp >= 0) && (temp < expirationTime)) {
				expirationTime = temp;
			}
		}
		if (expirationTime == Long.MAX_VALUE) {
			return -1; // no timeout
		}
		return expirationTime;
	}

	/**
	 * This gets the time when this entry was created.
	 * 
	 * @return The creation timestamp.
	 */
	public long getTimeStamp() {
		long timeStamp = this.timeStamp;

		int len = contentVector.size();
		for (int i = 0; i < len; i++) {
			Object object = contentVector.get(i);
			if (!(object instanceof FragmentComposer)) {
				continue;
			}
			FragmentComposer fragmentComposer = (FragmentComposer) object;
			long temp = fragmentComposer.getTimeStamp();
			if (temp > timeStamp) {
				timeStamp = temp;
			}
		}
		return timeStamp;
	}

	/**
	 * This sets the time when this entry was created.
	 * 
	 * @param timeStamp
	 *            The creation timestamp.
	 */
	public void setTimeStamp(long timeStamp) {
		if (timeStamp <= this.timeStamp) {
			return;
		}
		this.timeStamp = timeStamp;
	}

	/**
	 * This returns all of the invalidation ids, including both cache ids and
	 * data ids. It visits this FragmentComposer's entry and does a transitive
	 * closure on the fragments contained by it. It is used to in externally
	 * cached pages.
	 * 
	 * @return The set of all data ids.
	 */
	public ValueSet getAllInvalidationIds()
			throws UnexternalizablePageException { // ibm.com begin
		if (cacheType == NOT_CACHED) {
			throw new UnexternalizablePageException(
					"FragmentComposer is NOT_CACHED, therefore, "
							+ "you cannot externalize a top level page which includes it.");
		}
		if (fragmentInfo == null) {
			throw new UnexternalizablePageException(
					"FragmentInfo should not be null, or FragmentComposer "
							+ "should be marked NOT_CACHED");
		}
		ValueSet dataIds = new ValueSet(23);
		dataIds.add(fragmentInfo.getId());
		dataIds.union(fragmentInfo.dataIds);

		// add childrens dataids
		dataIds.union(externalDataIds);

		return dataIds;
	} // ibm.com end

	/**
	 * This returns all of the Templates. It visits this FragmentComposer's
	 * entry and does a transitive closure on the fragments contained by it. It
	 * is used to in externally cached pages.
	 * 
	 * @return The set of all Templates.
	 */
	public ValueSet getAllTemplates() throws UnexternalizablePageException {
		if (cacheType == NOT_CACHED) {
			throw new UnexternalizablePageException(
					"FragmentComposer is NOT_CACHED, therefore, "
							+ "you cannot externalize a top level page which includes it.");
		}
		if (fragmentInfo == null) {
			throw new UnexternalizablePageException(
					"FragmentInfo should not be null, or FragmentComposer "
							+ "should be marked NOT_CACHED");
		}

		ValueSet templates = new ValueSet(11);
		Iterator it = fragmentInfo.templates.iterator();
		while (it.hasNext()) {
			templates.add(it.next());
		}
		// add childrens templates
		templates.union(externalTemplates);
		return templates;
	}

	/**
	 * This adds a header to the list of state that is remembered just prior to
	 * the execution of a JSP so that it can be executed again without executing
	 * its parent JSP.
	 * 
	 * @param key
	 *            The header key.
	 * @param value
	 *            The header value.
	 */
	public void setHeader(String key, String value, boolean isSet) {
		if (getConsumeSubfragments() || currentChild == null) {

			if (key == null) {
				throw new IllegalArgumentException("setHeader key parameter must be non-null");
			}
			HeaderSideEffect headerSideEffect = new HeaderSideEffect(key,
					value, isSet);
			contentVector.add(headerSideEffect);
		}
	}

	public void sendRedirect(String location) {
		// defer error generation to web container--133340
		/*
		 * if (location == null) { throw new
		 * IllegalArgumentException("sendRedirect(URI) uri must be non null"); }
		 */
		isSendRedirect = true; // NK4
		if (getConsumeSubfragments() || currentChild == null) {
			SendRedirectSideEffect srse = new SendRedirectSideEffect(location);
			contentVector.add(srse);
		}
	}

	public void addDynamicContentProvider(
			DynamicContentProvider dynamicContentProvider) throws IOException { 
		if (dynamicContentProvider == null) {
			throw new IllegalArgumentException(
					"Dynamic Content Provider must be non-null");
		}

		saveCachedData();
		contentVector.add(dynamicContentProvider);

		if (outputStyle == FragmentComposer.CHAR) {
			boolean caching = cacheProxyWriter.isCaching();
			cacheProxyWriter.setCaching(false);
			dynamicContentProvider.provideDynamicContent(request,
					cacheProxyWriter.getWriter());
			cacheProxyWriter.setCaching(true);
		} else if (outputStyle == FragmentComposer.BYTE) {
			boolean caching = cacheProxyOutputStream.isCaching();
			cacheProxyOutputStream.setCaching(false);
			dynamicContentProvider.provideDynamicContent(request,
					cacheProxyOutputStream.getOutputStream());
			cacheProxyOutputStream.setCaching(true);
		}
	}

	/**
	 * This adds a cookie to the list of state that is remembered just prior to
	 * the execution of a JSP so that it can be executed again without executing
	 * its parent JSP.
	 * 
	 * @param cookie
	 *            The cookie.
	 */
	public void addCookie(Cookie cookie) {
		if (( getConsumeSubfragments() || currentChild == null)) {

			if (cookie == null) {
				throw new IllegalArgumentException("cookie must be non-null");
			}

			boolean addCookie;
			if (fragmentInfo.getStoreCookies()) {
				addCookie = true;
				String[] cookieExclude = fragmentInfo.getCookieExcludeList();
				if (cookieExclude != null) {
					for (int i = 0; i < cookieExclude.length; i++) {
						if (cookie.getName().equals(cookieExclude[i]))
							addCookie = false;
					}
				}
			} else {
				addCookie = false;
				String[] cookieExclude = fragmentInfo.getCookieExcludeList();
				if (cookieExclude != null) {
					for (int i = 0; i < cookieExclude.length; i++) {
						if (cookie.getName().equals(cookieExclude[i]))
							addCookie = true;
					}
				}
			}

			if (addCookie) {
				AddCookieSideEffect addCookieSideEffect = new AddCookieSideEffect(
						cookie);
				contentVector.add(addCookieSideEffect);
			}
		}
	}

	/**
	 * This adds a date header to the list of state that is remembered just
	 * prior to the execution of a JSP so that it can be executed again without
	 * executing its parent JSP.
	 * 
	 * @param name
	 *            The date header name.
	 * @param value
	 *            The date header value.
	 */
	public void setDateHeader(String name, long value, boolean isSet) {
		
		if (getConsumeSubfragments() || currentChild == null) {

			if (name == null) {
				throw new IllegalArgumentException("name must be non-null");
			}
			DateHeaderSideEffect dateHeaderSideEffect = new DateHeaderSideEffect(
					name, value, isSet);

			contentVector.add(dateHeaderSideEffect);
		}
	}

	/**
	 * This adds a int header to the list of state that is remembered just prior
	 * to the execution of a JSP so that it can be executed again without
	 * executing its parent JSP.
	 * 
	 * @param name
	 *            The int header name.
	 * @param value
	 *            The int header value.
	 */
	public void setIntHeader(String name, int value, boolean isSet) {
		if (getConsumeSubfragments() || currentChild == null) {

			if (name == null) {
				throw new IllegalArgumentException("name must be non-null");
			}
			IntHeaderSideEffect intHeaderSideEffect = new IntHeaderSideEffect(
					name, value, isSet);

			contentVector.add(intHeaderSideEffect);
		}
	}

	/**
	 * This adds a status code to the list of state that is remembered just
	 * prior to the execution of a JSP so that it can be executed again without
	 * executing its parent JSP.
	 * 
	 * @param status
	 *            The status code.
	 */
	public void setStatus(int statusCode) {
		if (getConsumeSubfragments() || currentChild == null) {

			DefaultStatusSideEffect defaultStatusSideEffect = new DefaultStatusSideEffect(
					statusCode);

			contentVector.add(defaultStatusSideEffect);
		}
	}

	/**
	 * This adds a status code with comment to the list of state that is
	 * remembered just prior to the execution of a JSP so that it can be
	 * executed again without executing its parent JSP.
	 * 
	 * @param status
	 *            The status code.
	 * @param comment
	 *            The status comment.
	 */
	public void setStatus(int statusCode, String comment) {
		if (getConsumeSubfragments() || currentChild == null) {

			if (comment == null) {
				throw new IllegalArgumentException("comment must be non-null");
			}
			StatusSideEffect statusSideEffect = new StatusSideEffect(
					statusCode, comment);
			contentVector.add(statusSideEffect);
		}
	}

	/**
	 * This adds a content length to the list of state that is remembered just
	 * prior to the execution of a JSP so that it can be executed again without
	 * executing its parent JSP.
	 * 
	 * @param contentLength
	 *            The content length.
	 */
	public void setContentLength(int contentLength) {
		if (getConsumeSubfragments() || currentChild == null) {

			ContentLengthSideEffect contentLengthSideEffect = new ContentLengthSideEffect(
					contentLength);

			contentVector.add(contentLengthSideEffect);
		}
	}

	/**
	 * This adds a content type to the list of state that is remembered just
	 * prior to the execution of a JSP so that it can be executed again without
	 * executing its parent JSP.
	 * 
	 * @param contentType
	 *            The content type.
	 */
	public void setContentType(String contentType) {
		if (!response._gotWriter && !response._gotOutputStream)
			this.contentType = contentType;
		if (getConsumeSubfragments() || currentChild == null) {
			contentVector.add(contentType);
		}
	}
	
	public void setCharacterEncoding(String charEnc)
    {
        if(!response._gotWriter && !response._gotOutputStream)
        {
            this.characterEncoding = charEnc;
            if(tc.isDebugEnabled())
                Tr.debug(tc, "setting self CharacterEncoding " + charEnc);
        }
        if(getConsumeSubfragments() || currentChild == null)
        {
            CharacterEncodingSideEffect charEncSideEffect = new CharacterEncodingSideEffect(charEnc);
            contentVector.add(charEncSideEffect);
        }
    }

	/**
	 * This adds a locale to the list of state that is remembered just prior to
	 * the execution of a JSP so that it can be executed again without executing
	 * its parent JSP.
	 * 
	 * @param contentType
	 *            The content type.
	 */
	public void setLocale(Locale locale) {
		if (getConsumeSubfragments() || currentChild == null) {
			LocaleSideEffect localeSideEffect = new LocaleSideEffect(locale);
			contentVector.add(localeSideEffect);
		}
	}

	// ESI Surrogate variables
	public void setESIVersion(int esiVersion) {
		this.esiVersion = esiVersion;
	}

	public int getESIVersion() {
		return esiVersion;
	}
	
	public boolean isDiscardJSPContent() {
		return discardJSPContent;
	}

	public void setDiscardJSPContent(boolean discardJSPContent) {
		this.discardJSPContent = discardJSPContent;
	}
	
	public boolean isAsyncDispatch() {
		return this.asyncDispatch;
	}
	
	public void setAsyncDispatch(boolean async) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.debug(tc, this +" setAsyncDispatch "+ async);
		this.asyncDispatch = async;
		if (asyncDispatch){
			this.consumeSubfragments = true;
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
				Tr.debug(tc, this +" Servlet 3.0 Scenario: FORCING consumeSubfragments to true");
		}		
	}

	public boolean isAsyncDoubleDip() {
		return this.asyncDoubleDip;
	}
	
	public void setAsyncDoubleDip(boolean b) {
		this.asyncDoubleDip = b;
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.debug(tc, this +" Servlet 3.0 Scenario: AsyncDoubleDip "+b);
	}
}