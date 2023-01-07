/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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

package com.ibm.ejb2x.jndiName.ejb;

/**
 * Remote interface for Enterprise Bean: JNDIName.
 */
public interface JNDIRemoteName extends javax.ejb.EJBObject {

    /**
    */
    public String foo() throws java.rmi.RemoteException;

}
