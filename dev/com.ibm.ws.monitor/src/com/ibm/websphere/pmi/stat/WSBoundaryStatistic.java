/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.pmi.stat;

/**
 * WebSphere boundary statistic interface.
 * 
 * @ibm-api
 */

public interface WSBoundaryStatistic extends WSStatistic {
    /** Returns the lower limit of the value of this attribute. */
    public long getLowerBound();

    /** Returns the upper limit of the value of this attribute. */
    public long getUpperBound();
}
