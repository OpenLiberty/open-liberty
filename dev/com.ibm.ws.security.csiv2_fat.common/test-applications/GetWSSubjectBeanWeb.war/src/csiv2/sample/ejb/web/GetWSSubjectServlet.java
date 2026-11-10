/*******************************************************************************
 * Copyright (c) 2016, 2026 IBM Corporation and others.
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
package csiv2.sample.ejb.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.security.PrivilegedAction;

import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.security.CustomRegistryException;
import com.ibm.websphere.security.UserRegistry;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.WSSecurityHelper;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.auth.callback.WSCallbackHandlerImpl;
import com.ibm.wsspi.security.registry.RegistryHelper;

import csiv2.sample.ejb.SetWSSubject;

@SuppressWarnings("serial")
@WebServlet("/GetWSSubject")
public class GetWSSubjectServlet extends HttpServlet {

    /**
     * This servlet uses the security subject to call the Remote EJB method which will
     * authenticate the subject and return whether the subject is validated
     */

    @EJB(beanName = "WSSubjectBean")
    private SetWSSubject bean;
    private HttpServletResponse myresp;
    private Subject subject;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {

        myresp = response;
        try {
            bean = (SetWSSubject) new InitialContext().lookup("java:module/WSSubjectBean!" + SetWSSubject.class.getName());
        } catch (NamingException e2) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
            e2.printStackTrace();
        }
        if (WSSecurityHelper.isServerSecurityEnabled()) {
            UserRegistry ur = null;
            try {
                ur = RegistryHelper.getUserRegistry(null);
            } catch (WSSecurityException e1) {
                return;
            }
            String userid = "balaji";
            String password = "p@ssw0rd";
            try {
                if (ur.isValidUser(userid)) {
                    System.out.println(userid + "is existing in the registry");
                    CallbackHandler wscbh = new WSCallbackHandlerImpl(userid, password);
                    LoginContext ctx;
                    ctx = new LoginContext("WSLogin", wscbh);
                    ctx.login();
                    subject = ctx.getSubject();
                    System.out.println("After succssfully logging in, action will be called "
                                       + "on behalf of the subject" + subject.toString());
                    WSSubject.doAs(subject, action);
                }
            } catch (CustomRegistryException e) {
                // record some diagnostic info
                return;
            } catch (RemoteException e) {
                // record some diagnostic info
                return;
            } catch (LoginException e) {
                // record some diagnostic info
                return;
            }
        }

    }

    private final PrivilegedAction<?> action = new PrivilegedAction<Object>() {
        @Override
        public Object run() {

            PrintWriter writer;
            try {
                writer = myresp.getWriter();
                String result = bean.returnSecuritySubject();
                if (result.contains("callerSubject: "))
                    writer.println("Returned WS subject is Valid");
                else
                    writer.println("Returned WS subject is Invalid");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }
    };
}
