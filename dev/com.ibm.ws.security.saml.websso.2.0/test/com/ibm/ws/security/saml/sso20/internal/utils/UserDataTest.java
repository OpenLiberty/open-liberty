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
package com.ibm.ws.security.saml.sso20.internal.utils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.xml.Namespace;
import org.opensaml.xml.NamespaceManager;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.xml.sax.SAXException;

import com.ibm.ws.security.saml.error.SamlException;

/**
 *
 */
public class UserDataTest {
    public static final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static UserData userData;

    private final static DateTime date = new DateTime(2015, 6, 12, 15, 23, 0, 0);
    private final static String PROVIDED_ID = "_257f9d9e9fa14962c0803903a6ccad931245264310738";

    private static final Assertion assertion = mockery.mock(Assertion.class);
    private static Element element = mockery.mock(Element.class);
    private static Document document = mockery.mock(Document.class);
    private static DOMImplementation domImplementation = mockery.mock(DOMImplementation.class);
    private static DOMImplementationLS domImplementationLs = mockery.mock(DOMImplementationLS.class);
    private static final QName qName = mockery.mock(QName.class, "qNameAssertion");
    private static final NamespaceManager nsManager = mockery.mock(NamespaceManager.class, "nsManager"); //assertion.getNamespaceManager();

    @BeforeClass
    public static void setUp() throws SamlException, ParserConfigurationException, SAXException, IOException {
        final Set<Namespace> namespaces = new HashSet<Namespace>();
        mockery.checking(new Expectations() {
            {
                allowing(assertion).getNamespaceManager();//
                will(returnValue(nsManager));//
                allowing(nsManager).getAllNamespacesInSubtreeScope();//
                will(returnValue(namespaces));//
                allowing(element).cloneNode(true);//
                will(returnValue(element));//

                one(assertion).getDOM();
                will(returnValue(element));

                one(element).getOwnerDocument();
                will(returnValue(document));

                one(document).getImplementation();
                will(returnValue(domImplementation));

                one(domImplementation).getFeature("LS", "3.0");
                will(returnValue(domImplementationLs));

                ignoring(domImplementationLs);

                one(assertion).detach();
                one(assertion).getID();
                will(returnValue(PROVIDED_ID));

                one(assertion).getElementQName();
                will(returnValue(qName));

                one(assertion).getIssueInstant();
                will(returnValue(date));

                one(assertion).getOrderedChildren();
            }
        });

        userData = new UserData(assertion, PROVIDED_ID);
    }

    @Test
    public void getSamlTokenTest() {
        userData.getSamlToken();
    }

    @Test
    public void getAssertionTest() {
        userData.getAssertion();
    }

    @Test
    public void toStringTest() {
        userData.toString();
    }
}
