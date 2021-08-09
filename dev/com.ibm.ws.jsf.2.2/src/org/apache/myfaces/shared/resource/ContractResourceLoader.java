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
 * ResourceLoaders that are able to handle contract aware resources
 * must extends from this class.
 */
public abstract class ContractResourceLoader extends ResourceLoader
{
    
    public static final String VERSION_INVALID = "INVALID";
    
    public ContractResourceLoader(String prefix)
    {
        super(prefix);
    }
    
    public String getResourceVersion(String path)
    {
        return null;
    }

    public String getLibraryVersion(String path)
    {
        return null;
    }

    public abstract String getResourceVersion(String path, String contractName);

    /**
     * Return the max available version found (if exists) or
     * return null if no version available. 
     */
    public abstract String getLibraryVersion(String path, String contractName);

    public ResourceMeta createResourceMeta(String prefix, String libraryName, 
            String libraryVersion, String resourceName, String resourceVersion)
    {
        return null;
    }
    
    public abstract ResourceMeta createResourceMeta(String prefix, String libraryName, 
            String libraryVersion, String resourceName, String resourceVersion, String contractName);
    
    public boolean libraryExists(String libraryName)
    {
        return false;
    }
    
    public abstract boolean libraryExists(String libraryName, String contractName);
    
}
