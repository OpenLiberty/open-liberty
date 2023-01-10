/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.subjectcast;

import java.io.IOException;
import java.security.Principal;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;

import io.openliberty.jcache.internal.fat.loginmodule.CustomPrincipal;
import io.openliberty.jcache.internal.fat.loginmodule.CustomPrivateCredential;
import io.openliberty.jcache.internal.fat.loginmodule.CustomPublicCredential;

/**
 * Servlet used to test casting of custom principals and credentials retrieved from the Subject.
 * The Subject can either be retrieved directly from the local server instance, or may also be
 * retrieved from a distributed JCache. When retrieved from the distributed JCache, casting
 * is prone to {@link ClassCastException}s when the libraries used by the application and the
 * local JCache provider are not the same.
 */
public class SubjectCastServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        StringBuffer sb = new StringBuffer();

        /*
         * >>>>> Copied output from BasicAuthServlet so we can use BasicAuthServletClient.
         *
         * This is here so we can use the BasicAuthServletClient with this application.
         */
        sb.append("ServletName: SubjectCastServlet").append("\n");

        sb.append("getAuthType: ").append(request.getAuthType()).append("\n");
        sb.append("getRemoteUser: ").append(request.getRemoteUser()).append("\n");
        sb.append("getUserPrincipal: ").append(request.getUserPrincipal()).append("\n");
        if (request.getUserPrincipal() != null) {
            sb.append("getUserPrincipal().getName(): "
                      + request.getUserPrincipal().getName()).append("\n");
        }
        sb.append("isUserInRole(Employee): "
                  + request.isUserInRole("Employee")).append("\n");
        sb.append("isUserInRole(Manager): " + request.isUserInRole("Manager")).append("\n");
        String role = request.getParameter("role");
        if (role == null) {
            sb.append("You can customize the isUserInRole call with the follow paramter: ?role=name").append("\n");
        }
        sb.append("isUserInRole(" + role + "): " + request.isUserInRole(role)).append("\n");
        /*
         * <<<<< END of copied output.
         */

        Subject subject = null;
        try {
            subject = WSSubject.getCallerSubject();
        } catch (WSSecurityException e) {
            sb.append("Unable to retrieve caller subject. Error: ").append(e.getMessage());
        }

        sb.append("callerSubject: ").append(subject).append("\n");

        if (subject != null) {
            for (Principal principal : subject.getPrincipals()) {
                if (principal.getClass().getName().equals(CustomPrincipal.class.getName())) {
                    try {
                        CustomPrincipal principal2 = (CustomPrincipal) principal;
                        sb.append("Successfully cast CustomPrincipal").append("\n");
                    } catch (ClassCastException e) {
                        sb.append("Error casting CustomPrincipal: ").append(principal).append("\n");
                    }
                }
            }

            for (Object privateCred : subject.getPrivateCredentials()) {
                if (privateCred.getClass().getName().equals(CustomPrivateCredential.class.getName())) {
                    try {
                        CustomPrivateCredential privateCred2 = (CustomPrivateCredential) privateCred;
                        sb.append("Successfully cast CustomPrivateCredential").append("\n");
                    } catch (ClassCastException e) {
                        sb.append("Error casting CustomPrivateCredential: ").append(privateCred).append("\n");
                    }
                }
            }

            for (Object publicCred : subject.getPublicCredentials()) {
                if (publicCred.getClass().getName().equals(CustomPublicCredential.class.getName())) {
                    try {
                        CustomPublicCredential publicCred2 = (CustomPublicCredential) publicCred;
                        sb.append("Successfully cast CustomPublicCredential");
                    } catch (ClassCastException e) {
                        sb.append("Error casting CustomPublicCredential: ").append(publicCred).append("\n");
                    }
                }
            }
        }

        response.getWriter().write(sb.toString());
        response.getWriter().flush();
        response.getWriter().close();
    }
}
