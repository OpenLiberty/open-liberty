
/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/package fats.cxf.jaxws22.mtom.client;

import javax.xml.ws.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.Handler;
import javax.xml.soap.*;

/**
 * A client to test the mtom feature in various containers.
 *
 *
 */
public class CommonMTOMClient {

    // the handler will set these so we can examine the message for correctness.
    public static SOAPMessage outboundmsg;
    public static SOAPMessage inboundmsg;

    // this can be reset by the testcase to something else.
    public static String hostAndPort = "http://localhost:8080";

    /**
     * For debug use. The tests should invoke the individual methods.
     *
     * public static void main(String[] args) throws Exception {
     *
     * Object pr = javax.xml.ws.spi.Provider.provider();
     * Class theImpl = pr.getClass();
     * System.out.println(theImpl.getName());
     *
     * //byte [] b = "I am a byte array, so there.".getBytes();
     * ManagedClientDriver driver = new ManagedClientDriver();
     * byte[] b = driver.genByteArray(250);
     * byte[] c = testProxy("MTOMonMultipleMethodsAnnotationOnlyService_echobyte64", b);
     *
     * //System.out.println("mtom enabled:" + driver.)
     *
     * System.out.println(new String(c));
     * System.out.println("\n outbound:");
     * outboundmsg.writeTo(System.out);
     * System.out.println("\n inbound:");
     * inboundmsg.writeTo(System.out);
     * System.out.println("\n ");
     *
     * }
     */
    public static void setHostAndPort(String hap) {
        hostAndPort = hap;
    }

    public static byte[] testProxy(String service, byte[] b, WebServiceFeature... features) throws Exception {
        URL u = null;
        System.out.println("===== service selected is: " + service);
        if (service == "MTOMDDOnly") {
            // service config: enabled, threshold=2048
            QName q = new QName("http://server.mtom.jaxws22.cxf.fats/", "MTOMDDOnlyService");
            u = new URL(hostAndPort + "/" + " jaxws22mtom/MTOMDDOnlyService?wsdl");
            // need this when jdk is readY: Service s = Service.create(u, q, features);
            Service s = Service.create(u, q, features);
            MTOMDDOnlyIF port = s.getPort(MTOMDDOnlyIF.class);
            addHandler((BindingProvider) port); // install the monitoring handler
            return (port.echobyte(b));

        } else if (service == "MTOMAnnotationOnly") {
            // service config: enabled, 0
            QName q = new QName("http://server.mtom.jaxws22.cxf.fats/", "MTOMAnnotationOnlyService");
            u = new URL(hostAndPort + "/" + "jaxws22mtom/MTOMAnnotationOnlyService?wsdl");
            Service s = Service.create(u, q);
            MTOMAnnotationOnlyIF port = s.getPort(MTOMAnnotationOnlyIF.class);
            addHandler((BindingProvider) port); // install the monitoring handler
            return (port.echobyte(b));

        } else if (service == "MTOMAnnotationNoMTOM") {
            // service config: disabled
            QName q = new QName("http://server.mtom.jaxws22.cxf.fats/", "MTOMAnnotationNoMTOMService");
            u = new URL(hostAndPort + "/" + "jaxws22mtom/MTOMAnnotationNoMTOMService?wsdl");
            Service s = Service.create(u, q);
            MTOMAnnotationNoMTOMIF port = s.getPort(MTOMAnnotationNoMTOMIF.class);
            addHandler((BindingProvider) port); // install the monitoring handler
            return (port.echobyte(b));

        } else if (service == "BindingTypeMTOMAnnotationOnly") {
            // service config: disabled
            QName q = new QName("http://server.mtom.jaxws22.cxf.fats/", "BindingTypeMTOMAnnotationOnlyService");
            u = new URL(hostAndPort + "/" + "jaxws22mtom/BindingTypeMTOMAnnotationOnlyService?wsdl");
            Service s = Service.create(u, q);
            BindingTypeMTOMAnnotationOnlyIF port = s.getPort(BindingTypeMTOMAnnotationOnlyIF.class);
            addHandler((BindingProvider) port); // install the monitoring handler
            return (port.echobyte(b));

        } else {
            throw new RuntimeException("bad argument to CommonMTOMClient");

        }
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

}
