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

public class ContainerRemoveException extends ContainerException
{
    private static final long serialVersionUID = 5955342514152542284L;

    public ContainerRemoveException(String s) {
        super(s);
    }

    public ContainerRemoveException(String s, java.lang.Throwable ex) {
        super(s, ex);
    }

    public ContainerRemoveException(java.lang.Throwable ex) {
        super(ex); //150727
    }
} // ContainerRemoveException
