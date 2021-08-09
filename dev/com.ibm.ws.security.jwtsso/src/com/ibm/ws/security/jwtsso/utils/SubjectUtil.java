/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwtsso.utils;

import java.util.Hashtable;

import javax.security.auth.Subject;

import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.wsspi.security.token.AttributeNameConstants;

public class SubjectUtil {

	private final Subject subject;
	private final SubjectHelper subjectHelper = new SubjectHelper();
	private static final String[] hashtableProperties = { AttributeNameConstants.WSCREDENTIAL_CACHE_KEY,
			AuthenticationConstants.INTERNAL_AUTH_PROVIDER };

	public SubjectUtil(Subject sub) {
		subject = sub;
	}

	public String getCustomCacheKey() {
		Hashtable customProperties = customPropertiesFromSubject(subject);
		if (customProperties != null && !customProperties.isEmpty()) {
			return (String) customProperties.get(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
		}
		return null;
	}

	public String getCustomAuthProvider() {
		Hashtable customProperties = customPropertiesFromSubject(subject);
		if (customProperties != null && !customProperties.isEmpty()) {
			return (String) customProperties.get(AuthenticationConstants.INTERNAL_AUTH_PROVIDER);
		}
		return null;
	}

	protected Hashtable<String, ?> customPropertiesFromSubject(Subject subject) {
		// TODO Auto-generated method stub
		return subjectHelper.getHashtableFromSubject(subject, hashtableProperties);
	}

}
