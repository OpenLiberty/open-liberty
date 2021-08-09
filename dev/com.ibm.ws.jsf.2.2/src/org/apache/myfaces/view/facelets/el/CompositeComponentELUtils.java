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
package org.apache.myfaces.view.facelets.el;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.view.Location;

/**
 * Utility class for composite components when used in EL Expressions --> #{cc}
 * 
 * @author Jakob Korherr (latest modification by $Author: lu4242 $)
 * @version $Revision: 1520154 $ $Date: 2013-09-04 22:15:38 +0000 (Wed, 04 Sep 2013) $
 */
public final class CompositeComponentELUtils
{
    
    /**
     * The key under which the component stack is stored in the FacesContext.
     * ATTENTION: this constant is duplicate in UIComponent.
     */
    //public static final String COMPONENT_STACK = "componentStack:" + UIComponent.class.getName();
    
    /**
     * The key under which the current composite component is stored in the attribute
     * map of the FacesContext.
     */
    public static final String CURRENT_COMPOSITE_COMPONENT_KEY = "org.apache.myfaces.compositecomponent.current";
    
    /**
     * The key under which the Location of the composite componente is stored
     * in the attributes map of the component by InterfaceHandler.
     */
    public static final String LOCATION_KEY = "org.apache.myfaces.compositecomponent.location";

    /**
     * Indicates the nesting level where the composite component was created, working as reference
     * point to all EL expressions created in that point from Facelets engine.
     */
    public static final String LEVEL_KEY = "oam.cc.ccLevel";
    
    /**
     * A regular expression used to determine if cc is used in an expression String.
     */
    public static final Pattern CC_EXPRESSION_REGEX = Pattern.compile(".*[^\\w\\.]cc[^\\w].*");
    
    /**
     * A regular expression used to determine if cc.attrs is used as a method expression
     * in an expression String. This means cc.attrs must occur, must stand before a '(',
     * because otherwise it would be a method parameter (EL 2.2), and there must be no '.' after
     * cc.attrs unless there is a left parenthesis before it (e.g. #{cc.attrs.method(bean.parameter)}).
     * 
     * Explanation of the parts:
     * - [^\\(]* - There can be any character except a '(' before cc.attrs
     * - [^\\w\\.\\(] - There must be no word character, dot, or left parenthesis directly before cc.attrs
     * - cc\\.attrs\\. - "cc.attrs." must occur
     * - [^\\.]* - There must be no dot after cc.attrs to indicate a method invocation on cc.attrs
     * - (\\(.*)? - If there is a left paranthesis after cc.attrs, a dot is allowed again
     */
    public static final Pattern CC_ATTRS_METHOD_EXPRESSION_REGEX
            = Pattern.compile("[^\\(]*[^\\w\\.\\(]cc\\.attrs\\.[^\\.]*(\\(.*)?");
    
    private static final String CC = "cc";
    
    private static final String CC_ATTRS = "cc.attrs";
    
    public static final String CC_FIND_COMPONENT_EXPRESSION = "oam.CC_FIND_COMPONENT_EXPRESSION";
    
    /**
     * private constructor
     */
    private CompositeComponentELUtils()
    {
        // no instantiation of this class
    }
    
    /**
     * Try to find a composite component on the composite component stack
     * and using UIComponent.getCurrentCompositeComponent() based on the 
     * location of the facelet page that generated the composite component.
     * @param facesContext
     * @param location
     * @return
     */
    public static UIComponent getCompositeComponentBasedOnLocation(final FacesContext facesContext, 
            final Location location)
    {
        //1 Use getCurrentComponent and getCurrentCompositeComponent to look on the component stack
        UIComponent currentCompositeComponent = UIComponent.getCurrentCompositeComponent(facesContext);
        
        //1.1 Use getCurrentCompositeComponent first!
        if (currentCompositeComponent != null)
        {
            Location componentLocation = (Location) currentCompositeComponent.getAttributes().get(LOCATION_KEY);
            if (componentLocation != null 
                    && componentLocation.getPath().equals(location.getPath()))
            {
                return currentCompositeComponent;
            }
        }

        UIComponent currentComponent = UIComponent.getCurrentComponent(facesContext);
        
        if (currentComponent == null)
        {
            // Cannot found any component, because we don't have any reference!
            return null;
        }

        //2. Look on the stack using a recursive algorithm.
        UIComponent matchingCompositeComponent
                = lookForCompositeComponentOnStack(facesContext, location, currentComponent);
        
        if (matchingCompositeComponent != null)
        {
            return matchingCompositeComponent;
        }
        
        //2. Try to find it using UIComponent.getCurrentCompositeComponent(). 
        // This one will look the direct parent hierarchy of the component,
        // to see if the composite component can be found.
        if (currentCompositeComponent != null)
        {
            currentComponent = currentCompositeComponent;
        }
        else
        {
            //Try to find the composite component looking directly the parent
            //ancestor of the current component
            //currentComponent = UIComponent.getCurrentComponent(facesContext);
            boolean found = false;
            while (currentComponent != null && !found)
            {
                String findComponentExpr = (String) currentComponent.getAttributes().get(CC_FIND_COMPONENT_EXPRESSION);
                if (findComponentExpr != null)
                {
                    UIComponent foundComponent = facesContext.getViewRoot().findComponent(findComponentExpr);
                    if (foundComponent != null)
                    {
                        Location foundComponentLocation = (Location) currentComponent.getAttributes().get(LOCATION_KEY);
                        if (foundComponentLocation != null 
                                && foundComponentLocation.getPath().equals(location.getPath()))
                        {
                            return foundComponent;
                        }
                        else
                        {
                            while (foundComponent != null)
                            {
                                Location componentLocation
                                        = (Location) foundComponent.getAttributes().get(LOCATION_KEY);
                                if (componentLocation != null 
                                        && componentLocation.getPath().equals(location.getPath()))
                                {
                                    return foundComponent;
                                }
                                // get the composite component's parent
                                foundComponent = UIComponent.getCompositeComponentParent(foundComponent);
                            }
                        }
                    }
                }

                if (UIComponent.isCompositeComponent(currentComponent))
                {
                    found = true;
                }
                else
                {
                    currentComponent = currentComponent.getParent();
                }
            }
        }
        
        //if currentComponent != null means we have a composite component that we can check
        //Use UIComponent.getCompositeComponentParent() to traverse here.
        while (currentComponent != null)
        {
            Location componentLocation = (Location) currentComponent.getAttributes().get(LOCATION_KEY);
            if (componentLocation != null 
                    && componentLocation.getPath().equals(location.getPath()))
            {
                return currentComponent;
            }
            // get the composite component's parent
            currentComponent = UIComponent.getCompositeComponentParent(currentComponent);
        }
        
        // not found
        return null;
    }
    
    private static UIComponent lookForCompositeComponentOnStack(final FacesContext facesContext,
                                                                final Location location,
                                                                UIComponent currentComponent)
    {
        if (UIComponent.isCompositeComponent(currentComponent))
        {
            Location componentLocation = (Location) currentComponent.getAttributes().get(LOCATION_KEY);
            if (componentLocation != null 
                    && componentLocation.getPath().equals(location.getPath()))
            {
                return currentComponent;
            }
        }
        currentComponent.popComponentFromEL(facesContext);
        try
        {
            UIComponent c = UIComponent.getCurrentComponent(facesContext);
            if (c != null)
            {
                return lookForCompositeComponentOnStack( facesContext, location, c);
            }
            else
            {
                return null;
            }
        }
        finally
        {
            currentComponent.pushComponentToEL(facesContext, currentComponent);
        }
    }

    private static int getCCLevel(UIComponent component)
    {
        Integer ccLevel = (Integer) component.getAttributes().get(LEVEL_KEY);
        if (ccLevel == null)
        {
            return 0;
        }
        return ccLevel.intValue();
    }
    
    public static UIComponent getCompositeComponentBasedOnLocation(final FacesContext facesContext, 
            UIComponent baseComponent, final Location location)
    {
        UIComponent currentComponent = baseComponent;
        while (currentComponent != null)
        {
            Location componentLocation = (Location) currentComponent.getAttributes().get(
                LOCATION_KEY);
            if (componentLocation != null 
                    && componentLocation.getPath().equals(location.getPath()))
            {
                return currentComponent;
            }
            // get the composite component's parent
            currentComponent = UIComponent.getCompositeComponentParent(currentComponent);
        }
        return null;
    }

    
    /**
     * Same as getCompositeComponentBasedOnLocation(final FacesContext facesContext, final Location location),
     * but takes into account the ccLevel to resolve the composite component. 
     * 
     * @param facesContext
     * @param location
     * @param ccLevel
     * @return 
     */
    public static UIComponent getCompositeComponentBasedOnLocation(final FacesContext facesContext, 
            final Location location, int ccLevel)
    {
        //1 Use getCurrentComponent and getCurrentCompositeComponent to look on the component stack
        UIComponent currentCompositeComponent = UIComponent.getCurrentCompositeComponent(facesContext);
        
        //1.1 Use getCurrentCompositeComponent first!
        if (currentCompositeComponent != null)
        {
            Location componentLocation = (Location) currentCompositeComponent.getAttributes().get(LOCATION_KEY);
            if (componentLocation != null 
                    && componentLocation.getPath().equals(location.getPath()) && 
                    (ccLevel == getCCLevel(currentCompositeComponent)) )
            {
                return currentCompositeComponent;
            }
        }

        UIComponent currentComponent = UIComponent.getCurrentComponent(facesContext);
        
        if (currentComponent == null)
        {
            // Cannot found any component, because we don't have any reference!
            return null;
        }
        
        //2. Look on the stack using a recursive algorithm.
        UIComponent matchingCompositeComponent
                = lookForCompositeComponentOnStack(facesContext, location, ccLevel, currentComponent);
        
        if (matchingCompositeComponent != null)
        {
            return matchingCompositeComponent;
        }
        
        //2. Try to find it using UIComponent.getCurrentCompositeComponent(). 
        // This one will look the direct parent hierarchy of the component,
        // to see if the composite component can be found.
        if (currentCompositeComponent != null)
        {
            currentComponent = currentCompositeComponent;
        }
        else
        {
            //Try to find the composite component looking directly the parent
            //ancestor of the current component
            //currentComponent = UIComponent.getCurrentComponent(facesContext);
            boolean found = false;
            while (currentComponent != null && !found)
            {
                String findComponentExpr = (String) currentComponent.getAttributes().get(CC_FIND_COMPONENT_EXPRESSION);
                if (findComponentExpr != null)
                {
                    UIComponent foundComponent = facesContext.getViewRoot().findComponent(findComponentExpr);
                    if (foundComponent != null)
                    {
                        Location foundComponentLocation = (Location) currentComponent.getAttributes().get(LOCATION_KEY);
                        if (foundComponentLocation != null 
                                && foundComponentLocation.getPath().equals(location.getPath()) &&
                                ccLevel == getCCLevel(foundComponent))
                        {
                            return foundComponent;
                        }
                        else
                        {
                            while (foundComponent != null)
                            {
                                Location componentLocation
                                        = (Location) foundComponent.getAttributes().get(LOCATION_KEY);
                                if (componentLocation != null 
                                        && componentLocation.getPath().equals(location.getPath()) &&
                                        ccLevel == getCCLevel(foundComponent))
                                {
                                    return foundComponent;
                                }
                                // get the composite component's parent
                                foundComponent = UIComponent.getCompositeComponentParent(foundComponent);
                            }
                        }
                    }
                }

                if (UIComponent.isCompositeComponent(currentComponent))
                {
                    found = true;
                }
                else
                {
                    currentComponent = currentComponent.getParent();
                }
            }
        }
        
        //if currentComponent != null means we have a composite component that we can check
        //Use UIComponent.getCompositeComponentParent() to traverse here.
        while (currentComponent != null)
        {
            Location componentLocation = (Location) currentComponent.getAttributes().get(LOCATION_KEY);
            if (componentLocation != null 
                    && componentLocation.getPath().equals(location.getPath()) &&
                    ccLevel == getCCLevel(currentComponent))
            {
                return currentComponent;
            }
            // get the composite component's parent
            currentComponent = UIComponent.getCompositeComponentParent(currentComponent);
        }
        
        // not found
        return null;
    }

    private static UIComponent lookForCompositeComponentOnStack(final FacesContext facesContext,
                                                                final Location location, int ccLevel,
                                                                UIComponent currentComponent)
    {
        if (UIComponent.isCompositeComponent(currentComponent))
        {
            Location componentLocation = (Location) currentComponent.getAttributes().get(LOCATION_KEY);
            if (componentLocation != null 
                    && componentLocation.getPath().equals(location.getPath()) &&
                    (ccLevel == getCCLevel(currentComponent)) )
            {
                return currentComponent;
            }
        }
        currentComponent.popComponentFromEL(facesContext);
        try
        {
            UIComponent c = UIComponent.getCurrentComponent(facesContext);
            if (c != null)
            {
                return lookForCompositeComponentOnStack( facesContext, location, ccLevel, c);
            }
            else
            {
                return null;
            }
        }
        finally
        {
            currentComponent.pushComponentToEL(facesContext, currentComponent);
        }
    }
    
    /**
     * Trys to get the composite component using getCompositeComponentBasedOnLocation()
     * and saves it in an attribute on the FacesContext, which is then used by 
     * CompositeComponentImplicitObject.
     * @param facesContext
     * @param location
     */
    public static void saveCompositeComponentForResolver(FacesContext facesContext, Location location, int ccLevel)
    {
        UIComponent cc = ccLevel > 0 ? getCompositeComponentBasedOnLocation(facesContext, location, ccLevel)
                : getCompositeComponentBasedOnLocation(facesContext, location);
        List<UIComponent> list = (List<UIComponent>) facesContext.getAttributes().get(CURRENT_COMPOSITE_COMPONENT_KEY);
        if (list == null)
        {
            list = new ArrayList<UIComponent>();
            facesContext.getAttributes().put(CURRENT_COMPOSITE_COMPONENT_KEY, list);
        }
        list.add(cc);
    }
    
    /**
     * Removes the composite component from the attribute map of the FacesContext.
     * @param facesContext
     */
    public static void removeCompositeComponentForResolver(FacesContext facesContext)
    {
        List<UIComponent> list = (List<UIComponent>) facesContext.getAttributes().get(CURRENT_COMPOSITE_COMPONENT_KEY);
        if (list != null)
        {
            list.remove(list.size()-1);
        }
    }
    
    /**
     * Tests if the expression refers to the current composite component: #{cc}
     * @return
     */
    public static boolean isCompositeComponentExpression(String expression)
    {
        if (expression.contains(CC))
        {
            return CC_EXPRESSION_REGEX.matcher(expression).matches();
        }
        else
        {
            return false;
        }
    }
    
    /**
     * Tests if cc.attrs is used as a method expression in an expression String. This means 
     * cc.attrs must occur, must stand before a '(', because otherwise it would be a method parameter 
     * (EL 2.2), and there must be no '.' after cc.attrs unless there is a left parenthesis
     * before it (e.g. #{cc.attrs.method(bean.parameter)}).
     * @param expression
     * @return
     */
    public static boolean isCompositeComponentAttrsMethodExpression(String expression)
    {
        if (expression.contains(CC_ATTRS))
        {
            return CC_ATTRS_METHOD_EXPRESSION_REGEX.matcher(expression).matches();
        }
        else
        {
            return false;
        }
    }
    
}
