/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.test.priority;

import javax.interceptor.Interceptor;

import com.ibm.ws.cdi12.test.priority.helpers.AbstractInterceptor;
import com.ibm.ws.cdi12.test.utils.Intercepted;

/**
 * Enabled for this bean archive in beans.xml.
 */
@Interceptor
@Intercepted
public class LocalJarInterceptor extends AbstractInterceptor {

    public LocalJarInterceptor() {
        super(LocalJarInterceptor.class);
    }
}
