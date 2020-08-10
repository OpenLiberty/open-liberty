package web;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.EJB;

import com.ibm.ws.ejbcontainer.security.test.SecurityEJBInterface;

/**
 * Security EJB servlet used for tests with ejb-jar.xml deployment descriptor.
 */
@SuppressWarnings("serial")
public class SecurityEJBXMLNoAuthServlet extends SecurityEJBBaseServlet {

    final String servletName;

    public SecurityEJBXMLNoAuthServlet() {
        this.servletName = "SecurityEJBXMLServlet";
    }

    @Override
    String servletName() {
        return servletName;
    }

    //Inject EJB interfaces for ejb-jar (without annotation) tests - SecurityEJBX0xTest

    @EJB(beanName = "SecurityEJBX01Bean")
    private SecurityEJBInterface injectedEJBX01;

    protected Map<String, Invoke> methodMap = new HashMap<String, Invoke>();

    @Override
    protected Map<String, SecurityEJBInterface> statelessBeans() {

        Map<String, SecurityEJBInterface> beanMap = new HashMap<String, SecurityEJBInterface>();
        if (beanMap.size() == 0) {
            beanMap.put("ejbx01", injectedEJBX01);
        }
        return beanMap;
    }

    @Override
    protected Map<String, String> statefulBeans() {

        Map<String, String> beanMap = new HashMap<String, String>();
        return beanMap;
    }

    protected void buildMethods() {

        if (methodMap.size() == 0) {
            methodMap.put("checkAuthenticated", new Invoke() {
                @Override
                public String go(SecurityEJBInterface ejb) {
                    return ejb.checkAuthenticated();
                }
            });
            methodMap.put("permitAuthenticated", new Invoke() {
                @Override
                public String go(SecurityEJBInterface ejb) {
                    return ejb.permitAuthenticated();
                }
            });
        }
    }

    @Override
    protected void invokeEJBMethod(StringBuffer sb, String testInstance, String testMethod) {

        try {
            stateless.populate(statelessBeans());
            stateful.populate(statefulBeans());
            buildMethods();

            SecurityBean bean = lookup(testInstance);
            validateInstance(testInstance, bean);

            Invoke invokeMe = methodMap.get(testMethod);
            validateMethodWithArgs(testMethod, invokeMe);

            writeLine(sb, invokeMe.go(bean.get()));

        } catch (RuntimeException e) {
            if (e.getClass().getCanonicalName().equals("javax.ejb.EJBAccessException")) {
                writeLine(sb, "EJBAccessException: " + e.getMessage());
            } else {
                writeLine(sb, "RuntimeException: " + e.getMessage());
            }
        } catch (Exception e) {
            writeLine(sb, "Unexpected exception: " + e.toString());
        }
    }

    private SecurityBean lookup(String beanname) {
        SecurityBean b;
        b = stateless.lookup(beanname);
        if (b != null)
            return b;
        b = stateful.lookup(beanname);
        if (b != null)
            return b;
        throw new RuntimeException("No bean found on lookup");
    }

    private void validateInstance(String testInstanceKey, SecurityBean bean) {
        if (bean == null)
            throw new RuntimeException("Unrecognized EJB test instance name: " + testInstanceKey);
    }

    private void validateMethodWithArgs(String testMethodKey, Invoke invoker) {
        if (invoker == null)
            throw new RuntimeException("Unrecognized EJB test method key: " + testMethodKey);
    }

}
