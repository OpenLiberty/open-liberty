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

import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.VariableResolver;

/**
 * This is the default VariableResolver. See JSF 1.2 spec section 5.8.1
 * 
 * @author Stan Silvert
 */
public class NullVariableResolver extends VariableResolver
{

    /** Creates a new instance of NullVariableResolver */
    public NullVariableResolver()
    {
    }

    @Override
    public Object resolveVariable(FacesContext facesContext, String name) throws EvaluationException
    {
        facesContext.getELContext().setPropertyResolved(false);

        return null;
    }

}
