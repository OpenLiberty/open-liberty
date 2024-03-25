
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.soap.MTOMFeature;

import org.junit.Test;

import componenttest.app.FATServlet;
import fats.cxf.jaxws22.mtom.server.ObjectFactory;

/**
 *
 */
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/MTOMAnnotationsTestServlet")
public class MTOMAnnotationsTestServlet extends FATServlet {

    private static final String SERVICE_NS = "http://server.mtom.jaxws22.cxf.fats/";

    private static final String wsdlLocation;

    private static String hostAndPort = null;
    //private static CommonMTOMClient client = new CommonMTOMClient();
    MessageCaptureHandler mch = null;

    private static String host = null;

    private static String largeString = "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";
    private static final StringReader reqMsg = new StringReader("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body xmlns=\"http://jaxws.basic.cxf.fats/types\"><invoke>JAXWS FVT Version: 2.0</invoke></soapenv:Body></soapenv:Envelope>");

    static {
        wsdlLocation = new StringBuilder().append("http://localhost:").append(Integer.getInteger("bvt.prop.HTTP_default")).append("jaxws22mtom/MTOMDDOnlyService?wsdl").toString();
        hostAndPort = new StringBuilder().append("http://localhost:").append(Integer.getInteger("bvt.prop.HTTP_default")).toString();
    }

    /**
     * Test Description:
     * Negative test that ensures a Service cannot be created by calling
     * Service.create(QName, Url, WebServiceFeature)
     *
     * Expected Result:
     * Cannot create a service, and should throw a WebServiceException
     *
     */
    @Test
    public void testCreate3argNoFeature() throws Exception {
        String thisMethod = "testCreate3argNoFeature()";

        QName q = new QName("http://server.mtom.jaxws22.cxf.fats/", "MTOMDDOnlyService");
        URL u = new URL(hostAndPort + "/" + "jaxws22mtom/MTOMDDOnlyService?wsdl");
        System.out.println(thisMethod + " Ensuring 3 argument constructor with WSDL at URL = " + u.toString());
        Service s = Service.create(u, q, new WebServiceFeature[] {});
        MTOMDDOnlyIF port = s.getPort(MTOMDDOnlyIF.class);
        addHandler((BindingProvider) port); // install the monitoring handler
        byte[] b = this.genByteArray(2000);
        byte[] c = port.echobyte(b); // invoke service, handler will capture soap messages
        // System.out.println("outbound message: "+ mch.getOutboundMsgAsString());
        //System.out.println("inbound message: "+ mch.getInboundMsgAsString());
        System.out.println(thisMethod + " Ensuring 3 argument constructor with WSDL at URL = " + u.toString());
        assertFalse("expected mtom to be disabled but was not", checkRequestforMTOMUsage());
        assertTrue("echo did not work correctly ", this.compareByteArrays(b, c));
        System.out.println(thisMethod + " The service was successfully created with no Feature argument");

    }

    /**
     * Test Description:
     * Test to make sure that a MTOMService can be successfully created even with
     * ?wsdl in URL for the constructor.
     *
     * Expected Result:
     * Service should be created and an echo response given as a reply.
     *
     */
    @Test
    public void testStaticCreate2argqmarkwsdl() throws Exception {
        String thisMethod = "testStaticCreate2argqmarkwsdl()";

        URL u = new URL(hostAndPort + "/" + "jaxws22mtom/MTOMDDOnlyService?wsdl");

        MTOMDDOnlyService s = new MTOMDDOnlyService(u);

        MTOMDDOnlyIF port = s.getPort(MTOMDDOnlyIF.class);
        addHandler((BindingProvider) port); // install the monitoring handler
        byte[] b = this.genByteArray(255);
        byte[] c = port.echobyte(b); // invoke service, handler will capture soap messages
        //  System.out.println("outbound message: "+ mch.getOutboundMsgAsString());
        //  System.out.println("inbound message: "+ mch.getInboundMsgAsString());
        assertTrue("echo did not work", this.compareByteArrays(b, c));
        System.out.println(thisMethod + " echo worked");
    }

    /**
     * Test Description:
     * Builds a SOAPMessage and then sends an MTOM enabled request to an MTOM service using
     * Dispatch<SOAPMessage>
     *
     * Expected Result:
     * Test should return a MTOM enabled response message from MTOMService
     *
     */
    @Test
    public void testDispatchWithSoapMessage() throws Exception {

        String thisMethod = "testDispatchWithJAXBContext()";

        QName svcQName = new QName("http://server.mtom.jaxws22.cxf.fats/", "MTOMDDOnlyService");
        QName portName = new QName("http://server.mtom.jaxws22.cxf.fats/", "MTOMDDOnlyPort");
        URL u = new URL(hostAndPort + "/" + "jaxws22mtom/MTOMDDOnlyService?wsdl");
        Service s = Service.create(u, svcQName);

        // let's see if this has any chance of working....
        Dispatch<SOAPMessage> dsp = s.createDispatch(
                                                     portName,
                                                     javax.xml.soap.SOAPMessage.class,
                                                     Service.Mode.MESSAGE,
                                                     new MTOMFeature(true, 64));

        mch = addHandler(dsp);

        SOAPMessage msg = null;
        String msgString = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><ns2:echobyte xmlns:ns2=\"http://server.mtom.jaxws22.cxf.fats/\"><arg0>AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8gISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0+P0BBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWltcXV5fYGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6e3x9fn+AgYKDhIWGh4iJiouMjY6PkJGSk5SVlpeYmZqbnJ2en6ChoqOkpaanqKmqq6ytrq+wsbKztLW2t7i5uru8vb6/wMHCw8TFxsfIycrLzM3Oz9DR0tPU1dbX2Nna29zd3t/g4eLj5OXm5+jp6uvs7e7v8PHy8/T19vf4+fr7/P3+</arg0></ns2:echobyte></soapenv:Body></soapenv:Envelope>";
        msg = toSOAPMessage(msgString);

        // a way to print the msg
        //mch.outboundmsg = msg;
        //System.out.println(mch.getOutboundMsgAsString());

        SOAPMessage result = dsp.invoke(msg);

        //System.out.println(mch.getInboundMsgAsString());
        //System.out.println(mch.getOutboundMsgAsString());

        checkResponseforMTOMUsage();
    }

    /**
     * Test Description:
     * Builds a SOAPMessage and then sends an MTOM enabled request to an MTOM service using
     * Dispatch<SOAPMessage>. This test creates a dispatch client which with an mtom feature with the threshold set to 21 bytes.
     * Outbound jaxb objects greater than 21 bytes in size should be sent as an mtom attachment.
     * A jaxb object is created from a 65-byte array and then transmitted by the client.
     * The outbound request is inspected to see if mtom was used.
     *
     * Expected Result:
     * Test should return a MTOM enabled response message from MTOMService and
     * the outbound request is inspected to see if mtom was used.
     *
     */
    @Test
    public void testDispatchWithJAXBContext() throws Exception {
        String thisMethod = "testDispatchWithJAXBContext()";

        URL u = new URL(hostAndPort + "/" + "jaxws22mtom/MTOMAnnotationOnlyService?wsdl");

        QName svcQName = new QName("http://server.mtom.jaxws22.cxf.fats/", "MTOMAnnotationOnlyService");
        QName portName = new QName("http://server.mtom.jaxws22.cxf.fats/", "MTOMAnnotationOnlyPort");
        Service s = Service.create(u, svcQName);

        System.out.println(thisMethod + " Service: " + s);
        // create context using objectfactory that was produced from wsimport.
        JAXBContext context = JAXBContext.newInstance(ObjectFactory.class);
        System.out.println(thisMethod + " JAXBContext: " + context.toString());
        // create the client and attach the feature using createdispatch(port, context,mode, feature) api.
        Dispatch dsp = s.createDispatch(portName,
                                        context,
                                        Service.Mode.PAYLOAD,
                                        new MTOMFeature(true, 21));

        mch = addHandler(dsp); // add the monitoring handler.

        Unmarshaller um = context.createUnmarshaller();

        // this is the xml representation of a byte array 65 bytes long.
        // I couldn't figure out how to marshal a byte array into a jaxb object directly,
        // i.e. new jaxbobject(byte[] foo) does not exist, so did it this way instead.
        // Looks strange but gets it done for testing purposes.

        String msgString = "<ns2:echobyte xmlns:ns2=\"http://server.mtom.jaxws22.cxf.fats/\"><arg0>AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8gISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0+P0BBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWltcXV5fYGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6e3x9fn+AgYKDhIWGh4iJiouMjY6PkJGSk5SVlpeYmZqbnJ2en6ChoqOkpaanqKmqq6ytrq+wsbKztLW2t7i5uru8vb6/wMHCw8TFxsfIycrLzM3Oz9DR0tPU1dbX2Nna29zd3t/g4eLj5OXm5+jp6uvs7e7v8PHy8/T19vf4+fr7/P3+</arg0></ns2:echobyte>";
        ByteArrayInputStream input = new ByteArrayInputStream(msgString.getBytes());
        Object jaxbObject = um.unmarshal(input);
        System.out.println(thisMethod + " JAX-B Object" + jaxbObject);
        Object result = dsp.invoke(jaxbObject);

        assertTrue("mtom should have been used but was not", checkResponseforMTOMUsage());
    }

    /**
     * Test Description:
     * Builds a SOAPMessage and then sends an MTOM enabled request to an MTOM service using
     * Dispatch<SOAPMessage>. When nothing is specified in annotation or dd, pass a large
     * message and make sure we don't have any hardcoded threshold
     * above which we mtom enable.
     *
     * Expected Result:
     * This service impl has annotations on the methods and Mtom should be used.
     *
     */
    @Test
    public void testDefaultWithLargeMessage() throws Exception {

        String thisMethod = "testDefaultWithLargeMessage()";

        QName svcQName = new QName("http://server.mtom.jaxws22.cxf.fats/", "MTOMAnnotationOnlyService");
        QName portName = new QName("http://server.mtom.jaxws22.cxf.fats/", "MTOMAnnotationOnlyPort");
        URL u = new URL(hostAndPort + "/" + "jaxws22mtom/MTOMAnnotationOnlyService?wsdl");
        Service s = Service.create(u, svcQName);

        // let's see if this has any chance of working....
        Dispatch<SOAPMessage> dsp = s.createDispatch(
                                                     portName,
                                                     javax.xml.soap.SOAPMessage.class,
                                                     Service.Mode.MESSAGE,
                                                     new MTOMFeature(true, 256));

        mch = addHandler(dsp);

        SOAPMessage msg = null;
        String msgString = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><ns2:echobyte xmlns:ns2=\"http://server.mtom.jaxws22.cxf.fats/\"><arg0>"
                           + largeString + "</arg0></ns2:echobyte></soapenv:Body></soapenv:Envelope>";
        msg = toSOAPMessage(msgString);

        // a way to print the msg
        //mch.outboundmsg = msg;
        //System.out.println(mch.getOutboundMsgAsString());

        SOAPMessage result = dsp.invoke(msg);

        //System.out.println(mch.getInboundMsgAsString());
        //System.out.println(mch.getOutboundMsgAsString());
        System.out.println(thisMethod + " Request to be sent: " + mch.getOutboundMsgAsString());
        System.out.println(thisMethod + " Response sent back: " + mch.getInboundMsgAsString());

        assertTrue("mtom was expected to be enabled but was not",
                   checkResponseforMTOMUsage());

    }

    /**
     * Test Description:
     * Builds a SOAPMessage and then sends an MTOM enabled request to an MTOM service using
     * Dispatch<SOAPMessage>. Tests that the size of the MTOM enablement threshold when set annotation = 0, will enable MTOM on
     * on any sized message
     *
     * Expected Result:
     * This service impl has annotations on the methods and Mtom should be used.
     *
     */
    @Test
    public void testAnnoOnlyEnabled() throws Exception {

        String thisMethod = "testAnnoOnlyEnabled()";

        QName svcQName = new QName("http://server.mtom.jaxws22.cxf.fats/", "MTOMAnnotationNoMTOMService");
        QName portName = new QName("http://server.mtom.jaxws22.cxf.fats/", "MTOMAnnotationNoMTOMPort");
        URL u = new URL(hostAndPort + "/" + "jaxws22mtom/MTOMAnnotationNoMTOMService?wsdl");
        Service s = Service.create(u, svcQName);

        // let's see if this has any chance of working....
        Dispatch<SOAPMessage> dsp = s.createDispatch(
                                                     portName,
                                                     javax.xml.soap.SOAPMessage.class,
                                                     Service.Mode.MESSAGE,
                                                     new MTOMFeature(true, 64));

        mch = addHandler(dsp);

        SOAPMessage msg = null;
        String msgString = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><ns2:echobyte xmlns:ns2=\"http://server.mtom.jaxws22.cxf.fats/\"><arg0>AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8gISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0+P0BBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWltcXV5fYGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6e3x9fn+AgYKDhIWGh4iJiouMjY6PkJGSk5SVlpeYmZqbnJ2en6ChoqOkpaanqKmqq6ytrq+wsbKztLW2t7i5uru8vb6/wMHCw8TFxsfIycrLzM3Oz9DR0tPU1dbX2Nna29zd3t/g4eLj5OXm5+jp6uvs7e7v8PHy8/T19vf4+fr7/P3+</arg0></ns2:echobyte></soapenv:Body></soapenv:Envelope>";
        msg = toSOAPMessage(msgString);

        // a way to print the msg
        //mch.outboundmsg = msg;
        //System.out.println(mch.getOutboundMsgAsString());

        SOAPMessage result = dsp.invoke(msg);

        //System.out.println(mch.getInboundMsgAsString());
        //System.out.println(mch.getOutboundMsgAsString());

        assertTrue("mtom was expected to be enabled but was not",
                   checkResponseforMTOMUsage());
    }

    /**
     * Test Description:
     * Builds a SOAPMessage and then sends an MTOM enabled request to an MTOM service using
     * Dispatch<SOAPMessage>. Tests that annotation is disabled and MTOM should not be used
     *
     * Expected Result:
     * This service impl has annotations on the methods and Mtom should NOT be used.
     *
     */
    @Test
    public void testAnnoOnlyDisabled() throws Exception {

        String thisMethod = "testBindingTypeMTOMAnnotationOnly()";

        QName svcQName = new QName("http://server.mtom.jaxws22.cxf.fats/", "MTOMAnnotationNoMTOMService");
        QName portName = new QName("http://server.mtom.jaxws22.cxf.fats/", "MTOMAnnotationNoMTOMPort");
        URL u = new URL(hostAndPort + "/" + "jaxws22mtom/MTOMAnnotationNoMTOMService?wsdl");
        Service s = Service.create(u, svcQName);

        // let's see if this has any chance of working....
        Dispatch<SOAPMessage> dsp = s.createDispatch(
                                                     portName,
                                                     javax.xml.soap.SOAPMessage.class,
                                                     Service.Mode.MESSAGE,
                                                     new MTOMFeature(true, 64));

        mch = addHandler(dsp);

        SOAPMessage msg = null;
        String msgString = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><ns2:echobyte xmlns:ns2=\"http://server.mtom.jaxws22.cxf.fats/\"><arg0>AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8gISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0+P0BBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWltcXV5fYGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6e3x9fn+AgYKDhIWGh4iJiouMjY6PkJGSk5SVlpeYmZqbnJ2en6ChoqOkpaanqKmqq6ytrq+wsbKztLW2t7i5uru8vb6/wMHCw8TFxsfIycrLzM3Oz9DR0tPU1dbX2Nna29zd3t/g4eLj5OXm5+jp6uvs7e7v8PHy8/T19vf4+fr7/P3+</arg0></ns2:echobyte></soapenv:Body></soapenv:Envelope>";
        msg = toSOAPMessage(msgString);

        // a way to print the msg
        //mch.outboundmsg = msg;
        //System.out.println(mch.getOutboundMsgAsString());

        SOAPMessage result = dsp.invoke(msg);

        //System.out.println(mch.getInboundMsgAsString());
        //System.out.println(mch.getOutboundMsgAsString());

        assertFalse("mtom was not expected to be enabled but it was",
                    checkRequestforMTOMUsage());
    }

    /**
     * Test Description:
     * Builds a SOAPMessage and then sends an MTOM enabled request to an MTOM service using
     * Dispatch<SOAPMessage>. Tests that MTOM is enabled using BindingType annotation
     *
     * Expected Result:
     * This service impl has annotations on the methods and Mtom should be used.
     *
     */
    @Test
    public void testBindingTypeMTOMAnnotationOnly() throws Exception {

        String thisMethod = "testBindingTypeMTOMAnnotationOnly()";

        QName svcQName = new QName("http://server.mtom.jaxws22.cxf.fats/", "BindingTypeMTOMAnnotationOnlyService");
        QName portName = new QName("http://server.mtom.jaxws22.cxf.fats/", "BindingTypeMTOMAnnotationOnlyPort");
        URL u = new URL(hostAndPort + "/" + "jaxws22mtom/BindingTypeMTOMAnnotationOnlyService?wsdl");
        Service s = Service.create(u, svcQName);

        // let's see if this has any chance of working....
        Dispatch<SOAPMessage> dsp = s.createDispatch(
                                                     portName,
                                                     javax.xml.soap.SOAPMessage.class,
                                                     Service.Mode.MESSAGE,
                                                     new MTOMFeature(true, 64));

        mch = addHandler(dsp);

        SOAPMessage msg = null;
        String msgString = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><ns2:echobyte xmlns:ns2=\"http://server.mtom.jaxws22.cxf.fats/\"><arg0>AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8gISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0+P0BBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWltcXV5fYGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6e3x9fn+AgYKDhIWGh4iJiouMjY6PkJGSk5SVlpeYmZqbnJ2en6ChoqOkpaanqKmqq6ytrq+wsbKztLW2t7i5uru8vb6/wMHCw8TFxsfIycrLzM3Oz9DR0tPU1dbX2Nna29zd3t/g4eLj5OXm5+jp6uvs7e7v8PHy8/T19vf4+fr7/P3+</arg0></ns2:echobyte></soapenv:Body></soapenv:Envelope>";
        msg = toSOAPMessage(msgString);

        // a way to print the msg
        //mch.outboundmsg = msg;
        //System.out.println(mch.getOutboundMsgAsString());

        SOAPMessage result = dsp.invoke(msg);

        //System.out.println(mch.getInboundMsgAsString());
        //System.out.println(mch.getOutboundMsgAsString());
        System.out.println(thisMethod + " Request to be sent: " + mch.getOutboundMsgAsString());
        System.out.println(thisMethod + " Response sent back: " + mch.getInboundMsgAsString());

        assertTrue("mtom was expected to be enabled but was not",
                   checkResponseforMTOMUsage());

    }

    /**
     * Method used to convert Strings to SOAPMessages. We will use a detection
     * routine to see if SOAP 1.2 support is present. If it is we will enable
     * dynamic protocol selection, otherwise we will default to SOAP 1.1 (SAAJ
     * 1.2)
     *
     * Recycled from Sedov's jaxws dispatch tests.
     *
     * @param msgString
     * @return
     */
    private static SOAPMessage toSOAPMessage(String msgString) {

        if (msgString == null)
            return null;
        String SOAP11_NAMESPACE = "http://schemas.xmlsoap.org/soap/envelope";

        SOAPMessage message = null;
        try {

            MessageFactory factory = null;

            // Force the usage of specific MesasgeFactories
            if (msgString.indexOf(SOAP11_NAMESPACE) >= 0) {
                factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
            } else {
                factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
            }

            message = factory.createMessage();
            message.getSOAPPart().setContent(
                                             new StreamSource(new StringReader(msgString)));
            message.saveChanges();
        } catch (SOAPException e) {
            System.out.println("toSOAPMessage Exception encountered: " + e);
            e.printStackTrace();
        }

        return message;
    }

    private byte[] genByteArray(int size) {
        byte[] ba = new byte[size];
        int i = 0;
        byte j = 0;
        for (i = 0; i < size; i++) {
            ba[i] = j++;
            if (j > 250) {
                j = 0;
            }
        }
        // System.out.println("request: "+size+" actual: "+ba.length);
        return ba;
    }

    private boolean compareByteArrays(byte[] expect, byte[] actual) {
        if (expect.length != actual.length) {
            System.out.println("length mismatch, expect =" + expect.length +
                               " actual = " + actual.length);
            return false;
        }
        int max = expect.length;
        for (int i = 0; i < max; i++) {
            if (expect[i] != actual[i]) {
                System.out.println("content mismatch at offset " + i);
                return false;
            }
        }
        return true;
    }

    /**
     * look for the mime boundary in the soap message, which would indicate mtom
     * was probably used. (No way to tell for sure without a wire monitor)
     *
     * @return
     */
    private boolean checkRequestforMTOMUsage() {
        String thisMethod = "checkRequestforMTOMUsage()";
        System.out.println(thisMethod + " Checking Request for MTOM: " + mch.getOutboundMsgAsString());
        if (mch.getOutboundMsgAsString().contains("_Part_")) {
            return true;
        }
        return false;
    }

    private boolean checkResponseforMTOMUsage() {
        String thisMethod = "checkResponseforMTOMUsage()";
        System.out.println(thisMethod + " Checking Request for MTOM: " + mch.getInboundMsgAsString());
        if (mch.getInboundMsgAsString().contains("_Part_")) {
            return true;
        }
        return false;
    }

    /**
     * install a handler on a port. We'll use the handler to capture the soap message.
     * Much easier than traffic monitoring, etc.
     *
     * @param port
     */
    private MessageCaptureHandler addHandler(BindingProvider port) {
        // set binding handler chain
        Binding binding = port.getBinding();

        // can create new list or use existing one
        List<Handler> handlerList = binding.getHandlerChain();

        if (handlerList == null) {
            handlerList = new ArrayList<Handler>();
        }

        MessageCaptureHandler mch = new MessageCaptureHandler();
        handlerList.add(mch);

        binding.setHandlerChain(handlerList);

        this.mch = mch;
        return mch;

    }

}
