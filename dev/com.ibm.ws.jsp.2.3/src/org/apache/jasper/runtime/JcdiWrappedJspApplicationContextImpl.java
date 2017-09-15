/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.apache.jasper.runtime;

import javax.el.ExpressionFactory;
import javax.servlet.ServletContext;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspFactory;

import com.ibm.ws.jsp.webcontainerext.JSPExtensionFactory;
import com.ibm.wsspi.el.ELFactoryWrapperForCDI;
import com.ibm.wsspi.webcontainer.WCCustomProperties;

public class JcdiWrappedJspApplicationContextImpl extends JspApplicationContextImpl implements JspApplicationContext {

    private final static String KEY = JcdiWrappedJspApplicationContextImpl.class.getName();  //use JspApplicationContextImpl as the key
    
    private ExpressionFactory expressionFactory = null;
    
    public JcdiWrappedJspApplicationContextImpl() {
    }

    public ExpressionFactory getExpressionFactory() {
        if (expressionFactory==null) {
            
            // casting using a null could produce an NPE, so structure this code such that we 
            // won't throw an NPE on the cast.
            ELFactoryWrapperForCDI x = JSPExtensionFactory.getWrapperExpressionFactory();
            if (x != null) {
                expressionFactory = (ExpressionFactory) x;
            }    
            
            // The code above works fine with Open Web Beans, under which we expect to have 
            // a 'wrapped' expression factory in the service registry. However we're not yet
            // sure that Weld provides such a thing. So...
            
            if (expressionFactory == null) { 
                expressionFactory = _impl.getExpressionFactory();
            }
        }
        
        return expressionFactory;
    }
    
    private JspApplicationContextImpl _impl = null;
    
    private JcdiWrappedJspApplicationContextImpl (JspApplicationContextImpl impl) { 
        _impl = impl;
    }

    public static JspApplicationContextImpl getInstance(ServletContext context) {
        if (context == null) {
            throw new IllegalArgumentException("ServletContext was null");
        }
        JspApplicationContext appCtx = JspFactory.getDefaultFactory().getJspApplicationContext(context);        
        JspApplicationContextImpl impl = (JspApplicationContextImpl) appCtx;        
        // PM05903 Start
        if ( WCCustomProperties.THROW_EXCEPTION_FOR_ADDELRESOLVER 
            && context.getAttribute("com.ibm.ws.jsp.servletContextListeners.contextInitialized")!= null) {          
                impl.listenersContextInitialized = true;            
        }//PM05903 End
        
        return new JcdiWrappedJspApplicationContextImpl (impl);
    }
    
}
