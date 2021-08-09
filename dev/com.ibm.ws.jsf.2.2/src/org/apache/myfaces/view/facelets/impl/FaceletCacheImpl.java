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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.faces.view.facelets.FaceletCache;
import javax.faces.view.facelets.FaceletException;

import org.apache.myfaces.shared.resource.ResourceLoaderUtils;
import org.apache.myfaces.view.facelets.util.ParameterCheck;

/**
 * TODO: Note MyFaces core has another type of Facelet for read composite component
 * metadata. The reason behind do this in MyFaces is to retrieve some information
 * related to insertChildren/insertFacet that can be used as metadata and use that
 * information later to do the hack for composite components using templates, instead
 * rely on component relocation. This is not handled by FaceletCache included here 
 * but in practice this should not be a problem, because the intention of this class
 * is handle Facelet instances created using custom ResourceResolver stuff, and 
 * usually those pages are for views, not for composite components. Even if that is
 * true, composite component metadata Facelet is smaller (only add cc:xxx stuff) that
 * the other ones used for views or the one used to apply the composite component
 * itself.  
 * 
 * @author Leonardo Uribe
 * @since 2.1.0
 *
 */
class FaceletCacheImpl extends FaceletCache<DefaultFacelet>
{

    private static final long INFINITE_DELAY = -1;
    private static final long NO_CACHE_DELAY = 0;
    
    private Map<String, DefaultFacelet> _facelets;
    
    private Map<String, DefaultFacelet> _viewMetadataFacelets;

    private long _refreshPeriod;
    
    FaceletCacheImpl(long refreshPeriod)
    {
        _refreshPeriod = refreshPeriod < 0 ? INFINITE_DELAY : refreshPeriod * 1000;
        
        _facelets = new HashMap<String, DefaultFacelet>();
        
        _viewMetadataFacelets = new HashMap<String, DefaultFacelet>();
    }

    @Override
    public DefaultFacelet getFacelet(URL url) throws IOException
    {
        ParameterCheck.notNull("url", url);
        
        String key = url.toString();
        
        DefaultFacelet f = _facelets.get(key);
        
        if (f == null || this.needsToBeRefreshed(f))
        {
            //f = this._createFacelet(url);
            f = getMemberFactory().newInstance(url);
            if (_refreshPeriod != NO_CACHE_DELAY)
            {
                Map<String, DefaultFacelet> newLoc = new HashMap<String, DefaultFacelet>(_facelets);
                newLoc.put(key, f);
                _facelets = newLoc;
            }
        }
        
        return f;
    }
    
    @Override
    public boolean isFaceletCached(URL url)
    {
        return _facelets.containsKey(url.toString());
    }

    @Override
    public DefaultFacelet getViewMetadataFacelet(URL url) throws IOException
    {
        ParameterCheck.notNull("url", url);
        
        String key = url.toString();
        
        DefaultFacelet f = _viewMetadataFacelets.get(key);
        
        if (f == null || this.needsToBeRefreshed(f))
        {
            //f = this._createViewMetadataFacelet(url);
            f = getMetadataMemberFactory().newInstance(url);
            if (_refreshPeriod != NO_CACHE_DELAY)
            {
                Map<String, DefaultFacelet> newLoc = new HashMap<String, DefaultFacelet>(_viewMetadataFacelets);
                newLoc.put(key, f);
                _viewMetadataFacelets = newLoc;
            }
        }
        
        return f;
    }

    @Override
    public boolean isViewMetadataFaceletCached(URL url)
    {
        return _viewMetadataFacelets.containsKey(url.toString());
    }

    /**
     * Template method for determining if the Facelet needs to be refreshed.
     * 
     * @param facelet
     *            Facelet that could have expired
     * @return true if it needs to be refreshed
     */
    protected boolean needsToBeRefreshed(DefaultFacelet facelet)
    {
        // if set to 0, constantly reload-- nocache
        if (_refreshPeriod == NO_CACHE_DELAY)
        {
            return true;
        }

        // if set to -1, never reload
        if (_refreshPeriod == INFINITE_DELAY)
        {
            return false;
        }

        long target = facelet.getCreateTime() + _refreshPeriod;
        if (System.currentTimeMillis() > target)
        {
            // Should check for file modification

            URLConnection conn = null;
            try
            {
                conn = facelet.getSource().openConnection();
                long lastModified = ResourceLoaderUtils.getResourceLastModified(conn);

                return lastModified == 0 || lastModified > target;
            }
            catch (IOException e)
            {
                throw new FaceletException("Error Checking Last Modified for " + facelet.getAlias(), e);
            }
            finally
            {
                // finally close input stream when finished, if fails just continue.
                if (conn != null)
                {
                    try 
                    {
                        InputStream is = conn.getInputStream();
                        if (is != null)
                        {
                            is.close();
                        }
                    }
                    catch (IOException e)
                    {
                        // Ignore 
                    }
                }
            }
        }

        return false;
    }
}
