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

import java.util.regex.Pattern;

import javax.faces.context.FacesContext;
import javax.faces.view.Location;

/**
 * Utility class when used in EL Expressions --> #{resource}
 */
public class ResourceELUtils
{

    // TODO: check this expression, maybe we can make it simpler, because "resource" implicit object
    // cannot be preceded by anything.
    public static final Pattern RESOURCE_EXPRESSION_REGEX = Pattern.compile(".*[^\\w\\.]resource[^\\w].*");
    
    private static final String RESOURCE = "resource";
    
    public static final String RESOURCE_LOCATION_KEY = "org.apache.myfaces.view.facelets.resource.location";
    
    public static final String RESOURCE_THIS_LIBRARY = "oam.resource.library";
    public static final String RESOURCE_THIS_CONTRACT = "oam.resource.contract";
    
    public static boolean isResourceExpression(String expression)
    {
        if (expression.contains(RESOURCE))
        {
            return RESOURCE_EXPRESSION_REGEX.matcher(expression).matches();
        }
        else
        {
            return false;
        }
    }

    public static Location getResourceLocationForResolver(FacesContext facesContext)
    {
        return (Location) facesContext.getAttributes().get(RESOURCE_LOCATION_KEY);
    }
    
    public static void saveResourceLocationForResolver(FacesContext facesContext, Location location)
    {
        facesContext.getAttributes().put(RESOURCE_LOCATION_KEY, location);
    }
    
    public static void removeResourceLocationForResolver(FacesContext facesContext)
    {
        facesContext.getAttributes().remove(RESOURCE_LOCATION_KEY);
    }

    public static String getResourceContractForResolver(FacesContext facesContext)
    {
        return (String) facesContext.getAttributes().get(RESOURCE_THIS_CONTRACT);
    }
    
    public static void saveResourceContractForResolver(FacesContext facesContext, String location)
    {
        facesContext.getAttributes().put(RESOURCE_THIS_CONTRACT, location);
    }
    
    public static void removeResourceContractForResolver(FacesContext facesContext)
    {
        facesContext.getAttributes().remove(RESOURCE_THIS_CONTRACT);
    }
    
    public static String getResourceLibraryForResolver(FacesContext facesContext)
    {
        return (String) facesContext.getAttributes().get(RESOURCE_THIS_LIBRARY);
    }
    
    public static void saveResourceLibraryForResolver(FacesContext facesContext, String location)
    {
        facesContext.getAttributes().put(RESOURCE_THIS_LIBRARY, location);
    }
    
    public static void removeResourceLibraryForResolver(FacesContext facesContext)
    {
        facesContext.getAttributes().remove(RESOURCE_THIS_LIBRARY);
    }
}
