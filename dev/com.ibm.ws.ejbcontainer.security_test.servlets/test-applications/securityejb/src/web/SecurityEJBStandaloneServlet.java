/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package web;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.EJB;

import com.ibm.ws.ejbcontainer.security.test.SecurityEJBInterface;

/**
 * Security EJB servlet used for standalone EJB tests.
 */
@SuppressWarnings("serial")
public class SecurityEJBStandaloneServlet extends SecurityEJBBaseServlet {

    final String servletName;

    public SecurityEJBStandaloneServlet() {
        this.servletName = "SecurityEJBStandaloneServlet";
    }

    @Override
    String servletName() {
        return servletName;
    }

    //Inject Stateless/Singleton EJB interfaces for standalone EJB tests.

    @EJB(lookup = "java:global/SecuritySAEJB/SecurityEJBA01Bean")
    private SecurityEJBInterface injectedEJB01;

    @EJB(lookup = "java:global/SecuritySAEJB/SecurityEJBA05Bean")
    private SecurityEJBInterface injectedEJB05;

    @EJB(lookup = "java:global/SecuritySAEJBM02/SecurityEJBM02Bean")
    private SecurityEJBInterface injectedEJBM02;

    @EJB(lookup = "java:global/SecuritySAEJBM07/SecurityEJBM07Bean")
    private SecurityEJBInterface injectedEJBM07;

    @EJB(lookup = "java:global/SecuritySAEJBX01/SecurityEJBX01Bean")
    private SecurityEJBInterface injectedEJBX01;

    @EJB(lookup = "java:global/SecuritySAEJBMC06/SecurityEJBMC06Bean")
    private SecurityEJBInterface injectedEJBMC06;

    protected Map<String, Invoke> methodMap = new HashMap<String, Invoke>();

    @Override
    protected Map<String, SecurityEJBInterface> statelessBeans() {

        Map<String, SecurityEJBInterface> beanMap = new HashMap<String, SecurityEJBInterface>();
        if (beanMap.size() == 0) {
            beanMap.put("ejb01", injectedEJB01);
            beanMap.put("ejb05", injectedEJB05);
            beanMap.put("ejbm02", injectedEJBM02);
            beanMap.put("ejbm07", injectedEJBM07);
            beanMap.put("ejbx01", injectedEJBX01);
            beanMap.put("ejbmc06", injectedEJBMC06);
        }
        return beanMap;
    }

    @Override
    protected Map<String, String> statefulBeans() {

        Map<String, String> beanMap = new HashMap<String, String>();
        beanMap.put("ejbm01", "java:global/SecuritySAEJBM01/SecurityEJBM01Bean");
        beanMap.put("ejbm08", "java:global/SecuritySAEJBM08/SecurityEJBM08Bean");
        beanMap.put("ejbx02", "java:global/SecuritySAEJBX02/SecurityEJBX02Bean");
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