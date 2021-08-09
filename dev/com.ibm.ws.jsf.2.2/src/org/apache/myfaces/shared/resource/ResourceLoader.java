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
package org.apache.myfaces.shared.resource;

import java.io.InputStream;
import java.net.URL;
import java.util.Comparator;

/**
 * Base class for resource loaders.  Resource loaders can lookup resources 
 * as URLs from arbitrary locations, including JAR files.
 */
public abstract class ResourceLoader
{
    
    public static final String VERSION_INVALID = "INVALID";
    
    private String _prefix;
    
    public ResourceLoader(String prefix)
    {
        _prefix = prefix;
    }

    public abstract String getResourceVersion(String path);

    /**
     * Return the max available version found (if exists) or
     * return null if no version available. 
     */
    public abstract String getLibraryVersion(String path);

    /**
     * Return the max available version found (if exists) or
     * return null if no version available. 
     */
    public abstract URL getResourceURL(ResourceMeta resourceMeta);

    public abstract InputStream getResourceInputStream(ResourceMeta resourceMeta);
    
    public abstract ResourceMeta createResourceMeta(String prefix, String libraryName, String libraryVersion,
            String resourceName, String resourceVersion);

    public ResourceMeta createResourceMeta(String prefix, String libraryName, 
            String libraryVersion, String resourceName, String resourceVersion, String contractName)
    {
        return createResourceMeta(prefix, libraryName, libraryVersion, resourceName, resourceVersion);
    }
    
    public abstract boolean libraryExists(String libraryName);
    
    public boolean resourceExists(ResourceMeta resourceMeta)
    {
        return (getResourceURL(resourceMeta) != null);
    }

    /*
    public URL getResourceURL(String resourceId)
    {
        throw new UnsupportedOperationException(
            "An implementation for getResourceURL(String resourceId) method is required for JSF 2.2");
    }
    
    public boolean resourceIdExists(String resourceId)
    {
        return (getResourceURL(resourceId) != null);
    }*/
    
    private Comparator<String> _versionComparator = null;

    protected Comparator<String> getVersionComparator()
    {
        if (_versionComparator == null)
        {
            _versionComparator = new VersionComparator();
        }
        return _versionComparator;
    }

    protected void setVersionComparator(Comparator<String> versionComparator)
    {
        _versionComparator = versionComparator;
    }

    public static class VersionComparator implements Comparator<String>
    {

        public int compare(String s1, String s2)
        {
            int n1 = 0;
            int n2 = 0;
            String o1 = s1;
            String o2 = s2;

            boolean p1 = true;
            boolean p2 = true;

            while (n1 == n2 && (p1 || p2))
            {
                int i1 = o1.indexOf('_');
                int i2 = o2.indexOf('_');
                if (i1 < 0)
                {
                    if (o1.length() > 0)
                    {
                        p1 = false;
                        n1 = Integer.valueOf(o1);
                        o1 = "";
                    }
                    else
                    {
                        p1 = false;
                        n1 = 0;
                    }
                }
                else
                {
                    n1 = Integer.valueOf(o1.substring(0, i1));
                    o1 = o1.substring(i1 + 1);
                }
                if (i2 < 0)
                {
                    if (o2.length() > 0)
                    {
                        p2 = false;
                        n2 = Integer.valueOf(o2);
                        o2 = "";
                    }
                    else
                    {
                        p2 = false;
                        n2 = 0;
                    }
                }
                else
                {
                    n2 = Integer.valueOf(o2.substring(0, i2));
                    o2 = o2.substring(i2 + 1);
                }
            }

            if (n1 == n2)
            {
                return s1.length() - s2.length();
            }
            return n1 - n2;
        }
    }
    
    public String getPrefix()
    {
        return _prefix;
    }

    public void setPrefix(String prefix)
    {
        _prefix = prefix;
    }
}
