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
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.Resource;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

/**
 * @since 2.0
 */
public abstract class ViewDeclarationLanguage
{
    /**
     * @since 2.1
     */
    public static final String JSP_VIEW_DECLARATION_LANGUAGE_ID = "java.faces.JSP";

    /**
     * @since 2.1
     */
    public static final String FACELETS_VIEW_DECLARATION_LANGUAGE_ID = "java.faces.Facelets";
    
    public abstract void buildView(FacesContext context, UIViewRoot view) throws IOException;

    public abstract UIViewRoot createView(FacesContext context, String viewId);

    public abstract BeanInfo getComponentMetadata(FacesContext context, Resource componentResource);

    public abstract Resource getScriptComponentResource(FacesContext context, Resource componentResource);
    
    public abstract StateManagementStrategy getStateManagementStrategy(FacesContext context, String viewId); 

    public abstract ViewMetadata getViewMetadata(FacesContext context, String viewId);

    public abstract void renderView(FacesContext context, UIViewRoot view) throws IOException;

    public abstract UIViewRoot restoreView(FacesContext context, String viewId);
    
    public void retargetAttachedObjects(FacesContext context, UIComponent topLevelComponent,
                                        List<AttachedObjectHandler> handlers)
    {
        throw new UnsupportedOperationException(); 
    }

    public void retargetMethodExpressions(FacesContext context, UIComponent topLevelComponent)
    {
        throw new UnsupportedOperationException(); 
    }
    
    /**
     * 
     * @since 2.1
     * @return
     */
    public String getId()
    {
        return this.getClass().getName();
    }
    
    /**
     * 
     * @since 2.1
     * @param facesContext
     * @param viewId
     * @return
     */
    public boolean viewExists(FacesContext facesContext, String viewId)
    {
        try
        {
            return facesContext.getExternalContext().getResource(viewId) != null;
        }
        catch (MalformedURLException e)
        {
            Logger log = Logger.getLogger(ViewDeclarationLanguage.class.getName());
            if (log.isLoggable(Level.SEVERE))
            {
                log.log(Level.SEVERE, "Malformed URL viewId: "+viewId, e);
            }
        }
        return false;
    }
    
    /**
     * @since 2.2
     * @param context
     * @param taglibURI
     * @param tagName
     * @param attributes
     * @return 
     */
    public UIComponent createComponent(FacesContext context,
                                   String taglibURI,
                                   String tagName,
                                   Map<String,Object> attributes)
    {
        return null;
    }
    
    /**
     * @since 2.2
     * @param context
     * @param viewId
     * @return 
     */
    public List<String> calculateResourceLibraryContracts(FacesContext context,
                                                      String viewId)
    {
        return null;
    }
}
