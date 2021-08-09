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

// Note:  This class is not used to generate any Javadoc for ConnectionWaitTimeoutException, 
// so it has all been changed to non-Javadoc formatted comments.
// To change the Javadoc for ConnectionWaitTimeoutException, please change the other copy.
// If code changes or comment changes are made here, please update the other copy as well.


// Used as a chained exception when unable to allocate a connection before the connection timeout is reached.
public class ConnectionWaitTimeoutException extends SQLTransientConnectionException {
    private static final long serialVersionUID = 5958695928250441720L;

    // Constructs a ConnectionWaitTimeoutException object.
    // The reason and SQLState are initialized to null and the vendor code is initialized to 0.
    public ConnectionWaitTimeoutException() {
        super();
    }

    // Constructs a ConnectionWaitTimeoutException object with a given reason. 
    // The SQLState is initialized to null and the vendor code is initialized to 0.
    //
    // parameter reason is a description of the exception.
    public ConnectionWaitTimeoutException(String reason) {
        super(reason);
    }

    // Constructs a ConnectionWaitTimeoutException object with a given reason and SQLState.
    // The vendor code is initialized to 0.
    //
    // parameter reason is a description of the exception.
    // parameter SQLState is an XOPEN or SQL:2003 code identifying the exception.
    public ConnectionWaitTimeoutException(String reason, String SQLState) {
        super(reason,SQLState);
    }

    // Constructs a ConnectionWaitTimeoutException object with a given reason, SQLState and vendorCode.
    //
    // parameter reason is a description of the exception.
    // parameter SQLState is an XOPEN or SQL:2003 code identifying the exception.
    // parameter vendorCode is a database vendor specific exception code.
    public ConnectionWaitTimeoutException(String reason, String SQLState, int vendorCode) {
        super(reason,SQLState,vendorCode);
    }

    // Constructs a ConnectionWaitTimeoutException object with a given cause.
    // The SQLState is initialized to null and the vendor code is initialized to 0.
    // The reason is initialized to null if cause==null or to cause.toString() if cause!=null.
    //
    // parameter cause is the underlying reason for this SQLException (which is saved for later retrieval by the getCause() method);
    //           may be null indicating the cause is non-existent or unknown.
    public ConnectionWaitTimeoutException(Throwable cause) {
        super(cause);
    }

    // Constructs a ConnectionWaitTimeoutException object with a given reason and cause.
    // The SQLState is initialized to null and the vendor code is initialized to 0.
    //
    // parameter reason is a description of the exception.
    // parameter cause is the underlying reason for this SQLException (which is saved for later retrieval by the getCause() method); 
    //           may be null indicating the cause is non-existent or unknown.
    public ConnectionWaitTimeoutException(String reason, Throwable cause) {
        super(reason,cause);
    }

    // Constructs a ConnectionWaitTimeoutException object with a given reason, SQLState and cause.
    // The vendor code is initialized to 0.
    //
    // parameter reason is a description of the exception.
    // parameter SQLState is an XOPEN or SQL:2003 code identifying the exception.
    // parameter cause is the underlying reason for this SQLException (which is saved for later retrieval by the getCause() method); 
    //           may be null indicating the cause is non-existent or unknown.
    public ConnectionWaitTimeoutException(String reason, String SQLState, Throwable cause) {
        super(reason,SQLState,cause);
    }

    // Constructs a ConnectionWaitTimeoutException object with a given reason, SQLState, vendorCode and cause.
    //
    // parameter reason is a description of the exception.
    // parameter SQLState is an XOPEN or SQL:2003 code identifying the exception.
    // parameter vendorCode is a database vendor-specific exception code.
    // parameter cause is the underlying reason for this SQLException (which is saved for later retrieval by the getCause() method); 
    //           may be null indicating the cause is non-existent or unknown.
    public ConnectionWaitTimeoutException(String reason, String SQLState, int vendorCode, Throwable cause) {
        super(reason,SQLState,vendorCode,cause);
    }
}