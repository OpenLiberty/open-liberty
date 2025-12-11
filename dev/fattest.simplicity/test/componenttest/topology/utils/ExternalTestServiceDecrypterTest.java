/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.topology.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import javax.net.ssl.HttpsURLConnection;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for ExternalTestServiceDecrypter
 */
public class ExternalTestServiceDecrypterTest {

    @Rule
    public TestName testName = new TestName();

    private AutoCloseable mockedObjets;

    @Before
    public void init() {
        mockedObjets = MockitoAnnotations.openMocks(this);
    }

    @After
    public void close() throws Exception {
        mockedObjets.close();
    }

    @Test
    public void testDecrypterForUnecryptedProperty() {
        String property = "clearTextProperty";

        String result = ExternalTestServiceDecrypter.decrypt(property);

        assertEquals(property, result);
    }

    @Test
    public void testDerypterFailsFastAccessToken() {
        String property = "encrypted!fakeEncryptedString";

        try {
            String result = ExternalTestServiceDecrypter.decrypt(property);
            fail("Should not have been able to decrypt a property without an access token. But got: " + result);
        } catch (IllegalStateException e) {
            //expected
            System.out.println("Caught expected exception: " + e.getMessage());
            assertTrue(e.getMessage().startsWith("Missing Property called: 'global.ghe.access.token'"));
        } catch (Exception e) {
            fail("Should have caught IllegalStateException but instead got: " + e.getMessage());
        }
    }

    /**
     * Must be called in a try-with-resources block
     */
    public MockedStatic<ExternalTestService> failingExternalTestService() {
        // Mock the ExternalTestService.getService(serviceName) static method
        MockedStatic<ExternalTestService> mockExternalTestService = Mockito.mockStatic(ExternalTestService.class);
        mockExternalTestService.when(() -> ExternalTestService.getService(anyString()))
                        .thenThrow(new Exception("Test failure"));
        return mockExternalTestService;
    }

    @Test
    @Ignore("Manual test only: test takes ~7 minutes to run due to sleeps. Also exhauses the retries and will make all the other tests fail.")
    public void testDecrytperRetriesOnFailingService() throws Exception {
        System.setProperty("global.ghe.access.token", "fake-token-123");

        String property = "encrypted!fakeEncryptedString";

        try (MockedStatic<ExternalTestService> service = failingExternalTestService()) {
            String result = ExternalTestServiceDecrypter.decrypt(property);
            fail("Should not have been able to decrypt a property with failing external test service. But got: " + result);
        } catch (RuntimeException e) {
            //expected
            System.out.println("Caught expected exception: " + e.getMessage());
            assertTrue(e.getMessage().startsWith("Could not get a liberty-properties-decrypter service"));
        } catch (Exception e) {
            fail("Should have caught RuntimeException but instead got: " + e.getMessage());
        } finally {
            System.clearProperty("global.ghe.access.token");
        }
    }

    /**
     * Must be called in a try-with-resources block
     */
    public MockedStatic<ExternalTestService> exampleExternalTestService() {
        // Create a mocked instance
        ExternalTestService instance = Mockito.mock(ExternalTestService.class);
        //Field methods
        when(instance.getAddress()).thenReturn("example.com");
        when(instance.getPort()).thenReturn(65535);
        when(instance.getServiceName()).thenReturn(testName.getMethodName());

        // Mock the ExternalTestService.getService(serviceName) static method
        MockedStatic<ExternalTestService> mockExternalTestService = Mockito.mockStatic(ExternalTestService.class);
        mockExternalTestService.when(() -> ExternalTestService.getService(anyString()))
                        .thenReturn(instance);
        return mockExternalTestService;
    }

    @Test
    @Ignore("Manual test only: Takes ~1 minute to execute due to connection timeouts and retries.")
    public void testDecrypterRetriesOnFailingConnection() throws Exception {
        System.setProperty("global.ghe.access.token", "fake-token-123");

        String property = "encrypted!fakeEncryptedString";

        try (MockedStatic<ExternalTestService> service = exampleExternalTestService()) {
            String result = ExternalTestServiceDecrypter.decrypt(property);
            fail("Should not have been able to decrypt a property with failing connections to service. But got: " + result);
        } catch (RuntimeException e) {
            //expected
            System.out.println("Caught expected exception: " + e.getMessage());
            assertTrue(e.getMessage().startsWith("Unable to decrypt the provided property."));
        } catch (Exception e) {
            fail("Should have caught RuntimeException but instead got: " + e.getMessage());
        } finally {
            System.clearProperty("global.ghe.access.token");
        }
    }

    /**
     * Must be called in a try-with-resources block
     */
    public MockedConstruction<HttpsRequest> getRejectedRequest(int responseCode) {
        return Mockito.mockConstruction(HttpsRequest.class, (mock, context) -> {
            when(mock.allowInsecure()).thenReturn(mock);
            when(mock.timeout(anyInt())).thenReturn(mock);
            when(mock.expectCode(anyInt())).thenReturn(mock);
            when(mock.silent()).thenReturn(mock);

            when(mock.run(any())).thenReturn("unexpectedResult");
            when(mock.getResponseCode()).thenReturn(responseCode);
        });
    }

    @Test
    public void testDecrypterFailsFastRejectedAccessToken() throws Exception {
        System.setProperty("global.ghe.access.token", "fake-token-123");

        String property = "encrypted!fakeEncryptedString";

        try (MockedStatic<ExternalTestService> service = exampleExternalTestService()) {
            try (MockedConstruction<HttpsRequest> request = getRejectedRequest(HttpsURLConnection.HTTP_UNAUTHORIZED)) {
                String result = ExternalTestServiceDecrypter.decrypt(property);
                fail("Should not have been able to decrypt a property with an unauthorized error code. But got: " + result);
            } catch (IllegalStateException e) {
                //expected
                System.out.println("Caught expected exception: " + e.getMessage());
                assertTrue(e.getMessage().contains("is not recognized by github.ibm.com"));
            } catch (Exception e) {
                fail("Should have caught IllegalStateException but instead got: " + e.getMessage());

            }

            try (MockedConstruction<HttpsRequest> request = getRejectedRequest(HttpsURLConnection.HTTP_FORBIDDEN)) {
                String result = ExternalTestServiceDecrypter.decrypt(property);
                fail("Should not have been able to decrypt a property with a forbidden error code. But got: " + result);
            } catch (IllegalStateException e) {
                //expected
                System.out.println("Caught expected exception: " + e.getMessage());
                assertTrue(e.getMessage().contains("is not able to be access organisation data"));
            } catch (Exception e) {
                fail("Should have caught IllegalStateException but instead got: " + e.getMessage());
            }
        } finally {
            System.clearProperty("global.ghe.access.token");
        }
    }

    /**
     * Must be called in a try-with-resources block
     */
    public MockedConstruction<HttpsRequest> getAcceptedResponse(String expectedResult) {
        return Mockito.mockConstruction(HttpsRequest.class, (mock, context) -> {
            when(mock.allowInsecure()).thenReturn(mock);
            when(mock.timeout(anyInt())).thenReturn(mock);
            when(mock.expectCode(anyInt())).thenReturn(mock);
            when(mock.silent()).thenReturn(mock);

            when(mock.run(any())).thenReturn(expectedResult);
            when(mock.getResponseCode()).thenReturn(HttpsURLConnection.HTTP_OK);
        });
    }

    @Test
    public void testDecrypterSuccessful() throws Exception {
        System.setProperty("global.ghe.access.token", "fake-token-123");

        String property = "encrypted!fakeEncryptedString";
        String expected = "fakeDecryptedString";

        try (MockedStatic<ExternalTestService> service = exampleExternalTestService()) {
            try (MockedConstruction<HttpsRequest> request = getAcceptedResponse(expected)) {
                String actual = ExternalTestServiceDecrypter.decrypt(property);
                assertEquals(expected, actual);
            } catch (Exception e) {
                fail("Should have been able to decrypt property. But instead got: " + e.getMessage());
            }
        } finally {
            System.clearProperty("global.ghe.access.token");
        }
    }
}
