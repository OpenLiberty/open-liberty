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
package javax.faces.validator;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.el.ValueReference;
import javax.faces.el.CompositeComponentExpressionHolder;

/**
 * Utility class that isolates UEL calls, to prevent ClassNotFoundException
 * when bean validation is used without it. It is a similar hack to the one
 * used in portlet case.
 * 
 * @since 2.0
 */
final class _BeanValidatorUELUtils
{

    /**
     * Get the ValueReference from the ValueExpression.
     *
     * @param valueExpression
     * @param elCtx
     * @return A ValueReferenceWrapper with the necessary information about the ValueReference.
     */
    public static _ValueReferenceWrapper getUELValueReferenceWrapper(ValueExpression valueExpression,
                                                                     final ELContext elCtx)
    {
        ValueReference valueReference = valueExpression.getValueReference(elCtx);
        
        while (valueReference != null 
                && valueReference.getBase() instanceof CompositeComponentExpressionHolder)
        {
            valueExpression = ((CompositeComponentExpressionHolder) valueReference.getBase())
                                  .getExpression((String) valueReference.getProperty());
            if(valueExpression == null)
            {
                break;
            }
            valueReference = valueExpression.getValueReference(elCtx);
        }
        
        if (valueReference != null)
        {
            return new _ValueReferenceWrapper(valueReference.getBase(), valueReference.getProperty());
        }
        
        return null;
    }

}
