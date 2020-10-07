/**
 * 
 */
package io.openliberty.microprofile.opentracing.internal.testing.mpOpenTracing;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.opentracing.Traced;

/**
 * POJO to test Traced annotation.
 */
@ApplicationScoped
@Traced
public class POJO {
    /**
     * Method that we expect to be Traced implicitly.
     */
    public void annotatedClassMethodImplicitlyTraced() {
        System.out.println("Called annotatedClassMethodImplicitlyTraced");
    }
}
