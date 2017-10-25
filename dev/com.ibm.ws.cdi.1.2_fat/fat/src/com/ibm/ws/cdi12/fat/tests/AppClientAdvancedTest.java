package com.ibm.ws.cdi12.fat.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import componenttest.custom.junit.runner.ClientOnly;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyClientFactory;

@ClientOnly
public class AppClientAdvancedTest {
    private final String testClientName = "cdiClientAdvanced";
    private final LibertyClient client = LibertyClientFactory.getLibertyClient(testClientName);

    /**
     * A more advanced test of CDI in the app client, which tests decorators, interceptors and event observers in the client container.
     * <p>
     * Test implementation details:
     * <ul>
     * <li>There are two implementations of the Greeter interface and we use qualifiers to select which one we want. We check both beans are called.</li>
     * <li>We have a decorator which decorates Greeters and we check that it modifies the return value correctly.</li>
     * <li>We have an interceptor which counts how often a @Countable method is called, we check the total is correct.</li>
     * <li>When a warning level is reached on the counter, it fires an event which is logged by an observer. We check for the observer's log message.</li>
     * </ul>
     */
    @Test
    public void testHelloAppClient() throws Exception {

        client.startClient();

        String featuresMessage = client.waitForStringInCopiedLog("CWWKF0034I", 0);
        assertNotNull("Did not receive features loaded message", featuresMessage);
        assertTrue("cdi-1.2 was not among the loaded features", featuresMessage.contains("cdi-1.2"));

        assertNotNull("Did not receive hello from decorated english beans. Decorator or bean qualifiers may have failed",
                      client.waitForStringInCopiedLog("Hello, I mean... Ahoy", 0));

        assertNotNull("Did not receive hello from decorated french beans. Decorator or bean qualifiers may have failed",
                      client.waitForStringInCopiedLog("Bonjour, I mean... Ahoy", 0));

        assertNotNull("Did not receive the correct observer log message. Observer or interceptor may have failed",
                      client.waitForStringInCopiedLog("Warning: 5 countable methods have been executed", 0));

        assertNotNull("Did not receive the correct final countable method execution count. Interceptor may have failed",
                      client.waitForStringInCopiedLog("There were 7 countable calls made", 0));

    }

}
