/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.context.SubjectManager;
//import com.ibm.ws.security.javaeesec.cdi.beans.BasicHttpAuthenticationMechanism;

import io.openliberty.security.jakartasec.handlers.wrappers.HttpAuthenticationMechanismHandlerWrapper;
import io.openliberty.security.jakartasec.handlers.wrappers.MultiHttpAuthenticationMechanismWrapper;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Testing the HttpAuthenticationMechanismHandlerImpl class which implements the
 * Jakarta Security 4.0 interface HttpAuthenticationMechanismHandler.
 *
 * NOTE: The implementation of validateRequest, cleanSubject and secureResponse are
 * identical, so have only comprehensively tested validateRequest.
 *
 */
public class HttpAuthenticationMechanismHandlerImplTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpMessageContext httpMessageContext;
    private CDI<?> cdi;
    private MultiHttpAuthenticationMechanism multiHttpAuthenticationMechanism;
    private HttpAuthenticationMechanism httpAuthenticationMechanism;

    private final SubjectManager subjectManager = new SubjectManager();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        cdi = mockery.mock(CDI.class);
        request = mockery.mock(HttpServletRequest.class);
        response = mockery.mock(HttpServletResponse.class);
        httpMessageContext = mockery.mock(HttpMessageContext.class);
        multiHttpAuthenticationMechanism = mockery.mock(MultiHttpAuthenticationMechanism.class);
        httpAuthenticationMechanism = mockery.mock(HttpAuthenticationMechanism.class);
    }

    @After
    public void tearDown() throws Exception {
        subjectManager.clearSubjects();
        mockery.assertIsSatisfied();
    }

    /**
     * Test the happy case scenario, just ensuring
     */
    @Test
    public void testValidateRequest_success() {
        // given ...
        AuthenticationStatus authenticationStatus = AuthenticationStatus.NOT_DONE;

        // although a subclass, it only overrides methods for mocking
        HttpAuthenticationMechanismHandlerWrapper mechanism = new HttpAuthenticationMechanismHandlerWrapper(cdi) {
            @Override
            protected MultiHttpAuthenticationMechanism getHighestPriorityHttpAuthenticationMechanism() {
                return multiHttpAuthenticationMechanism;
            }
        };

        // all expectations of actions on mocked objects
        try {

            mockery.checking(new Expectations() {
                {
                    allowing(multiHttpAuthenticationMechanism).validateRequest(with(any(HttpServletRequest.class)),
                                                                               with(any(HttpServletResponse.class)),
                                                                               with(any(HttpMessageContext.class)));
                    will(returnValue(AuthenticationStatus.SUCCESS));
                }
            });

            // when ...
            authenticationStatus = mechanism.validateRequest(request, response, httpMessageContext);
        } catch (Exception e) {
            fail("Unexpected Exception " + e.getMessage() + " thrown.");
        }

        // then ...
        assertEquals("The AuthenticationStatus must be SUCCESS.", AuthenticationStatus.SUCCESS, authenticationStatus);
    }

    /**
     * Ensure that AuthenticationException is propagated when thrown by the mechanism
     */
    @Test(expected = AuthenticationException.class)
    public void testValidateRequest_authentication_exception() throws AuthenticationException {
        // given ...

        // although a subclass, it only overrides methods for mocking
        HttpAuthenticationMechanismHandlerWrapper mechanism = new HttpAuthenticationMechanismHandlerWrapper(cdi) {
            @Override
            protected MultiHttpAuthenticationMechanism getHighestPriorityHttpAuthenticationMechanism() {
                return multiHttpAuthenticationMechanism;
            }
        };

        // all expectations of actions on mocked objects
        mockery.checking(new Expectations() {
            {
                oneOf(multiHttpAuthenticationMechanism).validateRequest(with(any(HttpServletRequest.class)),
                                                                        with(any(HttpServletResponse.class)),
                                                                        with(any(HttpMessageContext.class)));
                will(throwException(new AuthenticationException()));
            }
        });

        // when ...
        mechanism.validateRequest(request, response, httpMessageContext);
        // then ... should throw AuthenticationException
    }

    /**
     * Ensure that the NOT_DONE status is returned if an HttpAuthenticationMechanism
     * cannot be found
     */
    @Test
    public void testValidateRequest_not_done() {
        // given ...
        AuthenticationStatus authenticationStatus = AuthenticationStatus.SUCCESS;

        // although a subclass, it only overrides methods for mocking
        // force getHighestPriorityAuthMechanism to return null to ensure correct status is returned
        HttpAuthenticationMechanismHandlerWrapper mechanism = new HttpAuthenticationMechanismHandlerWrapper(cdi) {
            @Override
            protected MultiHttpAuthenticationMechanism getHighestPriorityHttpAuthenticationMechanism() {
                return null;
            }
        };

        // when ...
        try {
            authenticationStatus = mechanism.validateRequest(request, response, httpMessageContext);
        } catch (Exception e) {
            fail("Unexpected Exception " + e.getMessage() + " thrown.");
        }

        // then ...
        assertEquals("The AuthenticationStatus must be NOT_DONE.", AuthenticationStatus.NOT_DONE, authenticationStatus);
    }

    @Test
    public void testGetHighestPriorityHttpAuthenticationMechanism_with_application_ham() throws Exception {
        // given ...

        final MultiHttpAuthenticationMechanism expectedValue = getMultiHttpAuthenticationMechanism("ApplicationHttpAuthenticationMechanism");

        try {
            HttpAuthenticationMechanismHandlerWrapper mechanism = new HttpAuthenticationMechanismHandlerWrapper(cdi) {
                @Override
                protected void scanAuthenticationMechanisms(Set<MultiHttpAuthenticationMechanism> multiHttpAuthenticationMechanisms) {
                    multiHttpAuthenticationMechanisms.clear();

                    multiHttpAuthenticationMechanisms.add(getMultiHttpAuthenticationMechanism("CustomFormAuthenticationMechanism"));
                    multiHttpAuthenticationMechanisms.add(getMultiHttpAuthenticationMechanism("OidcHttpAuthenticationMechanism"));
                    multiHttpAuthenticationMechanisms.add(getMultiHttpAuthenticationMechanism("FormAuthenticationMechanism"));
                    multiHttpAuthenticationMechanisms.add(getMultiHttpAuthenticationMechanism("BasicHttpAuthenticationMechanism"));
                    multiHttpAuthenticationMechanisms.add(expectedValue); // "ApplicationHttpAuthenticationMechanism"
                }
            };

            // when ...
            MultiHttpAuthenticationMechanism multiHttpAuthenticationMechanism = mechanism.getHighestPriorityHttpAuthenticationMechanism();

            // then ...
            assertEquals(multiHttpAuthenticationMechanism, expectedValue);
        } catch (Exception e) {
            fail("Unexpected Exception " + e.getMessage() + " thrown.");
        }
    }

    @Test
    public void testGetHighestPriorityHttpAuthenticationMechanism_in_built_hams() throws Exception {
        // given ...
        final MultiHttpAuthenticationMechanism expectedValue = getMultiHttpAuthenticationMechanism("OidcHttpAuthenticationMechanism");

        try {
            HttpAuthenticationMechanismHandlerWrapper mechanism = new HttpAuthenticationMechanismHandlerWrapper(cdi) {
                @Override
                protected void scanAuthenticationMechanisms(Set<MultiHttpAuthenticationMechanism> multiHttpAuthenticationMechanisms) {
                    multiHttpAuthenticationMechanisms.clear();

                    multiHttpAuthenticationMechanisms.add(getMultiHttpAuthenticationMechanism("CustomFormAuthenticationMechanism"));
                    multiHttpAuthenticationMechanisms.add(getMultiHttpAuthenticationMechanism("BasicHttpAuthenticationMechanism"));
                    multiHttpAuthenticationMechanisms.add(getMultiHttpAuthenticationMechanism("FormAuthenticationMechanism"));
                    multiHttpAuthenticationMechanisms.add(expectedValue);
                }
            };

            // when ...
            MultiHttpAuthenticationMechanism multiHttpAuthenticationMechanism = mechanism.getHighestPriorityHttpAuthenticationMechanism();

            // then ...
            assertEquals("Found: " + multiHttpAuthenticationMechanism.getSimpleName() + ", but expected: " + expectedValue.getSimpleName(), multiHttpAuthenticationMechanism,
                         expectedValue);
        } catch (Exception e) {
            fail("Unexpected Exception " + e.getMessage() + " thrown.");
        }
    }

    @Test(expected = AmbiguousResolutionException.class)
    public void testGetHighestPriorityHttpAuthenticationMechanism_ambiguous_resolution_exception() throws Exception {
        // given ...
        String[] hamNames = { "ApplicationAuthenticationMechanism1",
                              "ApplicationAuthenticationMechanism2",
                              "ApplicationAuthenticationMechanism3" };
        try {
            HttpAuthenticationMechanismHandlerWrapper mechanism = new HttpAuthenticationMechanismHandlerWrapper(cdi) {
                @Override
                protected void scanAuthenticationMechanisms(Set<MultiHttpAuthenticationMechanism> multiHttpAuthenticationMechanisms) {
                    multiHttpAuthenticationMechanisms.clear();

                    for (String name : hamNames) {
                        multiHttpAuthenticationMechanisms.add(getMultiHttpAuthenticationMechanism(name));

                    }
                }
            };

            // when ...
            MultiHttpAuthenticationMechanism multiHttpAuthenticationMechanism = mechanism.getHighestPriorityHttpAuthenticationMechanism();
            fail("Expected AbiguousResolutionException, but getHighestPriorityHttpAuthenticationMechanism() returned a value.");

            // then ...
        } catch (AmbiguousResolutionException ae) {
            assertTrue("Missing substring", ae.getMessage().contains("Unable to determine which HttpAuthenticationMechanism to handle request"));
            assertTrue("Missing one or more HAM names", Arrays.asList(ae.getMessage().split(" ")).containsAll(Arrays.asList(hamNames)));

            throw ae;
        } catch (Exception e) {
            fail("Unexpected Exception " + e.getMessage() + " thrown.");
        }
    }

    private MultiHttpAuthenticationMechanism getMultiHttpAuthenticationMechanism(String hamName) {
        return new MultiHttpAuthenticationMechanismWrapper(httpAuthenticationMechanism, hamName);
    }

    @Test
    public void testGetHighestPriorityAuthMechanism_no_hams_found() throws Exception {

        // although a subclass, it only overrides methods for mocking
        HttpAuthenticationMechanismHandlerWrapper mechanism = new HttpAuthenticationMechanismHandlerWrapper(cdi) {
            @Override
            protected void scanAuthenticationMechanisms(Set<MultiHttpAuthenticationMechanism> multiHttpAuthenticationMechanisms) {
                multiHttpAuthenticationMechanisms.clear();
            }
        };

        // all expectations of actions on mocked objects

        try {
            // when ...
            HttpAuthenticationMechanism ham = mechanism.getHighestPriorityHttpAuthenticationMechanism();

            // then ...
            assertTrue(ham == null);
        } catch (Exception e) {
            fail("Unexpected Exception " + e.getMessage() + " thrown.");
        }
    }
}
