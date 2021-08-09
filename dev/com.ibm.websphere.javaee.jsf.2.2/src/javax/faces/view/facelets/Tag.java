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

import javax.faces.view.Location;

/**
 * Representation of a Tag in the Facelet definition
 */
public final class Tag
{
    private final TagAttributes attributes;

    private final Location location;

    private final String namespace;

    private final String localName;

    private final String qName;

    public Tag(Location location, String namespace, String localName, String qName, TagAttributes attributes)
    {
        this.location = location;
        this.namespace = namespace;
        this.localName = localName;
        this.qName = qName;
        this.attributes = attributes;
    }

    public Tag(Tag orig, TagAttributes attributes)
    {
        this(orig.getLocation(), orig.getNamespace(), orig.getLocalName(), orig.getQName(), attributes);
    }

    /**
     * All TagAttributes specified
     * 
     * @return all TagAttributes specified
     */
    public TagAttributes getAttributes()
    {
        return attributes;
    }

    /**
     * Local name of the tag &lt;my:tag /> would be "tag"
     * 
     * @return local name of the tag
     */
    public String getLocalName()
    {
        return localName;
    }

    /**
     * Location of the Tag in the Facelet file
     * 
     * @return location of the Tag in the Facelet file
     */
    public Location getLocation()
    {
        return location;
    }

    /**
     * The resolved Namespace for this tag
     * 
     * @return the resolved namespace for this tag
     */
    public String getNamespace()
    {
        return namespace;
    }

    /**
     * Get the qualified name for this tag &lt;my:tag /> would be "my:tag"
     * 
     * @return qualified name of the tag
     */
    public String getQName()
    {
        return qName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return this.location + " <" + this.qName + ">";
    }
}
