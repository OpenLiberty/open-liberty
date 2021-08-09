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
 * This exception is thrown to indicate an attempt has been made to use
 * a disabled home.
 */

public class HomeDisabledException
                extends RuntimeException
{
    private static final long serialVersionUID = 8801205964764897441L;

    /**
     * Create a new <code>HomeDisabledException</code> instance. <p>
     */

    public HomeDisabledException(String s) {
        super(s);
    } // HomeDisabledException

} // HomeDisabledException
