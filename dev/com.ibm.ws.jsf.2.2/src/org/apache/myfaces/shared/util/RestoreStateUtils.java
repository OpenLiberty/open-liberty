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
package org.apache.myfaces.shared.util;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

public class RestoreStateUtils
{
    //private static Log log = LogFactory.getLog(RestoreStateUtils.class);
    private static Logger log = Logger.getLogger(RestoreStateUtils.class.getName());

    /**
     * Walk the component tree, executing any component-bindings to reattach
     * components to their backing beans. 
     * <p>
     *  Note that this method effectively breaks encapsulation; instead of
     *  asking each component to update itself and its children, this
     * method just reaches into each component. That makes it impossible
     * for any component to customise its behaviour at this point.
     * <p>
     * This has been filed as an issue against the spec. Until this
     * issue is resolved, we'll add a new marker-interface for components
     * to allow them to define their interest in handling children bindings themselves.
     */
    public static void recursivelyHandleComponentReferencesAndSetValid(FacesContext facesContext,
                                                                       UIComponent parent)
    {
        recursivelyHandleComponentReferencesAndSetValid(facesContext, parent, false);
    }

    public static void recursivelyHandleComponentReferencesAndSetValid(FacesContext facesContext,
                                                                       UIComponent parent, boolean forceHandle)
    {
        Method handleBindingsMethod = getBindingMethod(parent);

        if(handleBindingsMethod!=null && !forceHandle)
        {
            try
            {
                handleBindingsMethod.invoke(parent,new Object[]{});
            }
            catch (Throwable th)
            {
                log.log(Level.SEVERE, "Exception while invoking handleBindings on component with client-id:"
                        +parent.getClientId(facesContext),th);
            }
        }
        else
        {
            for (Iterator it = parent.getFacetsAndChildren(); it.hasNext(); )
            {
                UIComponent component = (UIComponent)it.next();

                ValueExpression binding = component.getValueExpression("binding");    //TODO: constant
                if (binding != null)
                {
                    binding.setValue(facesContext.getELContext(), component);
                }

                //This part is not necessary on JSF 1.2
                //if (component instanceof UIInput)
                //{
                //    ((UIInput)component).setValid(true);
                //}

                recursivelyHandleComponentReferencesAndSetValid(facesContext, component);
            }
        }
    }

    /**This is all a hack to work around a spec-bug which will be fixed in JSF2.0
     *
     * @param parent
     * @return true if this component is bindingAware (e.g. aliasBean)
     */
    private static Method getBindingMethod(UIComponent parent)
    {
        Class[] clazzes = parent.getClass().getInterfaces();

        for (int i = 0; i < clazzes.length; i++)
        {
            Class clazz = clazzes[i];

            if(clazz.getName().indexOf("BindingAware")!=-1)
            {
                try
                {
                    return  parent.getClass().getMethod("handleBindings",new Class[]{});
                }
                catch (NoSuchMethodException e)
                {
                    // return
                }
            }
        }

        return null;
    }
}
