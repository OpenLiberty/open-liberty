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
package com.ibm.ws.cdi12.test.aroundconstruct.interceptors;

import javax.annotation.Priority;
import javax.interceptor.Interceptor;

import com.ibm.ws.cdi12.test.utils.Intercepted;

@Interceptor
@Intercepted
@Priority(Interceptor.Priority.APPLICATION)
public class SubConstructInterceptor extends SuperConstructInterceptor {

}
