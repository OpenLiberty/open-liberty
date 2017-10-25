/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package test;

/**
 *
 */
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

public class PlainExtension implements Extension {
    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
        System.out.println("PlainExtension: beginning the scanning process");
    }

    <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> pat) {
        System.out.println("PlainExtension: scanning type->" + pat.getAnnotatedType().getJavaClass().getName());
    }

    void afterBeanDiscovery(@Observes AfterBeanDiscovery abd) {
        System.out.println("PlainExtension: finished the scanning process");
    }

}
