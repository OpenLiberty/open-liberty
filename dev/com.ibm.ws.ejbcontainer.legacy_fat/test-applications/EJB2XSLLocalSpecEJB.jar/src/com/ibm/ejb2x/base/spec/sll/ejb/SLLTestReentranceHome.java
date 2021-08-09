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

package com.ibm.ejb2x.base.spec.sll.ejb;

/**
 * Home interface for Enterprise Bean: SLLTestReentranceHome
 */
public interface SLLTestReentranceHome extends javax.ejb.EJBLocalHome {
    /**
     * ejbCreate with pKey
     */
    public SLLTestReentrance create() throws javax.ejb.CreateException;
}
