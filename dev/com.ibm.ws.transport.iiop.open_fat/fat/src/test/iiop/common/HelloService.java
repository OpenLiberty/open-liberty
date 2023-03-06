/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package test.iiop.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface HelloService extends Remote {
    void sayHello() throws RemoteException;
}
