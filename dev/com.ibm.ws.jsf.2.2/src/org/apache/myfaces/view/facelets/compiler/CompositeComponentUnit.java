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
package org.apache.myfaces.view.facelets.compiler;

import javax.faces.view.facelets.FaceletHandler;

import org.apache.myfaces.view.facelets.tag.composite.CompositeComponentDefinitionTagHandler;

/**
 * This compilation unit is used to wrap cc:interface and cc:implementation in
 * a base handler, to allow proper handling of composite component metadata.
 * 
 * @author Leonardo Uribe (latest modification by $Author: bommel $)
 * @version $Revision: 1187701 $ $Date: 2011-10-22 12:21:54 +0000 (Sat, 22 Oct 2011) $
 */
class CompositeComponentUnit extends CompilationUnit
{

    public CompositeComponentUnit()
    {
    }

    public FaceletHandler createFaceletHandler()
    {
        return new CompositeComponentDefinitionTagHandler(this.getNextFaceletHandler());
    }

}
