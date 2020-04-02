/**
 *
 */
package concurrent.cdi2.web;

import java.util.concurrent.ExecutorService;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Producer that allows injection of ManagedExecutorService.
 */
public class ExecutorProducer {
    @Produces
    @ApplicationScoped
    ExecutorService getDefaultExecutor() throws NamingException {
        return InitialContext.doLookup("java:comp/DefaultManagedExecutorService");
    }
}
