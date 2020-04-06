/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.web;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import com.ibm.ws.cache.ValueSet;
import com.ibm.ws.cache.intf.ExternalInvalidation;

/**
 * This class is a struct object that contains additional caching metadata
 * for page fragments that are cached in external caches.
 */
public class ExternalCacheFragment implements ExternalInvalidation, Serializable {
    private static final long serialVersionUID = 1342185474L;
	//As defined in RFC 1123
	private static SimpleDateFormat httpDateFormat = null;
	static {
		httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

		TimeZone timeZone = TimeZone.getTimeZone("GMT");
		httpDateFormat.setTimeZone(timeZone);
	}

	private static final String EXPIRES = "Expires";

	/**
	 * The external URI for this page.
	 */
	private String uri = null;

	/**
	 * The host header from the request
	 */
	private String host = null;
	private ArrayList vHostList = new ArrayList();//NK5


	/**
	 * This is the set of external caches that rendered pages are
	 * written to.
	 * <p>Restrictions on fragments used for this are the following:
	 * <ul>
	 *     <li>It must be a top-level fragment (ie, an externally
	 *         requested page).
	 *     <li>It must not have any security access restrictions.
	 *         This restriction can be relaxed if the external cache
	 *         supports some form of access control.
	 * </ul>
	 */
	private String externalCacheGroupId = null;

	/**
	 * The fully-expanded character array version of the page.
	 * This is obtained via a transitive closure of contained fragments.
	 */
	private byte[] content = null;

	/**
	 * This contains all invalidation ids, including both cache ids
	 * and data ids, that this page depends on.
	 * This is obtained a transitive closure of contained fragments.
	 * It is used for invalidating the page by ids.
	 */
	private ValueSet invalidationIds = new ValueSet(4);

	/**
	 * This contains all Templates that this page depends on.
	 * This is obtained a transitive closure of contained fragments.
	 * It is used for invalidating the page by Templates.
	 */
	private ValueSet uris = new ValueSet(4);

	/**
	 * The hashtable of headers that have to be cached.
	 */
	private Vector[] headerTable = null;

	/**
	 * The absolute time when the external cache fragment should
	 * be invalidated.
	 */
	private long expirationTime = -1;

	/**
	 * This maximum time interval that the external cache fragment should
	 * be allowed to stay in cache.
	 */
	private long timeStamp = -1;

	/**
	 * The entry's externalCacheEntry
	 */
	private transient ExternalCacheEntry externalCacheEntry = null;

	/**
	 * This returns the externalCacheGroupId variable.
	 *
	 * @return The externalCacheGroupId variable.
	 */
	public String getExternalCacheGroupId() {
		return externalCacheGroupId;
	}

	/**
	 * This sets the externalCacheGroupId variable.
	 *
	 * @param url The new externalCacheGroupId variable.
	 */
	public void setExternalCacheGroupId(String externalCacheGroupId) {
		this.externalCacheGroupId = externalCacheGroupId;
	}

	/**
	 * This returns the uri variable.
	 *
	 * @return The uri variable.
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * This sets the uri variable.
	 *
	 * @param uri The new uri variable.
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * This returns the host variable.
	 *
	 * @return The host variable.
	 */
	public String getHost() {
		return host;
	}

	/**
	 * This sets the uri variable.
	 *
	 * @param uri The new uri variable.
	 */
	public void setHost(String host) {
		this.host = host;
	}


	/**
	*  This adds host to list of                   
	* hosts that requested this ECF
	*/
	public void addHostToList(String host) {	 //NK begin
	   if(!vHostList.contains(host))
		vHostList.add(host);
	}

	/**
	* This returns the list of 
	hosts that requested this ECF
	*/
	public ArrayList getHostList() {
	    return vHostList;
	  }					       //NK end

	/**
	 * This returns the content variable.
	 *
	 * @return The content variable.
	 */
	public byte[] getContent() {
		return content;
	}

	/**
	 * This sets the content variable.
	 *
	 * @param content The new content variable.
	 */
	public void setContent(byte[] content) {
		this.content = content;
	}

	/**
	 * This returns the invalidationIds variable.
	 *
	 * @return The invalidationIds variable.
	 */
	public Enumeration getInvalidationIds() {
		return invalidationIds.elements();
	}

	/**
	 * This sets the invalidationIds variable.
	 *
	 * @param invalidationIds The new invalidationIds variable.
	 */
	public void setInvalidationIds(ValueSet invalidationIds) {
		this.invalidationIds = invalidationIds;
	}

	/**
	 * This returns the uris variable.
	 *
	 * @return The uris variable.
	 */
	public Enumeration getTemplates() {
		return uris.elements();
	}

	/**
	 * This sets the uris variable.
	 *
	 * @param uris The new uris variable.
	 */
	public void setTemplates(ValueSet uris) {
		this.uris = uris;
	}

	public Vector[] getHeaderTable() {
		return headerTable;
	}

	public void setHeaderTable(Vector[] headerTable) {
		this.headerTable = headerTable;
	}

	public long getExpirationTime() {
		return expirationTime;
	}

	public void setExpirationTime(long expirationTime) {
		this.expirationTime = expirationTime;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public ExternalCacheEntry getEntry() {
		if (externalCacheEntry != null) {
			return externalCacheEntry;
		}
		externalCacheEntry = new ExternalCacheEntry();
		externalCacheEntry.uri = uri;
		externalCacheEntry.host = host; //@bkma
		externalCacheEntry.content = content;
		externalCacheEntry.headerTable = headerTable;
		synchronized(httpDateFormat) { // Protect the date formatting
			if (expirationTime >= 0) {
				if (headerTable[0].contains(EXPIRES)) {
					headerTable[1].set(headerTable[0].indexOf(EXPIRES), httpDateFormat.format(new Date(expirationTime)));
				} else {
					headerTable[0].add(EXPIRES);
					headerTable[1].add(httpDateFormat.format(new Date(expirationTime)));
				}
			}
		}
		return externalCacheEntry;
	}

	/**
	 * This overrides the method in Object.
	 * It uses the url's equals.
	 *
	 * @return True implies they are equal.
	 */
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (!(object instanceof ExternalCacheFragment)) {
			return false;
		}
		ExternalCacheFragment externalCacheFragment = (ExternalCacheFragment) object;

		return uri.equals(externalCacheFragment.uri);
	}

	/**
	 * This overrides the method in Object.
	 * It uses the url's hashCode.
	 *
	 * @return The hashCode.
	 */
	public int hashCode() {
		if (uri == null) {
			return ExternalCacheFragment.class.hashCode();
		}
		return uri.hashCode();
	}
}