/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb2x.base.spec.sfl.ejb;

/**
 * Local interface for Enterprise Bean: SFLTestReentrance
 */
public interface SFLTestReentrance extends javax.ejb.EJBLocalObject {
    /**
     * Call self recursively n times
     *
     * @return number of recursive call
     */
    public int callRecursiveSelf(int level, SFLTestReentrance ejb1) throws SFLApplException;

    /**
     * Call self recursively to cause an exception
     */
    public int callNonRecursiveSelf(int level, SFLTestReentrance ejb1) throws SFLApplException;
}
