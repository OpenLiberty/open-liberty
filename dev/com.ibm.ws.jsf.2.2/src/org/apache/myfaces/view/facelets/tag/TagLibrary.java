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

/**
 * A library of Tags associated with one or more namespaces.
 * 
 * @author Jacob Hookom
 * @version $Id: TagLibrary.java 1187701 2011-10-22 12:21:54Z bommel $
 */
public interface TagLibrary
{

    /**
     * If this library contains the passed namespace
     * 
     * @param ns
     *            namespace
     * @return true if the namespace is used in this library
     */
    public boolean containsNamespace(String ns);

    /**
     * If this library contains a TagHandler for the namespace and local name
     * 
     * @param ns
     *            namespace
     * @param localName
     *            local name
     * @return true if handled by this library
     */
    public boolean containsTagHandler(String ns, String localName);

    /**
     * Create a new instance of a TagHandler, using the passed TagConfig
     * 
     * @param ns
     *            namespace
     * @param localName
     *            local name
     * @param tag
     *            configuration information
     * @return a new TagHandler instance
     * @throws FacesException
     */
    public TagHandler createTagHandler(String ns, String localName, TagConfig tag) throws FacesException;

    /**
     * If this library contains the specified function name
     * 
     * @param ns
     *            namespace
     * @param name
     *            function name
     * @return true if handled
     */
    public boolean containsFunction(String ns, String name);

    /**
     * Return a Method instance for the passed namespace and name
     * 
     * @param ns
     *            namespace
     * @param name
     *            function name
     * @return
     */
    public Method createFunction(String ns, String name);
}
