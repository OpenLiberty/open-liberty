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
package org.apache.myfaces.el;

import javax.el.ELException;
import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.VariableResolver;

import org.apache.myfaces.shared.util.Assert;

/**
 * This class is used to delegate {@link #resolveVariable(FacesContext, String)} to the el resolver of the application.
 * 
 * @author Mathias Broekelmann (latest modification by $Author: bommel $)
 * @version $Revision: 1187701 $ $Date: 2011-10-22 12:21:54 +0000 (Sat, 22 Oct 2011) $
 */
@SuppressWarnings("deprecation")
public class VariableResolverToApplicationELResolverAdapter extends VariableResolver
{
    @Override
    public Object resolveVariable(FacesContext facesContext, String name) throws EvaluationException
    {
        Assert.notNull(facesContext, "facesContext");
        Assert.notNull(name, "name");

        try
        {
            return facesContext.getApplication().getELResolver().getValue(facesContext.getELContext(), null, name);
        }
        catch (ELException e)
        {
            throw new EvaluationException(e);
        }
    }

}
