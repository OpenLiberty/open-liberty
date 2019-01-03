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
package javax.faces.component;

import javax.el.MethodExpression;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextWrapper;
import javax.faces.el.MethodBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;
import javax.faces.event.FacesEvent;
import javax.faces.event.PhaseId;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFListener;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;

/**
 * @since 2.2
 */
@JSFComponent(name = "f:viewAction")
public class UIViewAction extends UIComponentBase implements ActionSource2
{
    //private static final Logger log = Logger.getLogger(UIViewAction.class.getName());
    public static final String COMPONENT_FAMILY = "javax.faces.ViewAction";
    public static final String COMPONENT_TYPE = "javax.faces.ViewAction";
    
    /**
     * Key in facesContext attribute map to check if a viewAction broadcast is 
     * being processed. This is used to check when a JSF lifecycle restart is required
     * by the NavigationHandler implementation.
     */
    private static final String BROADCAST_PROCESSING_KEY = "oam.viewAction.broadcast";
    
    /**
     * Key in facesContext attribute map to count the number of viewAction events that 
     * remains to be processed.
     */
    private static final String EVENT_COUNT_KEY = "oam.viewAction.eventCount";

    public UIViewAction()
    {
        setRendererType(null);
    }

    @Override
    public boolean isRendered() 
    {
        return (Boolean) getStateHelper().eval(PropertyKeys.renderedAttr, true);
    }

    @Override
    public void setRendered(final boolean condition) 
    {
        getStateHelper().put(PropertyKeys.renderedAttr, condition);
    }

    @Override
    public void broadcast(FacesEvent event) throws AbortProcessingException
    {
        super.broadcast(event);
        
        FacesContext context = getFacesContext();
        
        if (context.getResponseComplete())
        {
            return;
        }
        
        UIComponent c = event.getComponent();
        UIViewRoot sourceViewRoot = null;
        do
        {
            if (c instanceof UIViewRoot)
            {
                sourceViewRoot = (UIViewRoot) c;
                break;
            }
            else
            {
                c = c.getParent();
            }
        } while (c != null);
        
        if (!context.getViewRoot().equals(sourceViewRoot))
        {
            return;
        }
        
        if (event instanceof ActionEvent)
        {
            ActionListener defaultActionListener = context.getApplication().getActionListener();
            if (defaultActionListener != null)
            {
                String  viewIdBeforeAction = context.getViewRoot().getViewId();
                Boolean oldBroadcastProcessing = (Boolean) context.getAttributes().
                    get(BROADCAST_PROCESSING_KEY);
                try
                {
                    context.getAttributes().put(BROADCAST_PROCESSING_KEY, Boolean.TRUE);

                    ViewActionFacesContextWrapper wrappedFacesContext = new ViewActionFacesContextWrapper(context);

                    try
                    {
                        wrappedFacesContext.setWrapperAsCurrentFacesContext();
                        if (event instanceof ViewActionEvent)
                        {
                            ((ViewActionEvent) event).setFacesContext(wrappedFacesContext);
                        }

                        MethodBinding mb = getActionListener();
                        if (mb != null)
                        {
                            mb.invoke(context, new Object[]
                            { event });
                        }

                        if (defaultActionListener != null)
                        {
                            defaultActionListener.processAction((ActionEvent) event);
                        }
                        
                        // Decrement count
                        Integer count = (Integer) context.getAttributes().get(EVENT_COUNT_KEY);
                        count = (count == null) ? 0 : count - 1;
                        context.getAttributes().put(EVENT_COUNT_KEY, count);
                    }
                    finally
                    {
                        wrappedFacesContext.restoreCurrentFacesContext();
                    }
                }
                finally
                {
                    context.getAttributes().put(BROADCAST_PROCESSING_KEY, 
                        oldBroadcastProcessing == null ? Boolean.FALSE : oldBroadcastProcessing);
                }

                if (context.getResponseComplete())
                {
                    return;
                }
                else
                {
                    Integer count = (Integer) context.getAttributes().get(EVENT_COUNT_KEY);
                    count = (count == null) ? 0 : count;
                    String viewIdAfterAction = context.getViewRoot().getViewId();

                    if (viewIdBeforeAction.equals(viewIdAfterAction) && count == 0)
                    {
                        context.renderResponse();
                    }
                    // "... Otherwise, execute the lifecycle on the new UIViewRoot ..."
                    // Note these words are implemented in the NavigationHandler, but 
                    // the original proposal from seam s:viewAction did a trick here 
                    // to restart the JSF lifecycle.
                }
            }
        }
    }

    @Override
    public void decode(FacesContext context)
    {
        super.decode(context);
        
        if (context.isPostback() && !isOnPostback())
        {
            return;
        }
        
        if (!isRendered())
        {
            return;
        }
        
        ActionEvent evt = new ViewActionEvent(this);
        String phase = getPhase();
        PhaseId phaseId = (phase != null) ? PhaseId.phaseIdValueOf(phase) :
            isImmediate() ? PhaseId.APPLY_REQUEST_VALUES : PhaseId.INVOKE_APPLICATION;
        evt.setPhaseId(phaseId);
        this.queueEvent(evt);
        
        // "... Keep track of the number of events that are queued in this way 
        // on this run through the lifecycle. ...". The are two options:
        // 1. Use an attribute over FacesContext attribute map
        // 2. Use an attribute over the component
        // If the view is recreated again with the same viewId, the component 
        // state get lost, so the option 1 is preferred.
        Integer count = (Integer) context.getAttributes().get(EVENT_COUNT_KEY);
        count = (count == null) ? 1 : count + 1;
        context.getAttributes().put(EVENT_COUNT_KEY, count);
    }

    @Deprecated
    public MethodBinding getAction()
    {
        MethodExpression actionExpression = getActionExpression();
        if (actionExpression instanceof _MethodBindingToMethodExpression)
        {
            return ((_MethodBindingToMethodExpression) actionExpression)
                    .getMethodBinding();
        }
        if (actionExpression != null)
        {
            return new _MethodExpressionToMethodBinding(actionExpression);
        }
        return null;
    }

    /**
     * @deprecated Use setActionExpression instead.
     */
    @Deprecated
    public void setAction(MethodBinding action)
    {
        if (action != null)
        {
            setActionExpression(new _MethodBindingToMethodExpression(action));
        }
        else
        {
            setActionExpression(null);
        }
    }

    @JSFProperty
    public boolean isImmediate()
    {
        return (Boolean) getStateHelper().eval(PropertyKeys.immediate, Boolean.FALSE);
    }

    public void setImmediate(boolean immediate)
    {
        getStateHelper().put(PropertyKeys.immediate, immediate );
    }

    @JSFProperty(stateHolder=true, returnSignature = "java.lang.Object", jspName = "action", clientEvent="action")
    public MethodExpression getActionExpression()
    {
        return (MethodExpression) getStateHelper().eval(PropertyKeys.actionExpression);
    }

    public void setActionExpression(MethodExpression actionExpression)
    {
        getStateHelper().put(PropertyKeys.actionExpression, actionExpression);
    }

    @Deprecated
    @JSFProperty(stateHolder=true, returnSignature = "void", methodSignature = "javax.faces.event.ActionEvent")
    public MethodBinding getActionListener()
    {
        return (MethodBinding) getStateHelper().eval(PropertyKeys.actionListener);
    }

    /**
     * @deprecated
     */
    @Deprecated
    @JSFProperty(returnSignature="void",methodSignature="javax.faces.event.ActionEvent")
    public void setActionListener(MethodBinding actionListener)
    {
        getStateHelper().put(PropertyKeys.actionListener, actionListener);
        // Note f:viewAction does not have actionListener property defined.
        //throw new UnsupportedOperationException();
    }

    public void addActionListener(ActionListener listener)
    {
        addFacesListener(listener);
    }

    public void removeActionListener(ActionListener listener)
    {
        removeFacesListener(listener);
    }

    @JSFListener(event="javax.faces.event.ActionEvent",
            phases="Invoke Application, Apply Request Values")
    public ActionListener[] getActionListeners()
    {
        return (ActionListener[]) getFacesListeners(ActionListener.class);
    }
    
    @JSFProperty
    public String getPhase()
    {
        return (String) getStateHelper().get(PropertyKeys.phase);
    }
    
    public void setPhase(String phase)
    {
        getStateHelper().put(PropertyKeys.phase, phase);
    }
    
    @JSFProperty
    public boolean isOnPostback()
    {
        return (Boolean) getStateHelper().eval(PropertyKeys.onPostback, Boolean.FALSE);
    }

    public void setOnPostback(boolean onPostback)
    {
        getStateHelper().put(PropertyKeys.onPostback, onPostback );
    }    
    
    public static boolean isProcessingBroadcast(FacesContext context)
    {
        return Boolean.TRUE.equals(context.getAttributes().get(BROADCAST_PROCESSING_KEY));
    }

    enum PropertyKeys
    {
         immediate
        , value
        , actionExpression
        , actionListener
        , phase
        , onPostback
        , renderedAttr("if");

        private String name;

        PropertyKeys() 
        {
        }

        PropertyKeys(final String name) 
        {
            this.name = name;
        }

        @Override
        public String toString() 
        {
            return name != null ? name : super.toString();
        }
    }

    @Override
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }
    
    private static class ViewActionEvent extends ActionEvent
    {
        private transient FacesContext facesContext;

        public ViewActionEvent(FacesContext facesContext, UIComponent uiComponent)
        {
            super(facesContext, uiComponent);
            this.facesContext = facesContext;
        }

        public ViewActionEvent(UIComponent uiComponent)
        {
            super(uiComponent);
        }
        
        public FacesContext getFacesContext()
        {
            if (facesContext != null)
            {
                return facesContext;
            }
            return super.getFacesContext();
        }
        
        void setFacesContext(FacesContext facesContext)
        {
            this.facesContext = facesContext;
        }
    }
    
    private static class ViewActionFacesContextWrapper extends FacesContextWrapper
    {
        private FacesContext delegate;
        private boolean renderResponseCalled;

        public ViewActionFacesContextWrapper(FacesContext delegate)
        {
            this.delegate = delegate;
            this.renderResponseCalled = false;
        }

        @Override
        public void renderResponse()
        {
            //Take no action
            renderResponseCalled = true;
        }
        
        public boolean isRenderResponseCalled()
        {
            return renderResponseCalled;
        }
        
        @Override
        public FacesContext getWrapped()
        {
            return delegate;
        }
        
        void setWrapperAsCurrentFacesContext()
        {
            setCurrentInstance(this);
        }
        
        void restoreCurrentFacesContext()
        {
            setCurrentInstance(delegate);
        }
    }
}
