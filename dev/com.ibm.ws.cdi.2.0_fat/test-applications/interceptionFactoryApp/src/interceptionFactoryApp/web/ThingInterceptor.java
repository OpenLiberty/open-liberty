/**
 *
 */
package interceptionFactoryApp.web;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 *
 */
@Priority(Interceptor.Priority.APPLICATION + 1)
@ThingInterceptorBinding
@Interceptor
public class ThingInterceptor {

    @AroundInvoke
    public Object aroundInvoke(InvocationContext invocationContext) throws Exception {
        Intercepted.set();
        return invocationContext.proceed();
    }
}