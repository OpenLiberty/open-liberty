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
package org.apache.myfaces.view.facelets.tag.jsf;

import org.apache.myfaces.view.facelets.tag.AbstractTagLibrary;

/**
 * Dummy library, so passthrough namespace can be recognized by facelet compiler.
 * 
 * No real components or tags here, this is only used for attributes.
 * 
 * @author Leonardo Uribe
 */
public class PassThroughLibrary extends AbstractTagLibrary
{
    public final static String NAMESPACE = "http://xmlns.jcp.org/jsf/passthrough";
    public final static String ALIAS_NAMESPACE = "http://java.sun.com/jsf/passthrough";

    public final static PassThroughLibrary INSTANCE = new PassThroughLibrary();
    
    public PassThroughLibrary()
    {
        super(NAMESPACE, ALIAS_NAMESPACE);
    }
}
