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
package com.ibm.ejs.persistence;

public class EJSPrimaryKeyChangeException extends EJSPersistenceException
{
    private static final long serialVersionUID = 1560537528374641555L;

    public EJSPrimaryKeyChangeException() {} // EJSPrimaryKeyChangeException

    public EJSPrimaryKeyChangeException(String s) {
        super(s);
    } // EJSPrimaryKeyChangeException

    public EJSPrimaryKeyChangeException(String s, Throwable ex) {
        super(s, ex);
    }

} // EJSPrimaryKeyChangeException
