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

public class ContainerException extends java.rmi.RemoteException
{
    private static final long serialVersionUID = -3000641845739978815L;

    public ContainerException(String s) {
        super(s);
    }

    public ContainerException(String s, java.lang.Throwable ex) {
        super(s, ex);
    }

    public ContainerException(java.lang.Throwable ex) {
        super("", ex); //150727
    }

    public ContainerException() {
        super();
    }

}
