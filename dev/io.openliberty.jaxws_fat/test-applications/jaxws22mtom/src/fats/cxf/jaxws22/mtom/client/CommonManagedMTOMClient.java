
/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/


package fats.cxf.jaxws22.mtom.client;

import javax.xml.ws.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.ws.handler.Handler;

import fats.cxf.jaxws22.mtom.server.MTOMDDOnly;
import javax.xml.soap.*;
import javax.naming.InitialContext;

/**
 * A client to test the mtom feature. This one is very similar to CommonMTOMClient
 * but will use jndi lookup so we can
 * pull in the deployment descriptor application-client.xml and make sure
 * that works.
 *
 * This time we are testing mtom configuration on the client (request)
 * side, not the service (response) side.
 *
 *
 */
public class CommonManagedMTOMClient {

    // the handler will set these so we can examine the message for correctness.
    public static SOAPMessage outboundmsg;
    public static SOAPMessage inboundmsg;

    // this can be reset by the testcase to something else.
    public static String hostAndPort = "http://localhost:9080";

    /**
     * For debug use. The tests should invoke the individual methods.
     *
     * public static void main(String[] args) throws Exception {
     * CommonManagedMTOMClient me = new CommonManagedMTOMClient();
     * byte[] b = "I am a byte array, so there.".getBytes();
     * // byte [] c = me.genByteArray(3000);
     * //byte [] c = testDynamicProxy(b, new MTOMFeature(true)); // hmm, does not work
     *
     * outboundmsg.writeTo(System.out);
     * System.out.println("\n");
     *
     * System.out.println("mtom enabled?: " + me.checkRequestforMTOMUsage());
     * }
     */

    public static byte[] testProxy(String serviceref, byte[] b, WebServiceFeature... features) throws Exception {
        return testProxy(serviceref, b, null, features);
    }

    /**
     * Invoke the named service-ref . The soap messages will be captured in the
     * static fields of this class, inboundmsg and outboundmsg.
     *
     * To simplify things, there are multiple service-refs in application-client.xml that
     * point to the same web service. It's the name of the service-ref that describes
     * what's in application-client.xml
     *
     * @param service     - name of service to invoke.
     * @param b           - array to send
     * @param injectedRef - a resource that was injected using @WebServiceRef.
     *                        That can only be done in the main class, so we have to pass it in.
     * @param features    - webservice features
     * @return
     * @throws Exception
     */
    public static byte[] testProxy(String serviceref, byte[] b, Object injectedRef, WebServiceFeature... features) throws Exception {
        URL u = null;
        System.out.println("===== client configuration selected is: " + serviceref + " and message size is " + b.length);
        try {
            if (serviceref == "mtom_dd_enabled_2048_no_annotation") {
                // service config: enabled, threshold=2048 (we don't care)
                // client config: enabled, 2048 in application-client.xml, no annotation
                InitialContext ctx = new InitialContext();
                MTOMDDOnly port = (MTOMDDOnly) ctx.lookup("java:comp/env/service/mtom_dd_enabled_2048_no_annotation");
                updateEndpointURL(hostAndPort + "/webservicefeaturetests/MTOMDDOnlyService",
                                  (BindingProvider) port);
                addHandler((BindingProvider) port); // install the monitoring handler
                //System.out.println("invoking");
                byte[] c = port.echobyte(b);
                if (c == null) {
                    System.out.println(" problem, got back a null");
                } else {
                    // System.out.println("c.length = "+c.length);
                }
                return c;

            } else if (serviceref == "mtom_dd_empty_no_threshold") {
                InitialContext ctx = new InitialContext();
                System.out.println("attempting lookup...");
                MTOMDDOnly port = (MTOMDDOnly) ctx.lookup("java:comp/env/service/" + serviceref);
                updateEndpointURL(hostAndPort + "/webservicefeaturetests/MTOMDDOnlyService",
                                  (BindingProvider) port);
                addHandler((BindingProvider) port); // install the monitoring handler
                System.out.println("invoking");
                return (port.echobyte(b));

            } else if (serviceref == "mtom_dd_enabled_no_threshold") {
                InitialContext ctx = new InitialContext();
                System.out.println("attempting lookup...");
                MTOMDDOnly port = (MTOMDDOnly) ctx.lookup("java:comp/env/service/" + serviceref);
                updateEndpointURL(hostAndPort + "/webservicefeaturetests/MTOMDDOnlyService",
                                  (BindingProvider) port);
                addHandler((BindingProvider) port); // install the monitoring handler
                System.out.println("invoking");
                return (port.echobyte(b));

            } else if (serviceref == "mtom_dd_empty_no_annotation") {
                InitialContext ctx = new InitialContext();
                System.out.println("attempting lookup...");
                MTOMDDOnly port = (MTOMDDOnly) ctx.lookup("java:comp/env/service/mtom_dd_empty_no_annotation");
                updateEndpointURL(hostAndPort + "/webservicefeaturetests/MTOMDDOnlyService",
                                  (BindingProvider) port);
                addHandler((BindingProvider) port); // install the monitoring handler
                System.out.println("invoking");
                //if (b.length ==666){ b = null; }  // hack
                return (port.echobyte(b));

            } else if (serviceref == "mtom_dd_enabled_return_serviceref") {
                InitialContext ctx = new InitialContext();
                System.out.println("attempting lookup...");
                MTOMDDOnlyService svc = (MTOMDDOnlyService) ctx.lookup("java:comp/env/service/" + serviceref);
                MTOMDDOnly port = svc.getPort(MTOMDDOnly.class);
                updateEndpointURL(hostAndPort + "/webservicefeaturetests/MTOMDDOnlyService",
                                  (BindingProvider) port);
                addHandler((BindingProvider) port); // install the monitoring handler
                System.out.println("invoking");
                return (port.echobyte(b));

            } else if (serviceref == "mtom_dd_enabled_return_serviceref_injected") {
                //InitialContext ctx = new InitialContext();
                //System.out.println("attempting lookup...");
                //MTOMDDOnlyService svc = (MTOMDDOnlyService)ctx.lookup("java:comp/env/service/"+serviceref);
                MTOMDDOnlyService svc = (MTOMDDOnlyService) injectedRef;
                MTOMDDOnly port = svc.getPort(MTOMDDOnly.class);
                updateEndpointURL(hostAndPort + "/webservicefeaturetests/MTOMDDOnlyService",
                                  (BindingProvider) port);
                addHandler((BindingProvider) port); // install the monitoring handler
                System.out.println("invoking");
                return (port.echobyte(b));

            } else {
                throw new RuntimeException("bad argument: " + serviceref);
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw e;
        }
    }

    /**
     * update the endpoint url
     */
    private static void updateEndpointURL(String serviceURL, BindingProvider port) {
        Map<String, Object> rc = port.getRequestContext();
        rc.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, serviceURL);
    }

    /**
     * install a handler on a port. We'll use the handler to capture the soap message.
     * Much easier than traffic monitoring, etc.
     *
     * @param port
     */
    private static void addHandler(BindingProvider port) {
        // set binding handler chain
        Binding binding = port.getBinding();

        // can create new list or use existing one
        List<Handler> handlerList = binding.getHandlerChain();

        if (handlerList == null) {
            handlerList = new ArrayList<Handler>();
        }

        handlerList.add(new MessageCaptureHandler());

        binding.setHandlerChain(handlerList);

        // clear our static vars, prep for invoke
        inboundmsg = null;
        outboundmsg = null;
    }

    public void printMsg(SOAPMessage msg) {
        try {
            msg.writeTo(System.out);
        } catch (SOAPException e) {
            System.out.println("exception writing soap mesage");
        } catch (java.io.IOException e) {
            System.out.println("ioexception writing soap mesage");
        }
    }

    public String getInboundMsgAsString() {
        java.io.ByteArrayOutputStream baos = null;
        try {
            baos = new java.io.ByteArrayOutputStream();
            inboundmsg.writeTo(baos);

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return baos.toString();
    }

    public String getOutboundMsgAsString() {
        java.io.ByteArrayOutputStream baos = null;
        try {
            baos = new java.io.ByteArrayOutputStream();
            outboundmsg.writeTo(baos);

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        return baos.toString();
    }

    /**
     * look for the mime boundary in the soap message, which would indicate mtom
     * was probably used. (No way to tell for sure without a wire monitor)
     *
     * @return
     */
    private boolean checkRequestforMTOMUsage() {
        if (getInboundMsgAsString().contains("_Part_")) {
            return true;
        }
        return false;
    }

}
