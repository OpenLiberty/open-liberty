/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.security.CustomRegistryException;
import com.ibm.websphere.security.EntryNotFoundException;
import com.ibm.websphere.security.PasswordCheckFailedException;
import com.ibm.websphere.security.Result;
import com.ibm.websphere.security.UserRegistry;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.wsspi.security.registry.RegistryHelper;

/**
 * Servlet to access UserRegistry APIs. Will retrieved an instance of the
 * current UserRegistry on each request.
 */
public class UserRegistryServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * Grab the effective UserRegistry instance by looking up (in an ugly way)
     * from the OSGis service registry.
     *
     * @param realmName realm name.
     *
     * @return UserRegistry instance, or null if can't find service.
     * @throws RegistryException
     */
    private UserRegistry getCurrentUserRegistry(String realmName) throws CustomRegistryException {
        try {
            return RegistryHelper.getUserRegistry(realmName);
        } catch (WSSecurityException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String convertResultToString(Result result) {
        boolean hasMore = result.hasMore();
        List list = result.getList();
        return "Result hasMore=" + hasMore + " " + list.toString();
    }

    /**
     * Handles method calls which is the core purpose of this servlet.
     * There are certain test flows where exceptions may be expected.
     * Allow those to occur and capture them such that they can be
     * consumed by tests.
     *
     * @param req
     * @param pw
     */
    private void handleMethodRequest(HttpServletRequest req, PrintWriter pw) throws Exception {
        String response = null;

        String method = req.getParameter("method");
        String realmName = req.getParameter("realmName");
        UserRegistry ur = getCurrentUserRegistry(realmName);
        try {
            if ("getRealm".equals(method)) {
                response = ur.getRealm();
            } else if ("checkPassword".equals(method)) {
                String userSecurityName = req.getParameter("userSecurityName");
                String password = req.getParameter("password");
                response = String.valueOf(ur.checkPassword(userSecurityName, password));
            } else if ("mapCertificate".equals(method)) {
                response = "mapCertificate is not supported via the servlet";
            } else if ("isValidUser".equals(method)) {
                String userSecurityName = req.getParameter("userSecurityName");
                response = String.valueOf(ur.isValidUser(userSecurityName));
            } else if ("getUsers".equals(method)) {
                String pattern = req.getParameter("pattern");
                int limit = Integer.valueOf(req.getParameter("limit"));
                response = convertResultToString(ur.getUsers(pattern, limit));
            } else if ("getUserDisplayName".equals(method)) {
                String userSecurityName = req.getParameter("userSecurityName");
                response = ur.getUserDisplayName(userSecurityName);
            } else if ("getUniqueUserId".equals(method)) {
                String userSecurityName = req.getParameter("userSecurityName");
                response = ur.getUniqueUserId(userSecurityName);
            } else if ("getUserSecurityName".equals(method)) {
                String uniqueUserId = req.getParameter("uniqueUserId");
                response = ur.getUserSecurityName(uniqueUserId);
            } else if ("isValidGroup".equals(method)) {
                String groupSecurityName = req.getParameter("groupSecurityName");
                response = String.valueOf(ur.isValidGroup(groupSecurityName));
            } else if ("getGroups".equals(method)) {
                String pattern = req.getParameter("pattern");
                int limit = Integer.valueOf(req.getParameter("limit"));
                response = convertResultToString(ur.getGroups(pattern, limit));
            } else if ("getGroupDisplayName".equals(method)) {
                String groupSecurityName = req.getParameter("groupSecurityName");
                response = ur.getGroupDisplayName(groupSecurityName);
            } else if ("getUniqueGroupId".equals(method)) {
                String groupSecurityName = req.getParameter("groupSecurityName");
                response = ur.getUniqueGroupId(groupSecurityName);
            } else if ("getGroupSecurityName".equals(method)) {
                String uniqueGroupId = req.getParameter("uniqueGroupId");
                response = ur.getGroupSecurityName(uniqueGroupId);
            } else if ("getUniqueGroupIdsForUser".equals(method)) {
                String uniqueUserId = req.getParameter("uniqueUserId");
                response = String.valueOf(ur.getUniqueGroupIds(uniqueUserId));
            } else if ("getGroupsForUser".equals(method)) {
                String userSecurityName = req.getParameter("userSecurityName");
                response = String.valueOf(ur.getGroupsForUser(userSecurityName));
            } else {
                pw.println("Usage: url?method=name&paramName=paramValue&...");
            }
        } catch (CustomRegistryException re) {
            response = re.toString();
        } catch (EntryNotFoundException enof) {
            response = enof.toString();
        } catch (NullPointerException npe) {
            response = npe.toString();
        } catch (RemoteException re) {
            response = re.toString();
        } catch (PasswordCheckFailedException pcfe) {
            response = pcfe.toString();
        }

        pw.println("Result from method: " + method);
        pw.println(response);
        pw.flush();
    }

    /**
     * {@inheritDoc} GET handles method requests and calls them against the
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();
        try {
            handleMethodRequest(req, pw);
        } catch (CustomRegistryException e) {
            e.printStackTrace(pw);
            pw.println("getCurrentUserRegistry exception message:");
            pw.println(e);
            pw.flush();
        } catch (Exception e) {
            pw.println("Unexpected Exception during processing:");
            e.printStackTrace(pw);
        }
        pw.flush();
        pw.close();
    }

    /**
     * {@inheritDoc} POST does nothing for this servlet.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();
        pw.print("use GET method");
    }
}
