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

import com.ibm.ws.cache.EntryInfo;

/**
 * This extends the EntryInfo to add variables unique to JSP fragments.
 * @ibm-private-in-use
 */
public class FragmentInfo extends EntryInfo implements com.ibm.websphere.servlet.cache.FragmentInfo {
    
	private static final long serialVersionUID = 7954469091084317937L;

	private boolean externalCacheGroupIdFlag = EntryInfo.UNSET;
	private boolean uriFlag = EntryInfo.UNSET;

	/**
	 * This is the URI for the JSP or Servlet that created this fragment.
	 */
	protected String uri = null;

	protected boolean consumeSubfragments = false;

	protected boolean doNotConsume = false;	

	protected boolean edgeable = false;

	protected String ESICacheId = null;

	protected String ESIQueryString = null;

	protected boolean uncacheable = false;

	protected String alternateUrl = null; //NK2

	protected boolean storeAttributes = true;

        protected boolean storeCookies = true;

	protected boolean buildEsiInclude= false; //WCS

	protected String[] attributeExcludeList = null; //ST
	
	protected String[] cookieExcludeList = null;

	protected String instanceName = null;  //MSI

	protected boolean ignoreGetPost = false;
	protected boolean ignoreCharEnc = false;
	
	protected boolean hasCacheableConsumingParent = false;
	
	protected String skipCacheAttribute = null;
	
	protected boolean doNotCache = false;
	
	protected String[] consumeExcludeList = null;

	/**
	 * This method resets this FragmentInfo object for reuse.
         * @ibm-private-in-use
	 */

	public void reset() {
		super.reset();
		externalCacheGroupIdFlag = EntryInfo.UNSET;
		uriFlag = EntryInfo.UNSET;
		externalCacheGroupId = null;
		uri = null;
		consumeSubfragments = false;
		doNotConsume = false;
		edgeable = false;
		ESICacheId = null;
		ESIQueryString = null;
		uncacheable = false;
		alternateUrl = null; //NK2
		storeAttributes = true;
        storeCookies = true;
		buildEsiInclude = false; //WCS
		attributeExcludeList = null; //ST
		cookieExcludeList = null;
		instanceName = null; //MSI
		ignoreGetPost = false;
		ignoreCharEnc = false;
		hasCacheableConsumingParent = false;
		skipCacheAttribute = null;
		doNotCache = false;
		consumeExcludeList = null;
	}

	/**
	 * This method returns the value of the ESICacheId used to identify this fragment when cached on the edge.
	 * 
	 * @return a String indicating the ESI CacheId
         * @ibm-private-in-use
	 */
	public String getESICacheId() {
		return ESICacheId;
	}
	
	/**
	* This method sets the value of the ESICacheId used to identify this fragment when cached on the edge.
	*
	* @param s the ESICacheId 
        * @ibm-private-in-use
	*/
	public void setESICacheId(String s) {
		this.ESICacheId = s;
	}
	
	/**
	 * This method returns the value of the ESI QueryString used when this fragment is cached on the edge.
	 * 
	 * @return a String specifying the ESI QueryString
         * @ibm-private-in-use
	 */
	public String getESIQueryString() {
		return ESIQueryString;
	}

	/**
	* This method sets the value of the ESI QueryString used when this fragment is cached on the edge.
	*
	* @param l the ESIQueryString 
        * @ibm-private-in-use
	*/
	public void setESIQueryString(String l) {
		this.ESIQueryString = l;
	}

	/**
	 * This method returns a value specifying if this fragment should be cached on the edge.
	 * 
	 * @return a boolean indicating if this fragment is edge cacheable
         * @ibm-private-in-use
	 */
	public boolean isEdgeable() {
		return edgeable;
	}

	/**
	* This method sets the value of the edgecacheable property tspecifying if this fragment should be cached on the edge.
	*
	* @param s the edgecacheable property
        * @ibm-private-in-use
	*/
	public void setEdgeable(boolean s) {
		this.edgeable = s;
	}
	
	/**
	* This method sets the alternateUrl variable used to invoke the servlet or JSP file.
	*
	* @param  url the alternateUrl 
        * @ibm-private-in-use
	*/
	public void setAlternateUrl(String url) { //NK2 begin
		this.alternateUrl = url;
	}

	/**
	 * This method returns the name of the alternate URL used to invoke the servlet or JSP file.
	 *  The property is valid only if the EdgeCacheable property also is set for the cache entry.
	 * 
	 * @return a String indicating if this fragment will build an Esi include.
         * @ibm-private-in-use
	 */
	public String getAlternateUrl() {
		return alternateUrl;
	} //NK2 end

	/**
	* This method sets the buildEsiInclude that determines if an <esi:include.. will be
	* generated for this fragment
	*
	* @param  b The buildEsiInclude variable
        * @ibm-private-in-use
	*/
	public void setBuildEsiInclude(boolean b) { //WCS begin
		this.buildEsiInclude = b;
	}

	/**
	 * This method returns the value of the buildEsiInclude variable that determines if an <esi:include.. will be
	 *  generated for this fragment
	 * 
	 * @return a boolean indicating if this fragment will generate an esi:include tag
         * @ibm-private-in-use
	 */
	public boolean getBuildEsiInclude() {
		return buildEsiInclude;
	} 					 //WCS end

	/**
	* This method sets whether the attributes for this request should be stored for this fragment.
	*
	* @param  b the storeAttributes variable
        * @ibm-private-in-use
	*/
	public void setStoreAttributes(boolean b) {
		storeAttributes = b;
	}

	/**
	 * This method returns a value specifying if the attributes for this request should be stored for this fragment.
	 * 
	 * @return a boolean indicating if this fragment should store attributes
         * @ibm-private-in-use
	 */
	public boolean getStoreAttributes() {
		return storeAttributes;
	}

	/**
	* This method sets whether the cookies for this request should be stored for this fragment.
	*
	* @param  b the storeCookies variable
        * @ibm-private-in-use
	*/
        public void setStoreCookies(boolean b) {
		storeCookies = b;
	}

    /**
	 * This method returns a value specifying if the cookies for this request should be stored for this fragment.
	 * 
	 * @return a boolean indicating if this fragment should store cookies
         * @ibm-private-in-use
	 */
	public boolean getStoreCookies() {
		return storeCookies;
	}

	/**
	 * This method returns a value specifying if the fragment was uncacheable because there was a previous
	 * failed attempt to serialize attributes.
	 * 
	 * @return a boolean indicating if this fragment is uncacheable
         * @ibm-private-in-use
	 */
	public boolean isUncacheable() {
		return uncacheable;
	}

	/**
	* This method sets the value specifying if the fragment was uncacheable because there was a previous
	* failed attempt to serialize attributes.
	*
	* @param  b the uncacheable variable
        * @ibm-private-in-use
	*/
	public void setUncacheable(boolean uncacheable) {
		this.uncacheable = uncacheable;
	}

	/**
	 * This method returns the value of the consume-subfragments property that determines if this fragment will
	 * consume its children fragments.
	 * 
	 * @return a boolean indicating if this fragment will consume its children fragments
         * @ibm-private-in-use
	 */
	public boolean getConsumeSubfragments() {
		return consumeSubfragments;
	}

	/**
	* This method sets the value of the consume-subfragments property that determines if this fragment will
	* consume its children fragments.
	*
	* @param consumeSubfragments the consume-subfragments property
        * @ibm-private-in-use
	*/
	public void setConsumeSubfragments(boolean consumeSubfragments) {
		this.consumeSubfragments = consumeSubfragments;
	}

	/**
	 * This method returns the value of the do-not-consume property that will be used to determine if this fragment
	 * will be consumed by its parents.
	 * 
	 * @return a boolean indicating if this fragment will be consumed by its parent fragments
         * @ibm-private-in-use
	 */
	public boolean getDoNotConsume() {
		return doNotConsume;
	}

	/**
	* This method sets the value of the do-not-consume property that will be used to determine if this fragment
	* will be consumed by its parents.
	*
	* @param doNotConsume the do-not-consume property
        * @ibm-private-in-use
	*/
	public void setDoNotConsume(boolean doNotConsume) {
		this.doNotConsume = doNotConsume;
		}

	/**
	 * This sets the name of the external cache group.
	 * 
	 * @param externalCacheGroupId The name of the external cache group
         * @ibm-private-in-use
	 */
	public void setExternalCacheGroupId(String externalCacheGroupId) {
		if (externalCacheGroupIdFlag == EntryInfo.SET) {
			throw new IllegalStateException("externalCacheGroupId was already set");
		}
		externalCacheGroupIdFlag = EntryInfo.SET;
		this.externalCacheGroupId = externalCacheGroupId;
	}

	/**
	 * This indicates whether the client set the external cache group id 
	 * in this FragmentInfo. 
	 * 
	 * @return True implies it was set.
         * @ibm-private-in-use
	 */
	public boolean wasExternalCacheGroupIdSet() {
		return (externalCacheGroupIdFlag == EntryInfo.SET);
	}

	/**
	 * This method returns the uri of this fragment
	 * 
	 * @return a String specifying the uri of this fragment
         * @ibm-private-in-use
	 */
	public String getURI() {
		return uri;
	}

	/**
	* This method sets the uri of this fragment
	*
	* @param  uri the uri for this fragment
        * @ibm-private-in-use
	*/
	public void setURI(String uri) {
		if (uriFlag == EntryInfo.SET) {
			throw new IllegalStateException("uri was already set");
		}
		this.uri = uri;
	}

	/**
	 * This indicates whether the client set the uri  
	 * in this FragmentInfo. 
	 * 
	 * @return True implies it was set.
         * @ibm-private-in-use
	 */
	public boolean wasURISet() {
		return (uriFlag == EntryInfo.SET);
	}

	/**
	 * This overrides the method in Object so this object can be cloned.
	 *
	 * @return The cloned object.
         * @ibm-private-in-use
	 */
	public Object clone() throws CloneNotSupportedException {
		//TODO incomplete...
		return super.clone();
	}

	/**
	* This method sets the String Array containing the request attributes to be excluded from caching
	*
	* @param  excludeList the list of attributes to exlude
        * @ibm-private-in-use
	*/
	public void setAttributeExcludeList(String[] excludeList){    //ST-begin
	  this.attributeExcludeList = excludeList;

	}

	/**
	 * This method returns an Array of attributes that will not be stored with this fragment.
	 * 
	 * @return an Array specifying the request attributes to be excluded from caching.
         * @ibm-private-in-use
	 */
	public String[] getAttributeExcludeList(){
	    return attributeExcludeList;
	}

	/**
	* This method sets the String Array containing the response cookies to be excluded from caching
	*
	* @param  excludeList the list of cookies to exlude
        * @ibm-private-in-use
	*/
	public void setCookieExcludeList(String[] excludeList){    //ST-begin
	  this.cookieExcludeList = excludeList;
	}
	
	/**
	 * This method returns an Array of cookies that will not be stored with this fragment.
	 * 
	 * @return an Array specifying the response cookies to be excluded from caching.
         * @ibm-private-in-use
	 */
	public String[] getCookieExcludeList(){
	    return cookieExcludeList;
	}
	
	/**
	* This method sets the String Array of fragments that will not be consumed by this fragment.
	*
	* @param  consumeExcludeList an Array specifying the fragments that will not be consumed by this fragment.
    * @ibm-private-in-use
	*/
	public void setConsumeExcludeList(String[] consumeExcludeList){    
        this.consumeExcludeList = consumeExcludeList;
	}

	/**
	 * This method returns an Array of fragments that will not be consumed by this fragment.
	 * 
	 * @return an Array specifying the fragments that will not be consumed by this fragment.
     * @ibm-private-in-use
	 */
	public String[] getConsumeExcludeList(){
        return consumeExcludeList;
	}
	
	/**
	* This method sets the name of the cache-instance that will be used to store this fragment.
	*
	* @param  instanceName the name of the cache-instance this fragment will be cached in.
        * @ibm-private-in-use
	*/
	public void setInstanceName(String instanceName) {  //MSI-begin
		 this.instanceName = instanceName;
	 }

	/**
	 * This method returns the name of the cache-instance that will be used to store this fragment.
	 * 
	 * @return a String specifying the cache-instance name
         * @ibm-private-in-use
	 */
	 public String getInstanceName() {
		 return instanceName;
	 }						    //MSI-end

	 /**
	  * This method sets whether the request type is appended to the cache-id for GET and POST requests.
	  *
	  * @param  b the ignoreGetPost variable
	  * @ibm-private-in-use
	  */
	  public void setIgnoreGetPost(boolean b) {
	  	  ignoreGetPost = b;
	  }

	  public void setIgnoreCharEnc(boolean b) {
	  	  ignoreCharEnc = b;
	  }
	  
	  
	  /**
	   * This method returns a for whether the request type is appended to the cache-id for GET and POST requests.
	   * 
	   * @return a boolean indicating if this request will append the request type to the cache-id
	   * @ibm-private-in-use
	   */
	   public boolean isIgnoreGetPost() {
		 return ignoreGetPost;
	   }
	   
	   
	   public boolean isIgnoreCharEncoding() {
		return ignoreCharEnc;
	   }
	   	   
	   /**
	    * This method sets the skip-cache-attribute.
	    *
	    * @param  skipCacheAttribute If this attribute is present caching will be skipped for this fragment.
	    * @ibm-private-in-use
	    */
	   public void setSkipCacheAttribute(String skipCacheAttribute) { 
		this.skipCacheAttribute = skipCacheAttribute;
	   }

	   /**
	    * This method returns the skip-cache-attribute.
	    * 
	    * @return a String specifying the skip-cache-attribute
	    * @ibm-private-in-use
	    */
	   public String getSkipCacheAttribute() {
		return skipCacheAttribute;
	   }	
	   
	   /**
	    * This method sets a boolean indicating whether the request should be marked do-not-cache
	    *
	    * @param  b the doNotCache variable
	    * @ibm-private-in-use
	    */
	    public void setDoNotCache(boolean b) {
	    	doNotCache = b;
	    }

	   /**
	    * This method returns a for whether the request should be marked do-not-cache
	    * 
	    * @return a boolean indicating whether the request should be marked do-not-cache
	    * @ibm-private-in-use
	    */
	    public boolean getDoNotCache() {
	    	return doNotCache;
	    }
	    
	    /**
		 * This method sets whether the current fragment has a cacheable consuming parent.
		 *
		 * @param  b the hasCacheableConsumingParent variable
		 * @ibm-private-in-use
		 */
		 public void setHasCacheableConsumingParent(boolean b) {
			 hasCacheableConsumingParent = b;
		 }

		/**
		 * This method returns whether the current fragment has a cacheable consuming parent.
		 * 
		 * @return a boolean indicating whether the current fragment has a cacheable consuming parent
		 * @ibm-private-in-use
		 */
		 public boolean getHasCacheableConsumingParent() {
			 return hasCacheableConsumingParent;
		 }

		public String getExternalCacheGroupId() {
			return externalCacheGroupId;
		}
}