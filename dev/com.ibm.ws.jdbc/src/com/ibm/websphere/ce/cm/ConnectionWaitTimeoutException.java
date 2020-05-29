/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ce.cm;

import java.sql.SQLTransientConnectionException;

/**
 * Used as a chained exception when unable to allocate a connection before the connection timeout is reached.
 * Would like to get rid of this and combine with top level exception, but could any application code be relying
 * on the chained exception?
 */
public class ConnectionWaitTimeoutException extends SQLTransientConnectionException {
    private static final long serialVersionUID = 5958695928250441720L;

    /**
     * Constructs a <code>ConnectionWaitTimeoutException</code> object.
     * The <code>reason</code>, <code>SQLState</code> are initialized
     * to <code>null</code> and the vendor code is initialized to 0.
     * <p>
     */
    public ConnectionWaitTimeoutException() {
        super();
    }

    /**
     * Constructs a <code>ConnectionWaitTimeoutException</code> object
     * with a given <code>reason</code>. The <code>SQLState</code>
     * is initialized to <code>null</code> and the vendor code is initialized
     * to 0.
     * <p>
     * @param reason a description of the exception
     */
    public ConnectionWaitTimeoutException(String reason) {
        super(reason);
    }

    /**
     * Constructs a <code>ConnectionWaitTimeoutException</code> object
     * with a given <code>reason</code> and <code>SQLState</code>.
     * 
     * The vendor code is initialized to 0.
     * <p>
     * @param reason a description of the exception
     * @param SQLState an XOPEN or SQL:2003 code identifying the exception
     */
    public ConnectionWaitTimeoutException(String reason, String SQLState) {
        super(reason,SQLState);
    }

    /**
     * Constructs a <code>ConnectionWaitTimeoutException</code> object
     * with a given <code>reason</code>, <code>SQLState</code>  and
     * <code>vendorCode</code>.
     * <p>
     * @param reason a description of the exception
     * @param SQLState an XOPEN or SQL:2003 code identifying the exception
     * @param vendorCode a database vendor specific exception code
     */
    public ConnectionWaitTimeoutException(String reason, String SQLState, int vendorCode) {
        super(reason,SQLState,vendorCode);
    }

    /**
     * Constructs a <code>ConnectionWaitTimeoutException</code> object
     * with a given  <code>cause</code>.
     * The <code>SQLState</code> is initialized
     * to <code>null</code> and the vendor code is initialized to 0.
     * The <code>reason</code>  is initialized to <code>null</code> if
     * <code>cause==null</code> or to <code>cause.toString()</code> if
     * <code>cause!=null</code>.
     * <p>
     * @param cause the underlying reason for this <code>SQLException</code> (which is saved for later retrieval by the <code>getCause()</code> method); may be null indicating
     *     the cause is non-existent or unknown.
     */
    public ConnectionWaitTimeoutException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a <code>ConnectionWaitTimeoutException</code> object
     * with a given
     * <code>reason</code> and  <code>cause</code>.
     * The <code>SQLState</code> is  initialized to <code>null</code>
     * and the vendor code is initialized to 0.
     * <p>
     * @param reason a description of the exception.
     * @param cause the underlying reason for this <code>SQLException</code>(which is saved for later retrieval by the <code>getCause()</code> method); may be null indicating
     *     the cause is non-existent or unknown.
     */
    public ConnectionWaitTimeoutException(String reason, Throwable cause) {
        super(reason,cause);
    }

    /**
     * Constructs a <code>ConnectionWaitTimeoutException</code> object
     * with a given
     * <code>reason</code>, <code>SQLState</code> and  <code>cause</code>.
     * The vendor code is initialized to 0.
     * <p>
     * @param reason a description of the exception.
     * @param SQLState an XOPEN or SQL:2003 code identifying the exception
     * @param cause the underlying reason for this <code>SQLException</code> (which is saved for later retrieval by the <code>getCause()</code> method); may be null indicating
     *     the cause is non-existent or unknown.
     */
    public ConnectionWaitTimeoutException(String reason, String SQLState, Throwable cause) {
        super(reason,SQLState,cause);
    }

    /**
     *  Constructs a <code>ConnectionWaitTimeoutException</code> object
     * with a given
     * <code>reason</code>, <code>SQLState</code>, <code>vendorCode</code>
     * and  <code>cause</code>.
     * <p>
     * @param reason a description of the exception
     * @param SQLState an XOPEN or SQL:2003 code identifying the exception
     * @param vendorCode a database vendor-specific exception code
     * @param cause the underlying reason for this <code>SQLException</code> (which is saved for later retrieval by the <code>getCause()</code> method); may be null indicating
     *     the cause is non-existent or unknown.
     */
    public ConnectionWaitTimeoutException(String reason, String SQLState, int vendorCode, Throwable cause) {
        super(reason,SQLState,vendorCode,cause);
    }
}