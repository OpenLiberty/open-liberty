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
package org.apache.myfaces.config.impl.digester.elements;

import org.apache.myfaces.config.element.ViewPoolParameter;

/**
 *
 */
public class ViewPoolParameterImpl extends ViewPoolParameter
{
    private String _name;
    private String _value;

    @Override
    public String getName()
    {
        return _name;
    }

    @Override
    public String getValue()
    {
        return _value;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name)
    {
        this._name = name;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value)
    {
        this._value = value;
    }
}
