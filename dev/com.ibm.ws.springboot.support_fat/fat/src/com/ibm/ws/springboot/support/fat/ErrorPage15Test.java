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
public class ErrorPage15Test extends ErrorPageBaseTest {

    @AfterClass
    public static void stopTestServer() throws Exception {
        if (!javaVersion.startsWith("1.")) {
            server.stopServer("Exception: Thrown on purpose for FAT test", "SRVE0777E: Exception thrown by application class", "CWWKC0265W");
        } else {
            server.stopServer("Exception: Thrown on purpose for FAT test", "SRVE0777E: Exception thrown by application class");
        }
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-1.5", "servlet-3.1"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_15_APP_BASE;
    }

}
