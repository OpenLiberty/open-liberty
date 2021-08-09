/*******************************************************************************
 * Copyright (c) 2000 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.csi;

/**
 * An alternative way to mark an object as transactional,
 * so that JTS will establish a transactional context when
 * the object is invoked through an ORB.
 * 
 * No methods or other interfaces are required.
 */

public interface TransactionalObject extends java.rmi.Remote {}
