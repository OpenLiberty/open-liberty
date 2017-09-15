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
package org.apache.myfaces.view.facelets.pool.impl;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import org.apache.myfaces.context.RequestViewContext;
import org.apache.myfaces.shared.util.WebConfigParamUtils;
import org.apache.myfaces.view.facelets.pool.RestoreViewFromPoolResult;
import org.apache.myfaces.view.facelets.pool.ViewPool;
import org.apache.myfaces.view.facelets.pool.ViewEntry;
import org.apache.myfaces.view.facelets.pool.ViewStructureMetadata;
import org.apache.myfaces.view.facelets.tag.jsf.FaceletState;

/**
 *
 * @author Leonardo Uribe
 */
public class ViewPoolImpl extends ViewPool
{

    
    private static final String SKIP_VIEW_MAP_SAVE_STATE = "oam.viewPool.SKIP_VIEW_MAP_SAVE_STATE";    
            
    private Map<MetadataViewKey, ViewPoolEntryHolder > staticStructureViewPool;
    
    private Map<MetadataViewKey, Map<DynamicViewKey, ViewPoolEntryHolder>> dynamicStructureViewPool;
    
    private Map<MetadataViewKey, ViewPoolEntryHolder > partialStructureViewPool;
    
    private final int maxCount;
    private final int dynamicPartialLimit;
    
    private final boolean entryWeak;
    private final boolean deferredNavigation;
    
    // View metadata
    private Map<MetadataViewKey, ViewStructureMetadata> staticStructureViewMetadataMap;
    private Map<MetadataViewKey, Map<DynamicViewKey, ViewStructureMetadata>> 
            dynamicStructureViewMetadataMap;
    
    public ViewPoolImpl(FacesContext facesContext, Map<String, String> parameters)
    {
        staticStructureViewPool = new ConcurrentHashMap<MetadataViewKey, ViewPoolEntryHolder>();
        partialStructureViewPool = new ConcurrentHashMap<MetadataViewKey, ViewPoolEntryHolder>();
        dynamicStructureViewPool = new ConcurrentHashMap<MetadataViewKey, Map<DynamicViewKey, ViewPoolEntryHolder>>();
        maxCount = WebConfigParamUtils.getIntegerInitParameter(facesContext.getExternalContext(),
                INIT_PARAM_VIEW_POOL_MAX_POOL_SIZE, 
                parameters.containsKey(INIT_PARAM_VIEW_POOL_MAX_POOL_SIZE) ? 
                    Integer.parseInt(parameters.get(INIT_PARAM_VIEW_POOL_MAX_POOL_SIZE)) :
                    INIT_PARAM_VIEW_POOL_MAX_POOL_SIZE_DEFAULT);
        dynamicPartialLimit = WebConfigParamUtils.getIntegerInitParameter(facesContext.getExternalContext(),
                INIT_PARAM_VIEW_POOL_MAX_DYNAMIC_PARTIAL_LIMIT, 
                parameters.containsKey(INIT_PARAM_VIEW_POOL_MAX_DYNAMIC_PARTIAL_LIMIT) ?
                Integer.parseInt(parameters.get(INIT_PARAM_VIEW_POOL_MAX_DYNAMIC_PARTIAL_LIMIT)) : 
                INIT_PARAM_VIEW_POOL_MAX_DYNAMIC_PARTIAL_LIMIT_DEFAULT);
        String entryMode = WebConfigParamUtils.getStringInitParameter(facesContext.getExternalContext(),
                INIT_PARAM_VIEW_POOL_ENTRY_MODE,
                parameters.containsKey(INIT_PARAM_VIEW_POOL_ENTRY_MODE) ?
                parameters.get(INIT_PARAM_VIEW_POOL_ENTRY_MODE) :
                INIT_PARAM_VIEW_POOL_ENTRY_MODE_DEFAULT);
        entryWeak = ENTRY_MODE_WEAK.equals(entryMode);
        String deferredNavigationVal = WebConfigParamUtils.getStringInitParameter(facesContext.getExternalContext(),
                INIT_PARAM_VIEW_POOL_DEFERRED_NAVIGATION,
                parameters.containsKey(INIT_PARAM_VIEW_POOL_DEFERRED_NAVIGATION) ?
                parameters.get(INIT_PARAM_VIEW_POOL_DEFERRED_NAVIGATION) :
                "false");
        deferredNavigation = Boolean.valueOf(deferredNavigationVal);
        
        staticStructureViewMetadataMap = new ConcurrentHashMap<MetadataViewKey, ViewStructureMetadata>();
        dynamicStructureViewMetadataMap = new ConcurrentHashMap<MetadataViewKey, 
                Map<DynamicViewKey, ViewStructureMetadata>>();
    }
    
    protected void pushStaticStructureView(FacesContext context, MetadataViewKey key, ViewEntry entry)
    {
        ViewPoolEntryHolder q = staticStructureViewPool.get(key);
        if (q == null)
        {
            q = new ViewPoolEntryHolder(maxCount);
            staticStructureViewPool.put(key, q);
        }
        q.add(entry);
    }
    
    protected ViewEntry popStaticStructureView(FacesContext context, MetadataViewKey key)
    {
        ViewPoolEntryHolder q = staticStructureViewPool.get(key);
        if (q == null)
        {
            return null;
        }
        ViewEntry entry = q.poll();
        if (entry == null)
        {
            return null;
        }
        do
        {
            if (entry.activate())
            {
                return entry;
            }
            entry = q.poll();
        }
        while (entry != null);
        return null;
    }
    
    protected void pushPartialStructureView(FacesContext context, MetadataViewKey key, ViewEntry entry)
    {
        ViewPoolEntryHolder q = partialStructureViewPool.get(key);
        if (q == null)
        {
            q = new ViewPoolEntryHolder(maxCount);
            partialStructureViewPool.put(key, q);
        }
        q.add(entry);
    }
    
    protected ViewEntry popPartialStructureView(FacesContext context, MetadataViewKey key)
    {
        ViewPoolEntryHolder q = partialStructureViewPool.get(key);
        if (q == null)
        {
            return null;
        }
        ViewEntry entry = q.poll();
        if (entry == null)
        {
            return null;
        }
        do
        {
            if (entry.activate())
            {
                return entry;
            }
            entry = q.poll();
        }while (entry != null);
        return null;
    }

    /**
     * Generates an unique key according to the metadata information stored
     * in the passed UIViewRoot instance that can affect the way how the view is generated. 
     * By default, the "view" params are the viewId, the locale, the renderKit and 
     * the contracts associated to the view.
     * 
     * @param facesContext
     * @param root
     * @return 
     */
    protected MetadataViewKey deriveViewKey(FacesContext facesContext,
                    UIViewRoot root)
    {
        MetadataViewKey viewKey; 
        if (!facesContext.getResourceLibraryContracts().isEmpty())
        {
            String[] contracts = new String[facesContext.getResourceLibraryContracts().size()];
            contracts = facesContext.getResourceLibraryContracts().toArray(contracts);
            viewKey = new MetadataViewKeyImpl(root.getViewId(), root.getRenderKitId(), root.getLocale(), contracts);
        }
        else
        {
            viewKey = new MetadataViewKeyImpl(root.getViewId(), root.getRenderKitId(), root.getLocale());
        }
        return viewKey;
    }

    protected ViewEntry generateViewEntry(FacesContext facesContext,
                    UIViewRoot root)
    {
        return entryWeak ? new WeakViewEntry(root) : new SoftViewEntry(root);
    }

    protected DynamicViewKey generateDynamicStructureViewKey(FacesContext facesContext, UIViewRoot root,
            FaceletState faceletDynamicState)
    {
        return new DynamicViewKey(faceletDynamicState);
    }

    protected void pushDynamicStructureView(FacesContext context, UIViewRoot root, DynamicViewKey key, ViewEntry entry)
    {
        MetadataViewKey ordinaryKey = deriveViewKey(context, root);
        Map<DynamicViewKey, ViewPoolEntryHolder> map = dynamicStructureViewPool.get(ordinaryKey);
        if (map == null)
        {
            map = new ConcurrentHashMap<DynamicViewKey, ViewPoolEntryHolder>();
            dynamicStructureViewPool.put(ordinaryKey, map);
        }
        ViewPoolEntryHolder q = map.get(key);
        if (q == null)
        {
            q = new ViewPoolEntryHolder(maxCount);
            map.put(key, q);
        }
        if (!q.add(entry))
        {
            pushPartialStructureView(context, ordinaryKey, entry);
        }
    }

    protected ViewEntry popDynamicStructureView(FacesContext context, UIViewRoot root, DynamicViewKey key)
    {
        MetadataViewKey ordinaryKey = deriveViewKey(context, root);
        Map<DynamicViewKey, ViewPoolEntryHolder> map = dynamicStructureViewPool.get(ordinaryKey);
        if (map == null)
        {
            return null;
        }
        ViewPoolEntryHolder q = map.get(key);
        if (q == null)
        {
            return null;
        }
        ViewEntry entry = q.poll();
        while (entry != null)
        {
            if (entry.activate())
            {
                return entry;
            }
            entry = q.poll();
        }
        return null;
    }

    @Override
    public void pushStaticStructureView(FacesContext context, UIViewRoot root)
    {
        MetadataViewKey key = deriveViewKey(context, root);
        if (staticStructureViewMetadataMap.containsKey(key))
        {
            ViewEntry value = generateViewEntry(context, root);
            pushStaticStructureView(context, key, value);
        }
    }

    @Override
    public ViewEntry popStaticOrPartialStructureView(FacesContext context, UIViewRoot root)
    {
        MetadataViewKey key = deriveViewKey(context, root);
        ViewEntry entry = popStaticStructureView(context, key);
        if (entry != null)
        {
            entry.setResult(RestoreViewFromPoolResult.COMPLETE);
        }
        else
        {
            entry = popPartialStructureView(context, key);
            if (entry != null)
            {
                entry.setResult(RestoreViewFromPoolResult.REFRESH_REQUIRED);
            }
            else
            {
                Map<DynamicViewKey, ViewPoolEntryHolder> map = dynamicStructureViewPool.get(key);
                if (map != null)
                {
                    try
                    {
                        ViewPoolEntryHolder maxEntry = null;
                        long max = -1;
                        for (Iterator<ViewPoolEntryHolder> it = map.values().iterator(); it.hasNext();)
                        {
                            ViewPoolEntryHolder e = it.next();
                            long count = e.getCount();
                            if (count > max && count > dynamicPartialLimit)
                            {
                                maxEntry = e;
                                max = count;
                            }
                        }
                        if (maxEntry != null)
                        {
                            entry = maxEntry.poll();
                            if (entry != null)
                            {
                                do
                                {
                                    if (entry.activate())
                                    {
                                        break;
                                    }
                                    entry = maxEntry.poll();
                                }
                                while (entry != null);
                                if (entry != null)
                                {
                                    entry.setResult(RestoreViewFromPoolResult.REFRESH_REQUIRED);
                                }
                            }
                        }
                    }
                    catch(ConcurrentModificationException ex)
                    {
                        //do nothing
                    }
                }
            }
        }
        return entry;
    }

    @Override
    public void pushDynamicStructureView(FacesContext context, UIViewRoot root, 
            FaceletState faceletDynamicState)
    {
        DynamicViewKey key = (DynamicViewKey) generateDynamicStructureViewKey(context, root, faceletDynamicState);
        MetadataViewKey ordinaryKey = deriveViewKey(context, root);
        Map<DynamicViewKey, ViewStructureMetadata> map = dynamicStructureViewMetadataMap.get(ordinaryKey);
        if (map != null)
        {
            ViewEntry value = generateViewEntry(context, root);
            pushDynamicStructureView(context, root, key, value);
        }        
    }

    @Override
    public ViewEntry popDynamicStructureView(FacesContext context, UIViewRoot root, 
            FaceletState faceletDynamicState)
    {
        DynamicViewKey key = generateDynamicStructureViewKey(context, root, faceletDynamicState);
        ViewEntry entry = popDynamicStructureView(context, root, key);
        if (entry != null)
        {
            entry.setResult(RestoreViewFromPoolResult.COMPLETE);
        }
        return entry;
    }

    @Override
    public void pushPartialStructureView(FacesContext context, UIViewRoot root)
    {
        MetadataViewKey key = deriveViewKey(context, root);
        ViewEntry value = generateViewEntry(context, root);
        pushPartialStructureView(context, key, value);
    }

    @Override
    public boolean isWorthToRecycleThisView(FacesContext context, UIViewRoot root)
    {
        MetadataViewKey key = deriveViewKey(context, root);
        ViewPoolEntryHolder q = partialStructureViewPool.get(key);
        if (q != null && q.isFull())
        {
            return false;
        }
        return true;
    }

    @Override
    public void storeStaticViewStructureMetadata(FacesContext context, UIViewRoot root,
                FaceletState faceletState)
    {
        MetadataViewKey key = deriveViewKey(context, root);
        if (!staticStructureViewMetadataMap.containsKey(key))
        {
            RequestViewContext rvc = RequestViewContext.getCurrentInstance(context);
            Object state = saveViewRootState(context, root);
            ViewStructureMetadata metadata = new ViewStructureMetadataImpl(state, 
                    rvc.getRequestViewMetadata().cloneInstance());
            staticStructureViewMetadataMap.put(key, metadata);
        }
    }

    @Override
    public ViewStructureMetadata retrieveStaticViewStructureMetadata(FacesContext context, UIViewRoot root)
    {
        MetadataViewKey key = deriveViewKey(context, root);
        return staticStructureViewMetadataMap.get(key);
    }

    private Object saveViewRootState(FacesContext context, UIViewRoot root)
    {
        Object state;
        if (root.getViewMap(false) != null)
        {
            try
            {
                context.getAttributes().put(SKIP_VIEW_MAP_SAVE_STATE, Boolean.TRUE);
                state = root.saveState(context);
            }
            finally
            {
                context.getAttributes().remove(SKIP_VIEW_MAP_SAVE_STATE);
            }
        }
        else
        {
            state = root.saveState(context);
        }
        return state;
    }
    
    @Override
    public void storeDynamicViewStructureMetadata(FacesContext context, UIViewRoot root,
            FaceletState faceletDynamicState)
    {
        DynamicViewKey key = (DynamicViewKey) generateDynamicStructureViewKey(context, root, faceletDynamicState);
        MetadataViewKey ordinaryKey = deriveViewKey(context, root);
        if (!dynamicStructureViewMetadataMap.containsKey(ordinaryKey))
        {
            
            Map<DynamicViewKey, ViewStructureMetadata> map = dynamicStructureViewMetadataMap.get(ordinaryKey);
            if (map == null)
            {
                map = new ConcurrentHashMap<DynamicViewKey, ViewStructureMetadata>();
                dynamicStructureViewMetadataMap.put(ordinaryKey, map);
            }
            RequestViewContext rvc = RequestViewContext.getCurrentInstance(context);
            
            Object state = saveViewRootState(context, root);

            ViewStructureMetadata metadata = new ViewStructureMetadataImpl(state, 
                    rvc.getRequestViewMetadata().cloneInstance());
            map.put(key, metadata);
        }
    }

    @Override
    public ViewStructureMetadata retrieveDynamicViewStructureMetadata(FacesContext context, UIViewRoot root,
            FaceletState  faceletDynamicState)
    {
        DynamicViewKey key = (DynamicViewKey) generateDynamicStructureViewKey(context, root, faceletDynamicState);
        MetadataViewKey ordinaryKey = deriveViewKey(context, root);
        Map<DynamicViewKey, ViewStructureMetadata> map = dynamicStructureViewMetadataMap.get(ordinaryKey);
        if (map != null)
        {
            return map.get(key);
        }
        return null;
    }

    /**
     * @return the deferredNavigation
     */
    @Override
    public boolean isDeferredNavigationEnabled()
    {
        return deferredNavigation;
    }
    
}
