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
package org.apache.myfaces.application;

import javax.el.MethodExpression;
import javax.faces.application.Application;
import javax.faces.application.ConfigurableNavigationHandler;
import javax.faces.application.NavigationCase;
import javax.faces.application.NavigationHandler;
import javax.faces.component.ActionSource;
import javax.faces.component.ActionSource2;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;
import javax.faces.event.PhaseId;
import org.apache.myfaces.view.facelets.ViewPoolProcessor;
import org.apache.myfaces.view.facelets.pool.ViewPool;


/**
 * @author Manfred Geiler (latest modification by $Author: lu4242 $)
 * @author Anton Koinov
 * @version $Revision: 1550609 $ $Date: 2013-12-13 01:18:08 +0000 (Fri, 13 Dec 2013) $
 */
public class ActionListenerImpl implements ActionListener
{
    public void processAction(ActionEvent actionEvent) throws AbortProcessingException
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Application application = facesContext.getApplication();
        UIComponent component = actionEvent.getComponent();
        
        MethodExpression methodExpression = null;
        MethodBinding methodBinding = null;
        
        String fromAction = null;
        String outcome = null;
        
        if (component instanceof ActionSource2)
        {
            // Must be an instance of ActionSource2, so don't look on action if the actionExpression is set 
            methodExpression = ((ActionSource2) component).getActionExpression();            
        }
        if (methodExpression == null && component instanceof ActionSource)
        {
            // Backwards compatibility for pre-1.2.
            methodBinding = ((ActionSource) component).getAction();
        }
        
        if (methodExpression != null)
        {
            fromAction = methodExpression.getExpressionString();

            Object objOutcome = methodExpression.invoke(facesContext.getELContext(), null);
            if (objOutcome != null)
            {
                outcome = objOutcome.toString();
            }
            
        }
        
        else if (methodBinding != null)
        {
            fromAction = methodBinding.getExpressionString();
            Object objOutcome = methodBinding.invoke(facesContext, null);

            if (objOutcome != null)
            {
                outcome = objOutcome.toString();
            }

        }
        
        UIViewRoot root = facesContext.getViewRoot();
        ViewPoolProcessor processor = ViewPoolProcessor.getInstance(facesContext);
        ViewPool pool = (processor != null) ? processor.getViewPool(facesContext, root) : null;
        if (pool != null && pool.isDeferredNavigationEnabled() && 
            processor.isViewPoolStrategyAllowedForThisView(facesContext, root) &&
            (PhaseId.INVOKE_APPLICATION.equals(facesContext.getCurrentPhaseId()) ||
             PhaseId.APPLY_REQUEST_VALUES.equals(facesContext.getCurrentPhaseId())) )
        {
            NavigationHandler navigationHandler = application.getNavigationHandler();
            if (navigationHandler instanceof ConfigurableNavigationHandler)
            {
                NavigationCase navigationCase = ((ConfigurableNavigationHandler) navigationHandler).
                    getNavigationCase(facesContext, fromAction, outcome);
                if (navigationCase != null)
                {
                    // Deferred invoke navigation. The first one wins
                    if (!facesContext.getAttributes().containsKey(ViewPoolProcessor.INVOKE_DEFERRED_NAVIGATION))
                    {
                        String toFlowDocumentId = (component != null) ? 
                            (String) component.getAttributes().get(ActionListener.TO_FLOW_DOCUMENT_ID_ATTR_NAME) : null;
                        if (toFlowDocumentId != null)
                        {
                            facesContext.getAttributes().put(ViewPoolProcessor.INVOKE_DEFERRED_NAVIGATION, 
                                    new Object[]{fromAction, outcome, toFlowDocumentId});
                        }
                        else
                        {
                            facesContext.getAttributes().put(ViewPoolProcessor.INVOKE_DEFERRED_NAVIGATION, 
                                    new Object[]{fromAction, outcome});
                        }
                    }
                }
            }
        }
        else
        {
            NavigationHandler navigationHandler = application.getNavigationHandler();
            String toFlowDocumentId = (component != null) ? 
                (String) component.getAttributes().get(ActionListener.TO_FLOW_DOCUMENT_ID_ATTR_NAME) : null;

            if (toFlowDocumentId != null)
            {
                navigationHandler.handleNavigation(facesContext, fromAction, outcome, toFlowDocumentId);
            }
            else
            {
                navigationHandler.handleNavigation(facesContext, fromAction, outcome);
            }
            //Render Response if needed
            facesContext.renderResponse();
        }
    }
}
