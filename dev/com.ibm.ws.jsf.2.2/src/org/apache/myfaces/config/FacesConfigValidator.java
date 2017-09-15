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
package org.apache.myfaces.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.faces.context.ExternalContext;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.config.element.ManagedBean;
import org.apache.myfaces.config.element.NavigationCase;
import org.apache.myfaces.config.element.NavigationRule;
import org.apache.myfaces.shared.util.ClassUtils;

public class FacesConfigValidator
{

    /**
     * Validate if the managed beans and navigations rules are correct.
     * 
     * <p>For example, it checks if the managed bean classes really exists, or if the 
     * navigation rules points to existing view files.</p>
     */
    @JSFWebConfigParam(since="2.0", defaultValue="false", expectedValues="true, false")
    public static final String VALIDATE_CONTEXT_PARAM = "org.apache.myfaces.VALIDATE";
    
    private FacesConfigValidator()
    {
        // hidden 
    }

    public static List<String> validate(ExternalContext ctx)
    {
        
        RuntimeConfig runtimeConfig = RuntimeConfig.getCurrentInstance(ctx);
        
        Map<String, ManagedBean> managedBeansMap = runtimeConfig.getManagedBeans();
        
        Collection<? extends ManagedBean> managedBeans = null;
        if (managedBeansMap != null)
        {
            managedBeans = managedBeansMap.values();
        }
        
        Collection<? extends NavigationRule> navRules = runtimeConfig.getNavigationRules();
        
        return validate(managedBeans, navRules, ctx);
        
    }
    
    public static List<String> validate(Collection<? extends ManagedBean> managedBeans, 
                                        Collection<? extends NavigationRule> navRules, ExternalContext ctx)
    {
        
        List<String> list = new ArrayList<String>();
        
        if (managedBeans != null)
        {
            validateManagedBeans(managedBeans, list);
        }
        
        if (navRules != null)
        {
            validateNavRules(navRules, list, ctx);
        }
        
        return list;
    }

    private static void validateNavRules(Collection<? extends NavigationRule> navRules, List<String> list,
                                         ExternalContext ctx)
    {
        for (NavigationRule navRule : navRules)
        {
            validateNavRule(navRule, list, ctx);
        }
    }
    
    private static void validateNavRule(NavigationRule navRule, List<String> list, ExternalContext ctx)
    {
        String fromId = navRule.getFromViewId();
        URL filePath;
        try
        {
            filePath = ctx.getResource(fromId);

            if(fromId != null && ! "*".equals(fromId) && filePath == null)
            {
                list.add("File for navigation 'from id' does not exist " + filePath);
            }            
        }
        catch (MalformedURLException e)
        {
            list.add("File for navigation 'from id' does not exist " + fromId);
        }
        
        for (NavigationCase caze : navRule.getNavigationCases())
        {
            try
            {
                URL toViewPath = ctx.getResource(caze.getToViewId());
                
                if(toViewPath == null)
                {
                    list.add("File for navigation 'to id' does not exist " + toViewPath);
                }
            }
            catch (MalformedURLException e)
            {
                list.add("File for navigation 'from id' does not exist " + caze.getToViewId());
            }
        }
    }
    
    private static void validateManagedBeans(Collection<? extends ManagedBean> managedBeans, List<String> list)
    {
        for (ManagedBean managedBean : managedBeans)
        {
            validateManagedBean(managedBean, list);
        }
    }

    private static void validateManagedBean(ManagedBean managedBean, List<String> list)
    {
        String className = managedBean.getManagedBeanClassName();
        
        try
        {
            ClassUtils.classForName(className);
        }
        catch (ClassNotFoundException e)
        { 
            String msg = "Could not locate class " 
                + className + " for managed bean '" + managedBean.getManagedBeanName() + "'";
            
            list.add(msg);
        }
    }
}
