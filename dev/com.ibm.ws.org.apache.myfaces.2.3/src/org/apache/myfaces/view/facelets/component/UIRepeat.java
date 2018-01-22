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
package org.apache.myfaces.view.facelets.component;

import java.io.IOException;
import java.io.Serializable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.FacesMessage;
import javax.faces.component.ContextCallback;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitHint;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.FacesEvent;
import javax.faces.event.FacesListener;
import javax.faces.event.PhaseId;
import javax.faces.model.ArrayDataModel;
import javax.faces.model.CollectionDataModel;
import javax.faces.model.DataModel;
import javax.faces.model.IterableDataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.ResultSetDataModel;
import javax.faces.model.ScalarDataModel;
import javax.faces.render.Renderer;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;
import org.apache.myfaces.cdi.model.DataModelBuilderProxy;
import org.apache.myfaces.shared.renderkit.html.util.SharedStringBuilder;
import org.apache.myfaces.util.ExternalSpecifications;

/**
 *  
 */
@JSFComponent(name="ui:repeat", defaultRendererType="facelets.ui.Repeat")
public class UIRepeat extends UIComponentBase implements NamingContainer
{
    public static final String COMPONENT_TYPE = "facelets.ui.Repeat";

    public static final String COMPONENT_FAMILY = "facelets";
    
    private static final String STRING_BUILDER_KEY
            = UIRepeat.class.getName() + ".SHARED_STRING_BUILDER";
    
    //private static final String SKIP_ITERATION_HINT = "javax.faces.visit.SKIP_ITERATION";

    private final static DataModel<?> EMPTY_MODEL = new ListDataModel<Object>(Collections.emptyList());
    
    private static final Class<Object[]> OBJECT_ARRAY_CLASS = Object[].class;

    private static final Object[] LEAF_NO_STATE = new Object[]{null,null};
    
    private Object _initialDescendantComponentState = null;

    // Holds for each row the states of the child components of this UIData.
    // Note that only "partial" component state is saved: the component fields
    // that are expected to vary between rows.
    private Map<String, Collection<Object[]>> _rowStates = new HashMap<String, Collection<Object[]>>();
    
    /**
     * Handle case where this table is nested inside another table. See method getDataModel for more details.
     * <p>
     * Key: parentClientId (aka rowId when nested within a parent table) Value: DataModel
     */
    private Map<String, DataModel> _dataModelMap = new HashMap<String, DataModel>();
    
    // will be set to false if the data should not be refreshed at the beginning of the encode phase
    private boolean _isValidChilds = true;

    private int _end = -1;
    
    private int _count;
    
    private int _index = -1;

    private transient Object _origValue;
    private transient Object _origVarStatus;

    private transient FacesContext _facesContext;
    
    static final Integer RESET_MODE_OFF = 0;
    static final Integer RESET_MODE_SOFT = 1;
    static final Integer RESET_MODE_HARD = 2;    
    
    public UIRepeat()
    {
        setRendererType("facelets.ui.Repeat");
    }

    @Override
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }
    
    @JSFProperty
    public int getOffset()
    {
        return (Integer) getStateHelper().eval(PropertyKeys.offset, 0);
    }

    public void setOffset(int offset)
    {
        getStateHelper().put(PropertyKeys.offset, offset );
    }
    
    @JSFProperty
    public int getSize()
    {
        return (Integer) getStateHelper().eval(PropertyKeys.size, -1);
    }

    public void setSize(int size)
    {
        getStateHelper().put(PropertyKeys.size, size );
    }
    
    @JSFProperty
    public int getStep()
    {
        return (Integer) getStateHelper().eval(PropertyKeys.step, 1);
    }

    public void setStep(int step)
    {
        getStateHelper().put(PropertyKeys.step, step );
    }
    
    @JSFProperty
    public int getBegin()
    {
        return (Integer) getStateHelper().eval(PropertyKeys.begin, -1);
    }

    public void setBegin(int begin)
    {
        getStateHelper().put(PropertyKeys.begin, begin );
    }
    
    @JSFProperty
    public int getEnd()
    {
        return (Integer) getStateHelper().eval(PropertyKeys.end, -1);
    }

    public void setEnd(int end)
    {
        getStateHelper().put(PropertyKeys.end, end );
    }
    
    @JSFProperty(literalOnly=true)
    public String getVar()
    {
        return (String) getStateHelper().get(PropertyKeys.var);
    }

    public void setVar(String var)
    {
        getStateHelper().put(PropertyKeys.var, var );
    }
    
    @JSFProperty(literalOnly=true)
    public String getVarStatus ()
    {
        return (String) getStateHelper().get(PropertyKeys.varStatus);
    }
    
    public void setVarStatus (String varStatus)
    {
        getStateHelper().put(PropertyKeys.varStatus, varStatus );
    }
    
    protected DataModel getDataModel()
    {
        DataModel dataModel;
        String clientID = "";

        UIComponent parent = getParent();
        if (parent != null)
        {
            clientID = parent.getContainerClientId(getFacesContext());
        }
        dataModel = _dataModelMap.get(clientID);
        if (dataModel == null)
        {
            dataModel = createDataModel();
            _dataModelMap.put(clientID, dataModel);
        }
        return dataModel;
    }
    
    private DataModel createDataModel()
    {
        Object value = getValue();

        if (value == null)
        {
            return EMPTY_MODEL;
        }
        else if (value instanceof DataModel)
        {
            return (DataModel) value;
        }
        else
        {
            DataModel dataModel = null;
            if (ExternalSpecifications.isCDIAvailable(getFacesContext().getExternalContext()))
            {
                dataModel = (new DataModelBuilderProxy()).createDataModel(
                        getFacesContext(), value.getClass(), value);
            }
            if (dataModel == null)
            {
                if (value instanceof List)
                {
                    return new ListDataModel((List<?>) value);
                }
                else if (OBJECT_ARRAY_CLASS.isAssignableFrom(value.getClass()))
                {
                    return new ArrayDataModel((Object[]) value);
                }
                else if (value instanceof ResultSet)
                {
                    return new ResultSetDataModel((ResultSet) value);
                }
                else if (value instanceof Iterable)
                {
                    return new IterableDataModel<>((Iterable<?>) value);
                } 
                else if (value instanceof Map) 
                {
                    return new IterableDataModel<>(((Map<?, ?>) value).entrySet());
                }
                else if (value instanceof Collection)
                {
                    return new CollectionDataModel((Collection) value);
                }
                else
                {
                    return new ScalarDataModel(value);
                }
            }
            else
            {
                return dataModel;
            }
        }
    }
    
    @Override
    public void setValueExpression(String name, ValueExpression binding)
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        else if (name.equals("value"))
        {
            _dataModelMap.clear();
        }
        else if (name.equals("rowIndex"))
        {
            throw new IllegalArgumentException("name " + name);
        }
        super.setValueExpression(name, binding);
    }
    
    @JSFProperty
    public Object getValue()
    {
        return  getStateHelper().eval(PropertyKeys.value);
    }

    public void setValue(Object value)
    {
        getStateHelper().put(PropertyKeys.value, value);
        _dataModelMap.clear();
        _rowStates.clear();
        _isValidChilds = true;
    }

    @Override
    public String getContainerClientId(FacesContext context)
    {
        //MYFACES-2744 UIData.getClientId() should not append rowIndex, instead use UIData.getContainerClientId()
        String clientId = super.getContainerClientId(context);
        
        int index = getIndex();
        if (index == -1)
        {
            return clientId;
        }

        StringBuilder sb = SharedStringBuilder.get(context, STRING_BUILDER_KEY);
        return sb.append(clientId).append(context.getNamingContainerSeparatorChar()).append(index).toString();
    }
    
    private RepeatStatus _getRepeatStatus()
    {
        int begin = getBegin();
        if (begin == -1) 
        {
            begin = getOffset();
        }
        
        return new RepeatStatus(_count == 0, _index + getStep() >= getDataModel().getRowCount(),
            _count, _index, begin, _end, getStep());
        
    }

    private void _captureScopeValues()
    {
        String var = getVar();
        if (var != null)
        {
            _origValue = getFacesContext().getExternalContext().getRequestMap().get(var);
        }
        String varStatus = getVarStatus();
        if (varStatus != null)
        {
            _origVarStatus = getFacesContext().getExternalContext().getRequestMap().get(varStatus);
        }
    }

    private boolean _isIndexAvailable()
    {
        return getDataModel().isRowAvailable();
    }

    private void _restoreScopeValues()
    {
        String var = getVar();
        if (var != null)
        {
            Map<String, Object> attrs = getFacesContext().getExternalContext().getRequestMap();
            if (_origValue != null)
            {
                attrs.put(var, _origValue);
                _origValue = null;
            }
            else
            {
                attrs.remove(var);
            }
        }
        String varStatus = getVarStatus();
        if (getVarStatus() != null)
        {
            Map<String, Object> attrs = getFacesContext().getExternalContext().getRequestMap();
            if (_origVarStatus != null)
            {
                attrs.put(varStatus, _origVarStatus);
                _origVarStatus = null;
            }
            else
            {
                attrs.remove(varStatus);
            }
        }
    }
    
    /**
     * Overwrite the state of the child components of this component with data previously saved by method
     * saveDescendantComponentStates.
     * <p>
     * The saved state info only covers those fields that are expected to vary between rows of a table. 
     * Other fields are not modified.
     */
    @SuppressWarnings("unchecked")
    private void restoreDescendantComponentStates(UIComponent parent, boolean iterateFacets, Object state,
                                                  boolean restoreChildFacets)
    {
        int descendantStateIndex = -1;
        List<? extends Object[]> stateCollection = null;
        
        if (iterateFacets && parent.getFacetCount() > 0)
        {
            Iterator<UIComponent> childIterator = parent.getFacets().values().iterator();
            
            while (childIterator.hasNext())
            {
                UIComponent component = childIterator.next();

                // reset the client id (see spec 3.1.6)
                component.setId(component.getId());
                if (!component.isTransient())
                {
                    if (descendantStateIndex == -1)
                    {
                        stateCollection = ((List<? extends Object[]>) state);
                        descendantStateIndex = stateCollection.isEmpty() ? -1 : 0;
                    }
                    
                    if (descendantStateIndex != -1 && descendantStateIndex < stateCollection.size())
                    {
                        Object[] object = stateCollection.get(descendantStateIndex);
                        if (object[0] != null && component instanceof EditableValueHolder)
                        {
                            ((SavedState) object[0]).restoreState((EditableValueHolder) component);
                        }
                        // If there is descendant state to restore, call it recursively, otherwise
                        // it is safe to skip iteration.
                        if (object[1] != null)
                        {
                            restoreDescendantComponentStates(component, restoreChildFacets, object[1], true);
                        }
                        else
                        {
                            restoreDescendantComponentWithoutRestoreState(component, restoreChildFacets, true);
                        }
                    }
                    else
                    {
                        restoreDescendantComponentWithoutRestoreState(component, restoreChildFacets, true);
                    }
                    descendantStateIndex++;
                }
            }
        }
        
        if (parent.getChildCount() > 0)
        {
            for (int i = 0; i < parent.getChildCount(); i++)
            {
                UIComponent component = parent.getChildren().get(i);

                // reset the client id (see spec 3.1.6)
                component.setId(component.getId());
                if (!component.isTransient())
                {
                    if (descendantStateIndex == -1)
                    {
                        stateCollection = ((List<? extends Object[]>) state);
                        descendantStateIndex = stateCollection.isEmpty() ? -1 : 0;
                    }
                    
                    if (descendantStateIndex != -1 && descendantStateIndex < stateCollection.size())
                    {
                        Object[] object = stateCollection.get(descendantStateIndex);
                        if (object[0] != null && component instanceof EditableValueHolder)
                        {
                            ((SavedState) object[0]).restoreState((EditableValueHolder) component);
                        }
                        // If there is descendant state to restore, call it recursively, otherwise
                        // it is safe to skip iteration.
                        if (object[1] != null)
                        {
                            restoreDescendantComponentStates(component, restoreChildFacets, object[1], true);
                        }
                        else
                        {
                            restoreDescendantComponentWithoutRestoreState(component, restoreChildFacets, true);
                        }
                    }
                    else
                    {
                        restoreDescendantComponentWithoutRestoreState(component, restoreChildFacets, true);
                    }
                    descendantStateIndex++;
                }
            }
        }
    }

    /**
     * Just call component.setId(component.getId()) to reset all client ids and 
     * ensure they will be calculated for the current row, but do not waste time
     * dealing with row state code.
     * 
     * @param parent
     * @param iterateFacets
     * @param restoreChildFacets 
     */
    private void restoreDescendantComponentWithoutRestoreState(UIComponent parent, boolean iterateFacets,
                                                               boolean restoreChildFacets)
    {
        if (iterateFacets && parent.getFacetCount() > 0)
        {
            Iterator<UIComponent> childIterator = parent.getFacets().values().iterator();
            
            while (childIterator.hasNext())
            {
                UIComponent component = childIterator.next();

                // reset the client id (see spec 3.1.6)
                component.setId(component.getId());
                if (!component.isTransient())
                {
                    restoreDescendantComponentWithoutRestoreState(component, restoreChildFacets, true);
                }
            }
        }
        
        if (parent.getChildCount() > 0)
        {
            for (int i = 0; i < parent.getChildCount(); i++)
            {
                UIComponent component = parent.getChildren().get(i);

                // reset the client id (see spec 3.1.6)
                component.setId(component.getId());
                if (!component.isTransient())
                {
                    restoreDescendantComponentWithoutRestoreState(component, restoreChildFacets, true);
                }
            }
        }
    }

    /**
     * Walk the tree of child components of this UIData, saving the parts of their state that can vary between rows.
     * <p>
     * This is very similar to the process that occurs for normal components when the view is serialized. Transient
     * components are skipped (no state is saved for them).
     * <p>
     * If there are no children then null is returned. If there are one or more children, and all children are transient
     * then an empty collection is returned; this will happen whenever a table contains only read-only components.
     * <p>
     * Otherwise a collection is returned which contains an object for every non-transient child component; that object
     * may itself contain a collection of the state of that child's child components.
     */
    private Collection<Object[]> saveDescendantComponentStates(UIComponent parent, boolean iterateFacets,
                                                               boolean saveChildFacets)
    {
        Collection<Object[]> childStates = null;
        // Index to indicate how many components has been passed without state to save.
        int childEmptyIndex = 0;
        int totalChildCount = 0;
                
        if (iterateFacets && parent.getFacetCount() > 0)
        {
            Iterator<UIComponent> childIterator = parent.getFacets().values().iterator();

            while (childIterator.hasNext())
            {
                UIComponent child = childIterator.next();
                if (!child.isTransient())
                {
                    // Add an entry to the collection, being an array of two
                    // elements. The first element is the state of the children
                    // of this component; the second is the state of the current
                    // child itself.

                    if (child instanceof EditableValueHolder)
                    {
                        if (childStates == null)
                        {
                            childStates = new ArrayList<Object[]>(
                                    parent.getFacetCount()
                                    + parent.getChildCount()
                                    - totalChildCount
                                    + childEmptyIndex);
                            for (int ci = 0; ci < childEmptyIndex; ci++)
                            {
                                childStates.add(LEAF_NO_STATE);
                            }
                        }
                    
                        childStates.add(child.getChildCount() > 0 ? 
                                new Object[]{new SavedState((EditableValueHolder) child),
                                    saveDescendantComponentStates(child, saveChildFacets, true)} :
                                new Object[]{new SavedState((EditableValueHolder) child),
                                    null});
                    }
                    else if (child.getChildCount() > 0 || (saveChildFacets && child.getFacetCount() > 0))
                    {
                        Object descendantSavedState = saveDescendantComponentStates(child, saveChildFacets, true);
                        
                        if (descendantSavedState == null)
                        {
                            if (childStates == null)
                            {
                                childEmptyIndex++;
                            }
                            else
                            {
                                childStates.add(LEAF_NO_STATE);
                            }
                        }
                        else
                        {
                            if (childStates == null)
                            {
                                childStates = new ArrayList<Object[]>(
                                        parent.getFacetCount()
                                        + parent.getChildCount()
                                        - totalChildCount
                                        + childEmptyIndex);
                                for (int ci = 0; ci < childEmptyIndex; ci++)
                                {
                                    childStates.add(LEAF_NO_STATE);
                                }
                            }
                            childStates.add(new Object[]{null, descendantSavedState});
                        }
                    }
                    else
                    {
                        if (childStates == null)
                        {
                            childEmptyIndex++;
                        }
                        else
                        {
                            childStates.add(LEAF_NO_STATE);
                        }
                    }
                }
                totalChildCount++;
            }
        }
        
        if (parent.getChildCount() > 0)
        {
            for (int i = 0; i < parent.getChildCount(); i++)
            {
                UIComponent child = parent.getChildren().get(i);
                if (!child.isTransient())
                {
                    // Add an entry to the collection, being an array of two
                    // elements. The first element is the state of the children
                    // of this component; the second is the state of the current
                    // child itself.

                    if (child instanceof EditableValueHolder)
                    {
                        if (childStates == null)
                        {
                            childStates = new ArrayList<Object[]>(
                                    parent.getFacetCount()
                                    + parent.getChildCount()
                                    - totalChildCount
                                    + childEmptyIndex);
                            for (int ci = 0; ci < childEmptyIndex; ci++)
                            {
                                childStates.add(LEAF_NO_STATE);
                            }
                        }
                    
                        childStates.add(child.getChildCount() > 0 ? 
                                new Object[]{new SavedState((EditableValueHolder) child),
                                    saveDescendantComponentStates(child, saveChildFacets, true)} :
                                new Object[]{new SavedState((EditableValueHolder) child),
                                    null});
                    }
                    else if (child.getChildCount() > 0 || (saveChildFacets && child.getFacetCount() > 0))
                    {
                        Object descendantSavedState = saveDescendantComponentStates(child, saveChildFacets, true);
                        
                        if (descendantSavedState == null)
                        {
                            if (childStates == null)
                            {
                                childEmptyIndex++;
                            }
                            else
                            {
                                childStates.add(LEAF_NO_STATE);
                            }
                        }
                        else
                        {
                            if (childStates == null)
                            {
                                childStates = new ArrayList<Object[]>(
                                        parent.getFacetCount()
                                        + parent.getChildCount()
                                        - totalChildCount
                                        + childEmptyIndex);
                                for (int ci = 0; ci < childEmptyIndex; ci++)
                                {
                                    childStates.add(LEAF_NO_STATE);
                                }
                            }
                            childStates.add(new Object[]{null, descendantSavedState});
                        }
                    }
                    else
                    {
                        if (childStates == null)
                        {
                            childEmptyIndex++;
                        }
                        else
                        {
                            childStates.add(LEAF_NO_STATE);
                        }
                    }
                }
                totalChildCount++;
            }
        }
        
        return childStates;
    }
    
    /**
     * Returns the rowCount of the underlying DataModel.
     * @return
     */
    public int getRowCount()
    {
        return getDataModel().getRowCount();
    }
    
    /**
     * Returns the current index.
     */
    public int getIndex()
    {
        return _index;
    }
    
    public void setRowIndex(int index)
    {
        _setIndex(index);
    }
    
    private void _setIndex(int index)
    {
        // save child state
        //_saveChildState();
        if (index < -1)
        {
            throw new IllegalArgumentException("rowIndex is less than -1");
        }

        if (_index == index)
        {
            return;
        }

        FacesContext facesContext = getFacesContext();

        if (_index == -1)
        {
            if (_initialDescendantComponentState == null)
            {
                // Create a template that can be used to initialise any row
                // that we haven't visited before, ie a "saved state" that can
                // be pushed to the "restoreState" method of all the child
                // components to set them up to represent a clean row.
                _initialDescendantComponentState = saveDescendantComponentStates(this, true, true);
            }
        }
        else
        {
            // If no initial component state, there are no EditableValueHolder instances,
            // and that means there is no state to be saved for the current row, so we can
            // skip row state saving code safely.
            if (_initialDescendantComponentState != null)
            {
                // We are currently positioned on some row, and are about to
                // move off it, so save the (partial) state of the components
                // representing the current row. Later if this row is revisited
                // then we can restore this state.
                Collection<Object[]> savedRowState = saveDescendantComponentStates(this, true, true);
                if (savedRowState != null)
                {
                    _rowStates.put(getContainerClientId(facesContext), savedRowState);
                }
            }
        }

        _index = index;
        
        DataModel<?> localModel = getDataModel();
        localModel.setRowIndex(index);

        if (_index != -1)
        {
            String var = getVar();
            if (var != null && localModel.isRowAvailable())
            {
                getFacesContext().getExternalContext().getRequestMap()
                        .put(var, localModel.getRowData());
            }
            String varStatus = getVarStatus();
            if (varStatus != null)
            {
                getFacesContext().getExternalContext().getRequestMap()
                        .put(varStatus, _getRepeatStatus());
            }
        }

        // restore child state
        //_restoreChildState();
        
        if (_index == -1)
        {
            // reset components to initial state
            // If no initial state, skip row restore state code
            if (_initialDescendantComponentState != null)
            {
                restoreDescendantComponentStates(this, true, _initialDescendantComponentState, true);
            }
            else
            {
                restoreDescendantComponentWithoutRestoreState(this, true, true);
            }
        }
        else
        {
            Object rowState = _rowStates.get(getContainerClientId(facesContext));
            if (rowState == null)
            {
                // We haven't been positioned on this row before, so just
                // configure the child components of this component with
                // the standard "initial" state
                // If no initial state, skip row restore state code
                if (_initialDescendantComponentState != null)
                {
                    restoreDescendantComponentStates(this, true, _initialDescendantComponentState, true);
                }
                else
                {
                    restoreDescendantComponentWithoutRestoreState(this, true, true);
                }
            }
            else
            {
                // We have been positioned on this row before, so configure
                // the child components of this component with the (partial)
                // state that was previously saved. Fields not in the
                // partial saved state are left with their original values.
                restoreDescendantComponentStates(this, true, rowState, true);
            }
        }
    }
    
    /**
     * Calculates the count value for the given index.
     * @param index
     * @return
     */
    private int _calculateCountForIndex(int index)
    {
        return (index - getOffset()) / getStep();
    }

    private void _validateAttributes() throws FacesException
    {
        
        int begin = getBegin();
        int end = getEnd();
        int size = getSize();
        int count = getDataModel().getRowCount();
        int offset = getOffset();
        if (begin == -1)
        {
            if (size >= 0)
            {
                end = getOffset() + getSize();
            }
        }      
       
        if (end == -1 && size == -1) 
        {
            if (begin == -1) 
            {
                end = getDataModel().getRowCount();
            } 
            else 
            {
                end = getDataModel().getRowCount() - 1;
            }
        }
        
        int step = getStep();
        boolean sizeIsEnd = false;

        if (size == -1)
        {
            if (begin == -1)
            {
                size =  end;
                sizeIsEnd = true;
            } 
            else 
            {
                size = end - begin + 1;
            }     
        }

        if (end >= 0)
        {
            if (size < 0)
            {
                throw new FacesException("iteration size cannot be less " +
                        "than zero");
            }
            else if (!sizeIsEnd && (begin == -1) && (offset + size) > end)
            {
                throw new FacesException("iteration size cannot be greater " +
                        "than collection size");
            }
            else if (!sizeIsEnd && (begin == -1) && (offset + size) > count)
            {
                throw new FacesException("iteration size cannot be greater " +
                        "than collection size");
            }
            else if (!sizeIsEnd && (begin >= 0) && (begin + size) > end+1)
            {
                throw new FacesException("iteration size cannot be greater " +
                        "than collection size");
            }
            else if(!sizeIsEnd && (begin >= 0) && (end+1 > count))
            {
                throw new FacesException("end cannot be greater " +
                        "than collection size");
            }
            else if(!sizeIsEnd && (begin >= 0) && (begin > count))
            {
                throw new FacesException("begin cannot be greater " +
                        "than collection size");
            }
        }
        if ((begin >= 0) && (begin > end))
        {
            throw new FacesException("begin cannot be greater " +
                    "end");
        }
        if ((size > -1) && (offset > end))
        {
            throw new FacesException("iteration offset cannot be greater " +
                    "than collection size");
        }

        if (step == -1)
        {
            setStep(1);
        }

        if (step < 0)
        {
            throw new FacesException("iteration step size cannot be less " +
                    "than zero");
        }

        else if (step == 0)
        {
            throw new FacesException("iteration step size cannot be equal " +
                    "to zero");
        }

        
        _end = end;
        
    }

    public void process(FacesContext faces, PhaseId phase)
    {
        // stop if not rendered
        if (!isRendered())
        {
            return;
        }
        
        // validate attributes
        _validateAttributes();
        
        // reset index
        _captureScopeValues();
        _setIndex(-1);

        try
        {
            // has children
            if (getChildCount() > 0)
            {
                int i = getOffset();               
                
                int begin = getBegin();
                int end = getEnd(); 
                if (begin == -1)
                {
                    end = getSize();
                    end = (end >= 0) ? i + end - 1 : Integer.MAX_VALUE - 1;
                }
                
                if (begin >= 0) 
                {
                    i = begin;
                }
                int step = getStep();
                // grab renderer
                String rendererType = getRendererType();
                Renderer renderer = null;
                if (rendererType != null)
                {
                    renderer = getRenderer(faces);
                }
                
                _count = 0;
                
                _setIndex(i);
                
                while (i <= end && _isIndexAvailable())
                {

                    if (PhaseId.RENDER_RESPONSE.equals(phase) && renderer != null)
                    {
                        renderer.encodeChildren(faces, this);
                    }
                    else
                    {
                        for (int j = 0, childCount = getChildCount(); j < childCount; j++)
                        {
                            UIComponent child = getChildren().get(j);
                            if (PhaseId.APPLY_REQUEST_VALUES.equals(phase))
                            {
                                child.processDecodes(faces);
                            }
                            else if (PhaseId.PROCESS_VALIDATIONS.equals(phase))
                            {
                                child.processValidators(faces);
                            }
                            else if (PhaseId.UPDATE_MODEL_VALUES.equals(phase))
                            {
                                child.processUpdates(faces);
                            }
                            else if (PhaseId.RENDER_RESPONSE.equals(phase))
                            {
                                child.encodeAll(faces);
                            }
                        }
                    }
                    
                    ++_count;
                    
                    i += step;
                    
                    _setIndex(i);
                }
            }
        }
        catch (IOException e)
        {
            throw new FacesException(e);
        }
        finally
        {
            _setIndex(-1);
            _restoreScopeValues();
        }
    }

    @Override
    public boolean invokeOnComponent(FacesContext context, String clientId,
            ContextCallback callback) throws FacesException
    {
        if (context == null || clientId == null || callback == null)
        {
            throw new NullPointerException();
        }
        
        final String baseClientId = getClientId(context);

        // searching for this component?
        boolean returnValue = baseClientId.equals(clientId);

        boolean isCachedFacesContext = isTemporalFacesContext();
        if (!isCachedFacesContext)
        {
            setTemporalFacesContext(context);
        }

        pushComponentToEL(context, this);
        try
        {
            if (returnValue)
            {
                try
                {
                    callback.invokeContextCallback(context, this);
                    return true;
                }
                catch (Exception e)
                {
                    throw new FacesException(e);
                }
            }
    
            // Now Look throught facets on this UIComponent
            if (this.getFacetCount() > 0)
            {
                for (Iterator<UIComponent> it = this.getFacets().values().iterator(); !returnValue && it.hasNext();)
                {
                    returnValue = it.next().invokeOnComponent(context, clientId, callback);
                }
            }
    
            if (returnValue)
            {
                return returnValue;
            }
            
            // is the component an inner component?
            if (clientId.startsWith(baseClientId))
            {
                // Check if the clientId for the component, which we 
                // are looking for, has a rowIndex attached
                char separator = context.getNamingContainerSeparatorChar();
                String subId = clientId.substring(baseClientId.length() + 1);
                //If the char next to baseClientId is the separator one and
                //the subId matches the regular expression
                if (clientId.charAt(baseClientId.length()) == separator && 
                        subId.matches("[0-9]+"+separator+".*"))
                {
                    String clientRow = subId.substring(0, subId.indexOf(separator));
        
                    // safe the current index, count aside
                    final int prevIndex = _index;
                    final int prevCount = _count;
                    
                    try
                    {
                        int invokeIndex = Integer.parseInt(clientRow);
                        // save the current scope values and set the right index
                        _captureScopeValues();
                        if (invokeIndex != -1)
                        {
                            // calculate count for RepeatStatus
                            _count = _calculateCountForIndex(invokeIndex);
                        }
                        _setIndex(invokeIndex);
                        
                        if (!_isIndexAvailable())
                        {
                            return false;
                        }
                        
                        for (Iterator<UIComponent> it1 = getChildren().iterator(); 
                            !returnValue && it1.hasNext();)
                        {
                            //recursive call to find the component
                            returnValue = it1.next().invokeOnComponent(context, clientId, callback);
                        }
                    }
                    finally
                    {
                        // restore the previous count, index and scope values
                        _count = prevCount;
                        _setIndex(prevIndex);
                        _restoreScopeValues();
                    }
                }
                else
                {
                    // Searching for this component's children
                    if (this.getChildCount() > 0)
                    {
                        // Searching for this component's children/facets
                        for (Iterator<UIComponent> it = this.getChildren().iterator(); !returnValue && it.hasNext();)
                        {
                            returnValue = it.next().invokeOnComponent(context, clientId, callback);
                        }
                    }
                }
            }
        }
        finally
        {
            //all components must call popComponentFromEl after visiting is finished
            popComponentFromEL(context);
            if (!isCachedFacesContext)
            {
                setTemporalFacesContext(null);
            }
        }

        return returnValue;
    }
    
    @Override
    protected FacesContext getFacesContext()
    {
        if (_facesContext == null)
        {
            return super.getFacesContext();
        }
        else
        {
            return _facesContext;
        }
    }
    
    private boolean isTemporalFacesContext()
    {
        return _facesContext != null;
    }
    
    private void setTemporalFacesContext(FacesContext facesContext)
    {
        _facesContext = facesContext;
    }

    @Override
    public boolean visitTree(VisitContext context, VisitCallback callback)
    {
        // override the behavior from UIComponent to visit
        // all children once per "row"

        boolean skipIterationHint = context.getHints().contains(VisitHint.SKIP_ITERATION);
        if (skipIterationHint)
        {
            return super.visitTree(context, callback);
        }
        // push the Component to EL
        pushComponentToEL(context.getFacesContext(), this);
        try
        {
            if (!isVisitable(context))
            {
                return false;
            }

            // save the current index, count aside
            final int prevIndex = _index;
            final int prevCount = _count;

            // validate attributes
            _validateAttributes();

            // reset index and save scope values
            _captureScopeValues();
            _setIndex(-1);

            try
            {
                VisitResult res = context.invokeVisitCallback(this, callback);
                switch (res)
                {
                // we are done, nothing has to be processed anymore
                case COMPLETE:
                    return true;

                case REJECT:
                    return false;

                //accept
                default:
                    // determine if we need to visit our children
                    // Note that we need to do this check because we are a NamingContainer
                    Collection<String> subtreeIdsToVisit = context
                            .getSubtreeIdsToVisit(this);
                    boolean doVisitChildren = subtreeIdsToVisit != null
                            && !subtreeIdsToVisit.isEmpty();
                    if (doVisitChildren)
                    {
                        // visit the facets of the component
                        if (getFacetCount() > 0)
                        {
                            for (UIComponent facet : getFacets().values())
                            {
                                if (facet.visitTree(context, callback))
                                {
                                    return true;
                                }
                            }
                        }

                        // visit the children once per "row"
                        if (getChildCount() > 0)
                        {
                            int i = getOffset();
                            int end = getSize();
                            int step = getStep();
                            end = (end >= 0) ? i + end : Integer.MAX_VALUE - 1;
                            _count = 0;

                            _setIndex(i);
                            while (i < end && _isIndexAvailable())
                            {
                                for (int j = 0, childCount = getChildCount(); j < childCount; j++)
                                {
                                    UIComponent child = getChildren().get(j);
                                    if (child.visitTree(context, callback))
                                    {
                                        return true;
                                    }
                                }

                                _count++;
                                i += step;

                                _setIndex(i);
                            }
                        }
                    }
                    return false;
                }
            }
            finally
            {

                // restore the previous count, index and scope values
                _count = prevCount;
                _setIndex(prevIndex);
                _restoreScopeValues();
            }
        }
        finally
        {
            // pop the component from EL
            popComponentFromEL(context.getFacesContext());
        }
    }

    @Override
    public void processDecodes(FacesContext faces)
    {
        if (!isRendered())
        {
            return;
        }
        
        process(faces, PhaseId.APPLY_REQUEST_VALUES);
        decode(faces);
    }

    @Override
    public void processUpdates(FacesContext faces)
    {
        if (!isRendered())
        {
            return;
        }
        
        process(faces, PhaseId.UPDATE_MODEL_VALUES);
        
        if (faces.getRenderResponse())
        {
            _isValidChilds = false;
        }
    }

    @Override
    public void processValidators(FacesContext faces)
    {
        if (!isRendered())
        {
            return;
        }
        
        process(faces, PhaseId.PROCESS_VALIDATIONS);
        
        // check if an validation error forces the render response for our data
        if (faces.getRenderResponse())
        {
            _isValidChilds = false;
        }
    }

    // from RI
    private final static class SavedState implements Serializable
    {
        private boolean _localValueSet;
        private Object _submittedValue;
        private boolean _valid = true;
        private Object _value;

        private static final long serialVersionUID = 2920252657338389849L;
        
        public SavedState(EditableValueHolder evh)
        {
            _value = evh.getLocalValue();
            _localValueSet = evh.isLocalValueSet();
            _valid = evh.isValid();
            _submittedValue = evh.getSubmittedValue();
        }        

        Object getSubmittedValue()
        {
            return (_submittedValue);
        }

        void setSubmittedValue(Object submittedValue)
        {
            _submittedValue = submittedValue;
        }

        boolean isValid()
        {
            return (_valid);
        }

        void setValid(boolean valid)
        {
            _valid = valid;
        }

        Object getValue()
        {
            return _value;
        }

        public void setValue(Object value)
        {
            _value = value;
        }

        boolean isLocalValueSet()
        {
            return _localValueSet;
        }

        public void setLocalValueSet(boolean localValueSet)
        {
            _localValueSet = localValueSet;
        }

        @Override
        public String toString()
        {
            return ("submittedValue: " + _submittedValue + " value: " + _value + " localValueSet: " + _localValueSet);
        }
        
        public void restoreState(EditableValueHolder evh)
        {
            evh.setValue(_value);
            evh.setValid(_valid);
            evh.setSubmittedValue(_submittedValue);
            evh.setLocalValueSet(_localValueSet);
        }

        public void populate(EditableValueHolder evh)
        {
            _value = evh.getLocalValue();
            _valid = evh.isValid();
            _submittedValue = evh.getSubmittedValue();
            _localValueSet = evh.isLocalValueSet();
        }

        public void apply(EditableValueHolder evh)
        {
            evh.setValue(_value);
            evh.setValid(_valid);
            evh.setSubmittedValue(_submittedValue);
            evh.setLocalValueSet(_localValueSet);
        }
    }

    private final class IndexedEvent extends FacesEvent
    {
        private final FacesEvent _target;

        private final int _index;

        public IndexedEvent(UIRepeat owner, FacesEvent target, int index)
        {
            super(owner);
            _target = target;
            _index = index;
        }

        @Override
        public PhaseId getPhaseId()
        {
            return _target.getPhaseId();
        }

        @Override
        public void setPhaseId(PhaseId phaseId)
        {
            _target.setPhaseId(phaseId);
        }

        @Override
        public boolean isAppropriateListener(FacesListener listener)
        {
            return _target.isAppropriateListener(listener);
        }

        @Override
        public void processListener(FacesListener listener)
        {
            UIRepeat owner = (UIRepeat) getComponent();
            
            // safe the current index, count aside
            final int prevIndex = owner._index;
            final int prevCount = owner._count;
            
            try
            {
                owner._captureScopeValues();
                if (this._index != -1)
                {
                    // calculate count for RepeatStatus
                    _count = _calculateCountForIndex(this._index);
                }
                owner._setIndex(this._index);
                if (owner._isIndexAvailable())
                {
                    _target.processListener(listener);
                }
            }
            finally
            {
                // restore the previous count, index and scope values
                owner._count = prevCount;
                owner._setIndex(prevIndex);
                owner._restoreScopeValues();
            }
        }

        public int getIndex()
        {
            return _index;
        }

        public FacesEvent getTarget()
        {
            return _target;
        }

    }

    @Override
    public void broadcast(FacesEvent event) throws AbortProcessingException
    {
        if (event instanceof IndexedEvent)
        {
            IndexedEvent idxEvent = (IndexedEvent) event;
            
            // safe the current index, count aside
            final int prevIndex = _index;
            final int prevCount = _count;
            
            try
            {
                _captureScopeValues();
                if (idxEvent.getIndex() != -1)
                {
                    // calculate count for RepeatStatus
                    _count = _calculateCountForIndex(idxEvent.getIndex());
                }
                _setIndex(idxEvent.getIndex());
                if (_isIndexAvailable())
                {
                    // get the target FacesEvent
                    FacesEvent target = idxEvent.getTarget();
                    FacesContext facesContext = getFacesContext();
                    
                    // get the component associated with the target event and push
                    // it and its composite component parent, if available, to the
                    // component stack to have them available while processing the 
                    // event (see also UIViewRoot._broadcastAll()).
                    UIComponent targetComponent = target.getComponent();
                    UIComponent compositeParent = UIComponent
                            .getCompositeComponentParent(targetComponent);
                    if (compositeParent != null)
                    {
                        pushComponentToEL(facesContext, compositeParent);
                    }
                    pushComponentToEL(facesContext, targetComponent);
                    
                    try
                    {
                        // actual event broadcasting
                        targetComponent.broadcast(target);
                    }
                    finally
                    {
                        // remove the components from the stack again
                        popComponentFromEL(facesContext);
                        if (compositeParent != null)
                        {
                            popComponentFromEL(facesContext);
                        }
                    }
                }
            }
            finally
            {
                // restore the previous count, index and scope values
                _count = prevCount;
                _setIndex(prevIndex);
                _restoreScopeValues();
            }
        }
        else
        {
            super.broadcast(event);
        }
    }

    @Override
    public void queueEvent(FacesEvent event)
    {
        super.queueEvent(new IndexedEvent(this, event, _index));
    }

    @Override
    public void restoreState(FacesContext context, Object state)
    {
        if (state == null)
        {
            return;
        }
        
        Object values[] = (Object[]) state;
        super.restoreState(context, values[0]);
        //Object restoredRowStates = UIComponentBase.restoreAttachedState(context, values[1]);
        /*
        if (restoredRowStates == null)
        {
            if (!_rowDeltaStates.isEmpty())
            {
                _rowDeltaStates.clear();
            }
        }
        else
        {
            _rowDeltaStates = (Map<String, Map<String, Object> >) restoredRowStates;
        }*/
        if (values.length > 2)
        {
            Object rs = UIComponentBase.restoreAttachedState(context, values[2]);
            if (rs == null)
            {
                if (!_rowStates.isEmpty())
                {
                    _rowStates.clear();
                }
            }
            else
            {
                _rowStates = (Map<String, Collection<Object[]> >) rs;
            }
        }
    }

    @Override
    public Object saveState(FacesContext context)
    {
        if (context.getViewRoot() != null)
        {
            if (context.getViewRoot().getAttributes().get("oam.view.resetSaveStateMode") == RESET_MODE_SOFT)
            {
                _dataModelMap.clear();
                _isValidChilds=true;
                //_rowTransientStates.clear();
            }
            if (context.getViewRoot().getAttributes().get("oam.view.resetSaveStateMode") == RESET_MODE_HARD)
            {
                _dataModelMap.clear();
                _isValidChilds=true;
                //_rowTransientStates.clear();
                _rowStates.clear();
                //_rowDeltaStates.clear();
            }
        }
        if (initialStateMarked())
        {
            Object parentSaved = super.saveState(context);
            if (context.getCurrentPhaseId() != null && 
                !PhaseId.RENDER_RESPONSE.equals(context.getCurrentPhaseId()))
            {
                if (parentSaved == null /*&&_rowDeltaStates.isEmpty()*/ && _rowStates.isEmpty())
                {
                    return null;
                }
                else
                {
                    Object values[] = new Object[3];
                    values[0] = super.saveState(context);
                    //values[1] = UIComponentBase.saveAttachedState(context, _rowDeltaStates);
                    values[1] = null;
                    values[2] = UIComponentBase.saveAttachedState(context, _rowStates);
                    return values;
                }
            }
            else
            {
                if (parentSaved == null /*&&_rowDeltaStates.isEmpty()*/)
                {
                    return null;
                }
                else
                {
                    Object values[] = new Object[2];
                    values[0] = super.saveState(context);
                    //values[1] = UIComponentBase.saveAttachedState(context, _rowDeltaStates);
                    values[1] = null;
                    return values; 
                }
            }
        }
        else
        {
            if (context.getCurrentPhaseId() != null && 
                !PhaseId.RENDER_RESPONSE.equals(context.getCurrentPhaseId()))
            {
                Object values[] = new Object[3];
                values[0] = super.saveState(context);
                //values[1] = UIComponentBase.saveAttachedState(context, _rowDeltaStates);
                values[1] = null;
                values[2] = UIComponentBase.saveAttachedState(context, _rowStates);
                return values; 
            }
            else
            {
                Object values[] = new Object[2];
                values[0] = super.saveState(context);
                //values[1] = UIComponentBase.saveAttachedState(context, _rowDeltaStates);
                values[1] = null;
                return values;
            }
        }
    }
    
    @Override
    public void encodeBegin(FacesContext context) throws IOException
    {
        _initialDescendantComponentState = null;
        if (_isValidChilds && !hasErrorMessages(context))
        {
            // Clear the data model so that when rendering code calls
            // getDataModel a fresh model is fetched from the backing
            // bean via the value-binding.
            _dataModelMap.clear();

            // When the data model is cleared it is also necessary to
            // clear the saved row state, as there is an implicit 1:1
            // relation between objects in the _rowStates and the
            // corresponding DataModel element.
            _rowStates.clear();
        }
        super.encodeBegin(context);
    }
    
    private boolean hasErrorMessages(FacesContext context)
    {
        for (Iterator<FacesMessage> iter = context.getMessages(); iter.hasNext();)
        {
            FacesMessage message = iter.next();
            if (FacesMessage.SEVERITY_ERROR.compareTo(message.getSeverity()) <= 0)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void encodeChildren(FacesContext faces) throws IOException
    {
        if (!isRendered())
        {
            return;
        }
        
        process(faces, PhaseId.RENDER_RESPONSE);
    }

    @Override
    public boolean getRendersChildren()
    {
        if (getRendererType() != null)
        {
            Renderer renderer = getRenderer(getFacesContext());
            if (renderer != null)
            {
                return renderer.getRendersChildren();
            }
        }
        
        return true;
    }
    
    enum PropertyKeys
    {
         value
        , var
        , size
        , varStatus
        , offset
        , step
        , begin
        , end
    }
}
