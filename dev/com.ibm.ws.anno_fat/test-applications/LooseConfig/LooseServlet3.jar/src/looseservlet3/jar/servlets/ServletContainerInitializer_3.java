package looseservlet3.jar.servlets;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

@HandlesTypes(javax.servlet.Servlet.class)
public class ServletContainerInitializer_3 implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> setOfClassesInterestedIn, ServletContext context) throws ServletException {
        
        System.out.println("--- Servlet 3 CONTAINER INITIALIZER! ---");
        
        //going to add a context attribute to show the set of classes that were passed in
        if (setOfClassesInterestedIn != null) {
            
            String sciName = "SCI 3";
            context.setAttribute("Sci_3_RanMsg", sciName + " actually ran! ");   // Unique attribute for each SCI.  Intended to show that this SCI ran.
            context.setAttribute("SciLastOneToRun", "Last SCI to run is [" + sciName + "]!!! ");  // All SCIs set this attribute.  Value will only stick if this SCI runs last.

            String sciOrder = (String)(context.getAttribute("sciOrder"));
            context.setAttribute("sciOrder", (sciOrder == null ? "" : sciOrder)  + "3");
            
            // context.setRequestCharacterEncoding("KSC5601");
            // context.setResponseCharacterEncoding("KSC5601");
            context.setAttribute("SET_OF_SERVLETS_IN_APP", setOfClassesInterestedIn);
        } else {
            context.setAttribute("SET_OF_SERVLETS_IN_APP", "null");
        }
        System.out.println("--- Servlet 3 CONTAINER INITIALIZER! --- EXIT");
    }

}
