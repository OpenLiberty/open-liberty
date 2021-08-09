/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.uow;

/** 
 * <p>
 * This class provides a generic indication of failure of the completion of a
 * unit of work (UOW) begun by a call to the runUnderUOW method on the UOWManager
 * interface. The UOW-specific cause of the failure can be obtained via the
 * getCause() method.
 * </p>
 * @see UOWManager#runUnderUOW(int, boolean, UOWAction)
 * @ibm-spi
 */
public class UOWException extends Exception
{
    private static final long serialVersionUID = 48189790854141828L;

    /**
     * <p>
     * Creates a new UOWException with the given UOW-specific exception
     * as the cause.
     * </p>
     * @param cause The UOW-specific cause
     */
    public UOWException(Throwable cause)
    {
        super(cause);
    }

    /**
     * <p>
     * Returns the UOW-specific exception that describes the nature of
     * the completion failure.
     * </p>
     * 
     * @return The UOW-specific exception associated with the failure.
     */
    public Throwable getCause()
    {
        return super.getCause();
    }
}
