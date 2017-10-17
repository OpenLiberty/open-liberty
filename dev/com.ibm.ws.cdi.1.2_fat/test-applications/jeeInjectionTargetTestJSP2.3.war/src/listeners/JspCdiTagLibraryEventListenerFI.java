package listeners;

import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

import beans.TestFieldInjectionApplicationScoped;
import beans.TestFieldInjectionDependentScoped;
import beans.TestFieldInjectionRequestScoped;
import beans.TestFieldInjectionSessionScoped;

public class JspCdiTagLibraryEventListenerFI implements ServletRequestListener {

    @Inject
    TestFieldInjectionDependentScoped fieldInjection;
    @Inject
    TestFieldInjectionRequestScoped fieldInjectionRequest;
    @Inject
    TestFieldInjectionApplicationScoped fieldInjectionApplication;
    @Inject
    TestFieldInjectionSessionScoped fieldInjectionSession;

    static public final String ATTRIBUTE_NAME = "JspCdiTagLibraryEventListenerFI";

    private int valueFI, valueFIRequest, valueFIApplication, valueFISession = 0;
    private String response = generateResponse();

    @Override
    public void requestDestroyed(ServletRequestEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void requestInitialized(ServletRequestEvent arg0) {
        ServletRequest req = arg0.getServletRequest();
        //Checking what jsp page was requested to avoid incrementing the index when it is another page the requested
        if (((HttpServletRequest) req).getRequestURI().toString().equals("/TestJSP2.3/TagLibraryEventListenerFI.jsp"))
            if ("true".equals(req.getParameter("increment"))) {
                valueFI = fieldInjection.incrementAndGetIndex();
                valueFIRequest = fieldInjectionRequest.incrementAndGetIndex();
                valueFIApplication = fieldInjectionApplication.incrementAndGetIndex();
                valueFISession = fieldInjectionSession.incrementAndGetIndex();
                response = generateResponse();
                req.setAttribute(JspCdiTagLibraryEventListenerFI.ATTRIBUTE_NAME, response);
            } else
                req.setAttribute(JspCdiTagLibraryEventListenerFI.ATTRIBUTE_NAME, response);
    }

    private String generateResponse() {
        String response = "<ul>\n";
        response += "<li>TestFieldInjection index: " + valueFI + "</li>\n";
        response += "<li>TestFieldInjectionRequest index: " + valueFIRequest + "</li>\n";
        response += "<li>TestFieldInjectionApplication index: " + valueFIApplication + "</li>\n";
        response += "<li>TestFieldInjectionSession index: " + valueFISession + "</li>\n";
        response += "</ul>";

        return response;
    }

}
