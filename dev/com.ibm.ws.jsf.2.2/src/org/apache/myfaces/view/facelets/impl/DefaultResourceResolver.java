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

import java.net.URL;

import javax.faces.application.ViewResource;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.ResourceResolver;
import org.apache.myfaces.view.facelets.FaceletFactory;


public class DefaultResourceResolver extends ResourceResolver
{

    public DefaultResourceResolver()
    {
        super();
    }

    public URL resolveUrl(String path)
    {
        return resolveUrl(FacesContext.getCurrentInstance(), path);
    }
    
    public URL resolveUrl(FacesContext facesContext, String path)
    {
        /*
        try
        {
            return Resource.getResourceUrl(FacesContext.getCurrentInstance(), path);
        }
        catch (IOException e)
        {
            throw new FacesException(e);
        }*/
        ViewResource resource = facesContext.getApplication().
            getResourceHandler().createViewResource(facesContext, path);
        //return resource == null ? null : resource.getURL();
        if (resource != null)
        {
            facesContext.getAttributes().put(FaceletFactory.LAST_RESOURCE_RESOLVED, resource);
            return resource.getURL();
        }
        else
        {
            return null;
        }
    }

    public String toString()
    {
        return "DefaultResourceResolver";
    }

}
