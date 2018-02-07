/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.jar.base;

import java.security.Principal;
import java.util.Set;
import java.util.logging.Logger;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.security.enterprise.SecurityContext;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.IdentityStoreHandler;

/**
 * Base servlet which the JASPI test servlets extend.
 */
public abstract class FlexibleBaseServlet extends FlexibleBaseNoJavaEESecServlet {
    @Inject
    private SecurityContext securityContext;

    private static final long serialVersionUID = 1L;
    private static Logger log = Logger.getLogger(FlexibleBaseServlet.class.getName());

    public FlexibleBaseServlet(String servletName) {
        super(servletName);
    }

    public FlexibleBaseServlet(String servletName, String moduleName) {
        super(servletName, moduleName);
    }

    public class WriteRolesStepManagerEmployee implements BaseServletStep {

        @Override
        public void invoke(BaseServletParms p) throws Exception {

            writeLine(p.getBuffer(), "isUserInRole(Manager): "
                                     + p.getRequest().isUserInRole("Manager"));
            writeLine(p.getBuffer(), "isUserInRole(Manager): " + p.getRequest().isUserInRole("Manager"));
            String role = p.getRequest().getParameter("role");
            if (role == null) {
                writeLine(p.getBuffer(), "You can customize the isUserInRole call with the follow paramter: ?role=name");
            }
            writeLine(p.getBuffer(), "isUserInRole(" + role + "): " + p.getRequest().isUserInRole(role));
        }

    }

    public class WriteSecurityContextStep implements BaseServletStep {

        @Override
        public void invoke(BaseServletParms p) throws Exception {
            writeLine(p.getBuffer(), "**************WriteSecurityContextStep****************");
            writeLine(p.getBuffer(), "securityContext.isCallerInRole(javaeesec_basic): "
                                     + securityContext.isCallerInRole("javaeesec_basic"));
            writeLine(p.getBuffer(), "securityContext.isCallerInRole(javaeesec_form): " + securityContext.isCallerInRole("javaeesec_form"));
            String role = p.getRequest().getParameter("role");
            if (role != null) {
                writeLine(p.getBuffer(), "securityContext.isCallerInRole(" + role + "): " + securityContext.isCallerInRole(role));
            }

            // look for principals by type if type passed in
            String type = p.getRequest().getParameter("type");
            if (type != null) {
                if (type.equals("Principal")) {
                    Set<Principal> principals = securityContext.getPrincipalsByType(Principal.class);
                    writeLine(p.getBuffer(), "securityContext.GetPrincipalsByType number of principals: " + principals.size());
                }
            }

            // check to see if user has access to a resource
            String resource = p.getRequest().getParameter("resource");
            String methods = p.getRequest().getParameter("methods");
            if (resource != null) {
                if (methods != null) {
                    String[] servletMethods = methods.split(",");
                    if (servletMethods.length == 1) {
                        writeLine(p.getBuffer(), "securityContext.hasAccessToWebResource(" + resource + "," + servletMethods[0] + "): "
                                                 + securityContext.hasAccessToWebResource(resource, servletMethods[0]));
                    }
                    if (servletMethods.length == 2) {
                        writeLine(p.getBuffer(), "securityContext.hasAccessToWebResource(" + resource + "," + servletMethods[0] + "," + servletMethods[1] + "): "
                                                 + securityContext.hasAccessToWebResource(resource, servletMethods[0], servletMethods[1]));
                    }
                } else {
                    writeLine(p.getBuffer(), "securityContext.hasAccessToWebResource(" + resource + ",): "
                                             + securityContext.hasAccessToWebResource(resource));
                }
            }
            writeLine(p.getBuffer(), "securityContext.getCallerPrincipal(): " + securityContext.getCallerPrincipal());

            if (securityContext.getCallerPrincipal() != null) {
                writeLine(p.getBuffer(), "securityContext.getCallerPrincipal().getName(): "
                                         + securityContext.getCallerPrincipal().getName());
            }

        }

    }

    public class WriteSecurityContextStepDeclare01Role implements BaseServletStep {

        @Override
        public void invoke(BaseServletParms p) throws Exception {
            writeLine(p.getBuffer(), "**************WriteSecurityContextStep****************");
            writeLine(p.getBuffer(), "securityContext.isCallerInRole(DeclaredRole01): "
                                     + securityContext.isCallerInRole("DeclaredRole01"));
            writeLine(p.getBuffer(), "securityContext.isCallerInRole(DeclaredRole01): " + securityContext.isCallerInRole("DeclaredRole01"));
            String role = p.getRequest().getParameter("role");
            if (role != null) {
                writeLine(p.getBuffer(), "securityContext.isCallerInRole(" + role + "): " + securityContext.isCallerInRole(role));
            }

            // look for principals by type if type passed in
            String type = p.getRequest().getParameter("type");
            if (type != null) {
                if (type.equals("Principal")) {
                    Set<Principal> principals = securityContext.getPrincipalsByType(Principal.class);
                    writeLine(p.getBuffer(), "securityContext.GetPrincipalsByType number of principals: " + principals.size());
                }
            }

            // check to see if user has access to a resource
            String resource = p.getRequest().getParameter("resource");
            String methods = p.getRequest().getParameter("methods");
            if (resource != null) {
                if (methods != null) {
                    String[] servletMethods = methods.split(",");
                    if (servletMethods.length == 1) {
                        writeLine(p.getBuffer(), "securityContext.hasAccessToWebResource(" + resource + "," + servletMethods[0] + "): "
                                                 + securityContext.hasAccessToWebResource(resource, servletMethods[0]));
                    }
                    if (servletMethods.length == 2) {
                        writeLine(p.getBuffer(), "securityContext.hasAccessToWebResource(" + resource + "," + servletMethods[0] + "," + servletMethods[1] + "): "
                                                 + securityContext.hasAccessToWebResource(resource, servletMethods[0], servletMethods[1]));
                    }
                } else {
                    writeLine(p.getBuffer(), "securityContext.hasAccessToWebResource(" + resource + ",): "
                                             + securityContext.hasAccessToWebResource(resource));
                }
            }

            writeLine(p.getBuffer(), "securityContext.getCallerPrincipal(): " + securityContext.getCallerPrincipal());

            if (securityContext.getCallerPrincipal() != null) {
                writeLine(p.getBuffer(), "securityContext.getCallerPrincipal().getName(): "
                                         + securityContext.getCallerPrincipal().getName());
            }

        }
    }

    public class WriteJSR375Step implements BaseServletStep {

        @Override
        public void invoke(BaseServletParms p) throws Exception {
            // list avaiable identitystorehandler, identitystores, and httpauthemechs.
            Instance<IdentityStoreHandler> ishi = CDI.current().select(IdentityStoreHandler.class);
            String value;
            if (ishi != null && !ishi.isUnsatisfied() && !ishi.isAmbiguous()) {
                value = "1 exists: " + ishi.get().getClass();
            } else {
                value = "0 exists";
            }
            writeLine(p.getBuffer(), "IdentityStoreHandler : " + value);

            Instance<IdentityStore> isi = CDI.current().select(IdentityStore.class);
            if (isi != null) {
                StringBuffer sb = new StringBuffer();
                sb.append(" exists: [");
                int i = 0;
                for (IdentityStore is : isi) {
                    sb.append(is.getClass()).append(", ");
                    i++;
                }
                sb.append("]");
                value = i + sb.toString();
            } else {
                value = "0 exists";
            }
            writeLine(p.getBuffer(), "IdentityStore : " + value);

            Instance<HttpAuthenticationMechanism> hami = CDI.current().select(HttpAuthenticationMechanism.class);
            if (hami != null) {
                StringBuffer sb = new StringBuffer();
                sb.append(" exists: [");
                int i = 0;
                for (HttpAuthenticationMechanism ham : hami) {
                    sb.append(skipProxyClass(ham.getClass()).toString()).append(", ");
                    i++;
                }
                sb.append("]");
                value = i + sb.toString();
            } else {
                value = "0 exists";
            }
            writeLine(p.getBuffer(), "HttpAuthenticationMechanism : " + value);
        }
    }

    private Class skipProxyClass(Class clz) {
        Class output = clz;
        while (output.toString().toLowerCase().contains("weld")) {
            output = output.getSuperclass();
        }
        return output;
    }
}
