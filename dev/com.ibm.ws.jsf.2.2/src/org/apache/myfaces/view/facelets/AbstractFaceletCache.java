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
package org.apache.myfaces.view.facelets;

import java.io.IOException;
import java.net.URL;
import javax.faces.view.facelets.FaceletCache;
import javax.faces.view.facelets.FaceletContext;

/**
 * Extended FaceletCache contract that supports additional Myfaces specific concepts
 * that are necessary to implement.
 *
 * @author Leonardo Uribe
 * @since 2.1.12
 */
public abstract class AbstractFaceletCache<V> extends FaceletCache<V>
{
    private FaceletCache.MemberFactory<V> _compositeComponentMetadataFaceletFactory;

    /**
     * Retrieve a Facelet instance from the cache given the passed url, but taking into
     * account the facelet context too, so the cache can implement special rules 
     * according to the context for recompile the facelet if necessary.
     * 
     * @param ctx
     * @param url
     * @return
     * @throws IOException 
     */
    public V getFacelet(FaceletContext ctx, URL url) throws IOException
    {
        return getFacelet(url);
    }
    
    /**
     * Retrieve or create a Facelet instance used to create composite component 
     * metadata from the cache.
     * 
     * @param url
     * @return
     * @throws IOException 
     */
    public abstract V getCompositeComponentMetadataFacelet(URL url) throws IOException;

    /**
     * Check if the composite component metadata facelet associated with the url is
     * cached or not.
     * 
     * @param url
     * @return 
     */
    public abstract boolean isCompositeComponentMetadataFaceletCached(URL url);
    
    /**
     * Set the factories used for create Facelet instances.
     * 
     * @param faceletFactory
     * @param viewMetadataFaceletFactory
     * @param compositeComponentMetadataFaceletFactory 
     */
    protected void setMemberFactories(FaceletCache.MemberFactory<V> faceletFactory,
                                      FaceletCache.MemberFactory<V> viewMetadataFaceletFactory,
                                      FaceletCache.MemberFactory<V> compositeComponentMetadataFaceletFactory)
    {
        if  (compositeComponentMetadataFaceletFactory == null)
        {
            throw new NullPointerException("viewMetadataFaceletFactory is null");
        }
        _compositeComponentMetadataFaceletFactory = compositeComponentMetadataFaceletFactory;
        setMemberFactories(faceletFactory, viewMetadataFaceletFactory);
    }
    
    /**
     * 
     * @return 
     */
    protected FaceletCache.MemberFactory<V> getCompositeComponentMetadataMemberFactory()
    {
        return _compositeComponentMetadataFaceletFactory;
    }    
}
