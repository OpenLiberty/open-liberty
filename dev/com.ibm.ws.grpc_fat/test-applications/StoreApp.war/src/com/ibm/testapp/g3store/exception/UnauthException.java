/**
 *
 */
package com.ibm.testapp.g3store.exception;

/**
 *
 */
public class UnauthException extends Exception {

    /**  */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for this exception
     *
     * @param msg
     */
    public UnauthException(String msg) {
        super(msg);
    }

    public UnauthException() {
        super();
    }

    public UnauthException(Throwable t) {
        super(t);
    }

    public UnauthException(String msg, Throwable t) {
        super(msg, t);
    }

}
