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
package org.apache.myfaces.shared.renderkit.html;

import org.apache.myfaces.shared.renderkit.JSFAttr;

import javax.faces.application.ViewHandler;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.render.Renderer;
import java.io.IOException;
import java.util.List;

public abstract class HtmlRenderer
        extends Renderer
{

    /**
     * Return the list of children of the specified component.
     * <p>
     * This default implementation simply returns component.getChildren().
     * However this method should always be used in order to allow
     * renderer subclasses to override it and provide filtered or
     * reordered views of the component children to rendering
     * methods defined in their ancestor classes.
     * <p>
     * Any method that overrides this to "hide" child components
     * should also override the getChildCount method.
     * 
     * @return a list of UIComponent objects.
     */
    public List<UIComponent> getChildren(UIComponent component) 
    {
        return component.getChildren();
    }

    /**
     * Return the number of children of the specified component.
     * <p>
     * See {@link #getChildren(UIComponent)} for more information.
     */
    public int getChildCount(UIComponent component) 
    {
        return component.getChildCount();
    }
    
    /**
     * @param facesContext
     * @return String A String representing the action URL
     */
    protected String getActionUrl(FacesContext facesContext)
    {
        ViewHandler viewHandler = facesContext.getApplication().getViewHandler();
        String viewId = facesContext.getViewRoot().getViewId();
        return viewHandler.getActionURL(facesContext, viewId);
    }

    /**
     * Renders the client ID as an "id".
     */
    protected void renderId(
      FacesContext context,
      UIComponent  component) throws IOException
    {
      if (shouldRenderId(context, component))
      {
        String clientId = getClientId(context, component);
        context.getResponseWriter().writeAttribute(HTML.ID_ATTR, clientId, JSFAttr.ID_ATTR);
      }
    }

    /**
     * Returns the client ID that should be used for rendering (if
     * {@link #shouldRenderId} returns true).
     */
    protected String getClientId(
      FacesContext context,
      UIComponent  component)
    {
      return component.getClientId(context);
    }

    /**
     * Returns true if the component should render an ID.  Components
     * that deliver events should always return "true".
     * @todo Is this a bottleneck?  If so, optimize!
     */
    protected boolean shouldRenderId(
      FacesContext context,
      UIComponent  component)
    {
      String id = component.getId();

      // Otherwise, if ID isn't set, don't bother
      if (id == null)
      {
          return false;
      }

      // ... or if the ID was generated, don't bother
      if (id.startsWith(UIViewRoot.UNIQUE_ID_PREFIX))
      {
          return false;
      }

      return true;
    }

    /**
     * Coerces an object into a URI, accounting for JSF rules
     * with initial slashes.
     */
    static public String toUri(Object o)
    {
      if (o == null)
      {
          return null;
      }

      String uri = o.toString();
      if (uri.startsWith("/"))
      {
        // Treat two slashes as server-relative
        if (uri.startsWith("//"))
        {
          uri = uri.substring(1);
        }
        else
        {
          FacesContext fContext = FacesContext.getCurrentInstance();
          uri = fContext.getExternalContext().getRequestContextPath() + uri;
        }
      }

      return uri;
    }
    
    protected boolean isCommonPropertiesOptimizationEnabled(FacesContext facesContext)
    {
        return false;
    }
    
    protected boolean isCommonEventsOptimizationEnabled(FacesContext facesContext)
    {
        return false;
    }
}
