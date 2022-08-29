package io.openliberty.security.jakartasec.cdi.beans;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.javaeesec.properties.ModulePropertiesProvider;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

import io.openliberty.security.jakartasec.JakartaSec30Constants;
import io.openliberty.security.jakartasec.TestOpenIdAuthenticationMechanismDefinition;
import io.openliberty.security.oidcclientcore.client.Client;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class OidcHttpAuthenticationMechanismTest {

    private static final String REDIRECTION_URL = "authzEndPointUrlWithQuery";
    private static final ProviderAuthenticationResult REDIRECTION_PROVIDER_AUTH_RESULT = new ProviderAuthenticationResult(AuthResult.REDIRECT_TO_PROVIDER, HttpServletResponse.SC_OK, null, null, null, REDIRECTION_URL);
    private static final ProviderAuthenticationResult FAILURE_AUTH_RESULT = new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpMessageContext httpMessageContext;
    private Client client;
    private Subject clientSubject;
    @SuppressWarnings("rawtypes")
    private CDI cdi;
    private ModulePropertiesProvider mpp;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        cdi = mockery.mock(CDI.class);
        mpp = mockery.mock(ModulePropertiesProvider.class);
        request = mockery.mock(HttpServletRequest.class);
        response = mockery.mock(HttpServletResponse.class);
        httpMessageContext = mockery.mock(HttpMessageContext.class);
        client = mockery.mock(Client.class);
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    @Test
    public void testValidateRequest_authenticationRequest_redirects() throws Exception {
        OpenIdAuthenticationMechanismDefinition openIdAuthenticationMechanismDefinition = TestOpenIdAuthenticationMechanismDefinition.getInstanceofAnnotation(null);
        setModulePropertiesProvider(openIdAuthenticationMechanismDefinition);
        setHttpMessageContextExpectations(true);
        withoutJaspicSessionPrincipal();
        clientStartsFlow(REDIRECTION_PROVIDER_AUTH_RESULT);
        mechanismRedirectsTo(REDIRECTION_URL);
        OidcHttpAuthenticationMechanism mechanism = new TestOidcHttpAuthenticationMechanism();

        AuthenticationStatus authenticationStatus = mechanism.validateRequest(request, response, httpMessageContext);

        assertEquals("The AuthenticationStatus must be SEND_CONTINUE.", AuthenticationStatus.SEND_CONTINUE, authenticationStatus);
    }

    @Test
    public void testValidateRequest_authenticationRequest_fails() throws Exception {
        OpenIdAuthenticationMechanismDefinition openIdAuthenticationMechanismDefinition = TestOpenIdAuthenticationMechanismDefinition.getInstanceofAnnotation(null);
        setModulePropertiesProvider(openIdAuthenticationMechanismDefinition);
        setHttpMessageContextExpectations(true);
        withoutJaspicSessionPrincipal();
        clientStartsFlow(FAILURE_AUTH_RESULT);
        mechanismSetsResponseUnauthorized();
        OidcHttpAuthenticationMechanism mechanism = new TestOidcHttpAuthenticationMechanism();

        AuthenticationStatus authenticationStatus = mechanism.validateRequest(request, response, httpMessageContext);

        assertEquals("The AuthenticationStatus must be SEND_FAILURE.", AuthenticationStatus.SEND_FAILURE, authenticationStatus);
    }

    @SuppressWarnings("unchecked")
    private void setModulePropertiesProvider(final OpenIdAuthenticationMechanismDefinition openIdAuthenticationMechanismDefinition) {
        final Properties props = new Properties();
        props.put(JakartaSec30Constants.OIDC_ANNOTATION, openIdAuthenticationMechanismDefinition);
        final Instance<ModulePropertiesProvider> mppi = mockery.mock(Instance.class, "mppi");
        mockery.checking(new Expectations() {
            {
                one(cdi).select(ModulePropertiesProvider.class);
                will(returnValue(mppi));
                one(mppi).get();
                will(returnValue(mpp));
//                one(mpp).getAuthMechProperties(OidcHttpAuthenticationMechanism.class);
//                will(returnValue(props));
            }
        });
    }

    private void setHttpMessageContextExpectations(final boolean protectedResource) {
        mockery.checking(new Expectations() {
            {
                allowing(httpMessageContext).getClientSubject();
                will(returnValue(clientSubject));
                allowing(httpMessageContext).getRequest();
                will(returnValue(request));
                allowing(httpMessageContext).getResponse();
                will(returnValue(response));
                allowing(httpMessageContext).isProtected();
                will(returnValue(protectedResource));
            }
        });
    }

    private void withoutJaspicSessionPrincipal() {
        mockery.checking(new Expectations() {
            {
                one(request).getUserPrincipal();
                will(returnValue(null));
            }
        });
    }

    private void clientStartsFlow(ProviderAuthenticationResult providerAuthenticationResult) {
        mockery.checking(new Expectations() {
            {
                one(client).startFlow(request, response);
                will(returnValue(providerAuthenticationResult));
            }
        });
    }

    private void mechanismRedirectsTo(String location) {
        mockery.checking(new Expectations() {
            {
                one(httpMessageContext).redirect(location);
                will(returnValue(AuthenticationStatus.SEND_CONTINUE));
            }
        });
    }

    private void mechanismSetsResponseUnauthorized() {
        mockery.checking(new Expectations() {
            {
                one(httpMessageContext).responseUnauthorized();
                will(returnValue(AuthenticationStatus.SEND_FAILURE));
            }
        });
    }

    private class TestOidcHttpAuthenticationMechanism extends OidcHttpAuthenticationMechanism {

        @SuppressWarnings("rawtypes")
        @Override
        protected CDI getCDI() {
            return cdi;
        }

        @Override
        protected Client getClient(HttpServletRequest request) {
            return client;
        }

    }

}
