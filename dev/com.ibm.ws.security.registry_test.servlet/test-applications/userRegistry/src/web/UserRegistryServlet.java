/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.registry.CustomRegistryException;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.NotImplementedException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;

/**
 * Servlet to access UserRegistry APIs. Will retrieved an instance of the
 * current UserRegistry on each request.
 */
public class UserRegistryServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * Try to get the UserRegistryService directly from ServiceRegistry.
     *
     * @param writer
     * @param bundleContext
     * @return
     */
    private UserRegistryService tryUserRegistryService(PrintWriter writer, BundleContext bundleContext) {
        String serviceName = UserRegistryService.class.getName();
        writer.println("No SecurityService found, trying UserRegistryService");

        // Unable to find the security service, try the UserRegistryService directly
        serviceName = UserRegistryService.class.getName();
        writer.println("Looking up " + serviceName);
        ServiceReference<?> ref = bundleContext.getServiceReference(serviceName);
        writer.println(serviceName + " reference is " + ref);
        UserRegistryService urServ = (UserRegistryService) bundleContext.getService(ref);
        return urServ;
    }

    /**
     * Try to get the UserRegistryService from the SecurityService.
     *
     * @param writer
     * @param bundleContext
     * @return
     */
    private UserRegistryService trySecurityService(PrintWriter writer, BundleContext bundleContext) {
        try {
            String serviceName = SecurityService.class.getName();
            writer.println("Looking up " + serviceName);
            ServiceReference<?> secServRef = bundleContext.getServiceReference(serviceName);
            writer.println(serviceName + " reference is " + secServRef);
            if (secServRef != null) {
                SecurityService secServ = (SecurityService) bundleContext.getService(secServRef);
                return secServ.getUserRegistryService();
            } else {
                return null;
            }
        } catch (NoClassDefFoundError ncdfe) {
            writer.println("SecurityService resulted in NoClassDefFoundError");
            return null;
        }
    }

    /**
     * Tries to resolve the UserRegistryService, first by checking if there is a
     * SecurityService, and if that is not available, then it tries to find the
     * UserRegistryService directly.
     *
     * @param writer
     * @param loader
     * @return UserRegistryService, which may possibly be null (which is bad but
     *         if it happens, it happens).
     */
    private UserRegistryService getUserRegistryService(PrintWriter writer) {
        UserRegistryService ret;

        Bundle bundle = FrameworkUtil.getBundle(Servlet.class);
        if (bundle == null) {
            writer.println("Unable to determine bundle");
            return null;
        }

        BundleContext bundleContext = AccessController.doPrivileged(new PrivilegedAction<BundleContext>() {

            @Override
            public BundleContext run() {
                return bundle.getBundleContext();
            }
        });
        if (bundleContext == null) {
            writer.println("Unable to determine bundle context");
            return null;
        }

        // First, try to get the SecurityService, since if that is configured,
        // we want to get the server's configured security domain
        ret = trySecurityService(writer, bundleContext);
        if (ret == null)
            ret = tryUserRegistryService(writer, bundleContext);
        if (ret == null)
            throw new IllegalStateException("Unable to find UserRegistryService");
        return ret;
    }

    /**
     * Grab the effective UserRegistry instance by looking up (in an ugly way)
     * from the OSGis service registry.
     *
     * @return UserRegistry instance, or null if can't find service.
     * @throws RegistryException
     */
    private UserRegistry getCurrentUserRegistry(PrintWriter writer) throws RegistryException {
        UserRegistryService urServ = getUserRegistryService(writer);
        writer.println("UserRegistryService class is " + urServ.getClass());
        UserRegistry reg = urServ.getUserRegistry();
        writer.println("UserRegistry class is " + reg.getClass());
        writer.flush();
        return reg;
    }

    /**
     * Handles method calls which is the core purpose of this servlet.
     * There are certain test flows where exceptions may be expected.
     * Allow those to occur and capture them such that they can be
     * consumed by tests.
     *
     * @param req
     * @param pw
     * @param ur  UserRegistry instance
     * @throws CustomRegistryException
     * @throws NotImplementedException
     * @throws RemoteException
     */
    private void handleMethodRequest(HttpServletRequest req, PrintWriter pw, UserRegistry ur) {
        String response = null;

        String method = req.getParameter("method");
        try {
            if ("getRealm".equals(method)) {
                response = ur.getRealm();
            } else if ("getType".equals(method)) {
                response = ur.getType();
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
                response = convertFromSR(ur.getUsers(pattern, limit));
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
                response = convertFromSR(ur.getGroups(pattern, limit));
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
                response = convertFromList(ur.getUniqueGroupIdsForUser(uniqueUserId));
            } else if ("getGroupsForUser".equals(method)) {
                String userSecurityName = req.getParameter("userSecurityName");
                response = convertFromList(ur.getGroupsForUser(userSecurityName));
            } else if ("getUsersForGroup".equals(method)) {
                String groupSecurityName = req.getParameter("groupSecurityName");
                int limit = Integer.valueOf(req.getParameter("limit"));
                response = convertFromSR(ur.getUsersForGroup(groupSecurityName, limit));
            } else {
                pw.println("Usage: url?method=name&paramName=paramValue&...");
            }
        } catch (RegistryException re) {
            response = re.toString();
        } catch (EntryNotFoundException enof) {
            response = enof.toString();
        } catch (NullPointerException npe) {
            response = npe.toString();
        } catch (RemoteException re) {
            response = re.toString();
        } catch (NotImplementedException nie) {
            response = nie.toString();
        } catch (CustomRegistryException cre) {
            response = cre.toString();
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
            handleMethodRequest(req, pw, getCurrentUserRegistry(pw));
        } catch (RegistryException e) {
            e.printStackTrace(pw);
            pw.println("getCurrentUserRegistry exception message:");
            pw.println(e);
            pw.flush();
        } catch (IllegalArgumentException e) {
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

    /**
     * Create a DN safe string representation of a List. The problem with just using the
     * List.toString() method is that it separates entries by a ", ". That is perfectly
     * valid in a DN and results in a single result being parsed into separate
     * unique results.
     *
     * <p/>
     * TODO Should consider changing this servlet to respond with JSON to get rid of the
     * need for parsing on the client side.
     *
     * @param sr The {@link List} instance to create a DN-safe string from.
     * @return The {@link List} as a String list.
     */
    private static String convertFromList(List<?> results) {
        System.out.println("UserRegistryServlet.convertFromList(): " + results.getClass() + " " + results);

        if (results.isEmpty()) {
            return results.toString();
        }

        /*
         * Something unlikely to occur in a DN, yet still readable. If this value changes
         * remember to update UserRegistryServletConnection#convertToList() as well.
         */
        final String delimiter = " :: ";

        StringBuffer sb = new StringBuffer();

        sb.append('[');
        for (int idx = 0; idx < results.size(); idx++) {
            sb.append(results.get(idx));
            if (idx < (results.size() - 1)) {
                sb.append(delimiter);
            }
        }
        sb.append(']');

        return sb.toString();
    }

    /**
     * Create a DN safe string representation of a SearchResult. The problem with just
     * using the List.toString() method is that it separates entries by a ", ". That
     * is perfectly valid in a DN and results in a single result being parsed into
     * separate unique results.
     *
     * @param sr The {@link List} instance to create a DN-safe string from.
     * @return The {@link List} as a String list.
     */
    private static String convertFromSR(SearchResult results) {
        /*
         * Originated from SearchResult.toString(). We make DN safe string representation of the list
         * here instead of calling List.toString().
         */
        return "SearchResult hasMore=" + results.hasMore() + " " + convertFromList(results.getList());
    }
}
