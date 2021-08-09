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

import javax.el.ELContext;
import java.beans.FeatureDescriptor;
import java.util.Map;
import javax.faces.context.FacesContext;

/**
 * Encapsulates information needed by the ImplicitObjectResolver
 * 
 * @author Leonardo Uribe
 */
public class FlowScopeImplicitObject extends ImplicitObject
{

    private static final String NAME = "flowScope";

    /** Creates a new instance of FlowScopeImplicitObject */
    public FlowScopeImplicitObject()
    {
    }

    @Override
    public Object getValue(ELContext context)
    {
        FacesContext facesContext = facesContext(context);
        return facesContext.getApplication().getFlowHandler().getCurrentFlowScope();
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
        return makeDescriptor(NAME, "Flow scope attributes", Map.class);
    }

}
