/**
 *
 */
package builtinAnnoApp.web;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * Use this class to get the Bean where SomeRandomClass is injected. This allows us
 * to introspect that Bean.
 */
@RequestScoped
public class SomeRandomClassProducer {
    @Produces
    public SomeRandomClass getInstance(InjectionPoint injectionPoint) {
        return new SomeRandomClass(injectionPoint.getBean());
    }
}
