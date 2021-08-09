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

/**
 * Contains the metadata information to reference a resource 
 */
public class ResourceMetaImpl extends ResourceMeta
{

    private final String _prefix;
    private final String _libraryName;
    private final String _libraryVersion;
    private final String _resourceName;
    private final String _resourceVersion;
    private final String _contractName;

    public ResourceMetaImpl(String prefix, String libraryName, String libraryVersion,
            String resourceName, String resourceVersion)
    {
        this(prefix, libraryName, libraryVersion, resourceName, resourceVersion, null);
    }
    
    public ResourceMetaImpl(String prefix, String libraryName, String libraryVersion,
            String resourceName, String resourceVersion, String contractName)
    {
        _prefix = prefix;
        _libraryName = libraryName;
        _libraryVersion = libraryVersion;
        _resourceName = resourceName;
        _resourceVersion = resourceVersion;
        _contractName = contractName;
    }

    public String getLibraryName()
    {
        return _libraryName;
    }    
    
    public String getResourceName()
    {
        return _resourceName;
    }    

    public String getLocalePrefix()
    {
        return _prefix;
    }

    public String getLibraryVersion()
    {
        return _libraryVersion;
    }

    public String getResourceVersion()
    {
        return _resourceVersion;
    }

    public String getContractName()
    {
        return _contractName;
    }
    
    @Override
    public String getResourceIdentifier()
    {
        StringBuilder builder = new StringBuilder();
        boolean firstSlashAdded = false;
        if (_prefix != null && _prefix.length() > 0)
        {
            builder.append(_prefix);
            firstSlashAdded = true;
        }
        if (_libraryName != null)
        {
            if (firstSlashAdded)
            {
                builder.append('/');
            }
            builder.append(_libraryName);
            firstSlashAdded = true;
        }
        if (_libraryVersion != null)
        {
            if (firstSlashAdded)
            {
                builder.append('/');
            }
            builder.append(_libraryVersion);
            firstSlashAdded = true;
        }
        if (_resourceName != null)
        {
            if (firstSlashAdded)
            {
                builder.append('/');
            }
            builder.append(_resourceName);
            firstSlashAdded = true;
        }
        if (_resourceVersion != null)
        {
            if (firstSlashAdded)
            {
                builder.append('/');
            }
            builder.append(_resourceVersion);
            builder.append(
                    _resourceName.substring(_resourceName.lastIndexOf('.')));
            firstSlashAdded = true;
        }

        return builder.toString();
    }

    @Override
    public boolean couldResourceContainValueExpressions()
    {
        return false;
    }

}
