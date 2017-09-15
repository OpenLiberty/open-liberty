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
package javax.faces.view;

import java.beans.BeanInfo;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.faces.FacesWrapper;
import javax.faces.application.Resource;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

/**
 * @since 2.2
 */
public abstract class ViewDeclarationLanguageWrapper extends ViewDeclarationLanguage 
    implements FacesWrapper<ViewDeclarationLanguage>
{

    public void buildView(FacesContext context, UIViewRoot view) throws IOException
    {
        getWrapped().buildView(context, view);
    }

    public UIViewRoot createView(FacesContext context, String viewId)
    {
        return getWrapped().createView(context, viewId);
    }

    public BeanInfo getComponentMetadata(FacesContext context, Resource componentResource)
    {
        return getWrapped().getComponentMetadata(context, componentResource);
    }

    public Resource getScriptComponentResource(FacesContext context, Resource componentResource)
    {
        return getWrapped().getScriptComponentResource(context, componentResource);
    }

    public StateManagementStrategy getStateManagementStrategy(FacesContext context, String viewId)
    {
        return getWrapped().getStateManagementStrategy(context, viewId);
    }

    public ViewMetadata getViewMetadata(FacesContext context, String viewId)
    {
        return getWrapped().getViewMetadata(context, viewId);
    }

    public void renderView(FacesContext context, UIViewRoot view) throws IOException
    {
        getWrapped().renderView(context, view);
    }

    public UIViewRoot restoreView(FacesContext context, String viewId)
    {
        return getWrapped().restoreView(context, viewId);
    }

    public void retargetAttachedObjects(FacesContext context, UIComponent topLevelComponent, 
        List<AttachedObjectHandler> handlers)
    {
        getWrapped().retargetAttachedObjects(context, topLevelComponent, handlers);
    }

    public void retargetMethodExpressions(FacesContext context, UIComponent topLevelComponent)
    {
        getWrapped().retargetMethodExpressions(context, topLevelComponent);
    }

    public String getId()
    {
        return getWrapped().getId();
    }

    public boolean viewExists(FacesContext facesContext, String viewId)
    {
        return getWrapped().viewExists(facesContext, viewId);
    }

    public UIComponent createComponent(FacesContext context, String taglibURI, String tagName,
        Map<String, Object> attributes)
    {
        return getWrapped().createComponent(context, taglibURI, tagName, attributes);
    }

    public List<String> calculateResourceLibraryContracts(FacesContext context, String viewId)
    {
        return getWrapped().calculateResourceLibraryContracts(context, viewId);
    }
    
    public abstract ViewDeclarationLanguage getWrapped();
}
