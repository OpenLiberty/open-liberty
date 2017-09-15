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
import javax.faces.convert.Converter;
import javax.faces.webapp.ConverterELTag;
import javax.servlet.jsp.JspException;

/**
 * Implementation of ConverterELTag
 * 
 * @author Bruno Aranda (latest modification by $Author: jakobk $)
 * @version $Revision: 922543 $ $Date: 2010-03-13 11:55:01 +0000 (Sat, 13 Mar 2010) $
 */
public class ConverterTag extends ConverterELTag
{

    private static final long serialVersionUID = -4506829108081L;
    private ValueExpression _converterId;
    private ValueExpression _binding;
    private String _converterIdString = null;

    public ConverterTag()
    {
        super();
    }

    public void setConverterId(ValueExpression converterId)
    {
        _converterId = converterId;
    }

    public void setBinding(ValueExpression binding)
    {
        _binding = binding;
    }

    /**
     * Use this method to specify the converterId programmatically.
     * 
     * @param converterIdString
     */
    public void setConverterIdString(String converterIdString)
    {
        _converterIdString = converterIdString;
    }

    @Override
    public void release()
    {
        super.release();
        _converterId = null;
        _binding = null;
        _converterIdString = null;
    }

    @Override
    protected Converter createConverter() throws JspException
    {
        Converter converter = null;

        FacesContext facesContext = FacesContext.getCurrentInstance();
        ELContext elContext = facesContext.getELContext();

        // try to create the converter from the binding expression first, and then from
        // the converterId
        if (_binding != null)
        {
            try
            {
                converter = (Converter)_binding.getValue(elContext);

                if (converter != null)
                {
                    return converter;
                }
            }
            catch (Exception e)
            {
                throw new JspException("Exception creating converter using binding", e);
            }
        }

        if ((_converterId != null) || (_converterIdString != null))
        {
            try
            {
                if (null != _converterIdString)
                {
                    converter = facesContext.getApplication().createConverter(_converterIdString);
                }
                else
                {
                    String converterId = (String)_converterId.getValue(elContext);
                    converter = facesContext.getApplication().createConverter(converterId);
                }

                // with binding no converter was created, set its value with the converter
                // created using the converterId
                if (converter != null && _binding != null)
                {
                    _binding.setValue(elContext, converter);
                }
            }
            catch (Exception e)
            {
                throw new JspException("Exception creating converter with converterId: " + _converterId, e);
            }
        }
        
        if (converter == null)
        {
            throw new JspException("Could not create converter. Please specify a valid converterId" +
                    " or a non-null binding.");
        }

        return converter;
    }

}
