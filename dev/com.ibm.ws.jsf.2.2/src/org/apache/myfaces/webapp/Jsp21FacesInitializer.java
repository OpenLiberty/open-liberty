/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.webapp;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ExpressionFactory;
import javax.faces.FactoryFinder;
import javax.faces.context.ExternalContext;
import javax.faces.event.PhaseListener;
import javax.faces.lifecycle.LifecycleFactory;
import javax.servlet.ServletContext;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspFactory;

import org.apache.myfaces.config.RuntimeConfig;
import org.apache.myfaces.el.ResolverForJSPInitializer;
import org.apache.myfaces.el.unified.ELResolverBuilder;
import org.apache.myfaces.el.unified.ResolverBuilderForJSP;
import org.apache.myfaces.el.unified.resolver.FacesCompositeELResolver;
import org.apache.myfaces.el.unified.resolver.FacesCompositeELResolver.Scope;

/**
 * Initializes MyFaces in a JSP 2.1 environment.
 *
 */
public class Jsp21FacesInitializer extends AbstractFacesInitializer
{
    /**
     * The logger instance for this class.
     */
    //private static final Log log = LogFactory.getLog(Jsp21FacesInitializer.class);
    private static final Logger log = Logger.getLogger(Jsp21FacesInitializer.class.getName());
    
    /**
     * Cached instance of the JspFactory to use.
     */
    private JspFactory jspFactory;
    
    @Override
    protected void initContainerIntegration(
            ServletContext servletContext, ExternalContext externalContext)
    {
        JspApplicationContext appCtx = 
            getJspFactory().getJspApplicationContext(servletContext);
        appCtx.addELContextListener(new FacesELContextListener());
        
        // check for user-specified ExpressionFactory
        ExpressionFactory expressionFactory = getUserDefinedExpressionFactory(externalContext);
        if (expressionFactory == null)
        {
            expressionFactory = appCtx.getExpressionFactory();
        }

        RuntimeConfig runtimeConfig =
            buildConfiguration(servletContext, externalContext, expressionFactory);
        
        // configure the el resolver for jsp
        configureResolverForJSP(appCtx, runtimeConfig);
    }
    
    protected JspFactory getJspFactory()
    {
        if (jspFactory == null)
        {
            // TODO: this Class.forName will be removed when Tomcat fixes a bug
            // also, we should then be able to remove jasper.jar from the deployment
            try
            {
                Class.forName("org.apache.jasper.compiler.JspRuntimeContext");
            }
            catch (ClassNotFoundException e)
            {
                // ignore
            }
            catch (Exception ex)
            {
                log.log(Level.FINE, "An unexpected exception occured "
                        + "while loading the JspRuntimeContext.", ex);
            }

            jspFactory = JspFactory.getDefaultFactory();
        }

        return jspFactory;
    }

    /**
     * Sets the JspFactory to use. Currently, this method just simplifies
     * testing.
     * 
     * @param jspFactory
     *            the JspFactory to use
     */
    protected void setJspFactory(JspFactory jspFactory)
    {
        this.jspFactory = jspFactory;
    }

    /**
     * Register a phase listener to every lifecycle. This listener will lazy fill the el resolver for jsp as soon as the
     * first lifecycle is executed. This is necessarry to allow a faces application further setup after MyFaces has been
     * initialized. When the first request is processed no further configuation of the el resolvers is allowed.
     * 
     * @param appCtx
     * @param runtimeConfig
     */
    private void configureResolverForJSP(JspApplicationContext appCtx, RuntimeConfig runtimeConfig)
    {
        FacesCompositeELResolver facesCompositeELResolver = new FacesCompositeELResolver(Scope.JSP);
        appCtx.addELResolver(facesCompositeELResolver);
        PhaseListener resolverForJSPInitializer = new ResolverForJSPInitializer(
                createResolverBuilderForJSP(runtimeConfig), facesCompositeELResolver);

        LifecycleFactory factory = (LifecycleFactory) FactoryFinder.getFactory(FactoryFinder.LIFECYCLE_FACTORY);
        for (Iterator<String> iter = factory.getLifecycleIds(); iter.hasNext();)
        {
            factory.getLifecycle(iter.next()).addPhaseListener(resolverForJSPInitializer);
        }
    }
    
    protected ELResolverBuilder createResolverBuilderForJSP(RuntimeConfig runtimeConfig)
    {
        return new ResolverBuilderForJSP(runtimeConfig);
    }
}
