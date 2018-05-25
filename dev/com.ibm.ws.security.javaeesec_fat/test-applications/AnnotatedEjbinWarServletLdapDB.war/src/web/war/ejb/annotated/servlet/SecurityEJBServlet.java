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
package web.war.ejb.annotated.servlet;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.EJB;
import javax.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;

import web.ejb.jar.bean.SecurityEJBInterface;

/**
 * Security EJB servlet used for Pure Annotation tests - PureAnnA0xTest.
 */
@BasicAuthenticationMechanismDefinition(realmName = "ejbRealm")
@LdapIdentityStoreDefinition(
                             url = "ldap://127.0.0.1:10389/",
                             callerBaseDn = "",
                             callerSearchBase = "ou=users,o=ibm,c=us",
                             callerSearchScope = LdapIdentityStoreDefinition.LdapSearchScope.SUBTREE,
                             callerSearchFilter = "(&(objectclass=person)(uid=%s))",
                             callerNameAttribute = "uid",
                             groupNameAttribute = "cn",
                             groupSearchBase = "ou=groups,o=ibm,c=us",
                             groupSearchScope = LdapIdentityStoreDefinition.LdapSearchScope.SUBTREE,
                             groupSearchFilter = "(objectclass=groupofnames)",
                             groupMemberAttribute = "member",
                             bindDn = "uid=admin,ou=users,o=ibm,c=us",
                             bindDnPassword = "s3cur1ty",
                             priority = 100)
@SuppressWarnings("serial")
public class SecurityEJBServlet extends SecurityEJBBaseServlet {

    final String servletName;

    public SecurityEJBServlet() {
        this.servletName = "SecurityEJBServlet";
    }

    @Override
    String servletName() {
        return servletName;
    }

    //Inject Stateless/Singleton EJB interfaces for Pure Annotation Tests - PureAnnA0xTest.

    @EJB(beanName = "SecurityEJBA02Bean")
    private SecurityEJBInterface injectedEJB02;

    @EJB(beanName = "SecurityEJBA03Bean")
    private SecurityEJBInterface injectedEJB03;

    protected Map<String, Invoke> methodMap = new HashMap<String, Invoke>();

    @Override
    protected Map<String, SecurityEJBInterface> statelessBeans() {

        Map<String, SecurityEJBInterface> beanMap = new HashMap<String, SecurityEJBInterface>();
        if (beanMap.size() == 0) {
            beanMap.put("ejb02", injectedEJB02);
            beanMap.put("ejb03", injectedEJB03);
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
