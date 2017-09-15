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
package javax.faces.view.facelets;

import javax.faces.component.UIComponent;

/**
 * Implementation of the tag logic used in the JSF specification. This is your golden hammer for wiring UIComponents to
 * Facelets.
 */
public class ComponentHandler extends DelegatingMetaTagHandler
{
    private ComponentConfig config;
    private TagHandlerDelegate helper;
    
    public ComponentHandler(ComponentConfig config)
    {
        super(config);
        
        this.config = config;

    }

    public ComponentConfig getComponentConfig()
    {
        return config;
    }

    public static boolean isNew(UIComponent component)
    {
        // -= Leonardo Uribe =- It seems org.apache.myfaces.view.facelets.tag.jsf.ComponentSupport.isNew(UIComponent)
        // has been moved to this location.
        // Originally this method was called from all tags that generate any kind of listeners
        // (f:actionListener, f:phaseListener, f:setPropertyActionListener, f:valueChangeListener).
        // This method prevent add listener when a facelet is applied twice. 
        // On MYFACES-2502 there is an explanation about where this is useful (partial state saving disabled).
        // return component != null && component.getParent() == null; 
        if (component != null)
        {
            UIComponent parent = component.getParent();
            if (parent == null)
            {
                return true;
            }
            else
            {
                // When a composite component is used, we could have tags attaching
                // objects or doing some operation on composite:implementation body 
                // like this:
                // <composite:implementation>
                //   <f:event ...../>
                // </composite:implementation>
                // This case is valid, but the parent is the UIPanel inside 
                // UIComponent.COMPOSITE_FACET_NAME facet key of the composite component.
                // So in this case we have to check if the component is a composite component
                // or not and if so, try to get the parent again.
                if (UIComponent.isCompositeComponent(parent))
                {
                    parent = parent.getParent();
                    if (parent == null)
                    {
                        return true;
                    }
                    else
                    {
                        return false;
                    }
                }
                else
                {
                    return false;
                }
            }
        }
        else
        {
            return false;
        }
    }

    public void onComponentCreated(FaceletContext ctx, UIComponent c, UIComponent parent)
    {
        // no-op.
    }

    public void onComponentPopulated(FaceletContext ctx, UIComponent c, UIComponent parent)
    {
        // no-op.
    }

    protected TagHandlerDelegate getTagHandlerDelegate()
    {
        if (helper == null)
        {
            // Spec seems to indicate that the helper is created here, as opposed to other Handler
            // instances, where it's presumably a new instance for every getter call.
            
            this.helper = delegateFactory.createComponentHandlerDelegate (this);
        }
        return helper;
    }
    
    /**
     * 
     * @since 2.2
     * @param ctx
     * @return 
     */
    public UIComponent createComponent(FaceletContext ctx)
    {
        return null;
    }
}
