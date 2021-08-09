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
package com.ibm.wsspi.cache;

/**
 * This abstract class is used to indicate the features that are supported
 * by a cache provider. Functionality such as servlet caching, JSP,
 * webservices, command cache, DistributedMap and DistributedNioMap
 * checks with this class before invoking a particular function.
 *
 * <p>
 * Methods need to be extended by CacheProviders for each of the features listed below.
 * All cache providers other than the default (Dynacache) will
 * return <code>false</code> for ALL the methods in this abstract class. In subsequent releases
 * of WebSphere, CacheProviders may be allowed to support the features listed below.
 *
 * @ibm-spi
 * @since WAS 6.1.0.27
 */
public abstract class CacheFeatureSupport {

	/**
	 * Indicates if the cache alias ID is supported.
	 *
	 * @return true - the cache alias feature is supported.
	 */
	public abstract boolean isAliasSupported();

	/**
	 * Indicates if WebSphere Data Replication Services (DRS) style cache replication is supported.
	 *
	 * @return true - the cache replication feature is support.
	 */
	public abstract boolean isReplicationSupported();

	/**
	 * Indicates if WebSphere disk cache feature is supported.
	 *
	 * @return true - the disk cache feature is support.
	 */
	public abstract boolean isDiskCacheSupported();

}
