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
package javax.faces.convert;

import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

/**
 * This class encapsulates a FacesMessage to evaluate the label expression on render response, where f:loadBundle is
 * available
 */
class _LabeledFacesMessage extends FacesMessage
{

    public _LabeledFacesMessage()
    {
        super();
    }

    public _LabeledFacesMessage(Severity severity, String summary, String detail)
    {
        super(severity, summary, detail);
    }

    public _LabeledFacesMessage(String summary, String detail)
    {
        super(summary, detail);
    }

    public _LabeledFacesMessage(String summary)
    {
        super(summary);
    }

    @Override
    public String getDetail()
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ValueExpression value =
                facesContext.getApplication().getExpressionFactory().createValueExpression(facesContext.getELContext(),
                    super.getDetail(), String.class);
        return (String)value.getValue(facesContext.getELContext());
    }

    @Override
    public String getSummary()
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ValueExpression value =
                facesContext.getApplication().getExpressionFactory().createValueExpression(facesContext.getELContext(),
                    super.getSummary(), String.class);
        return (String)value.getValue(facesContext.getELContext());
    }

}
