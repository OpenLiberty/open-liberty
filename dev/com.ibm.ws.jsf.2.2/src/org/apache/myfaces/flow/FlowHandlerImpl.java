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
//  PI59422     hwibell      Flow beans are destroyed before flow is finalized
package org.apache.myfaces.flow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.faces.FacesWrapper;
import javax.faces.application.ConfigurableNavigationHandler;
import javax.faces.application.NavigationCase;
import javax.faces.application.NavigationHandler;
import javax.faces.application.NavigationHandlerWrapper;
import javax.faces.context.FacesContext;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.faces.flow.Flow;
import javax.faces.flow.FlowCallNode;
import javax.faces.flow.FlowHandler;
import javax.faces.flow.FlowNode;
import javax.faces.flow.Parameter;
import javax.faces.flow.ReturnNode;
import javax.faces.lifecycle.ClientWindow;
import org.apache.myfaces.event.PostClientWindowAndViewInitializedEvent;
import org.apache.myfaces.spi.FacesFlowProvider;
import org.apache.myfaces.spi.FacesFlowProviderFactory;

/**
 *
 * @since 2.2
 * @author Leonardo Uribe
 */
public class FlowHandlerImpl extends FlowHandler implements SystemEventListener
{
    private final static String CURRENT_FLOW_STACK = "oam.flow.STACK.";
    private final static String ROOT_LAST_VIEW_ID = "oam.flow.ROOT_LAST_VIEW_ID.";
    
    private final static String RETURN_MODE = "oam.flow.RETURN_MODE";
    private final static String FLOW_RETURN_STACK = "oam.flow.RETURN_STACK.";
    private final static String CURRENT_FLOW_REQUEST_STACK = "oam.flow.REQUEST_STACK.";
    
    private Map<String, Map<String, Flow>> _flowMapByDocumentId;
    private Map<String, Flow> _flowMapById;
    
    private FacesFlowProvider _facesFlowProvider;
    
    public FlowHandlerImpl()
    {
        _flowMapByDocumentId = new ConcurrentHashMap<String, Map<String, Flow>>();
        _flowMapById = new ConcurrentHashMap<String, Flow>();
    }

    @Override
    public Flow getFlow(FacesContext context, String definingDocumentId, String id)
    {
        checkNull(context, "context");
        checkNull(definingDocumentId, "definingDocumentId");
        checkNull(id, "id");
        
        // First try the combination.
        Map<String, Flow> flowMap = _flowMapByDocumentId.get(definingDocumentId);
        if (flowMap != null)
        {
            Flow flow = flowMap.get(id);
            if (flow != null)
            {
                return flow;
            }
        }
        
        //if definingDocumentId is an empty string, 
        if ("".equals(definingDocumentId))
        {
            return _flowMapById.get(id);
        }
        return null;
    }

    @Override
    public void addFlow(FacesContext context, Flow toAdd)
    {
        checkNull(context, "context");
        checkNull(toAdd, "toAdd");
        
        String id = toAdd.getId();
        String definingDocumentId = toAdd.getDefiningDocumentId();
        
        if (id == null)
        {
            throw new IllegalArgumentException("Flow must have a non null id");
        }
        else if (id.length() == 0)
        {
            throw new IllegalArgumentException("Flow must have a non empty id");
        }
        if (definingDocumentId == null)
        {
            throw new IllegalArgumentException("Flow must have a non null definingDocumentId");
        }
        
        Map<String, Flow> flowMap = _flowMapByDocumentId.get(definingDocumentId);
        if (flowMap == null)
        {
            flowMap = new ConcurrentHashMap<String, Flow>();
            _flowMapByDocumentId.put(definingDocumentId, flowMap);
        }
        flowMap.put(id, toAdd);
        
        Flow duplicateFlow = _flowMapById.get(id);
        if (duplicateFlow != null)
        {
            // There are two flows with the same flowId.
            // Give priority to the flow with no defining document id
            if ("".equals(toAdd.getDefiningDocumentId()))
            {
                _flowMapById.put(id, toAdd);
            }
            else if ("".equals(duplicateFlow.getDefiningDocumentId()))
            {
                // Already added, skip
            }
            else
            {
                // Put the last one
                _flowMapById.put(id, toAdd);
            }
        }
        else
        {
            _flowMapById.put(id, toAdd);
        }

        // Once the flow is added to the map, it is still necessary to 
        // pass the flow to the ConfigurableNavigationHandler, so it can be
        // inspected for navigation rules. This is the best place to do that because
        // the spec says "... Called by the flow system to cause the flow to 
        // be inspected for navigation rules... " (note it says "flow system" not
        // "configuration system" where the calls to addFlow() are done).
        invokeInspectFlow(context, context.getApplication().getNavigationHandler(), toAdd);
    }

    @Override
    public Flow getCurrentFlow(FacesContext context)
    {
        Object session = context.getExternalContext().getSession(false);
        if (session == null)
        {
            return null;
        }
        ClientWindow clientWindow = context.getExternalContext().getClientWindow();
        if (clientWindow == null)
        {
            return null;
        }
        
        
        _FlowContextualInfo info = getCurrentFlowReference(context, clientWindow);
        if (info == null)
        {
            return null;
        }
        FlowReference flowReference = info.getFlowReference();
        return getFlow(context, flowReference.getDocumentId(), flowReference.getId());
    }
    
    @Override
    public void transition(FacesContext context, Flow sourceFlow, Flow targetFlow, 
        FlowCallNode outboundCallNode, String toViewId)
    {
        checkNull(context, "context");
        checkNull(toViewId, "toViewId");
        ClientWindow clientWindow = context.getExternalContext().getClientWindow();
        boolean outboundCallNodeProcessed = false;
        if (clientWindow == null)
        {
            return;
        }
        
        if (sourceFlow == null && targetFlow == null)
        {
            return;
        }

        // Calculate the parentFlowReference, since it will be used later.
        FlowReference parentFlowReference = (outboundCallNode != null && sourceFlow != null) ?
            new FlowReference(sourceFlow.getDefiningDocumentId(), sourceFlow.getId()) : null;
        
        if (sourceFlow == null)
        {
            // Entering a flow
            Map<String, Object> outboundParameters = doBeforeEnterFlow(context, 
                targetFlow, !outboundCallNodeProcessed ? outboundCallNode : null);
            outboundCallNodeProcessed = true;
            pushFlowReference(context, clientWindow, 
                    new FlowReference(targetFlow.getDefiningDocumentId(), targetFlow.getId()), 
                    toViewId, parentFlowReference);
            doAfterEnterFlow(context, targetFlow, outboundParameters);
        }
        else if (targetFlow == null)
        {
            // Getting out of the flow, since targetFlow is null,
            // we need to take sourceFlow and take it out and all the chain
            List<_FlowContextualInfo> currentFlowStack = getCurrentFlowStack(context, clientWindow);
            if (currentFlowStack != null)
            {
                removeFlowFromStack(context, currentFlowStack, sourceFlow);
            }
        }
        else
        {
            // Both sourceFlow and targetFlow are not null, so we need to check the direction
            // If targetFlow is on the stack, remove elements until get there.
            // If targetFlow is not there, add it to the stack.
            List<_FlowContextualInfo> currentFlowStack = getCurrentFlowStack(context, clientWindow);
            if (currentFlowStack != null)
            {
                FlowReference targetFlowReference = new FlowReference(
                        targetFlow.getDefiningDocumentId(), targetFlow.getId());
                int targetFlowIndex = -1;
                for (int j = currentFlowStack.size()-1; j >= 0; j--)
                {
                    if (targetFlowReference.equals(currentFlowStack.get(j).getFlowReference()))
                    {
                        targetFlowIndex = j;
                        break;
                    }
                }
                if (targetFlowIndex >= 0)
                {
                    // targetFlow is on the stack, so it is a return.
                    removeFlowFromStack(context, currentFlowStack, sourceFlow);
                }
                else
                {
                    // targetFlow is not on the stack, so it is flow call.
                    Map<String, Object> outboundParameters = doBeforeEnterFlow(context,
                        targetFlow, !outboundCallNodeProcessed ? outboundCallNode : null);
                    outboundCallNodeProcessed = true;
                    pushFlowReference(context, clientWindow, 
                            new FlowReference(targetFlow.getDefiningDocumentId(), targetFlow.getId()), toViewId,
                            parentFlowReference);
                    doAfterEnterFlow(context, targetFlow, outboundParameters);
                }
            }
            else
            {
                // sourceFlow and targetFlow are not null, but there is no currentFlowStack. It that
                // case just enter into targetFlow
                Map<String, Object> outboundParameters = doBeforeEnterFlow(context, 
                    targetFlow, !outboundCallNodeProcessed ? outboundCallNode : null);
                outboundCallNodeProcessed = true;
                pushFlowReference(context, clientWindow, 
                        new FlowReference(targetFlow.getDefiningDocumentId(), targetFlow.getId()), toViewId,
                        parentFlowReference);
                doAfterEnterFlow(context, targetFlow, outboundParameters);
            }
        }
    }
    
    private void removeFlowFromStack(FacesContext context, List<_FlowContextualInfo> currentFlowStack, Flow sourceFlow)
    {
        // Steps to remove a flow:
        // 1. locate where is the flow in the chain
        int sourceFlowIndex = -1;
        FlowReference sourceFlowReference = new FlowReference(sourceFlow.getDefiningDocumentId(),
            sourceFlow.getId());
        List<_FlowContextualInfo> flowsToRemove = new ArrayList<_FlowContextualInfo>();
        for (int i = currentFlowStack.size()-1; i >= 0; i--)
        {
            _FlowContextualInfo fci = currentFlowStack.get(i);
            if (fci.getFlowReference().equals(sourceFlowReference))
            {
                sourceFlowIndex = i;
                flowsToRemove.add(fci);
                break;
            }
        }

        if (sourceFlowIndex != -1)
        {
            // From sourceFlowIndex, add all flows 
            traverseDependantFlows(sourceFlowReference, sourceFlowIndex+1, currentFlowStack, flowsToRemove);

            // Remove all marked elements
            if (!flowsToRemove.isEmpty())
            {
                for (int i = flowsToRemove.size()-1; i >= 0; i--)
                {
                    _FlowContextualInfo fci = flowsToRemove.get(i);
                    FlowReference fr = fci.getFlowReference();
                    doBeforeExitFlow(context, getFlow(context, fr.getDocumentId(), fr.getId()));
                    //popFlowReference(context, clientWindow, currentFlowStack, i);
                    currentFlowStack.remove(fci);
                }
            }

            if (currentFlowStack.isEmpty())
            {
                // Remove it from session but keep it in request scope.
                context.getAttributes().put(ROOT_LAST_VIEW_ID, 
                    context.getExternalContext().getSessionMap().remove(ROOT_LAST_VIEW_ID + 
                    context.getExternalContext().getClientWindow().getId()));
            }
        }
    }
    
    private void traverseDependantFlows(FlowReference sourceFlowReference, 
        int index, List<_FlowContextualInfo> currentFlowStack, List<_FlowContextualInfo> flowsToRemove)
    {
        if (index < currentFlowStack.size())
        {
            for (int i = index; i < currentFlowStack.size(); i++)
            {
                _FlowContextualInfo info = currentFlowStack.get(i);
                if (sourceFlowReference.equals(info.getSourceFlowReference()) &&
                    !flowsToRemove.contains(info))
                {
                    flowsToRemove.add(info);
                    traverseDependantFlows(info.getFlowReference(), i+1, currentFlowStack, flowsToRemove);
                }
            }
        }
    }
    
    private Map<String, Object> doBeforeEnterFlow(FacesContext context, Flow flow, FlowCallNode outboundCallNode)
    {
        Map<String, Object> outboundParameters = null;
        if (outboundCallNode != null && !outboundCallNode.getOutboundParameters().isEmpty())
        {
            outboundParameters = new HashMap<String, Object>();
            for (Map.Entry<String, Parameter> entry : outboundCallNode.getOutboundParameters().entrySet())
            {
                Parameter parameter = entry.getValue();
                if (parameter.getValue() != null)
                {
                    outboundParameters.put(entry.getKey(), parameter.getValue().getValue(context.getELContext()));
                }
            }
        }
        return outboundParameters;
    }
    
    private void doAfterEnterFlow(FacesContext context, Flow flow, Map<String, Object> outboundParameters)
    {
        getFacesFlowProvider(context).doAfterEnterFlow(context, flow);
        
        if (outboundParameters != null)
        {
            for (Map.Entry<String, Parameter> entry : flow.getInboundParameters().entrySet())
            {
                Parameter parameter = entry.getValue();
                if (parameter.getValue() != null && outboundParameters.containsKey(entry.getKey()))
                {
                    parameter.getValue().setValue(context.getELContext(), outboundParameters.get(entry.getKey()));
                }
            }
        }

        if (flow.getInitializer() != null)
        {
            flow.getInitializer().invoke(context.getELContext(), null);
        }
    }
    
    public FacesFlowProvider getFacesFlowProvider(FacesContext facesContext)
    {
        if (_facesFlowProvider == null)
        {
            FacesFlowProviderFactory factory = 
                FacesFlowProviderFactory.getFacesFlowProviderFactory(
                    facesContext.getExternalContext());
            _facesFlowProvider = factory.getFacesFlowProvider(
                    facesContext.getExternalContext());
            
            facesContext.getApplication().unsubscribeFromEvent(PostClientWindowAndViewInitializedEvent.class, this);
            facesContext.getApplication().subscribeToEvent(PostClientWindowAndViewInitializedEvent.class, this);
        }
        return _facesFlowProvider;
    }
    
    private void doBeforeExitFlow(FacesContext context, Flow flow)
    {
        if (flow.getFinalizer() != null)
        {
            flow.getFinalizer().invoke(context.getELContext(), null);
        }

        getFacesFlowProvider(context).doBeforeExitFlow(context, flow); // PI59422
    }

    @Override
    public boolean isActive(FacesContext context, String definingDocumentId, String id)
    {
        checkNull(context, "context");
        checkNull(definingDocumentId, "definingDocumentId");
        checkNull(id, "id");
        
        Object session = context.getExternalContext().getSession(false);
        if (session == null)
        {
            return false;
        }
        ClientWindow clientWindow = context.getExternalContext().getClientWindow();
        if (clientWindow == null)
        {
            return false;
        }
        Map<String, Object> sessionMap = context.getExternalContext().getSessionMap();
        String currentFlowMapKey = CURRENT_FLOW_STACK + clientWindow.getId();

        List<_FlowContextualInfo> currentFlowStack = (List<_FlowContextualInfo>) sessionMap.get(currentFlowMapKey);
        if (currentFlowStack == null)
        {
            return false;
        }
        FlowReference reference = new FlowReference(definingDocumentId, id);
        
        for (_FlowContextualInfo info : currentFlowStack)
        {
            if (reference.equals(info.getFlowReference()))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public Map<Object, Object> getCurrentFlowScope()
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        return getFacesFlowProvider(facesContext).getCurrentFlowScope(facesContext);
    }

    /**
     * The interpretation done for this issue is this:
     * 
     * There are two basic cases: Enter into a flow and return from a flow.
     * 
     * - FlowHandler.TO_FLOW_DOCUMENT_ID_REQUEST_PARAM_NAME : value of the toFlowDocumentId property 
     *   of the navigation case when enter into a flow OR FlowHandler.NULL_FLOW when return from a flow.
     * 
     * - FlowHandler.FLOW_ID_REQUEST_PARAM_NAME : value of the fromOutcome property of the navigation case.
     * According to the intention it has multiple options:
     * 
     *  1. It can be a flowId, which means enter into a flow.
     *  2. It can be a flow call id, which means enter into a flow.
     *  3. It can be a flow return id, which means return from a flow.

     * - The javadoc of NavigationCase.getToFlowDocumentId() says this:
     * "... If this navigation case represents a flow invocation, this property is the documentId in 
     * which the flow whose id is given by the return from getFromOutcome() is defined. Implementations 
     * must override this method to return the value defined in the corresponding application 
     * configuration resources element. The base implementation returns the empty string. ..."
     * 
     * This is consistent with the previous interpretation, but we need to include the case where 
     * toFlowDocumentId is FlowHandler.NULL_FLOW too, which is derived implicitly. The key of the trick 
     * is override fromOutcome / toFlowDocumentId in the navigation algorithm to indicate when the 
     * navigation case is entering into a flow or return from a flow. In that way, it is possible 
     * to use ConfigurableNavigationHandler.getNavigationCase(...) to know the "route" using the 
     * initial fromOutcome given in FLOW_ID_REQUEST_PARAM_NAME.
     * 
     * @param context 
     */
    @Override
    public void clientWindowTransition(FacesContext context)
    {
        String flowDocumentIdRequestParam = (String) context.getExternalContext().
            getRequestParameterMap().get(FlowHandler.TO_FLOW_DOCUMENT_ID_REQUEST_PARAM_NAME);
        
        if (flowDocumentIdRequestParam != null)
        {
            String flowIdRequestParam = (String) context.getExternalContext().
                getRequestParameterMap().get(FlowHandler.FLOW_ID_REQUEST_PARAM_NAME);
            
            if (flowIdRequestParam == null)
            {
                // If we don't have an fromOutcome, it is not possible to calculate the transitions
                // involved.
                return;
            }
            
            FlowHandler flowHandler = context.getApplication().getFlowHandler();
            ConfigurableNavigationHandler nh = 
                (ConfigurableNavigationHandler) context.getApplication().getNavigationHandler();
            
            if (FlowHandler.NULL_FLOW.equals(flowDocumentIdRequestParam))
            {
                // It is a return node. The trick here is we need to calculate
                // where the flow should return, because that information was not passed
                // in the parameters of the link. 
                String toFlowDocumentId = FlowHandler.NULL_FLOW;
                String fromOutcome = flowIdRequestParam;
                //Flow sourceFlow = null;
                List<Flow> sourceFlows = null;
                List<Flow> targetFlows = null;
                
                boolean failed = false;
                int i = 0;
                while (FlowHandler.NULL_FLOW.equals(toFlowDocumentId) && !failed)
                {
                    Flow currentFlow = flowHandler.getCurrentFlow(context);
                    if (currentFlow == null)
                    {
                        failed = true;
                        break;
                    }
                    String currentLastDisplayedViewId = flowHandler.getLastDisplayedViewId(context);
                    FlowNode node = currentFlow.getNode(fromOutcome);
                    if (node instanceof ReturnNode)
                    {
                        if (targetFlows == null)
                        {
                            sourceFlows = new ArrayList<Flow>(4);
                            targetFlows = new ArrayList<Flow>(4);
                        }
                        // Get the navigation case using the outcome
                        Flow sourceFlow = currentFlow;
                        flowHandler.pushReturnMode(context);
                        currentFlow = flowHandler.getCurrentFlow(context);
                        i++;
                        
                        NavigationCase navCase = nh.getNavigationCase(context, null, 
                            ((ReturnNode) node).getFromOutcome(context), FlowHandler.NULL_FLOW);

                        if (navCase == null)
                        {
                            if (currentLastDisplayedViewId != null)
                            {
                                sourceFlows.add(sourceFlow);
                                if (currentFlow != null)
                                {
                                    toFlowDocumentId = currentFlow.getDefiningDocumentId();
                                    targetFlows.add(currentFlow);
                                }
                                else
                                {
                                    // No active flow
                                    toFlowDocumentId = null;
                                    targetFlows.add(null);
                                }
                            }
                            else
                            {
                                // Invalid state because no navCase and 
                                // no saved lastDisplayedViewId into session
                                failed = true;
                            }
                        }
                        else
                        {
                            if (FlowHandler.NULL_FLOW.equals(navCase.getToFlowDocumentId()))
                            {
                                fromOutcome = navCase.getFromOutcome();
                            }
                            else
                            {
                                sourceFlows.add(sourceFlow);
                                // The absence of FlowHandler.NULL_FLOW means the return went somewhere else.
                                if (currentFlow != null)
                                {
                                    toFlowDocumentId = currentFlow.getDefiningDocumentId();
                                    targetFlows.add(currentFlow);
                                }
                                else
                                {
                                    // No active flow
                                    toFlowDocumentId = null;
                                    targetFlows.add(null);
                                }
                            }
                        }
                    }
                    else
                    {
                        // No return node found in current flow, push it and check 
                        // the next flow
                        flowHandler.pushReturnMode(context);
                        currentFlow = flowHandler.getCurrentFlow(context);
                        i++;
                        if (currentFlow == null)
                        {
                            failed = true;
                        }
                    }
                }
                for (int j = 0; j<i; j++)
                {
                    flowHandler.popReturnMode(context);
                }
                if (!failed)
                {
                    //Call transitions.
                    for (int j = 0; j < targetFlows.size(); j++)
                    {
                        Flow sourceFlow = sourceFlows.get(j);
                        Flow targetFlow = targetFlows.get(j);
                        flowHandler.transition(context, 
                            sourceFlow,
                            targetFlow, null, context.getViewRoot().getViewId());
                        
                    }
                }
            }
            else
            {
                // This transition is for start a new flow. In this case 
                // FlowHandler.FLOW_ID_REQUEST_PARAM_NAME could be the flow name to enter
                // or the flow call node to activate.
                // 1. check if is a flow
                Flow targetFlow = flowHandler.getFlow(context, flowDocumentIdRequestParam, flowIdRequestParam);
                Flow currentFlow = null;
                FlowCallNode outboundCallNode = null;
                FlowNode node = null;
                if (targetFlow == null)
                {
                    //Check if is a call flow node
                    List<Flow> activeFlows = FlowHandlerImpl.getActiveFlows(context, flowHandler);
                    for (Flow activeFlow : activeFlows)
                    {
                        node = activeFlow != null ? activeFlow.getNode(flowIdRequestParam) : null;
                        if (node != null && node instanceof FlowCallNode)
                        {
                            outboundCallNode = (FlowCallNode) node;

                            String calledFlowDocumentId = outboundCallNode.getCalledFlowDocumentId(context);
                            if (calledFlowDocumentId == null)
                            {
                                calledFlowDocumentId = activeFlow.getDefiningDocumentId();
                            }
                            targetFlow = flowHandler.getFlow(context, 
                                calledFlowDocumentId, 
                                outboundCallNode.getCalledFlowId(context));
                            if (targetFlow == null && !"".equals(calledFlowDocumentId))
                            {
                                targetFlow = flowHandler.getFlow(context, "", 
                                    outboundCallNode.getCalledFlowId(context));
                            }
                            if (targetFlow != null)
                            {
                                currentFlow = activeFlow;
                                break;
                            }
                        }
                    }
                }
                
                if (targetFlow != null)
                {
                    if (flowHandler.isActive(context, targetFlow.getDefiningDocumentId(), targetFlow.getId()))
                    {
                        Flow baseReturnFlow = flowHandler.getCurrentFlow();
                        if (!(baseReturnFlow.getDefiningDocumentId().equals(targetFlow.getDefiningDocumentId()) &&
                             baseReturnFlow.getId().equals(targetFlow.getId())))
                        {
                            flowHandler.transition(context, 
                                baseReturnFlow, targetFlow, outboundCallNode, context.getViewRoot().getViewId());
                        }
                        flowHandler.pushReturnMode(context);
                        Flow previousFlow = flowHandler.getCurrentFlow(context);
                        flowHandler.popReturnMode(context);
                        flowHandler.transition(context, 
                                targetFlow, previousFlow, outboundCallNode, context.getViewRoot().getViewId());
                    }
                    // Invoke transition
                    flowHandler.transition(context, 
                        currentFlow, targetFlow, outboundCallNode, context.getViewRoot().getViewId());

                    // Handle 2 or more flow consecutive start.
                    boolean failed = false;
                    
                    String startNodeId = targetFlow.getStartNodeId();
                    while (startNodeId != null && !failed)
                    {
                        NavigationCase navCase = nh.getNavigationCase(context, null, 
                                    startNodeId, targetFlow.getDefiningDocumentId());
                        
                        if (navCase != null && navCase.getToFlowDocumentId() != null)
                        {
                            currentFlow = flowHandler.getCurrentFlow(context);
                            node = currentFlow.getNode(navCase.getFromOutcome());
                            if (node != null && node instanceof FlowCallNode)
                            {
                                outboundCallNode = (FlowCallNode) node;
                                
                                String calledFlowDocumentId = outboundCallNode.getCalledFlowDocumentId(context);
                                if (calledFlowDocumentId == null)
                                {
                                    calledFlowDocumentId = currentFlow.getDefiningDocumentId();
                                }
                                targetFlow = flowHandler.getFlow(context, 
                                    calledFlowDocumentId, 
                                    outboundCallNode.getCalledFlowId(context));
                                if (targetFlow == null && !"".equals(calledFlowDocumentId))
                                {
                                    targetFlow = flowHandler.getFlow(context, "", 
                                        outboundCallNode.getCalledFlowId(context));
                                }
                            }
                            else
                            {
                                String calledFlowDocumentId = navCase.getToFlowDocumentId();
                                if (calledFlowDocumentId == null)
                                {
                                    calledFlowDocumentId = currentFlow.getDefiningDocumentId();
                                }
                                targetFlow = flowHandler.getFlow(context, 
                                    calledFlowDocumentId, 
                                    navCase.getFromOutcome());
                                if (targetFlow == null && !"".equals(calledFlowDocumentId))
                                {
                                    targetFlow = flowHandler.getFlow(context, "", 
                                        navCase.getFromOutcome());
                                }
                            }
                            if (targetFlow != null)
                            {
                                flowHandler.transition(context, 
                                    currentFlow, targetFlow, outboundCallNode, context.getViewRoot().getViewId());
                                startNodeId = targetFlow.getStartNodeId();
                            }
                            else
                            {
                                startNodeId = null;
                            }
                        }
                        else
                        {
                            startNodeId = null;
                        }
                    }
                }
                
            }
        }
    }
    
    private void checkNull(final Object o, final String param)
    {
        if (o == null)
        {
            throw new NullPointerException(param + " can not be null.");
        }
    }
    
    private void invokeInspectFlow(FacesContext context, NavigationHandler navHandler, Flow toAdd)
    {
        if (navHandler instanceof ConfigurableNavigationHandler)
        {
            ((ConfigurableNavigationHandler)navHandler).inspectFlow(context, toAdd);
        }
        else if (navHandler instanceof NavigationHandlerWrapper)
        {
            invokeInspectFlow(context, ((NavigationHandlerWrapper)navHandler).getWrapped(), toAdd);
        }
    }
    
    private _FlowContextualInfo getCurrentFlowReference(FacesContext context, ClientWindow clientWindow)
    {
        if ( Boolean.TRUE.equals(context.getAttributes().get(RETURN_MODE)) )
        {
            List<_FlowContextualInfo> returnFlowList = getCurrentReturnModeFlowStack(
                    context, clientWindow, CURRENT_FLOW_REQUEST_STACK);
            if (returnFlowList != null && !returnFlowList.isEmpty())
            {
                _FlowContextualInfo info = returnFlowList.get(returnFlowList.size()-1);
                return info;
            }
            return null;
        }
        else
        {
            Map<String, Object> sessionMap = context.getExternalContext().getSessionMap();
            String currentFlowMapKey = CURRENT_FLOW_STACK + clientWindow.getId();
            List<_FlowContextualInfo> currentFlowStack = 
                (List<_FlowContextualInfo>) sessionMap.get(currentFlowMapKey);
            if (currentFlowStack == null)
            {
                return null;
            }
            return currentFlowStack.size() > 0 ? 
                currentFlowStack.get(currentFlowStack.size()-1) : null;
        }
    }
    
    private void pushFlowReference(FacesContext context, ClientWindow clientWindow, FlowReference flowReference,
        String toViewId, FlowReference sourceFlowReference)
    {
        Map<String, Object> sessionMap = context.getExternalContext().getSessionMap();
        String currentFlowMapKey = CURRENT_FLOW_STACK + clientWindow.getId();
        List<_FlowContextualInfo> currentFlowStack = (List<_FlowContextualInfo>) sessionMap.get(currentFlowMapKey);
        if (currentFlowStack == null)
        {
            currentFlowStack = new ArrayList<_FlowContextualInfo>(4);
            sessionMap.put(currentFlowMapKey, currentFlowStack);
        }
        if (!currentFlowStack.isEmpty())
        {
            currentFlowStack.get(currentFlowStack.size()-1).setLastDisplayedViewId(context.getViewRoot().getViewId());
        }
        else
        {
            //Save root lastDisplayedViewId
            context.getExternalContext().getSessionMap().put(ROOT_LAST_VIEW_ID + clientWindow.getId(), 
                context.getViewRoot().getViewId());
        }
        currentFlowStack.add(new _FlowContextualInfo(flowReference, toViewId, sourceFlowReference));
    }
    
    private List<_FlowContextualInfo> getCurrentFlowStack(FacesContext context, ClientWindow clientWindow)
    {
        Map<String, Object> sessionMap = context.getExternalContext().getSessionMap();
        String currentFlowMapKey = CURRENT_FLOW_STACK + clientWindow.getId();
        List<_FlowContextualInfo> currentFlowStack = (List<_FlowContextualInfo>) sessionMap.get(currentFlowMapKey);
        return currentFlowStack;
    }

    @Override
    public String getLastDisplayedViewId(FacesContext context)
    {
        Object session = context.getExternalContext().getSession(false);
        if (session == null)
        {
            return null;
        }
        ClientWindow clientWindow = context.getExternalContext().getClientWindow();
        if (clientWindow == null)
        {
            return null;
        }
        
        _FlowContextualInfo info = getCurrentFlowReference(context, clientWindow);
        if (info == null)
        {
            String lastDisplayedViewId = (String) context.getAttributes().get(ROOT_LAST_VIEW_ID);
            if (lastDisplayedViewId == null)
            {
                lastDisplayedViewId = (String) context.getExternalContext().getSessionMap().
                    get(ROOT_LAST_VIEW_ID + clientWindow.getId());
            }
            return lastDisplayedViewId;
        }
        return info.getLastDisplayedViewId();
    }

    @Override
    public void pushReturnMode(FacesContext context)
    {
        // The return mode is a way to allow NavigationHandler to know the context
        // without expose it. The idea is call pushReturnMode()/popReturnMode() and
        // then check for getCurrentFlow(). 
        //
        // Remember the navigation algorithm is split in two parts:
        // - Calculates the navigation
        // - Perform the navigation
        //
        // Generated links requires only to perform the first one, but the operations
        // are only perfomed when the transition between pages occur or in a get request
        // when there is a pending navigation. 
        ClientWindow clientWindow = context.getExternalContext().getClientWindow();
        
        if (clientWindow == null)
        {
            return;
        }
        
        if ( !Boolean.TRUE.equals(context.getAttributes().get(RETURN_MODE)) )
        {
            // Return mode not active, activate it, copy the current flow stack.
            List<_FlowContextualInfo> currentFlowStack = getCurrentFlowStack(context, clientWindow);
            
            Map<Object, Object> attributesMap = context.getAttributes();
            String returnFlowMapKey = CURRENT_FLOW_REQUEST_STACK + clientWindow.getId();
            List<_FlowContextualInfo> returnFlowStack = new ArrayList<_FlowContextualInfo>(currentFlowStack);
            attributesMap.put(returnFlowMapKey, returnFlowStack);
            context.getAttributes().put(RETURN_MODE, Boolean.TRUE);
        }
        
        _FlowContextualInfo flowReference = popFlowReferenceReturnMode(context, 
            clientWindow, CURRENT_FLOW_REQUEST_STACK);
        pushFlowReferenceReturnMode(context, clientWindow, FLOW_RETURN_STACK, flowReference);
    }

    @Override
    public void popReturnMode(FacesContext context)
    {
        ClientWindow clientWindow = context.getExternalContext().getClientWindow();
        
        if (clientWindow == null)
        {
            return;
        }
        
        _FlowContextualInfo flowReference = popFlowReferenceReturnMode(context, clientWindow, FLOW_RETURN_STACK);
        pushFlowReferenceReturnMode(context, clientWindow, CURRENT_FLOW_REQUEST_STACK, flowReference);
        
        Map<Object, Object> attributesMap = context.getAttributes();
        String returnFlowMapKey = FLOW_RETURN_STACK + clientWindow.getId();
        List<_FlowContextualInfo> returnFlowStack = (List<_FlowContextualInfo>) attributesMap.get(returnFlowMapKey);
        if (returnFlowStack != null && returnFlowStack.isEmpty())
        {
            context.getAttributes().put(RETURN_MODE, Boolean.FALSE);
        }
    }
    
    public List<Flow> getActiveFlows(FacesContext context)
    {
        Object session = context.getExternalContext().getSession(false);
        if (session == null)
        {
            return Collections.emptyList();
        }
        ClientWindow clientWindow = context.getExternalContext().getClientWindow();
        if (clientWindow == null)
        {
            return Collections.emptyList();
        }
        if ( Boolean.TRUE.equals(context.getAttributes().get(RETURN_MODE)) )
        {
            // Use the standard form
            FlowHandler fh = context.getApplication().getFlowHandler();
            Flow curFlow = fh.getCurrentFlow(context);
            if (curFlow != null)
            {
                List<Flow> activeFlows = new ArrayList<Flow>();
                while (curFlow != null)
                {
                    activeFlows.add(curFlow);
                    fh.pushReturnMode(context);
                    curFlow = fh.getCurrentFlow(context);
                }

                for (int i = 0; i < activeFlows.size(); i++)
                {
                    fh.popReturnMode(context);
                }
                return activeFlows;
            }
            else
            {
                return Collections.emptyList();
            }
        }
        else
        {
            Map<String, Object> sessionMap = context.getExternalContext().getSessionMap();
            String currentFlowMapKey = CURRENT_FLOW_STACK + clientWindow.getId();

            List<_FlowContextualInfo> currentFlowStack = (List<_FlowContextualInfo>) sessionMap.get(currentFlowMapKey);
            if (currentFlowStack == null)
            {
                return Collections.emptyList();
            }

            if (!currentFlowStack.isEmpty())
            {
                List<Flow> activeFlows = new ArrayList<Flow>();
                for(_FlowContextualInfo info : currentFlowStack)
                {
                    activeFlows.add(0, getFlow(context, 
                        info.getFlowReference().getDocumentId(), 
                        info.getFlowReference().getId()));
                }
                return activeFlows;
            }

            return Collections.emptyList();
        }
    }

    private void pushFlowReferenceReturnMode(FacesContext context, ClientWindow clientWindow,
            String stackKey, _FlowContextualInfo flowReference)
    {
        Map<Object, Object> attributesMap = context.getAttributes();
        String currentFlowMapKey = stackKey + clientWindow.getId();
        List<_FlowContextualInfo> currentFlowStack = (List<_FlowContextualInfo>) attributesMap.get(currentFlowMapKey);
        if (currentFlowStack == null)
        {
            currentFlowStack = new ArrayList<_FlowContextualInfo>(4);
            attributesMap.put(currentFlowMapKey, currentFlowStack);
        }
        currentFlowStack.add(flowReference);
    }

    private _FlowContextualInfo popFlowReferenceReturnMode(FacesContext context, ClientWindow clientWindow,
            String stackKey)
    {
        Map<Object, Object> attributesMap = context.getAttributes();
        String currentFlowMapKey = stackKey + clientWindow.getId();
        List<_FlowContextualInfo> currentFlowStack = (List<_FlowContextualInfo>) attributesMap.get(currentFlowMapKey);
        if (currentFlowStack == null)
        {
            return null;
        }
        return currentFlowStack.size() > 0 ? currentFlowStack.remove(currentFlowStack.size()-1) : null;
    }
    
    private List<_FlowContextualInfo> getCurrentReturnModeFlowStack(FacesContext context, ClientWindow clientWindow,
            String stackKey)
    {
        Map<Object, Object> attributesMap = context.getAttributes();
        String currentFlowMapKey = stackKey + clientWindow.getId();
        List<_FlowContextualInfo> currentFlowStack = (List<_FlowContextualInfo>) attributesMap.get(currentFlowMapKey);
        return currentFlowStack;
    }
    
    public static List<Flow> getActiveFlows(FacesContext facesContext, FlowHandler fh)
    {
        FlowHandler flowHandler = fh;
        while (flowHandler != null)
        {
            if (flowHandler instanceof FlowHandlerImpl)
            {
                break;
            }
            else if (flowHandler instanceof FacesWrapper)
            {
                flowHandler = ((FacesWrapper<FlowHandler>)flowHandler).getWrapped();
            }
            else
            {
                flowHandler = null;
            }
        }
        if (flowHandler == null)
        {
            // Use the standard form
            Flow curFlow = fh.getCurrentFlow(facesContext);
            if (curFlow != null)
            {
                List<Flow> activeFlows = new ArrayList<Flow>();
                while (curFlow != null)
                {
                    activeFlows.add(curFlow);
                    fh.pushReturnMode(facesContext);
                    curFlow = fh.getCurrentFlow(facesContext);
                }

                for (int i = 0; i < activeFlows.size(); i++)
                {
                    fh.popReturnMode(facesContext);
                }
                return activeFlows;
            }
            else
            {
                return Collections.emptyList();
            }
        }
        else
        {
            FlowHandlerImpl flowHandlerImpl = (FlowHandlerImpl) flowHandler;
            return flowHandlerImpl.getActiveFlows(facesContext);
        }
    }

    @Override
    public boolean isListenerForSource(Object source)
    {
        return source instanceof ClientWindow;
    }

    @Override
    public void processEvent(SystemEvent event)
    {
        // refresh client window to faces flow provider
        FacesContext facesContext = FacesContext.getCurrentInstance();
        FacesFlowProvider provider = getFacesFlowProvider(facesContext);
        provider.refreshClientWindow(facesContext);
    }

}

