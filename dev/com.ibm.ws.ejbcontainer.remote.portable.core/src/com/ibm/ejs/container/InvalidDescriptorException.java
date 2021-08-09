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
 * Thrown when a malformed descriptor is detected by the container.<p>
 * 
 */

public class InvalidDescriptorException extends ContainerException
{
    private static final long serialVersionUID = 7008930165729679009L;

    public InvalidDescriptorException(String s) {
        super(s);
    }

    public InvalidDescriptorException() {
        super();
    }
} // InvalidDescriptorException
