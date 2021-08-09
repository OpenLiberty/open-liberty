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
public class AliasResourceMetaImpl extends ResourceMetaImpl
{
    private String _realResourceName;
    
    private boolean _couldContainValueExpressions;

    public AliasResourceMetaImpl(String prefix, String libraryName, String libraryVersion,
            String resourceName, String resourceVersion, String realResourceName, boolean couldContainValueExpressions)
    {
        this(prefix, libraryName, libraryVersion, resourceName, resourceVersion, realResourceName, 
            couldContainValueExpressions, null);
    }
    
    public AliasResourceMetaImpl(String prefix, String libraryName, String libraryVersion,
            String resourceName, String resourceVersion, String realResourceName, 
            boolean couldContainValueExpressions, String contractName)
    {
        super(prefix, libraryName, libraryVersion,
            resourceName, resourceVersion, contractName);
        _realResourceName = realResourceName;
        _couldContainValueExpressions = couldContainValueExpressions;
    }
    
    public String getRealResourceName()
    {
        return _realResourceName;
    }

    public void setRealResourceName(String realResourceName)
    {
        _realResourceName = realResourceName;
    }
    
    @Override
    public String getResourceIdentifier()
    {
        StringBuilder builder = new StringBuilder();
        boolean firstSlashAdded = false;
        if (getLocalePrefix() != null && getLocalePrefix().length() > 0)
        {
            builder.append(getLocalePrefix());
            firstSlashAdded = true;
        }
        if (getLibraryName() != null)
        {
            if (firstSlashAdded)
            {
                builder.append('/');
            }
            builder.append(getLibraryName());
            firstSlashAdded = true;
        }
        if (getLibraryVersion() != null)
        {
            if (firstSlashAdded)
            {
                builder.append('/');
            }
            builder.append(getLibraryVersion());
            firstSlashAdded = true;
        }
        if (getRealResourceName() != null)
        {
            if (firstSlashAdded)
            {
                builder.append('/');
            }
            builder.append(getRealResourceName());
            firstSlashAdded = true;
        }
        if (getResourceVersion() != null)
        {
            if (firstSlashAdded)
            {
                builder.append('/');
            }
            builder.append(getResourceVersion());
            builder.append(
                    getRealResourceName().substring(getRealResourceName().lastIndexOf('.')));
            firstSlashAdded = true;
        }
        return builder.toString();
    }

    @Override
    public boolean couldResourceContainValueExpressions()
    {
        return _couldContainValueExpressions;
    }
}
