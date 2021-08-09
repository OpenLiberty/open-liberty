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
package org.apache.myfaces.view.facelets.tag.jsf.html;

import javax.faces.view.facelets.Tag;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagDecorator;

import org.apache.myfaces.view.facelets.tag.TagAttributesImpl;

/**
 * @author Jacob Hookom
 * @version $Id: HtmlDecorator.java 1189926 2011-10-27 18:36:29Z struberg $
 */
public final class HtmlDecorator implements TagDecorator
{

    public final static String XHTML_NAMESPACE = "http://www.w3.org/1999/xhtml";

    public final static HtmlDecorator INSTANCE = new HtmlDecorator();

    /**
     * 
     */
    public HtmlDecorator()
    {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.myfaces.view.facelets.tag.TagDecorator#decorate(org.apache.myfaces.view.facelets.tag.Tag)
     */
    public Tag decorate(Tag tag)
    {
        if (XHTML_NAMESPACE.equals(tag.getNamespace()))
        {
            String n = tag.getLocalName();
            if ("a".equals(n))
            {
                return new Tag(tag.getLocation(), HtmlLibrary.NAMESPACE, "commandLink", tag.getQName(), tag
                        .getAttributes());
            }
            if ("form".equals(n))
            {
                return new Tag(tag.getLocation(), HtmlLibrary.NAMESPACE, "form", tag.getQName(), tag.getAttributes());
            }
            if ("input".equals(n))
            {
                TagAttribute attr = tag.getAttributes().get("type");
                if (attr != null)
                {
                    String t = attr.getValue();
                    TagAttributes na = removeType(tag.getAttributes());
                    if ("text".equals(t))
                    {
                        return new Tag(tag.getLocation(), HtmlLibrary.NAMESPACE, "inputText", tag.getQName(), na);
                    }
                    if ("password".equals(t))
                    {
                        return new Tag(tag.getLocation(), HtmlLibrary.NAMESPACE, "inputSecret", tag.getQName(), na);
                    }
                    if ("hidden".equals(t))
                    {
                        return new Tag(tag.getLocation(), HtmlLibrary.NAMESPACE, "inputHidden", tag.getQName(), na);
                    }
                    if ("submit".equals(t))
                    {
                        return new Tag(tag.getLocation(), HtmlLibrary.NAMESPACE, "commandButton", tag.getQName(), na);
                    }
                }
            }
        }
        return null;
    }

    private static TagAttributes removeType(TagAttributes attrs)
    {
        TagAttribute[] o = attrs.getAll();
        TagAttribute[] a = new TagAttribute[o.length - 1];
        int p = 0;
        for (int i = 0; i < o.length; i++)
        {
            if (!"type".equals(o[i].getLocalName()))
            {
                a[p++] = o[i];
            }
        }
        return new TagAttributesImpl(a);
    }

}
