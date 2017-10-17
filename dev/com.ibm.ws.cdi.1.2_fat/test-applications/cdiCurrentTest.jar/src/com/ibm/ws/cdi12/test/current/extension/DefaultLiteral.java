/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.test.current.extension;

import javax.enterprise.inject.Default;
import javax.enterprise.util.AnnotationLiteral;

@SuppressWarnings("all")
public class DefaultLiteral extends AnnotationLiteral<Default> implements Default {

    public static final Default INSTANCE = new DefaultLiteral();

    private DefaultLiteral() {}

}
