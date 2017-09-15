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
package org.apache.myfaces.context;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.faces.application.ResourceDependency;
import javax.faces.context.FacesContext;
import org.apache.myfaces.view.facelets.el.ELText;

/**
 *
 * @author lu4242
 */
public class RequestViewMetadata implements Serializable
{
    public static final String RESOURCE_DEPENDENCY_KEY = "oam.component.resource.RDK";

    // No lazy init: every view has one (UIView.class) or more classes to process   
    private Map<Class<?>, Boolean> processedClasses = new HashMap<Class<?>,Boolean>();
    
    private Map<ResourceDependency, Boolean> addedResources;
    
    private Map<Class<?>, Boolean> initialProcessedClasses;
    private Map<ResourceDependency, Boolean> initialAddedResources;
    
    public RequestViewMetadata()
    {
        initialProcessedClasses = null;
        initialAddedResources = null;
    }
    
    /**
     * Clone the current request view metadata into another instance, so 
     * it can be used in a view.
     * 
     * @return 
     */
    public RequestViewMetadata cloneInstance()
    {
        RequestViewMetadata rvm = new RequestViewMetadata();
        rvm.initialProcessedClasses = new HashMap<Class<?>, Boolean>(
                this.initialProcessedClasses != null ? 
                    this.initialProcessedClasses : this.processedClasses);
        if (this.initialAddedResources != null)
        {
            rvm.initialAddedResources = new HashMap<ResourceDependency, Boolean>(
                    this.initialAddedResources);
        }
        else if (this.addedResources != null)
        {
            rvm.initialAddedResources = new HashMap<ResourceDependency, Boolean>(
                    this.addedResources);
        }
        return rvm;
    }
    
    public boolean isResourceDependencyAlreadyProcessed(ResourceDependency dependency)
    {
        if (initialAddedResources != null)
        {
            if (initialAddedResources.containsKey(dependency))
            {
                return true;
            }
        }
        if (addedResources == null)
        {
            return false;
        }
        return addedResources.containsKey(dependency); 
    }
    
    public void setResourceDependencyAsProcessed(ResourceDependency dependency)
    {
        if (addedResources == null)
        {
            addedResources = new HashMap<ResourceDependency,Boolean>();
        }
        addedResources.put(dependency, true);
    }

    public boolean isClassAlreadyProcessed(Class<?> inspectedClass)
    {
        if (initialProcessedClasses != null)
        {
            if (initialProcessedClasses.containsKey(inspectedClass))
            {
                return true;
            }
        }
        return processedClasses.containsKey(inspectedClass);
    }

    public void setClassProcessed(Class<?> inspectedClass)
    {
        processedClasses.put(inspectedClass, Boolean.TRUE);
    }
    
    public Map<String, List<ResourceDependency>> getResourceDependencyAnnotations(FacesContext context)
    {
        if (initialAddedResources == null && addedResources == null)
        {
            return Collections.emptyMap();
        }
        Map<String, List<ResourceDependency>> map = new HashMap<String, List<ResourceDependency>>();
        //List<ResourceDependency> list = new ArrayList<ResourceDependency>();
        if (initialAddedResources != null)
        {
            for (ResourceDependency annotation : initialAddedResources.keySet())
            {
                String target = annotation.target();
                if (target != null && target.length() > 0)
                {
                    target = ELText.parse(context.getApplication().getExpressionFactory(),
                                          context.getELContext(), target).toString(context.getELContext());
                }
                else
                {
                    target = "head";
                }
                List<ResourceDependency> list = map.get(target);
                if (list == null)
                {
                    list = new ArrayList<ResourceDependency>();
                    map.put(target, list);
                }
                list.add(annotation);
            }
        }
        if (addedResources != null)
        {
            for (ResourceDependency annotation : addedResources.keySet())
            {
                String target = annotation.target();
                if (target != null && target.length() > 0)
                {
                    target = ELText.parse(context.getApplication().getExpressionFactory(),
                                          context.getELContext(), target).toString(context.getELContext());
                }
                else
                {
                    target = "head";
                }
                List<ResourceDependency> list = map.get(target);
                if (list == null)
                {
                    list = new ArrayList<ResourceDependency>();
                    map.put(target, list);
                }
                list.add(annotation);
            }
        }
        return map;
    }
}
