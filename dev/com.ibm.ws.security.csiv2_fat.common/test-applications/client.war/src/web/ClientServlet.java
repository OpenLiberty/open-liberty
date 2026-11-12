/*******************************************************************************
 * Copyright (c) 2014, 2026 IBM Corporation and others.
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
package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Properties;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;

import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.wsspi.security.auth.callback.WSCallbackHandlerFactory;

import csiv2.sample.shared.IntermediateTestBean;
import csiv2.sample.shared.RemoteTestBean;

@SuppressWarnings("serial")
@WebServlet("/ClientServlet")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ClientServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(ClientServlet.class.getName());
    private static final String LOOKUP_JAVA_GLOBAL = "global";
    private static final String LOOKUP_JNDI = "jndi";
    private static final String LOOKUP_COSNAMING = "cosnaming";
    public static final String AutoMessage = "This is ClientServlet.";

    @EJB(beanName = "DownstreamBean")
    private RemoteTestBean bean;

    @EJB(beanName = "IntermediateBean")
    private IntermediateTestBean intermediateBean;

    /**
     * A simple servlet that when it received a request it simply outputs the message
     * as defined by the static field.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        writer.println(AutoMessage);

        String method = request.getParameter("method");

        String lookup = request.getParameter("lookup");
        if (lookup == null || lookup.isEmpty()) {
            lookup = LOOKUP_JAVA_GLOBAL;
        }

        String port = request.getParameter("port");

        try {
            String result = "";

            if ("invokeEJBAfterProgrammaticLogin".equals(method)) {
                result = invokeEJBAfterProgrammaticLogin(request, lookup, port);
            } else if ("invokeViaIntermediateBean".equals(method)) {
                String downstreamPort = request.getParameter("downstreamPort");
                result = invokeViaIntermediateBean(port, downstreamPort);
            } else {
                result = invokeEJB(lookup, port);
            }

            if (result.contains("callerSubject: "))
                writer.println(result);
            else {
                writer.println("Returned WS subject is Invalid");
            }
        } catch (PrivilegedActionException pae) {
            Exception e = pae.getException();
            writer.println("There was an exception tyring to invoke the remote bean: " + e.getMessage());
            e.printStackTrace(writer);
        } catch (Exception e) {
            writer.println("There was an exception trying to invoke the remote bean: " + e.getMessage());
            e.printStackTrace(writer);
        } finally {
            writer.flush();
            writer.close();
        }
    }

    private String invokeEJB(String lookup, String port) throws Exception {
        RemoteTestBean bean = getBean(lookup, port);
        return bean.test();
    }

    private String invokeEJBAfterProgrammaticLogin(HttpServletRequest request, final String lookup, final String port) throws Exception {
        String user = request.getParameter("user");
        String password = request.getParameter("password");
        String realmName = request.getParameter("realmName");

        WSCallbackHandlerFactory wsCallbackHandlerFactory = WSCallbackHandlerFactory.getInstance();
        CallbackHandler handler = wsCallbackHandlerFactory.getCallbackHandler(user, realmName, password);

        LoginContext loginContext;
        loginContext = new LoginContext("WSLogin", handler);
        loginContext.login();

        Subject subject = loginContext.getSubject();
        PrivilegedExceptionAction<?> invokeEJBAction = new PrivilegedExceptionAction<Object>() {
            @Override
            public Object run() throws Exception {
                RemoteTestBean bean = getBean(lookup, port);
                return bean.test();
            }
        };
        return (String) WSSubject.doAs(subject, invokeEJBAction);
    }

    private String invokeViaIntermediateBean(String port, String downstreamPort) throws Exception {
        IntermediateTestBean bean = getIntermediateBean(port);
        return bean.callRemoteTestBean(downstreamPort);
    }

    private RemoteTestBean getBean(String lookup, String port) throws Exception {
        RemoteTestBean bean = null;

        if (LOOKUP_JNDI.equals(lookup)) {
            logger.info("Looking up RemoteTestBean bean using JNDI.");
            bean = narrow(jndiResolve(port, "ejb/global/DownstreamApp/DownstreamEJB/DownstreamBean!" + RemoteTestBean.class.getName()), RemoteTestBean.class);
        } else if (LOOKUP_JAVA_GLOBAL.equals(lookup)) {
            logger.info("Looking up bean using java:global.");
            bean = (RemoteTestBean) new InitialContext().lookup("java:global/DownstreamApp/DownstreamEJB/DownstreamBean");
        } else if (LOOKUP_COSNAMING.equals(lookup)) {
            logger.info("Looking up bean using CosNaming.");
            bean = narrow(cosNamingResolve("ejb", "global", "DownstreamApp", "DownstreamEJB", "DownstreamBean!" + RemoteTestBean.class.getName()), RemoteTestBean.class);
        } else {
            logger.info("There was no lookup type provided. Looking up bean using java:global.");
            bean = (RemoteTestBean) new InitialContext().lookup("java:global/DownstreamApp/DownstreamEJB/DownstreamBean");
        }
        return bean;
    }

    private IntermediateTestBean getIntermediateBean(String port) throws Exception {
        logger.info("Looking up IntermediateTestBean bean using JNDI.");
        return narrow(jndiResolve(port, "ejb/global/IntermediateApp/IntermediateEJB/IntermediateBean!" + IntermediateTestBean.class.getName()), IntermediateTestBean.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> T narrow(Object from, Class<T> to) {
        return (T) PortableRemoteObject.narrow(from, to);
    }

    private Object jndiResolve(String port, String name) throws Exception {
        String url = "corbaname::localhost:" + port + "/NameService#" + name;
        logger.info("Using corbaname url: " + url);
        Object obj = new InitialContext().lookup(url);
        logger.info("Looked up stub for ejb: " + obj);
        return obj;
    }

    private Object cosNamingResolve(String... names) throws Exception {
        Properties props = new Properties();
        props.put("org.omg.PortableInterceptor.ORBInitializerClass." + "com.ibm.ws.transport.iiop.security.SecurityInitializer", "");
        //String nsUrl = "corbaloc::" + "IBM-4DA2598FA65" + ":" + "2810" + "/NameService";
        String nsUrl = "corbaloc::localhost:" + "2809" + "/NameService";
        ORB orb = ORB.init(new String[] { "-ORBInitRef", "NameService=" + nsUrl, "-IIOPconnectionHelper", "com.ibm.ws.transport.iiop.security.config.ssl.yoko.SocketFactory" },
                           props);

        org.omg.CORBA.Object nameService = orb.resolve_initial_references("NameService");
        NamingContext context = NamingContextHelper.narrow(nameService);
        logger.info("resolved initial reference to NameService");
        for (int i = 0; i < names.length - 1; i++) {
            logger.info("resolving context " + names[i] + " relative to " + context);
            context = NamingContextHelper.narrow(context.resolve(new NameComponent[] { new NameComponent(names[i], "") }));
        }

        logger.info("resolving object " + names[names.length - 1] + " relative to " + context);
        return context.resolve(new NameComponent[] { new NameComponent(names[names.length - 1], "") });
    }
}
