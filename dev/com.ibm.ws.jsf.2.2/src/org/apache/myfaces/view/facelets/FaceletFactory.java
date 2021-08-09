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

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.view.facelets.FaceletException;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.Facelet;
import javax.faces.view.facelets.FaceletContext;

/**
 * FaceletFactory for producing Facelets relative to the context of the underlying implementation.
 * 
 * @author Jacob Hookom
 * @version $Id: FaceletFactory.java 1522674 2013-09-12 17:31:24Z lu4242 $
 */
public abstract class FaceletFactory
{

    private static ThreadLocal<FaceletFactory> instance = new ThreadLocal<FaceletFactory>();

    public final static String LAST_RESOURCE_RESOLVED = "oam.facelets.LAST_RESOURCE_RESOLVED";

    /**
     * Return a Facelet instance as specified by the file at the passed URI.
     * 
     * @param uri
     * @return
     * @throws IOException
     * @throws FaceletException
     * @throws FacesException
     * @throws ELException
     */
    public abstract Facelet getFacelet(FacesContext context, String uri) throws IOException;
    
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
    public abstract Facelet getFacelet(URL url) throws IOException, FaceletException, FacesException, ELException;
    
    /**
     * Create a Facelet from the passed URL, but take into account the context. This method is
     * useful in cases where the facelet instance must replace the one in the cache based on 
     * the context, instead take the one from the cache, like for example when the EL expression
     * cache is used.
     * 
     * @param url
     *            source url
     * @return Facelet instance
     * @throws IOException
     * @throws FaceletException
     * @throws FacesException
     * @throws ELException
     */
    public abstract Facelet getFacelet(FaceletContext ctx, URL url)
        throws IOException, FaceletException, FacesException, ELException;    

    /**
     * Return a Facelet instance as specified by the file at the passed URI. The returned facelet is used
     * to create view metadata in this form: 
     * <p>
     * UIViewRoot(in facet javax_faces_metadata(one or many UIViewParameter instances))
     * </p>
     * <p>
     * This method should be called from FaceletViewMetadata.createMetadataView(FacesContext context)  
     * </p>
     * 
     * @since 2.0
     * @param uri
     * @return
     * @throws IOException
     */
    public abstract Facelet getViewMetadataFacelet(
        FacesContext context, String uri) throws IOException;
    
    /**
     * Create a Facelet used to create view metadata from the passed URL. This method checks if the 
     * cached Facelet needs to be refreshed before returning. If so, uses the passed URL to build a new instance;
     * 
     * @since 2.0
     * @param url source url
     * @return Facelet instance
     * @throws IOException
     * @throws FaceletException
     * @throws FacesException
     * @throws ELException
     */
    public abstract Facelet getViewMetadataFacelet(URL url)
            throws IOException, FaceletException, FacesException, ELException;

    /**
     * Return a Facelet instance as specified by the file at the passed URI. The returned facelet is used
     * to create composite component metadata.
     * <p>
     * This method should be called from vdl.getComponentMetadata(FacesContext context)  
     * </p>
     * 
     * @since 2.0.1
     * @param uri
     * @return
     * @throws IOException
     */
    public abstract Facelet getCompositeComponentMetadataFacelet(FacesContext context, String uri) 
        throws IOException;
    
    /**
     * Create a Facelet used to create composite component metadata from the passed URL. This method checks if the 
     * cached Facelet needs to be refreshed before returning. If so, uses the passed URL to build a new instance.
     * 
     * @since 2.0.1
     * @param url source url
     * @return Facelet instance
     * @throws IOException
     * @throws FaceletException
     * @throws FacesException
     * @throws ELException
     */
    public abstract Facelet getCompositeComponentMetadataFacelet(URL url)
            throws IOException, FaceletException, FacesException, ELException;

    /**
     * Compile a component tag on the fly.
     * 
     * @param taglibURI
     * @param tagName
     * @param attributes
     * @return 
     */
    public abstract Facelet compileComponentFacelet(String taglibURI, String tagName, Map<String,Object> attributes);
    
    /**
     * Set the static instance
     * 
     * @param factory
     */
    public static final void setInstance(FaceletFactory factory)
    {
        if (factory == null)
        {
            instance.remove();
        }
        else
        {
            instance.set(factory);
        }
    }

    /**
     * Get the static instance
     * 
     * @return
     */
    public static final FaceletFactory getInstance()
    {
        return instance.get();
    }
}
