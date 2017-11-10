/**
 *
 */
package interceptionFactoryApp.web;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InterceptionFactory;

/**
 *
 */
@RequestScoped
public class ThingProducer {
    @Produces
    public Thing thingProducer(InterceptionFactory<Thing> interceptionFactory) {
        interceptionFactory.configure().add(ThingInterceptorBinding.BindingLiteral.INSTANCE).filterMethods(m -> m.getJavaMember().getName().equals("hello"));
        return interceptionFactory.createInterceptedInstance(new Thing());
    }
}
