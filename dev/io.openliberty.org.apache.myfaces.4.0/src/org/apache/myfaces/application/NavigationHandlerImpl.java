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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.regex.Pattern;
import jakarta.el.MethodExpression;
import jakarta.faces.FacesException;
import jakarta.faces.application.ConfigurableNavigationHandler;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.application.NavigationCase;
import jakarta.faces.application.ProjectStage;
import jakarta.faces.application.ViewHandler;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIViewAction;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.component.visit.VisitCallback;
import jakarta.faces.component.visit.VisitContext;
import jakarta.faces.component.visit.VisitResult;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.PartialViewContext;
import jakarta.faces.flow.Flow;
import jakarta.faces.flow.FlowCallNode;
import jakarta.faces.flow.FlowHandler;
import jakarta.faces.flow.FlowNode;
import jakarta.faces.flow.MethodCallNode;
import jakarta.faces.flow.Parameter;
import jakarta.faces.flow.ReturnNode;
import jakarta.faces.flow.SwitchCase;
import jakarta.faces.flow.SwitchNode;
import jakarta.faces.flow.ViewNode;
import jakarta.faces.view.ViewDeclarationLanguage;
import jakarta.faces.view.ViewMetadata;

import org.apache.myfaces.config.RuntimeConfig;
import org.apache.myfaces.config.element.NavigationRule;
import org.apache.myfaces.flow.FlowHandlerImpl;
import org.apache.myfaces.core.api.shared.lang.SharedStringBuilder;
import org.apache.myfaces.util.lang.ClassUtils;
import org.apache.myfaces.util.lang.HashMapUtils;
import org.apache.myfaces.util.lang.StringUtils;
import org.apache.myfaces.util.lang.FilenameUtils;
import org.apache.myfaces.component.visit.MyFacesVisitHints;
import org.apache.myfaces.util.NavigationUtils;
import org.apache.myfaces.view.facelets.ViewPoolProcessor;
import org.apache.myfaces.view.facelets.tag.faces.PreDisposeViewEvent;

/**
 * @author Thomas Spiegl (latest modification by $Author$)
 * @author Anton Koinov
 * @version $Revision$ $Date$
 */
public class NavigationHandlerImpl extends ConfigurableNavigationHandler
{
    private static final Logger log = Logger.getLogger(NavigationHandlerImpl.class.getName());

    public static final String CALL_PRE_DISPOSE_VIEW = "oam.CALL_PRE_DISPOSE_VIEW";
    
    private static final String OUTCOME_NAVIGATION_SB = "oam.navigation.OUTCOME_NAVIGATION_SB";
    
    private static final Pattern AMP_PATTERN = Pattern.compile("&(amp;)?"); // "&" or "&amp;"
    
    private static final String ASTERISK = "*";

    private Map<String, Set<NavigationCase>> _navigationCases = null;
    private List<_WildcardPattern> _wildcardPatterns = new ArrayList<>();
    private Boolean _developmentStage;
    
    private Map<String, _FlowNavigationStructure> _flowNavigationStructureMap = new ConcurrentHashMap<>();
    
    private ViewIdSupport viewIdSupport;

    public final String STARTED_FLOW_TRANSITION = "STARTED_FLOW_TRANSITION";

    public NavigationHandlerImpl()
    {
        if (log.isLoggable(Level.FINEST))
        {
            log.finest("New NavigationHandler instance created");
        }
    }

    @Override
    public void handleNavigation(FacesContext facesContext, String fromAction, String outcome)
    {
        handleNavigation(facesContext, fromAction, outcome, null);
    }

    @Override
    public void handleNavigation(FacesContext facesContext, String fromAction, 
        String outcome, String toFlowDocumentId)
    {
        NavigationContext navigationContext = new NavigationContext();
        NavigationCase navigationCase = null;
        try
        {
            navigationCase = getNavigationCommand(facesContext, navigationContext, fromAction, outcome,
                    toFlowDocumentId);
        }
        finally
        {
            navigationContext.finish(facesContext);
        }

        if (navigationCase != null)
        {
            if (log.isLoggable(Level.FINEST))
            {
                log.finest("handleNavigation fromAction=" + fromAction + " outcome=" + outcome +
                          " toViewId =" + navigationCase.getToViewId(facesContext) +
                          " redirect=" + navigationCase.isRedirect());
            }
            boolean isViewActionProcessingBroadcastAndRequiresRedirect = false;
            if (UIViewAction.isProcessingBroadcast(facesContext))
            {
                // f:viewAction tag always triggers a redirect to enforce execution of 
                // the lifecycle again. Note this requires enables flash scope 
                // keepMessages automatically, because a view action can add messages
                // and these ones requires to be renderer afterwards.
                facesContext.getExternalContext().getFlash().setKeepMessages(true);
                String fromViewId = (facesContext.getViewRoot() == null) ? null :
                    facesContext.getViewRoot().getViewId();
                String toViewId = navigationCase.getToViewId(facesContext);
                // A redirect is required only if the viewId changes. If the viewId
                // does not change, section 7.4.2 says that a redirect/restart Faces
                // lifecycle is not necessary.
                if (fromViewId == null && toViewId != null)
                {
                    isViewActionProcessingBroadcastAndRequiresRedirect = true;
                }
                else if (fromViewId != null && !fromViewId.equals(toViewId))
                {
                    isViewActionProcessingBroadcastAndRequiresRedirect = true;
                }
            }
            if (navigationCase.isRedirect() || isViewActionProcessingBroadcastAndRequiresRedirect)
            { 
                // Need to add the FlowHandler parameters here.
                FlowHandler flowHandler = facesContext.getApplication().getFlowHandler();
                List<Flow> activeFlows = FlowHandlerImpl.getActiveFlows(facesContext, flowHandler);
                Flow currentFlow = flowHandler.getCurrentFlow(facesContext);
                Flow targetFlow = calculateTargetFlow(facesContext, outcome, flowHandler, 
                                                      activeFlows, toFlowDocumentId);
                
                Map<String, List<String>> navigationCaseParameters = navigationCase.getParameters();
                
                // Spec: If this navigation is a flow transition (where current flow is not the same as the new flow)
                // sourceFlow and targetFlow could both be null so need to have multiple checks here
                if (currentFlow != targetFlow)
                { 
                    // Ensure that at least one has a value and check for equality
                    if ((currentFlow != null && !currentFlow.equals(targetFlow)) ||
                                    (targetFlow != null && !targetFlow.equals(currentFlow)))
                    {
                        // MYFACES-4504: check isEmpty()
                        if (navigationCaseParameters == null || navigationCaseParameters.isEmpty())
                        {
                            navigationCaseParameters = new HashMap<>(5, 1f);
                        }
                        
                        // If current flow (sourceFlow) is not null and new flow (targetFlow) is null,
                        // include the following entries:
                        if (currentFlow != null && targetFlow == null)
                        {
                            // Set the TO_FLOW_DOCUMENT_ID_REQUEST_PARAM_NAME parameter
                            navigationCaseParameters.put(FlowHandler.TO_FLOW_DOCUMENT_ID_REQUEST_PARAM_NAME,
                                    Arrays.asList(FlowHandler.NULL_FLOW));
                    
                            // Set the FLOW_ID_REQUEST_PARAM_NAME
                            navigationCaseParameters.put(FlowHandler.FLOW_ID_REQUEST_PARAM_NAME,
                                    Arrays.asList(""));
                        }
                        else
                        {
                            // If current flow (sourceFlow) is null and new flow (targetFlow) is not null,
                            // include the following entries:
                            // If we make it this far we know the above statement is true due to the other
                            // logical checks we have hit to this point.
                            // Set the TO_FLOW_DOCUMENT_ID_REQUEST_PARAM_NAME parameter
                            navigationCaseParameters.put(FlowHandler.TO_FLOW_DOCUMENT_ID_REQUEST_PARAM_NAME,
                                    Arrays.asList((toFlowDocumentId == null ? "" : toFlowDocumentId)));
                            
                            // Set the FLOW_ID_REQUEST_PARAM_NAME
                            navigationCaseParameters.put(FlowHandler.FLOW_ID_REQUEST_PARAM_NAME,
                                    Arrays.asList(targetFlow.getId()));
                        }
                    }
                }            

                ExternalContext externalContext = facesContext.getExternalContext();
                ViewHandler viewHandler = facesContext.getApplication().getViewHandler();
                String toViewId = navigationCase.getToViewId(facesContext);
                
                String redirectPath = viewHandler.getRedirectURL(
                        facesContext, toViewId, 
                        NavigationUtils.getEvaluatedNavigationParameters(facesContext, navigationCaseParameters),
                        navigationCase.isIncludeViewParams());
                
                // The spec doesn't say anything about how to handle redirect but it is
                // better to apply the transition here where we have already calculated the
                // route than add the parameters and delegate to 
                // FlowHandler.clientWindowTransition(facesContext)
                applyFlowTransition(facesContext, navigationContext);
                
                //Clear ViewMap if we are redirecting to other resource
                UIViewRoot viewRoot = facesContext.getViewRoot(); 
                if (viewRoot != null && !toViewId.equals(viewRoot.getViewId()))
                {
                    //call getViewMap(false) to prevent unnecessary map creation
                    Map<String, Object> viewMap = viewRoot.getViewMap(false);
                    if (viewMap != null)
                    {
                        viewMap.clear();
                    }
                }
                
                // Faces 2.0 the javadoc of handleNavigation() says something like this 
                // "...If the view has changed after an application action, call
                // PartialViewContext.setRenderAll(true)...". The effect is that ajax requests
                // are included on navigation.
                PartialViewContext partialViewContext = facesContext.getPartialViewContext();
                String viewId = facesContext.getViewRoot() != null ? facesContext.getViewRoot().getViewId() : null;
                if (partialViewContext.isPartialRequest()
                        && !partialViewContext.isRenderAll()
                        && toViewId != null
                        && !toViewId.equals(viewId))
                {
                    partialViewContext.setRenderAll(true);
                }

                // Dispose view if the view has been marked as disposable by default action listener
                ViewPoolProcessor processor = ViewPoolProcessor.getInstance(facesContext);
                if (processor != null && 
                    processor.isViewPoolEnabledForThisView(facesContext, facesContext.getViewRoot()))
                {
                    processor.disposeView(facesContext, facesContext.getViewRoot());
                }
                
                // Faces 2.0 Spec call Flash.setRedirect(true) to notify Flash scope and take proper actions
                externalContext.getFlash().setRedirect(true);
                try
                {
                    externalContext.redirect(redirectPath);
                    facesContext.responseComplete();
                }
                catch (IOException e)
                {
                    throw new FacesException(e.getMessage(), e);
                }
            }
            else
            {
                ViewHandler viewHandler = facesContext.getApplication().getViewHandler();
                //create new view
                String newViewId = navigationCase.getToViewId(facesContext);

                // Faces 2.0 the javadoc of handleNavigation() says something like this 
                // "...If the view has changed after an application action, call
                // PartialViewContext.setRenderAll(true)...". The effect is that ajax requests
                // are included on navigation.
                PartialViewContext partialViewContext = facesContext.getPartialViewContext();
                String viewId = facesContext.getViewRoot() != null ? facesContext.getViewRoot().getViewId() : null;
                if ( partialViewContext.isPartialRequest() && 
                     !partialViewContext.isRenderAll() && 
                     newViewId != null &&
                     !newViewId.equals(viewId))
                {
                    partialViewContext.setRenderAll(true);
                }

                if (facesContext.getViewRoot() != null &&
                    facesContext.getViewRoot().getAttributes().containsKey(CALL_PRE_DISPOSE_VIEW))
                {
                    try
                    {
                        facesContext.getAttributes().put(MyFacesVisitHints.SKIP_ITERATION_HINT, Boolean.TRUE);

                        VisitContext visitContext = VisitContext.createVisitContext(facesContext,
                                null, MyFacesVisitHints.SET_SKIP_ITERATION);
                        facesContext.getViewRoot().visitTree(visitContext, PreDisposeViewCallback.INSTANCE);
                    }
                    finally
                    {
                        facesContext.getAttributes().remove(MyFacesVisitHints.SKIP_ITERATION_HINT);
                    }
                }
                
                applyFlowTransition(facesContext, navigationContext);

                // Dispose view if the view has been marked as disposable by default action listener
                ViewPoolProcessor processor = ViewPoolProcessor.getInstance(facesContext);
                if (processor != null && 
                    processor.isViewPoolEnabledForThisView(facesContext, facesContext.getViewRoot()))
                {
                    processor.disposeView(facesContext, facesContext.getViewRoot());
                }
                
                // create UIViewRoot for new view
                UIViewRoot viewRoot = null;
                
                String derivedViewId = viewHandler.deriveViewId(facesContext, newViewId);
                if (derivedViewId != null)
                {
                    ViewDeclarationLanguage vdl = viewHandler.getViewDeclarationLanguage(facesContext, derivedViewId);
                    if (vdl != null)
                    {
                        ViewMetadata metadata = vdl.getViewMetadata(facesContext, newViewId);
                        if (metadata != null)
                        {
                            viewRoot = metadata.createMetadataView(facesContext);
                        }
                    }
                }
                
                // viewRoot can be null here, if ...
                //   - we don't have a ViewDeclarationLanguage (e.g. when using facelets-1.x)
                //   - there is no view metadata or metadata.createMetadataView() returned null
                //   - viewHandler.deriveViewId() returned null
                if (viewRoot == null)
                {
                    viewRoot = viewHandler.createView(facesContext, newViewId);
                }
                
                facesContext.setViewRoot(viewRoot);
                facesContext.renderResponse();
            }
        }
        else
        {
            // no navigationcase found, stay on current ViewRoot
            if (log.isLoggable(Level.FINEST))
            {
                log.finest("handleNavigation fromAction=" + fromAction + " outcome=" + outcome +
                          " no matching navigation-case found, staying on current ViewRoot");
            }
        }
    }

    private Boolean didFlowTransitionAlready(FacesContext facesContext)
    {
        return facesContext.getAttributes().containsKey(STARTED_FLOW_TRANSITION);
    }
    
    private void applyFlowTransition(FacesContext facesContext, NavigationContext navigationContext)
    {
        //Apply Flow transition if any
        // Is any flow transition on the way?
        if (navigationContext != null
                && navigationContext.getSourceFlows() != null
                || (navigationContext.getTargetFlows() != null && !navigationContext.getTargetFlows().isEmpty()))
        {
            FlowHandler flowHandler = facesContext.getApplication().getFlowHandler();

            int size = navigationContext.getTargetFlows().size();
            // if we already transitioned (MYFACES-4553), skip the first entry
            for (int i = didFlowTransitionAlready(facesContext) ? 1 : 0; i < size; i++)
            {
                Flow sourceFlow = navigationContext.getSourceFlows().get(i);
                Flow targetFlow = navigationContext.getTargetFlows().get(i);

                flowHandler.transition(facesContext, sourceFlow, targetFlow, 
                    navigationContext.getFlowCallNodes().get(i), 
                    navigationContext.getNavigationCase().getToViewId(facesContext));
                sourceFlow = targetFlow;
            }
        }
    }

    protected ViewIdSupport getViewIdSupport()
    {
        if (viewIdSupport == null)
        {
            viewIdSupport = ViewIdSupport.getInstance(FacesContext.getCurrentInstance());
        }
        return viewIdSupport;
    }

    public void setViewIdSupport(ViewIdSupport viewIdSupport)
    {
        this.viewIdSupport = viewIdSupport;
    }

    private static class PreDisposeViewCallback implements VisitCallback
    {
        public static final PreDisposeViewCallback INSTANCE = new PreDisposeViewCallback();
        
        private PreDisposeViewCallback()
        {
        }
        
        @Override
        public VisitResult visit(VisitContext context, UIComponent target)
        {
            context.getFacesContext().getApplication().publishEvent(context.getFacesContext(),
                                                                    PreDisposeViewEvent.class, target);
            
            return VisitResult.ACCEPT;
        }
    }

    /**
     * Returns the navigation case that applies for the given action and outcome
     */
    @Override
    public NavigationCase getNavigationCase(FacesContext facesContext, String fromAction, String outcome)
    {
        NavigationContext navigationContext = new NavigationContext();
        try
        {
            return getNavigationCommand(facesContext, navigationContext, fromAction, outcome, null);
        }
        finally
        {
            navigationContext.finish(facesContext);
        }
    }
    
    public NavigationCase getNavigationCommandFromGlobalNavigationCases(
        FacesContext facesContext, String viewId, NavigationContext navigationContext, 
        String fromAction, String outcome)
    {
        Map<String, Set<NavigationCase>> casesMap = getNavigationCases();
        NavigationCase navigationCase = null;
        
        Set<? extends NavigationCase> casesSet;
        if (viewId != null)
        {
            casesSet = casesMap.get(viewId);
            if (casesSet != null)
            {
                // Exact match?
                navigationCase = calcMatchingNavigationCase(facesContext, casesSet, fromAction, outcome);
            }
        }

        if (navigationCase == null)
        {
            // Wildcard match?
            List<_WildcardPattern> wildcardPatterns = getSortedWildcardPatterns();
            
            for (int i = 0; i < wildcardPatterns.size(); i++)
            {
                _WildcardPattern wildcardPattern = wildcardPatterns.get(i);
                if (wildcardPattern.match(viewId))
                {
                    casesSet = casesMap.get(wildcardPattern.getPattern());
                    if (casesSet != null)
                    {
                        navigationCase = calcMatchingNavigationCase(facesContext, casesSet, fromAction, outcome);
                        if (navigationCase != null)
                        {
                            break;
                        }
                    }
                }
            }
        }
        return navigationCase;
    }
    
    private Flow calculateTargetFlow(FacesContext facesContext, String outcome, 
        FlowHandler flowHandler, List<Flow> activeFlows, String toFlowDocumentId)
    {
        Flow targetFlow = null;
        if (toFlowDocumentId != null)
        {
            targetFlow = flowHandler.getFlow(facesContext, toFlowDocumentId, outcome);
        }
        if (targetFlow == null && !activeFlows.isEmpty())
        {
            for (Flow currentFlow : activeFlows)
            {
                targetFlow = flowHandler.getFlow(facesContext, currentFlow.getDefiningDocumentId(), outcome);
                if (targetFlow != null)
                {
                    break;
                }
            }
        }
        if (targetFlow == null)
        {
            targetFlow = flowHandler.getFlow(facesContext, "", outcome);
        }
        return targetFlow;
    }

    public NavigationCase getNavigationCommand(
        FacesContext facesContext, NavigationContext navigationContext, String fromAction, String outcome, 
        String toFlowDocumentId)
    {
        String viewId = facesContext.getViewRoot() != null ? facesContext.getViewRoot().getViewId() : null;
        NavigationCase navigationCase = getNavigationCommandFromGlobalNavigationCases(
                facesContext, viewId, navigationContext, fromAction, outcome);
        if (outcome != null && navigationCase == null)
        {
            FlowHandler flowHandler = facesContext.getApplication().getFlowHandler();
            List<Flow> activeFlows = FlowHandlerImpl.getActiveFlows(facesContext, flowHandler);
            // Faces 2.2 section 7.4.2: "... When outside of a flow, view identifier 
            // has the additional possibility of being a flow id.
            Flow targetFlow = calculateTargetFlow(facesContext, outcome, flowHandler, activeFlows, toFlowDocumentId);
            Flow currentFlow = navigationContext.getCurrentFlow(facesContext);
            FlowCallNode targetFlowCallNode = null;
            boolean startFlow = false;
            String startFlowDocumentId = null;
            String startFlowId = null;
            boolean checkFlowNode = false;
            String outcomeToGo = outcome;
            String actionToGo = fromAction;
            if (currentFlow != null)
            {
                // Faces 2.2 section 7.4.2: When inside a flow, a view identifier has 
                // the additional possibility of being the id of any node within the 
                // current flow or the id of another flow
                if (targetFlow != null)
                {
                    if (flowHandler.isActive(facesContext, targetFlow.getDefiningDocumentId(), targetFlow.getId()))
                    {
                        // If the flow is already active, there is a chance that a node id has the same name as
                        // the flow and if that so, give preference to that node instead reenter into the flow.
                        FlowNode flowNode = targetFlow.getNode(outcome);
                        if (flowNode != null)
                        {
                            checkFlowNode = true;
                        }
                        else
                        {
                            startFlow = true;
                        }
                    }
                    else
                    {
                        startFlow = true;
                    }
                }
                else
                {
                    checkFlowNode = true;
                }
            }
            else
            {
                if (targetFlow != null)
                {
                    startFlow = true;
                }
            }
            if (!startFlow)
            {
                for (Flow activeFlow : activeFlows)
                {
                    FlowNode node = activeFlow.getNode(outcome);
                    if (node != null)
                    {
                        currentFlow = activeFlow;
                        break;
                    }
                    _FlowNavigationStructure flowNavigationStructure = _flowNavigationStructureMap.get(
                            activeFlow.getId());
                    navigationCase = getNavigationCaseFromFlowStructure(facesContext, 
                            flowNavigationStructure, fromAction, outcome, viewId);
                    if (navigationCase != null)
                    {
                        currentFlow = activeFlow;
                        break;
                    }
                }
            }
            // If is necessary to enter a flow or there is a current
            // flow and it is necessary to check a flow node
            if (startFlow || (checkFlowNode && currentFlow != null))
            {
                boolean complete = false;
                boolean checkNavCase = true;
                while (!complete && (startFlow || checkFlowNode))
                {
                    if (startFlow)
                    {
                        if (flowHandler.isActive(facesContext, targetFlow.getDefiningDocumentId(), targetFlow.getId())
                            && targetFlowCallNode == null)
                        {
                            // Add the transition to exit from the flow
                            Flow baseReturnFlow = navigationContext.getCurrentFlow(facesContext);
                            // This is the part when the pseudo "recursive call" is done. 
                            while (baseReturnFlow != null && !(baseReturnFlow.getDefiningDocumentId().equals(
                                    targetFlow.getDefiningDocumentId()) &&
                                   baseReturnFlow.getId().equals(targetFlow.getId())) )
                            {
                                navigationContext.popFlow(facesContext);
                                baseReturnFlow = navigationContext.getCurrentFlow(facesContext);
                            }
                            navigationContext.popFlow(facesContext);
                            currentFlow = navigationContext.getCurrentFlow(facesContext);
                            navigationContext.addTargetFlow(baseReturnFlow, currentFlow, null);
                        }
                        if (startFlowId == null)
                        {
                            startFlowDocumentId = targetFlow.getDefiningDocumentId();
                            startFlowId = targetFlowCallNode == null ? targetFlow.getId() : targetFlowCallNode.getId();
                        }
                        navigationContext.addTargetFlow(currentFlow, targetFlow, targetFlowCallNode);
                        targetFlowCallNode = null;
                        // Since we start a new flow, the current flow is now the
                        // target flow.
                        navigationContext.pushFlow(facesContext, targetFlow);
                        currentFlow = targetFlow;
                        //No outboundCallNode.
                        //Resolve start node.
                        outcomeToGo = resolveStartNodeOutcome(targetFlow);
                        checkFlowNode = true;
                        startFlow = false;
                    }
                    if (checkFlowNode)
                    {
                        FlowNode flowNode = currentFlow.getNode(outcomeToGo);
                        if (flowNode != null)
                        {
                            checkNavCase = true;
                            // Note: Node Ordering changed in MYFACES-4553
                            if (!complete && flowNode instanceof ViewNode)
                            {
                                ViewNode viewNode = (ViewNode) flowNode;
                                navigationCase = createNavigationCase(viewId, flowNode.getId(), 
                                    viewNode.getVdlDocumentId());
                                complete = true;
                            }
                            if (!complete && flowNode instanceof ReturnNode)
                            {
                                ReturnNode returnNode = (ReturnNode) flowNode;
                                String fromOutcome = returnNode.getFromOutcome(facesContext);
                                actionToGo = currentFlow.getId();
                                Flow sourceFlow = currentFlow;
                                Flow baseReturnFlow = navigationContext.getCurrentFlow(facesContext);
                                // This is the part when the pseudo "recursive call" is done. 
                                while (baseReturnFlow != null && !(baseReturnFlow.getDefiningDocumentId().equals(
                                        currentFlow.getDefiningDocumentId()) &&
                                       baseReturnFlow.getId().equals(currentFlow.getId())) )
                                {
                                    navigationContext.popFlow(facesContext);
                                    baseReturnFlow = navigationContext.getCurrentFlow(facesContext);
                                }
                                navigationContext.popFlow(facesContext);
                                currentFlow = navigationContext.getCurrentFlow(facesContext);
                                navigationContext.addTargetFlow(sourceFlow, currentFlow, null);
                                outcomeToGo = fromOutcome;
                                String lastDisplayedViewId = navigationContext.getLastDisplayedViewId(facesContext, 
                                            currentFlow);
                                
                                // The part where FlowHandler.NULL_FLOW is passed as documentId causes the effect of
                                // do not take into account the documentId of the returned flow in the command. In 
                                // theory there is no Flow with defining documentId as FlowHandler.NULL_FLOW. It has 
                                // sense because the one who specify the return rules should be the current flow 
                                // after it is returned.
                                navigationCase = getNavigationCommand(facesContext, 
                                        navigationContext, actionToGo, outcomeToGo, FlowHandler.NULL_FLOW);
                                if (navigationCase != null)
                                {
                                    navigationCase = new FlowNavigationCase(navigationCase, 
                                        flowNode.getId(), FlowHandler.NULL_FLOW);
                                    complete = true;
                                }
                                else
                                {
                                    // No navigation case
                                    if (lastDisplayedViewId != null)
                                    {
                                        navigationCase = createNavigationCase(
                                            viewId, flowNode.getId(), lastDisplayedViewId, FlowHandler.NULL_FLOW);
                                        complete = true;
                                    }
                                }
                                if (currentFlow == null)
                                {
                                    complete = true;
                                }
                                continue;
                            }
                            if(!complete && flowHandler.getCurrentFlow() == null) // See  MYFACES-4553 for details
                            {
                                flowHandler.transition(facesContext, null, targetFlow, null, outcomeToGo);
                                facesContext.getAttributes().put(STARTED_FLOW_TRANSITION, true);
                                continue;
                            }
                            if (!complete && flowNode instanceof FlowCallNode)
                            {
                                // "... If the node is a FlowCallNode, save it aside as facesFlowCallNode. ..."
                                FlowCallNode flowCallNode = (FlowCallNode) flowNode;
                                targetFlow = calculateFlowCallTargetFlow(facesContext, 
                                    flowHandler, flowCallNode, currentFlow);
                                if (targetFlow != null)
                                {
                                    targetFlowCallNode = flowCallNode;
                                    startFlow = true;
                                    continue;
                                }
                                else
                                {
                                    // Ask the FlowHandler for a Flow for this flowId, flowDocumentId pair. Obtain a 
                                    // reference to the start node and execute this algorithm again, on that start node.
                                    complete = true;
                                }
                            }
                            if (!complete && flowNode instanceof SwitchNode)
                            {
                                outcomeToGo = calculateSwitchOutcome(facesContext, (SwitchNode) flowNode);
                                // Start over again checking if the node exists.
                                actionToGo = currentFlow.getId();
                                flowNode = currentFlow.getNode(outcomeToGo);
                                continue;
                            }
                            if (!complete && flowNode instanceof MethodCallNode)
                            {
                                MethodCallNode methodCallNode = (MethodCallNode) flowNode;
                                String vdlViewIdentifier = calculateVdlViewIdentifier(facesContext, methodCallNode);
                                // note a vdlViewIdentifier could be a flow node too
                                if (vdlViewIdentifier != null)
                                {
                                    outcomeToGo = vdlViewIdentifier;
                                    actionToGo = currentFlow.getId();
                                    continue;
                                }
                                else
                                {
                                    complete = true;
                                }
                            }
                            else
                            {
                                complete = true; //Should not happen
                            }
                        }
                        else if (checkNavCase)
                        {
                            // Not found in current flow.
                            _FlowNavigationStructure flowNavigationStructure = _flowNavigationStructureMap.get(
                                    currentFlow.getId());
                            navigationCase = getNavigationCaseFromFlowStructure(facesContext, 
                                    flowNavigationStructure, actionToGo, outcomeToGo, viewId);
                            
                            // Faces 2.2 section 7.4.2 "... any text that references a view identifier, such as 
                            // <from-view-id> or <to-view-id>, can also refer to a flow node ..."
                            if (navigationCase != null)
                            {
                                outcomeToGo = navigationCase.getToViewId(facesContext);
                                checkNavCase = false;
                            }
                            else
                            {
                                // No matter if navigationCase is null or not, complete the look.
                                complete = true;
                            }
                        }
                        else
                        {
                            complete = true;
                        }
                    }
                }
                if (outcomeToGo != null && navigationCase == null) // Apply implicit navigation rules over outcomeToGo
                {
                    navigationCase = getOutcomeNavigationCase (facesContext, actionToGo, outcomeToGo);
                }
            }
            if (startFlowId != null)
            {
                navigationCase = new FlowNavigationCase(navigationCase, startFlowId, startFlowDocumentId);
            }
        }
        if (outcome != null && navigationCase == null)
        {
            //if outcome is null, we don't check outcome based nav cases
            //otherwise, if navgiationCase is still null, check outcome-based nav cases
            navigationCase = getOutcomeNavigationCase (facesContext, fromAction, outcome);
        }
        if (outcome != null && navigationCase == null && !facesContext.isProjectStage(ProjectStage.Production))
        {
            final FacesMessage facesMessage = new FacesMessage("No navigation case match for viewId " + viewId + 
                    ",  action " + fromAction + " and outcome " + outcome);
            facesMessage.setSeverity(FacesMessage.SEVERITY_WARN);
            facesContext.addMessage(null, facesMessage);
        }
        if (navigationCase != null)
        {
            navigationContext.setNavigationCase(navigationCase);
        }
        return navigationContext.getNavigationCase(); // if navigationCase == null, will stay on current view
    }

    private String resolveStartNodeOutcome(Flow targetFlow)
    {
        String outcomeToGo;
        if (targetFlow.getStartNodeId() == null)
        {
            // In faces-config javadoc says:
            // "If there is no <start-node> element declared, it 
            //  is assumed to be <flowName>.xhtml."
            outcomeToGo = '/' + targetFlow.getId()+ '/' +
                targetFlow.getId() + ".xhtml";
        }
        else
        {
            outcomeToGo = targetFlow.getStartNodeId();
        }
        return outcomeToGo;
    }
    
    private String calculateSwitchOutcome(FacesContext facesContext, SwitchNode switchNode)
    {
        String outcomeToGo = null;
        boolean resolved = false;
        // "... iterate over the NavigationCase instances returned from its getCases()
        // method. For each, one call getCondition(). If the result is true, let vdl 
        // view identifier be the value of its fromOutcome property.
        for (SwitchCase switchCase : switchNode.getCases())
        {
            Boolean isConditionTrue = switchCase.getCondition(facesContext);
            if (Boolean.TRUE.equals(isConditionTrue))
            {
                outcomeToGo = switchCase.getFromOutcome();
                resolved = true;
                break;
            }
        }
        if (!resolved)
        {
            outcomeToGo = switchNode.getDefaultOutcome(facesContext);
        }
        return outcomeToGo;
    }
    
    private Flow calculateFlowCallTargetFlow(FacesContext facesContext, FlowHandler flowHandler,
        FlowCallNode flowCallNode, Flow currentFlow)
    {
        // " ... Let flowId be the value of its calledFlowId property and flowDocumentId 
        // be the value of its calledFlowDocumentId property. .."

        // " ... If no flowDocumentId exists for the node, let it be the string 
        // resulting from flowId + "/" + flowId + ".xhtml". ..." 
        // -=Leonardo Uribe=- flowDocumentId is inconsistent, waiting for answer of the EG,
        // for not let it be null.

        String calledFlowDocumentId = flowCallNode.getCalledFlowDocumentId(facesContext);
        if (calledFlowDocumentId == null)
        {
            calledFlowDocumentId = currentFlow.getDefiningDocumentId();
        }

        Flow targetFlow = flowHandler.getFlow(facesContext, 
            calledFlowDocumentId, 
            flowCallNode.getCalledFlowId(facesContext));
        if (targetFlow == null && StringUtils.isNotBlank(calledFlowDocumentId))
        {
            targetFlow = flowHandler.getFlow(facesContext, "", flowCallNode.getCalledFlowId(facesContext));
        }
        return targetFlow;
    }
    
    private String calculateVdlViewIdentifier(FacesContext facesContext, MethodCallNode methodCallNode)
    {
        String vdlViewIdentifier = null;
        MethodExpression method = methodCallNode.getMethodExpression();
        if (method != null)
        {
            Object value = invokeMethodCallNode(facesContext, methodCallNode);
            if (value != null)
            {
                vdlViewIdentifier = value.toString();
            }
            else if (methodCallNode.getOutcome() != null)
            {
                vdlViewIdentifier = methodCallNode.getOutcome().getValue(
                    facesContext.getELContext());
            }
        }
        return vdlViewIdentifier;
    }
    
    private Object invokeMethodCallNode(FacesContext facesContext, MethodCallNode methodCallNode)
    {
        MethodExpression method = methodCallNode.getMethodExpression();
        Object value = null;
        
        if (methodCallNode.getParameters() != null &&
            !methodCallNode.getParameters().isEmpty())
        {
            Object[] parameters = new Object[methodCallNode.getParameters().size()];
            Class[] clazzes = new Class[methodCallNode.getParameters().size()];
            for (int i = 0; i < methodCallNode.getParameters().size(); i++)
            {
                Parameter param = methodCallNode.getParameters().get(i);
                parameters[i] = param.getValue().getValue(facesContext.getELContext());
                clazzes[i] = param.getName() != null ? 
                    ClassUtils.simpleJavaTypeToClass(param.getName()) :
                    (parameters[i] == null ? String.class : parameters[i].getClass());
            }
            
            // Now we need to recreate the EL method expression with the correct clazzes as parameters.
            // We should do it per invocation, because we don't know the parameter type.
            method = facesContext.getApplication().getExpressionFactory().createMethodExpression(
                facesContext.getELContext(), method.getExpressionString(), null, clazzes);
            value = method.invoke(facesContext.getELContext(), parameters);
        }
        else
        {
            value = method.invoke(facesContext.getELContext(), null);
        }
        return value;
    }
    
    private NavigationCase getNavigationCaseFromFlowStructure(FacesContext facesContext, 
            _FlowNavigationStructure flowNavigationStructure, String fromAction, String outcome, String viewId)
    {
        Set<NavigationCase> casesSet = null;
        NavigationCase navigationCase = null;
        
        if (flowNavigationStructure == null)
        {
            return navigationCase;
        }
        if (viewId != null)
        {
            casesSet = flowNavigationStructure.getNavigationCases().get(viewId);
            if (casesSet != null)
            {
                // Exact match?
                navigationCase = calcMatchingNavigationCase(facesContext, casesSet, fromAction, outcome);
            }
        }
        if (navigationCase == null)
        {
            List<_WildcardPattern> wildcardPatterns = flowNavigationStructure.getWildcardKeys();
            for (int i = 0; i < wildcardPatterns.size(); i++)
            {
                _WildcardPattern wildcardPattern = wildcardPatterns.get(i);
                if (wildcardPattern.match(viewId))
                {
                    casesSet = flowNavigationStructure.getNavigationCases().get(wildcardPattern.getPattern());
                    if (casesSet != null)
                    {
                        navigationCase = calcMatchingNavigationCase(facesContext, casesSet, fromAction, outcome);
                        if (navigationCase != null)
                        {
                            break;
                        }
                    }
                }
            }
        }
        return navigationCase;
    }
    
    /**
     * Derive a NavigationCase from a flow node. 
     * 
     * @param flowNode
     * @return 
     */
    private NavigationCase createNavigationCase(String fromViewId, String outcome, String toViewId)
    {
        return new NavigationCase(fromViewId, null, outcome, null, toViewId, null, false, false);
    }
    
    private NavigationCase createNavigationCase(String fromViewId, String outcome, 
        String toViewId, String toFlowDocumentId)
    {
        return new NavigationCase(fromViewId, null, outcome, null, toViewId, 
            toFlowDocumentId, null, false, false);
    }
    
    /**
     * Performs the algorithm specified in 7.4.2 for situations where no navigation cases are defined and instead
     * the navigation case is to be determined from the outcome.
     * 
     * TODO: cache results?
     */
    private NavigationCase getOutcomeNavigationCase(FacesContext facesContext, String fromAction, String outcome)
    {
        String implicitViewId = null;
        boolean includeViewParams = false;
        int index;
        boolean isRedirect = false;
        String queryString = null;
        NavigationCase result = null;
        String viewId = facesContext.getViewRoot() != null ? facesContext.getViewRoot().getViewId() : null;
        StringBuilder viewIdToTest = SharedStringBuilder.get(facesContext, OUTCOME_NAVIGATION_SB);
        viewIdToTest.append(outcome);
        
        // If viewIdToTest contains a query string, remove it and set queryString with that value.
        index = viewIdToTest.indexOf("?");
        if (index != -1)
        {
            queryString = viewIdToTest.substring(index + 1);
            viewIdToTest.setLength(index);
            
            // If queryString contains "faces-redirect=true", set isRedirect to true.
            if (queryString.contains("faces-redirect=true"))
            {
                isRedirect = true;
            }
            
            // If queryString contains "includeViewParams=true" or 
            // "faces-include-view-params=true", set includeViewParams to true.
            if (queryString.contains("includeViewParams=true")
                    || queryString.contains("faces-include-view-params=true"))
            {
                includeViewParams = true;
            }
        }
        
        // If viewIdToTest does not have a "file extension", use the one from the current viewId.
        index = viewIdToTest.indexOf(".");
        if (index == -1)
        {
            if (viewId != null)
            {
                index = viewId.lastIndexOf('.');
                if (index != -1)
                {
                    viewIdToTest.append(viewId.substring (index));
                }
            }
            else
            {
                // This case happens when for for example there is a ViewExpiredException,
                // and a custom ExceptionHandler try to navigate using implicit navigation.
                // In this case, there is no UIViewRoot set on the FacesContext, so viewId 
                // is null.

                // In this case, it should try to derive the viewId of the view that was
                // not able to restore, to get the extension and apply it to
                // the implicit navigation.
                String tempViewId = getViewIdSupport().calculateViewId(facesContext);
                if (tempViewId != null)
                {
                    index = tempViewId.lastIndexOf('.');
                    if(index != -1)
                    {
                        viewIdToTest.append(tempViewId.substring(index));
                    }
                }
            }
            if (log.isLoggable(Level.FINEST))
            {
                log.finest("getOutcomeNavigationCase -> viewIdToTest: " + viewIdToTest);
            } 
        }

        // If viewIdToTest does not start with "/", look for the last "/" in viewId.  If not found, simply prepend "/".
        // Otherwise, prepend everything before and including the last "/" in viewId.
        
        boolean startWithSlash = false;
        if (viewIdToTest.length() > 0)
        {
            startWithSlash = viewIdToTest.charAt(0) == '/';
        } 
        if (!startWithSlash) 
        {
            index = -1;
            if (viewId != null)
            {
               index = viewId.lastIndexOf('/');
            }
            
            if (index == -1)
            {
                viewIdToTest.insert(0, '/');
            }
            else
            {
                viewIdToTest.insert(0, viewId, 0, index + 1);
            }
        }
        
        // Apply normalization 
        String viewIdToTestString = null;
        boolean applyNormalization = false;
        for (int i = 0; i < viewIdToTest.length() - 1; i++)
        {
            if (viewIdToTest.charAt(i) == '.' &&
                viewIdToTest.charAt(i+1) == '/')
            {
                applyNormalization = true; 
                break;
            }
        }
        if (applyNormalization)
        {
            viewIdToTestString = FilenameUtils.normalize(viewIdToTest.toString(), true);
        }
        else
        {
            viewIdToTestString = viewIdToTest.toString();
        }
        
        // Call ViewHandler.deriveViewId() and set the result as implicitViewId.
        implicitViewId = facesContext.getApplication().getViewHandler().deriveViewId(facesContext, viewIdToTestString);
        if (implicitViewId != null)
        {
            // Append all params from the queryString
            // (excluding faces-redirect, includeViewParams and faces-include-view-params)
            Map<String, List<String>> params = null;
            if (StringUtils.isNotBlank(queryString))
            {
                String[] splitQueryParams = AMP_PATTERN.split(queryString); // "&" or "&amp;"
                params = new HashMap<>(splitQueryParams.length, 1f);
                for (String queryParam : splitQueryParams)
                {
                    String[] splitParam = StringUtils.splitShortString(queryParam, '=');
                    if (splitParam.length == 2)
                    {
                        // valid parameter - add it to params
                        if ("includeViewParams".equals(splitParam[0])
                                || "faces-include-view-params".equals(splitParam[0])
                                || "faces-redirect".equals(splitParam[0]))
                        {
                            // ignore includeViewParams, faces-include-view-params and faces-redirect
                            continue;
                        }
                        List<String> paramValues = params.get(splitParam[0]);
                        if (paramValues == null)
                        {
                            paramValues = new ArrayList<>(5);
                            params.put(splitParam[0], paramValues);
                        }
                        paramValues.add(splitParam[1]);
                    }
                    else
                    {
                        // invalid parameter
                        throw new FacesException("Invalid parameter \"" + queryParam + "\" in outcome " + outcome);
                    }
                }
            }
            
            // Finally, create the NavigationCase.
            result = new NavigationCase(viewId, fromAction, outcome, null, implicitViewId, params, isRedirect,
                    includeViewParams);
        }

        return result;
    }
    
    /**
     * Returns the view ID that would be created for the given action and outcome
     */
    public String getViewId(FacesContext context, String fromAction, String outcome)
    {
        return this.getNavigationCase(context, fromAction, outcome).getToViewId(context);
    }

    private NavigationCase calcMatchingNavigationCase(FacesContext context,
                                                      Set<? extends NavigationCase> casesList,
                                                      String actionRef,
                                                      String outcome)
    {
        NavigationCase noConditionCase = null;
        NavigationCase firstCase = null;
        NavigationCase firstCaseIf = null;
        NavigationCase secondCase = null;
        NavigationCase secondCaseIf = null;
        NavigationCase thirdCase = null;
        NavigationCase thirdCaseIf = null;
        NavigationCase fourthCase = null;
        NavigationCase fourthCaseIf = null;
                        
        for (NavigationCase caze : casesList)
        {
            String cazeOutcome = caze.getFromOutcome();
            String cazeActionRef = caze.getFromAction();
            Boolean cazeIf = caze.getCondition(context);
            boolean ifMatches = cazeIf == null ? false : cazeIf;
            // Faces 2.0: support conditional navigation via <if>.
            // Use for later cases.
            
            if(outcome == null && (cazeOutcome != null || cazeIf == null) && actionRef == null)
            {
                //To match an outcome value of null, the <from-outcome> must be absent and the <if> element present.
                continue;
            }
            
            //If there are no conditions on navigation case save it and return as last resort
            if (cazeOutcome == null && cazeActionRef == null &&
                cazeIf == null && noConditionCase == null && outcome != null)
            {
                noConditionCase = caze;
            }
            
            if (cazeActionRef != null)
            {
                if (cazeOutcome != null)
                {
                    if (actionRef != null && outcome != null
                            && cazeActionRef.equals(actionRef) && cazeOutcome.equals(outcome))
                    {
                        // First case: match if <from-action> matches action and <from-outcome> matches outcome.
                        // Caveat: evaluate <if> if available.

                        if (cazeIf != null)
                        {
                            if (ifMatches)
                            {
                                firstCaseIf = caze;
                                //return caze;
                            }

                            continue;
                        }
                        else
                        {
                            firstCase = caze;
                        }
                    }
                }
                else
                {
                    if ((actionRef != null) && cazeActionRef.equals (actionRef))
                    {
                        // Third case: if only <from-action> specified, match against action.
                        // Caveat: if <if> is available, evaluate.  If not, only match if outcome is not null.
                        if (cazeIf != null)
                        {
                            if (ifMatches)
                            {
                                thirdCaseIf = caze;
                            }
                            
                            continue;
                        }
                        else
                        {
                            if (outcome != null)
                            {
                                thirdCase = caze;
                            }
                            
                            continue;
                        }
                    }
                    else
                    {
                        // cazeActionRef != null and cazeOutcome == null
                        // but cazeActionRef does not match. No additional operation
                        // required because cazeIf is only taken into account 
                        // it cazeActionRef match. 
                        continue;
                    }
                }
            }
            else
            {
                if (cazeOutcome != null && (outcome != null) && cazeOutcome.equals (outcome))
                {
                    // Second case: if only <from-outcome> specified, match against outcome.
                    // Caveat: if <if> is available, evaluate.

                    if (cazeIf != null)
                    {
                        if (ifMatches)
                        {
                            secondCaseIf = caze;
                        }

                        continue;
                    }
                    else
                    {
                        secondCase = caze;
                    }
                }
            }

            // Fourth case: anything else matches if outcome is not null or <if> is specified.
            if (outcome != null && cazeIf != null)
            {
                // Again, if <if> present, evaluate.
                if (ifMatches)
                {
                    fourthCaseIf = caze;
                }

                continue;
            }

            if ((cazeIf != null) && ifMatches)
            {
                fourthCase = caze;
            }
        }
        
        if (firstCaseIf != null)
        {
            return firstCaseIf;
        }
        else if (firstCase != null)
        {
            return firstCase;
        }
        else if (secondCaseIf != null)
        {
            return secondCaseIf;
        }
        else if (secondCase != null)
        {
            return secondCase;
        }
        else if (thirdCaseIf != null)
        {
            return thirdCaseIf;
        }
        else if (thirdCase != null)
        {
            return thirdCase;
        }
        else if (fourthCaseIf != null)
        {
            return fourthCaseIf;
        }
        else if (fourthCase != null)
        {
            return fourthCase;
        }
        
        return noConditionCase;
    }

    private List<_WildcardPattern> getSortedWildcardPatterns()
    {
        return _wildcardPatterns;
    }

    @Override
    public Map<String, Set<NavigationCase>> getNavigationCases()
    {
        if (_developmentStage == null)
        {
            _developmentStage = FacesContext.getCurrentInstance().isProjectStage(ProjectStage.Development);
        }
        if (!Boolean.TRUE.equals(_developmentStage))
        {
            if (_navigationCases == null)
            {
                FacesContext facesContext = FacesContext.getCurrentInstance();
                ExternalContext externalContext = facesContext.getExternalContext();
                RuntimeConfig runtimeConfig = RuntimeConfig.getCurrentInstance(externalContext);
                
                calculateNavigationCases(runtimeConfig);
            }
            return _navigationCases;
        }
        else
        {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();
            RuntimeConfig runtimeConfig = RuntimeConfig.getCurrentInstance(externalContext);

            if (_navigationCases == null || runtimeConfig.isNavigationRulesChanged())
            {
                calculateNavigationCases(runtimeConfig);
            }
            return _navigationCases;
        }
    }

    @Override
    public void inspectFlow(FacesContext context, Flow flow)
    {
        Map<String, Set<NavigationCase>> rules = flow.getNavigationCases();
        int rulesSize = rules.size();

        Map<String, Set<NavigationCase>> cases = new HashMap<>(HashMapUtils.calcCapacity(rulesSize));

        List<_WildcardPattern> wildcardPatterns = new ArrayList<>();

        for (Map.Entry<String, Set<NavigationCase>> entry : rules.entrySet())
        {
            String fromViewId = entry.getKey();

            //specification 7.4.2 footnote 4 - missing fromViewId is allowed:
            if (fromViewId == null)
            {
                fromViewId = ASTERISK;
            }
            else
            {
                fromViewId = fromViewId.trim();
            }

            Set<NavigationCase> set = cases.get(fromViewId);
            if (set == null)
            {
                set = new HashSet<>(entry.getValue());
                cases.put(fromViewId, set);
                if (fromViewId.endsWith(ASTERISK))
                {
                    wildcardPatterns.add(new _WildcardPattern(fromViewId));
                }
            }
            else
            {
                set.addAll(entry.getValue());
            }
        }

        Collections.sort(wildcardPatterns, KeyComparator.INSTANCE);

        _flowNavigationStructureMap.put(
            flow.getId(), 
            new _FlowNavigationStructure(flow.getDefiningDocumentId(), flow.getId(), cases, wildcardPatterns) );
    }
    
    private synchronized void calculateNavigationCases(RuntimeConfig runtimeConfig)
    {
        if (_navigationCases == null || runtimeConfig.isNavigationRulesChanged())
        {
            Collection<? extends NavigationRule> rules = runtimeConfig.getNavigationRules();
            int rulesSize = rules.size();

            Map<String, Set<NavigationCase>> cases = new HashMap<>(HashMapUtils.calcCapacity(rulesSize));

            List<_WildcardPattern> wildcardPatterns = new ArrayList<>();
            for (NavigationRule rule : rules)
            {
                String fromViewId = rule.getFromViewId();

                //specification 7.4.2 footnote 4 - missing fromViewId is allowed:
                if (fromViewId == null)
                {
                    fromViewId = ASTERISK;
                }
                else
                {
                    fromViewId = fromViewId.trim();
                }

                Set<NavigationCase> set = cases.get(fromViewId);
                if (set == null)
                {
                    set = new HashSet<>(convertNavigationCasesToAPI(rule));
                    cases.put(fromViewId, set);
                    if (fromViewId.endsWith(ASTERISK))
                    {
                        wildcardPatterns.add(new _WildcardPattern(fromViewId));
                    }
                }
                else
                {
                    set.addAll(convertNavigationCasesToAPI(rule));
                }
            }

            Collections.sort(wildcardPatterns, KeyComparator.INSTANCE);

            synchronized (cases)
            {
                // We do not really need this sychronization at all, but this
                // gives us the peace of mind that some good optimizing compiler
                // will not rearrange the execution of the assignment to an
                // earlier time, before all init code completes
                _navigationCases = cases;
                _wildcardPatterns = wildcardPatterns;

                runtimeConfig.setNavigationRulesChanged(false);
            }
        }
    }

    private static final class KeyComparator implements Comparator<_WildcardPattern>
    {
        private static final KeyComparator INSTANCE = new KeyComparator();
        
        private KeyComparator()
        {
        }

        @Override
        public int compare(_WildcardPattern s1, _WildcardPattern s2)
        {
            return -s1.getPattern().compareTo(s2.getPattern());
        }
    }
    
    private Set<NavigationCase> convertNavigationCasesToAPI(NavigationRule rule)
    {
        Collection<? extends org.apache.myfaces.config.element.NavigationCase> configCases = rule.getNavigationCases();
        Set<NavigationCase> apiCases = new HashSet<>(configCases.size());

        for(org.apache.myfaces.config.element.NavigationCase configCase : configCases)
        {   
            if (configCase.getRedirect() != null)
            {
                String includeViewParamsAttribute = configCase.getRedirect().getIncludeViewParams();
                boolean includeViewParams = false; // default value is false
                if (includeViewParamsAttribute != null)
                {
                    includeViewParams = Boolean.valueOf(includeViewParamsAttribute);
                }
                apiCases.add(new NavigationCase(rule.getFromViewId(),configCase.getFromAction(),
                                                configCase.getFromOutcome(),configCase.getIf(),configCase.getToViewId(),
                                                configCase.getRedirect().getViewParams(),true,includeViewParams));
            }
            else
            {
                apiCases.add(new NavigationCase(rule.getFromViewId(),configCase.getFromAction(),
                                                configCase.getFromOutcome(),configCase.getIf(),
                                                configCase.getToViewId(),null,false,false));
            }
        }
        
        return apiCases;
    }
    
    /**
     * A navigation command is an operation to do by the navigation handler like
     * do a redirect, execute a normal navigation or enter or exit a flow. 
     * 
     * To resolve a navigation command, it is necessary to get an snapshot of the
     * current "navigation context" and try to resolve the command.
     */
    protected static class NavigationContext
    {
        private NavigationCase navigationCase;
        private List<Flow> sourceFlows;
        private List<Flow> targetFlows;
        private List<FlowCallNode> targetFlowCallNodes;
        private List<Flow> currentFlows;
        private int returnCount = 0;

        public NavigationContext()
        {
        }

        public NavigationContext(NavigationCase navigationCase)
        {
            this.navigationCase = navigationCase;
        }
        
        public NavigationCase getNavigationCase()
        {
            return navigationCase;
        }

        public void setNavigationCase(NavigationCase navigationCase)
        {
            this.navigationCase = navigationCase;
        }

        public List<Flow> getSourceFlows()
        {
            return sourceFlows;
        }

        public List<Flow> getTargetFlows()
        {
            return targetFlows;
        }
        
        public List<FlowCallNode> getFlowCallNodes()
        {
            return targetFlowCallNodes;
        }

        public void addTargetFlow(Flow sourceFlow, Flow targetFlow, FlowCallNode flowCallNode)
        {
            if (targetFlows == null)
            {
                sourceFlows = new ArrayList<>(4);
                targetFlows = new ArrayList<>(4);
                targetFlowCallNodes = new ArrayList<>(4);
            }
            this.sourceFlows.add(sourceFlow);
            this.targetFlows.add(targetFlow);
            this.targetFlowCallNodes.add(flowCallNode);
        }
        
        public Flow getCurrentFlow(FacesContext facesContext)
        {
            if (currentFlows != null && !currentFlows.isEmpty())
            {
                return currentFlows.get(currentFlows.size()-1);
            }
            else
            {
                FlowHandler flowHandler = facesContext.getApplication().getFlowHandler();
                return flowHandler.getCurrentFlow(facesContext);
            }
        }
        
        public void finish(FacesContext facesContext)
        {
            // Get back flowHandler to its original state
            for (int i=0; i < returnCount; i++)
            {
                FlowHandler flowHandler = facesContext.getApplication().getFlowHandler();
                flowHandler.popReturnMode(facesContext);
            }
            returnCount = 0;
        }
        
        public void popFlow(FacesContext facesContext)
        {
            if (currentFlows != null && !currentFlows.isEmpty())
            {
                currentFlows.remove(currentFlows.size()-1);
            }
            else
            {
                FlowHandler flowHandler = facesContext.getApplication().getFlowHandler();
                flowHandler.pushReturnMode(facesContext);
                returnCount++;
            }
        }
        
        public void pushFlow(FacesContext facesContext, Flow flow)
        {
            if (currentFlows == null)
            {
                currentFlows = new ArrayList<>();
            }
            currentFlows.add(flow);
        }
        
        public String getLastDisplayedViewId(FacesContext facesContext, Flow flow)
        {
            FlowHandler flowHandler = facesContext.getApplication().getFlowHandler();
            return flowHandler.getLastDisplayedViewId(facesContext);
        }
    }
}
