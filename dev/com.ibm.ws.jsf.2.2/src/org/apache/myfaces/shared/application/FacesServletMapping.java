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
package org.apache.myfaces.shared.application;

/**
 * Represents a mapping entry of the FacesServlet in the web.xml
 * configuration file.
 */
public class FacesServletMapping
{

    /**
     * The path ("/faces", for example) which has been specified in the
     * url-pattern of the FacesServlet mapping.
     */
    private String prefix;

    /**
     * The extension (".jsf", for example) which has been specified in the
     * url-pattern of the FacesServlet mapping.
     */
    private String extension;

    /**
     * Creates a new FacesServletMapping object using prefix mapping.
     *
     * @param path The path ("/faces", for example) which has been specified
     *             in the url-pattern of the FacesServlet mapping.
     * @return a newly created FacesServletMapping
     */
    public static FacesServletMapping createPrefixMapping(String path)
    {
        FacesServletMapping mapping = new FacesServletMapping();
        mapping.setPrefix(path);
        return mapping;
    }

    /**
     * Creates a new FacesServletMapping object using extension mapping.
     *
     * @param path The extension (".jsf", for example) which has been
     *             specified in the url-pattern of the FacesServlet mapping.
     * @return a newly created FacesServletMapping
     */
    public static FacesServletMapping createExtensionMapping(
        String extension)
    {
        FacesServletMapping mapping = new FacesServletMapping();
        mapping.setExtension(extension);
        return mapping;
    }

    /**
     * Returns the path ("/faces", for example) which has been specified in
     * the url-pattern of the FacesServlet mapping. If this mapping is based
     * on an extension, <code>null</code> will be returned. Note that this
     * path is not the same as the specified url-pattern as the trailing
     * "/*" is omitted.
     *
     * @return the path which has been specified in the url-pattern
     */
    public String getPrefix()
    {
        return prefix;
    }

    /**
     * Sets the path ("/faces/", for example) which has been specified in
     * the url-pattern.
     *
     * @param path The path which has been specified in the url-pattern
     */
    public void setPrefix(String path)
    {
        this.prefix = path;
    }

    /**
     * Returns the extension (".jsf", for example) which has been specified
     * in the url-pattern of the FacesServlet mapping. If this mapping is
     * not based on an extension, <code>null</code> will be returned.
     *
     * @return the extension which has been specified in the url-pattern
     */
    public String getExtension()
    {
        return extension;
    }

    /**
     * Sets the extension (".jsf", for example) which has been specified in
     * the url-pattern of the FacesServlet mapping.
     *
     * @param extension The extension which has been specified in the url-pattern
     */
    public void setExtension(String extension)
    {
        this.extension = extension;
    }

    /**
     * Indicates whether this mapping is based on an extension (e.g.
     * ".jsp").
     *
     * @return <code>true</code>, if this mapping is based is on an
     *         extension, <code>false</code> otherwise
     */
    public boolean isExtensionMapping()
    {
        return extension != null;
    }

    /**
     * Indicates whether this mapping is based on a prefix (e.g.
     * /faces/*").
     *
     * @return <code>true</code>, if this mapping is based is on a
     *         prefix, <code>false</code> otherwise
     */
    public boolean isPrefixMapping()
    {
        return prefix != null;
    }
    
    /**
     * Returns the url-pattern entry for this servlet mapping.
     *
     * @return the url-pattern entry for this servlet mapping
     */
    public String getUrlPattern()
    {
        if (isExtensionMapping())
        {
            return "*" + extension;
        }
        else
        {
            return prefix + "/*";
        }
    }

}