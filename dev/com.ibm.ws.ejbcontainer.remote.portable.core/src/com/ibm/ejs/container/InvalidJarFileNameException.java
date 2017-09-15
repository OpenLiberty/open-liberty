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
 * InvalidJarFileNameException
 * This exception is thrown by the container when an attempt is
 * made to install an EJB by specifying its attributes in the
 * form of a Properties object.
 * It indicates that the name of the jar file that contains the
 * EJB code is incorrect or could not be accessed.
 **/
public class InvalidJarFileNameException extends ContainerException
{
    private static final long serialVersionUID = -8291283892884860366L;

    public InvalidJarFileNameException(String s) {
        super(s);
    }
}
