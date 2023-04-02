/*******************************************************************************
 * Copyright (c) 1998, 2005 IBM Corporation and others.
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
package com.ibm.ejs.container;

/**
 * Thrown when a duplicate primary key is found.
 */

public class ContainerDuplicateKeyException extends ContainerException
{
    private static final long serialVersionUID = 2461664244955831159L;

    public ContainerDuplicateKeyException(String s) {
        super(s);
    }

    public ContainerDuplicateKeyException() {
        super();
    }
} // ContainerDuplicateKeyException
