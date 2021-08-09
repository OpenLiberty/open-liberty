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
package org.apache.myfaces.view.facelets;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.faces.FactoryFinder;

import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.component.UniqueIdVendor;
import javax.faces.component.visit.VisitContextFactory;
import javax.faces.context.FacesContext;
import javax.faces.view.AttachedObjectHandler;
import javax.faces.view.EditableValueHolderAttachedObjectHandler;
import javax.faces.view.facelets.FaceletContext;

/**
 * @since 2.0.1
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 1641481 $ $Date: 2014-11-24 21:42:00 +0000 (Mon, 24 Nov 2014) $
 */
abstract public class FaceletCompositionContext
{
    static protected final String FACELET_COMPOSITION_CONTEXT_KEY = "oam.facelets.FACELET_COMPOSITION_CONTEXT";

    protected FaceletCompositionContext()
    {
    }
    
    static public FaceletCompositionContext getCurrentInstance()
    {
        return (FaceletCompositionContext)
                FacesContext.getCurrentInstance().getAttributes().get(FACELET_COMPOSITION_CONTEXT_KEY);
    }
    
    static public FaceletCompositionContext getCurrentInstance(FaceletContext ctx)
    {
        if (ctx instanceof AbstractFaceletContext)
        {
            return ((AbstractFaceletContext)ctx).getFaceletCompositionContext();
        }
        else
        {
            // Here we have two choices: retrieve it throught ThreadLocal var
            // or use the attribute value on FacesContext, but it seems better
            // use the FacesContext attribute map.
            return (FaceletCompositionContext)
                    ctx.getFacesContext().getAttributes().get(FACELET_COMPOSITION_CONTEXT_KEY);
        }
    }
    
    static public FaceletCompositionContext getCurrentInstance(FacesContext ctx)
    {
        return (FaceletCompositionContext) ctx.getAttributes().get(FACELET_COMPOSITION_CONTEXT_KEY);
    }

    public void init(FacesContext facesContext)
    {
        facesContext.getAttributes().put(
                FaceletCompositionContext.FACELET_COMPOSITION_CONTEXT_KEY, this);
    }

    /**
     * Releases the MyFaceletContext object.  This method must only
     * be called by the code that created the MyFaceletContext.
     */
    public void release(FacesContext facesContext)
    {
        facesContext.getAttributes().remove(FACELET_COMPOSITION_CONTEXT_KEY);
    }
    
    public abstract FaceletFactory getFaceletFactory();
    
    /**
     * Return the composite component being applied on the current facelet. 
     * 
     * Note this is different to UIComponent.getCurrentCompositeComponent, because a composite
     * component is added to the stack each time a composite:implementation tag handler is applied.
     * 
     * This could be used by InsertChildrenHandler and InsertFacetHandler to retrieve the current
     * composite component to be applied.
     * 
     * @since 2.0.1
     * @return
     */
    public abstract UIComponent getCompositeComponentFromStack();

    /**
     * @since 2.0.1
     * @param parent
     */
    public abstract void pushCompositeComponentToStack(UIComponent parent);

    /**
     * @since 2.0.1
     */
    public abstract void popCompositeComponentToStack();
    
    /**
     * Return the latest UniqueIdVendor created from stack. The reason why we need to keep
     * a UniqueIdVendor stack is because we need to look the closest one in ComponentTagHandlerDelegate.
     * Note that facelets tree is built from leafs to root, that means use UIComponent.getParent() does not
     * always return parent components.
     * 
     * @since 2.0.1
     * @return
     */
    public abstract UniqueIdVendor getUniqueIdVendorFromStack();

    /**
     * @since 2.0.1
     * @param parent
     */
    public abstract void pushUniqueIdVendorToStack(UniqueIdVendor parent);

    /**
     * @since 2.0.1
     */
    public abstract void popUniqueIdVendorToStack();
    
    /**
     * Gets the top of the validationGroups stack.
     * @return
     * @since 2.0.1
     */
    @Deprecated
    public abstract String getFirstValidationGroupFromStack();
    
    /**
     * Removes top of stack.
     * @since 2.0.1
     */
    @Deprecated
    public abstract void popValidationGroupsToStack();
    
    /**
     * Pushes validationGroups to the stack.
     * @param validationGroups
     * @since 2.0.1
     */
    @Deprecated
    public abstract void pushValidationGroupsToStack(String validationGroups);
    
    /**
     * Gets all validationIds on the stack.
     * @return
     * @since 2.0.1
     */
    @Deprecated
    public abstract Iterator<String> getExcludedValidatorIds();
    
    /**
     * Removes top of stack.
     * @since 2.0.1
     */
    @Deprecated
    public abstract void popExcludedValidatorIdToStack();
    
    /**
     * Pushes validatorId to the stack of excluded validatorIds.
     * @param validatorId
     * @since 2.0.1
     */
    @Deprecated
    public abstract void pushExcludedValidatorIdToStack(String validatorId);
    
    /**
     * Gets all validationIds on the stack.
     * @return
     * @since 2.0.1
     */
    @Deprecated
    public abstract Iterator<String> getEnclosingValidatorIds();
    
    /**
     * Removes top of stack.
     * @since 2.0.1
     */
    public abstract void popEnclosingValidatorIdToStack();
    
    /**
     * Pushes validatorId to the stack of all enclosing validatorIds.
     * @param validatorId
     * @since 2.0.1
     */
    @Deprecated
    public abstract void pushEnclosingValidatorIdToStack(String validatorId);
    
    /**
     * Pushes validatorId to the stack of all enclosing validatorIds.
     * 
     * @param validatorId
     * @param attachedObjectHandler
     * @since 2.0.10
     */
    public abstract void pushEnclosingValidatorIdToStack(String validatorId, 
            EditableValueHolderAttachedObjectHandler attachedObjectHandler);

    /**
     * Gets all validationIds with its associated EditableValueHolderAttachedObjectHandler from the stack.
     * 
     * @return
     * @since 2.0.10
     */
    public abstract Iterator<Map.Entry<String, EditableValueHolderAttachedObjectHandler>> 
        getEnclosingValidatorIdsAndHandlers();
    
    /**
     * 
     * @param id
     * @return
     * @since 2.0.10
     */
    public abstract boolean containsEnclosingValidatorId(String id);
    
    /**
     * Check if this build is being refreshed, adding transient components
     * and adding/removing components under c:if or c:forEach or not.
     * 
     * @return
     * @since 2.0.1
     */
    public abstract boolean isRefreshingTransientBuild();
    
    /**
     * Check if this build should be marked as initial state. In other words,
     * all components must call UIComponent.markInitialState.
     * 
     * @return
     * @since 2.0.1
     */
    public abstract boolean isMarkInitialState();
    
    public void setMarkInitialState(boolean value)
    {
    }
    
    /**
     * Check if the current view will be refreshed with partial state saving.
     * 
     * This param is used in two posible events:
     * 
     * 1. To notify UIInstruction instances to look for instances moved by
     *    cc:insertChildren or cc:insertFacet.
     * 2. To do proper actions when a tag that could change tree structure is applied
     *    (c:if, c:forEach...)
     * 
     * @return
     * @since 2.0.1
     */
    public abstract boolean isRefreshTransientBuildOnPSS();
    
    /**
     * 
     * @since 2.0.12, 2.1.6
     * @return
     */
    public boolean isRefreshTransientBuildOnPSSPreserveState()
    {
        return false;
    }
    
    /**
     * Check if we are using partial state saving on this view
     * 
     * @return
     * @since 2.0.1
     */
    public abstract boolean isUsingPSSOnThisView();
    
    /**
     * @since 2.0.1
     * @return
     */
    public abstract boolean isMarkInitialStateAndIsRefreshTransientBuildOnPSS();

    /**
     * Add to the composite component parent this handler, so it will be processed later when
     * ViewDeclarationLanguage.retargetAttachedObjects is called.
     *
     * Tag Handlers exposing attached objects should call this method to expose them when the
     * parent to be applied is a composite components.
     *
     * @since 2.0.2
     * @param compositeComponentParent
     * @param handler
     */
    public abstract void addAttachedObjectHandler(UIComponent compositeComponentParent, AttachedObjectHandler handler);

    /**
     * Remove from the composite component parent the list of attached handlers.
     * 
     * @since 2.0.2
     * @param compositeComponentParent
     */
    public abstract void removeAttachedObjectHandlers(UIComponent compositeComponentParent);

    /**
     * Retrieve the list of object handlers attached to a composite component parent. 
     * 
     * @since 2.0.2
     * @param compositeComponentParent
     */
    public abstract List<AttachedObjectHandler> getAttachedObjectHandlers(UIComponent compositeComponentParent);

    /**
     * Marks all direct children and Facets with an attribute for deletion.
     *
     * @since 2.0.2
     * @see #finalizeForDeletion(UIComponent)
     * @param component
     *            UIComponent to mark
     */
    public abstract void markForDeletion(UIComponent component);
    
    /**
     * Used in conjunction with markForDeletion where any UIComponent marked will be removed.
     * 
     * @since 2.0.2
     * @param component
     *            UIComponent to finalize
     */
    public abstract void finalizeForDeletion(UIComponent component);
    
    public void removeComponentForDeletion(UIComponent component)
    {
    }

    /**
     * Marks the given resource for deletion. Is to be used for relocatable 
     * components instead of {@link #markForDeletion(UIComponent)}.
     *
     * @since 2.0.17 2.1.11
     * @param component
     *            UIComponent to finalize
     */
    public void markRelocatableResourceForDeletion(UIComponent component)
    {
    }

    /**
     * Used to clean up all unused relocatable components on the root component.
     *
     * @since 2.0.17 2.1.11
     * @param component
     *            UIComponent to finalize (root component)
     */
    public void finalizeRelocatableResourcesForDeletion(UIViewRoot root)
    {
    }

    /**
     * Add a method expression as targeted for the provided composite component
     * 
     * @since 2.0.3
     * @param targetedComponent
     * @param attributeName
     * @param backingValue A value that could be useful to revert its effects.
     */
    public abstract void addMethodExpressionTargeted(UIComponent targetedComponent, String attributeName,
                                                     Object backingValue);

    /**
     * Check if the MethodExpression attribute has been applied using vdl.retargetMethodExpression 
     * 
     * @since 2.0.3
     * @param compositeComponentParent
     * @param attributeName
     * @return
     */
    public abstract boolean isMethodExpressionAttributeApplied(UIComponent compositeComponentParent,
                                                               String attributeName);
    
    /**
     * Mark the MethodExpression attribute as applied using vdl.retargetMethodExpression
     * 
     * @since 2.0.3
     * @param compositeComponentParent
     * @param attributeName
     */
    public abstract void markMethodExpressionAttribute(UIComponent compositeComponentParent, String attributeName);
    
    /**
     * Clear the MethodExpression attribute to call vdl.retargetMethodExpression again
     * 
     * @since 2.0.3
     * @param compositeComponentParent
     * @param attributeName
     */
    public abstract void clearMethodExpressionAttribute(UIComponent compositeComponentParent, String attributeName);
    
    /**
     * Remove a method expression as targeted for the provided composite component
     * 
     * @since 2.0.3
     * @param targetedComponent
     * @param attributeName
     * @return A value that could be useful to revert its effects.
     */
    public abstract Object removeMethodExpressionTargeted(UIComponent targetedComponent, String attributeName);
    
    /**
     * Indicates if a EL Expression can be or not cached by facelets vdl.
     * 
     * @since 2.0.8
     * @return
     */
    public ELExpressionCacheMode getELExpressionCacheMode()
    {
        return ELExpressionCacheMode.noCache;
    }
    
    /**
     * 
     * @since 2.0.9
     * @return
     */
    public boolean isWrapTagExceptionsAsContextAware()
    {
        return true;
    }

    /**
     * Start a new unique id section, which means a new counter is used to
     * generate unique ids to components
     * 
     * @since 2.0.10, 2.1.4
     * @return
     */
    public String startComponentUniqueIdSection()
    {
        return null;
    }
    
    /**
     * Start a new unique id section, which means a new counter is used to
     * generate unique ids to components, but appending a base to the
     * new counter.
     * 
     * @since 2.2.0
     * @return
     */
    public String startComponentUniqueIdSection(String base)
    {
        return null;
    }
    
    /**
     * @since 2.2.0
     * @param base 
     */
    public void endComponentUniqueIdSection(String base)
    {
    }
    
    /**
     * Generate a unique id that will be used later to derive a unique id per tag
     * by FaceletContext.generateUniqueId(). This generator ensures uniqueness per
     * view but FaceletContext.generateUniqueId() ensures uniqueness per view and
     * per facelet hierarchy, so different included facelets will generate different
     * ids.
     * 
     * @return
     */
    public String generateUniqueId()
    {
        return null;
    }
    
    public void generateUniqueId(StringBuilder builderToAdd)
    {
    }
    
    /**
     * Generate a unique id for component instances. 
     * 
     * @return
     */
    public String generateUniqueComponentId()
    {
        return null;
    }

    /**
     * Ends the current unique id section, so the previous counter will be used
     * to generate unique ids to components.
     */
    public void endComponentUniqueIdSection()
    {
    }
    
    /**
     * Set the iterator used to retrieve unique ids.
     * 
     * since 2.1.7, 2.0.13
     * @param uniqueIdsIterator 
     */
    public void setUniqueIdsIterator(Iterator<String> uniqueIdsIterator)
    {
    }
    
    /**
     * Activater record unique id mode, so an structure will be
     * used to hold those values.
     * 
     * since 2.1.7, 2.0.13
     */
    public void initUniqueIdRecording()
    {
    }
    
    /**
     * Add an unique id to the list if recording is enabled,
     * if recording is not enabled it has no effect.
     * 
     * since 2.1.7, 2.0.13
     * @param uniqueId 
     */
    public void addUniqueId(String uniqueId)
    {
    }
    
    /**
     * Return the unique id from the iterator if applies
     * 
     * since 2.1.7, 2.0.13
     * @return 
     */
    public String getUniqueIdFromIterator()
    {
        return null;
    }
    
    /**
     * Return the list of unique ids
     * 
     * since 2.1.7, 2.0.13
     * @return 
     */
    public List<String> getUniqueIdList()
    {
        return null;
    }

    /**
     * Increment the unique id without construct it.
     * 
     * since 2.1.7, 2.0.13
     * @return 
     */
    public void incrementUniqueId()
    {
    }

    /**
     * Check if the facelet is building view metadata
     * 
     * since 2.1.7, 2.0.13
     * @return 
     */
    public boolean isBuildingViewMetadata()
    {
        return FaceletViewDeclarationLanguage.isBuildingViewMetadata(
                FacesContext.getCurrentInstance());
    }
    
    /**
     * Call this method to indicate a f:metadata section is about to be processed
     * 
     * since 2.1.7, 2.0.13
     * @return 
     */
    public void startMetadataSection()
    {
    }
    
    /**
     * Call this method to indicate f:metadata section has been already processed
     * 
     * since 2.1.7, 2.0.13
     * @return 
     */
    public void endMetadataSection()
    {
    }
    
    /**
     * Check if the component is created inside f:metadata section
     * 
     * since 2.1.7, 2.0.13
     * @return 
     */
    public boolean isInMetadataSection()
    {
       return false;
    }
    
    /**
     * Check if the section to be processed is being refreshed.
     * 
     * since 2.1.7, 2.0.13
     * @return 
     */
    public boolean isRefreshingSection()
    {
       return isRefreshingTransientBuild() ||  (!isBuildingViewMetadata() && isInMetadataSection());
    }

    /**
     * 
     * @since 2.1.8, 2.0.14
     */
    public void incrementUniqueComponentId()
    {
    }
    
    public StringBuilder getSharedStringBuilder()
    {
        return new StringBuilder();
    }

    /**
     * Returns the current nesting level of composite components found. If
     * no composite component has been used returns 0.
     * 
     * @since 2.1.9, 2.0.15
     */
    public int getCompositeComponentLevel()
    {
        return 0;
    }
    
    public boolean isDynamicCompositeComponentHandler()
    {
        return false;
    }
    
    public void setDynamicCompositeComponentHandler(boolean value)
    {
    }
    
    public void pushDynamicComponentSection(String baseKey)
    {
    }
    
    public void popDynamicComponentSection()
    {
    }
    
    /**
     * Check if the algorithm is applying a dynamic component and the current component
     * is the top level one. This is necessary to avoid remove/add the component under
     * a refresh and in that way change the position of the component in the tree. Remember
     * facelets algorithm removes/add components to sort the components under a refresh, but
     * in this case, it is up to the user to put the component in the tree, so the idea is
     * do not make interference with the user's code. Note if the dynamic content is wrapped
     * by a generated panel, the top level component is the wrapper itself, which has no 
     * attached tag handler.
     * 
     * @since 2.2
     * @return 
     */
    public boolean isDynamicComponentTopLevel()
    {
        return false;
    }
    
    public void setDynamicComponentTopLevel(boolean value)
    {
    }
    
    /**
     * Indicate if the current facelet section is a dynamic component section,
     * which means it was added to the component tree using vdl.createComponent(...);
     * 
     * @since 2.2
     * @return 
     */
    public boolean isDynamicComponentSection()
    {
        return false;
    }
    
    public UIViewRoot getViewRoot(FacesContext facesContext)
    {
        return facesContext.getViewRoot();
    }
    
    public void setViewRoot(UIViewRoot root)
    {
    }
    
    public VisitContextFactory getVisitContextFactory()
    {
        return (VisitContextFactory)FactoryFinder.getFactory(FactoryFinder.VISIT_CONTEXT_FACTORY);
    }
}
