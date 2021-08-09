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
 * This exception is thrown if an attempt is made to install an EJB
 * into a container with the same JNDI name for its home as an
 * EJS that is already installed in the container. <p>
 * 
 * If this exception is thrown the EJB installation fails and the
 * already installed EJB remains usable. <p>
 */

public class DuplicateHomeNameException
                extends ContainerException
{
    private static final long serialVersionUID = 4878512837435795367L;

    public DuplicateHomeNameException(String s) {
        super(s);
    }
} // DuplicateHomeNameException
