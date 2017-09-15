/*
 * Copyright 1999,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Created for feature LIDB4147-9 "Integrate Unified Expression Language"  2006/08/14  Scott Johnson
 * defect  388930 "Incorrect ELContext may be used"  2006/09/06  Scott Johnson
 * 
 */
 // CHANGE HISTORY
 // Defect       Date        Modified By         Description
 //--------------------------------------------------------------------------------------
 // PM05903	03/02/10    anupag		AddELResolver in servlet is allowed.
 // PI31922     12/19/14    hwibell             Allow multiple expression factories to be used on a server.
 //
package org.apache.jasper.runtime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContextEvent;
import javax.el.ELContextListener;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.ResourceBundleELResolver;
import javax.servlet.ServletContext;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.el.ImplicitObjectELResolver;
import javax.servlet.jsp.el.ScopedAttributeELResolver;

import org.apache.jasper.el.ELContextImpl;

import com.ibm.wsspi.webcontainer.WCCustomProperties; //PM05903

/**
 * Implementation of JspApplicationContext
 * 
 * @author Jacob Hookom
 */
public class JspApplicationContextImpl implements JspApplicationContext {

	private final static String KEY = JspApplicationContextImpl.class.getName();  //388930
        protected final static Logger logger = Logger.getLogger("com.ibm.ws.jsp");

        // PI31922 start
        protected final ExpressionFactory expressionFactory;
        private final static ExpressionFactory staticExpressionFactory = ExpressionFactory.newInstance();
        // PI31922 end

	private final List<ELContextListener> contextListeners = new ArrayList<ELContextListener>();

	private final List<ELResolver> resolvers = new ArrayList<ELResolver>();

	private boolean instantiated = false;
	protected boolean listenersContextInitialized = false; //PM05903

	private ELResolver resolver;

	public JspApplicationContextImpl() {
            // PI31922 start

            /*
             * This fixes a problem when the expression factory is set only when the first application
             * is loaded. This is not the desirable behavior as expressionFactory should be set for each
             * application. expressionFactory will point to either expression factory based on the custom
             * property 'com.ibm.ws.jsp.allowExpressionFactoryPerApp'.
             *
             * This issue was addressed by the Japser project bug 52998 (https://issues.apache.org/bugzilla/show_bug.cgi?id=52998).
             */
            if (!WCCustomProperties.ALLOW_EXPRESSION_FACTORY_PER_APP) {
                    logger.logp(Level.FINE, KEY, "JspApplicationContextImpl", "Setting expressionFactory to the expression factory of the first loaded application.");
                    expressionFactory = staticExpressionFactory;
            }
            else {
                    logger.logp(Level.FINE, KEY, "JspApplicationContextImpl", "Setting expressionFactory to a new instance of ExpressionFactory.");
                    expressionFactory = ExpressionFactory.newInstance();
            }  
            // PI31922 end
	}

	public void addELContextListener(ELContextListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("ELConextListener was null");
		}
		this.contextListeners.add(listener);
	}

	public static JspApplicationContextImpl getInstance(ServletContext context) {
		if (context == null) {
			throw new IllegalArgumentException("ServletContext was null");
		}
		JspApplicationContextImpl impl = (JspApplicationContextImpl) context.getAttribute(KEY);
		if (impl == null) {
			impl = new JspApplicationContextImpl();
			context.setAttribute(KEY, impl);
		}
		//PM05903 Start
        	if ( WCCustomProperties.THROW_EXCEPTION_FOR_ADDELRESOLVER 
        		&& context.getAttribute("com.ibm.ws.jsp.servletContextListeners.contextInitialized")!= null) {       	
					impl.listenersContextInitialized = true;            
        	}//PM05903 End

		return impl;
	}

	public ELContextImpl createELContext(JspContext context) {
		if (context == null) {
			throw new IllegalArgumentException("JspContext was null");
		}

		// create ELContext for JspContext
		ELResolver r = this.createELResolver();
		ELContextImpl ctx = new ELContextImpl(r);
		ctx.putContext(JspContext.class, context);

		// alert all ELContextListeners
		ELContextEvent event = new ELContextEvent(ctx);
		for (int i = 0; i < this.contextListeners.size(); i++) {
			this.contextListeners.get(i).contextCreated(event);
		}

		return ctx;
	}

	private ELResolver createELResolver() {
		this.instantiated = true;
		if (this.resolver == null) {
			CompositeELResolver r = new CompositeELResolver();
			r.add(new ImplicitObjectELResolver());
			for (Iterator itr = this.resolvers.iterator(); itr.hasNext();) {
				r.add((ELResolver) itr.next());
			}
			r.add(new MapELResolver());
			r.add(new ResourceBundleELResolver());
			r.add(new ListELResolver());
			r.add(new ArrayELResolver());	
			r.add(new BeanELResolver());
			r.add(new ScopedAttributeELResolver());
			this.resolver = r;
		}
		return this.resolver;
	}

	public void addELResolver(ELResolver resolver) throws IllegalStateException {
		if (resolver == null) {
			throw new IllegalArgumentException("ELResolver was null");
		}
		if (this.instantiated || this.listenersContextInitialized) { //PM05903
			throw new IllegalStateException(
					"cannot call addELResolver after the first request has been made");
		}
		this.resolvers.add(resolver);
	}

	public ExpressionFactory getExpressionFactory() {
		return expressionFactory;
	}

}
