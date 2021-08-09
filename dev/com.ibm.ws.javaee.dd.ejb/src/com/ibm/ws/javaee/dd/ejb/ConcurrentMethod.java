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
package com.ibm.ws.javaee.dd.ejb;

/**
 * Represents &lt;concurrent-method>.
 */
public interface ConcurrentMethod
{
    /**
     * Represents an unspecified value for {@link #getConcurrentLockTypeValue}.
     */
    int LOCK_TYPE_UNSPECIFIED = -1;

    /**
     * Represents "Read" for {@link #getConcurrentLockTypeValue}.
     * 
     * @see org.eclipse.jst.j2ee.ejb.ConcurrentLockType#READ
     */
    int LOCK_TYPE_READ = 0;

    /**
     * Represents "Write" for {@link #getConcurrentLockTypeValue}.
     * 
     * @see org.eclipse.jst.j2ee.ejb.ConcurrentLockType#WRITE
     */
    int LOCK_TYPE_WRITE = 1;

    /**
     * @return &lt;method>
     */
    NamedMethod getMethod();

    /**
     * @return &lt;lock>
     *         <ul>
     *         <li>{@link #LOCK_TYPE_UNSPECIFIED} if unspecified
     *         <li>{@link #LOCK_TYPE_READ} - Read
     *         <li>{@link #LOCK_TYPE_WRITE} - Write
     *         </ul>
     */
    int getLockTypeValue();

    /**
     * @return &lt;access-timeout>, or null if unspecified
     */
    AccessTimeout getAccessTimeout();
}
