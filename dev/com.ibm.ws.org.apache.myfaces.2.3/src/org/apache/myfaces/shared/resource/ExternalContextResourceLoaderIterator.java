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

package org.apache.myfaces.shared.resource;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import javax.faces.application.ResourceVisitOption;
import javax.faces.context.FacesContext;

/**
 *
 */
public class ExternalContextResourceLoaderIterator implements Iterator<String>
{
    private FacesContext facesContext;
    private String basePath;
    private int maxDepth;
    private ResourceVisitOption[] options;
    private Deque<String> stack = new LinkedList<String>();

    public ExternalContextResourceLoaderIterator(FacesContext facesContext, String path,
             int maxDepth, ResourceVisitOption... options)
    {
        this.facesContext = facesContext;
        this.basePath = path;
        this.maxDepth = maxDepth;
        this.options = options;

        Set<String> paths = facesContext.getExternalContext().getResourcePaths(basePath);

        if (paths == null)
        {
            //empty stack means empty iterator.
        }
        else
        {
            for (String p : paths)
            {
                if (p.startsWith("/WEB-INF") && isTopLevelViewsOnly(options))
                {
                    // skip
                }
                else if (p.startsWith("/META-INF") && isTopLevelViewsOnly(options))
                {
                    // skip 
                }
                else
                {
                    stack.add(p);
                }
            }
        }
    }

    private boolean isTopLevelViewsOnly(ResourceVisitOption... options) 
    {
        boolean isTopLevelViewsOnly = false;
        
        for (ResourceVisitOption option : options) 
        {
            if(option == ResourceVisitOption.TOP_LEVEL_VIEWS_ONLY) 
            {
                isTopLevelViewsOnly = true;
                break;
            }
        }
        
        return isTopLevelViewsOnly;
    }

    @Override
    public boolean hasNext()
    {
        if (!stack.isEmpty())
        {
            String path = stack.peek();
            do 
            {
                if (ResourceLoaderUtils.isDirectory(path))
                {
                    path = stack.pop();
                    int depth = ResourceLoaderUtils.getDepth(path);
                    if (depth < maxDepth)
                    {
                        Set<String> list = facesContext.getExternalContext().getResourcePaths(path);
                        for (String p : list)
                        {
                            stack.add(p);
                        }
                    }
                    if (!stack.isEmpty())
                    {
                        path = stack.peek();
                    }
                    else
                    {
                        path = null;
                    }
                }
            }
            while (path != null && ResourceLoaderUtils.isDirectory(path) && !stack.isEmpty());

            return !stack.isEmpty();
        }
        return false;
    }

    @Override
    public String next()
    {
        if (!stack.isEmpty())
        {
            String path = stack.pop();
            do 
            {
                if (ResourceLoaderUtils.isDirectory(path))
                {
                    int depth = ResourceLoaderUtils.getDepth(path);
                    if (depth < maxDepth)
                    {
                        Set<String> list = facesContext.getExternalContext().getResourcePaths(path);
                        for (String p : list)
                        {
                            stack.add(p);
                        }
                    }
                    if (!stack.isEmpty())
                    {
                        path = stack.pop();
                    }
                    else
                    {
                        path = null;
                    }
                }
            }
            while (path != null && ResourceLoaderUtils.isDirectory(path) && !stack.isEmpty());
            if (path != null)
            {
                // Calculate name based on url, basePath.
                return path;
            }
        }
        return null;
    }

    @Override
    public void remove()
    {
    }
}

