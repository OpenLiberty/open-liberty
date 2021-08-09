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
package javax.faces.webapp;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UpdateModelException;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerFactory;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;
import javax.faces.event.SystemEvent;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;

/**
 * @since 2.0
 */
public class PreJsf2ExceptionHandlerFactory extends ExceptionHandlerFactory
{
    
    public PreJsf2ExceptionHandlerFactory()
    {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExceptionHandler getExceptionHandler()
    {
        return new PreJsf2ExceptionHandlerImpl();
    }
    
    private static class PreJsf2ExceptionHandlerImpl extends ExceptionHandler
    {
        /*
         * PLEASE NOTE!!!
         * This is a copy of the ExceptionHandler implementation of myfaces-impl
         * (org.apache.myfaces.context.ExceptionHandlerImpl), only handle() differs a bit.
         * Any changes made here should also be applied to ExceptionHandlerImpl in the right way.
         * 
         * This is really ugly, but I think we have to do this due to the fact that PreJsf2ExceptionHandlerFactory
         * can be declared directly as an exception handler factory, so it's not as if we can make the methods
         * in the factory abstract and have a concrete impl in the impl project (and therefore be able to
         * extend ExceptionHandlerImpl).  If this is not the case, please change accordingly.
         */
        
        private static final Logger log = Logger.getLogger(PreJsf2ExceptionHandlerImpl.class.getName());
        
        /**
         * Since JSF 2.0 there is a standard way to deal with unexpected Exceptions: the ExceptionHandler.
         * Due to backwards compatibility MyFaces 2.0 also supports the init parameter 
         * org.apache.myfaces.ERROR_HANDLER, introduced in MyFaces 1.2.4. However, the given error handler
         * now only needs to include the following method:
         * <ul>
         * <li>handleException(FacesContext fc, Exception ex)</li>
         * </ul>
         * Furthermore, the init parameter only works when using the PreJsf2ExceptionHandlerFactory.
         * 
         * @deprecated
         */
        @Deprecated
        @JSFWebConfigParam(since="1.2.4",desc="Deprecated: use JSF 2.0 ExceptionHandler", deprecated=true)
        private static final String ERROR_HANDLER_PARAMETER = "org.apache.myfaces.ERROR_HANDLER";
        
        private Queue<ExceptionQueuedEvent> handled;
        private Queue<ExceptionQueuedEvent> unhandled;
        private ExceptionQueuedEvent handledAndThrown;

        public PreJsf2ExceptionHandlerImpl()
        {
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public ExceptionQueuedEvent getHandledExceptionQueuedEvent()
        {
            return handledAndThrown;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Iterable<ExceptionQueuedEvent> getHandledExceptionQueuedEvents()
        {
            return handled == null ? Collections.<ExceptionQueuedEvent>emptyList() : handled;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Throwable getRootCause(Throwable t)
        {
            if (t == null)
            {
                throw new NullPointerException("t");
            }
            
            while (t != null)
            {
                Class<?> clazz = t.getClass();
                if (!clazz.equals(FacesException.class) && !clazz.equals(ELException.class))
                {
                    return t;
                }
                
                t = t.getCause();
            }
            
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Iterable<ExceptionQueuedEvent> getUnhandledExceptionQueuedEvents()
        {
            return unhandled == null ? Collections.<ExceptionQueuedEvent>emptyList() : unhandled;
        }

        /**
         * {@inheritDoc}
         * 
         * Differs from ExceptionHandlerImpl.handle() in three points:
         *  - Any exceptions thrown before or after phase execution will be logged and swallowed.
         *  - If the Exception is an instance of UpdateModelException, extract the
         *  FacesMessage from the UpdateModelException.
         *    Log a SEVERE message to the log and queue the FacesMessage on the
         *    FacesContext, using the clientId of the source
         *    component in a call to FacesContext.addMessage(java.lang.String, javax.faces.application.FacesMessage).
         *  - Checks org.apache.myfaces.ERROR_HANDLER for backwards compatibility to myfaces-1.2's error handling
         */
        @Override
        public void handle() throws FacesException
        {
            if (unhandled != null && !unhandled.isEmpty())
            {
                if (handled == null)
                {
                    handled = new LinkedList<ExceptionQueuedEvent>();
                }
                
                FacesException toThrow = null;
                
                do
                {
                    // For each ExceptionEvent in the list
                    
                    // get the event to handle
                    ExceptionQueuedEvent event = unhandled.peek();
                    try
                    {
                        // call its getContext() method
                        ExceptionQueuedEventContext context = event.getContext();
                        
                        // and call getException() on the returned result
                        Throwable exception = context.getException();

                        // spec described behaviour of PreJsf2ExceptionHandler

                        // UpdateModelException needs special treatment here
                        if (exception instanceof UpdateModelException)
                        {
                            FacesMessage message = ((UpdateModelException) exception).getFacesMessage();
                            // Log a SEVERE message to the log
                            log.log(Level.SEVERE, message.getSummary(), exception.getCause());
                            // queue the FacesMessage on the FacesContext
                            UIComponent component = context.getComponent();
                            String clientId = null;
                            if (component != null)
                            {
                                clientId = component.getClientId(context.getContext());
                            }
                            context.getContext().addMessage(clientId, message);
                        }
                        else if (!shouldSkip(exception) && !context.inBeforePhase() && !context.inAfterPhase())
                        {
                            // set handledAndThrown so that getHandledExceptionQueuedEvent() returns this event
                            handledAndThrown = event;

                            // Re-wrap toThrow in a ServletException or
                            // (PortletException, if in a portlet environment)
                            // and throw it
                            // FIXME: The spec says to NOT use a FacesException
                            // to propagate the exception, but I see
                            //        no other way as ServletException is not a RuntimeException
                            toThrow = wrap(getRethrownException(exception));
                            break;
                        }
                        else
                        {
                            // Testing mojarra it logs a message and the exception
                            // however, this behaviour is not mentioned in the spec
                            log.log(Level.SEVERE, exception.getClass().getName() + " occured while processing " +
                                    (context.inBeforePhase() ? "beforePhase() of " : 
                                            (context.inAfterPhase() ? "afterPhase() of " : "")) + 
                                    "phase " + context.getPhaseId() + ": " +
                                    "UIComponent-ClientId=" + 
                                    (context.getComponent() != null ? 
                                            context.getComponent().getClientId(context.getContext()) : "") + ", " +
                                    "Message=" + exception.getMessage());

                            log.log(Level.SEVERE, exception.getMessage(), exception);
                        }
                    }
                    catch (Throwable t)
                    {
                        // A FacesException must be thrown if a problem occurs while performing
                        // the algorithm to handle the exception
                        throw new FacesException("Could not perform the algorithm to handle the Exception", t);
                    }
                    finally
                    {
                        // if we will throw the Exception or if we just logged it,
                        // we handled it in either way --> add to handled
                        handled.add(event);
                        unhandled.remove(event);
                    }
                } while (!unhandled.isEmpty());
                
                // do we have to throw an Exception?
                if (toThrow != null)
                {
                    throw toThrow;
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isListenerForSource(Object source)
        {
            return source instanceof ExceptionQueuedEventContext;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void processEvent(SystemEvent exceptionQueuedEvent) throws AbortProcessingException
        {
            if (unhandled == null)
            {
                unhandled = new LinkedList<ExceptionQueuedEvent>();
            }
            
            unhandled.add((ExceptionQueuedEvent)exceptionQueuedEvent);
        }
        
        protected Throwable getRethrownException(Throwable exception)
        {
            // Let toRethrow be either the result of calling getRootCause() on the Exception, 
            // or the Exception itself, whichever is non-null
            Throwable toRethrow = getRootCause(exception);
            if (toRethrow == null)
            {
                toRethrow = exception;
            }
            
            return toRethrow;
        }
        
        protected FacesException wrap(Throwable exception)
        {
            if (exception instanceof FacesException)
            {
                return (FacesException) exception;
            }
            return new FacesException(exception);
        }
        
        protected boolean shouldSkip(Throwable exception)
        {
            return exception instanceof AbortProcessingException;
        }
    }
}
