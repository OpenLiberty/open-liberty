/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.jar.base;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigProvider;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.Part;

import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.wsspi.security.token.SingleSignonToken;

/**
 * Base servlet which the JASPI test servlets extend.
 */
public abstract class FlexibleBaseNoJavaEESecServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private String servletName;
    public List<BaseServletStep> mySteps = new ArrayList<BaseServletStep>();
    protected BaseServletStep myErrorStep = new WriteErrorStep();
    private static Logger log = Logger.getLogger(FlexibleBaseNoJavaEESecServlet.class.getName());
    private String moduleName = null;

    public FlexibleBaseNoJavaEESecServlet(String servletName) {
        this.servletName = servletName;
    }

    public FlexibleBaseNoJavaEESecServlet(String servletName, String moduleName) {
        this.servletName = servletName;
        this.moduleName = moduleName;
    }

    public void updateServletName(String servletName) {
        this.servletName = servletName;
    }

    @Override
    public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if ("CUSTOM".equalsIgnoreCase(req.getMethod()))
            doCustom(req, res);
        else
            super.service(req, res);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest("GET", req, resp);
        String op = req.getParameter("logout");
        if (op != null && "true".equals(op)) {
            req.logout();
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest("POST", req, resp);
    }

    public void doCustom(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest("CUSTOM", req, resp);
    }

    /**
     * Common logic to handle any of the various requests this servlet supports.
     * The actual business logic can be customized by overriding performTask.
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    public void handleRequest(String type, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter writer = resp.getWriter();
        writer.println("HandleRequest: We are at the WRONG manager method call!!!!");
        writer.println("ServletName: " + servletName);
        writer.println("Request type: " + type);

        StringBuffer sb = new StringBuffer();
        try {
            performTask(type, req, resp, sb);
        } catch (Throwable t) {
            t.printStackTrace(writer);
        }

        writer.write(sb.toString());
//        writer.flush();
//        writer.close();
    }

    /**
     * Default action for the servlet if not overridden.
     *
     * @param req
     * @param resp
     * @param writer
     * @throws ServletException
     * @throws IOException
     */
    public void performTask(String type,
                            HttpServletRequest req,
                            HttpServletResponse resp, StringBuffer sb) throws Exception {
        performCustomTasks(type, req, resp, sb);
    }

    /**
     * Gets the SSO token from the subject.
     *
     * @param subject {@code null} is not supported.
     * @return
     */
    private SingleSignonToken getSSOToken(Subject subject) {
        SingleSignonToken ssoToken = null;
        Set<SingleSignonToken> ssoTokens = subject.getPrivateCredentials(SingleSignonToken.class);
        Iterator<SingleSignonToken> ssoTokensIterator = ssoTokens.iterator();
        if (ssoTokensIterator.hasNext()) {
            ssoToken = ssoTokensIterator.next();
        }
        return ssoToken;
    }

    class BaseServletParms {
        private String myType;
        private HttpServletRequest myRequest;
        private HttpServletResponse myResponse;
        private StringBuffer myBuffer;
        private Throwable myError;

        public String getType() {
            return myType;
        }

        public void setType(String type) {
            myType = type;
        }

        public HttpServletRequest getRequest() {
            return myRequest;
        }

        public void setRequest(HttpServletRequest req) {
            myRequest = req;
        }

        public HttpServletResponse getResponse() {
            return myResponse;
        }

        public void setResponse(HttpServletResponse res) {
            myResponse = res;
        }

        public StringBuffer getBuffer() {
            return myBuffer;
        }

        public void setBuffer(StringBuffer sb) {
            myBuffer = sb;
        }

        public Subject getSubject() throws Exception {
            return fetchSubject();
        }

        public Throwable getError() {
            return myError;
        }

        public void setError(Throwable t) {
            myError = t;
        }
    }

    static interface BaseServletStep {
        void invoke(BaseServletParms p) throws Exception;
    }

    public class WriteParametersStep implements BaseServletStep {

        @Override
        public void invoke(BaseServletParms p) {

            if (!p.getType().equals("POST")) {
                // print all the parms
                Enumeration e = p.getRequest().getParameterNames();
                if (e.hasMoreElements()) {
                    writeLine(p.getBuffer(), "All Parameters");
                    while (e.hasMoreElements()) {
                        String name = (String) e.nextElement();
                        writeLine(p.getBuffer(), "Param: " + name + " with value: " + p.getRequest().getParameter(name));
                    }
                }
            } else {

                try {
                    Collection<Part> myParts = p.getRequest().getParts();
                    writeLine(p.getBuffer(), "All Parameters");
                    for (Iterator<Part> myIt = myParts.iterator(); myIt.hasNext();) {
                        Part pt = myIt.next();
                        InputStream inst = pt.getInputStream();
                        StringBuilder inputStringBuilder = new StringBuilder();
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inst, "UTF-8"));
                        String line = bufferedReader.readLine();
                        Boolean firstOneDone = false;
                        while (line != null) {
                            if (firstOneDone) {
                                inputStringBuilder.append('\n');
                                firstOneDone = true;
                            }
                            inputStringBuilder.append(line);
                            line = bufferedReader.readLine();
                        }
                        writeLine(p.getBuffer(), "Param: " + pt.getName() + " with value: " + inputStringBuilder.toString());
                    }
                } catch (Exception exc) {
                    writeLine(p.getBuffer(), "Exception occurred: " + exc.toString());

                }
            }
        }

    }

    public class WriteRequestBasicsStep implements BaseServletStep {

        @Override
        public void invoke(BaseServletParms p) {
            writeLine(p.getBuffer(), "getAuthType: " + p.getRequest().getAuthType());
            writeLine(p.getBuffer(), "getRequestURL: " + p.getRequest().getRequestURL().toString());
            if (p.getRequest().getQueryString() != null)
                writeLine(p.getBuffer(), "Query string: " + p.getRequest().getQueryString().toString());
        }
    }

    public class WritePrincipalStep implements BaseServletStep {

        @Override
        public void invoke(BaseServletParms p) throws Exception {
            writeLine(p.getBuffer(), "getRemoteUser: " + p.getRequest().getRemoteUser());
            writeLine(p.getBuffer(), "getUserPrincipal: " + p.getRequest().getUserPrincipal());

            if (p.getRequest().getUserPrincipal() != null) {
                writeLine(p.getBuffer(), "getUserPrincipal().getName(): "
                                         + p.getRequest().getUserPrincipal().getName());
            }
        }
    }

    public class WriteRolesStep implements BaseServletStep {

        @Override
        public void invoke(BaseServletParms p) throws Exception {

            writeLine(p.getBuffer(), "isUserInRole(javaeesec_basic): "
                                     + p.getRequest().isUserInRole("javaeesec_basic"));
            writeLine(p.getBuffer(), "isUserInRole(javaeesec_form): " + p.getRequest().isUserInRole("javaeesec_form"));
            String role = p.getRequest().getParameter("role");
            if (role == null) {
                writeLine(p.getBuffer(), "You can customize the isUserInRole call with the follow paramter: ?role=name");
            }
            writeLine(p.getBuffer(), "isUserInRole(" + role + "): " + p.getRequest().isUserInRole(role));
        }

    }

    public class WriteCookiesStep implements BaseServletStep {

        @Override
        public void invoke(BaseServletParms p) throws Exception {

            Cookie[] cookies = p.getRequest().getCookies();
            writeLine(p.getBuffer(), "Getting cookies");
            if (cookies != null && cookies.length > 0) {
                for (int i = 0; i < cookies.length; i++) {
                    writeLine(p.getBuffer(), "cookie: " + cookies[i].getName() + " value: "
                                             + cookies[i].getValue());
                }
            }
        }
    }

    public class WriteSubjectStep implements BaseServletStep {

        @Override
        public void invoke(BaseServletParms p) throws Exception {
            writeLine(p.getBuffer(), "CallerSubject: " + p.getSubject());
        }
    }

    public class JASPIHttpServletRequestWrapper extends HttpServletRequestWrapper {
        public JASPIHttpServletRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getHeader(String name) {
            if ("hasWrapper".equals(name)) {
                return "true";
            }
            return super.getHeader(name);
        }
    }

    public class JASPIHttpServletResponseWrapper extends HttpServletResponseWrapper {

        public JASPIHttpServletResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public String getHeader(String name) {
            if ("hasWrapper".equals(name)) {
                return "true";
            }
            return super.getHeader(name);
        }
    }

    public class WriteWrapperStep implements BaseServletStep {

        @Override
        public void invoke(BaseServletParms p) throws Exception {
            if (p.getRequest().getHeader("hasWrapper").equalsIgnoreCase("true")) {
                writeLine(p.getBuffer(), "The httpServletRequest has been wrapped by httpServletRequestWrapper.");
            }
            if (p.getResponse().getHeader("hasWrapper").equalsIgnoreCase("true")) {
                writeLine(p.getBuffer(), "The httpServletRestponse has been wrapped by httpServletResponseWrapper.");
            }
        }
    }

    public class WritePublicCredentialsStep implements BaseServletStep {

        @Override
        public void invoke(BaseServletParms p) throws Exception {

            // Get the public credential from the CallerSubject
            if (p.getSubject() != null) {
                WSCredential callerCredential = p.getSubject().getPublicCredentials(WSCredential.class).iterator().next();
                if (callerCredential != null) {
                    writeLine(p.getBuffer(), "callerCredential One: " + callerCredential);
                } else {
                    writeLine(p.getBuffer(), "callerCredential Two: null");
                }
            } else {
                writeLine(p.getBuffer(), "callerCredential Three: null");
            }
        }
    }

    public class WriteRunAsSubjectStep implements BaseServletStep {

        @Override
        public void invoke(BaseServletParms p) throws Exception {
            // getInvocationSubject for RunAs tests
            Subject runAsSubject = WSSubject.getRunAsSubject();
            writeLine(p.getBuffer(), "RunAsSubject: " + runAsSubject);
        }
    }

    public abstract class ProcessStep implements BaseServletStep {
        public HttpURLConnection prepareConnection(BaseServletParms p, String rawUrl) throws MalformedURLException, IOException, ProtocolException {
            URL url = new URL(rawUrl);
            writeLine(p.getBuffer(), "HttpURLConnection URL is set to: " + url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            writeLine(p.getBuffer(), "HttpURLConnection successfully opened the connection to URL " + url);
            connection.setRequestMethod("GET");
            writeLine(p.getBuffer(), "HttpURLConnection set request method to GET");
            connection.setDoOutput(true);
            return connection;
        }

        public void connect(BaseServletParms p, HttpURLConnection connection) throws IOException {
            connection.connect();
            writeLine(p.getBuffer(), "HttpURLConnection successfully completed connection.connect");
        }

        public void writeToConnection(BaseServletParms p, HttpURLConnection connection, String message) throws IOException {
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(message);
//            writer.flush();
//            writer.close();
            writeLine(p.getBuffer(), "HttpURLConnection successfully completed Writer.write");
        }

        public void processResponse(BaseServletParms p,
                                    HttpURLConnection connection) throws IOException, UnsupportedEncodingException {
            InputStream responseStream = null;
            int statusCode = connection.getResponseCode();
            writeLine(p.getBuffer(), "HttpURLConnection status code from connect: " + statusCode);
            if (statusCode == 200) {
                responseStream = connection.getInputStream();
            } else {
                responseStream = connection.getErrorStream();
            }
            final char[] buffer = new char[1024];
            StringBuffer sb2 = new StringBuffer();
            InputStreamReader reader = new InputStreamReader(responseStream, "UTF-8");
            int bytesRead;
            do {
                bytesRead = reader.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    sb2.append(buffer, 0, bytesRead);
                }
            } while (bytesRead >= 0);
            reader.close();
            String resultResource = new String(sb2.toString().trim());
            writeLine(p.getBuffer(), "HttpURLConnection result from invoking servlet: " + resultResource);
        }
    }

    public class WriteErrorStep implements BaseServletStep {

        @Override
        public void invoke(BaseServletParms p) throws Exception {
            if (p.getError() != null) {
                if (p.getError() instanceof NoClassDefFoundError) {
                    // For OSGI App testing (EBA file), we expect this exception for all
                    // packages that are not public
                    writeLine(p.getBuffer(), "NoClassDefFoundError for SubjectManager: " + p.getError());

                } else {
                    p.getError().printStackTrace();
                }
            }

        }

    }

    public class ProcessRegistrationStep implements BaseServletStep {

        //       private final String validProviderClass = "com.ibm.ws.security.jaspi.test.AuthProvider_1";

        @Override
        public void invoke(BaseServletParms p) {
            if (p.getRequest() != null && p.getRequest().getQueryString() != null) {
                String query = p.getRequest().getQueryString();
                String[] queryInfo = query.split("\\*\\*");

                if (queryInfo.length != 4)
                    error(p.getBuffer());
                else {
                    AuthConfigFactory factory = AuthConfigFactory.getFactory();
                    if (factory != null)
                        writeLine(p.getBuffer(), "Factory: " + factory.toString());
                    String q = queryInfo[0];
                    if (q.equals("REGISTER")) {
                        writeLine(p.getBuffer(), "Registering provider...");
                        registerProvider(p.getBuffer(), factory, queryInfo[1], queryInfo[2], queryInfo[3]);
                    } else if (q.equals("GET")) {
                        writeLine(p.getBuffer(), "Getting provider...");
                        getProvider(p.getBuffer(), factory, queryInfo[1], queryInfo[2]);
                    } else if (q.equals("REMOVE")) {
                        writeLine(p.getBuffer(), "Removing provider...");
                        removeProvider(p.getBuffer(), factory, queryInfo[1], queryInfo[2]);
                    } else if (q.equals("REMOVEINVALID")) {
                        writeLine(p.getBuffer(), "Removing provider...");
                        removeInvalidProvider(p.getBuffer(), factory, queryInfo[1], queryInfo[2]);
                    } else if (q.equals("REGISTERINVALIDCLASS")) {
                        writeLine(p.getBuffer(), "Registering invalid provider class...");
                        registerProvider(p.getBuffer(), factory, queryInfo[1], queryInfo[2], queryInfo[3]);
                    }
                }
            } else
                error(p.getBuffer());

        }

        /**
         * Register a JASPI persistent provider with the given className, msgLayer, and appContext.
         * Print "Success" to the response object if the registration is successful.
         * Print a failure message to the response object if an exception is encountered.
         *
         * @param out
         * @param factory
         * @param msgLayer
         * @param appContext
         * @param className
         */
        private void registerProvider(StringBuffer sb, AuthConfigFactory factory, String msgLayer, String appContext, String className) {

            try {
                writeLine(sb, "className:" + className + "  msgLayer:" + msgLayer + "  appContext:" + appContext);
                factory.registerConfigProvider(className, null, msgLayer, appContext, null);
                writeLine(sb, "Successfully registered provider.");
            } catch (Exception e) {
                writeLine(sb, "Exception registering provider: " + e);
            }

        }

        /**
         * Get the persistent provider registration for the given msgLayer and appContext.
         * Print the registration context information to the response object if the registration is found.
         * Print a failure message to the response object if the registration is not found.
         *
         * @param out
         * @param factory
         * @param msgLayer
         * @param appContext
         */
        private void getProvider(StringBuffer sb, AuthConfigFactory factory, String msgLayer, String appContext) {

            appContext = appContext.replace("__", " "); //handle blank spaces in the appContext

            AuthConfigProvider provider = factory.getConfigProvider(msgLayer, appContext, null);
            if (provider == null) {
                writeLine(sb, "Failed to get registered provider for message layer=" + msgLayer + " and application context=" + appContext);
                return;
            }

            String[] regIDs = factory.getRegistrationIDs(provider);
            if (regIDs.length == 0) {
                writeLine(sb, "Failed to get registration ID for provider " + provider);
                return;
            }

            AuthConfigFactory.RegistrationContext context = factory.getRegistrationContext(regIDs[0]);
            if (context == null) {
                writeLine(sb, "Failed to get registration context for registration ID " + regIDs[0]);
                return;
            }

            //successfully found registration context, print context properties
            writeLine(sb, "\nProvider registration --\nMessage layer: " + context.getMessageLayer() + "\nApp Context: " + context.getAppContext() + "\nIsPersistent: "
                          + context.isPersistent()
                          + "\nProvider Class: " + provider.getClass() + "\n");

        }

        /**
         * Remove persistent provider registration associated with an invalid msgLayer and appContext.
         * Print "Success" to the response object if the removal is successful.
         * Print a failure message to the response object if the registration does not exist.
         *
         * @param out
         * @param factory
         * @param msgLayer
         * @param appContext
         */
        private void removeInvalidProvider(StringBuffer sb, AuthConfigFactory factory, String msgLayer, String appContext) {

            String[] regIDs = { "invalidRegID" };
            AuthConfigProvider provider = factory.getConfigProvider(msgLayer, appContext, null);

            boolean result = factory.removeRegistration(regIDs[0]);
            if (result)
                writeLine(sb, "Successfully removed provider registration.");
            else
                writeLine(sb, "Failed to remove registered provider for message layer=" + msgLayer + " and application context=" + appContext);

        }

        /**
         * Remove persistent provider registration associated with the given msgLayer and appContext.
         * Print "Success" to the response object if the removal is successful.
         * Print a failure message to the response object if the registration does not exist.
         *
         * @param out
         * @param factory
         * @param msgLayer
         * @param appContext
         */
        private void removeProvider(StringBuffer sb, AuthConfigFactory factory, String msgLayer, String appContext) {

            String[] regIDs = { "RegIDNotSet" };
            AuthConfigProvider provider = factory.getConfigProvider(msgLayer, appContext, null);

            if (provider != null) {
                regIDs = factory.getRegistrationIDs(provider);
            }

            boolean result = factory.removeRegistration(regIDs[0]);
            if (result)
                writeLine(sb, "Successfully removed provider registration.");
            else
                writeLine(sb, "Failed to remove registered provider for message layer=" + msgLayer + " and application context=" + appContext);

        }

        private void error(StringBuffer sb) {
            writeLine(sb, "Invalid servlet query.");
        }
    }

    public class ProcessServlet30MethodStep implements BaseServletStep {

        Map<String, String> parseQuery(String query, StringBuffer sb) {
            Scanner commands = new Scanner(query).useDelimiter("[,;\\|&]");
            Map<String, String> parms = new TreeMap<String, String>();
            while (commands.hasNext()) {
                String cmd = commands.next();
                int i = cmd.indexOf('=');
                if (i < 1) {
                    writeLine(sb, "Ignoring command: " + cmd + ", command syntax must be cmdname=parm");
                } else {
                    parms.put(cmd.substring(0, i).trim(), cmd.substring(i + 1));
                }
            }
            return parms;
        }

        @Override
        public void invoke(BaseServletParms p) throws Exception {
            String query = p.getRequest().getQueryString();
            if (query == null || query.isEmpty()) {
                throw new ServletException("A query string must be specified, e.g. ?method=login,user=id,password=pwd");
            }
            Map<String, String> args = parseQuery(query, p.getBuffer());
            String method = args.get("method");
            String user = args.get("user");
            String pwd = args.get("password");
            Principal principal;
            String remoteUser;

            writeLine(p.getBuffer(), "\nQueryString: " + query);

            try {
                if (method.equalsIgnoreCase("LOGIN")) {
                    // When protected=true, logout() before login()
                    if ("true".equalsIgnoreCase(args.get("protected"))) {
                        p.getRequest().logout();
                    }
                    p.getRequest().login(user, pwd);
                    writeLine(p.getBuffer(), "Servlet login() method invoked.\n");
                } else if (method.equalsIgnoreCase("LOGOUT")) {
                    p.getRequest().logout();
//                    principal = p.getRequest().getUserPrincipal();
//                    remoteUser = p.getRequest().getRemoteUser();
                    writeLine(p.getBuffer(), "Servlet logout() method invoked.\n");
                } else if (method.startsWith("authenticate")) {
                    // When protected=true, logout() before authenticate()
                    if ("true".equalsIgnoreCase(args.get("protected"))) {
                        p.getRequest().logout();
                    }
                    boolean isAuthenticated = p.getRequest().authenticate(p.getResponse());
//                    principal = p.getRequest().getUserPrincipal();
//                    remoteUser = p.getRequest().getRemoteUser();
                    writeLine(p.getBuffer(), "Servlet authenticate invoked for " + method + "  \nisAuthenticated: " + isAuthenticated);
                }
            } catch (ServletException se) {
                writeLine(p.getBuffer(), "ServletExecption: " + se);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    protected void createDefaultFlow() {
        mySteps.add(new WriteRequestBasicsStep());
        mySteps.add(new WritePrincipalStep());
        mySteps.add(new WriteRolesStep());
        mySteps.add(new WriteSubjectStep());
        mySteps.add(new WriteRunAsSubjectStep());
    }

    protected void performCustomTasks(String type, HttpServletRequest req, HttpServletResponse res,
                                      StringBuffer sb) throws Exception {
        if (mySteps.isEmpty())
            throw new IllegalArgumentException("No steps specified for test flow");

        BaseServletParms parms = new BaseServletParms();
        parms.setType(type);
        parms.setRequest(req);
        parms.setResponse(res);
        parms.setBuffer(sb);
        try {
            for (BaseServletStep step : mySteps) {

                step.invoke(parms);
            }
        } catch (Throwable t) {
            parms.setError(t);
            myErrorStep.invoke(parms);
        }
    }

    private Subject fetchSubject() throws WSSecurityException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Subject>() {
                @Override
                public Subject run() throws Exception {
                    // Get the CallerSubject
                    return WSSubject.getCallerSubject();
                }
            });
        } catch (PrivilegedActionException e) {
            /*
             * Re-throw the cause. It is either the only named exception from
             * WSSubject.getCallerSubject(), WSSecurityException, or it is a
             * RuntimeException.
             */
            Exception cause = e.getException();
            if (cause instanceof WSSecurityException) {
                throw (WSSecurityException) cause;
            } else {
                throw (RuntimeException) cause;
            }
        }
    }

    /**
     * "Writes" the msg out to the client and System.out. This actually appends the msg
     * and a line delimiters to the running StringBuffer. This is necessary
     * because if too much data is written to the PrintWriter before the
     * logic is done, a flush() may get called and lock out changes to the
     * response.
     *
     * @param sb  Running StringBuffer
     * @param msg Message to write
     */
    protected void writeLine(StringBuffer sb, String msg) {
        sb.append(msg + "\n");
        log.info(msg);
    }

    protected Hashtable<String, ?> getHashtable(Set<Object> creds, String[] properties) {
        for (Object cred : creds) {
            if (cred instanceof Hashtable) {
                for (int j = 0; j < properties.length; j++) {
                    if (((Hashtable) cred).get(properties[j]) != null)
                        return (Hashtable) cred;
                }
            }
        }
        return null;
    }

}
