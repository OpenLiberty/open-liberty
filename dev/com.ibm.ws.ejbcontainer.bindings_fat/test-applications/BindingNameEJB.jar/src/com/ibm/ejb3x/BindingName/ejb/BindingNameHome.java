/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb3x.BindingName.ejb;

/**
 * Home interface for Enterprise Bean: BindingName
 */
public interface BindingNameHome extends javax.ejb.EJBLocalHome {
    /**
     * Creates a default instance of Session Bean: BindingName
     */
    public BindingName create() throws javax.ejb.CreateException;
}
