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
package com.ibm.ws.cdi12.test.priority.helpers;

import com.ibm.ws.cdi12.test.utils.ChainableList;


public interface Bean {

    ChainableList<String> getDecorators();

    ChainableList<String> getInterceptors();
}
