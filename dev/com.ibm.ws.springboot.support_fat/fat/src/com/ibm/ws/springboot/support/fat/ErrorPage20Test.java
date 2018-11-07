/**
 *
 */
package com.ibm.ws.springboot.support.fat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;

/**
 *
 */
public class ErrorPage20Test extends ErrorPageBaseTest {

    @AfterClass
    public static void stopTestServer() throws Exception {
        server.stopServer("Exception: Thrown on purpose for FAT test", "SRVE0777E: Exception thrown by application class");
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-2.0", "servlet-4.0"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_20_APP_BASE;
    }

}
