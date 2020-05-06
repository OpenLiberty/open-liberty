/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.opensaml.saml2.metadata.Endpoint;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml2.metadata.SingleSignOnService;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.parse.ParserPool;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.ls.LSSerializerFilter;

import com.ibm.ws.security.common.structures.Cache;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.impl.Activator;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContextBuilder;
import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;
import com.ibm.ws.security.saml.sso20.internal.utils.ForwardRequestInfo;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.wsspi.security.tai.TAIResult;

import test.common.SharedOutputManager;

public class InitiatorTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private static final CommonMockObjects common = new CommonMockObjects();
    private static final Mockery mockery = common.getMockery();

    private static final SsoSamlService ssoService = common.getSsoService();
    private static final HttpServletResponse response = common.getServletResponse();
    private static final HttpServletRequest request = common.getServletRequest();

    private static final HttpSession session = common.getSession();
    private static final WebAppSecurityConfig webAppSecConfig = common.getWebAppSecConfig();
    static {
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);
    }

    private static Initiator initiator;

    private static final BasicMessageContext<?, ?, ?> basicMessageContext = common.getBasicMessageContext();
    private static final BasicMessageContextBuilder<?, ?, ?> basicMessageContextBuilder = common.getBasicMessageContextBuilder();
    private static final MetadataProvider metadataProvider = common.getMetadataProvider();
    private static final XMLObject metadata = mockery.mock(EntityDescriptor.class, "metadata");
    private static final IDPSSODescriptor ssoDescriptor = mockery.mock(IDPSSODescriptor.class, "ssoDescriptor");
    private static final SingleSignOnService singleSignOnService = mockery.mock(SingleSignOnService.class, "singleSignOnService");
    private static final SsoConfig ssoConfig = common.getSsoConfig();
    private static final Endpoint endpoint = mockery.mock(Endpoint.class, "endpoint");
    private static final ParserPool parserPool = mockery.mock(ParserPool.class, "parserPool");
    private static final Document document = mockery.mock(Document.class, "document");
    private static final Element element = mockery.mock(Element.class, "element");
    private static final DOMImplementation implementation = mockery.mock(DOMImplementation.class, "implementation");
    private static final DOMImplementationLS implementationLS = mockery.mock(DOMImplementationLS.class, "implementationLS");
    private static final LSSerializer serializer = mockery.mock(LSSerializer.class, "serializer");
    private static final LSOutput serializerOut = mockery.mock(LSOutput.class, "serializerOut");
    private static final PrintWriter out = mockery.mock(PrintWriter.class, "out");
    private static final Cache cache = common.getCache();
    private static final String PROVIDER_ID = "b07b804c";
    private static final String DEFAULT_KS_PASS = "Liberty";

    private static List<SingleSignOnService> listSingleSignOnServices = new ArrayList<SingleSignOnService>();

    private static final Activator activator = new Activator();
    @SuppressWarnings("rawtypes")
    static BasicMessageContextBuilder<?, ?, ?> instance = new BasicMessageContextBuilder();

    @BeforeClass
    public static void setUp() throws Exception {
        outputMgr.trace("*=all");
        BasicMessageContextBuilder.setInstance(basicMessageContextBuilder);
        activator.start(null);
        final String[] arrayAuthnContextClassRef = { "test" };

        mockery.checking(new Expectations() {
            {
                allowing(basicMessageContextBuilder).buildIdp(request, response, ssoService);
                will(returnValue(basicMessageContext));
                allowing(basicMessageContext).setPeerEntityId(null);
                allowing(basicMessageContext).setPeerEntityEndpoint(singleSignOnService);
                allowing(basicMessageContext).getPeerEntityEndpoint();
                will(returnValue(endpoint));
                allowing(basicMessageContext).getSsoConfig();
                will(returnValue(ssoConfig));
                allowing(ssoConfig).getSpHostAndPort();
                will(returnValue(null));

                allowing(ssoConfig).createSession();
                will(returnValue(with(any(Boolean.class))));
                allowing(request).getSession(true);
                will(returnValue(session));

                allowing(endpoint).getLocation();
                will(returnValue(with(any(String.class))));

                allowing(metadataProvider).getMetadata();
                will(returnValue(metadata));

                allowing((EntityDescriptor) metadata).getEntityID();
                will(returnValue(null));
                allowing((EntityDescriptor) metadata).getIDPSSODescriptor(Constants.SAML20P_NS);
                will(returnValue(ssoDescriptor));

                allowing(ssoDescriptor).getSingleSignOnServices();
                will(returnValue(listSingleSignOnServices));

                allowing(singleSignOnService).getBinding();
                will(returnValue(Constants.SAML2_POST_BINDING_URI));
                allowing(singleSignOnService).getLocation();
                will(returnValue("https://localhost:8020/ibm/saml20/sp"));

                allowing(request).getRequestURL();
                will(returnValue(new StringBuffer("http://localhost:8010/formlogin")));
                allowing(request).getQueryString(); //
                will(returnValue(null)); //
                allowing(request).getMethod();
                will(returnValue("PUT"));
                allowing(request).getServerName();
                will(returnValue("localhost"));
                allowing(request).getServerPort();
                will(returnValue(8020));
                allowing(request).getScheme();
                will(returnValue("https"));
                allowing(request).isSecure();
                will(returnValue(true));
                allowing(ssoService).getProviderId();
                will(returnValue(PROVIDER_ID));
                allowing(ssoService).getDefaultKeyStorePassword();
                will(returnValue(DEFAULT_KS_PASS));
                allowing(ssoService).getConfig();
                will(returnValue(ssoConfig));
                one(ssoService).getAcsCookieCache(PROVIDER_ID);
                will(returnValue(cache));

                one(response).setStatus(with(any(Integer.class)));
                one(response).setHeader(with(any(String.class)), with(any(String.class)));
                one(response).setHeader(with(any(String.class)), with(any(String.class)));
                one(response).setDateHeader(with(any(String.class)), with(any(Integer.class)));
                one(response).setContentType(with(any(String.class)));
                allowing(response).addCookie(with(any(Cookie.class)));
                one(response).getWriter();
                will(returnValue(out));

                one(out).println(with(any(String.class)));
                one(out).flush();

                allowing(ssoConfig).isForceAuthn();
                will(returnValue(with(any(Boolean.class))));
                allowing(ssoConfig).isPassive();
                will(returnValue(with(any(Boolean.class))));
                allowing(ssoConfig).getNameIDFormat();
                will(returnValue("NameIDPolicy"));
                allowing(ssoConfig).getAllowCreate();
                will(returnValue(new Boolean(true)));
                allowing(ssoConfig).getAuthnContextClassRef();
                will(returnValue(arrayAuthnContextClassRef));
                allowing(ssoConfig).getAuthnContextComparisonType();
                will(returnValue("default"));
                allowing(ssoConfig).isHttpsRequired();
                will(returnValue(with(any(Boolean.class))));
                allowing(parserPool).newDocument();
                will(returnValue(document));

                allowing(document).createElementNS(with(any(String.class)), with(any(String.class)));
                will(returnValue(element));
                allowing(document).getDocumentElement();
                will(returnValue(null));
                allowing(document).appendChild(element);
                one(document).getImplementation();
                will(returnValue(implementation));
                allowing(document).createTextNode(with(any(String.class)));
                will(returnValue(null));

                one(element).setPrefix(with(any(String.class)));
                allowing(element).hasAttributes();
                will(returnValue(false));
                allowing(element).getParentNode();
                will(returnValue(null));
                allowing(element).setAttributeNS(with(any(String.class)), with(any(String.class)), with(any(String.class)));
                allowing(element).setIdAttributeNS(with(any(String.class)), with(any(String.class)), with(any(Boolean.class)));
                allowing(element).getOwnerDocument();
                will(returnValue(document));
                allowing(element).getNamespaceURI();
                will(returnValue("NamespaceURI"));
                allowing(element).getLocalName();
                will(returnValue("LocalName"));
                allowing(element).getPrefix();
                will(returnValue("Prefix"));
                allowing(element).appendChild(element);
                allowing(element).appendChild(null);
                allowing(element).setPrefix(with(any(String.class)));

                one(implementation).getFeature(with(any(String.class)), with(any(String.class)));
                will(returnValue(implementationLS));
                one(implementationLS).createLSSerializer();
                will(returnValue(serializer));
                one(implementationLS).createLSOutput();
                will(returnValue(serializerOut));

                one(serializer).setFilter(with(any(LSSerializerFilter.class)));
                one(serializer).write(with(any(Element.class)), with(any(LSOutput.class)));
                one(serializerOut).setCharacterStream(with(any(Writer.class)));

                one(cache).put(with(any(String.class)), with(any(ForwardRequestInfo.class)));
                allowing(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecConfig).getSameSiteCookie();
                allowing(webAppSecConfig).createReferrerURLCookieHandler();

            }
        });

        initiator = new Initiator(ssoService);
    }

    @Before
    public void before() {
        Configuration.setParserPool(parserPool);
        listSingleSignOnServices.clear();
        listSingleSignOnServices.add(singleSignOnService);
    }

    @AfterClass
    public static void tearDown() {
        BasicMessageContextBuilder.setInstance(instance);
        mockery.assertIsSatisfied();
        outputMgr.trace("*=all=disabled");
    }

    @Test
    public void nada() {}

    @Test
    public void testForwardRequestToSamlIdp() {
        mockery.checking(new Expectations() {
            {
                allowing(request).getAttribute("FormLogoutExitPage");
                will(returnValue(null));
                allowing(request).setAttribute("SpSLOInProgress", "true"); // why no work?
                one(ssoConfig).getLoginPageURL();
                will(returnValue(null));
                one(basicMessageContext).getMetadataProvider();
                will(returnValue(metadataProvider));
                one(ssoConfig).isAuthnRequestsSigned();
                will(returnValue(false));

                allowing(ssoConfig).createSession();
                will(returnValue(with(any(Boolean.class))));

                allowing(request).getSession(true);
                will(returnValue(session));
            }
        });
        try {
            TAIResult result = initiator.forwardRequestToSamlIdp(request, response);
            assertTrue("The TAIResult must not be null.", result != null);
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }
}
