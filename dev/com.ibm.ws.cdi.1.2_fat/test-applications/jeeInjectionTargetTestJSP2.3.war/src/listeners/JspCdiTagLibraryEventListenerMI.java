package listeners;

import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

import beans.TestMethodInjectionApplicationScoped;
import beans.TestMethodInjectionDependentScoped;
import beans.TestMethodInjectionRequestScoped;
import beans.TestMethodInjectionSessionScoped;

public class JspCdiTagLibraryEventListenerMI implements ServletRequestListener {

    TestMethodInjectionDependentScoped methodInjection;
    TestMethodInjectionRequestScoped methodInjectionRequest;
    TestMethodInjectionApplicationScoped methodInjectionApplication;
    TestMethodInjectionSessionScoped methodInjectionSession;

    static public final String ATTRIBUTE_NAME = "JspCdiTagLibraryEventListenerMI";

    private int valueMI, valueMIRequest, valueMIApplication, valueMISession = 0;
    private String response = generateResponse();

    @Inject
    public void injectCorrespondingObjects(TestMethodInjectionDependentScoped methodInjection, TestMethodInjectionRequestScoped methodInjectionRequest,
                                           TestMethodInjectionApplicationScoped methodInjectionApplication,
                                           TestMethodInjectionSessionScoped methodInjectionSession) {
        this.methodInjection = methodInjection;
        this.methodInjectionRequest = methodInjectionRequest;
        this.methodInjectionApplication = methodInjectionApplication;
        this.methodInjectionSession = methodInjectionSession;
    }

    @Override
    public void requestDestroyed(ServletRequestEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void requestInitialized(ServletRequestEvent arg0) {
        ServletRequest req = arg0.getServletRequest();
        //Checking what jsp page was requested to avoid incrementing the index when it is another page the requested
        if (((HttpServletRequest) req).getRequestURI().toString().equals("/TestJSP2.3/TagLibraryEventListenerMI.jsp"))
            if ("true".equals(req.getParameter("increment"))) {
                valueMI = methodInjection.incrementAndGetIndex();
                valueMIRequest = methodInjectionRequest.incrementAndGetIndex();
                valueMIApplication = methodInjectionApplication.incrementAndGetIndex();
                valueMISession = methodInjectionSession.incrementAndGetIndex();
                response = generateResponse();
                req.setAttribute(JspCdiTagLibraryEventListenerMI.ATTRIBUTE_NAME, response);
            } else
                req.setAttribute(JspCdiTagLibraryEventListenerMI.ATTRIBUTE_NAME, response);
    }

    private String generateResponse() {
        String response = "<ul>\n";
        response += "<li>TestMethodInjection index: " + valueMI + "</li>\n";
        response += "<li>TestMethodInjectionRequest index: " + valueMIRequest + "</li>\n";
        response += "<li>TestMethodInjectionApplication index: " + valueMIApplication + "</li>\n";
        response += "<li>TestMethodInjectionSession index: " + valueMISession + "</li>\n";
        response += "</ul>";

        return response;
    }

}
