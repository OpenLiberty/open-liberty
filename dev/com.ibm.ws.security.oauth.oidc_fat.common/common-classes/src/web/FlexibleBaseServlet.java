/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.wsspi.security.token.SingleSignonToken;
      
/**
 * Base servlet which all of our test servlets extend.
 */
public abstract class FlexibleBaseServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private String servletName;
    protected List<BaseServletStep> mySteps = new ArrayList<BaseServletStep>();
    protected BaseServletStep myErrorStep =  new WriteErrorStep();

    FlexibleBaseServlet(String servletName) {
        this.servletName = servletName;
    }
    

    protected void updateServletName(String servletName) {
        this.servletName = servletName;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {
        if ("CUSTOM".equalsIgnoreCase(req.getMethod()))
            doCustom(req, res);
        else
            super.service(req, res);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
        handleRequest("GET", req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
        handleRequest("POST", req, resp);
    }

    private void doCustom(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
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
    protected void handleRequest(String type, HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
        PrintWriter writer = resp.getWriter();
        writer.println("ServletName: " + servletName);
        writer.println("Request type: " + type);

        StringBuffer sb = new StringBuffer();
        try {
            performTask(type, req, resp, sb);
        } catch (Throwable t) {
            t.printStackTrace(writer);
        }

        writer.write(sb.toString());
        writer.flush();
        writer.close();
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
    protected void performTask(String type,
    						   HttpServletRequest req,
                               HttpServletResponse resp, StringBuffer sb)
                    throws Exception {
        performCustomTasks(type, req, sb);
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

    
    class BaseServletParms{
    	private String myType;
    	private HttpServletRequest myRequest;
    	private StringBuffer myBuffer;
    	private String myAccessToken;
       	private Throwable myError;
    	
    	public String getType(){
    		return myType;
    	}
    	public void setType(String type){
    		myType = type;
    	}
    	public HttpServletRequest getRequest() {
    		return myRequest;
    	}
    	public void setRequest(HttpServletRequest req) {
    		myRequest = req;
    	}
    	public StringBuffer getBuffer(){
    		return myBuffer;
    	}
    	public void setBuffer(StringBuffer sb){
    		myBuffer = sb;
    	}
     	public Subject getSubject() throws Exception {
    		return fetchSubject();
    	}
     	public String getAccessToken(){
    		return myAccessToken;
    	}
    	public void setAccessToken(String accessToken){
    		myAccessToken = accessToken;
    	}
     	public Throwable getError(){
    		return myError;
    	}
    	public void setError(Throwable t){
    		myError = t;
    	}
    }
    
    interface BaseServletStep {
    	void invoke (BaseServletParms p) throws Exception;
    }
    

    
    class WriteParametersStep implements BaseServletStep{

		@Override
		public void invoke(BaseServletParms p) {

		if (!p.getType().equals("POST")) {
    		// print all the parms 
    		Enumeration e = p.getRequest().getParameterNames();
    		if ( e.hasMoreElements() )
    		{
    			writeLine(p.getBuffer(), "All Parameters");
    			while ( e.hasMoreElements() )
    			{
    				String name = (String)e.nextElement();
    				writeLine(p.getBuffer(), "Param: " + name + " with value: " + p.getRequest().getParameter(name));
    			}
    		}
    	} else {

    		try {
    			Collection<Part> myParts = p.getRequest().getParts();
    			writeLine(p.getBuffer(), "All Parameters");
    			for (Iterator<Part> myIt = myParts.iterator(); myIt.hasNext();) {
    				Part pt = myIt.next();
    				InputStream inst = pt.getInputStream() ;
    				StringBuilder inputStringBuilder = new StringBuilder();
    				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inst, "UTF-8"));
    				String line = bufferedReader.readLine();
    				Boolean firstOneDone = false ;
    				while(line != null){
    					if (firstOneDone) {
    						inputStringBuilder.append('\n');
    						firstOneDone = true ;
    					}
    					inputStringBuilder.append(line);
    					line = bufferedReader.readLine();
    				}
    				writeLine(p.getBuffer(), "Param: " + pt.getName() + " with value: " + inputStringBuilder.toString());
     			}
    		} catch (Exception exc) {
    			writeLine(p.getBuffer(), "Exception occurred: " + exc.toString()) ;

    		}
    	}
		}
    	
    }
    
    class WriteRequestBasicsStep implements BaseServletStep{

		@Override
		public void invoke(BaseServletParms p) {
				writeLine(p.getBuffer(), "getAuthType: " + p.getRequest().getAuthType());
				writeLine(p.getBuffer(), "getRequestURL: " + p.getRequest().getRequestURL().toString());
		}
    }
    
    class WritePrincipalStep implements BaseServletStep{

		@Override
		public void invoke(BaseServletParms p)  throws Exception {
		        writeLine(p.getBuffer(), "getRemoteUser: " + p.getRequest().getRemoteUser());
		        writeLine(p.getBuffer(), "getUserPrincipal: " + p.getRequest().getUserPrincipal());
		        
		        if (p.getRequest().getUserPrincipal() != null) {
		            writeLine(p.getBuffer(), "getUserPrincipal().getName(): "
		                          + p.getRequest().getUserPrincipal().getName());
		        }
			}
    }
    class WriteRolesStep implements BaseServletStep{

		@Override
		public void invoke(BaseServletParms p)  throws Exception {

				writeLine(p.getBuffer(), "isUserInRole(Employee): "
		                      + p.getRequest().isUserInRole("Employee"));
		        writeLine(p.getBuffer(), "isUserInRole(Manager): " + p.getRequest().isUserInRole("Manager"));
		        String role = p.getRequest().getParameter("role");
		        if (role == null) {
		            writeLine(p.getBuffer(), "You can customize the isUserInRole call with the follow paramter: ?role=name");
		        }
		        writeLine(p.getBuffer(), "isUserInRole(" + role + "): " + p.getRequest().isUserInRole(role));
			}	

    }
    class WriteCookiesStep implements BaseServletStep{

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
    class WriteSubjectStep implements BaseServletStep{

		@Override
		public void invoke(BaseServletParms p) throws Exception {
				writeLine(p.getBuffer(), "CallerSubject: " + p.getSubject());
			}	
    }
    class WritePublicCredentialsStep implements BaseServletStep{

		@Override
		public void invoke(BaseServletParms p)  throws Exception {

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
    class WriteRunAsSubjectStep implements BaseServletStep{

		@Override
		public void invoke(BaseServletParms p) throws Exception  {
				// getInvocationSubject for RunAs tests
				Subject runAsSubject = WSSubject.getRunAsSubject();
				writeLine(p.getBuffer(), "RunAs subject: " + runAsSubject);
			}
	    }
    class WriteAccessTokenStep implements BaseServletStep{

		@Override
		public void invoke(BaseServletParms p) throws Exception {
			String accessToken = null;
			if (p.getSubject() != null) {
				String[] properties = { "access_token" };
			  Set<Object> list_public = p.getSubject().getPublicCredentials();
			  Hashtable<String, ?> hashtableFromPublic = getHashtable(list_public, properties);
			  if (hashtableFromPublic != null) {
			        accessToken = (String) hashtableFromPublic.get("access_token");
			        writeLine(p.getBuffer(),"Access token value from public credential: " + accessToken);
			        p.setAccessToken(accessToken);
			    }
			}
		}
    }
 
    abstract class ProcessStep implements BaseServletStep {
		protected HttpURLConnection prepareConnection(BaseServletParms p, String rawUrl)
				throws MalformedURLException, IOException, ProtocolException {
				    		URL url = new URL(rawUrl);
				    		writeLine(p.getBuffer(),"HttpURLConnection URL is set to: " + url);
				        	HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				        	writeLine(p.getBuffer(),"HttpURLConnection successfully opened the connection to URL " + url);
				        	connection.setRequestMethod("GET");
				        	writeLine(p.getBuffer(),"HttpURLConnection set request method to GET");
				        	connection.setDoOutput(true);
			return connection;
		}
		
		protected void connect(BaseServletParms p, HttpURLConnection connection)
				throws IOException {
			connection.connect();
			writeLine(p.getBuffer(),"HttpURLConnection successfully completed connection.connect");
		}

		protected void writeToConnection(BaseServletParms p, HttpURLConnection connection, String message
				) throws IOException {
			OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
			writer.write(message);
			writer.flush();
			writer.close();
			writeLine(p.getBuffer(),"HttpURLConnection successfully completed Writer.write");
		}
		protected void processResponse(BaseServletParms p,
				HttpURLConnection connection) throws IOException,
				UnsupportedEncodingException {
			InputStream responseStream = null;
			int statusCode = connection.getResponseCode();
			writeLine(p.getBuffer(),"HttpURLConnection status code from connect: " + statusCode);
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
    
	class ProcessAccessTokenStep extends ProcessStep {

		@Override
		public void invoke(BaseServletParms p) throws Exception {
			writeLine(p.getBuffer(),"Entering ProcessAccessTokenStep to forward access token to second servlet");

			String webServerPort = p.getRequest().getParameter("port");
			writeLine(p.getBuffer(), "Passed in from URL: port:["+ webServerPort + "]");
			
			String opServerPort = p.getRequest().getParameter("opPort");
			writeLine(p.getBuffer(), "Passed in from URL: opPort:["+ opServerPort + "]");

			if (webServerPort == null) {
				writeLine(p.getBuffer(),
						"Usage: SimpleServlet?port=<httpPort>&opPort=<httpPort>");
				writeLine(p.getBuffer(), "Using default HTTP port "
						+ Constants.DEFAULT_HTTP_PORT);
				webServerPort = Constants.DEFAULT_HTTP_PORT;
			}
			
			if (opServerPort == null) {
				writeLine(p.getBuffer(),
						"Usage: SimpleServlet?port=<httpPort>&opPort=<httpPort>");
				writeLine(p.getBuffer(), "Using default OP HTTP port 8040");
				opServerPort = "8040";
			}

			if (p.getAccessToken() != null) {
				HttpURLConnection connection = prepareConnection(p,"http://localhost:" + webServerPort + "/formlogine2e2/SimpleServlet2?opPort=" + opServerPort);
				writeToConnection(p, connection, "token=" + p.getAccessToken());
				connect(p, connection);
				processResponse(p, connection);
			}
		}
	}

	class ProcessIntrospectionStep extends ProcessStep {
		@Override
		public void invoke(BaseServletParms p) throws Exception {
			writeLine(p.getBuffer(),"Entering ProcessIntrospectionStep to invoke introspection endpoint");
			
			String opServerPort = p.getRequest().getParameter("opPort");
			writeLine(p.getBuffer(), "Passed in from URL: opPort:["+ opServerPort + "]");
			
			if (opServerPort == null) {
				writeLine(p.getBuffer(),"Usage: SimpleServlet2?token=<accesstoken>&opPort=<httpPort>");
				writeLine(p.getBuffer(), "No opPort specified. Using default OP HTTP port 8040");
				opServerPort = "8040";
			}

			String accessToken = p.getRequest().getParameter("token");
			writeLine(p.getBuffer(), "Passed in from URL: access_token:["+ accessToken + "]");

			if (accessToken == null) {
				writeLine(p.getBuffer(),"Usage: SimpleServlet2?token=<accesstoken>&opPort=<httpPort>");
			}

			if (accessToken != null) {
				HttpURLConnection connection = prepareConnection(p,"http://localhost:" + opServerPort + "/oidc/providers/OidcConfigSample/introspect");
				//TO DO: client_id and client_secret are hardcoded until APIs are available for getting the values.
				writeToConnection(p, connection, "client_id=client01" + "&client_secret=secret" + "&token=" + accessToken);
				connect(p, connection);
				processResponse(p, connection);
			}
		}
	}
    class WriteErrorStep implements BaseServletStep
    {

		@Override
		public void invoke(BaseServletParms p) throws Exception {
			if (p.getError() != null) {
				if (p.getError() instanceof NoClassDefFoundError){
					// For OSGI App testing (EBA file), we expect this exception for all
					// packages that are not public
					writeLine(p.getBuffer(), "NoClassDefFoundError for SubjectManager: " + p.getError());

				} else {
					p.getError().printStackTrace();
				}
			}
			
		}
    	
    }


   protected void createDefaultFlow () {
   	mySteps.add(new WriteParametersStep());
 	mySteps.add(new WriteRequestBasicsStep());
 	mySteps.add(new WritePrincipalStep());
 	mySteps.add(new WriteRolesStep());
 	mySteps.add(new WriteCookiesStep());
 	mySteps.add(new WriteSubjectStep());
 	mySteps.add(new WritePublicCredentialsStep());
 	mySteps.add(new WriteRunAsSubjectStep());
 	mySteps.add(new WriteAccessTokenStep());  
   }
   
	protected void performCustomTasks(String type, HttpServletRequest req,
			StringBuffer sb) throws Exception {
		if (mySteps.isEmpty()) 
			throw new IllegalArgumentException("No steps specified for test flow");

		BaseServletParms parms = new BaseServletParms();
		parms.setType(type);
		parms.setRequest(req);
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

	private String writeAccessToken(StringBuffer sb, Subject callerSubject) {
		// get access_token from public credentials
		String accessToken = null;
		if (callerSubject != null) {
			String[] properties = { "access_token" };
		  Set<Object> list_public = callerSubject.getPublicCredentials();
		  Hashtable<String, ?> hashtableFromPublic = getHashtable(list_public, properties);
		  if (hashtableFromPublic != null) {
		        accessToken = (String) hashtableFromPublic.get("access_token");
		        writeLine(sb,"Access token value from public credential: " + accessToken);
		    }
		}
		return accessToken;
	}


	private Subject fetchSubject() throws WSSecurityException {
		// Get the CallerSubject
		Subject callerSubject = WSSubject.getCallerSubject();
		return callerSubject;
	}

    /**
     * "Writes" the msg out to the client. This actually appends the msg
     * and a line delimiters to the running StringBuffer. This is necessary
     * because if too much data is written to the PrintWriter before the
     * logic is done, a flush() may get called and lock out changes to the
     * response.
     * 
     * @param sb Running StringBuffer
     * @param msg Message to write
     */
    void writeLine(StringBuffer sb, String msg) {
        sb.append(msg + "\n");
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
