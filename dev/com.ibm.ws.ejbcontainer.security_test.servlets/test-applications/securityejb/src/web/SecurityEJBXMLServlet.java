package web;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.EJB;

import com.ibm.ws.ejbcontainer.security.test.SecurityEJBInterface;

/**
 * Security EJB servlet used for tests with ejb-jar.xml deployment descriptor.
 */
@SuppressWarnings("serial")
public class SecurityEJBXMLServlet extends SecurityEJBBaseServlet {

    final String servletName;

    public SecurityEJBXMLServlet() {
        this.servletName = "SecurityEJBXMLServlet";
    }

    @Override
    String servletName() {
        return servletName;
    }

    //Inject EJB interfaces for ejb-jar (without annotation) tests - SecurityEJBX0xTest

    @EJB(beanName = "SecurityEJBX01Bean")
    private SecurityEJBInterface injectedEJBX01;

    @EJB(beanName = "SecurityEJBX01MappedStarStarBean")
    private SecurityEJBInterface injectedEJBX01Star;

    @EJB(beanName = "SecurityEJBX03Bean")
    private SecurityEJBInterface injectedEJBX03;

    //Inject EJB interfaces for ejb-jar with annotations tests -  SecurityEJBM0xTest

    @EJB(beanName = "SecurityEJBM02Bean")
    private SecurityEJBInterface injectedEJBM02;

    @EJB(beanName = "SecurityEJBM03Bean")
    private SecurityEJBInterface injectedEJBM03;

    @EJB(beanName = "SecurityEJBM05Bean")
    private SecurityEJBInterface injectedEJBM05;

    @EJB(beanName = "SecurityEJBM07Bean")
    private SecurityEJBInterface injectedEJBM07;

    @EJB(beanName = "SecurityEJBM09Bean")
    private SecurityEJBInterface injectedEJBM09;

    @EJB(beanName = "SecurityEJBM10Bean")
    private SecurityEJBInterface injectedEJBM10;

    protected Map<String, Invoke> methodMap = new HashMap<String, Invoke>();

    @Override
    protected Map<String, SecurityEJBInterface> statelessBeans() {

        Map<String, SecurityEJBInterface> beanMap = new HashMap<String, SecurityEJBInterface>();
        if (beanMap.size() == 0) {
            beanMap.put("ejbx01", injectedEJBX01);
            beanMap.put("ejbx01Star", injectedEJBX01Star);
            beanMap.put("ejbx03", injectedEJBX03);
            beanMap.put("ejbm02", injectedEJBM02);
            beanMap.put("ejbm03", injectedEJBM03);
            beanMap.put("ejbm05", injectedEJBM05);
            beanMap.put("ejbm07", injectedEJBM07);
            beanMap.put("ejbm09", injectedEJBM09);
            beanMap.put("ejbm10", injectedEJBM10);
        }
        return beanMap;
    }

    @Override
    protected Map<String, String> statefulBeans() {

        Map<String, String> beanMap = new HashMap<String, String>();
        beanMap.put("ejbm01", "java:app/SecurityEJBM01/SecurityEJBM01Bean");
        beanMap.put("ejbm01w", "java:module/SecurityEJBM01Bean");
        beanMap.put("ejbx02", "java:app/SecurityEJBX02/SecurityEJBX02Bean");
        beanMap.put("ejbx02w", "java:module/SecurityEJBX02Bean");
        beanMap.put("ejbm04", "java:app/SecurityEJBM04/SecurityEJBM04Bean");
        beanMap.put("ejbm08", "java:app/SecurityEJBM08/SecurityEJBM08Bean");
        beanMap.put("ejbm08w", "java:module/SecurityEJBM08Bean");
        return beanMap;
    }

    protected void buildMethods() {

        if (methodMap.size() == 0) {
            methodMap.put("denyAll", new Invoke() {
                @Override
                public String go(SecurityEJBInterface ejb) {
                    return ejb.denyAll();
                }
            });
            methodMap.put("denyAllwithParam", new Invoke() {
                @Override
                public String go(SecurityEJBInterface ejb) {
                    return ejb.denyAll("input string");
                }
            });
            methodMap.put("permitAll", new Invoke() {
                @Override
                public String go(SecurityEJBInterface ejb) {
                    return ejb.permitAll();
                }
            });
            methodMap.put("permitAllwithParam", new Invoke() {
                @Override
                public String go(SecurityEJBInterface ejb) {
                    return ejb.permitAll("input string");
                }
            });
            methodMap.put("manager", new Invoke() {
                @Override
                public String go(SecurityEJBInterface ejb) {
                    return ejb.manager();
                }
            });
            methodMap.put("managerwithParam", new Invoke() {
                @Override
                public String go(SecurityEJBInterface ejb) {
                    return ejb.manager("input string");
                }
            });
            methodMap.put("employee", new Invoke() {
                @Override
                public String go(SecurityEJBInterface ejb) {
                    return ejb.employee();
                }
            });
            methodMap.put("employeewithParam", new Invoke() {
                @Override
                public String go(SecurityEJBInterface ejb) {
                    return ejb.employee("input string");
                }
            });
            methodMap.put("employeeAndManager", new Invoke() {
                @Override
                public String go(SecurityEJBInterface ejb) {
                    return ejb.employeeAndManager();
                }
            });
            methodMap.put("employeeAndManagerwithParam", new Invoke() {
                @Override
                public String go(SecurityEJBInterface ejb) {
                    return ejb.employeeAndManager("input string");
                }
            });
            methodMap.put("employeeAndManagerwithInt", new Invoke() {
                @Override
                public String go(SecurityEJBInterface ejb) {
                    return ejb.employeeAndManager(3);
                }
            });
            methodMap.put("employeeAndManagerwithParams", new Invoke() {
                @Override
                public String go(SecurityEJBInterface ejb) {
                    return ejb.employeeAndManager("string1", "string2");
                }
            });
            methodMap.put("declareRoles01", new Invoke() {
                @Override
                public String go(SecurityEJBInterface ejb) {
                    return ejb.declareRoles01();
                }
            });
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
            methodMap.put("runAsClient", new Invoke() {
                @Override
                public String go(SecurityEJBInterface ejb) {
                    return ejb.runAsClient();
                }
            });
            methodMap.put("runAsSpecified", new Invoke() {
                @Override
                public String go(SecurityEJBInterface ejb) {
                    return ejb.runAsSpecified();
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
