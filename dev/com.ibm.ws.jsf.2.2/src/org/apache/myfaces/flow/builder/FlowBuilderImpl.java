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
package org.apache.myfaces.flow.builder;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.faces.flow.Flow;
import javax.faces.flow.builder.FlowBuilder;
import javax.faces.flow.builder.FlowCallBuilder;
import javax.faces.flow.builder.MethodCallBuilder;
import javax.faces.flow.builder.NavigationCaseBuilder;
import javax.faces.flow.builder.ReturnBuilder;
import javax.faces.flow.builder.SwitchBuilder;
import javax.faces.flow.builder.ViewBuilder;
import org.apache.myfaces.flow.FlowImpl;
import org.apache.myfaces.flow.ParameterImpl;

/**
 *
 * @since 2.2
 * @author Leonardo Uribe
 */
public class FlowBuilderImpl extends FlowBuilder
{

    private static final Class[] EMPTY_PARAMS = new Class[]
    {
    };
    private FlowImpl _facesFlow;
    private FacesContext _facesContext;

    public FlowBuilderImpl()
    {
        _facesFlow = new FlowImpl();
    }

    public FlowBuilderImpl(FacesContext context)
    {
        super();
        _facesContext = context;
    }

    @Override
    public FlowBuilder id(String definingDocumentId, String id)
    {
        _facesFlow.setDefiningDocumentId(definingDocumentId);
        _facesFlow.setId(id);
        return this;
    }

    @Override
    public ViewBuilder viewNode(String viewNodeId, String vdlDocumentId)
    {
        return new ViewBuilderImpl(_facesFlow, viewNodeId, vdlDocumentId);
    }

    @Override
    public SwitchBuilder switchNode(String switchNodeId)
    {
        return new SwitchBuilderImpl(this, _facesFlow, switchNodeId);
    }

    @Override
    public ReturnBuilder returnNode(String returnNodeId)
    {
        return new ReturnBuilderImpl(this, _facesFlow, returnNodeId);
    }

    @Override
    public MethodCallBuilder methodCallNode(String methodCallNodeId)
    {
        return new MethodCallBuilderImpl(this, _facesFlow, methodCallNodeId);
    }

    @Override
    public FlowCallBuilder flowCallNode(String flowCallNodeId)
    {
        return new FlowCallBuilderImpl(this, _facesFlow, flowCallNodeId);
    }

    @Override
    public FlowBuilder initializer(MethodExpression methodExpression)
    {
        _facesFlow.setInitializer(methodExpression);
        return this;
    }

    @Override
    public FlowBuilder initializer(String methodExpression)
    {
        _facesFlow.setInitializer(createMethodExpression(methodExpression));
        return this;
    }

    @Override
    public FlowBuilder finalizer(MethodExpression methodExpression)
    {
        _facesFlow.setFinalizer(methodExpression);
        return this;
    }

    @Override
    public FlowBuilder finalizer(String methodExpression)
    {
        _facesFlow.setFinalizer(createMethodExpression(methodExpression));
        return this;
    }

    @Override
    public FlowBuilder inboundParameter(String name, ValueExpression value)
    {
        _facesFlow.putInboundParameter(name, new ParameterImpl(name, value));
        return this;
    }

    @Override
    public FlowBuilder inboundParameter(String name, String value)
    {
        _facesFlow.putInboundParameter(name, new ParameterImpl(name,
            createValueExpression(value)));
        return this;
    }

    @Override
    public Flow getFlow()
    {
        _facesContext = null;
        return _facesFlow;
    }

    /**
     * The idea is grab FacesContext just once and then when the flow is returned clear the variable.
     *
     * @return
     */
    FacesContext getFacesContext()
    {
        if (_facesContext == null)
        {
            _facesContext = FacesContext.getCurrentInstance();
        }
        return _facesContext;
    }
    
    public MethodExpression createMethodExpression(String methodExpression)
    {
        FacesContext facesContext = getFacesContext();
        return facesContext.getApplication().getExpressionFactory().createMethodExpression(
            facesContext.getELContext(), methodExpression, null, EMPTY_PARAMS);
    }
    
    public MethodExpression createMethodExpression(String methodExpression, Class[] paramTypes)
    {
        FacesContext facesContext = getFacesContext();
        return facesContext.getApplication().getExpressionFactory().createMethodExpression(
            facesContext.getELContext(), methodExpression, null, paramTypes);
    }
    
    public ValueExpression createValueExpression(String value)
    {
        FacesContext facesContext = getFacesContext();
        return facesContext.getApplication().getExpressionFactory()
            .createValueExpression(facesContext.getELContext(), value, Object.class); 
    }

    @Override
    public NavigationCaseBuilder navigationCase()
    {
        return new NavigationCaseBuilderImpl(this, _facesFlow);
    }
}
