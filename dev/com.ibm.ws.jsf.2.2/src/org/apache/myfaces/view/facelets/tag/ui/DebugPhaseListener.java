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
package org.apache.myfaces.view.facelets.tag.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitHint;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.apache.myfaces.renderkit.ErrorPageWriter;

/**
 * PhaseListener to create extended debug information.
 * Installed in FacesConfigurator.configureLifecycle() if ProjectStage is Development.
 * 
 * @author Jakob Korherr (latest modification by $Author: martinkoci $)
 * @version $Revision: 1133517 $ $Date: 2011-06-08 19:19:52 +0000 (Wed, 08 Jun 2011) $
 */
public class DebugPhaseListener implements PhaseListener
{
    
    private static final long serialVersionUID = -1517198431551012882L;
    
    private static final String SUBMITTED_VALUE_FIELD = "submittedValue";
    private static final String LOCAL_VALUE_FIELD = "localValue";
    private static final String VALUE_FIELD = "value";
    
    /**
     * Returns the debug-info Map for the given component.
     * ATTENTION: this method is duplicate in UIInput.
     * @param clientId
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Map<String, List<Object[]>> getDebugInfoMap(String clientId)
    {
        final Map<String, Object> requestMap = FacesContext.getCurrentInstance()
                .getExternalContext().getRequestMap();
        Map<String, List<Object[]>> debugInfo = (Map<String, List<Object[]>>) 
                requestMap.get(ErrorPageWriter.DEBUG_INFO_KEY + clientId);
        if (debugInfo == null)
        {
            // no debug info available yet, create one and put it on the attributes map
            debugInfo = new HashMap<String, List<Object[]>>();
            requestMap.put(ErrorPageWriter.DEBUG_INFO_KEY + clientId, debugInfo);
        }
        return debugInfo;
    }
    
    /**
     * Returns the field's debug-infos from the component's debug-info Map.
     * ATTENTION: this method is duplicate in UIInput.
     * @param field
     * @param clientId
     * @return
     */
    public static List<Object[]> getFieldDebugInfos(final String field, String clientId)
    {
        Map<String, List<Object[]>> debugInfo = getDebugInfoMap(clientId);
        List<Object[]> fieldDebugInfo = debugInfo.get(field);
        if (fieldDebugInfo == null)
        {
            // no field debug-infos yet, create them and store it in the Map
            fieldDebugInfo = new ArrayList<Object[]>();
            debugInfo.put(field, fieldDebugInfo);
        }
        return fieldDebugInfo;
    }
    
    /**
     * Creates the field debug-info for the given field, which changed
     * from oldValue to newValue in the given component.
     * ATTENTION: this method is duplicate in UIInput.
     * @param facesContext
     * @param field
     * @param oldValue
     * @param newValue
     * @param clientId
     */
    public static void createFieldDebugInfo(FacesContext facesContext,
            final String field, Object oldValue, 
            Object newValue, String clientId)
    {
        if ((oldValue == null && newValue == null)
                || (oldValue != null && oldValue.equals(newValue)))
        {
            // nothing changed - NOTE that this is a difference to the method in 
            // UIInput, because in UIInput every call to this method comes from
            // setSubmittedValue or setLocalValue and here every call comes
            // from the VisitCallback of the PhaseListener.
            return;
        }
        
        // convert Array values into a more readable format
        if (oldValue != null && oldValue.getClass().isArray())
        {
            oldValue = Arrays.deepToString((Object[]) oldValue);
        }
        if (newValue != null && newValue.getClass().isArray())
        {
            newValue = Arrays.deepToString((Object[]) newValue);
        }
        
        // NOTE that the call stack does not make much sence here
        
        // create the debug-info array
        // structure:
        //     - 0: phase
        //     - 1: old value
        //     - 2: new value
        //     - 3: StackTraceElement List
        // NOTE that we cannot create a class here to encapsulate this data,
        // because this is not on the spec and the class would not be available in impl.
        Object[] debugInfo = new Object[4];
        debugInfo[0] = facesContext.getCurrentPhaseId();
        debugInfo[1] = oldValue;
        debugInfo[2] = newValue;
        debugInfo[3] = null; // here we have no call stack (only in UIInput)
        
        // add the debug info
        getFieldDebugInfos(field, clientId).add(debugInfo);
    }

    /**
     * VisitCallback used for visitTree()  
     *  
     * @author Jakob Korherr
     */
    private class DebugVisitCallback implements VisitCallback
    {

        public VisitResult visit(VisitContext context, UIComponent target)
        {
            if (target instanceof EditableValueHolder)
            {
                EditableValueHolder evh = (EditableValueHolder) target;
                final String clientId = target.getClientId(context.getFacesContext());
                Map<String, Object> requestMap = context.getFacesContext()
                        .getExternalContext().getRequestMap();
                
                if (_afterPhase)
                {
                    // afterPhase - check for value changes
                    
                    // submittedValue
                    _createFieldDebugInfosIfNecessary(SUBMITTED_VALUE_FIELD, clientId,
                            evh.getSubmittedValue(), requestMap, context.getFacesContext());
                    
                    // localValue
                    final Object localValue = evh.getLocalValue();
                    _createFieldDebugInfosIfNecessary(LOCAL_VALUE_FIELD, clientId,
                            localValue, requestMap, context.getFacesContext());
                    
                    // value
                    final Object value = _getRealValue(evh, target, localValue,
                            context.getFacesContext().getELContext());
                    _createFieldDebugInfosIfNecessary(VALUE_FIELD, clientId,
                            value, requestMap, context.getFacesContext());
                }
                else
                {
                    // beforePhase - save the current value state
                    
                    // submittedValue
                    requestMap.put(ErrorPageWriter.DEBUG_INFO_KEY + clientId 
                            + SUBMITTED_VALUE_FIELD, evh.getSubmittedValue());
                    
                    // localValue
                    final Object localValue = evh.getLocalValue();
                    requestMap.put(ErrorPageWriter.DEBUG_INFO_KEY + clientId 
                            + LOCAL_VALUE_FIELD, localValue);
                    
                    // value
                    final Object value = _getRealValue(evh, target, localValue,
                            context.getFacesContext().getELContext());
                    requestMap.put(ErrorPageWriter.DEBUG_INFO_KEY + clientId 
                            + VALUE_FIELD, value);
                }
            }
            
            return VisitResult.ACCEPT;
        }
        
        /**
         * Checks if there are debug infos available for the given field and the
         * current Phase and if NOT, it creates and adds them to the request Map.
         * @param field
         * @param clientId
         * @param newValue
         * @param requestMap
         * @param facesContext
         */
        private void _createFieldDebugInfosIfNecessary(final String field, 
                final String clientId, Object newValue,
                Map<String, Object> requestMap, FacesContext facesContext)
        {
            // check if there are already debugInfos from UIInput
            List<Object[]> fieldDebugInfos = getFieldDebugInfos(field, clientId);
            boolean found = false;
            for (int i = 0, size = fieldDebugInfos.size(); i < size; i++)
            {
                Object[] debugInfo = fieldDebugInfos.get(i);
                if (debugInfo[0].equals(_currentPhase))
                {
                    found = true;
                    break;
                }
            }
            if (!found)
            {
                // there are no debug infos for this field in this lifecycle phase yet
                // --> create them
                Object oldValue = requestMap.remove(ErrorPageWriter.DEBUG_INFO_KEY 
                        + clientId + field);
                createFieldDebugInfo(facesContext, field,
                        oldValue, newValue, clientId);
            }
        }
        
        /**
         * Gets the real value of the EditableValueHolder component.
         * This is necessary, because if the localValue is set, getValue()
         * normally returns the localValue and not the real value.
         * @param evh
         * @param target
         * @param localValue
         * @param elCtx
         * @return
         */
        private Object _getRealValue(EditableValueHolder evh, UIComponent target,
                final Object localValue, ELContext elCtx)
        {
            Object value = evh.getValue();
            if (localValue != null && localValue.equals(value))
            {
                // getValue() normally returns the localValue, if it is set
                // --> try to get the real value from the ValueExpression
                ValueExpression valueExpression = target.getValueExpression("value");
                if (valueExpression != null)
                {
                    value = valueExpression.getValue(elCtx);
                }
            }
            return value;
        }
        
    }
    
    private boolean _afterPhase = false;
    private PhaseId _currentPhase;
    private DebugVisitCallback _visitCallback = new DebugVisitCallback();

    public void afterPhase(PhaseEvent event)
    {
        _doTreeVisit(event, true);
    }

    public void beforePhase(PhaseEvent event)
    {
        _doTreeVisit(event, false);
    }

    public PhaseId getPhaseId()
    {
        return PhaseId.ANY_PHASE;
    }
    
    private void _doTreeVisit(PhaseEvent event, boolean afterPhase)
    {
        _afterPhase = afterPhase;
        _currentPhase = event.getPhaseId();
        
        // visitTree() on the UIViewRoot
        UIViewRoot viewroot = event.getFacesContext().getViewRoot();
        if (viewroot != null)
        {
            // skip all unrendered components to really only show
            // the rendered components and to circumvent data access problems
            viewroot.visitTree(VisitContext.createVisitContext(
                    event.getFacesContext(), null, 
                    EnumSet.of(VisitHint.SKIP_UNRENDERED)),
                    _visitCallback);
        }
    }

}
