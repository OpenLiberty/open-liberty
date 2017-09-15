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
package org.apache.myfaces.view.jsp;

import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.FacesException;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PostAddToViewEvent;
import javax.faces.render.ResponseStateManager;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.jstl.core.Config;

import org.apache.myfaces.application.jsp.ServletViewResponseWrapper;
import org.apache.myfaces.application.viewstate.StateCacheUtils;
import org.apache.myfaces.shared.view.JspViewDeclarationLanguageBase;
import org.apache.myfaces.view.facelets.tag.composite.CompositeLibrary;
import org.apache.myfaces.view.facelets.tag.jsf.core.CoreLibrary;
import org.apache.myfaces.view.facelets.tag.jsf.html.HtmlLibrary;
import org.apache.myfaces.view.facelets.tag.ui.UILibrary;

/**
 * @author Simon Lessard (latest modification by $Author: lu4242 $)
 * @version $Revision: 1410190 $ $Date: 2012-11-16 03:37:03 +0000 (Fri, 16 Nov 2012) $
 * 
 * @since 2.0
 */
public class JspViewDeclarationLanguage extends JspViewDeclarationLanguageBase
{
    //private static final Log log = LogFactory.getLog(JspViewDeclarationLanguage.class);
    public static final Logger log = Logger.getLogger(JspViewDeclarationLanguage.class.getName());
    
    /**
     * Tags that are only available on facelets and not on JSP.
     * If a user uses one of these tags on a JSP, we will provide
     * a more informative error message than the standard one.
     */
    public static final String[] FACELETS_ONLY_F_TAGS = {"ajax", "event", "metadata"};
    public static final String[] FACELETS_ONLY_H_TAGS = {"outputScript", "outputStylesheet",
                                                         "head", "body", "button", "link"};
    
    /**
     * 
     */
    public JspViewDeclarationLanguage()
    {
        if (log.isLoggable(Level.FINEST))
        {
            log.finest("New JspViewDeclarationLanguage instance created");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildView(FacesContext context, UIViewRoot view) throws IOException
    {
        // call buildView() from JspViewDeclarationLanguageBase to do some startup work
        super.buildView(context, view);
        
        ExternalContext externalContext = context.getExternalContext();
        ServletResponse response = (ServletResponse) externalContext.getResponse();
        ServletRequest request = (ServletRequest) externalContext.getRequest();
        
        Locale locale = view.getLocale();
        response.setLocale(locale);
        Config.set(request, Config.FMT_LOCALE, context.getViewRoot().getLocale());

        String viewId = view.getViewId();
        ServletViewResponseWrapper wrappedResponse = new ServletViewResponseWrapper((HttpServletResponse) response);

        externalContext.setResponse(wrappedResponse);
        try
        {
            externalContext.dispatch(viewId);
        }
        catch (FacesException e)
        {
            // try to extract the most likely exceptions here
            // and provide a better error message for them
            
            String message = e.getMessage(); 
            
            // errors related to using facelets-only tags on a JSP page
            if (message != null)
            {
                // does the message contain "f" (prefix f of tags)
                // or the related uri http://java.sun.com/jsf/core
                if (message.contains("\"f\"") 
                        || message.contains("\"" + CoreLibrary.NAMESPACE + "\""))
                {
                    // check facelets-only f tags
                    for (String tag : FACELETS_ONLY_F_TAGS)
                    {
                        if (message.contains("\"" + tag + "\""))
                        {
                            String exceptionMessage = "The tag f:" + tag + 
                                    " is only available on facelets.";
                            throw new FacesException(exceptionMessage, 
                                    new FaceletsOnlyException(exceptionMessage, e.getCause()));
                        }
                    }
                }  
                else if (message.contains("\"h\"") 
                        || message.contains("\"" + HtmlLibrary.NAMESPACE + "\""))
                {
                    // check facelets-only h tags
                    for (String tag : FACELETS_ONLY_H_TAGS)
                    {
                        if (message.contains("\"" + tag + "\""))
                        {
                            String exceptionMessage = "The tag h:" + tag + 
                                    " is only available on facelets.";
                            throw new FacesException(exceptionMessage, 
                                    new FaceletsOnlyException(exceptionMessage, e.getCause()));
                        }
                    }
                }
                else 
                {
                    // check facelets-only namespaces
                    String namespace = null;
                    if (message.contains(UILibrary.NAMESPACE))
                    {
                        namespace = UILibrary.NAMESPACE;
                    }
                    else if (message.contains(CompositeLibrary.NAMESPACE))
                    {
                        namespace = CompositeLibrary.NAMESPACE;
                    }
                    
                    if (namespace != null)
                    {
                        // the message contains a facelets-only namespace
                        String exceptionMessage = "All tags with namespace " +
                                namespace + " are only available on facelets.";
                        throw new FacesException(exceptionMessage, 
                                new FaceletsOnlyException(exceptionMessage, e.getCause()));
                    }
                }
            }
            
            // no rule applied to this Exception - rethrow it
            throw e;
        }
        finally
        {
            externalContext.setResponse(response);
        }

        boolean errorResponse = wrappedResponse.getStatus() < 200 || wrappedResponse.getStatus() > 299;
        if (errorResponse)
        {
            wrappedResponse.flushToWrappedResponse();
            return;
        }

        // Skip this step if we are rendering an ajax request, because no content outside
        // f:view tag should be output.
        // Note that the ResponseSwitch would prevent this output from beeing written
        // in renderView(), but not providing the information at all makes it faster!
        if (!context.getPartialViewContext().isPartialRequest())
        {
            // store the wrapped response in the request, so it is thread-safe
            setAfterViewTagResponseWrapper(externalContext, wrappedResponse);
        }
        
        // Publish PostAddToView over UIViewRoot, because this is not done automatically.
        context.getApplication().publishEvent(context, PostAddToViewEvent.class, UIViewRoot.class, view);
    }

    /**
     * 
     */
    @Override
    protected boolean isViewStateAlreadyEncoded(FacesContext context)
    {
        ResponseStateManager responseStateManager = context.getRenderKit().getResponseStateManager();
        if (StateCacheUtils.isMyFacesResponseStateManager(responseStateManager))
        {
            if (StateCacheUtils.getMyFacesResponseStateManager(responseStateManager).
                    isWriteStateAfterRenderViewRequired(context))
            {
                return false;
            }
            else
            {
                return true;
            }
        }
        else
        {
            return false;
        }
    }

    @Override
    protected void sendSourceNotFound(FacesContext context, String message)
    {
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        try
        {
            context.responseComplete();
            response.sendError(HttpServletResponse.SC_NOT_FOUND, message);
        }
        catch (IOException ioe)
        {
            throw new FacesException(ioe);
        }
    }

}
