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
package org.apache.myfaces.view.facelets.impl;

import java.util.HashMap;
import java.util.Map;

import javax.el.ValueExpression;

import org.apache.myfaces.view.facelets.PageContext;

public class PageContextImpl extends PageContext
{
    
    private Map<String, ValueExpression> _attributes = null;
    
    private boolean _isCacheELExpressions;

    public PageContextImpl()
    {
        _isCacheELExpressions = true;
    }

    @Override
    public Map<String, ValueExpression> getAttributes()
    {
        if (_attributes == null)
        {
            _attributes = new HashMap<String, ValueExpression>();
        }
        return _attributes;
    }

    @Override
    public int getAttributeCount()
    {
        return _attributes == null ? 0 : _attributes.size();
    }

    @Override
    public boolean isAllowCacheELExpressions()
    {
        return _isCacheELExpressions;
    }

    @Override
    public void setAllowCacheELExpressions(boolean cacheELExpressions)
    {
        _isCacheELExpressions = cacheELExpressions;
    }
}
