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
package javax.faces.context;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.el.ELContext;
import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.application.ProjectStage;
import javax.faces.component.UINamingContainer;
import javax.faces.component.UIViewRoot;
import javax.faces.event.PhaseId;
import javax.faces.render.RenderKit;

/**
 * see Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 */
public abstract class FacesContext
{
    private static ThreadLocal<FacesContext> currentInstance = new ThreadLocal<FacesContext>();

    private static ThreadLocal<FacesContext> firstInstance = new ThreadLocal<FacesContext>();

    public abstract void addMessage(String clientId, FacesMessage message);

    public abstract Application getApplication();

    /**
     * 
     * @return
     * 
     * @since 2.0
     */
    public Map<Object, Object> getAttributes()
    {
        FacesContext ctx = firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.getAttributes();
    }

    public abstract Iterator<String> getClientIdsWithMessages();

    public static FacesContext getCurrentInstance()
    {
        return currentInstance.get();
    }

    /**
     * 
     * @return
     * 
     * @since 2.0
     */
    public PhaseId getCurrentPhaseId()
    {
        FacesContext ctx = firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.getCurrentPhaseId();
    }
    
    /**
     * Return the context within which all EL-expressions are evaluated.
     * <p>
     * A JSF implementation is expected to provide a full implementation of this class. However JSF also explicitly
     * allows user code to apply the "decorator" pattern to this type, by overriding the FacesContextFactory class. In
     * that pattern, the decorating class has a reference to an "underlying" implementation and forward calls to it,
     * possibly after taking other related actions.
     * <p>
     * The decorator pattern does have difficulties with backwards-compatibility when new methods are added to the class
     * being decorated, as with this method which was added in JSF1.2. Decorator classes that were written for JSF1.1
     * will subclass this class, but will not override this method to pass the call on to the "underlying" instance.
     * This base implementation therefore must do that for it.
     * <p>
     * Unfortunately the JSF designers stuffed up the design; this base class has no way of knowing what the
     * "underlying" instance is! The current implementation here is therefore to delegate directly to the very
     * <i>first</i> FacesContext instance registered within this request (via setCurrentInstance). This instance should
     * be the "full" implementation provided by the JSF framework. The drawback is that when any decorator class is
     * present which defaults to this base implementation, then any following decorator instances that do override this
     * method do not get it invoked.
     * <p>
     * It is believed that the Sun JSF implementation (Mojarra) does something similar.
     * 
     * @since 1.2
     */
    public ELContext getELContext()
    {
        // Do NOT use getCurrentInstance here. For FacesContext decorators that
        // register themselves as "the current instance" that will cause an
        // infinite loop. For FacesContext decorators that do not register
        // themselves as "the current instance", if they are themselves wrapped
        // by a decorator that *does* register itself, then an infinite loop
        // also occurs.
        //
        // As noted above, we really want to do something like
        // ctx = getWrappedInstance();
        // where the subclass can return the object it is delegating to.
        // As there is no such method, however, the best we can do is pass the
        // method call on to the first-registered FacesContext instance. That
        // instance will never be "this", as the real original FacesContext
        // object will provide a proper implementation of this method.
        FacesContext ctx = firstInstance.get();

        if (ctx == null)
        {
            throw new NullPointerException(FacesContext.class.getName());
        }

        ELContext elctx = ctx.getELContext();
        if (elctx == null)
        {
            throw new UnsupportedOperationException();
        }

        return elctx;
    }

    /**
     * 
     * @return
     * 
     * @since 2.0
     */
    public ExceptionHandler getExceptionHandler()
    {
        FacesContext ctx = firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.getExceptionHandler();
    }

    public abstract ExternalContext getExternalContext();

    public abstract FacesMessage.Severity getMaximumSeverity();

    /**
     * 
     * @return
     * 
     * @since 2.0
     */
    public List<FacesMessage> getMessageList()
    {
        FacesContext ctx = firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.getMessageList();
    }
    
    /**
     * 
     * @param clientId
     * @return
     * 
     * @since 2.0
     */
    public List<FacesMessage> getMessageList(String clientId)
    {
        FacesContext ctx = firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.getMessageList(clientId);
    }

    public abstract Iterator<FacesMessage> getMessages();

    public abstract Iterator<FacesMessage> getMessages(String clientId);

    /**
     * <p>
     * Return the PartialViewContext for this request. The PartialViewContext is used to control the processing of
     * specified components during the execute portion of the request processing lifecycle (known as partial processing)
     * and the rendering of specified components (known as partial rendering). This method must return a new
     * PartialViewContext if one does not already exist.
     * </p>
     * 
     * @return The PartialViewContext
     * @throws IllegalStateException
     *             if this method is called after this instance has been released
     * 
     * @since 2.0
     */
    public PartialViewContext getPartialViewContext()
    {
        FacesContext ctx = firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.getPartialViewContext();
    }

    public abstract RenderKit getRenderKit();

    public abstract boolean getRenderResponse();

    public abstract boolean getResponseComplete();

    public abstract ResponseStream getResponseStream();

    public abstract ResponseWriter getResponseWriter();
    
    /**
     * 
     * @return
     * 
     * @since 2.0
     */
    public boolean isValidationFailed()
    {
        FacesContext ctx = firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.isValidationFailed();
    }

    public abstract UIViewRoot getViewRoot();

    /**
     * 
     * @return
     * 
     * @since 2.0
     */
    public boolean isPostback()
    {
        FacesContext ctx = firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.isPostback();
    }
    
    /**
     * 
     * @return
     * 
     * @since 2.0
     */
    public boolean isProcessingEvents()
    {
        FacesContext ctx = firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        return ctx.isProcessingEvents();
    }

    public abstract void release();

    public abstract void renderResponse();

    public abstract void responseComplete();

    protected static void setCurrentInstance(FacesContext context)
    {
        if (context == null)
        {
            currentInstance.remove();
            firstInstance.remove();
        }
        else
        {
            currentInstance.set(context);

            if (firstInstance.get() == null)
            {
                firstInstance.set(context);
            }
        }
    }

    /**
     * 
     * @return
     * 
     * @since 2.0
     */
    public void setCurrentPhaseId(PhaseId currentPhaseId)
    {
        FacesContext ctx = firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        ctx.setCurrentPhaseId(currentPhaseId);
    }

    /**
     * 
     * @return
     * 
     * @since 2.0
     */
    public void setExceptionHandler(ExceptionHandler exceptionHandler)
    {
        FacesContext ctx = firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        ctx.setExceptionHandler(exceptionHandler);
    }
    
    /**
     * 
     * @param processingEvents
     * 
     * @since 2.0
     */
    public void setProcessingEvents(boolean processingEvents)
    {
        FacesContext ctx = firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        ctx.setProcessingEvents(processingEvents);
    }

    public abstract void setResponseStream(ResponseStream responseStream);

    public abstract void setResponseWriter(ResponseWriter responseWriter);

    public abstract void setViewRoot(UIViewRoot root);
    
    /**
     * 
     * 
     * @since 2.0
     */
    public void validationFailed()
    {
        FacesContext ctx = firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }
        
        ctx.validationFailed();
    }
    
    public boolean isProjectStage(ProjectStage stage)
    {
        if (stage == null)
        {
            throw new NullPointerException();
        }
        
        if (stage.equals(getApplication().getProjectStage()))
        {
            return true;
        }
        return false;
    }
    
    /**
     * 
     * @since 2.1
     * @return
     */
    public boolean isReleased()
    {
        FacesContext ctx = firstInstance.get();
        
        if (ctx == null)
        {
            throw new UnsupportedOperationException();
        }

        return ctx.isReleased();
    }
    
    /**
     * @since 2.2
     * @return 
     */
    public List<String> getResourceLibraryContracts()
    {
        FacesContext ctx = firstInstance.get();
        
        if (ctx == null)
        {
            return Collections.emptyList();
        }        
        
        return ctx.getResourceLibraryContracts();
    }
    
    /**
     * @since 2.2
     * @param contracts 
     */
    public void setResourceLibraryContracts(List<String> contracts)
    {
        FacesContext ctx = firstInstance.get();
        
        if (ctx == null)
        {
            return;
        }
        ctx.setResourceLibraryContracts(contracts);
    }
    
    /**
     * @since 2.2
     * @return 
     */
    public char getNamingContainerSeparatorChar()
    {
        FacesContext ctx = firstInstance.get();
        
        if (ctx == null)
        {
            return UINamingContainer.getSeparatorChar(this);
        }
        return ctx.getNamingContainerSeparatorChar();
    }
}
