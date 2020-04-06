package testservletd.jar.listeners;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class Servlet_D_Listener implements ServletContextListener {
    
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        ServletContext context = servletContextEvent.getServletContext();
        String listenerName = "D";
        System.out.println("! ------------ LISTENER " + listenerName + " STARTED -----------");
        

        context.setAttribute("Listener_D_Ran_Msg", "Listener " + listenerName + " actually ran! ");   // Unique attribute for each listener.  Intended to show that this listener ran.
        
        String listenerOrderAttr = "listenerOrder";
        String listenerOrder = (String)(context.getAttribute(listenerOrderAttr));
        context.setAttribute(listenerOrderAttr, (listenerOrder == null ? "" : listenerOrder)  + listenerName);

        System.out.println("! ------------ LISTENER " + listenerName + " EXITED -----------");
    }

    public void contextDestroyed(ServletContextEvent sce) {
        // Ignore
    }
}
