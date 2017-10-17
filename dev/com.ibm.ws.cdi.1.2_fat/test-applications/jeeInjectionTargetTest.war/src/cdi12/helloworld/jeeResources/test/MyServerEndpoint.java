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
package cdi12.helloworld.jeeResources.test;

import javax.annotation.Resource;
import javax.websocket.EndpointConfig;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import cdi12.helloworld.jeeResources.ejb.MyManagedBean1;

/**
 *
 */
@ServerEndpoint("/MyServerEndpoint")
public class MyServerEndpoint {

    @Resource
    MyManagedBean1 managedBean1;

    /**
     * @return
     */
    public String hello() {
        return managedBean1.hello();
    }

    @OnOpen
    public void onOpen(final Session session, final EndpointConfig ec) {
//no-op
    }
}
