/**
 *
 */
package jaxrs21.fat.providerPriority;

import javax.annotation.Priority;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(20)
public class MyLowPriorityContextResolver implements ContextResolver<Version> {

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.ext.ContextResolver#getContext(java.lang.Class)
     */
    @Override
    public Version getContext(Class<?> arg0) {
        return new Version(1);
    }

}
