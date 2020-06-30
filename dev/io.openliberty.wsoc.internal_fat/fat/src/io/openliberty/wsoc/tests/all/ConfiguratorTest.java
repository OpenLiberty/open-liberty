/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.tests.all;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.ClientEndpointConfig.Builder;
import javax.websocket.Extension;
import javax.websocket.Extension.Parameter;

import org.junit.Assert;

import io.openliberty.wsoc.util.wsoc.WsocTest;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;
import io.openliberty.wsoc.util.wsoc.WsocTestRunner;
import io.openliberty.wsoc.common.Constants;
import io.openliberty.wsoc.common.ExtensionExt;
import io.openliberty.wsoc.common.ParameterExt;
import io.openliberty.wsoc.endpoints.client.basic.AnnotatedClientEP;
import io.openliberty.wsoc.endpoints.client.basic.AnnotatedConfiguratorClientEP;
import io.openliberty.wsoc.endpoints.client.basic.ConfiguratorClientEP;
import io.openliberty.wsoc.endpoints.client.basic.ProgrammaticConfiguratorClientEP;

/**
 * Tests WebSocket Stuff
 * 
 * @author unknown
 */
public class ConfiguratorTest {

    public static int DEFAULT_TIMEOUT = Constants.getDefaultTimeout();

    private WsocTest wsocTest = null;

    public ConfiguratorTest(WsocTest test) {
        this.wsocTest = test;
    }

    /*
     * ServerEndpoint - @see AnnotatedConfiguratorServerEP - CheckOriginTest
     */
    public void testCheckOriginFailedSuccess() throws Exception {
        try {
            String[] checkValues = { "Test2", "true", "true" };
            wsocTest.runEchoTest(new AnnotatedClientEP.TextTest(checkValues), "/basic/annotatedCheckOrigin", checkValues);
            throw new Exception("Did not get IOException as expected...");
        }

        catch (IOException f) {
            // expected.....
        }

    }

    /*
     * ServerEndpoint - @see AnnotatedConfiguratorServerEP - ConfiguratorTest
     */
    public void testConfiguratorSuccess() throws Exception {

        // Annotated
        WsocTestContext testdata = wsocTest.runWsocTest(new AnnotatedConfiguratorClientEP.ConfiguratorTest(),
                                                        "/basic/annotatedModifyHandshake", WsocTestRunner.getDefaultConfig(), 1, DEFAULT_TIMEOUT);

        testdata.reThrowException(); // throw an exception if there was one
        if (!testdata.getSingleMessage().equals("SUCCESS")) {
            Assert.fail("Configurator failed to modify request and add needed header. msg: " + testdata.getSingleMessage());
        }

        //Programmatic
        testdata = wsocTest.runWsocTest(new ProgrammaticConfiguratorClientEP.ConfiguratorTest(), "/basic/extendedModifyHandshake",
                                        WsocTestRunner.getConfig(new ProgrammaticConfiguratorClientEP.ClientConfigurator()),
                                        1, DEFAULT_TIMEOUT);
        testdata.reThrowException(); // throw an exception if there was one
        if (!testdata.getSingleMessage().equals("SUCCESS")) {
            Assert.fail("Configurator failed to modify request and add needed header. msg: " + testdata.getSingleMessage());
        }

    }

    /*
     * ServerEndpoint - @see AnnotatedConfiguratorServerEP - ConfiguratorTest
     * 
     * @see ProgrammaticServerEP - TextEndpoint.
     */
    public void testNewConfiguratorSuccess() throws Exception {

        // Annotated
        WsocTestContext testdata = wsocTest.runWsocTest(new AnnotatedConfiguratorClientEP.ConfiguratorTest(),
                                                        "/basic/annotatedModifyHandshake", WsocTestRunner.getDefaultConfig(), 1, DEFAULT_TIMEOUT);

        testdata.reThrowException(); // throw an exception if there was one
        if (!testdata.getSingleMessage().equals("SUCCESS")) {
            Assert.fail("Configurator failed to modify request and add needed header. msg: " + testdata.getSingleMessage());
        }

        //Programmatic
        testdata = wsocTest.runWsocTest(new ProgrammaticConfiguratorClientEP.ConfiguratorTest(), "/basic/extendedModifyHandshake",
                                        WsocTestRunner.getConfig(new ProgrammaticConfiguratorClientEP.ClientConfigurator()),
                                        1, DEFAULT_TIMEOUT);
        testdata.reThrowException(); // throw an exception if there was one
        if (!testdata.getSingleMessage().equals("SUCCESS")) {
            Assert.fail("Configurator failed to modify request and add needed header. msg: " + testdata.getSingleMessage());
        }

    }

    /*
     * ServerEndpoint - AnnotatedConfiguratorServerEP - FailHandshakeTest
     */
    public void testFailHandshakeSuccess() throws Exception {
        // Annotated
        try {
            wsocTest.runWsocTest(new AnnotatedConfiguratorClientEP.FailedHandshakeTest(),
                                 "/basic/annotatedFailHandshake", WsocTestRunner.getDefaultConfig(), 1, DEFAULT_TIMEOUT);
            Assert.fail("Able to connect to server when accept header was removed, configurator not working? ");
        }

        catch (IOException f) {
            // expected.....
        }
    }

    /*
     * ServerEndpoint - @see ExtensionServerEP - TextEndpoint
     */
    public void testBasicExtensionsSuccess() throws Exception {

        Builder b = ClientEndpointConfig.Builder.create();
        List<Extension> extensionList = new ArrayList<Extension>(1);
        extensionList.add(new Extension() {

            @Override
            public String getName() {
                return "TestExtension";
            }

            @Override
            public List<Parameter> getParameters() {
                return Collections.emptyList();
            }

        });
        b.extensions(extensionList);

        String[] textValues = { "CLIENTNEGOTIATED0", "NEGOTIATED0" };
        WsocTestContext testdata = wsocTest.runWsocTest(new AnnotatedConfiguratorClientEP.VerifyNoExtensionTest(),
                                                        "/basic/codedExtension", b.build(), 2, DEFAULT_TIMEOUT);
        testdata.reThrowException();
        Assert.assertArrayEquals(textValues, testdata.getMessage().toArray());

    }

    /*
     * ServerEndpoint - @see ExtensionServerEP - TextEndpoint
     */
    public void testNegotiatedExtensionsSuccess() throws Exception {

        Builder b = ClientEndpointConfig.Builder.create();
        Parameter p = new ParameterExt("BLAH", "BLAH");
        Extension e = new ExtensionExt("SecondExt", Arrays.asList(p));
        b.extensions(Arrays.asList(e));

        String[] textValues = { "CLIENTNEGOTIATED1", "SecondExt", "PARAMS1", "BLAH", "BLAH" };
        WsocTestContext testdata = wsocTest.runWsocTest(new ConfiguratorClientEP(),
                                                        "/basic/codedText", b.build(), 2, DEFAULT_TIMEOUT);
        testdata.reThrowException();
        Assert.assertArrayEquals(textValues, testdata.getMessage().toArray());

        String[] values = { "CLIENTNEGOTIATED0" };
        testdata = wsocTest.runWsocTest(new ConfiguratorClientEP(),
                                        "/basic/codedText", ClientEndpointConfig.Builder.create().build(), 2, DEFAULT_TIMEOUT);
        testdata.reThrowException();
        Assert.assertArrayEquals(values, testdata.getMessage().toArray());

    }

    /*
     * ServerEndpoint - @see ExtensionServerEP - TextEndpoint
     */
    public void testNegotiatedExtensionsPart2Success() throws Exception {

        Builder b = ClientEndpointConfig.Builder.create();
        Parameter p = new ParameterExt("12345", "12345");
        Parameter p2 = new ParameterExt("qqq", "qqq");
        Extension e = new ExtensionExt("SecondExt", Arrays.asList(p2, p));
        Parameter p3 = new ParameterExt("ASDF", "ASDF");
        Extension e2 = new ExtensionExt("FirstExt", Arrays.asList(p3));

        b.extensions(Arrays.asList(e, e2));

        String[] textValues = { "CLIENTNEGOTIATED2", "SecondExt", "PARAMS2", "qqq", "qqq", "12345", "12345", "FirstExt", "PARAMS1", "ASDF", "ASDF" };
        WsocTestContext testdata = wsocTest.runWsocTest(new ConfiguratorClientEP(),
                                                        "/basic/codedText", b.build(), 2, DEFAULT_TIMEOUT);
        testdata.reThrowException();
        Assert.assertArrayEquals(textValues, testdata.getMessage().toArray());
    }

    /*
     * ServerEndpoint - @see ExtensionServerEP - ConfiguredTextEndpoint
     */
    public void testDetailedExtensionsSuccess() throws Exception {

        Builder b = ClientEndpointConfig.Builder.create();
        List<Extension> extensionList = new ArrayList<Extension>(1);
        extensionList.add(new Extension() {

            @Override
            public String getName() {
                return "DeflateFrameExtension";
            }

            @Override
            public List<Parameter> getParameters() {
                return Collections.emptyList();
            }

        });
        b.extensions(extensionList);

        String[] textValues = { "CLIENTNEGOTIATED0", "INSTALLED0", "REQUESTED0", "NEGOTIATED0" };

        // Run this through twice to test the other endpoint creation.
        WsocTestContext testdata = wsocTest.runWsocTest(new AnnotatedConfiguratorClientEP.VerifyNoExtensionTest(),
                                                        "/basic/programmaticExtension", b.build(), 4, DEFAULT_TIMEOUT);

        testdata.reThrowException();
        Assert.assertArrayEquals(textValues, testdata.getMessage().toArray());

    }

    /*
     * ServerEndpoint - @see AnnotatedConfiguratorServerEP - SubprotocolTest
     */
    public void testBasicSubprotocolsSuccess() throws Exception {

        String[] textValues = { "Test3" };
        wsocTest.runEchoTest(new AnnotatedConfiguratorClientEP.SubprotocolTest(), "/basic/annotatedSubprotocol", textValues);

    }

    /*
     * ServerEndpoint - @see AnnotatedConfiguratorServerEP - ModifySubprotocolTest
     */
    public void testConfiguredSubprotocolsSuccess() throws Exception {

        String[] checkValues = { "Test2", "true", "true" };
        wsocTest.runEchoTest(new AnnotatedConfiguratorClientEP.ConfiguredSubprotocolTest(), "/basic/annotatedModifySubprotocol", checkValues);

    }

    /*
     * ServerEndpoint - @see AnnotatedConfiguratorServerEP - SubprotocolTest
     */
    public void testConfiguratorCaseSuccess() throws Exception {

        String[] textValues = { "SUCCESS", "SUCCESS" };
        wsocTest.runEchoTest(new AnnotatedConfiguratorClientEP.CaseConfiguratorTest(), "/basic/annotatedCaseConfigurator", textValues);

    }

    public void testTCKConfiguratorSuccess() throws Exception {

        // Annotated
        WsocTestContext testdata = wsocTest.runWsocTest(new AnnotatedConfiguratorClientEP.TCKConfiguratorTest(),
                                                        "/basic/annotatedTCKModifyHandshake", WsocTestRunner.getDefaultConfig(), 1, DEFAULT_TIMEOUT);

        testdata.reThrowException(); // throw an exception if there was one
        if (!testdata.getSingleMessage().equals("SUCCESS")) {
            Assert.fail("Configurator failed to modify request and add needed header. msg: " + testdata.getSingleMessage());
        }
    }

}