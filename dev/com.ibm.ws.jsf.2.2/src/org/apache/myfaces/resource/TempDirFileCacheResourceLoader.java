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
package org.apache.myfaces.resource;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.faces.FacesException;
import javax.faces.application.Resource;
import javax.faces.context.FacesContext;
import org.apache.myfaces.application.ResourceHandlerImpl;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.shared.resource.ResourceLoader;
import org.apache.myfaces.shared.resource.ResourceLoaderWrapper;
import org.apache.myfaces.shared.resource.ResourceMeta;
import org.apache.myfaces.shared.util.WebConfigParamUtils;

/**
 * ResourceLoader that uses a temporal folder to cache resources, avoiding the problem
 * described on  MYFACES-3586 (Performance improvement in Resource loading - 
 * HIGH CPU inflating bytes in ResourceHandlerImpl.handleResourceRequest).
 *
 * @author Leonardo Uribe
 */
public class TempDirFileCacheResourceLoader extends ResourceLoaderWrapper
{
    /**
     * If this param is set to true (default false), a temporal directory is created and
     * all files handled by this ResourceLoader are cached there, avoiding the problem
     * described on MYFACES-3586. (Performance improvement in Resource loading - 
     * HIGH CPU inflating bytes in ResourceHandlerImpl.handleResourceRequest).
     */
    @JSFWebConfigParam(since="2.1.11", expectedValues="true, false", defaultValue="false")
    public final static String INIT_PARAM_TEMPORAL_RESOURCEHANDLER_CACHE_ENABLED = 
        "org.apache.myfaces.TEMPORAL_RESOURCEHANDLER_CACHE_ENABLED";
    public final static boolean INIT_PARAM_TEMPORAL_RESOURCEHANDLER_CACHE_ENABLED_DEFAULT = false;
    
    public final static String TEMP_FILES_LOCK_MAP = "oam.rh.TEMP_FILES_LOCK_MAP";
    
    /**
     * Subdir of the ServletContext tmp dir to store temporal resources.
     */
    private static final String TEMP_FOLDER_BASE_DIR = "oam-rh-cache/";

    /**
     * Suffix for temporal files.
     */
    private static final String TEMP_FILE_SUFFIX = ".tmp";
    
    private ResourceLoader delegate;
    
    private volatile File _tempDir;
    
    private int _resourceBufferSize = -1;
    
    public TempDirFileCacheResourceLoader(ResourceLoader delegate)
    {
        this.delegate = delegate;
        initialize();
    }
    
    public static boolean isValidCreateTemporalFiles(FacesContext facesContext)
    {
        if (WebConfigParamUtils.getBooleanInitParameter(facesContext.getExternalContext(),
            INIT_PARAM_TEMPORAL_RESOURCEHANDLER_CACHE_ENABLED,
            INIT_PARAM_TEMPORAL_RESOURCEHANDLER_CACHE_ENABLED_DEFAULT))
        {
            // Try create a temporal folder to check if is valid to do so, otherwise, disable it.
            try
            {
                Map<String, Object> applicationMap = facesContext.getExternalContext().getApplicationMap();
                File tempdir = (File) applicationMap.get("javax.servlet.context.tempdir");
                File imagesDir = new File(tempdir, TEMP_FOLDER_BASE_DIR);
                if (!imagesDir.exists())
                {
                    imagesDir.mkdirs();
                }
                return true;
            }
            catch (Exception e)
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }
    
    protected void initialize()
    {
        //Get startup FacesContext
        FacesContext facesContext = FacesContext.getCurrentInstance();
    
        //1. Create temporal directory for temporal resources
        Map<String, Object> applicationMap = facesContext.getExternalContext().getApplicationMap();
        File tempdir = (File) applicationMap.get("javax.servlet.context.tempdir");
        File imagesDir = new File(tempdir, TEMP_FOLDER_BASE_DIR);
        if (!imagesDir.exists())
        {
            imagesDir.mkdirs();
        }
        else
        {
            //Clear the cache
            deleteDir(imagesDir);
            imagesDir.mkdirs();
        }
        _tempDir = imagesDir;

        //2. Create map for register temporal resources
        Map<String, FileProducer> temporalFilesLockMap = new ConcurrentHashMap<String, FileProducer>();
        facesContext.getExternalContext().getApplicationMap().put(TEMP_FILES_LOCK_MAP, temporalFilesLockMap);
    }

    private static boolean deleteDir(File dir)
    {
        if (dir.isDirectory())
        {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++)
            {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success)
                {
                    return false;
                }
            }
        }
        return dir.delete();
    }
    
    @Override
    public URL getResourceURL(ResourceMeta resourceMeta)
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        if (resourceExists(resourceMeta))
        {
            File file = createOrGetTempFile(facesContext, resourceMeta);
            
            try
            {
                return file.toURL();
            }
            catch (MalformedURLException e)
            {
                throw new FacesException(e);
            }
        }
        else
        {
            return null;
        }
    }    
    
    public InputStream getResourceInputStream(ResourceMeta resourceMeta, Resource resource)
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();

        if (resourceExists(resourceMeta))
        {
            File file = createOrGetTempFile(facesContext, resourceMeta);
            
            try
            {
                return new BufferedInputStream(new FileInputStream(file));
            }
            catch (FileNotFoundException e)
            {
                throw new FacesException(e);
            }
        }
        else
        {
            return null;
        }
    }

    @Override
    public InputStream getResourceInputStream(ResourceMeta resourceMeta)
    {
        return getResourceInputStream(resourceMeta, null);
    }
    
    @Override
    public boolean resourceExists(ResourceMeta resourceMeta)
    {
        return super.resourceExists(resourceMeta);
    }

    @SuppressWarnings("unchecked")
    private File createOrGetTempFile(FacesContext facesContext, ResourceMeta resourceMeta)
    {
        String identifier = resourceMeta.getResourceIdentifier();
        File file = getTemporalFile(resourceMeta);
        if (!file.exists())
        {
            Map<String, FileProducer> map = (Map<String, FileProducer>) 
                facesContext.getExternalContext().getApplicationMap().get(TEMP_FILES_LOCK_MAP);

            FileProducer creator = map.get(identifier);
            
            if (creator == null)
            {
                synchronized(this)
                {
                    creator = map.get(identifier);
                    
                    if (creator == null)
                    {
                        creator = new FileProducer();
                        map.put(identifier, creator);
                    }
                }
            }
            
            if (!creator.isCreated())
            {
                creator.createFile(facesContext, resourceMeta, file, this);
            }
        }
        return file;
    }    
    
    private File getTemporalFile(ResourceMeta resourceMeta)
    {
        return new File(_tempDir, resourceMeta.getResourceIdentifier() + TEMP_FILE_SUFFIX);
    }

    /*
    private boolean couldResourceContainValueExpressions(ResourceMeta resourceMeta)
    {
        return resourceMeta.couldResourceContainValueExpressions() || resourceMeta.getResourceName().endsWith(".css");
    }*/
    
    protected void createTemporalFileVersion(FacesContext facesContext, ResourceMeta resourceMeta, File target)
    {
        target.mkdirs();  // ensure necessary directories exist
        target.delete();  // remove any existing file

        InputStream inputStream = null;
        FileOutputStream fileOutputStream;
        try
        {
            /*
            if (couldResourceContainValueExpressions(resourceMeta))
            {
                inputStream = new ValueExpressionFilterInputStream(
                        getWrapped().getResourceInputStream(resourceMeta),
                        resourceMeta.getLibraryName(), 
                        resourceMeta.getResourceName());
            }
            else
            {*/
                inputStream = getWrapped().getResourceInputStream(resourceMeta);
            /*}*/
            fileOutputStream = new FileOutputStream(target);
            byte[] buffer = new byte[this.getResourceBufferSize()];

            pipeBytes(inputStream, fileOutputStream, buffer);
        }
        catch (FileNotFoundException e)
        {
            throw new FacesException("Unexpected exception while create file:", e);
        }
        catch (IOException e)
        {
            throw new FacesException("Unexpected exception while create file:", e);
        }
        finally
        {
            if (inputStream != null)
            {
                try
                {
                    inputStream.close();
                }
                catch (IOException e)
                {
                    // Ignore
                }
            }
        }
    }
    
    /**
     * Reads the specified input stream into the provided byte array storage and
     * writes it to the output stream.
     */
    private static void pipeBytes(InputStream in, OutputStream out, byte[] buffer) throws IOException
    {
        int length;

        while ((length = (in.read(buffer))) >= 0)
        {
            out.write(buffer, 0, length);
        }
    }
    
    public static class FileProducer 
    {
        
        public volatile boolean created = false;
        
        public FileProducer()
        {
            super();
        }

        public boolean isCreated()
        {
            return created;
        }

        public synchronized void createFile(FacesContext facesContext, 
            ResourceMeta resourceMeta, File file, TempDirFileCacheResourceLoader loader)
        {
            if (!created)
            {
                loader.createTemporalFileVersion(facesContext, resourceMeta, file);
                created = true;
            }
        }
    }
    
    protected int getResourceBufferSize()
    {
        if (_resourceBufferSize == -1)
        {
            _resourceBufferSize = WebConfigParamUtils.getIntegerInitParameter(
                FacesContext.getCurrentInstance().getExternalContext(),
                ResourceHandlerImpl.INIT_PARAM_RESOURCE_BUFFER_SIZE,
                ResourceHandlerImpl.INIT_PARAM_RESOURCE_BUFFER_SIZE_DEFAULT);
        }
        return _resourceBufferSize;
    }
    
    public ResourceLoader getWrapped()
    {
        return delegate;
    }
}
