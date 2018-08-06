/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corporation 2011, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.wsspi.anno.targets;

public interface AnnotationTargets_Fault {
    String getUnresolvedText();

    // Alternate to obtaining the parameters,
    // use these to avoid copying the parameters array.
    int getParameterCount();

    Object getParamater(int paramNo);

    // A distinct array which may be safely modified
    // without disturbing the receiver.
    Object[] getParameters();

    //

    String getResolvedText();
}
