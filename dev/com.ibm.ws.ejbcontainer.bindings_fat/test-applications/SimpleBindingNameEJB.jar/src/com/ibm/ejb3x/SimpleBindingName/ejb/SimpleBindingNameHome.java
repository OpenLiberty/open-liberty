/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

package com.ibm.ejb3x.SimpleBindingName.ejb;

/**
 * Home interface for Enterprise Bean: SimpleBindingName
 */
public interface SimpleBindingNameHome extends javax.ejb.EJBLocalHome {
    /**
     * Creates a default instance of Session Bean: SimpleBindingName
     */
    public SimpleBindingName create() throws javax.ejb.CreateException;
}
