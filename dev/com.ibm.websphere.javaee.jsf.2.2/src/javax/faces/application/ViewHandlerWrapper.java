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

package javax.faces.application;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.faces.FacesException;
import javax.faces.FacesWrapper;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewDeclarationLanguage;

/**
 * see Javadoc of <a href="http://java.sun.com/j2ee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 */
public abstract class ViewHandlerWrapper extends ViewHandler
    implements FacesWrapper<ViewHandler>
{

    @Override
    public String calculateCharacterEncoding(FacesContext context)
    {
        return getWrapped().calculateCharacterEncoding(context);
    }

    @Override
    public void initView(FacesContext context) throws FacesException
    {
        getWrapped().initView(context);
    }

    public abstract ViewHandler getWrapped();

    @Override
    public void renderView(FacesContext context, UIViewRoot viewToRender) throws IOException, FacesException
    {
        getWrapped().renderView(context, viewToRender);
    }

    @Override
    public void writeState(FacesContext context) throws IOException
    {
        getWrapped().writeState(context);
    }

    @Override
    public String calculateRenderKitId(FacesContext context)
    {
        return getWrapped().calculateRenderKitId(context);
    }

    @Override
    public Locale calculateLocale(FacesContext context)
    {
        return getWrapped().calculateLocale(context);
    }

    @Override
    public UIViewRoot restoreView(FacesContext context, String viewId)
    {
        return getWrapped().restoreView(context, viewId);
    }

    @Override
    public String getResourceURL(FacesContext context, String path)
    {
        return getWrapped().getResourceURL(context, path);
    }

    @Override
    public String getActionURL(FacesContext context, String viewId)
    {
        return getWrapped().getActionURL(context, viewId);
    }

    @Override
    public UIViewRoot createView(FacesContext context, String viewId)
    {
        return getWrapped().createView(context, viewId);
    }

    @Override
    public String deriveViewId(FacesContext context, String input)
    {
        return getWrapped().deriveViewId(context, input);
    }

    @Override
    public String deriveLogicalViewId(FacesContext context, String rawViewId)
    {
        return getWrapped().deriveLogicalViewId(context, rawViewId);
    }

    @Override
    public String getBookmarkableURL(FacesContext context, String viewId,
            Map<String, List<String>> parameters, boolean includeViewParams)
    {
        return getWrapped().getBookmarkableURL(context, viewId, parameters, includeViewParams);
    }

    @Override
    public String getRedirectURL(FacesContext context, String viewId,
            Map<String, List<String>> parameters, boolean includeViewParams)
    {
        return getWrapped().getRedirectURL(context, viewId, parameters, includeViewParams);
    }

    @Override
    public ViewDeclarationLanguage getViewDeclarationLanguage(
            FacesContext context, String viewId)
    {
        return getWrapped().getViewDeclarationLanguage(context, viewId);
    }

    @Override
    public Set<String> getProtectedViewsUnmodifiable()
    {
        return getWrapped().getProtectedViewsUnmodifiable();
    }

    @Override
    public boolean removeProtectedView(String urlPattern)
    {
        return getWrapped().removeProtectedView(urlPattern);
    }

    @Override
    public void addProtectedView(String urlPattern)
    {
        getWrapped().addProtectedView(urlPattern);
    }

}
