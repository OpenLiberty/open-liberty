/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.war.ejb.is.servlet2;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.EJB;
import javax.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;
import javax.servlet.annotation.WebServlet;

import web.ejb.jar.bean.SecurityEJBInterface;

/**
 * Security EJB servlet used for Pure Annotation tests - PureAnnA0xTest.
 */
@WebServlet("/SimpleServlet2")
@ServletSecurity(value = @HttpConstraint(EmptyRoleSemantic.DENY), httpMethodConstraints = { @HttpMethodConstraint(value = "GET", rolesAllowed = { "Manager", "DeclaredRole01" }) })
@BasicAuthenticationMechanismDefinition(realmName = "EJBMultipleIdentityStore2")

@SuppressWarnings("serial")
public class SecurityEJBServlet2 extends SecurityEJBBaseServlet2 {

    final String servletName;

    public SecurityEJBServlet2() {
        this.servletName = "SecurityEJBServlet2";
    }

    @Override
    String servletName() {
        return servletName;
    }

    //Inject Stateless/Singleton EJB interfaces for Pure Annotation Tests - PureAnnA0xTest.

    @EJB(beanName = "SecurityEJBA01Bean")
    private SecurityEJBInterface injectedEJB01;

    @EJB(beanName = "SecurityEJBA02Bean")
    private SecurityEJBInterface injectedEJB02;

    @EJB(beanName = "SecurityEJBA03Bean")
    private SecurityEJBInterface injectedEJB03;

    @EJB(beanName = "SecurityEJBA05Bean")
    private SecurityEJBInterface injectedEJB05;

    @EJB(beanName = "SecurityEJBA06Bean")
    private SecurityEJBInterface injectedEJB06;

    @EJB(beanName = "SecurityEJBA07Bean")
    private SecurityEJBInterface injectedEJB07;

    @EJB(beanName = "SecurityEJBA08Bean")
    private SecurityEJBInterface injectedEJB08;

    protected Map<String, Invoke> methodMap = new HashMap<String, Invoke>();

    @Override
    protected Map<String, SecurityEJBInterface> statelessBeans() {

        Map<String, SecurityEJBInterface> beanMap = new HashMap<String, SecurityEJBInterface>();
        if (beanMap.size() == 0) {
            beanMap.put("ejb01", injectedEJB01);
            beanMap.put("ejb02", injectedEJB02);
            beanMap.put("ejb03", injectedEJB03);
            beanMap.put("ejb05", injectedEJB05);
            beanMap.put("ejb06", injectedEJB06);
            beanMap.put("ejb07", injectedEJB07);
            beanMap.put("ejb08", injectedEJB08);
        }
        return beanMap;
    }

    @Override
    protected Map<String, String> statefulBeans() {

        Map<String, String> beanMap = new HashMap<String, String>();
        beanMap.put("ejb04", "java:app/SecurityEJB/SecurityEJBA04Bean");
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
