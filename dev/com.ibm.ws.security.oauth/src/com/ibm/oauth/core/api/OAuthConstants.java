/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.api;

/**
 * This interface contains constants that may be useful to callers of the public
 * API's of the OAuth component.
 * 
 */
public interface OAuthConstants {
    /*
     * Attribute types used for normalizing OAuth-related HTTP requests into an
     * AttributeList and preparing responses.
     */
    public final static String ATTRTYPE_PARAM_OAUTH = "urn:ibm:names:oauth:param";
    public final static String ATTRTYPE_PARAM_QUERY = "urn:ibm:names:query:param";
    public final static String ATTRTYPE_PARAM_BODY = "urn:ibm:names:body:param";
    public final static String ATTRTYPE_REQUEST = "urn:ibm:names:oauth:request";
    public final static String ATTRTYPE_RESPONSE_ATTRIBUTE = "urn:ibm:names:oauth:response:attribute";
    public final static String ATTRTYPE_RESPONSE_DECISION = "urn:ibm:names:oauth:response:decision";
    public final static String ATTRTYPE_RESPONSE_META = "urn:ibm:names:oauth:response:metadata";
    public final static String ATTRTYPE_RESPONSE_STATE = "urn:ibm:names:oauth:state";

}
