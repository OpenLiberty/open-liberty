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

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.el.ValueReference;
import javax.faces.context.FacesContext;
import javax.faces.view.Location;

/**
 * ResourceLocationValueExpression for el-api 2.2
 * 
 * @author Leonardo Uribe
 * 
 */
public class ResourceLocationValueExpressionUEL extends ResourceLocationValueExpression
{

    private static final long serialVersionUID = 1824869909994211424L;

    public ResourceLocationValueExpressionUEL()
    {
        super();
    }
    
    public ResourceLocationValueExpressionUEL(Location location, ValueExpression delegate)
    {
        super(location, delegate);
    }
    
    @Override
    public ValueReference getValueReference(ELContext context)
    {
        FacesContext facesContext = (FacesContext) context.getContext(FacesContext.class);
        ResourceELUtils.saveResourceLocationForResolver(facesContext, location);
        try
        {
            return delegate.getValueReference(context);
        }
        finally
        {
            ResourceELUtils.removeResourceLocationForResolver(facesContext);
        }
    }

}
