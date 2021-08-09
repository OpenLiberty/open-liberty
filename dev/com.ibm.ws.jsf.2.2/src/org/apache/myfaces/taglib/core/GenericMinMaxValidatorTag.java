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
package org.apache.myfaces.taglib.core;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.servlet.jsp.JspException;

/**
 * This is the base Tag for all ValidatorTags which got a minimum and a maimum attribute.
 * 
 * @author Andreas Berger (latest modification by $Author: slessard $)
 * @version $Revision: 701829 $ $Date: 2008-10-05 17:06:02 +0000 (Sun, 05 Oct 2008) $
 * @since 1.2
 */
public abstract class GenericMinMaxValidatorTag<T> extends ValidatorTag
{
    protected ValueExpression _minimum;
    protected ValueExpression _maximum;
    protected T _min = null;
    protected T _max = null;

    public void setMinimum(ValueExpression minimum)
    {
        _minimum = minimum;
    }

    public void setMaximum(ValueExpression maximum)
    {
        _maximum = maximum;
    }

    @Override
    public void release()
    {
        _minimum = null;
        _maximum = null;
        _min = null;
        _max = null;
    }

    /**
     * This method returns the Validator, you have to cast it to the correct type and apply the min and max values.
     * 
     * @return
     * @throws JspException
     */
    @Override
    protected Validator createValidator() throws JspException
    {
        if (null == _minimum && null == _maximum)
        {
            throw new JspException("a minimum and / or a maximum have to be specified");
        }
        ELContext elContext = FacesContext.getCurrentInstance().getELContext();
        if (null != _minimum)
        {
            _min = getValue(_minimum.getValue(elContext));
        }
        if (null != _maximum)
        {
            _max = getValue(_maximum.getValue(elContext));
        }
        if (null != _minimum && null != _maximum)
        {
            if (!isMinLTMax())
            {
                throw new JspException("maximum limit must be greater than the minimum limit");
            }
        }
        return super.createValidator();
    }

    /**
     * @return true if min is lower than max
     */
    protected abstract boolean isMinLTMax();

    /**
     * Wrapper method.
     * 
     * @param value
     * @return
     */
    protected abstract T getValue(Object value);
}
