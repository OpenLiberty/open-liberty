/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.uow.embeddable;

public class UOWCompensatedException extends RuntimeException
{
    private static final long serialVersionUID = -1868184181459590196L;;

    public UOWCompensatedException(String message)
    {
        super(message);
    }

    public UOWCompensatedException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public UOWCompensatedException(Throwable cause)
    {
        super(cause);
    }


}
