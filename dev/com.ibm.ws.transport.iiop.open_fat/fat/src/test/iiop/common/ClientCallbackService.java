/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package test.iiop.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * To be passed into the server for the server to call back
 */
public interface ClientCallbackService extends Remote {
    void throwRuntimeException() throws RemoteException;
}
