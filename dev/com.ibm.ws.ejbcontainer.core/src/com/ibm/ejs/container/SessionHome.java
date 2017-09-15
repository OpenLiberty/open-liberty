/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.rmi.RemoteException;

/**
 * A special EJSHome used for session beans with no local or remote home defined.
 */
public final class SessionHome extends EJSHome
{
    private static final long serialVersionUID = -5599958674558718699L;

    public SessionHome() throws RemoteException
    {
        super();
    }
}
