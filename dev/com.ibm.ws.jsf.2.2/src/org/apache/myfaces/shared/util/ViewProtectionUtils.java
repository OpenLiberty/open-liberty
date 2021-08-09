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
package org.apache.myfaces.shared.util;

import java.util.Set;
import javax.faces.context.FacesContext;

/**
 *
 * @since 2.2
 */
public class ViewProtectionUtils
{
    
    /**
     * NOTE: Taken from org.apache.catalina.deploy.SecurityConstraint
     * 
     * Does the specified request path match the specified URL pattern?
     * This method follows the same rules (in the same order) as those used
     * for mapping requests to servlets.
     *
     * @param path Context-relative request path to be checked
     *  (must start with '/')
     * @param pattern URL pattern to be compared against
     */
    public static boolean matchPattern(String path, String pattern)
    {
        // Normalize the argument strings
        if ((path == null) || (path.length() == 0))
        {
            path = "/";
        }
        if ((pattern == null) || (pattern.length() == 0))
        {
            pattern = "/";
        }

        // Check for exact match
        if (path.equals(pattern))
        {
            return (true);
        }

        // Check for path prefix matching
        if (pattern.startsWith("/") && pattern.endsWith("/*"))
        {
            pattern = pattern.substring(0, pattern.length() - 2);
            if (pattern.length() == 0)
            {
                return (true);  // "/*" is the same as "/"
            }
            if (path.endsWith("/"))
            {
                path = path.substring(0, path.length() - 1);
            }
            while (true)
            {
                if (pattern.equals(path))
                {
                    return (true);
                }
                int slash = path.lastIndexOf('/');
                if (slash <= 0)
                {
                    break;
                }
                path = path.substring(0, slash);
            }
            return (false);
        }

        // Check for suffix matching
        if (pattern.startsWith("*."))
        {
            int slash = path.lastIndexOf('/');
            int period = path.lastIndexOf('.');
            if ((slash >= 0) && (period > slash) &&
                path.endsWith(pattern.substring(1)))
            {
                return (true);
            }
            return (false);
        }

        // Check for universal mapping
        if (pattern.equals("/"))
        {
            return (true);
        }

        return (false);
    }
    
    public static boolean isViewProtected(FacesContext context, String viewId)
    {
        Set<String> protectedViews = context.getApplication().getViewHandler().getProtectedViewsUnmodifiable();
        if (!protectedViews.isEmpty())
        {
            boolean matchFound = false;
            for (String urlPattern : protectedViews)
            {
                if (ViewProtectionUtils.matchPattern(viewId, urlPattern))
                {
                    matchFound = true;
                    break;
                }
            }
            return matchFound;
        }
        else
        {
            return false;
        }
    }
}
