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

import org.apache.myfaces.view.facelets.tag.AbstractTagLibrary;

/**
 * @author Jacob Hookom
 * @version $Id: AbstractHtmlLibrary.java 1477272 2013-04-29 19:10:56Z lu4242 $
 */
public abstract class AbstractHtmlLibrary extends AbstractTagLibrary
{

    /**
     * @param namespace
     */
    public AbstractHtmlLibrary(String namespace)
    {
        super(namespace);
    }
    
    public AbstractHtmlLibrary(String namespace, String aliasNamespace)
    {
        super(namespace, aliasNamespace);
    }    

    public void addHtmlComponent(String name, String componentType, String rendererType)
    {
        super.addComponent(name, componentType, rendererType, HtmlComponentHandler.class);
    }

}
