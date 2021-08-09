/*******************************************************************************
 * Copyright (c) 1998, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

/**
 * Thrown when an object is not found. <p>
 */

public class ContainerObjectNotFoundException extends ContainerException
{
    private static final long serialVersionUID = 4916404802059115748L;

    public ContainerObjectNotFoundException(String s) {
        super(s);
    }

    public ContainerObjectNotFoundException() {
        super();
    }
} // ContainerObjectNotFoundException
