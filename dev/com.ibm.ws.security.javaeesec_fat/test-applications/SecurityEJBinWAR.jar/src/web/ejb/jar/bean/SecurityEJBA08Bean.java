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
package web.ejb.jar.bean;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.enterprise.SecurityContext;

import com.ibm.websphere.security.auth.callback.WSCallbackHandlerImpl;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;

/**
 * Bean implementation class for Enterprise Bean which issues JAAS programmatic
 * login().
 *
 * Note: The user for the login is specified in this bean and relies on the
 * basic registry configuration in server.xml with user1 in group1 and user3 in group3.
 */

@Stateless
@PermitAll
public class SecurityEJBA08Bean extends SecurityEJBBeanBase implements SecurityEJBInterface {

    private static final Class<?> c = SecurityEJBA08Bean.class;
    protected Logger logger = Logger.getLogger(c.getCanonicalName());

    @EJB(beanName = "SecurityEJBRunAsBean")
    private SecurityEJBRunAsInterface injectedRunAs;

    @Resource
    private SessionContext context;

    public SecurityEJBA08Bean() {
        withDeprecation();
    }

    @Override
    protected SessionContext getContext() {
        return context;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    @DenyAll
    public String denyAll() {
        String result = authenticate("denyAll");
        return result;
    }

    @Override
    @DenyAll
    public String denyAll(String input) {
        return authenticate("denyAll(input)");
    }

    @Override
    @PermitAll
    public String permitAll() {
        String result = authenticate("permitAll");
        String loginUser = "user1";
        String loginPassword = "user1pwd";
        result = result + "Perform JAAS login() with same user: " + loginUser + "\n";

        try {
            CallbackHandler loginCallbackHandler = new WSCallbackHandlerImpl(loginUser, loginPassword);
            LoginContext ctx = new LoginContext(JaasLoginConfigConstants.APPLICATION_WSLOGIN, loginCallbackHandler);
            ctx.login();
            Subject subject = ctx.getSubject();
            result = result + "\nNew login subject: " + subject.toString();
            ctx.logout();

        } catch (Exception e) {
            return result + "\n Caught unexpected exception during login: " + e.getMessage();
        }

        return result;
    }

    @Override
    public String checkAuthenticated() {
        return authenticate("checkAuthenticated()");
    }

    @Override
    @RolesAllowed("**")
    public String permitAuthenticated() {
        return authenticate("permitAuthenticated()");
    }

    @Override
    @PermitAll
    public String permitAll(String input) {
        String result = authenticate("permitAll(input)");
        String loginUser = "user3";
        String loginPassword = "user3pwd";
        result = result + "Perform JAAS login() with new user: " + loginUser + "\n";

        try {
            CallbackHandler loginCallbackHandler = new WSCallbackHandlerImpl(loginUser, loginPassword);
            LoginContext ctx = new LoginContext(JaasLoginConfigConstants.APPLICATION_WSLOGIN, loginCallbackHandler);
            ctx.login();
            Subject subject = ctx.getSubject();
            result = result + "\nNew login subject: " + subject.toString();
            ctx.logout();

        } catch (Exception e) {
            return result + "\n Caught unexpected exception during login: " + e.getMessage();
        }

        return result;
    }

    /**
     * @param loginUser
     * @param loginPassword
     * @return
     * @throws Exception
     */
    private LoginContext createLoginContext(String loginUser, String loginPassword) throws Exception {
        final CallbackHandler loginCallbackHandler = new WSCallbackHandlerImpl(loginUser, loginPassword);
        final LoginContext[] ctx2 = { null };
        final String ctxresult[] = { "" };
        new java.security.PrivilegedAction<String>() {
            @Override
            public String run() {
                try {
                    ctx2[0] = new LoginContext(JaasLoginConfigConstants.APPLICATION_WSLOGIN, loginCallbackHandler);
                } catch (LoginException le) {
                    ctxresult[0] = "Caught exception getting login context: " + le.getMessage();
                }
                return ctxresult[0];
            }
        };
        if (ctxresult[0].contains("exception"))
            throw new Exception(ctxresult[0]);

        return ctx2[0];

    }

    @Override
    @RolesAllowed("Manager")
    public String manager() {
        return authenticate("manager");
    }

    @Override
    @RolesAllowed("Manager")
    public String manager(String input) {
        return authenticate("manager(input)");
    }

    @Override
    @RolesAllowed("Employee")
    public String employee() {
        return authenticate("employee");
    }

    @Override
    @RolesAllowed("Employee")
    public String employee(String input) {
        return authenticate("employee(input)");
    }

    @Override
    public String employeeAndManager() {
        return authenticate("employeeAndManager");
    }

    @Override
    public String employeeAndManager(String input) {
        return authenticate("employeeAndManager(input)");
    }

    @Override
    public String employeeAndManager(String input, String input2) {
        return authenticate("employeeAndManager(string1, string2)");
    }

    @Override
    public String employeeAndManager(int i) {
        return authenticate("employeeAndManager(3)");
    }

    @Override
    public String declareRoles01() {
        String result1 = authenticate("declareRoles01");
        boolean isDeclaredMgr = context.isCallerInRole("DeclaredRole01");
        int len = result1.length() + 5;
        StringBuffer result2 = new StringBuffer(len);
        result2.append(result1);
        result2.append("\n");
        result2.append("   isCallerInRole(DeclaredRole01)=");
        result2.append(isDeclaredMgr);
        logger.info("result2: " + result2);
        return result2.toString();
    }

    @Override
    @PermitAll
    public String runAsClient() {
        return authenticate("runAsClient");
    }

    @Override
    @PermitAll
    public String runAsSpecified() {
        return authenticate("runAsSpecified");
    }

    /*
     * (non-Javadoc)
     * 
     * @see web.ejb.jar.bean.SecurityEJBBeanBase#getSecurityContext()
     */
    @Override
    protected SecurityContext getSecurityContext() {
        // TODO Auto-generated method stub
        return null;
    }

}
