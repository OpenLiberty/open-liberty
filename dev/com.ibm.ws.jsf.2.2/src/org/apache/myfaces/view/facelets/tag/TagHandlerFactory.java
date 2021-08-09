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

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

/**
 * Delegate class for TagLibraries
 * 
 * @see TagLibrary
 * @author Jacob Hookom
 * @version $Id: TagHandlerFactory.java 1187701 2011-10-22 12:21:54Z bommel $
 */
interface TagHandlerFactory
{
    /**
     * A new TagHandler instantiated with the passed TagConfig
     * 
     * @param cfg
     *            TagConfiguration information
     * @return a new TagHandler
     * @throws FacesException
     * @throws ELException
     */
    public TagHandler createHandler(TagConfig cfg) throws FacesException, ELException;
}
