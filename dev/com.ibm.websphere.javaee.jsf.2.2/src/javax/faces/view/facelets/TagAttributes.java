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
package javax.faces.view.facelets;


/**
 * A set of TagAttributes, usually representing all attributes on a Tag.
 */
public abstract class TagAttributes
{
    /**
     * 
     */
    public TagAttributes()
    {
    }

    /**
     * Using no namespace, find the TagAttribute
     * 
     * @see #get(String, String)
     * @param localName
     *            tag attribute name
     * @return the TagAttribute found, otherwise null
     */
    public abstract TagAttribute get(String localName);

    /**
     * Find a TagAttribute that matches the passed namespace and local name.
     * 
     * @param ns
     *            namespace of the desired attribute
     * @param localName
     *            local name of the attribute
     * @return a TagAttribute found, otherwise null
     */
    public abstract TagAttribute get(String ns, String localName);

    /**
     * Return an array of all TagAttributes in this set
     * 
     * @return a non-null array of TagAttributes
     */
    public abstract TagAttribute[] getAll();

    /**
     * Get all TagAttributes for the passed namespace
     * 
     * @param namespace
     *            namespace to search
     * @return a non-null array of TagAttributes
     */
    public abstract TagAttribute[] getAll(String namespace);

    /**
     * A list of Namespaces found in this set
     * 
     * @return a list of Namespaces found in this set
     */
    public abstract String[] getNamespaces();
    
    /**
     * @since 2.2
     * @return 
     */
    public Tag getTag()
    {
        return null;
    }

    /**
     * @since 2.2
     * @param tag 
     */
    public void setTag(Tag tag)
    {
    }
}
