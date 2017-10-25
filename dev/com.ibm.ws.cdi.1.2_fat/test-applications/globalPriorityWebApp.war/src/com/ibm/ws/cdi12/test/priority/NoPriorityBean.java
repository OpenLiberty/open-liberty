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

import javax.enterprise.context.RequestScoped;

import com.ibm.ws.cdi12.test.priority.helpers.AbstractBean;

/**
 * Globally enabled {@code @Alternative} beans should take priority over this.
 */
@RequestScoped
public class NoPriorityBean extends AbstractBean {}
