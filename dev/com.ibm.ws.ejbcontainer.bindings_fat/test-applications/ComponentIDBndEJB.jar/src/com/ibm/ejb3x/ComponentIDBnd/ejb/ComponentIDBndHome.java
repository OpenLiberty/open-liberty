/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb3x.ComponentIDBnd.ejb;

/**
 * Home interface for Enterprise Bean: ComponentIDBnd
 */
public interface ComponentIDBndHome extends javax.ejb.EJBLocalHome {
    /**
     * Creates a default instance of Session Bean: ComponentIDBnd
     */
    public ComponentIDBnd create() throws javax.ejb.CreateException;
}
