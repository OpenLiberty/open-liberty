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

import org.omg.CORBA.LocalObject;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

/**
 *
 */
public class ServerRequestInterceptorImpl extends LocalObject implements ServerRequestInterceptor {
    private static final long serialVersionUID = 1L;

    @Override
    public void receive_request_service_contexts(ServerRequestInfo ri) throws ForwardRequest {}

    @Override
    public void receive_request(ServerRequestInfo ri) throws ForwardRequest {
        System.out.println("### in receive_request()");
        System.out.println("###    operation: '" + ri.operation() + "'");
        if (ri.operation().equals("sayHello")) {
            System.out.println("###    raising NO_PERMISSION");
            throw new NO_PERMISSION("Can't touch this.");
        }
    }

    @Override
    public void send_reply(ServerRequestInfo ri) {}

    @Override
    public void send_exception(ServerRequestInfo ri) throws ForwardRequest {
        System.out.println("### in send_exception()");
    }

    @Override
    public void send_other(ServerRequestInfo ri) throws ForwardRequest {}

    @Override
    public String name() {
        return ServerRequestInterceptorImpl.class.getName();
    }

    @Override
    public void destroy() {}
}
