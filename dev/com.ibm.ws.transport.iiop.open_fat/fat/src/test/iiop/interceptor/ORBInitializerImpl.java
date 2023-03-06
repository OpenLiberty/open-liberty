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
package test.iiop.interceptor;

import org.omg.CORBA.INITIALIZE;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.SystemException;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitializer;
import org.omg.PortableInterceptor.ORBInitInfoPackage.DuplicateName;

public class ORBInitializerImpl extends LocalObject implements ORBInitializer {
    private static final long serialVersionUID = 1L;

    @Override
    public void pre_init(ORBInitInfo info) {}

    @Override
    public void post_init(ORBInitInfo info) {
        try {
            info.add_server_request_interceptor(new ServerRequestInterceptorImpl());
        } catch (DuplicateName e) {
            throw (SystemException) (new INITIALIZE().initCause(e));
        }
    }
}
