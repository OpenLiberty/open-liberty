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
 * composite:actionSource, composite:valueHolder and composite:editableValueHolder
 * do the same: register an AttachedObjectTarget on the "targetList" mentioned on
 * ViewDeclarationLanguage.retargetAttachedObjects. AttachedObjectTargetHandler group the
 * common behavior
 * 
 * @author Leonardo Uribe (latest modification by $Author: struberg $)
 * @version $Revision: 1194849 $ $Date: 2011-10-29 09:28:45 +0000 (Sat, 29 Oct 2011) $
 */
@JSFFaceletTag
public abstract class AttachedObjectTargetHandler<T extends AttachedObjectTarget> 
    extends TagHandler implements InterfaceDescriptorCreator
{

    //private static final Log log = LogFactory.getLog(AttachedObjectTargetHandler.class);
    private static final Logger log = Logger.getLogger(AttachedObjectTargetHandler.class.getName());
    
    /**
     * Indicate the name of the attribute that the component should expose
     * to page authors.
     * 
     */
    @JSFFaceletAttribute(name="name",
            className="javax.el.ValueExpression",
            deferredValueType="java.lang.String",
            required=true)
    protected final TagAttribute _name;

    /**
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
    
    private AttachedObjectTarget _target;

    public AttachedObjectTargetHandler(TagConfig config)
    {
        super(config);
        _name = getRequiredAttribute("name");
        _targets = getAttribute("targets");
        if (_name.isLiteral())
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
            AttachedObjectTarget target = createAttachedObjectTarget(ctx);
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
    
    //@Override
    //public FaceletHandler getNextHandler()
    //{
    //    return nextHandler;
    //}

    /**
     * Create a new AttachedObjectTarget instance to be added on the 
     * target list.
     * 
     * @return
     */
    protected abstract T createAttachedObjectTarget(FaceletContext ctx);    
}
