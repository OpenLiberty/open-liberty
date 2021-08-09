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
package org.apache.myfaces.shared.webapp.webxml;

public class FilterMapping
{
    private String _filterName;
    private Class _filterClass;
    private String _urlPattern;
    private boolean _isExtensionMapping = false;

    public FilterMapping(String filterName,
                          Class filterClass,
                          String urlPattern)
    {
        _filterName = filterName;
        _filterClass = filterClass;
        _urlPattern = urlPattern;
        if (_urlPattern != null)
        {
            if (_urlPattern.startsWith("*."))
            {
                _isExtensionMapping = true;
            }
        }
    }

    public boolean isExtensionMapping()
    {
        return _isExtensionMapping;
    }

    public String getFilterName()
    {
        return _filterName;
    }

    public Class getFilterClass()
    {
        return _filterClass;
    }

    public String getUrlPattern()
    {
        return _urlPattern;
    }
}
