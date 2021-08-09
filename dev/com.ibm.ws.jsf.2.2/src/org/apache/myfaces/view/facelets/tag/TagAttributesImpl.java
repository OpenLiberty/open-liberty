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
package org.apache.myfaces.view.facelets.tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributes;

/**
 * A set of TagAttributes, usually representing all attributes on a Tag.
 * 
 * TODO: PROFILE - Explore the possibility of using HashMap instead of sorted arrays. 
 *       The footprint should be higher, but the instanciation and access speed should be faster 
 *       Instanciation: from O(n log n) to O(1)
 *       Access: from O(log n) to O(1)
 * 
 * @see org.apache.myfaces.view.facelets.tag.Tag
 * @see org.apache.myfaces.view.facelets.tag.TagAttributeImpl
 * @author Jacob Hookom
 * @version $Id: TagAttributesImpl.java 1187701 2011-10-22 12:21:54Z bommel $
 */
public final class TagAttributesImpl extends TagAttributes
{
    private final static TagAttribute[] EMPTY = new TagAttribute[0];

    private final TagAttribute[] _attributes;

    private final String[] _namespaces;

    private final List<TagAttribute[]> _nsattrs;

    /**
     * 
     */
    public TagAttributesImpl(TagAttribute[] attrs)
    {
        _attributes = attrs;

        // grab namespaces
        Set<String> set = new HashSet<String>();
        for (TagAttribute attribute : _attributes)
        {
            set.add(attribute.getNamespace());
        }
        
        _namespaces = set.toArray(new String[set.size()]);
        Arrays.sort(_namespaces);

        // assign attrs
        int size = _namespaces.length;
        List<List<TagAttribute>> temp = new ArrayList<List<TagAttribute>>(size);
        for (int i = 0; i < size; i++)
        {
            temp.add(new ArrayList<TagAttribute>());
        }
        
        for (TagAttribute attribute : _attributes)
        {
            temp.get(Arrays.binarySearch(_namespaces, attribute.getNamespace())).add(attribute);
        }
        
        _nsattrs = new ArrayList<TagAttribute[]>(size);
        for (int i = 0; i < size; i++)
        {
            List<TagAttribute> l = temp.get(i);
            _nsattrs.add(l.toArray(new TagAttribute[l.size()]));
        }
    }

    /**
     * Return an array of all TagAttributes in this set
     * 
     * @return a non-null array of TagAttributes
     */
    public TagAttribute[] getAll()
    {
        return _attributes;
    }

    /**
     * Using no namespace, find the TagAttribute
     * 
     * @see #get(String, String)
     * @param localName
     *            tag attribute name
     * @return the TagAttribute found, otherwise null
     */
    public TagAttribute get(String localName)
    {
        return get("", localName);
    }

    /**
     * Find a TagAttribute that matches the passed namespace and local name.
     * 
     * @param ns
     *            namespace of the desired attribute
     * @param localName
     *            local name of the attribute
     * @return a TagAttribute found, otherwise null
     */
    public TagAttribute get(String ns, String localName)
    {
        if (ns != null && localName != null)
        {
            int idx = Arrays.binarySearch(_namespaces, ns);
            if (idx >= 0)
            {
                for (TagAttribute attribute : _nsattrs.get(idx))
                {
                    if (localName.equals(attribute.getLocalName()))
                    {
                        return attribute;
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Get all TagAttributes for the passed namespace
     * 
     * @param namespace
     *            namespace to search
     * @return a non-null array of TagAttributes
     */
    public TagAttribute[] getAll(String namespace)
    {
        int idx = 0;
        if (namespace == null)
        {
            idx = Arrays.binarySearch(_namespaces, "");
        }
        else
        {
            idx = Arrays.binarySearch(_namespaces, namespace);
        }
        
        if (idx >= 0)
        {
            return _nsattrs.get(idx);
        }
        
        return EMPTY;
    }

    /**
     * A list of Namespaces found in this set
     * 
     * @return a list of Namespaces found in this set
     */
    public String[] getNamespaces()
    {
        return _namespaces;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        for (TagAttribute attribute : _attributes)
        {
            sb.append(attribute);
            sb.append(' ');
        }
        
        if (sb.length() > 1)
        {
            sb.setLength(sb.length() - 1);
        }
        
        return sb.toString();
    }
}
