/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.msgstore.gbs;

/**
 * An exception that is thrown during a GBSTree operation when a
 * Kprogramming error is detected.
 *
 * @author Stewart L. Palmer
 */

public class GBSTreeException extends RuntimeException
{
    private static final long serialVersionUID = -8662457232975171206L;

    /**
     * Construct
     */
    GBSTreeException()
    {
        super();
    }

    /**
     * Construct with an error message
     */
    GBSTreeException(
                    String      msg)
    {
        super(msg);
    }

    /**
     * Construct with a String and a causing Exception
     */
    GBSTreeException(
                    String      msg,
                    Throwable   cause)
    {
        super(msg, cause);
    }
}
