/**
 *
 */
package com.ibm.ws.springboot.support.fat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class ErrorPage20Test extends ErrorPageBaseTest {

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.springboot.support.fat.AbstractSpringTests#getFeatures()
     */
    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-2.0", "servlet-4.0"));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.springboot.support.fat.AbstractSpringTests#getApplication()
     */
    @Override
    public String getApplication() {
        return SPRING_BOOT_20_APP_BASE;
    }

}
