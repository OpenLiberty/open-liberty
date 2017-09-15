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
import javax.el.ELException;
import javax.el.PropertyNotFoundException;
import javax.el.ValueExpression;
import javax.el.ValueReference;
import javax.faces.view.facelets.TagAttribute;

/**
 * TagValueExpression for el-api 2.2
 * 
 * @author Jakob Korherr (latest modification by $Author: bommel $)
 * @version $Revision: 1187701 $ $Date: 2011-10-22 12:21:54 +0000 (Sat, 22 Oct 2011) $
 */
public class ContextAwareTagValueExpressionUEL extends ContextAwareTagValueExpression
{

    public ContextAwareTagValueExpressionUEL()
    {
        super();
    }

    public ContextAwareTagValueExpressionUEL(TagAttribute attr, ValueExpression orig)
    {
        super(attr, orig);
    }
    
    @Override
    public ValueReference getValueReference(ELContext context)
    {
        try
        {
            return getWrapped().getValueReference(context);
        }
        catch (PropertyNotFoundException pnfe)
        {
            throw new ContextAwarePropertyNotFoundException(getLocation(), getExpressionString(), getQName() ,  pnfe);
        }
        catch (ELException e)
        {
            throw new ContextAwareELException(getLocation(), getExpressionString(), getQName(),  e);
        }
        //Not necessary because NullPointerException by null context never occur and should not be wrapped
        //catch (Exception e) {
        //    throw new ContextAwareException(getLocation(), getExpressionString(), getQName(), e);
        //}
    }
    
}
