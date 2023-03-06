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
package test.iiop.client;

import java.rmi.RemoteException;

import test.iiop.common.ClientCallbackService;
import test.iiop.notcommon.VersionedException;

public class ClientCallbackServiceImpl implements ClientCallbackService {
    @Override
    public void throwRuntimeException() throws RemoteException {
        throw new VersionedException();
    }
}
