/*
 * Copyright 2015, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.transport.iiop.security;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import javax.net.ssl.SSLSession;
import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.ORB;
import org.omg.CORBA.UserException;
import org.omg.CSI.AuthorizationElement;
import org.omg.CSI.EstablishContext;
import org.omg.CSI.IdentityToken;
import org.omg.CSI.SASContextBody;
import org.omg.CSI.SASContextBodyHelper;
import org.omg.IOP.Codec;
import org.omg.IOP.SecurityAttributeService;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.ServerRequestInfo;

import test.common.SharedOutputManager;

import com.ibm.ws.security.csiv2.test.tools.TestCodec;
import com.ibm.ws.transport.iiop.security.config.tss.TSSConfig;

public class ServerSecurityInterceptorTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private ServerSecurityInterceptor serverSecurityInterceptor;
    private Codec codec;
    private ServerRequestInfo ri;
    private TSSConfig tssConfig;
    private EstablishContext establishContextMsg;
    private Subject authenticatedSubject;
    private com.ibm.ws.security.context.SubjectManager subjectManager;

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() {
        outputMgr.trace("com.ibm.ws.transport.iiop.security.ServerSecurityInterceptor=all");
    }

    @Before
    public void setUp() {
        codec = new TestCodec();
        serverSecurityInterceptor = new ServerSecurityInterceptor(codec);
        subjectManager = new com.ibm.ws.security.context.SubjectManager();
        ri = mockery.mock(ServerRequestInfo.class);
        tssConfig = mockery.mock(TSSConfig.class);
        establishContextMsg = new EstablishContext();
        authenticatedSubject = new Subject();

        setRequestInfoExpectations();
    }

    private void setRequestInfoExpectations() {
        mockery.checking(new Expectations() {
            {
                allowing(ri).request_id();
                will(returnValue(1));
                allowing(ri).operation();
                will(returnValue("TestOperation"));
                allowing(ri).object_id();
                will(returnValue("123".getBytes()));
            }
        });
    }

    @After
    public void tearDown() {
        subjectManager.clearSubjects();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void traceTearDown() {
        outputMgr.trace("*=all=disabled");
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.ServerSecurityInterceptor#receive_request(org.omg.PortableInterceptor.ServerRequestInfo)}.
     */
    // @Test TODO: Temporarily DISABLED until it can be done properly in sun jdk. There is an NPE in the receive_request method when running this unit test in a sun/mac jdk.
    public void testReceive_request() throws Exception {
        ServerPolicy serverPolicy = new ServerPolicy(tssConfig);
        setRequestInfoServerPolicy(serverPolicy);
        ServiceContext serviceContext = generateServiceContext(codec);
        setRequestInfoServiceContext(serviceContext);
        setTSSConfigCheckExpectations(authenticatedSubject);

        serverSecurityInterceptor.receive_request(ri);

        assertNotNull("There must be a caller subject on the thread.", subjectManager.getCallerSubject());
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.ServerSecurityInterceptor#receive_request(org.omg.PortableInterceptor.ServerRequestInfo)}.
     */
    @Test
    public void testReceive_request_noServerPolicy() {
        setRequestInfoServerPolicy(null);
        serverSecurityInterceptor.receive_request(ri);
        assertNull("There must not be a caller subject on the thread.", subjectManager.getCallerSubject());
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.ServerSecurityInterceptor#receive_request(org.omg.PortableInterceptor.ServerRequestInfo)}.
     */
    @Test
    public void testReceive_request_noTSSConfig() {
        TSSConfig tssConfig = null;
        ServerPolicy serverPolicy = new ServerPolicy(tssConfig);
        setRequestInfoServerPolicy(serverPolicy);
        serverSecurityInterceptor.receive_request(ri);
        assertNull("There must not be a caller subject on the thread.", subjectManager.getCallerSubject());
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.ServerSecurityInterceptor#receive_request(org.omg.PortableInterceptor.ServerRequestInfo)}.
     */
    @Test
    public void testReceive_request_noServiceContext() throws Exception {
        ServerPolicy serverPolicy = new ServerPolicy(tssConfig);
        setRequestInfoServerPolicy(serverPolicy);
        noServiceContextThrowsBadParamException();
        setAcceptTransportContextExpectations(authenticatedSubject);

        serverSecurityInterceptor.receive_request(ri);

        assertNotNull("There must be a caller subject on the thread.", subjectManager.getCallerSubject());
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.ServerSecurityInterceptor#receive_request(org.omg.PortableInterceptor.ServerRequestInfo)}.
     */
    @Test
    public void testReceive_request_malformedSASContextBodyInServiceContext() throws Exception {
        ServerPolicy serverPolicy = new ServerPolicy(tssConfig);
        setRequestInfoServerPolicy(serverPolicy);
        ServiceContext serviceContext = generateServiceContextWithoutValidSASContextBody(codec);
        setRequestInfoServiceContext(serviceContext);
        //setAcceptTransportContextExpectations(authenticatedSubject);

        try {
            serverSecurityInterceptor.receive_request(ri);
            fail("There must be an exception thrown when the SAS context body is malformed.");
        } catch (Exception e) {
        }
    }

    private void setRequestInfoServerPolicy(final ServerPolicy serverPolicy) {
        mockery.checking(new Expectations() {
            {
                one(ri).get_server_policy(ServerPolicyFactory.POLICY_TYPE);
                will(returnValue(serverPolicy));
            }
        });
    }

    private void setRequestInfoServiceContext(final ServiceContext serviceContext) {
        mockery.checking(new Expectations() {
            {
                one(ri).get_request_service_context(SecurityAttributeService.value);
                will(returnValue(serviceContext));
            }
        });
    }

    private void noServiceContextThrowsBadParamException() {
        mockery.checking(new Expectations() {
            {
                one(ri).get_request_service_context(SecurityAttributeService.value);
                will(throwException(new BAD_PARAM()));
            }
        });
    }

    private ServiceContext generateServiceContext(Codec codec) throws UserException {

        establishContextMsg.client_context_id = 0;
        // Contents do not matter for now since only the relationships are being tested.
        establishContextMsg.client_authentication_token = new byte[0];
        establishContextMsg.authorization_token = new AuthorizationElement[0];
        IdentityToken identityToken = new IdentityToken();
        identityToken.absent(true);
        establishContextMsg.identity_token = identityToken;

        ServiceContext context = new ServiceContext();

        SASContextBody sas = new SASContextBody();
        sas.establish_msg(establishContextMsg);
        Any sas_any = ORB.init().create_any();
        SASContextBodyHelper.insert(sas_any, sas);
        context.context_data = codec.encode_value(sas_any);

        context.context_id = SecurityAttributeService.value;

        return context;
    }

    private ServiceContext generateServiceContextWithoutValidSASContextBody(Codec codec) throws UserException {
        ServiceContext context = new ServiceContext();
        context.context_data = new byte[0];
        context.context_id = SecurityAttributeService.value;
        return context;
    }

    private void setTSSConfigCheckExpectations(final Subject subject) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(tssConfig).check(with(aNull(SSLSession.class)), with(aNonNull(EstablishContext.class)), with(codec));
                will(returnValue(subject));
            }
        });
    }

    private void setAcceptTransportContextExpectations(final Subject subject) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(tssConfig).check(with(aNull(SSLSession.class)), with(aNull(EstablishContext.class)), with(codec));
                will(returnValue(subject));
            }
        });
    }

}
