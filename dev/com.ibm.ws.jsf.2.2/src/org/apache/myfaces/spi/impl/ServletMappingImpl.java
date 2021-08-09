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
package org.apache.myfaces.spi.impl;

import org.apache.myfaces.spi.ServletMapping;

public class ServletMappingImpl implements ServletMapping
{

    private org.apache.myfaces.shared_impl.webapp.webxml.ServletMapping _delegateMapping;

    public ServletMappingImpl(
            org.apache.myfaces.shared_impl.webapp.webxml.ServletMapping delegateMapping)
    {
        super();
        this._delegateMapping = delegateMapping;
    }
    

    public boolean isExtensionMapping()
    {
        return _delegateMapping.isExtensionMapping();
    }

    public String getExtension()
    {
        return _delegateMapping.getExtension();
    }

    public String getPrefix()
    {
        return _delegateMapping.getPrefix();
    }

    public String getServletName()
    {
        return _delegateMapping.getServletName();
    }

    public Class getServletClass()
    {
        return _delegateMapping.getServletClass();
    }

    public String getUrlPattern()
    {
        return _delegateMapping.getUrlPattern();
    }

}
