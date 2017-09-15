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
 * Foundation class for FaceletHandlers associated with markup in a Facelet document.
 */
public abstract class TagHandler implements FaceletHandler
{
    protected final String tagId;

    protected final Tag tag;

    protected final FaceletHandler nextHandler;

    public TagHandler(TagConfig config)
    {
        this.tagId = config.getTagId();
        this.tag = config.getTag();
        this.nextHandler = config.getNextHandler();
    }

    /**
     * Utility method for fetching the appropriate TagAttribute
     * 
     * @param localName
     *            name of attribute
     * @return TagAttribute if found, otherwise null
     */
    protected final TagAttribute getAttribute(String localName)
    {
        return this.tag.getAttributes().get(localName);
    }

    /**
     * Utility method for fetching a required TagAttribute
     * 
     * @param localName
     *            name of the attribute
     * @return TagAttribute if found, otherwise error
     * @throws TagException
     *             if the attribute was not found
     */
    protected final TagAttribute getRequiredAttribute(String localName) throws TagException
    {
        TagAttribute attr = this.getAttribute(localName);
        if (attr == null)
        {
            throw new TagException(this.tag, "Attribute '" + localName + "' is required");
        }
        
        return attr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return this.tag.toString();
    }
}
