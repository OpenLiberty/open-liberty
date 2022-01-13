/**
 *
 */
package test.jakarta.concurrency.jsp;

import static jakarta.enterprise.concurrent.ContextServiceDefinition.SECURITY;

import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorDefinition;
import jakarta.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;

@ContextServiceDefinition(name = "java:app/concurrent/securityClearedContextSvc",
                          cleared = SECURITY)

@ContextServiceDefinition(name = "java:app/concurrent/securityUnchangedContextSvc",
                          unchanged = SECURITY)

@ManagedExecutorDefinition(name = "java:app/concurrent/executor2",
                           context = "java:app/concurrent/securityUnchangedContextSvc")

/**
 * Empty Servlet to define Context Services for the JSP to use, since application.xml is not available
 */
@WebServlet("/NotUsed")
public class ConcurrencyJSPTestServlet extends FATServlet {

}
