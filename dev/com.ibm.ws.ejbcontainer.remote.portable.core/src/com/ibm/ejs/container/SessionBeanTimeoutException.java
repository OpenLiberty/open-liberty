/*******************************************************************************
 * Copyright (c) 1998, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.rmi.NoSuchObjectException;

/**
 * This exception is thrown whenever session bean timesout
 * 
 */

public class SessionBeanTimeoutException
                extends NoSuchObjectException
{
    private static final long serialVersionUID = 2591380441572291126L;

    /**
     * Create a new <code>SessionBeanTimeoutException</code>
     * instance. <p>
     */

    public SessionBeanTimeoutException(String s) {
        super(s);
    } // SessionBeanTimeoutException

} // SessionBeanTimeoutException
