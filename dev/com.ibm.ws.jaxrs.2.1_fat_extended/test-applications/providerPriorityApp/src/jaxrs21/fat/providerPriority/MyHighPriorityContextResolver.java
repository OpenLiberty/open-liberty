/**
 *
 */
package jaxrs21.fat.providerPriority;

import javax.annotation.Priority;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(10)
public class MyHighPriorityContextResolver implements ContextResolver<Version> {

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.ext.ContextResolver#getContext(java.lang.Class)
     */
    @Override
    public Version getContext(Class<?> arg0) {
        return new Version(2);
    }

}
