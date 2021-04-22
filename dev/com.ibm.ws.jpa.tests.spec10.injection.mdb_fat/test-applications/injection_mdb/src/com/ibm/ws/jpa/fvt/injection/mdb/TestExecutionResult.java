/**
 *
 */
package com.ibm.ws.jpa.fvt.injection.mdb;

import java.io.Serializable;

public class TestExecutionResult implements Serializable {
    private Throwable exception = null;

    public void setException(Throwable e) {
        this.exception = e;
    }

    public Throwable getException() {
        return exception;
    }
}
