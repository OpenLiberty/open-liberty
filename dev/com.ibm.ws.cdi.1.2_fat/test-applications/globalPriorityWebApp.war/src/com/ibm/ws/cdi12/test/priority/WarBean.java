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

import static com.ibm.ws.cdi12.test.priority.helpers.RelativePriority.LOW_PRIORITY;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;

import com.ibm.ws.cdi12.test.priority.helpers.AbstractBean;

@Alternative
@Priority(LOW_PRIORITY)
@FromWar
public class WarBean extends AbstractBean {}
