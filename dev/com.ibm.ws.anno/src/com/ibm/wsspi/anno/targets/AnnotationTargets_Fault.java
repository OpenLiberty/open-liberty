/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
