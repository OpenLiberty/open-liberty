package com.ibm.ws.cdi.test.session.destroy;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

@WebListener
public class TestHttpSessionListener implements HttpSessionListener {

    @Inject
    private BeanManager beanManager;

    @Inject
    private SimpleSessionBean simpleBean;

    private static HttpSession timeoutSession = null;
    private static HttpSession invalidateSession = null;

    private static Map<String,String> results = new HashMap<String,String>();

    public static String getResults() {
        StringBuilder sb = new StringBuilder();
        if (timeoutSession != null) {
            sb.append("Timeout Session - ");
            sb.append(results.get(timeoutSession.getId()));
            sb.append(System.lineSeparator());
        }
        if (invalidateSession != null) {
            sb.append("Invalidate Session - ");
            sb.append(results.get(invalidateSession.getId()));
        }
        return sb.toString();
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        String msg = "session created: " + checkSessionContextActive() + " ";
        System.out.println(msg);

        String hs = se.getSession().getId();

        if (results.containsKey(hs)) {
            String newResult = results.get(hs);
            newResult += msg;
            results.put(hs,newResult);
        } else {
            results.put(hs,msg);
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        String msg = "session destroyed: " + checkSessionContextActive() + " ";
        System.out.println(msg);
        
        String hs = se.getSession().getId();

        if (results.containsKey(hs)) {
            String newResult = results.get(hs);
            newResult += msg;
            results.put(hs,newResult);
        } else {
            results.put(hs,msg);
        }
    }

    private String checkSessionContextActive() throws IllegalStateException {
        try {
            if (!beanManager.getContext(SessionScoped.class).isActive()) {
                return " context is not active";
            }
            if (simpleBean == null) {
                return " simple bean is null";
            }
            simpleBean.getID();
        } catch (Exception e) {
            return " simple bean invocation failed " + e.getMessage();
        }

        return "true";
    }

    public static void setInvalidateSession(HttpSession hs) {
        System.out.println("registered invalidate session - " + hs.getId());
        invalidateSession = hs;
    }

    public static void setTimeoutSession(HttpSession hs) {
        System.out.println("registered timeout session - " + hs.getId());
        timeoutSession = hs;
    }

}
