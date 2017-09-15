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
package org.apache.myfaces.application;

import org.apache.myfaces.shared.util.ClassUtils;

import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Manfred Geiler (latest modification by $Author: lu4242 $)
 * @version $Revision: 1470770 $ $Date: 2013-04-23 01:08:02 +0000 (Tue, 23 Apr 2013) $
 */
public class TreeStructureManager
{
    //private static final Log log = LogFactory.getLog(TreeStructureManager.class);

    //private FacesContext _facesContext;

    public TreeStructureManager()
    {
        //_facesContext = facesContext;
    }

    public Object buildTreeStructureToSave(UIViewRoot viewRoot)
    {
        return internalBuildTreeStructureToSave(viewRoot);
    }

    private TreeStructComponent internalBuildTreeStructureToSave(UIComponent component)
    {
        TreeStructComponent structComp = new TreeStructComponent(component.getClass().getName(),
                                                                 component.getId());

        //children
        if (component.getChildCount() > 0)
        {
            List<TreeStructComponent> structChildList = new ArrayList<TreeStructComponent>();
            for (int i = 0, childCount = component.getChildCount(); i < childCount; i++)
            {
                UIComponent child = component.getChildren().get(i);
                if (!child.isTransient())
                {
                    TreeStructComponent structChild = internalBuildTreeStructureToSave(child);
                    structChildList.add(structChild);
                }
            }
            
            TreeStructComponent[] childArray = structChildList.toArray(new TreeStructComponent[structChildList.size()]);
            structComp.setChildren(childArray);
        }

        //facets
        
        if (component.getFacetCount() > 0)
        {
            Map<String, UIComponent> facetMap = component.getFacets();
            List<Object[]> structFacetList = new ArrayList<Object[]>();
            for (Map.Entry<String, UIComponent> entry : facetMap.entrySet())
            {
                UIComponent child = entry.getValue();
                if (!child.isTransient())
                {
                    String facetName = entry.getKey();
                    TreeStructComponent structChild = internalBuildTreeStructureToSave(child);
                    structFacetList.add(new Object[] {facetName, structChild});
                }
            }
            
            Object[] facetArray = structFacetList.toArray(new Object[structFacetList.size()]);
            structComp.setFacets(facetArray);
        }

        return structComp;
    }


    public UIViewRoot restoreTreeStructure(Object treeStructRoot)
    {
        if (treeStructRoot instanceof TreeStructComponent)
        {
            return (UIViewRoot)internalRestoreTreeStructure((TreeStructComponent)treeStructRoot, true);
        }
        
        
        throw new IllegalArgumentException("TreeStructure of type " + treeStructRoot.getClass().getName() + 
                                           " is not supported.");
        
    }

    private UIComponent internalRestoreTreeStructure(TreeStructComponent treeStructComp, boolean checkViewRoot)
    {
        String compClass = treeStructComp.getComponentClass();
        String compId = treeStructComp.getComponentId();
        UIComponent component = (UIComponent)ClassUtils.newInstance(compClass);
        component.setId(compId);

        if (checkViewRoot && component instanceof UIViewRoot)
        {
            FacesContext.getCurrentInstance().setViewRoot((UIViewRoot) component);
        }
        //children
        TreeStructComponent[] childArray = treeStructComp.getChildren();
        if (childArray != null)
        {
            List<UIComponent> childList = component.getChildren();
            for (int i = 0, len = childArray.length; i < len; i++)
            {
                UIComponent child = internalRestoreTreeStructure(childArray[i], false);
                childList.add(child);
            }
        }

        //facets
        Object[] facetArray = treeStructComp.getFacets();
        if (facetArray != null)
        {
            Map<String, UIComponent> facetMap = component.getFacets();
            for (int i = 0, len = facetArray.length; i < len; i++)
            {
                Object[] tuple = (Object[])facetArray[i];
                String facetName = (String)tuple[0];
                TreeStructComponent structChild = (TreeStructComponent)tuple[1];
                UIComponent child = internalRestoreTreeStructure(structChild, false);
                facetMap.put(facetName, child);
            }
        }

        return component;
    }


    public static class TreeStructComponent
            implements Serializable
    {
        private static final long serialVersionUID = 5069109074684737231L;
        private String _componentClass;
        private String _componentId;
        private TreeStructComponent[] _children = null;    // Array of children
        private Object[] _facets = null;            // Array of Array-tuples with Facetname and TreeStructComponent

        TreeStructComponent(String componentClass, String componentId)
        {
            _componentClass = componentClass;
            _componentId = componentId;
        }

        public String getComponentClass()
        {
            return _componentClass;
        }

        public String getComponentId()
        {
            return _componentId;
        }

        void setChildren(TreeStructComponent[] children)
        {
            _children = children;
        }

        TreeStructComponent[] getChildren()
        {
            return _children;
        }

        Object[] getFacets()
        {
            return _facets;
        }

        void setFacets(Object[] facets)
        {
            _facets = facets;
        }
    }

}
