/*******************************************************************************
 * Copyright (c) 2007, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.bindings.bnd.ejb;

/**
 * Local interface for Enterprise Bean: TargetOneBean
 */
public interface LocalTargetOne extends javax.ejb.EJBLocalObject {
    public String echo(String message);
}
