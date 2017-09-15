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

public class ServletMapping
{
    private final String _servletName;
    private final Class _servletClass;
    private final String _urlPattern;
    private final String _extension;
    private final String _prefix;

    public ServletMapping(String servletName, Class servletClass, String urlPattern)
    {
        _servletName = servletName;
        _servletClass = servletClass;
        _urlPattern = urlPattern;
        _extension = _urlPattern != null && _urlPattern.startsWith("*.") ? _urlPattern.substring(_urlPattern
                .indexOf('.')) : null;
        if (_extension == null)
        {
            int index = _urlPattern.indexOf("/*");
            if (index != -1)
            {
                _prefix = _urlPattern.substring(0, _urlPattern.indexOf("/*"));
            }
            else
            {
                _prefix = _urlPattern;
            }
        }
        else
        {
            _prefix = null;
        }
    }

    public boolean isExtensionMapping()
    {
        return _extension != null;
    }

    public String getExtension()
    {
        return _extension;
    }

    public String getPrefix()
    {
        return _prefix;
    }

    public String getServletName()
    {
        return _servletName;
    }

    public Class getServletClass()
    {
        return _servletClass;
    }

    public String getUrlPattern()
    {
        return _urlPattern;
    }
}
