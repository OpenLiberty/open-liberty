/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.security.wim.VMMService;
import com.ibm.wsspi.security.wim.SchemaConstants;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.IdentifierType;
import com.ibm.wsspi.security.wim.model.LoginAccount;
import com.ibm.wsspi.security.wim.model.LoginControl;
import com.ibm.wsspi.security.wim.model.PersonAccount;
import com.ibm.wsspi.security.wim.model.PropertyControl;
import com.ibm.wsspi.security.wim.model.Root;

/**
 * Servlet to access VMM APIs. VMM APIs are called using VMM Service
 */
public class VmmServiceServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * Tries to resolve the VMMService
     *
     * @param writer
     * @param loader
     * @return VMMService, which may possibly be null (which is bad but
     *         if it happens, it happens).
     */
    private VMMService getVMMService(PrintWriter writer) {
        VMMService vmmServ;

        Bundle bundle = FrameworkUtil.getBundle(Servlet.class);
        if (bundle == null) {
            writer.println("Unable to determine bundle");
            return null;
        }

        BundleContext bundleContext = bundle.getBundleContext();
        if (bundleContext == null) {
            writer.println("Unable to determine bundle context");
            return null;
        }

        String serviceName = VMMService.class.getName();

        writer.println("Looking up " + serviceName);
        ServiceReference<?> ref = bundleContext.getServiceReference(serviceName);
        writer.println(serviceName + " reference is " + ref);
        vmmServ = (VMMService) bundleContext.getService(ref);

        if (vmmServ == null)
            throw new IllegalStateException("Unable to find VMMService");
        return vmmServ;
    }

    /**
     * Handles method calls which is the core purpose of this servlet.
     * There are certain test flows where exceptions may be expected.
     * Allow those to occur and capture them such that they can be
     * consumed by tests.
     *
     * @param req
     * @param pw
     * @param vmmServ VMMService instance
     * @throws NullPointerException
     * @throws RemoteException
     */
    private void handleMethodRequest(HttpServletRequest req, PrintWriter pw, VMMService vmmServ) {
        String response = null;

        String method = req.getParameter("method");
        try {
            if ("getUser".equals(method)) {

                IdentifierType id = new IdentifierType();
                id.setUniqueName(req.getParameter("uniqueName"));

                Entity entity = new Entity();
                entity.setIdentifier(id);

                PropertyControl properties = new PropertyControl();
                properties.getProperties().add("uid");
                properties.getProperties().add("cn");
                properties.getProperties().add("sn");
                properties.getProperties().add("mail");
                properties.getProperties().add("telephoneNumber");
                properties.getProperties().add("photoURL");
                properties.getProperties().add("photoURLThumbnail");

                Root root = new Root();
                root.getEntities().add(entity);
                root.getControls().add(properties);

                root = vmmServ.get(root);

                PersonAccount person = (PersonAccount) root.getEntities().get(0);

                Map<String, Object> props = new HashMap<String, Object>();

                props.put("uid", person.getUid());
                props.put("cn", person.getCn());
                props.put("sn", person.getSn());
                props.put("mail", person.getMail());
                props.put("telephoneNumber", person.getTelephoneNumber());
                props.put("photoURL", person.getPhotoUrl());
                props.put("photoURLThumbnail", person.getPhotoUrlThumbnail());

                response = props.toString();
            } else if ("login".equals(method)) {

                LoginAccount person = new LoginAccount();
                person.set(SchemaConstants.PROP_PRINCIPAL_NAME, req.getParameter("userName"));
                try {
                    person.set(SchemaConstants.PROP_PASSWORD, req.getParameter("password").getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    pw.println(e.getMessage());
                }

                LoginControl loginCtrl = new LoginControl();

                Root root = new Root();
                root.getEntities().add(person);
                root.getControls().add(loginCtrl);

                root = vmmServ.login(root);

                response = root.getEntities().get(0).getIdentifier().getUniqueName();
            } else {
                pw.println("Usage: url?method=name&paramName=paramValue&...");
            }
        } catch (NullPointerException npe) {
            response = npe.toString();
        } catch (WIMException e) {
            response = e.toString();
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
            handleMethodRequest(req, pw, getVMMService(pw));
        } catch (IllegalArgumentException e) {
            e.printStackTrace(pw);
            pw.println("getVMMService exception message:");
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
