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
package org.apache.myfaces.view.facelets.pool;

import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.view.facelets.tag.jsf.FaceletState;

/**
 * This class defines the necessary operations that a view pool should comply in
 * order to be used by MyFaces.
 * 
 * <p>A View Pool is a set of initialized views that are kept ready to use. The idea 
 * is reset and reuse views taking advantage of existing JSF 2.0 Partial State 
 * Saving algorithm with some small additions to component's saveState() method.
 * </p>
 * <p>This technique works by these reasons:
 * </p>
 * <ul>
 * <li>A view is composed by many small objects that are created multiple times per
 * each request, and the time spent creating these objects is usually larger than 
 * the time used to traverse the component tree.</li>
 * <li>The structure of a view usually does not change over application lifetime.</li>
 * <li>The "delta state" or in other words the elements that change in view are
 * small compared with the elements that does not change.</li>
 * <ul>
 * <p>
 * The implementation proposed uses a lock free view pool structure with soft or
 * weak references. The lock free view pool ensures fast access and the soft or weak
 * references ensured the garbage collection algorithm is not affected by the view
 * pool.
 * </p>
 *
 * @author Leonardo Uribe
 */
public abstract class ViewPool
{
    /**
     * Defines the number of views to be hold per each view metadata definition.
     * By default is 5. 
     * 
     * Usually a view is defined by its viewId, locale, renderKitId
     * and active contracts. If a view shares the same values for these parameters
     * belongs to the same group that can be pooled.
     */
    @JSFWebConfigParam(defaultValue="5", tags="performance")
    public static final String INIT_PARAM_VIEW_POOL_MAX_POOL_SIZE =
            "org.apache.myfaces.VIEW_POOL_MAX_POOL_SIZE";
    public static final int INIT_PARAM_VIEW_POOL_MAX_POOL_SIZE_DEFAULT = 5;
    
    /**
     * Defines the limit of the views that cannot be reused partially.
     */
    @JSFWebConfigParam(defaultValue="2", tags="performance")
    public static final String INIT_PARAM_VIEW_POOL_MAX_DYNAMIC_PARTIAL_LIMIT =
            "org.apache.myfaces.VIEW_POOL_MAX_DYNAMIC_PARTIAL_LIMIT";
    public static final int INIT_PARAM_VIEW_POOL_MAX_DYNAMIC_PARTIAL_LIMIT_DEFAULT = 2;
    
    /**
     * Defines the type of memory reference that is used to hold the view into memory. By
     * default a "soft" reference is used. 
     */
    @JSFWebConfigParam(defaultValue="soft", expectedValues="weak,soft", tags="performance")
    public static final String INIT_PARAM_VIEW_POOL_ENTRY_MODE =
            "org.apache.myfaces.VIEW_POOL_ENTRY_MODE";
    public static final String ENTRY_MODE_SOFT = "soft";
    public static final String ENTRY_MODE_WEAK = "weak";
    public static final String INIT_PARAM_VIEW_POOL_ENTRY_MODE_DEFAULT = ENTRY_MODE_SOFT;
    
    /**
     * Defines if the view pool uses deferred navigation to recycle views when navigation
     * is performed. The difference is a normal navigation is not done when the broadcast is
     * done but at the end of invoke application phase.
     */
    @JSFWebConfigParam(defaultValue="false", expectedValues="true, false", tags="performance")
    public static final String INIT_PARAM_VIEW_POOL_DEFERRED_NAVIGATION =
            "org.apache.myfaces.VIEW_POOL_DEFERRED_NAVIGATION";    
    
    /**
     * Indicate if the view pool uses deferred navigation.
     * 
     * @return 
     */
    public abstract boolean isDeferredNavigationEnabled();
    
    public abstract void storeStaticViewStructureMetadata(FacesContext context, 
        UIViewRoot root, FaceletState faceletState);
    
    public abstract ViewStructureMetadata retrieveStaticViewStructureMetadata(FacesContext context,
            UIViewRoot root);
    
    public abstract void pushStaticStructureView(FacesContext context, UIViewRoot root);

    public abstract void pushPartialStructureView(FacesContext context, UIViewRoot root);
    
    public abstract ViewEntry popStaticOrPartialStructureView(FacesContext context, UIViewRoot root);

    public abstract boolean isWorthToRecycleThisView(FacesContext context, UIViewRoot root);
    
    public abstract void storeDynamicViewStructureMetadata(FacesContext context, 
            UIViewRoot root, FaceletState faceletState);
    
    public abstract ViewStructureMetadata retrieveDynamicViewStructureMetadata(FacesContext context,
            UIViewRoot root, FaceletState faceletState);

    public abstract void pushDynamicStructureView(FacesContext context, UIViewRoot root, 
            FaceletState faceletDynamicState);
    
    public abstract ViewEntry popDynamicStructureView(FacesContext context, UIViewRoot root,
            FaceletState faceletDynamicState);

}
