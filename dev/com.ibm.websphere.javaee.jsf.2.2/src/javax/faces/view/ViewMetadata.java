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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import javax.faces.component.UIComponent;
import javax.faces.component.UIViewAction;
import javax.faces.component.UIViewParameter;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

/**
 * @since 2.0
 */
public abstract class ViewMetadata
{
    public abstract UIViewRoot createMetadataView(FacesContext context);
    
    public abstract String getViewId();
    
    public static Collection<UIViewParameter> getViewParameters(UIViewRoot root)
    {
        LinkedList<UIViewParameter> result = null;
        UIComponent metadataFacet = root.getFacet (UIViewRoot.METADATA_FACET_NAME);
        Iterator<UIComponent> children;
        
        if (metadataFacet == null)
        {
             // No metadata, so return an empty collection.
             
             return Collections.emptyList();
        }
        
        // Iterate over all the children, keep only the view parameters.
        
        if (metadataFacet.getChildCount() > 0)
        {
            children = metadataFacet.getChildren().iterator();
            
            while (children.hasNext())
            {
                 UIComponent component = children.next();
                 
                 if (result == null)
                 {
                     result = new LinkedList<UIViewParameter>();
                 }
                 
                 if (component instanceof UIViewParameter)
                 {
                      result.add ((UIViewParameter) component);
                 }
            }
        }
        
        // TODO: does this need to be immutable?  Spec does not indicate either
        // way.
        if (result == null)
        {
            return Collections.emptyList();
        }
        else
        {
            return Collections.unmodifiableCollection (result);
        }
    }
    
    /**
     * @since 2.2
     * @param root
     * @return 
     */
    public static Collection<UIViewAction> getViewActions(UIViewRoot root)
    {
        LinkedList<UIViewAction> result = null;
        UIComponent metadataFacet = root.getFacet (UIViewRoot.METADATA_FACET_NAME);
        Iterator<UIComponent> children;
        
        if (metadataFacet == null)
        {
             // No metadata, so return an empty collection.
             
             return Collections.emptyList();
        }
        
        // Iterate over all the children, keep only the view parameters.
        
        if (metadataFacet.getChildCount() > 0)
        {
            children = metadataFacet.getChildren().iterator();
            
            while (children.hasNext())
            {
                 UIComponent component = children.next();
                 
                 if (result == null)
                 {
                     result = new LinkedList<UIViewAction>();
                 }
                 
                 if (component instanceof UIViewAction)
                 {
                      result.add ((UIViewAction) component);
                 }
            }
        }
        
        // TODO: does this need to be immutable?  Spec does not indicate either
        // way.
        if (result == null)
        {
            return Collections.emptyList();
        }
        else
        {
            return Collections.unmodifiableCollection (result);
        }
    }
    
    /**
     * @since 2.2
     * @param root
     * @return 
     */
    public static boolean hasMetadata(UIViewRoot root)
    {
        UIComponent metadataFacet = root.getFacet(UIViewRoot.METADATA_FACET_NAME);
        return metadataFacet != null ? metadataFacet.getChildCount() > 0 : false;
    }
}
