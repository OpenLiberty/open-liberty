/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.test.implicitWar;

import javax.enterprise.context.RequestScoped;

import com.ibm.ws.cdi12.test.utils.SimpleAbstract;

/**
 * This bean has a bean-defining annotation, so this container should be considered to be a bean archive.
 */
@RequestScoped
public class AnnotatedBean extends SimpleAbstract {

}
