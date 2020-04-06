/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javax.servlet.sip;

import java.io.IOException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;


/**
 * Provides an abstract class to be subclassed to create a SIP servlet.
 *
 * <p>This class receives incoming messages through the
 * {@link #service service} method. This method calls
 * {@link #doRequest doRequest} or {@link #doResponse doResponse}
 * for incoming requests and responses, respectively. These two methods
 * in turn dispatch on request method or status code to one of the
 * following methods:
 * 
 * <ul>
 * <li>{@link #doInvite doInvite}   - for SIP INVITE requests
 * <li>{@link #doAck doAck}         - for SIP ACK requests
 * <li>{@link #doOptions doOptions} - for SIP OPTIONS requests
 * <li>{@link #doBye doBye}         - for SIP BYE requests
 * <li>{@link #doCancel doCancel}   - for SIP CANCEL requests
 * <li>{@link #doRegister doRegister} - for SIP REGISTER requests
 * <li>{@link #doSubscribe doSubscribe} - for SIP SUBSCRIBE requests
 * <li>{@link #doNotify doNotify}   - for SIP NOTIFY requests
 * <li>{@link #doMessage doMessage} - for SIP MESSAGE requests
 * <li>{@link #doInfo doInfo}       - for SIP INFO requests
 * <li>{@link #doPrack doPrack}     - for SIP PRACK requests
 * 
 * <li>{@link #doProvisionalResponse doProvisionalResponse}
 *      - for SIP 1xx informational responses
 * <li>{@link #doSuccessResponse doSuccessResponse} - for SIP 2xx responses
 * <li>{@link #doRedirectResponse doRedirectResponse}
 *      - for SIP 3xx responses
 * <li>{@link #doErrorResponse doErrorResponse}
 *      - for SIP 4xx, 5xx, and 6xx responses
 * </ul>
 * 
 * <p>The default implementation of <code>doAck</code>, <code>doCancel</code>
 * and all the response handling methods are empty. All other request handling
 * methods reject the request with a 500 error response.
 * 
 * <p>Subclasses of <code>SipServlet</code> will usually override one
 * or more of these methods.
 * 
 * @see <a href="http://www.ietf.org/rfc/rfc2976.txt">RFC 2976, The SIP INFO Method</a>
 * @see <a href="http://www.ietf.org/rfc/rfc3262.txt">RFC 3262, Reliability of Provisional Responses in the Session Initiation Protocol (SIP)</a>
 * @see <a href="http://www.ietf.org/rfc/rfc3265.txt">RFC 3265, Session Initiation Protocol (SIP)-Specific Event Notification</a>
 * @see <a href="http://www.ietf.org/internet-drafts/draft-rosenberg-impp-im-00.txt">SIP Extensions for Instant Messaging</a>
 */
public abstract class SipServlet extends GenericServlet
{
	
    /**
	 * Add in the version uid from the last release
	 */
	private static final long serialVersionUID = 3463495171197805860L;

	/**
     * The string "javax.servlet.sip.outboundInterfaces". 
     * This is the name of the ServletContext attribute whose 
     * value is a list of SipURI objects which represent the 
     * available outbound interfaces for sending SIP requests. 
     * 
     * On a multihomed machine, a specific outbound interface can 
     * be selected for sending requests by calling the the
     *  
     * SipSession.setOutboundInterface(java.net.InetSocketAddress) or 
     * Proxy.setOutboundInterface(java.net.InetSocketAddress) with an 
     * available interface address chosen from this list.
     */
    public static final String OUTBOUND_INTERFACES =  "javax.servlet.sip.outboundInterfaces";
    
    /**
     * @deprecated. in favor of using the "javax.servlet.sip.supported" attribute
     * The string "javax.servlet.sip.100rel". This is the name of the ServletContext 
     * attribute whose value suggests whether the container supports the 100rel extension 
     * i.e. RFC 3262. 
     */
    public static final String PRACK_SUPPORTED = "javax.servlet.sip.100rel";
	
	/**
     * The string "javax.servlet.sip.SipFactory". This is the name of
     * the <code>ServletContext</code> attribute whose value is an
     * instance of the <code>SipFactory</code> interface.
     * 
     * @see SipFactory
     */
    public static final String SIP_FACTORY = "javax.servlet.sip.SipFactory";
	
    /**
     * The string "javax.servlet.sip.SipServletUtil". This is the name of the 
     * ServletContext attribute whose value is the @{link SipServerUtil} class 
     * providing support for the implementation.
     */
    private static final String SIP_SERVLET_UTIL = "javax.servlet.sip.SipServletUtil";

    /**
     * The string "javax.servlet.sip.SipSessionsUtil". This is the name of the 
     * ServletContext attribute whose value is the @{link Sessions} utility class 
     * providing support for converged SIP/HTTP applications.
     *  
     */
    public static final String SIP_SESSIONS_UTIL = "javax.servlet.sip.SipSessionsUtil";
    
    
    /**
     * The string "javax.servlet.sip.supported". This is the name of
     * the <code>ServletContext</code> attribute whose value is a
     * <code>List</code> containing the names of SIP extensions supported
     * by the container.
     */
    public static final String SUPPORTED = "javax.servlet.sip.supported";

    
    /**
     * The string "javax.servlet.sip.supportedRfcs". This is the name of the ServletContext 
     * attribute whose value is a List containing the RFC numbers represented as Strings 
     * of SIP RFCs supported by the container. For e.g., if the container supports RFC 3261, 
     * RFC 3262 and RFC 3265, the List associated with this attribute should contain the 
     * Strings "3261", "3262" and "3265". 
     */
    public static final java.lang.String SUPPORTED_RFCs = "javax.servlet.sip.supportedRfcs";
    
    /**
     * The string "javax.servlet.sip.TimerService". This is the name of
     * the <code>ServletContext</code> attribute whose value is an
     * instance of the <code>TimerService</code> interface.
     * 
     * @see TimerService
     */
    public static final String TIMER_SERVICE = "javax.servlet.sip.TimerService";
    
    /**
     * Special attribute that will hold the real application name
     */
    public static final String APP_NAME_ATTRIBUTE = "com.ibm.ws.sip.container.app.name";
    
    
    /**
     * Special attribute that will hold the real application name
     */
    public static final String DOMAIN_RESOLVER_ATTRIBUTE = "com.ibm.ws.sip.container.domain.resolver";
    

    /**
     * Servlet Config associated with this Siplet.
     */
    private ServletConfig m_servletConfig;

    
   
    /**
     * Invoked by the server (via the service method) to handle incoming
     * ACK requests.
     * 
     * <p>The default implementation is empty and must be overridden by
     * subclasses to do something useful.
     * 
     * @param req   represents the incoming SIP ACK request
     * @throws ServletException if an exception occurs that interferes
     *                          with the servlet's normal operation
     * @throws IOException if an input or output exception occurs
     */
    protected void doAck(SipServletRequest req)
        throws ServletException, IOException
    {
    }

    /**
     * Invoked by the server to handle intermediate final responses only if this Servlet 
     * behaves as a proxy.
     *  
     * The default implementation is empty and must be overridden by subclasses to 
     * handle intermediate final responses received on a ProxyBranch. 
     * 
     * @param req   represents the incoming SIP BYE request
     * @throws ServletException if an exception occurs that interferes
     *                          with the servlet's normal operation
     * @throws IOException if an input or output exception occurs
     */
    protected void doBranchResponse(SipServletResponse resp)
        throws ServletException, IOException
    {
    	
    }
    
    
    /**
     * Invoked by the server (via the service method) to handle incoming
     * BYE requests.
     * 
     * <p>The default implementation is empty and must be overridden by
     * subclasses to do something useful.
     * 
     * @param req   represents the incoming SIP BYE request
     * @throws ServletException if an exception occurs that interferes
     *                          with the servlet's normal operation
     * @throws IOException if an input or output exception occurs
     */
    protected void doBye(SipServletRequest req)
        throws ServletException, IOException
    {
        if (req.isInitial())
            notHandled(req);
    }



    /**
     * Invoked by the server (via the service method) to handle incoming
     * CANCEL requests.
     * 
     * <p>The default implementation is empty and must be overridden by
     * subclasses to do something useful.
     * 
     * @param req   represents the incoming SIP CANCEL request
     * @throws ServletException if an exception occurs that interferes
     *                          with the servlet's normal operation
     * @throws IOException if an input or output exception occurs
     */
    protected void doCancel(SipServletRequest req)
        throws ServletException, IOException
    {
    }

    /**
     * Invoked by the server (via the doResponse method) to handle incoming
     * 4xx - 6xx class responses.
     * 
     * <p>The default implementation is empty and must be overridden by
     * subclasses to do something useful.
     * 
     * @param resp  the response object
     * @throws ServletException if an exception occurs that interferes
     *                          with the servlet's normal operation
     * @throws IOException if an input or output exception occurs
     */
    protected void doErrorResponse(SipServletResponse resp)
        throws ServletException, IOException
    {
    }

    /**
     * Invoked by the server (via the service method) to handle incoming
     * INFO requests.
     * 
     * <p>The default implementation is empty and must be overridden by
     * subclasses to do something useful.
     * 
     * @param req   represents the incoming SIP INFO request
     * @throws ServletException if an exception occurs that interferes
     *                          with the servlet's normal operation
     * @throws IOException if an input or output exception occurs
     */
    protected void doInfo(SipServletRequest req)
        throws ServletException, IOException
    {
        if (req.isInitial())
            notHandled(req);
    }

    /**
     * Invoked by the server (via the service method) to handle incoming
     * INVITE requests.
     * 
     * <p>The default implementation is empty and must be overridden by
     * subclasses to do something useful.
     * 
     * @param req   represents the incoming SIP INVITE request
     * @throws ServletException if an exception occurs that interferes
     *                          with the servlet's normal operation
     * @throws IOException if an input or output exception occurs
     */
    protected void doInvite(SipServletRequest req)
        throws ServletException, IOException
    {
        if (req.isInitial())
            notHandled(req);
    }

    /**
     * Invoked by the server (via the service method) to handle incoming
     * MESSAGE requests.
     * 
     * <p>The default implementation is empty and must be overridden by
     * subclasses to do something useful.
     * 
     * @param req   represents the incoming SIP MESSAGE request
     * @throws ServletException if an exception occurs that interferes
     *                          with the servlet's normal operation
     * @throws IOException if an input or output exception occurs
     */
    protected void doMessage(SipServletRequest req)
        throws ServletException, IOException
    {
        if (req.isInitial())
            notHandled(req);
    }

    /**
     * Invoked by the server (via the service method) to handle incoming
     * NOTIFY requests.
     * 
     * <p>The default implementation is empty and must be overridden by
     * subclasses to do something useful.
     * 
     * @param req   represents the incoming SIP NOTIFY request
     * @throws ServletException if an exception occurs that interferes
     *                          with the servlet's normal operation
     * @throws IOException if an input or output exception occurs
     */
    protected void doNotify(SipServletRequest req)
        throws ServletException, IOException
    {
        if (req.isInitial())
            notHandled(req);
    }

    /**
     * Invoked by the server (via the service method) to handle incoming
     * OPTIONS requests.
     * 
     * <p>The default implementation is empty and must be overridden by
     * subclasses to do something useful.
     * 
     * @param req   represents the incoming SIP OPTIONS request
     * @throws ServletException if an exception occurs that interferes
     *                          with the servlet's normal operation
     * @throws IOException if an input or output exception occurs
     */
    protected void doOptions(SipServletRequest req)
        throws ServletException, IOException
    {
        if (req.isInitial())
            notHandled(req);
    }

    /**
     * Invoked by the server (via the service method) to handle incoming
     * PRACK requests.
     * 
     * <p>The default implementation is empty and must be overridden by
     * subclasses to do something useful.
     * 
     * @param req   represents the incoming SIP PRACK request
     * @throws ServletException if an exception occurs that interferes
     *                          with the servlet's normal operation
     * @throws IOException if an input or output exception occurs
     */
    protected void doPrack(SipServletRequest req)
        throws ServletException, IOException
    {
        if (req.isInitial())
            notHandled(req);
    }

    /**
     * Invoked by the server (via the doResponse method) to handle incoming
     * 1xx class responses.
     * 
     * <p>The default implementation is empty and must be overridden by
     * subclasses to do something useful.
     * 
     * @param resp  the response object
     * @throws ServletException if an exception occurs that interferes
     *                          with the servlet's normal operation
     * @throws IOException if an input or output exception occurs
     */
    protected void doProvisionalResponse(SipServletResponse resp)
        throws ServletException, IOException
    {
    }
	
	/**
	 * Invoked by the server (via the service method) to handle incoming
	 * PUBLISH requests.
	 * 
	 * <p>The default implementation is empty and must be overridden by
	 * subclasses to do something useful.
	 * 
	 * @param req   represents the incoming SIP PUBLISH request
	 * @throws ServletException if an exception occurs that interferes
	 *                          with the servlet's normal operation
	 * @throws IOException if an input or output exception occurs
	 */
	protected void doPublish(SipServletRequest req)
		throws ServletException, IOException
	{
		if (req.isInitial())
			notHandled(req);
	}

    /**
     * Invoked by the server to notify the servlet of incoming 3xx class
     * responses.
     * 
     * <p>The default implementation is empty and must be overridden by
     * subclasses to do something useful.
     * 
     * @param resp  the response object
     * @throws ServletException if an exception occurs that interferes
     *                          with the servlet's normal operation
     * @throws IOException if an input or output exception occurs
     */
    protected void doRedirectResponse(SipServletResponse resp)
        throws ServletException, IOException
    {
    }

    /**
     * Invoked by the server (via the service method) to handle incoming 
     * REFER requests. 
     *  
     * <p>The default implementation is empty and must be overridden by
     * subclasses to do something useful.
     * 
     * @param req   represents the incoming SIP REGISTER request
     * @throws ServletException if an exception occurs that interferes
     *                          with the servlet's normal operation
     * @throws IOException if an input or output exception occurs
     */
    protected void doRefer(SipServletRequest req) 
    	throws ServletException, IOException{
    	/*TODO*/
    }
    
    /**
     * Invoked by the server (via the service method) to handle incoming
     * REGISTER requests.
     * 
     * <p>The default implementation is empty and must be overridden by
     * subclasses to do something useful.
     * 
     * @param req   represents the incoming SIP REGISTER request
     * @throws ServletException if an exception occurs that interferes
     *                          with the servlet's normal operation
     * @throws IOException if an input or output exception occurs
     */
    protected void doRegister(SipServletRequest req)
        throws ServletException, IOException
    {
        if (req.isInitial())
            notHandled(req);
    } 

    /**
     * Invoked to handle incoming requests. This method dispatched requests
     * to one of the doXxx methods where Xxx is the SIP method used in the
     * request. Servlets will not usually need to override this method. 
     * 
     * @param req   represents the incoming SIP request
     * @throws ServletException if an exception occurs that interferes
     *                          with the servlet's normal operation
     * @throws IOException if an input or output exception occurs
     */
    protected void doRequest(SipServletRequest req)
        throws ServletException, IOException
    {
        String m = req.getMethod();

        if ("INVITE".equals(m))
        {
            doInvite(req);
        }
        else if ("ACK".equals(m))
        {
            doAck(req);
        }
        else if ("OPTIONS".equals(m))
        {
            doOptions(req);
        }
        else if ("BYE".equals(m))
        {
            doBye(req);
        }
        else if ("CANCEL".equals(m))
        {
            doCancel(req);
        }
        else if ("REGISTER".equals(m))
        {
            doRegister(req);
        }
        else if ("SUBSCRIBE".equals(m))
        {
            doSubscribe(req);
        }
        else if ("NOTIFY".equals(m))
        {
            doNotify(req);
        }
        else if ("MESSAGE".equals(m))
        {
            doMessage(req);
        }
        else if ("INFO".equals(m))
        {
            doInfo(req);
        }
        else if ("PRACK".equals(m))
        {
            doPrack(req);
        }
		else if ("PUBLISH".equals(m))
		{
			doPublish(req);
		}
		else if ("REFER".equals(m))
		{
			doRefer(req);
		}
		else if ("UPDATE".equals(m))
		{
			doUpdate(req);
		}
        else
        {
            if (req.isInitial())
                notHandled(req);
        }
    }

    /**
     *  This Wrapper is for handling Exception not handled by Application
     * //SPR #IESR6AGKQA
     * @param req
     * @throws ServletException
     * @throws IOException
     */
    private void doRequestWrapper(SipServletRequest req) throws ServletException, IOException{
    	try{
    		doRequest(req);
    	}catch(TooManyHopsException te){
    	    logExceptionToSessionLog(req, te);
    	    //Since a SIP Servlet container cannot know a priori whether an 
            //application will proxy a request or not, it cannot perform the 
            //Max-Forwards check before invoking the application. Instead, the 
            //container performs the check when the application invokes getProxy() 
            //on a SipServletRequest, and if Max-Forwards is 0 a TooManyHopsException 
            //is thrown. If unhandled by the application, this will be caught by 
            //the container which must then generate a 483 (Too many hops) error response.
            
    		req.createResponse(SipServletResponse.SC_TOO_MANY_HOPS).send();
    	}
    	catch(ServletException e)
    	{
    	    logExceptionToSessionLog(req, e);
    	    throw e;
    	}
    	catch(IOException e)
    	{
    	    logExceptionToSessionLog(req, e);
    	    throw e;
    	}
    }

    // response callbacks

    /**
     * Invoked to handle incoming responses. This method dispatched responses
     * to one of the {@link #doProvisionalResponse doProvisionalResponse()},
     * {@link #doErrorResponse doErrorResponse()}, 
     * {@link #doSuccessResponse doSuccessResponse()}.
     * Servlets will not usually need to override this method. 
     * 
     * @param resp  the response object
     * @throws ServletException if an exception occurs that interferes
     *                          with the servlet's normal operation
     * @throws IOException if an input or output exception occurs
     */
    protected void doResponse(SipServletResponse resp)
        throws ServletException, IOException
    {
        
        int status = resp.getStatus();
        
        if (status < 200)
        {
            doProvisionalResponse(resp);
        }
        else if (status < 300)
        {
            doSuccessResponse(resp);
        }
        else if (status < 400)
        {
            doRedirectResponse(resp);
        }
        else
        {
            doErrorResponse(resp);
        }
    }

    /**
     *  This Wrapper is for handling Exception not handled by Application
     * //SPR #IESR6AGKQA
     * @param res
     * @throws ServletException
     * @throws IOException
     */
    private void doResponseWrapper(SipServletResponse res) throws ServletException, IOException{
    	try{
    		if (res.isBranchResponse()) {
        		doBranchResponse(res);
        		//JSR 289 10.2.4.2
        		//"Note that if the doBranchResponse() is not overridden then 
        		//doResponse() method will be invoked only for the best final response as before.""
        	}
            else{ 
            	doResponse(res);
            }
    	}
    	catch(ServletException e)
    	{
    	    logExceptionToSessionLog(res, e);
    	    throw e;
    	}
    	catch(IOException e)
    	{
    	    logExceptionToSessionLog(res, e);
    	    throw e;
    	}
    }

    /**
     * Invoked by the server (via the service method) to handle incoming
     * SUBSCRIBE requests.
     * 
     * <p>The default implementation is empty and must be overridden by
     * subclasses to do something useful.
     * 
     * @param req   represents the incoming SIP SUBSCRIBE request
     * @throws ServletException if an exception occurs that interferes
     *                          with the servlet's normal operation
     * @throws IOException if an input or output exception occurs
     */
    protected void doSubscribe(SipServletRequest req)
        throws ServletException, IOException
    {
        if (req.isInitial())
            notHandled(req);
    }

    /**
     * Invoked by the server (via the doResponse method) to handle incoming
     * 2xx class responses.
     * 
     * <p>The default implementation is empty and must be overridden by
     * subclasses to do something useful.
     * 
     * @param resp  the response object
     * @throws ServletException if an exception occurs that interferes
     *                          with the servlet's normal operation
     * @throws IOException if an input or output exception occurs
     */
    protected void doSuccessResponse(SipServletResponse resp)
        throws ServletException, IOException
    {
    }
    
    
    /**
     * Invoked by the server (via the service method) to handle incoming 
     * UPDATE requests. 
     * 
     * <p>The default implementation is empty and must be overridden by
     * subclasses to do something useful.
     * 
     * @param req   represents the incoming SIP REGISTER request
     * @throws ServletException if an exception occurs that interferes
     *                          with the servlet's normal operation
     * @throws IOException if an input or output exception occurs
     */
    protected void doUpdate(SipServletRequest req)
        throws ServletException, IOException
    {
    	/*TODO*/
    } 

    /**
     * @see javax.servlet.Servlet#getServletConfig()
     */
    public ServletConfig getServletConfig()
    {
        if (null == m_servletConfig)
        {
            ServletConfig cfg = super.getServletConfig();
            
            if(cfg != null)
            {
        		ServletContext servletContext = cfg.getServletContext();
        		SipServletUtil servletUtil = (SipServletUtil) servletContext.getAttribute(SipServlet.SIP_SERVLET_UTIL);
        		if (servletUtil != null) {
        			m_servletConfig = servletUtil.wrapConfig(cfg);
        		}
            }
        }

        return m_servletConfig;
    }

    /** 
     * @see javax.servlet.ServletConfig#getServletContext()
     */
    public ServletContext getServletContext()
    {
		//We need to direct the call through the Servlet Config. 
		//Both call to getServletContext() from either the Servlet Context
		//or Servlet Config will return the same object. In our case the 
		//Servlet Config wraps the original Servlet Context with a wrapper
		//that transforms SIP Requests to Http Request before passing the 
		//request to the wrapped implementation, e.g. Websphere.  
       ServletConfig cfg = getServletConfig();
       if(cfg!=null){
    	   return getServletConfig().getServletContext();
       }
       else{
    	   return null;
       }
    }

    /** (non-Javadoc)
	 * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
	 */
	public void init(ServletConfig cfg) throws ServletException {
		// TODO Auto-generated method stub
		super.init(cfg);
		
		ServletContext servletContext = cfg.getServletContext();
		SipServletUtil servletUtil = (SipServletUtil) servletContext.getAttribute(SipServlet.SIP_SERVLET_UTIL);
		if (servletUtil != null) {
			servletUtil.initSiplet(servletContext, this);
		}
	}

    /**
     * Writes the specified message to a servlet log file.
     * See {link ServletContext#log(String)}.
     * 
     * @param message   a <code>String</code> specifying the message to be
     *              written to the log file
     */
    public void log(String message)
    {
        ServletContext ctx = null;

        try
        {
            ctx = getServletContext();
        }
        catch (NullPointerException e)
        {
            //Will not work in standalone mode. Ignore and go on. 
        }

        if (null != ctx)
        {
            ctx.log(message);
        }
        else
        {
            //Overide the default behavior for now
            System.out.println(message);
        }
    }

    /**
     * Writes an explanatory message and a stack trace for a given
     * <code>Throwable</code> exception to the servlet log file. See
     * {@link ServletContext#log(String, Throwable)}.
     * 
     * @param message   a <code>String</code> that describes the error
     *                  or exception
     * @param t the <code>java.lang.Throwable</code> error or exception
     */
    public void log(String message, Throwable t)
    {
        ServletContext ctx = null;

        try
        {
            ctx = getServletContext();
        }
        catch (NullPointerException e)
        {
            //Will not work in standalone mode. Ignore and go on. 
        }

        if (null != ctx)
        {
            ctx.log(message, t);
        }
        else
        {
            //Overide the default behavior for now
            System.out.println(message + " " + t.getMessage() + "\n");
        }

    }

    /**
     * An exception has occured, log it to the session's sequence log. 
     * @param sipMsg
     * @param e
     */
    private void logExceptionToSessionLog(SipServletMessage sipMsg, Exception e) {
/*
    	SipServletMessageImpl msgImpl =  (SipServletMessageImpl) sipMsg;
        TransactionUserWrapper tuw = msgImpl.getTransactionUser();
        
        if(tuw != null)
        {
        	tuw.logToContext(SipSessionSeqLog.ERROR_DISPATCH_EXCEPTION, e);
        }
*/        
        
    }
    
    
    /**
     * Responds to specified request with a 500 status code.
     * 
     * @param req
     * 
     * @throws IOException if an input or output exception occurs
     */
    private void notHandled(SipServletRequest req) throws IOException
    {
		//21.4.6 405 Method Not Allowed
		//
		//   The method specified in the Request-Line is understood, but not
		//   allowed for the address identified by the Request-URI.
		//
		//   The response MUST include an Allow header field containing a list of
		//   valid methods for the indicated address.

        SipServletResponse resp =
            req.createResponse(SipServletResponse.SC_METHOD_NOT_ALLOWED,
            		"Request not handled by application");
        resp.addHeader("Allow", "REGISTER");
        resp.addHeader("Allow", "OPTIONS");
        resp.addHeader("Allow", "INFO");
        resp.addHeader("Allow", "INVITE");
        resp.addHeader("Allow", "ACK");
        resp.addHeader("Allow", "BYE");
        resp.addHeader("Allow", "CANCEL");
        resp.addHeader("Allow", "SUBSCRIBE");
        resp.addHeader("Allow", "NOTIFY");
        resp.addHeader("Allow", "PUBLISH");
        resp.addHeader("Allow", "MESSAGE");
        resp.send();
    }

    /**
     * Invoked to handle incoming SIP messages: requests or responses.
     * Exactly one of the arguments is null: if the event is a request
     * the response argument is null, and vice versa, if the event is
     * a response the request argument is null.
     * 
     * <p>This method dispatched to {@link #doRequest doRequest()} or
     * {@link #doResponse doResponse()} as appropriate. Servlets will not
     * usually need to override this method.
     * 
     * @param req   the request to handle, or null if the triggering event
     *              was an incoming response
     * @param resp  incoming response or null if the triggering event was
     *              an incoming request
     * @throws ServletException if an exception occurs that interferes
     *                          with the servlet's normal operation
     * @throws IOException if an input or output exception occurs
     */
    public void service(ServletRequest req, ServletResponse resp)
        throws ServletException, IOException
    {

    	if(req instanceof HttpServletRequest){
        	//We can only get to the filter if the invoked URL was mapped to a siplet
        	//If we get here, it probably means that an HTTP request was directed to 
        	//the siplet URL. This must be an error in the web.xml - since a siplet
        	//URL must be in the form of /<sipletname>, and there shuoldn't be another
        	//HTTP servlet with the same URL. 
        	throw new ServletException( "SIP servlet " + getClass().getName() + 
        			" was not invoked properly. This can be the cause of directing an HTTP request to a SIP servlet. Please check the URL patterns in web.xml"); 
    	}
    	
    	if (!"DUMMY_SIP".equals(req.getProtocol())) {
    		doRequestWrapper((SipServletRequest) req);
    	} else {
    		doResponseWrapper((SipServletResponse) resp);
    	}    	
    }
}
