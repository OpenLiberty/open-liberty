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

import org.apache.myfaces.convert.ConverterUtils;

import javax.faces.validator.LengthValidator;
import javax.faces.validator.Validator;
import javax.servlet.jsp.JspException;

/**
 * @author Thomas Spiegl (latest modification by $Author: bommel $)
 * @author Manfred Geiler
 * @version $Revision: 1187701 $ $Date: 2011-10-22 12:21:54 +0000 (Sat, 22 Oct 2011) $
 */
public class ValidateLengthTag extends GenericMinMaxValidatorTag<Integer>
{
    private static final long serialVersionUID = 4858632671998693059L;

    private static final String VALIDATOR_ID = "javax.faces.Length";

    @Override
    protected Validator createValidator() throws JspException
    {
        setValidatorIdString(VALIDATOR_ID);
        LengthValidator validator = (LengthValidator)super.createValidator();
        if (null != _min)
        {
            validator.setMinimum(_min);
        }
        if (null != _max)
        {
            validator.setMaximum(_max);
        }
        return validator;
    }

    @Override
    protected boolean isMinLTMax()
    {
        return _min <= _max;
    }

    @Override
    protected Integer getValue(Object value)
    {
        return ConverterUtils.convertToInt(value);
    }
}
