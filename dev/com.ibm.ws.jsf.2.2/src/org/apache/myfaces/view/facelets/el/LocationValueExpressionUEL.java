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
 * LocationValueExpression for el-api 2.2
 * 
 * @author Jakob Korherr (latest modification by $Author: lu4242 $)
 * @version $Revision: 1351625 $ $Date: 2012-06-19 09:48:54 +0000 (Tue, 19 Jun 2012) $
 */
public class LocationValueExpressionUEL extends LocationValueExpression
{

    private static final long serialVersionUID = 1824869909994211424L;

    public LocationValueExpressionUEL()
    {
        super();
    }
    
    public LocationValueExpressionUEL(Location location, ValueExpression delegate)
    {
        super(location, delegate);
    }
    
    public LocationValueExpressionUEL(Location location, ValueExpression delegate, int ccLevel)
    {
        super(location, delegate, ccLevel);
    }
    
    public LocationValueExpression apply(int newCCLevel)
    {
        if(this.ccLevel == newCCLevel)
        {
            return this;
        }
        else
        {
            return new LocationValueExpressionUEL(this.location, this.delegate, newCCLevel);
        }
    }
    
    @Override
    public ValueReference getValueReference(ELContext context)
    {
        FacesContext facesContext = (FacesContext) context.getContext(FacesContext.class);
        CompositeComponentELUtils.saveCompositeComponentForResolver(facesContext, location, ccLevel);
        try
        {
            return delegate.getValueReference(context);
        }
        finally
        {
            CompositeComponentELUtils.removeCompositeComponentForResolver(facesContext);
        }
    }

}
