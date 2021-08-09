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
package org.apache.myfaces.application;

/**
 *
 * @author Leonardo Uribe
 */
class _WildcardPattern
{
    private final String pattern;
    private final boolean checkPrefix;
    private final String prefix;

    public _WildcardPattern(String pattern)
    {
        this.pattern = pattern;
        if (pattern.length() > 2)
        {
            checkPrefix = true;
            prefix = pattern.substring(0, pattern.length() - 1);
        }
        else
        {
            //It is a plain asterisk.
            checkPrefix = false;
            prefix = null;
        }
    }

    public String getPattern()
    {
        return pattern;
    }

    public boolean match(String value)
    {
        if (checkPrefix)
        {
            if (value != null && value.startsWith(prefix))
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            // Plain asterisk always match.
            return true;
        }
    }
}
