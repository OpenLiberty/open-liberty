package com.ibm.ws.security.jwtsso.utils;

import java.util.Hashtable;

import javax.security.auth.Subject;

import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.wsspi.security.token.AttributeNameConstants;

public class SubjectUtil {

	private final Subject subject;
	private final SubjectHelper subjectHelper = new SubjectHelper();
	private static final String[] hashtableProperties = { AttributeNameConstants.WSCREDENTIAL_CACHE_KEY };

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

	protected Hashtable<String, ?> customPropertiesFromSubject(Subject subject) {
		// TODO Auto-generated method stub
		return subjectHelper.getHashtableFromSubject(subject, hashtableProperties);
	}

}
