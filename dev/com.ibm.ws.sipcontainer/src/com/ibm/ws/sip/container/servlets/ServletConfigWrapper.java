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
package com.ibm.ws.sip.container.servlets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.sip.resolver.DomainResolver;
import com.ibm.ws.sip.container.internal.SipContainerComponent;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.timer.ExternTimerService;
import com.ibm.ws.sip.container.util.SipUtil;
import com.ibm.ws.sip.properties.CoreProperties;

/**
 * @author Amir Perlman, Jan 2, 2004
 *
 * Wraps the actuals implementation of the Servlet Config in order to direct
 * calls to the appropriate Servlet Context associated with the Sip Servlet. 
 */
public class ServletConfigWrapper implements ServletConfig,java.io.Serializable
{
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(ServletConfigWrapper.class);

    /**
     * The actual implementation of the Servlet Config
     */
    private ServletConfig m_impl;

    /**
     * The Servlet Context associated with Servlet Config.
     */
    private ServletContext m_servletContext;
    
    
    /**
     * Attribute that should be added when the Reliably responses are supprted
     */
    public static final String ATTRIBUTE_100_REL = "javax.servlet.sip.100rel";

    public static final String PARAM_100_REL = "100rel";
    
    public static final String PARAM_FROM_CHANGE = "from-change";
    
    public static final String SIP_SERVLET_UTIL = "javax.servlet.sip.SipServletUtil";

    /**
     * Servlet Attribute which contains supported SIP RFC's
     */
    public static final String ATTRIBUTE_SUPPORTED_RFCS = "javax.servlet.sip.supportedRfcs";

    
    /**
     * Immutable list of supported extentions by the container. For now we 
     * don't support any extentions. When we will have extentions we will 
     * need to save the internal list as a member as well so it can be modified
     * internally. 
     */
    private static final List<String> c_supported;
    
    /**
     * Immutable instance of the java.util.List containing the RFC numbers 
     * represented as Strings of SIP RFCs supported by the container. 
     */
    private static final List<String> c_supportedRfcs;
    
    /**
     * Special attribute that will hold the real application name
     */    
    public static final String APP_NAME_ATTRIBUTE = "com.ibm.ws.sip.container.app.name";
		
    
    static {
    	
        c_supported = Collections.unmodifiableList(getListOfSupportedForContainer());

        c_supportedRfcs = Collections.unmodifiableList(getListOfSupportedRFCsForContainer());
    }
    
    /**
     * Constructs a new Servlet Config Wrapper associated with
     * specified Serlvlet Config Impl and Servlet Context.
     */
    public ServletConfigWrapper(ServletConfig impl)
    {
        m_impl = impl;
    }

    /**
     * @see javax.servlet.ServletConfig#getServletName()
     */
    public String getServletName()
    {
        return m_impl.getServletName();
    }
    
    /**
     *	Setting the SIP attributes on the module context 
     * @param ctx
     */
    public static void setContextAttributes(ServletContext ctx){
    	
    	if(ctx.getAttribute(SipServlet.SIP_FACTORY) != null){
    		//means attributes were not already set on the collaborator
    		//(WebsphereAppLoadListener.wrapServletConfigs)
    		return;
    	}
    	String appName = getAppName(ctx);
    	
    	if (c_logger.isTraceDebugEnabled()) {
        	c_logger.traceDebug("Got application: " + appName);    		
    	}

    	//There is a one to one relation between a factory instance
        //and a SIP Application.
    	SipFactory factory = SipServletsFactoryImpl.getInstance(appName);
    	
    	
    	//we'll get here only on stand alone or WAS versions earlier then 6.1
        ctx.setAttribute(SipServlet.SIP_FACTORY, factory);

        ctx.setAttribute(
            SipServlet.TIMER_SERVICE,
            ExternTimerService.getInstance());
        
        ctx.setAttribute(SipServlet.SUPPORTED, c_supported);
        ctx.setAttribute(ATTRIBUTE_SUPPORTED_RFCS, c_supportedRfcs);        
        ctx.setAttribute(ATTRIBUTE_100_REL,Boolean.TRUE);
        ctx.setAttribute(SIP_SERVLET_UTIL, new SipServletUtilImpl());
        ctx.setAttribute(SipServlet.SIP_SESSIONS_UTIL,new SipSessionsUtilImpl(appName));
        ctx.setAttribute(SipServlet.DOMAIN_RESOLVER_ATTRIBUTE, (DomainResolver)SipContainerComponent.getDomainResolverService());
    }
    
    /**
     * @see javax.servlet.ServletConfig#getServletContext()
     */
    public ServletContext getServletContext()
    {
        if (m_servletContext == null)
        {
            ServletContext ctx = m_impl.getServletContext();

            if (ctx != null)
            {
            	setContextAttributes(ctx);
            }

            m_servletContext = (ServletContext) new ServletContextWrapper(ctx);
        }
        return m_servletContext;
    }

    /**
     * Retrieve application name from contexts or sip.xml
     * @param ctx
     * @return
     */
    private static String getAppName(ServletContext ctx)
    {
    	return ctx.getServletContextName();
    }
    
    
    
    /**
     * @see javax.servlet.ServletConfig#getInitParameter(java.lang.String)
     */
    public String getInitParameter(String arg0)
    {
        return m_impl.getInitParameter(arg0);
    }

    /**
     * @see javax.servlet.ServletConfig#getInitParameterNames()
     */
    public Enumeration getInitParameterNames()
    {
        return m_impl.getInitParameterNames();
    }
    
    public static List <String> getSupportedList() {
    	return c_supported;
    }
    
    private static final List<String> getListOfSupportedForContainer() {
        List<String> list = new ArrayList<String>(10);
        
        list.add(ReliableResponse.RELIABLY_PARAM);
        list.add(SipUtil.PATH_PARAM);
        list.add(ServletConfigWrapper.PARAM_100_REL);
        if (!PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.JSR289_SUPPORT_LEGACY_CLIENT)) {
        	list.add(ServletConfigWrapper.PARAM_FROM_CHANGE);
        }
        return list;
    }
    
    
    /**
     * Initialize currently supported RFC's
     */
    private static final List<String> getListOfSupportedRFCsForContainer() {
    	List<String> rfcList = new ArrayList<String>(100);
    	rfcList.add("RFC2543");
    	rfcList.add("RFC2848");
    	rfcList.add("RFC2976");
    	rfcList.add("RFC3050");
    	rfcList.add("RFC3087");
    	rfcList.add("RFC3261");
    	rfcList.add("RFC3262");
    	rfcList.add("RFC3264");
    	rfcList.add("RFC3265");
    	rfcList.add("RFC3266");
    	rfcList.add("RFC3311");
    	rfcList.add("RFC3312");
    	rfcList.add("RFC3313");
    	rfcList.add("RFC3319");
    	rfcList.add("RFC3326");
    	rfcList.add("RFC3327");
    	rfcList.add("RFC3351");
    	rfcList.add("RFC3372");
    	rfcList.add("RFC3398");
    	rfcList.add("RFC3428");
    	rfcList.add("RFC3455");
    	rfcList.add("RFC3487");
    	rfcList.add("RFC3515");
    	rfcList.add("RFC3578");
    	rfcList.add("RFC3603");
    	rfcList.add("RFC3608");
    	rfcList.add("RFC3665");
    	rfcList.add("RFC3666");
    	rfcList.add("RFC3680");
    	rfcList.add("RFC3702");
    	rfcList.add("RFC3725");
    	rfcList.add("RFC3764");
    	rfcList.add("RFC3824");
    	rfcList.add("RFC3840");
    	rfcList.add("RFC3842");
    	rfcList.add("RFC3856");
    	rfcList.add("RFC3857");
    	rfcList.add("RFC3903");
    	rfcList.add("RFC3959");
    	rfcList.add("RFC3960");
    	rfcList.add("RFC3968");
    	rfcList.add("RFC3969");
    	rfcList.add("RFC3976");
    	rfcList.add("RFC4032");
    	rfcList.add("RFC4083");
    	rfcList.add("RFC4092");
    	rfcList.add("RFC4117");
    	rfcList.add("RFC4123");
    	rfcList.add("RFC4189");
    	rfcList.add("RFC4235");
    	rfcList.add("RFC4240");
    	rfcList.add("RFC4245");
    	rfcList.add("RFC4321");
    	rfcList.add("RFC4353");
    	rfcList.add("RFC4354");
    	rfcList.add("RFC4411");
    	rfcList.add("RFC4453");
    	rfcList.add("RFC4457");
    	rfcList.add("RFC4458");
    	rfcList.add("RFC4483");
    	rfcList.add("RFC4497");
    	rfcList.add("RFC4504");
    	rfcList.add("RFC4508");
    	
    	return rfcList;
	}    

}
