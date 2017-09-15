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
// PI64714      wtlucy          JSF MESSAGE SEVERITIES ALWAYS SET TO ERROR AFTER VALIDATOREXCEPTION
package org.apache.myfaces.view.facelets.compiler;

import java.util.Collection;

import javax.faces.application.FacesMessage;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIViewRoot;
import javax.faces.component.UniqueIdVendor;
import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.MethodBinding;
import javax.faces.el.ValueBinding;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

/**
 * A collection of static helper methods for locating UIComponents.
 * 
 * @author Manfred Geiler (latest modification by $Author: lu4242 $)
 * @version $Revision: 1542444 $ $Date: 2013-11-16 01:41:08 +0000 (Sat, 16 Nov 2013) $
 */
class _ComponentUtils
{
    private _ComponentUtils()
    {
    }

    static UIComponent findParentNamingContainer(UIComponent component, boolean returnRootIfNotFound)
    {
        UIComponent parent = component.getParent();
        if (returnRootIfNotFound && parent == null)
        {
            return component;
        }
        while (parent != null)
        {
            if (parent instanceof NamingContainer)
            {
                return parent;
            }
            if (returnRootIfNotFound)
            {
                UIComponent nextParent = parent.getParent();
                if (nextParent == null)
                {
                    return parent; // Root
                }
                parent = nextParent;
            }
            else
            {
                parent = parent.getParent();
            }
        }
        return null;
    }

    static UniqueIdVendor findParentUniqueIdVendor(UIComponent component)
    {
        UIComponent parent = component.getParent();

        while (parent != null)
        {
            if (parent instanceof UniqueIdVendor)
            {
                return (UniqueIdVendor) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }
    
    static UIComponent getRootComponent(UIComponent component)
    {
        UIComponent parent;
        for (;;)
        {
            parent = component.getParent();
            if (parent == null)
            {
                return component;
            }
            component = parent;
        }
    }

    /**
     * Find the component with the specified id starting from the specified component.
     * <p>
     * Param id must not contain any NamingContainer.SEPARATOR_CHAR characters (ie ":"). This method explicitly does
     * <i>not</i> search into any child naming container components; this is expected to be handled by the caller of
     * this method.
     * <p>
     * For an implementation of findComponent which does descend into child naming components, see
     * org.apache.myfaces.custom.util.ComponentUtils.
     * 
     * @return findBase, a descendant of findBase, or null.
     */
    static UIComponent findComponent(UIComponent findBase, String id, final char separatorChar)
    {
        if (!(findBase instanceof NamingContainer) && idsAreEqual(id, findBase))
        {
            return findBase;
        }

        int facetCount = findBase.getFacetCount();
        if (facetCount > 0)
        {
            for (UIComponent facet : findBase.getFacets().values())
            {
                if (!(facet instanceof NamingContainer))
                {
                    UIComponent find = findComponent(facet, id, separatorChar);
                    if (find != null)
                    {
                        return find;
                    }
                }
                else if (idsAreEqual(id, facet))
                {
                    return facet;
                }
            }
        }
        
        for (int i = 0, childCount = findBase.getChildCount(); i < childCount; i++)
        {
            UIComponent child = findBase.getChildren().get(i);
            if (!(child instanceof NamingContainer))
            {
                UIComponent find = findComponent(child, id, separatorChar);
                if (find != null)
                {
                    return find;
                }
            }
            else if (idsAreEqual(id, child))
            {
                return child;
            }
        }

        if (findBase instanceof NamingContainer && idsAreEqual(id, findBase))
        {
            return findBase;
        }

        return null;
    }
    
    static UIComponent findComponentChildOrFacetFrom(UIComponent parent, String id, String innerExpr)
    {
        if (parent.getFacetCount() > 0)
        {
            for (UIComponent facet : parent.getFacets().values())
            {
                if (id.equals(facet.getId()))
                {
                    if (innerExpr == null)
                    {
                        return facet;
                    }
                    else if (facet instanceof NamingContainer)
                    {
                        UIComponent find = facet.findComponent(innerExpr);
                        if (find != null)
                        {
                            return find;
                        }
                    }
                }
                else if (!(facet instanceof NamingContainer))
                {
                    UIComponent find = findComponentChildOrFacetFrom(facet, id, innerExpr);
                    if (find != null)
                    {
                        return find;
                    }
                }
            }
        }
        if (parent.getChildCount() > 0)
        {
            for (int i = 0, childCount = parent.getChildCount(); i < childCount; i++)
            {
                UIComponent child = parent.getChildren().get(i);
                if (id.equals(child.getId()))
                {
                    if (innerExpr == null)
                    {
                        return child;
                    }
                    else if (child instanceof NamingContainer)
                    {
                        UIComponent find = child.findComponent(innerExpr);
                        if (find != null)
                        {
                            return find;
                        }
                    }
                }
                else if (!(child instanceof NamingContainer))
                {
                    UIComponent find = findComponentChildOrFacetFrom(child, id, innerExpr);
                    if (find != null)
                    {
                        return find;
                    }
                }
            }
        }
        return null;
    }

    /*
     * Return true if the specified component matches the provided id. This needs some quirks to handle components whose
     * id value gets dynamically "tweaked", eg a UIData component whose id gets the current row index appended to it.
     */
    private static boolean idsAreEqual(String id, UIComponent cmp)
    {
        if (id.equals(cmp.getId()))
        {
            return true;
        }

        /* By the spec, findComponent algorithm does not take into account UIData.rowIndex() property,
         * because it just scan over nested plain ids. 
        if (cmp instanceof UIData)
        {
            UIData uiData = ((UIData)cmp);

            if (uiData.getRowIndex() == -1)
            {
                return dynamicIdIsEqual(id, cmp.getId());
            }
            return id.equals(cmp.getId() + separatorChar + uiData.getRowIndex());
        }
        */

        return false;
    }

    static void callValidators(FacesContext context, UIInput input, Object convertedValue)
    {
        // first invoke the list of validator components
        Validator[] validators = input.getValidators();
        for (int i = 0; i < validators.length; i++)
        {
            Validator validator = validators[i];
            try
            {
                validator.validate(context, input, convertedValue);
            }
            catch (ValidatorException e)
            {
                input.setValid(false);

                String validatorMessage = input.getValidatorMessage();
                if (validatorMessage != null)
                {
                    context.addMessage(input.getClientId(context), new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        validatorMessage, validatorMessage));
                }
                else
                {
                    FacesMessage facesMessage = e.getFacesMessage();
                    if (facesMessage != null)
                    {
                        context.addMessage(input.getClientId(context), facesMessage);
                    }
                    Collection<FacesMessage> facesMessages = e.getFacesMessages();
                    if (facesMessages != null)
                    {
                        for (FacesMessage message : facesMessages)
                        {
                            context.addMessage(input.getClientId(context), message);
                        }
                    }
                }
            }
        }

        // now invoke the validator method defined as a method-binding attribute
        // on the component
        MethodBinding validatorBinding = input.getValidator();
        if (validatorBinding != null)
        {
            try
            {
                validatorBinding.invoke(context, new Object[] { context, input, convertedValue });
            }
            catch (EvaluationException e)
            {
                input.setValid(false);
                Throwable cause = e.getCause();
                if (cause instanceof ValidatorException)
                {
                    String validatorMessage = input.getValidatorMessage();
                    if (validatorMessage != null)
                    {
                        context.addMessage(input.getClientId(context), new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            validatorMessage, validatorMessage));
                    }
                    else
                    {
                        FacesMessage facesMessage = ((ValidatorException)cause).getFacesMessage();
                        if (facesMessage != null)
                        {
                            context.addMessage(input.getClientId(context), facesMessage);
                        }
                        Collection<FacesMessage> facesMessages = ((ValidatorException)cause).getFacesMessages();
                        if (facesMessages != null)
                        {
                            for (FacesMessage message : facesMessages)
                            {
                                context.addMessage(input.getClientId(context), message);
                            }
                        }
                    }
                }
                else
                {
                    throw e;
                }
            }
        }
    }

    static String getStringValue(FacesContext context, ValueBinding vb)
    {
        Object value = vb.getValue(context);
        if (value == null)
        {
            return null;
        }
        return value.toString();
    }

    /*
    @SuppressWarnings("unchecked")
    static <T> T getExpressionValue(UIComponent component, String attribute, T overrideValue, T defaultValue)
    {
        if (overrideValue != null)
        {
            return overrideValue;
        }
        ValueExpression ve = component.getValueExpression(attribute);
        if (ve != null)
        {
            return (T)ve.getValue(component.getFacesContext().getELContext());
        }
        return defaultValue;
    }*/

    static String getPathToComponent(UIComponent component)
    {
        StringBuffer buf = new StringBuffer();

        if (component == null)
        {
            buf.append("{Component-Path : ");
            buf.append("[null]}");
            return buf.toString();
        }

        getPathToComponent(component, buf);

        buf.insert(0, "{Component-Path : ");
        buf.append("}");

        return buf.toString();
    }
    
    /**
     * Call {@link UIComponent#pushComponentToEL(javax.faces.context.FacesContext,javax.faces.component.UIComponent)},
     * reads the isRendered property, call {@link
     * UIComponent#popComponentFromEL} and returns the value of isRendered.
     */
    static boolean isRendered(FacesContext facesContext, UIComponent uiComponent)
    {
        // We must call pushComponentToEL here because ValueExpression may have 
        // implicit object "component" used. 
        try
        {
            uiComponent.pushComponentToEL(facesContext, uiComponent);
            return uiComponent.isRendered();
        }
        finally
        {       
            uiComponent.popComponentFromEL(facesContext);
        }
    }

    private static void getPathToComponent(UIComponent component, StringBuffer buf)
    {
        if (component == null)
        {
            return;
        }

        StringBuffer intBuf = new StringBuffer();

        intBuf.append("[Class: ");
        intBuf.append(component.getClass().getName());
        if (component instanceof UIViewRoot)
        {
            intBuf.append(",ViewId: ");
            intBuf.append(((UIViewRoot)component).getViewId());
        }
        else
        {
            intBuf.append(",Id: ");
            intBuf.append(component.getId());
        }
        intBuf.append("]");

        buf.insert(0, intBuf.toString());

        getPathToComponent(component.getParent(), buf);
    }
}
