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
package jakarta.faces.component;

import org.apache.myfaces.core.api.shared.lang.ClassUtils;
import org.apache.myfaces.core.api.shared.ComponentUtils;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.el.ValueExpression;
import jakarta.faces.FacesException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.application.StateManager;
import jakarta.faces.component.visit.VisitCallback;
import jakarta.faces.component.visit.VisitContext;
import jakarta.faces.component.visit.VisitHint;
import jakarta.faces.component.visit.VisitResult;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.AbortProcessingException;
import jakarta.faces.event.FacesEvent;
import jakarta.faces.event.FacesListener;
import jakarta.faces.event.PhaseId;
import jakarta.faces.event.PostValidateEvent;
import jakarta.faces.event.PreValidateEvent;
import jakarta.faces.model.ArrayDataModel;
import jakarta.faces.model.CollectionDataModel;
import jakarta.faces.model.DataModel;
import jakarta.faces.model.IterableDataModel;
import jakarta.faces.model.ListDataModel;
import jakarta.faces.model.ResultSetDataModel;
import jakarta.faces.model.ScalarDataModel;
import java.util.regex.Pattern;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFacet;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;
import org.apache.myfaces.core.api.shared.lang.Assert;

/**
 * Represents an abstraction of a component which has multiple "rows" of data.
 * <p>
 * The children of this component are expected to be UIColumn components.
 * <p>
 * Note that the same set of child components are reused to implement each row of the table in turn during such phases
 * as apply-request-values and render-response. Altering any of the members of these components therefore affects the
 * attribute for every row, except for the following members:
 * <ul>
 * <li>submittedValue
 * <li>value (where no EL binding is used)
 * <li>valid
 * </ul>
 * <p>
 * This reuse of the child components also means that it is not possible to save a reference to a component during table
 * processing, then access it later and expect it to still represent the same row of the table.
 * <h1>
 * Implementation Notes</h1>
 * <p>
 * Each of the UIColumn children of this component has a few component children of its own to render the contents of the
 * table cell. However there can be a very large number of rows in a table, so it isn't efficient for the UIColumn and
 * all its child objects to be duplicated for each row in the table. Instead the "flyweight" pattern is used where a
 * serialized state is held for each row. When setRowIndex is invoked, the UIColumn objects and their children serialize
 * their current state then reinitialise themselves from the appropriate saved state. This allows a single set of real
 * objects to represent multiple objects which have the same types but potentially different internal state. When a row
 * is selected for the first time, its state is set to a clean "initial" state. Transient components (including any
 * read-only component) do not save their state; they are just reinitialised as required. The state saved/restored when
 * changing rows is not the complete component state, just the fields that are expected to vary between rows:
 * "submittedValue", "value", "isValid".
 * </p>
 * <p>
 * Note that a table is a "naming container", so that components within the table have their ids prefixed with the id of
 * the table. Actually, when setRowIndex has been called on a table with id of "zzz" the table pretends to its children
 * that its ID is "zzz_n" where n is the row index. This means that renderers for child components which call
 * component.getClientId automatically get ids of form "zzz_n:childId" thus ensuring that components in different rows
 * of the table get different ids.
 * </p>
 * <p>
 * When decoding a submitted page, this class iterates over all its possible rowIndex values, restoring the appropriate
 * serialized row state then calling processDecodes on the child components. Because the child components (or their
 * renderers) use getClientId to get the request key to look for parameter data, and because this object pretends to
 * have a different id per row ("zzz_n") a single child component can decode data from each table row in turn without
 * being aware that it is within a table. The table's data model is updated before each call to child.processDecodes, so
 * the child decode method can assume that the data model's rowData points to the model object associated with the row
 * currently being decoded. Exactly the same process applies for the later validation and updateModel phases.
 * </p>
 * <p>
 * When the data model for the table is bound to a backing bean property, and no validation errors have occured during
 * processing of a postback, the data model is refetched at the start of the rendering phase (ie after the update model
 * phase) so that the contents of the data model can be changed as a result of the latest form submission. Because the
 * saved row state must correspond to the elements within the data model, the row state must be discarded whenever a new
 * data model is fetched; not doing this would cause all sorts of inconsistency issues. This does imply that changing
 * the state of any of the members "submittedValue", "value" or "valid" of a component within the table during the
 * invokeApplication phase has no effect on the rendering of the table. When a validation error has occurred, a new
 * DataModel is <i>not</i> fetched, and the saved state of the child components is <i>not</i> discarded.
 * </p>
 * see Javadoc of the <a href="http://java.sun.com/j2ee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 * for more information.
 */
@JSFComponent(defaultRendererType = "jakarta.faces.Table")
public class UIData extends UIComponentBase implements NamingContainer, UniqueIdVendor
{
    public static final String COMPONENT_FAMILY = "jakarta.faces.Data";
    public static final String COMPONENT_TYPE = "jakarta.faces.Data"; // for unit tests

    private static final String SUB_ID_PATTERN = "oam.UIData.SUB_ID_PATTERN";
    
    private static final String FACES_DATA_MODEL_MANAGER_CLASS_NAME
            = "org.apache.myfaces.cdi.model.FacesDataModelManager";
    private static final Class<?> FACES_DATA_MODEL_MANAGER_CLASS;
    private static final Method FACES_DATA_MODEL_MANAGER_CREATE_DATAMODEL_METHOD;
    
    static
    {
        Class<?> dataModelBuilderClass = null;
        Method createDataModelMethod = null;
        try
        {
            dataModelBuilderClass = ClassUtils.classForName(FACES_DATA_MODEL_MANAGER_CLASS_NAME);
            if (dataModelBuilderClass != null)
            {
                createDataModelMethod = dataModelBuilderClass.getMethod("createDataModel",
                        new Class[]{FacesContext.class, Class.class, Object.class});
            }
        }
        catch(Exception e)
        {
            //No Op
        }
        FACES_DATA_MODEL_MANAGER_CLASS = dataModelBuilderClass;
        FACES_DATA_MODEL_MANAGER_CREATE_DATAMODEL_METHOD = createDataModelMethod;
    }
    
    private static final String FOOTER_FACET_NAME = "footer";
    private static final String HEADER_FACET_NAME = "header";
    private static final Class<Object[]> OBJECT_ARRAY_CLASS = Object[].class;
    private static final int PROCESS_DECODES = 1;
    private static final int PROCESS_VALIDATORS = 2;
    private static final int PROCESS_UPDATES = 3;

    private static final Object[] LEAF_NO_STATE = new Object[]{null,null};

    private int _rowIndex = -1;

    // Holds for each row the states of the child components of this UIData.
    // Note that only "partial" component state is saved: the component fields
    // that are expected to vary between rows.
    private Map<String, Object> _rowStates = new HashMap<>();
    private Map<String, Map<String, Object>> _rowDeltaStates = new HashMap<>();
    private Map<String, Map<String, Object>> _rowTransientStates = new HashMap<>();

    /**
     * Handle case where this table is nested inside another table. See method getDataModel for more details.
     * <p>
     * Key: parentClientId (aka rowId when nested within a parent table) Value: DataModel
     */
    private Map<String, DataModel> _dataModelMap = new HashMap<>(3, 1f);

    // will be set to false if the data should not be refreshed at the beginning of the encode phase
    private boolean _isValidChilds = true;

    private Object _initialDescendantComponentState = null;
    
    private Object _initialDescendantFullComponentState = null;

    private static class FacesEventWrapper extends FacesEvent
    {
        private static final long serialVersionUID = 6648047974065628773L;
        private FacesEvent _wrappedFacesEvent;
        private int _rowIndex;

        public FacesEventWrapper(FacesEvent facesEvent, int rowIndex, UIData redirectComponent)
        {
            super(redirectComponent);
            _wrappedFacesEvent = facesEvent;
            _rowIndex = rowIndex;
        }

        @Override
        public PhaseId getPhaseId()
        {
            return _wrappedFacesEvent.getPhaseId();
        }

        @Override
        public void setPhaseId(PhaseId phaseId)
        {
            _wrappedFacesEvent.setPhaseId(phaseId);
        }

        @Override
        public void queue()
        {
            _wrappedFacesEvent.queue();
        }

        @Override
        public String toString()
        {
            return _wrappedFacesEvent.toString();
        }

        @Override
        public boolean isAppropriateListener(FacesListener faceslistener)
        {
            return _wrappedFacesEvent.isAppropriateListener(faceslistener);
        }

        @Override
        public void processListener(FacesListener faceslistener)
        {
            _wrappedFacesEvent.processListener(faceslistener);
        }

        public FacesEvent getWrappedFacesEvent()
        {
            return _wrappedFacesEvent;
        }

        public int getRowIndex()
        {
            return _rowIndex;
        }
    }

    private static final DataModel EMPTY_DATA_MODEL = new DataModel()
    {
        @Override
        public boolean isRowAvailable()
        {
            return false;
        }

        @Override
        public int getRowCount()
        {
            return 0;
        }

        @Override
        public Object getRowData()
        {
            throw new IllegalArgumentException();
        }

        @Override
        public int getRowIndex()
        {
            return -1;
        }

        @Override
        public void setRowIndex(int i)
        {
            if (i < -1)
            {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public Object getWrappedData()
        {
            return null;
        }

        @Override
        public void setWrappedData(Object obj)
        {
            if (obj == null)
            {
                return; // Clearing is allowed
            }
            throw new UnsupportedOperationException(this.getClass().getName() + " UnsupportedOperationException");
        }
    };

    private static class EditableValueHolderState implements Serializable
    {
        private final Object _value;
        private final boolean _localValueSet;
        private final boolean _valid;
        private final Object _submittedValue;

        public EditableValueHolderState(EditableValueHolder evh)
        {
            _value = evh.getLocalValue();
            _localValueSet = evh.isLocalValueSet();
            _valid = evh.isValid();
            _submittedValue = evh.getSubmittedValue();
        }

        public void restoreState(EditableValueHolder evh)
        {
            evh.setValue(_value);
            evh.setLocalValueSet(_localValueSet);
            evh.setValid(_valid);
            evh.setSubmittedValue(_submittedValue);
        }
    }

    /**
     * Construct an instance of the UIData.
     */
    public UIData()
    {
        setRendererType("jakarta.faces.Table");
    }

    @Override
    public boolean invokeOnComponent(FacesContext context, String clientId, ContextCallback callback)
        throws FacesException
    {
        Assert.notNull(context, "context");
        Assert.notNull(clientId, "clientId");
        Assert.notNull(callback, "callback");
        
        final String baseClientId = getClientId(context);

        // searching for this component?
        boolean returnValue = baseClientId.equals(clientId);

        boolean isCachedFacesContext = isCachedFacesContext();
        if (!isCachedFacesContext)
        {
            setCachedFacesContext(context);
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
                Pattern pattern = getSubIdPattern(context, separator);
                
                String subId = clientId.substring(baseClientId.length() + 1);
                //If the char next to baseClientId is the separator one and
                //the subId matches the regular expression
                if (clientId.charAt(baseClientId.length()) == separator && pattern.matcher(subId).matches())
                {
                    String clientRow = subId.substring(0, subId.indexOf(separator));

                    //Now we save the current position
                    int oldRow = this.getRowIndex();
                    
                    // try-finally --> make sure, that the old row index is restored
                    try
                    {
                        //The conversion is safe, because its already checked on the
                        //regular expresion
                        this.setRowIndex(Integer.parseInt(clientRow));
                        
                        // check, if the row is available
                        if (!isRowAvailable())
                        {
                            return false;
                        }
            
                        for (Iterator<UIComponent> it1 = getChildren().iterator(); !returnValue && it1.hasNext();)
                        {
                            //recursive call to find the component
                            returnValue = it1.next().invokeOnComponent(context, clientId, callback);
                        }
                    }
                    finally
                    {
                        //Restore the old position. Doing this prevent
                        //side effects.
                        this.setRowIndex(oldRow);
                    }
                }
                else
                {
                    // MYFACES-2370: search the component in the childrens' facets too.
                    // We have to check the childrens' facets here, because in MyFaces
                    // the rowIndex is not attached to the clientId for the children of
                    // facets of the UIColumns. However, in RI the rowIndex is 
                    // attached to the clientId of UIColumns' Facets' children.
                    for (Iterator<UIComponent> itChildren = this.getChildren().iterator();
                            !returnValue && itChildren.hasNext();)
                    {
                        UIComponent child = itChildren.next();
                        if (child instanceof UIColumn && clientId.equals(child.getClientId(context)))
                        {
                            try
                            {
                                callback.invokeContextCallback(context, child);
                            }
                            catch (Exception e)
                            {
                                throw new FacesException(e);
                            }
                            returnValue = true;
                        }
                        // process the child's facets
                        if (child.getFacetCount() > 0)
                        {
                            for (Iterator<UIComponent> itChildFacets = child.getFacets().values().iterator(); 
                                !returnValue && itChildFacets.hasNext();)
                            {
                                //recursive call to find the component
                                returnValue = itChildFacets.next().invokeOnComponent(context, clientId, callback);
                            }
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
                setCachedFacesContext(null);
            }
        }

        return returnValue;
    }

    protected Pattern getSubIdPattern(FacesContext context, char separator)
    {
        Pattern pattern = (Pattern) context.getAttributes().get(SUB_ID_PATTERN);
        if (pattern == null)
        {
            pattern = Pattern.compile("[0-9]+" + separator + ".*");
            context.getAttributes().put(SUB_ID_PATTERN, pattern);
        }
        return pattern;
    }
    
    public void setFooter(UIComponent footer)
    {
        getFacets().put(FOOTER_FACET_NAME, footer);
    }

    @JSFFacet
    public UIComponent getFooter()
    {
        return getFacets().get(FOOTER_FACET_NAME);
    }

    public void setHeader(UIComponent header)
    {
        getFacets().put(HEADER_FACET_NAME, header);
    }

    @JSFFacet
    public UIComponent getHeader()
    {
        return getFacets().get(HEADER_FACET_NAME);
    }

    public boolean isRowAvailable()
    {
        return getDataModel().isRowAvailable();
    }

    public int getRowCount()
    {
        return getDataModel().getRowCount();
    }

    public Object getRowData()
    {
        return getDataModel().getRowData();
    }

    public int getRowIndex()
    {
        return _rowIndex;
    }

    /**
     * Set the current row index that methods like getRowData use.
     * <p>
     * Param rowIndex can be -1, meaning "no row".
     * <p>
     * 
     * @param rowIndex
     */
    public void setRowIndex(int rowIndex)
    {
        if (isRowStatePreserved())
        {
            setRowIndexPreserveComponentState(rowIndex);
        }
        else
        {
            setRowIndexWithoutPreserveComponentState(rowIndex);
        }
    }

    private void setRowIndexWithoutPreserveComponentState(int rowIndex)
    {
        if (rowIndex < -1)
        {
            throw new IllegalArgumentException("rowIndex is less than -1");
        }

        if (_rowIndex == rowIndex)
        {
            return;
        }

        FacesContext facesContext = getFacesContext();

        if (_rowIndex == -1)
        {
            if (_initialDescendantComponentState == null)
            {
                // Create a template that can be used to initialise any row
                // that we haven't visited before, ie a "saved state" that can
                // be pushed to the "restoreState" method of all the child
                // components to set them up to represent a clean row.
                _initialDescendantComponentState = saveDescendantComponentStates(this, false, false);
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
                Collection<Object[]> savedRowState = saveDescendantComponentStates(this, false, false);
                if (savedRowState != null)
                {
                    _rowStates.put(getContainerClientId(facesContext), savedRowState);
                }
            }
        }

        _rowIndex = rowIndex;

        DataModel dataModel = getDataModel();
        dataModel.setRowIndex(rowIndex);

        String var = (String) getStateHelper().get(PropertyKeys.var);
        if (rowIndex == -1)
        {
            if (var != null)
            {
                facesContext.getExternalContext().getRequestMap().remove(var);
            }
        }
        else
        {
            if (var != null)
            {
                if (isRowAvailable())
                {
                    Object rowData = dataModel.getRowData();
                    facesContext.getExternalContext().getRequestMap().put(var, rowData);
                }
                else
                {
                    facesContext.getExternalContext().getRequestMap().remove(var);
                }
            }
        }

        if (_rowIndex == -1)
        {
            // reset components to initial state
            // If no initial state, skip row restore state code
            if (_initialDescendantComponentState != null)
            {
                restoreDescendantComponentStates(this, false, _initialDescendantComponentState, false);
            }
            else
            {
                restoreDescendantComponentWithoutRestoreState(this, false, false);
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
                    restoreDescendantComponentStates(this, false, _initialDescendantComponentState, false);
                }
                else
                {
                    restoreDescendantComponentWithoutRestoreState(this, false, false);
                }
            }
            else
            {
                // We have been positioned on this row before, so configure
                // the child components of this component with the (partial)
                // state that was previously saved. Fields not in the
                // partial saved state are left with their original values.
                restoreDescendantComponentStates(this, false, rowState, false);
            }
        }
    }

    private void setRowIndexPreserveComponentState(int rowIndex)
    {
        if (rowIndex < -1)
        {
            throw new IllegalArgumentException("rowIndex is less than -1");
        }

        if (_rowIndex == rowIndex)
        {
            return;
        }

        FacesContext facesContext = getFacesContext();

        if (_initialDescendantFullComponentState != null)
        {
            //Just save the row
            Map<String, Object> sm = saveFullDescendantComponentStates(facesContext, null,
                                                                       getChildren().iterator(), false);
            if (sm != null && !sm.isEmpty())
            {
                _rowDeltaStates.put(getContainerClientId(facesContext), sm);
            }
            if (_rowIndex != -1)
            {
                _rowTransientStates.put(getContainerClientId(facesContext),
                        saveTransientDescendantComponentStates(facesContext, null, getChildren().iterator(), false));
            }
        }

        _rowIndex = rowIndex;

        DataModel dataModel = getDataModel();
        dataModel.setRowIndex(rowIndex);

        String var = (String) getStateHelper().get(PropertyKeys.var);
        if (rowIndex == -1)
        {
            if (var != null)
            {
                facesContext.getExternalContext().getRequestMap().remove(var);
            }
        }
        else
        {
            if (var != null)
            {
                if (isRowAvailable())
                {
                    Object rowData = dataModel.getRowData();
                    facesContext.getExternalContext().getRequestMap().put(var, rowData);
                }
                else
                {
                    facesContext.getExternalContext().getRequestMap().remove(var);
                }
            }
        }

        if (_initialDescendantFullComponentState != null)
        {
            Map<String, Object> rowState = _rowDeltaStates.get(getContainerClientId(facesContext));
            if (rowState == null)
            {
                //Restore as original
                restoreFullDescendantComponentStates(facesContext, getChildren().iterator(),
                        _initialDescendantFullComponentState, false);
            }
            else
            {
                //Restore first original and then delta
                restoreFullDescendantComponentDeltaStates(facesContext, getChildren().iterator(),
                        rowState, _initialDescendantFullComponentState, false);
            }
            if (_rowIndex == -1)
            {
                restoreTransientDescendantComponentStates(facesContext, getChildren().iterator(), null, false);
            }
            else
            {
                rowState = _rowTransientStates.get(getContainerClientId(facesContext));
                if (rowState == null)
                {
                    restoreTransientDescendantComponentStates(facesContext, getChildren().iterator(), null, false);
                }
                else
                {
                    restoreTransientDescendantComponentStates(facesContext, getChildren().iterator(), rowState, false);
                }
            }
        }

    }
    

    /**
     * Overwrite the state of the child components of this component with data previously saved by method
     * saveDescendantComponentStates.
     * <p>
     * The saved state info only covers those fields that are expected to vary between rows of a table. Other fields are
     * not modified.
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
                            ((EditableValueHolderState) object[0]).restoreState((EditableValueHolder) component);
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
                            ((EditableValueHolderState) object[0]).restoreState((EditableValueHolder) component);
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
                            childStates = new ArrayList<>(
                                    parent.getFacetCount()
                                    + parent.getChildCount()
                                    - totalChildCount
                                    + childEmptyIndex);
                            for (int ci = 0; ci < childEmptyIndex; ci++)
                            {
                                childStates.add(LEAF_NO_STATE);
                            }
                        }
                    
                        childStates.add(child.getChildCount() > 0
                                ? new Object[]{ new EditableValueHolderState((EditableValueHolder) child),
                                    saveDescendantComponentStates(child, saveChildFacets, true) }
                                : new Object[]{ new EditableValueHolderState((EditableValueHolder) child), null });
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
                                childStates = new ArrayList<>(
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
                            childStates = new ArrayList<>(
                                    parent.getFacetCount()
                                    + parent.getChildCount()
                                    - totalChildCount
                                    + childEmptyIndex);
                            for (int ci = 0; ci < childEmptyIndex; ci++)
                            {
                                childStates.add(LEAF_NO_STATE);
                            }
                        }
                    
                        childStates.add(child.getChildCount() > 0
                                ? new Object[]{ new EditableValueHolderState((EditableValueHolder) child),
                                    saveDescendantComponentStates(child, saveChildFacets, true) }
                                : new Object[]{ new EditableValueHolderState((EditableValueHolder) child), null });
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
                                childStates = new ArrayList<>(
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
    
    
    
    @Override
    public void markInitialState()
    {
        if (isRowStatePreserved() && 
            getFacesContext().getAttributes().containsKey(StateManager.IS_BUILDING_INITIAL_STATE))
        {
            _initialDescendantFullComponentState
                    = saveDescendantInitialComponentStates(getFacesContext(), getChildren().iterator(), false);
        }
        super.markInitialState();
    }

    private void restoreFullDescendantComponentStates(FacesContext facesContext,
            Iterator<UIComponent> childIterator, Object initialState,
            boolean restoreChildFacets)
    {
        Iterator<? extends Object[]> descendantStateIterator = null;
        while (childIterator.hasNext())
        {
            if (descendantStateIterator == null && initialState != null)
            {
                descendantStateIterator = ((Collection<? extends Object[]>) initialState).iterator();
            }
            
            UIComponent component = childIterator.next();
            // reset the client id (see spec 3.1.6)
            component.setId(component.getId());

            if (!component.isTransient())
            {
                Object childState = null;
                Object descendantState = null;
                String childId = null;
                if (descendantStateIterator != null && descendantStateIterator.hasNext())
                {
                    do
                    {
                        Object[] object = descendantStateIterator.next();
                        childState = object[0];
                        descendantState = object[1];
                        childId = (String) object[2];
                    }
                    while(descendantStateIterator.hasNext() && !component.getId().equals(childId));
                    
                    if (!component.getId().equals(childId))
                    {
                        // cannot apply initial state to components correctly.
                        throw new IllegalStateException("Cannot restore row correctly.");
                    }
                }
                
                component.clearInitialState();
                component.restoreState(facesContext, childState);
                component.markInitialState();
                
                Iterator<UIComponent> childsIterator = restoreChildFacets
                        ? component.getFacetsAndChildren()
                        : component.getChildren().iterator();
                restoreFullDescendantComponentStates(facesContext, childsIterator, descendantState, true);
            }
        }
    }

    private Collection<Object[]> saveDescendantInitialComponentStates(FacesContext facesContext,
            Iterator<UIComponent> childIterator, boolean saveChildFacets)
    {
        Collection<Object[]> childStates = null;
        while (childIterator.hasNext())
        {
            UIComponent child = childIterator.next();
            if (!child.isTransient())
            {
                Iterator<UIComponent> childsIterator = saveChildFacets
                        ? child.getFacetsAndChildren()
                        : child.getChildren().iterator();
                Object descendantState = saveDescendantInitialComponentStates(facesContext, childsIterator, true);
                Object state = null;
                if (child.initialStateMarked())
                {
                    child.clearInitialState();
                    state = child.saveState(facesContext); 
                    child.markInitialState();
                }
                else
                {
                    state = child.saveState(facesContext);
                }

                // Add an entry to the collection, being an array of two elements.
                // The first element is the state of the children of this component;
                // the second is the state of the current child itself.
                if (childStates == null)
                {
                    childStates = new ArrayList<>();
                }
                childStates.add(new Object[] { state, descendantState, child.getId()});
            }
        }
        return childStates;
    }
    
    private Map<String,Object> saveFullDescendantComponentStates(FacesContext facesContext, Map<String,Object> stateMap,
            Iterator<UIComponent> childIterator, boolean saveChildFacets)
    {
        while (childIterator.hasNext())
        {
            UIComponent child = childIterator.next();
            if (!child.isTransient())
            {
                // Add an entry to the collection, being an array of two
                // elements. The first element is the state of the children
                // of this component; the second is the state of the current
                // child itself.
                Iterator<UIComponent> childsIterator = saveChildFacets 
                        ? child.getFacetsAndChildren()
                        : child.getChildren().iterator();
                stateMap = saveFullDescendantComponentStates(facesContext, stateMap, childsIterator, true);
                Object state = child.saveState(facesContext);
                if (state != null)
                {
                    if (stateMap == null)
                    {
                        stateMap = new HashMap<>();
                    }
                    stateMap.put(child.getClientId(facesContext), state);
                }
            }
        }
        return stateMap;
    }
    
    private void restoreFullDescendantComponentDeltaStates(FacesContext facesContext,
            Iterator<UIComponent> childIterator, Map<String, Object> state, Object initialState,
            boolean restoreChildFacets)
    {
        Iterator<? extends Object[]> descendantFullStateIterator = null;
        while (childIterator.hasNext())
        {
            if (descendantFullStateIterator == null && initialState != null)
            {
                descendantFullStateIterator = ((Collection<? extends Object[]>) initialState).iterator();
            }

            UIComponent component = childIterator.next();
            // reset the client id (see spec 3.1.6)
            component.setId(component.getId());
            
            if (!component.isTransient())
            {
                Object childInitialState = null;
                Object descendantInitialState = null;
                Object childState = null;
                String childId = null;
                childState = (state == null) ? null : state.get(component.getClientId(facesContext));

                if (descendantFullStateIterator != null && descendantFullStateIterator.hasNext())
                {
                    do
                    {
                        Object[] object = descendantFullStateIterator.next();
                        childInitialState = object[0];
                        descendantInitialState = object[1];
                        childId = (String) object[2];
                    } while(descendantFullStateIterator.hasNext() && !component.getId().equals(childId));
                    
                    if (!component.getId().equals(childId))
                    {
                        // cannot apply initial state to components correctly. State is corrupt
                        throw new IllegalStateException("Cannot restore row correctly.");
                    }
                }
                
                component.clearInitialState();
                if (childInitialState != null)
                {
                    component.restoreState(facesContext, childInitialState);
                    component.markInitialState();
                    component.restoreState(facesContext, childState);
                }
                else
                {
                    component.restoreState(facesContext, childState);
                    component.markInitialState();
                }
                
                Iterator<UIComponent> childsIterator = restoreChildFacets
                        ? component.getFacetsAndChildren()
                        : component.getChildren().iterator();
                restoreFullDescendantComponentDeltaStates(facesContext, childsIterator, state,
                        descendantInitialState , true);
            }
        }
    }
    
    private void restoreTransientDescendantComponentStates(FacesContext facesContext,
                                                           Iterator<UIComponent> childIterator,
                                                           Map<String, Object> state,
                                                           boolean restoreChildFacets)
    {
        while (childIterator.hasNext())
        {
            UIComponent component = childIterator.next();

            // reset the client id (see spec 3.1.6)
            component.setId(component.getId());
            if (!component.isTransient())
            {
                component.restoreTransientState(facesContext,
                        state == null ? null : state.get(component.getClientId(facesContext)));
                
                Iterator<UIComponent> childsIterator = restoreChildFacets
                        ? component.getFacetsAndChildren()
                        : component.getChildren().iterator();
                restoreTransientDescendantComponentStates(facesContext, childsIterator, state, true);
            }
        }

    }
    
    private Map<String, Object> saveTransientDescendantComponentStates(FacesContext facesContext,
                                                                       Map<String, Object> childStates,
                                                                       Iterator<UIComponent> childIterator,
                                                                       boolean saveChildFacets)
    {
        while (childIterator.hasNext())
        {
            UIComponent child = childIterator.next();
            if (!child.isTransient())
            {
                Iterator<UIComponent> childsIterator = saveChildFacets
                        ? child.getFacetsAndChildren()
                        : child.getChildren().iterator();
                childStates = saveTransientDescendantComponentStates(facesContext, childStates, childsIterator, true);
                Object state = child.saveTransientState(facesContext);
                if (state != null)
                {
                    if (childStates == null)
                    {
                        childStates = new HashMap<>();
                    }
                    childStates.put(child.getClientId(facesContext), state);
                }
            }
        }
        return childStates;
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

        Object restoredRowStates = UIComponentBase.restoreAttachedState(context, values[1]);
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
        }

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
                _rowStates = (Map<String, Object>) rs;
            }
        }
        if (values.length > 3)
        {
            Object rs = UIComponentBase.restoreAttachedState(context, values[3]);
            if (rs == null)
            {
                if (!_rowTransientStates.isEmpty())
                {
                    _rowTransientStates.clear();
                }
            }
            else
            {
                _rowTransientStates = (Map<String, Map<String, Object> >) rs;
            }
        }
    }

    @Override
    public Object saveState(FacesContext context)
    {
        if (context.getViewRoot() != null)
        {
            if (context.getViewRoot().getResetSaveStateMode() == RESET_MODE_SOFT)
            {
                _dataModelMap.clear();
                _isValidChilds = true;
                _rowTransientStates.clear();
            }
            if (context.getViewRoot().getResetSaveStateMode() == RESET_MODE_HARD)
            {
                _dataModelMap.clear();
                _isValidChilds = true;
                _rowTransientStates.clear();
                _rowStates.clear();
                _rowDeltaStates.clear();
            }
        }

        if (initialStateMarked())
        {
            Object parentSaved = super.saveState(context);
            if (context.getCurrentPhaseId() != null && 
                !PhaseId.RENDER_RESPONSE.equals(context.getCurrentPhaseId()))
            {
                if (parentSaved == null &&_rowDeltaStates.isEmpty() && _rowStates.isEmpty())
                {
                    return null;
                }
                else
                {
                    Object values[] = new Object[4];
                    values[0] = super.saveState(context);
                    values[1] = UIComponentBase.saveAttachedState(context, _rowDeltaStates);
                    values[2] = UIComponentBase.saveAttachedState(context, _rowStates);
                    values[3] = UIComponentBase.saveAttachedState(context, _rowTransientStates);
                    return values;
                }
            }
            else
            {
                if (parentSaved == null &&_rowDeltaStates.isEmpty())
                {
                    return null;
                }
                else
                {
                    Object values[] = new Object[2];
                    values[0] = super.saveState(context);
                    values[1] = UIComponentBase.saveAttachedState(context, _rowDeltaStates);
                    return values; 
                }
            }
        }
        else
        {
            if (context.getCurrentPhaseId() != null && !PhaseId.RENDER_RESPONSE.equals(context.getCurrentPhaseId()))
            {
                Object values[] = new Object[4];
                values[0] = super.saveState(context);
                values[1] = UIComponentBase.saveAttachedState(context, _rowDeltaStates);
                values[2] = UIComponentBase.saveAttachedState(context, _rowStates);
                values[3] = UIComponentBase.saveAttachedState(context, _rowTransientStates);
                return values; 
            }
            else
            {
                Object values[] = new Object[2];
                values[0] = super.saveState(context);
                values[1] = UIComponentBase.saveAttachedState(context, _rowDeltaStates);
                return values;
            }
        }
    }

    @Override
    public void setValueExpression(String name, ValueExpression binding)
    {
        Assert.notNull(name, "name");

        if (name.equals("value"))
        {
            _dataModelMap.clear();
        }
        else if (name.equals("rowIndex"))
        {
            throw new IllegalArgumentException("name " + name);
        }
        super.setValueExpression(name, binding);
    }

    @Override
    public String getContainerClientId(FacesContext context)
    {
        //MYFACES-2744 UIData.getClientId() should not append rowIndex, instead use UIData.getContainerClientId()
        String clientId = super.getContainerClientId(context);
        
        int rowIndex = getRowIndex();
        if (rowIndex == -1)
        {
            return clientId;
        }

        StringBuilder bld = _getSharedStringBuilder(context);
        return bld.append(clientId).append(context.getNamingContainerSeparatorChar()).append(rowIndex).toString();
    }

    /**
     * Modify events queued for any child components so that the UIData state will be correctly configured before the
     * event's listeners are executed.
     * <p>
     * Child components or their renderers may register events against those child components. When the listener for
     * that event is eventually invoked, it may expect the uidata's rowData and rowIndex to be referring to the same
     * object that caused the event to fire.
     * <p>
     * The original queueEvent call against the child component has been forwarded up the chain of ancestors in the
     * standard way, making it possible here to wrap the event in a new event whose source is <i>this</i> component, not
     * the original one. When the event finally is executed, this component's broadcast method is invoked, which ensures
     * that the UIData is set to be at the correct row before executing the original event.
     */
    @Override
    public void queueEvent(FacesEvent event)
    {
        Assert.notNull(event, "event");

        super.queueEvent(new FacesEventWrapper(event, getRowIndex(), this));
    }

    /**
     * Ensure that before the event's listeners are invoked this UIData component's "current row" is set to the row
     * associated with the event.
     * <p>
     * See queueEvent for more details.
     */
    @Override
    public void broadcast(FacesEvent event) throws AbortProcessingException
    {
        if (event instanceof FacesEventWrapper)
        {
            FacesEvent originalEvent = ((FacesEventWrapper) event).getWrappedFacesEvent();
            int eventRowIndex = ((FacesEventWrapper) event).getRowIndex();
            final int currentRowIndex = getRowIndex();
            UIComponent source = originalEvent.getComponent();
            UIComponent compositeParent = UIComponent.getCompositeComponentParent(source);

            setRowIndex(eventRowIndex);
            if (compositeParent != null)
            {
                pushComponentToEL(getFacesContext(), compositeParent);
            }

            pushComponentToEL(getFacesContext(), source);

            try
            {
                source.broadcast(originalEvent);
            }
            finally
            {
                source.popComponentFromEL(getFacesContext());
                if (compositeParent != null)
                {
                    compositeParent.popComponentFromEL(getFacesContext());
                }
                setRowIndex(currentRowIndex);
            }
        }
        else
        {
            super.broadcast(event);
        }
    }

    /**
     * 
     * {@inheritDoc}
     * 
     * @since 2.0
     */
    @Override
    public String createUniqueId(FacesContext context, String seed)
    {
        StringBuilder bld = _getSharedStringBuilder(context);

        // Generate an identifier for a component. The identifier will be prefixed with UNIQUE_ID_PREFIX,
        // and will be unique within this UIViewRoot.
        if(seed == null)
        {
            Long uniqueIdCounter = (Long) getStateHelper().get(PropertyKeys.uniqueIdCounter);
            uniqueIdCounter = (uniqueIdCounter == null) ? 0 : uniqueIdCounter;
            getStateHelper().put(PropertyKeys.uniqueIdCounter, (uniqueIdCounter+1L));
            return bld.append(UIViewRoot.UNIQUE_ID_PREFIX).append(uniqueIdCounter).toString();    
        }
        // Optionally, a unique seed value can be supplied by component creators
        // which should be included in the generated unique id.
        else
        {
            return bld.append(UIViewRoot.UNIQUE_ID_PREFIX).append(seed).toString();
        }
    }

    /**
     * Perform necessary actions when rendering of this component starts, before delegating to the inherited
     * implementation which calls the associated renderer's encodeBegin method.
     */
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
            if (!isRowStatePreserved())
            {
                _rowStates.clear();
            }
        }
        super.encodeBegin(context);
    }

    private boolean hasErrorMessages(FacesContext context)
    {
        // perf: getMessageList() return a RandomAccess instance.
        // See org.apache.myfaces.context.servlet.FacesContextImpl.addMessage
        List<FacesMessage> messageList = context.getMessageList();
        for (int i = 0, size = messageList.size(); i < size;  i++)
        {
            FacesMessage message = messageList.get(i);
            if (FacesMessage.SEVERITY_ERROR.compareTo(message.getSeverity()) <= 0)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * @see jakarta.faces.component.UIComponentBase#encodeEnd(jakarta.faces.context.FacesContext)
     */
    @Override
    public void encodeEnd(FacesContext context) throws IOException
    {
        try
        {
            setCachedFacesContext(context);
            setRowIndex(-1);
        }
        finally
        {
            setCachedFacesContext(null);
        }
        super.encodeEnd(context);
    }

    @Override
    public void processDecodes(FacesContext context)
    {
        Assert.notNull(context, "context");

        try
        {
            setCachedFacesContext(context);
            pushComponentToEL(context, this);
            if (!isRendered())
            {
                return;
            }

            setRowIndex(-1);
            processFacets(context, PROCESS_DECODES);
            processColumnFacets(context, PROCESS_DECODES);
            processColumnChildren(context, PROCESS_DECODES);
            setRowIndex(-1);

            try
            {
                decode(context);
            }
            catch (RuntimeException e)
            {
                context.renderResponse();
                throw e;
            }
        }
        finally
        {
            popComponentFromEL(context);
            setCachedFacesContext(null);
        }
    }

    @Override
    public void processValidators(FacesContext context)
    {
        Assert.notNull(context, "context");

        try
        {
            setCachedFacesContext(context);
            pushComponentToEL(context, this);
            if (!isRendered())
            {
                return;
            }
            
            //Pre validation event dispatch for component
            context.getApplication().publishEvent(context,  PreValidateEvent.class, getClass(), this);
            
            try
            {
                setRowIndex(-1);
                processFacets(context, PROCESS_VALIDATORS);
                processColumnFacets(context, PROCESS_VALIDATORS);
                processColumnChildren(context, PROCESS_VALIDATORS);
                setRowIndex(-1);
            }
            finally
            {
                context.getApplication().publishEvent(context,  PostValidateEvent.class, getClass(), this);
            }
            
            // check if an validation error forces the render response for our data
            if (context.getRenderResponse())
            {
                _isValidChilds = false;
            }
        }
        finally
        {
            popComponentFromEL(context);
            setCachedFacesContext(null);
        }
    }

    @Override
    public void processUpdates(FacesContext context)
    {
        Assert.notNull(context, "context");

        try
        {
            setCachedFacesContext(context);
            pushComponentToEL(context, this);
            if (!isRendered())
            {
                return;
            }

            setRowIndex(-1);
            processFacets(context, PROCESS_UPDATES);
            processColumnFacets(context, PROCESS_UPDATES);
            processColumnChildren(context, PROCESS_UPDATES);
            setRowIndex(-1);
    
            if (context.getRenderResponse())
            {
                _isValidChilds = false;
            }
        }
        finally
        {
            popComponentFromEL(context);
            setCachedFacesContext(null);
        }
    }

    private void processFacets(FacesContext context, int processAction)
    {
        if (this.getFacetCount() > 0)
        {
            for (UIComponent facet : getFacets().values())
            {
                process(context, facet, processAction);
            }
        }
    }

    /**
     * Invoke the specified phase on all facets of all UIColumn children of this component. Note that no methods are
     * called on the UIColumn child objects themselves.
     * 
     * @param context
     *            is the current faces context.
     * @param processAction
     *            specifies a JSF phase: decode, validate or update.
     */
    private void processColumnFacets(FacesContext context, int processAction)
    {
        for (int i = 0, childCount = getChildCount(); i < childCount; i++)
        {
            UIComponent child = getChildren().get(i);
            if (child instanceof UIColumn)
            {
                if (!ComponentUtils.isRendered(context, child))
                {
                    // Column is not visible
                    continue;
                }
                
                if (child.getFacetCount() > 0)
                {
                    for (UIComponent facet : child.getFacets().values())
                    {
                        process(context, facet, processAction);
                    }
                }
            }
        }
    }

    /**
     * Invoke the specified phase on all non-facet children of all UIColumn children of this component. Note that no
     * methods are called on the UIColumn child objects themselves.
     * 
     * @param context
     *            is the current faces context.
     * @param processAction
     *            specifies a JSF phase: decode, validate or update.
     */
    private void processColumnChildren(FacesContext context, int processAction)
    {
        int first = getFirst();
        int rows = getRows();
        int last;
        if (rows == 0)
        {
            last = getRowCount();
        }
        else
        {
            last = first + rows;
        }
        for (int rowIndex = first; last == -1 || rowIndex < last; rowIndex++)
        {
            setRowIndex(rowIndex);

            // scrolled past the last row
            if (!isRowAvailable())
            {
                break;
            }
            
            for (int i = 0, childCount = getChildCount(); i < childCount; i++)
            {
                UIComponent child = getChildren().get(i);
                if (child instanceof UIColumn)
                {
                    if (!ComponentUtils.isRendered(context, child))
                    {
                        // Column is not visible
                        continue;
                    }

                    for (int j = 0, columnChildCount = child.getChildCount(); j < columnChildCount; j++)
                    {
                        UIComponent columnChild = child.getChildren().get(j);
                        process(context, columnChild, processAction);
                    }
                }
            }
        }
    }

    private void process(FacesContext context, UIComponent component, int processAction)
    {
        switch (processAction)
        {
            case PROCESS_DECODES:
                component.processDecodes(context);
                break;
            case PROCESS_VALIDATORS:
                component.processValidators(context);
                break;
            case PROCESS_UPDATES:
                component.processUpdates(context);
                break;
            default:
                // do nothing
        }
    }

    /**
     * Return the datamodel for this table, potentially fetching the data from a backing bean via a value-binding if
     * this is the first time this method has been called.
     * <p>
     * This is complicated by the fact that this table may be nested within another table. In this case a different
     * datamodel should be fetched for each row. When nested within a parent table, the parent reference won't change
     * but parent.getContainerClientId() will, as the suffix changes
     * depending upon the current row index. A map object on this
     * component is therefore used to cache the datamodel for each row of the table. In the normal case where this table
     * is not nested inside a component that changes its id (like a table does) then this map only ever has one entry.
     */
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

    protected void setDataModel(DataModel dataModel)
    {
        String clientID = "";

        UIComponent parent = getParent();
        if (parent != null)
        {
            clientID = parent.getContainerClientId(getFacesContext());
        }
        if (dataModel == null)
        {
            _dataModelMap.remove(clientID);
        }
        else
        {
            _dataModelMap.put(clientID, dataModel);
        }
    }

    /**
     * Evaluate this object's value property and convert the result into a DataModel. Normally this object's value
     * property will be a value-binding which will cause the value to be fetched from some backing bean.
     * <p>
     * The result of fetching the value may be a DataModel object, in which case that object is returned directly. If
     * the value is of type List, Array, ResultSet, Result, other object or null then an appropriate wrapper is created
     * and returned.
     * <p>
     * Null is never returned by this method.
     */
    private DataModel createDataModel()
    {
        Object value = getValue();

        if (value == null)
        {
            return EMPTY_DATA_MODEL;
        }
        else if (value instanceof DataModel)
        {
            return (DataModel) value;
        }
        else
        {
            DataModel dataModel = null;
            if (FACES_DATA_MODEL_MANAGER_CLASS != null && value != null)
            {
                try
                {
                    dataModel = (DataModel) FACES_DATA_MODEL_MANAGER_CREATE_DATAMODEL_METHOD.invoke(null,
                            getFacesContext(), value.getClass(), value);
                }
                catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
                {
                    //No op
                }
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

    /**
     * An EL expression that specifies the data model that backs this table.
     * <p>
     * The value referenced by the EL expression can be of any type.
     * </p>
     * <ul>
     * <li>A value of type DataModel is used directly.</li>
     * <li>Array-like parameters of type array-of-Object, java.util.List or java.sql.ResultSet
     * are wrapped in a corresponding DataModel that knows how to iterate over the
     * elements.</li>
     * <li>Other values are wrapped in a DataModel as a single row.</li>
     * </ul>
     * <p>
     * Note in particular that unordered collections, eg Set are not supported. Therefore if the value expression
     * references such an object then the table will be considered to contain just one element - the collection itself.
     * </p>
     */
    @JSFProperty
    public Object getValue()
    {
        return getStateHelper().eval(PropertyKeys.value);
    }

    public void setValue(Object value)
    {
        getStateHelper().put(PropertyKeys.value, value);
        _dataModelMap.clear();
        _rowStates.clear();
        _isValidChilds = true;
    }

    /**
     * Defines the index of the first row to be displayed, starting from 0.
     */
    @JSFProperty
    public int getFirst()
    {
        return (Integer) getStateHelper().eval(PropertyKeys.first,0);
    }

    public void setFirst(int first)
    { 
        if (first < 0)
        {
            throw new IllegalArgumentException("Illegal value for first row: " + first);
        }
        getStateHelper().put(PropertyKeys.first, first );
    }

    /**
     * Defines the maximum number of rows of data to be displayed.
     * <p>
     * Specify zero to display all rows from the "first" row to the end of available data.
     * </p>
     */
    @JSFProperty
    public int getRows()
    {
        return (Integer) getStateHelper().eval(PropertyKeys.rows,0);
    }

    /**
     * Set the maximum number of rows displayed in the table.
     */
    public void setRows(int rows)
    {
        if (rows < 0)
        {
            throw new IllegalArgumentException("rows: " + rows);
        }
        getStateHelper().put(PropertyKeys.rows, rows ); 
    }

    /**
     * Defines the name of the request-scope variable that will hold the current row during iteration.
     * <p>
     * During rendering of child components of this UIData, the variable with this name can be read to learn what the
     * "rowData" object for the row currently being rendered is.
     * </p>
     * <p>
     * This value must be a static value, ie an EL expression is not permitted.
     * </p>
     */
    @JSFProperty(literalOnly = true)
    public String getVar()
    {
        return (String) getStateHelper().get(PropertyKeys.var);
    }

    /**
     * Overrides the behavior in 
     * UIComponent.visitTree(jakarta.faces.component.visit.VisitContext, jakarta.faces.component.visit.VisitCallback)
     * to handle iteration correctly.
     * 
     * @param context the visit context which handles the processing details
     * @param callback the callback to be performed
     * @return false if the processing is not done true if we can shortcut
     * the visiting because we are done with everything
     * 
     * @since 2.0
     */
    @Override
    public boolean visitTree(VisitContext context, VisitCallback callback)
    {
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

            boolean isCachedFacesContext = isCachedFacesContext();
            if (!isCachedFacesContext)
            {
                setCachedFacesContext(context.getFacesContext());
            }

            // save the current row index
            int oldRowIndex = getRowIndex();
            // set row index to -1 to process the facets and to get the rowless clientId
            setRowIndex(-1);
            try
            {
                VisitResult visitResult = context.invokeVisitCallback(this, callback);
                switch (visitResult)
                {
                    case COMPLETE:
                        //we are done nothing has to be processed anymore
                        return true;
                    case REJECT:
                        return false;
                    default:
                        // accept; determine if we need to visit our children
                        Collection<String> subtreeIdsToVisit = context.getSubtreeIdsToVisit(this);
                        boolean doVisitChildren = subtreeIdsToVisit != null && !subtreeIdsToVisit.isEmpty();
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

                            if (skipIterationHint)
                            {
                                // If SKIP_ITERATION is enabled, do not take into account rows.
                                for (int i = 0, childCount = getChildCount(); i < childCount; i++ )
                                {
                                    UIComponent child = getChildren().get(i);
                                    if (child.visitTree(context, callback))
                                    {
                                        return true;
                                    }
                                }
                            }
                            else
                            {
                                // visit every column directly without visiting its children
                                // (the children of every UIColumn will be visited later for
                                // every row) and also visit the column's facets
                                for (int i = 0, childCount = getChildCount(); i < childCount; i++)
                                {
                                    UIComponent child = getChildren().get(i);
                                    if (child instanceof UIColumn)
                                    {
                                        VisitResult columnResult = context.invokeVisitCallback(child, callback);
                                        if (columnResult == VisitResult.COMPLETE)
                                        {
                                            return true;
                                        }
                                        if (child.getFacetCount() > 0)
                                        {
                                            for (UIComponent facet : child.getFacets().values())
                                            {
                                                if (facet.visitTree(context, callback))
                                                {
                                                    return true;
                                                }
                                            }
                                        }
                                    }
                                }
                                // iterate over the rows
                                int rowsToProcess = getRows();
                                // if getRows() returns 0, all rows have to be processed
                                if (rowsToProcess == 0)
                                {
                                    rowsToProcess = getRowCount();
                                }
                                int rowIndex = getFirst();
                                for (int rowsProcessed = 0; rowsProcessed < rowsToProcess; rowsProcessed++, rowIndex++)
                                {
                                    setRowIndex(rowIndex);
                                    if (!isRowAvailable())
                                    {
                                        return false;
                                    }
                                    // visit the children of every child of the UIData that is an instance of UIColumn
                                    for (int i = 0, childCount = getChildCount(); i < childCount; i++)
                                    {
                                        UIComponent child = getChildren().get(i);
                                        if (child instanceof UIColumn)
                                        {
                                            for (int j = 0, grandChildCount = child.getChildCount();
                                                 j < grandChildCount; j++)
                                            {
                                                UIComponent grandchild = child.getChildren().get(j);
                                                if (grandchild.visitTree(context, callback))
                                                {
                                                    return true;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                }
            }
            finally
            {
                // restore the old row index
                setRowIndex(oldRowIndex);
                if (!isCachedFacesContext)
                {
                    setCachedFacesContext(null);
                }
            }
        }
        finally
        {
            // pop the component from EL
            popComponentFromEL(context.getFacesContext());
        }
        // Return false to allow the visiting to continue
        return false;
    }

    public void setVar(String var)
    {
        getStateHelper().put(PropertyKeys.var, var ); 
    }
    
    /**
     * Indicates whether the state for a component in each row should not be 
     * discarded before the datatable is rendered again.
     * 
     * This will only work reliable if the datamodel of the 
     * datatable did not change either by sorting, removing or 
     * adding rows. Default: false
     * 
     * @return
     */
    @JSFProperty(literalOnly=true, faceletsOnly=true)
    public boolean isRowStatePreserved()
    {
        Boolean b = (Boolean) getStateHelper().get(PropertyKeys.rowStatePreserved);
        return b == null ? false : b; 
    }
    
    public void setRowStatePreserved(boolean preserveComponentState)
    {
        getStateHelper().put(PropertyKeys.rowStatePreserved, preserveComponentState);
    }

    enum PropertyKeys
    {
        value,
        first,
        rows,
        var,
        uniqueIdCounter,
        rowStatePreserved
    }

    @Override
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }
}
