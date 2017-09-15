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
package org.apache.myfaces.view.facelets.tag.composite;

import java.beans.BeanDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.component.UIComponent;
import javax.faces.view.AttachedObjectTarget;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletAttribute;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFFaceletTag;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;

/**
 * @author Leonardo Uribe (latest modification by $Author: struberg $)
 * @version $Revision: 1194849 $ $Date: 2011-10-29 09:28:45 +0000 (Sat, 29 Oct 2011) $
 */
@JSFFaceletTag(name="composite:clientBehavior")
public class ClientBehaviorHandler extends TagHandler implements InterfaceDescriptorCreator
{
    
    private static final Logger log = Logger.getLogger(ClientBehaviorHandler.class.getName());
    
    /**
     * This attribute is used as the target event 
     * name, so client behaviors pointing to "name" 
     * will be attached on the related components 
     * identified by "targets" attribute and on 
     * the event name this attribute holds. In other
     * words, this is the "real" event name.
     */
    @JSFFaceletAttribute(name="event",
            className="javax.el.ValueExpression",
            deferredValueType="java.lang.String",
            required=true)
    protected final TagAttribute _event;

    /**
     * This attribute represents the source event name 
     * that is used when instances of the composite 
     * component are used. In other
     * words, this is the "logical" event name.
     * 
     */
    @JSFFaceletAttribute(name="name",
            className="javax.el.ValueExpression",
            deferredValueType="java.lang.String",
            required=true)
    protected final TagAttribute _name;
    
    /**
     * Indicate this clientBehavior description is the one
     * that has to be taken by default. There should be only
     * one clientBehavior with this property set to true in
     * a composite component interface description.
     */
    @JSFFaceletAttribute(name="default",
            className="javax.el.ValueExpression",
            deferredValueType="boolean")
    protected final TagAttribute _default;

    /**
     * Contains a list of clientIds separated by spaces that 
     * identify the component(s) that will be used to attach 
     * client behaviors from the composite component.
     * 
     */
    @JSFFaceletAttribute(name="targets",
            className="javax.el.ValueExpression",
            deferredValueType="java.lang.String")
    protected final TagAttribute _targets;

    /**
     * Check if the PropertyDescriptor instance created by this handler
     * can be cacheable or not. 
     */
    private boolean _cacheable;
    
    private ClientBehaviorAttachedObjectTargetImpl _target;

    public ClientBehaviorHandler(TagConfig config)
    {
        super(config);
        _name = getRequiredAttribute("name");
        _event = getAttribute("event");
        _default = getAttribute("default");
        _targets = getAttribute("targets");
        if (_name.isLiteral() && 
            (_event == null || _event.isLiteral()) &&
            (_default == null || _default.isLiteral() ))
        {
            _cacheable = true;
        }
        else
        {
            _cacheable = false;
        }
    }

    @SuppressWarnings("unchecked")
    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException
    {
        UIComponent compositeBaseParent
                = FaceletCompositionContext.getCurrentInstance(ctx).getCompositeComponentFromStack();

        CompositeComponentBeanInfo beanInfo = 
            (CompositeComponentBeanInfo) compositeBaseParent.getAttributes()
            .get(UIComponent.BEANINFO_KEY);
        
        if (beanInfo == null)
        {
            if (log.isLoggable(Level.SEVERE))
            {
                log.severe("Cannot find composite bean descriptor UIComponent.BEANINFO_KEY ");
            }
            return;
        }
        
        BeanDescriptor beanDescriptor = beanInfo.getBeanDescriptor(); 
        
        //1. Obtain the list mentioned as "targetList" on ViewDeclarationLanguage.retargetAttachedObjects
        List<AttachedObjectTarget> targetList = (List<AttachedObjectTarget>)
            beanDescriptor.getValue(
                    AttachedObjectTarget.ATTACHED_OBJECT_TARGETS_KEY);
        
        if (targetList == null)
        {
            //2. If not found create it and set
            targetList = new ArrayList<AttachedObjectTarget>();
            beanDescriptor.setValue(
                    AttachedObjectTarget.ATTACHED_OBJECT_TARGETS_KEY,
                    targetList);
        }
        
        //3. Create the instance of AttachedObjectTarget
        if (isCacheable())
        {
            if (_target == null)
            {
                _target = createAttachedObjectTarget(ctx);
            }
            targetList.add(_target);
        }
        else
        {
            ClientBehaviorAttachedObjectTargetImpl target = createAttachedObjectTarget(ctx);
            targetList.add(target);
        }
        
        this.nextHandler.apply(ctx, parent);
    }
    
    public boolean isCacheable()
    {
        return _cacheable;
    }
    
    public void setCacheable(boolean cacheable)
    {
        _cacheable = cacheable;
    }

    /**
     * Create a new AttachedObjectTarget instance to be added on the 
     * target list.
     * 
     * @return
     */
    protected ClientBehaviorAttachedObjectTargetImpl createAttachedObjectTarget(FaceletContext ctx)
    {
        ClientBehaviorAttachedObjectTargetImpl target = new ClientBehaviorAttachedObjectTargetImpl();
        
        if (_event != null)
        {
            target.setEvent(_event.getValueExpression(ctx, String.class));
        }
        if (_name != null)
        {
            target.setName(_name.getValueExpression(ctx, String.class));
        }
        if (_default != null)
        {
            target.setDefault(_default.getBoolean(ctx));
        }
        if (_targets != null)
        {
            target.setTargets(_targets.getValueExpression(ctx, String.class));
        }
        return target;
    }
}
