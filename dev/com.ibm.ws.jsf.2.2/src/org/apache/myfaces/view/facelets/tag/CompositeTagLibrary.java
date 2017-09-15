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

import java.lang.reflect.Method;

import javax.faces.FacesException;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.apache.myfaces.view.facelets.util.ParameterCheck;

/**
 * A TagLibrary that is composed of 1 or more TagLibrary children. Uses the chain of responsibility pattern to stop
 * searching as soon as one of the children handles the requested method.
 * 
 * @author Jacob Hookom
 * @version $Id: CompositeTagLibrary.java 1187701 2011-10-22 12:21:54Z bommel $
 */
public final class CompositeTagLibrary implements TagLibrary
{

    private final TagLibrary[] libraries;

    public CompositeTagLibrary(TagLibrary[] libraries)
    {
        ParameterCheck.notNull("libraries", libraries);
        this.libraries = libraries;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.myfaces.view.facelets.tag.TagLibrary#containsNamespace(java.lang.String)
     */
    public boolean containsNamespace(String ns)
    {
        for (int i = 0; i < this.libraries.length; i++)
        {
            if (this.libraries[i].containsNamespace(ns))
            {
                return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.myfaces.view.facelets.tag.TagLibrary#containsTagHandler(java.lang.String, java.lang.String)
     */
    public boolean containsTagHandler(String ns, String localName)
    {
        for (int i = 0; i < this.libraries.length; i++)
        {
            if (this.libraries[i].containsTagHandler(ns, localName))
            {
                return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.myfaces.view.facelets.tag.TagLibrary#createTagHandler(java.lang.String, java.lang.String,
     * org.apache.myfaces.view.facelets.tag.TagConfig)
     */
    public TagHandler createTagHandler(String ns, String localName, TagConfig tag) throws FacesException
    {
        for (int i = 0; i < this.libraries.length; i++)
        {
            if (this.libraries[i].containsTagHandler(ns, localName))
            {
                return this.libraries[i].createTagHandler(ns, localName, tag);
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.myfaces.view.facelets.tag.TagLibrary#containsFunction(java.lang.String, java.lang.String)
     */
    public boolean containsFunction(String ns, String name)
    {
        for (int i = 0; i < this.libraries.length; i++)
        {
            if (this.libraries[i].containsFunction(ns, name))
            {
                return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.myfaces.view.facelets.tag.TagLibrary#createFunction(java.lang.String, java.lang.String)
     */
    public Method createFunction(String ns, String name)
    {
        for (int i = 0; i < this.libraries.length; i++)
        {
            if (this.libraries[i].containsFunction(ns, name))
            {
                return this.libraries[i].createFunction(ns, name);
            }
        }
        return null;
    }
}
