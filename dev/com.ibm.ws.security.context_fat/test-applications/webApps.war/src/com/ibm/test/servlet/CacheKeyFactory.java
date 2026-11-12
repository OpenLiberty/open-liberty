/*******************************************************************************
 * Copyright (c) 2016, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.test.servlet;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import javax.security.auth.Subject;
import com.ibm.wsspi.security.token.AttributeNameConstants;

public class CacheKeyFactory {

	public static String getCacheKeyFromSubject(Subject subject )  {

		String cacheKey = null;
		Set<Object> credentials = subject.getPublicCredentials();
		Hashtable<String, String> publicAttributes = getPublicAttributes( credentials );
		if (publicAttributes != null ) {
			cacheKey = publicAttributes.get(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
			if(cacheKey == null ) {
				System.out.println("Null Cache key from subject " + subject.toString());
			}
		} else
			System.out.println("Error getting public attributes from Subject");
		return cacheKey;

	}
	private static Hashtable<String, String> getPublicAttributes( Set<Object> credentials) {
		Hashtable<String, String> attributes = null;
		Iterator<Object> iCredentials = credentials.iterator();
		while( iCredentials.hasNext() ) {
			Object credential = (Object)iCredentials.next();
			if( credential instanceof java.util.Hashtable ) {
				attributes = (Hashtable<String, String>)credential;
				break;
			}
		}
		return attributes;
	}
}
