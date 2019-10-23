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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.FactoryFinder;
import javax.faces.application.ViewResource;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.Facelet;
import javax.faces.view.facelets.FaceletCache;
import javax.faces.view.facelets.FaceletCacheFactory;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.ResourceResolver;

import org.apache.myfaces.shared.resource.ResourceLoaderUtils;
import org.apache.myfaces.view.facelets.AbstractFaceletCache;
import org.apache.myfaces.view.facelets.FaceletFactory;
import org.apache.myfaces.view.facelets.compiler.Compiler;
import org.apache.myfaces.view.facelets.util.ParameterCheck;

/**
 * Default FaceletFactory implementation.
 * 
 * @author Jacob Hookom
 * @version $Id: DefaultFaceletFactory.java 1522674 2013-09-12 17:31:24Z lu4242 $
 */
public final class DefaultFaceletFactory extends FaceletFactory
{
    private static final long INFINITE_DELAY = -1;
    private static final long NO_CACHE_DELAY = 0;
    
    //protected final Log log = LogFactory.getLog(DefaultFaceletFactory.class);
    protected final Logger log = Logger.getLogger(DefaultFaceletFactory.class.getName());

    private URL _baseUrl;

    private Compiler _compiler;
    
    //private Map<String, DefaultFacelet> _facelets;
    
    //private Map<String, DefaultFacelet> _viewMetadataFacelets;
    
    private Map<String, DefaultFacelet> _compositeComponentMetadataFacelets;

    private long _refreshPeriod;

    private Map<String, URL> _relativeLocations;

    private javax.faces.view.facelets.ResourceResolver _resolver;
    private DefaultResourceResolver _defaultResolver;
    
    private FaceletCache<Facelet> _faceletCache;
    private AbstractFaceletCache<Facelet> _abstractFaceletCache;
    
    public DefaultFaceletFactory(Compiler compiler, ResourceResolver resolver) throws IOException
    {
        this(compiler, resolver, -1);
    }

    public DefaultFaceletFactory(Compiler compiler, ResourceResolver resolver, long refreshPeriod)
    {
        ParameterCheck.notNull("compiler", compiler);
        ParameterCheck.notNull("resolver", resolver);

        _compiler = compiler;

        //_facelets = new HashMap<String, DefaultFacelet>();
        
        //_viewMetadataFacelets = new HashMap<String, DefaultFacelet>();
        
        _compositeComponentMetadataFacelets = new HashMap<String, DefaultFacelet>();

        _relativeLocations = new HashMap<String, URL>();

        _resolver = resolver;
        if (_resolver instanceof DefaultResourceResolver)
        {
            _defaultResolver = (DefaultResourceResolver) _resolver;
        }

        //_baseUrl = resolver.resolveUrl("/");

        _refreshPeriod = refreshPeriod < 0 ? INFINITE_DELAY : refreshPeriod * 1000;
        
        // facelet cache. Lookup here, because after all this is a "part" of the facelet factory implementation.
        FaceletCacheFactory cacheFactory
                = (FaceletCacheFactory) FactoryFinder.getFactory(FactoryFinder.FACELET_CACHE_FACTORY);
        _faceletCache = (FaceletCache<Facelet>) cacheFactory.getFaceletCache();
        
        FaceletCache.MemberFactory<Facelet> faceletFactory = new FaceletCache.MemberFactory<Facelet>()
        {
            public Facelet newInstance(URL url) throws IOException
            {
                return _createFacelet(url);
            }
        };
        FaceletCache.MemberFactory<Facelet> viewMetadataFaceletFactory = new FaceletCache.MemberFactory<Facelet>()
        {
            public Facelet newInstance(URL url) throws IOException
            {
                return _createViewMetadataFacelet(url);
            }
        };
        
        if (_faceletCache instanceof AbstractFaceletCache)
        {
            _abstractFaceletCache = (AbstractFaceletCache<Facelet>) _faceletCache;
            
            FaceletCache.MemberFactory<Facelet> compositeComponentMetadataFaceletFactory = 
                new FaceletCache.MemberFactory<Facelet>()
            {
                public Facelet newInstance(URL url) throws IOException
                {
                    return _createCompositeComponentMetadataFacelet(url);
                }
            };

            try
            {
                Method setMemberFactoriesMethod = AbstractFaceletCache.class.getDeclaredMethod("setMemberFactories",
                        new Class[]{FaceletCache.MemberFactory.class, FaceletCache.MemberFactory.class, 
                                    FaceletCache.MemberFactory.class});
                setMemberFactoriesMethod.setAccessible(true);
                setMemberFactoriesMethod.invoke(_faceletCache, faceletFactory, viewMetadataFaceletFactory, 
                    compositeComponentMetadataFaceletFactory);
            } 
            catch (Exception e)
            {
                throw new FacesException(
                    "Cannot call setMemberFactories method, Initialization of FaceletCache failed.",
                                         e);
            }   
        }
        else
        {
            // Note that FaceletCache.setMemberFactories method is protected, and this is the place where call
            // this method has sense, because DefaultFaceletFactory is the responsible to create Facelet instances.
            // The only way to do it is using reflection, and it has sense, because in this way it is possible to
            // setup a java SecurityManager that prevents call this method (because it is protected, and to do that
            // the code first check for "suppressAccessChecks" permission).
            try
            {
                Method setMemberFactoriesMethod = FaceletCache.class.getDeclaredMethod("setMemberFactories",
                        new Class[]{FaceletCache.MemberFactory.class, FaceletCache.MemberFactory.class});
                setMemberFactoriesMethod.setAccessible(true);
                setMemberFactoriesMethod.invoke(_faceletCache, faceletFactory, viewMetadataFaceletFactory);
            } 
            catch (Exception e)
            {
                throw new FacesException(
                    "Cannot call setMemberFactories method, Initialization of FaceletCache failed.",
                                         e);
            }            
        }

        if (log.isLoggable(Level.FINE))
        {
            log.fine("Using ResourceResolver: " + _resolver);
            log.fine("Using Refresh Period: " + _refreshPeriod);
        }
    }

    /**
     * Compiler this factory uses
     * 
     * @return final Compiler instance
     */
    public Compiler getCompiler()
    {
        return _compiler;
    }
    
    private URL getBaseUrl()
    {
        if (_baseUrl == null)
        {
            _baseUrl = _resolver.resolveUrl("/");
        }
        return _baseUrl;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.myfaces.view.facelets.FaceletFactory#getFacelet(java.lang.String)
     */
    @Override
    public Facelet getFacelet(FacesContext facesContext, String uri) 
        throws IOException, FaceletException, FacesException, ELException
    {
        URL url = (URL) _relativeLocations.get(uri);
        if (url == null)
        {
            url = resolveURL(facesContext, getBaseUrl(), uri);
            if (url != null)
            {
                ViewResource viewResource = (ViewResource) facesContext.getAttributes().get(
                    FaceletFactory.LAST_RESOURCE_RESOLVED);
                if (viewResource != null)
                {
                    // If a view resource has been used to resolve a resource, the cache is in
                    // the ResourceHandler implementation. No need to cache in _relativeLocations.
                }
                else
                {
                    Map<String, URL> newLoc = new HashMap<String, URL>(_relativeLocations);
                    newLoc.put(uri, url);
                    _relativeLocations = newLoc;
                }
            }
            else
            {
                throw new IOException("'" + uri + "' not found.");
            }
        }
        return this.getFacelet(url);
    }

    /**
     * Create a Facelet from the passed URL. This method checks if the cached Facelet needs to be refreshed before
     * returning. If so, uses the passed URL to build a new instance;
     * 
     * @param url
     *            source url
     * @return Facelet instance
     * @throws IOException
     * @throws FaceletException
     * @throws FacesException
     * @throws ELException
     */
    @Override
    public Facelet getFacelet(URL url) throws IOException, FaceletException, FacesException, ELException
    {
        return _faceletCache.getFacelet(url);
    }
    
    
    @Override
    public Facelet getFacelet(FaceletContext ctx, URL url) 
            throws IOException, FaceletException, FacesException, ELException
    {
        if (_abstractFaceletCache != null)
        {
            return _abstractFaceletCache.getFacelet(ctx, url);
        }
        else
        {
            return _faceletCache.getFacelet(url);
        }
    }

    public long getRefreshPeriod()
    {
        return _refreshPeriod;
    }

    /**
     * Resolves a path based on the passed URL. If the path starts with '/', then resolve the path against
     * {@link javax.faces.context.ExternalContext#getResource(java.lang.String)
     * javax.faces.context.ExternalContext#getResource(java.lang.String)}. Otherwise create a new URL via
     * {@link URL#URL(java.net.URL, java.lang.String) URL(URL, String)}.
     * 
     * @param source
     *            base to resolve from
     * @param path
     *            relative path to the source
     * @return resolved URL
     * @throws IOException
     */
    public URL resolveURL(FacesContext context, URL source, String path) throws IOException
    {
        if (path.startsWith("/"))
        {
            context.getAttributes().put(LAST_RESOURCE_RESOLVED, null);
            URL url = resolveURL(context, path);
            if (url == null)
            {
                throw new FileNotFoundException(path + " Not Found in ExternalContext as a Resource");
            }
            return url;
        }
        else
        {
            return new URL(source, path);
        }
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
                if (conn != null)
                {
                    try
                    {
                        conn.getInputStream().close();
                    }
                    catch (Exception e)
                    {
                        // Ignored
                    }
                }
            }
        }

        return false;
    }

    /**
     * Uses the internal Compiler reference to build a Facelet given the passed URL.
     * 
     * @param url
     *            source
     * @return a Facelet instance
     * @throws IOException
     * @throws FaceletException
     * @throws FacesException
     * @throws ELException
     */
    private DefaultFacelet _createFacelet(URL url) throws IOException, FaceletException, FacesException, ELException
    {
        if (log.isLoggable(Level.FINE))
        {
            log.fine("Creating Facelet for: " + url);
        }

        String alias = "/" + _removeFirst(url.getFile(), getBaseUrl().getFile());
        try
        {
            FaceletHandler h = _compiler.compile(url, alias);
            DefaultFacelet f = new DefaultFacelet(this, _compiler.createExpressionFactory(), url, alias, alias, h);
            return f;
        }
        catch (FileNotFoundException fnfe)
        {
            throw new FileNotFoundException("Facelet " + alias + " not found at: " + url.toExternalForm());
        }
    }
    
    /**
     * @since 2.0
     * @param url
     * @return
     * @throws IOException
     * @throws FaceletException
     * @throws FacesException
     * @throws ELException
     */
    private DefaultFacelet _createViewMetadataFacelet(URL url)
            throws IOException, FaceletException, FacesException, ELException
    {
        if (log.isLoggable(Level.FINE))
        {
            log.fine("Creating Facelet used to create View Metadata for: " + url);
        }

        // The alias is used later for informative purposes, so we append 
        // some prefix to identify later where the errors comes from.
        String faceletId = "/"+ _removeFirst(url.getFile(), getBaseUrl().getFile());
        String alias = "/viewMetadata" + faceletId;
        try
        {
            FaceletHandler h = _compiler.compileViewMetadata(url, alias);
            DefaultFacelet f = new DefaultFacelet(this, _compiler.createExpressionFactory(), url, alias, 
                    faceletId, h);
            return f;
        }
        catch (FileNotFoundException fnfe)
        {
            throw new FileNotFoundException("Facelet " + alias + " not found at: " + url.toExternalForm());
        }

    }
    
    /**
     * @since 2.0.1
     * @param url
     * @return
     * @throws IOException
     * @throws FaceletException
     * @throws FacesException
     * @throws ELException
     */
    private DefaultFacelet _createCompositeComponentMetadataFacelet(URL url)
            throws IOException, FaceletException, FacesException, ELException
    {
        if (log.isLoggable(Level.FINE))
        {
            log.fine("Creating Facelet used to create Composite Component Metadata for: " + url);
        }

        // The alias is used later for informative purposes, so we append 
        // some prefix to identify later where the errors comes from.
        String alias = "/compositeComponentMetadata/" + _removeFirst(url.getFile(), getBaseUrl().getFile());
        try
        {
            FaceletHandler h = _compiler.compileCompositeComponentMetadata(url, alias);
            DefaultFacelet f = new DefaultFacelet(this, _compiler.createExpressionFactory(), url, alias,
                    alias, h, true);
            return f;
        }
        catch (FileNotFoundException fnfe)
        {
            throw new FileNotFoundException("Facelet " + alias + " not found at: " + url.toExternalForm());
        }
    }

    /**
     * Works in the same way as getFacelet(String uri), but redirect
     * to getViewMetadataFacelet(URL url)
     * @since 2.0
     */
    @Override
    public Facelet getViewMetadataFacelet(FacesContext facesContext, String uri) 
        throws IOException
    {
        URL url = (URL) _relativeLocations.get(uri);
        if (url == null)
        {
            url = resolveURL(facesContext, getBaseUrl(), uri);
            ViewResource viewResource = (ViewResource) facesContext.getAttributes().get(
                FaceletFactory.LAST_RESOURCE_RESOLVED);
            if (url != null)
            {
                if (viewResource != null)
                {
                    // If a view resource has been used to resolve a resource, the cache is in
                    // the ResourceHandler implementation. No need to cache in _relativeLocations.
                }
                else
                {
                    Map<String, URL> newLoc = new HashMap<String, URL>(_relativeLocations);
                    newLoc.put(uri, url);
                    _relativeLocations = newLoc;
                }
            }
            else
            {
                throw new IOException("'" + uri + "' not found.");
            }
        }
        return this.getViewMetadataFacelet(url);
    }

    /**
     * @since 2.0
     */
    @Override
    public Facelet getViewMetadataFacelet(URL url) throws IOException,
            FaceletException, FacesException, ELException
    {
        if (_abstractFaceletCache != null)
        {
            return _abstractFaceletCache.getViewMetadataFacelet(url);
        }
        else
        {
            return _faceletCache.getViewMetadataFacelet(url);
        }
    }
    
    /**
     * Works in the same way as getFacelet(String uri), but redirect
     * to getViewMetadataFacelet(URL url)
     * @since 2.0.1
     */
    @Override
    public Facelet getCompositeComponentMetadataFacelet(FacesContext facesContext, String uri)
        throws IOException
    {
        URL url = (URL) _relativeLocations.get(uri);
        if (url == null)
        {
            url = resolveURL(facesContext, getBaseUrl(), uri);
            ViewResource viewResource = (ViewResource) facesContext.getAttributes().get(
                FaceletFactory.LAST_RESOURCE_RESOLVED);            
            if (url != null)
            {
                if (viewResource != null)
                {
                    // If a view resource has been used to resolve a resource, the cache is in
                    // the ResourceHandler implementation. No need to cache in _relativeLocations.
                }
                else
                {
                    Map<String, URL> newLoc = new HashMap<String, URL>(_relativeLocations);
                    newLoc.put(uri, url);
                    _relativeLocations = newLoc;
                }
            }
            else
            {
                throw new IOException("'" + uri + "' not found.");
            }
        }
        return this.getCompositeComponentMetadataFacelet(url);
    }

    /**
     * @since 2.0.1
     */
    @Override
    public Facelet getCompositeComponentMetadataFacelet(URL url) throws IOException,
            FaceletException, FacesException, ELException
    {
        if (_abstractFaceletCache != null)
        {
            return _abstractFaceletCache.getCompositeComponentMetadataFacelet(url);
        }
        else
        {
            ParameterCheck.notNull("url", url);

            String key = url.toString();

            DefaultFacelet f = _compositeComponentMetadataFacelets.get(key);

            if (f == null || this.needsToBeRefreshed(f))
            {
                f = this._createCompositeComponentMetadataFacelet(url);
                if (_refreshPeriod != NO_CACHE_DELAY)
                {
                    Map<String, DefaultFacelet> newLoc
                            = new HashMap<String, DefaultFacelet>(_compositeComponentMetadataFacelets);
                    newLoc.put(key, f);
                    _compositeComponentMetadataFacelets = newLoc;
                }
            }
            return f;
        }
    }
    
    private URL resolveURL(FacesContext context, String path)
    {
        if (_defaultResolver != null)
        {
            return _defaultResolver.resolveUrl(context, path);
        }
        else
        {
            return _resolver.resolveUrl(path);
        }
    }

    public Facelet compileComponentFacelet(String taglibURI, String tagName, Map<String,Object> attributes)
    {
        FaceletHandler handler = _compiler.compileComponent(taglibURI, tagName, attributes);
        String alias = "/component/oamf:"+tagName;
        return new DefaultFacelet(this, _compiler.createExpressionFactory(), getBaseUrl(), alias, alias, handler);
    }
    
    /**
     * Removes the first appearance of toRemove in string.
     *
     * Works just like string.replaceFirst(toRemove, ""), except that toRemove
     * is not treated as a regex (which could cause problems with filenames).
     *
     * @param string
     * @param toRemove
     * @return
     */
    private String _removeFirst(String string, String toRemove)
    {
        // do exactly what String.replaceFirst(toRemove, "") internally does,
        // except treating the search as literal text and not as regex

        return Pattern.compile(toRemove, Pattern.LITERAL).matcher(string).replaceFirst("");
    }

}
