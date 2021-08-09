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
package com.ibm.websphere.csi;

import java.io.PrintWriter;

/**
 * A <code>IllegalOperation</code> exception is thrown by a
 * CSI plugin instance whenever an illegal operation is attempted.<p>
 */

public class IllegalOperationException
                extends CSIRuntimeException
{
    private static final long serialVersionUID = 3730695908019323766L;

    /**
     * Create a new IllegalOperationException with the associated string
     * description. <p.
     * 
     * @param s the <code>String</code> describing the exception <p>
     */
    public IllegalOperationException(String s) {

        super(s);

    } // IllegalOperationException

    /**
     * Print backtrace for this exception and any nested exception as well.
     */
    public void printStackTrace(PrintWriter s) {

        super.printStackTrace(s);

    } // printStackTrace

} // IllegalOperationException
