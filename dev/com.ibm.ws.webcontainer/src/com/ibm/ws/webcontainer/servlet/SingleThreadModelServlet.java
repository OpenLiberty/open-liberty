/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet;

import java.io.IOException;

import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.ibm.ejs.ras.TraceNLS;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.ibm.websphere.servlet.error.ServletErrorReport;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

/**
 * Implements the SingleThreadModel contract for a servlet class.
 * This class transparently added STM support to a servlet class by
 * acting as the target servlet and then proxying invocations to a
 * pool of servlet instances.
 *
 * This class uses a max idle time mechanism to implement the growing
 * and shrinking of the servlet pool.  If there are servlets in the
 * pool that have been idle for longer than the max idle time, then
 * they will be destoyed and removed from the pool.  The check for
 * idle servlet occurs on each service call to avoid the need for an
 * external polling mechanism.
 */
@SuppressWarnings("unchecked")
public class SingleThreadModelServlet extends GenericServlet
{
    private static final long serialVersionUID = 1L;

    private static TraceNLS nls = TraceNLS.getTraceNLS(SingleThreadModelServlet.class, "com.ibm.ws.webcontainer.resources.Messages");
    private TimedServletPool _pool;
    private Class _servletClass;
protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.servlet");
	private static final String CLASS_NAME="com.ibm.ws.webcontainer.servlet.SingleThreadModelServlet";

    public SingleThreadModelServlet()
    {
    }

    /**
     * Initialize the servlet with the servlet class that it will be
     * pooling instances of.
     */
    public SingleThreadModelServlet(Class cls)
    {
        setServletClass(cls);
    }

    public void init() throws ServletException
    {
        try
        {
            //defaults
            int initialPoolSize=5;
            long maxIdleTime=60000;

            String _initialPoolSize=getServletConfig().getServletContext().getInitParameter("singlethreadmodel.initialpoolsize");
            String _maxIdleTime=getServletConfig().getServletContext().getInitParameter("singlethreadmodel.maxidletime");

            if (_initialPoolSize != null)
            {
                try
                {
                    initialPoolSize = Integer.valueOf(_initialPoolSize).intValue();
                }
                catch (NumberFormatException ex)
                {
                    initialPoolSize=5; 
                    logger.logp(Level.SEVERE, CLASS_NAME,"init", "setting.initialPoolSize.to.default.value.of.5", ex);
                }
            }

            if (_maxIdleTime != null)
            {
                try
                {
                    maxIdleTime=Long.valueOf(_maxIdleTime).longValue();
                }
                catch (NumberFormatException ex)
                {
                    maxIdleTime=60000;
                    logger.logp(Level.SEVERE, CLASS_NAME,"init", "setting.maxIdleTime.to.default.value.of.60000", ex);
                }
            }

            _pool = getTimedServletPool(initialPoolSize, maxIdleTime, getServletClass(), getServletConfig());
        }
        catch (Throwable th)
        {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, "com.ibm.ws.webcontainer.servlet.SingleThreadModelServlet.init", "64", this);
            throw new ServletException(th);
        }
    }
    
    /**
     * Overloaded by subclasses to perform the servlet's request handling operation.
     */
    public void service(ServletRequest req, ServletResponse resp)
    throws ServletException, IOException
    {
        try
        {
            TimedServletPoolElement e = _pool.getNextElement();
            e.getServlet().service(req, resp);
            _pool.returnElement(e);
            _pool.removeExpiredElements();
        }
        catch (Throwable th)
        {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, "com.ibm.ws.webcontainer.servlet.SingleThreadModelServlet.service", "84", this);
            throw new ServletErrorReport(th);
        }
    }

    /**
     * Overloaded by subclasses to perform the servlet's destroy operation.
     */
    public void destroy()
    {
        _pool.removeAllElements();
    }

    public Class getServletClass()
    {
        return _servletClass;
    }

    public void setServletClass(Class servletClass)
    {
        if (!(Servlet.class.isAssignableFrom(servletClass)))
        {
        	Object[] args = {servletClass};
            throw new IllegalArgumentException(nls.getFormattedMessage("Class.{0}.does.not.implement.servlet", args, "Class "+servletClass+" does not implement servlet"));
        }
        _servletClass = servletClass;
    }

    public int getInstanceCount()
    {
        return _pool.getSize();
    }
    
	protected TimedServletPool getTimedServletPool(
		int initialPoolSize,
		long maxIdleTime,
		Class servletClass,
		ServletConfig config) throws ServletException {
		try {
			return new TimedServletPool(initialPoolSize, maxIdleTime, servletClass, config);
		}
			catch (Throwable th)
	        {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(th, "com.ibm.ws.webcontainer.servlet.SingleThreadModelServlet.getTimedServletPool", "65", this);
	            throw new ServletException(th);
	        }
	}
}
