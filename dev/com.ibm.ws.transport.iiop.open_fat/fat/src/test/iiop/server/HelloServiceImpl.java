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
package test.iiop.server;

import java.rmi.RemoteException;

import org.osgi.service.component.annotations.Component;

import test.iiop.common.HelloService;

@Component(immediate = true)
public class HelloServiceImpl extends AbstractRemoteService<HelloServiceImpl> implements HelloService {
    @Override
    public void sayHello() throws RemoteException {
        System.out.println("Hello, world! I am your humble servant.");
    }
}
