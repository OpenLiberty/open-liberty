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
package org.apache.myfaces.el.unified.resolver.implicitobject;

import java.beans.FeatureDescriptor;
import java.util.List;

import javax.el.ELContext;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.myfaces.view.facelets.el.CompositeComponentELUtils;

/**
 * Encapsulates information needed by the ImplicitObjectResolver
 * 
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 1351625 $ $Date: 2012-06-19 09:48:54 +0000 (Tue, 19 Jun 2012) $
 */
public class CompositeComponentImplicitObject extends ImplicitObject
{

    private static final String NAME = "cc";

    /** Creates a new instance of CompositeComponentImplicitObjectImplicitObject */
    public CompositeComponentImplicitObject()
    {
    }

    @Override
    public Object getValue(ELContext context)
    {
        FacesContext facesContext = facesContext(context);
        
        // Look for the attribute set by LocationValueExpression
        // or LocationMethodExpression on the FacesContext
        List<UIComponent> list = (List<UIComponent>) facesContext.getAttributes()
                .get(CompositeComponentELUtils.CURRENT_COMPOSITE_COMPONENT_KEY);
        
        UIComponent cc = null;
        
        if (list != null && !list.isEmpty())
        {
            cc = list.get(list.size()-1);
        }
        if (cc == null)
        {
            // take the composite component from the stack
            cc = UIComponent.getCurrentCompositeComponent(facesContext(context));
        }
        return cc;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public Class<?> getType()
    {
        return null;
    }

    @Override
    public FeatureDescriptor getDescriptor()
    {
        return makeDescriptor(NAME,
                "Represents the composite component most recently pushed using UIComponent.pushComponentToEL",
                UIComponent.class);
    }
}
