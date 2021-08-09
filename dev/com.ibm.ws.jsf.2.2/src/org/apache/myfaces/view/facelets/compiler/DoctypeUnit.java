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

import javax.faces.view.facelets.CompositeFaceletHandler;
import javax.faces.view.facelets.FaceletHandler;
import org.apache.myfaces.view.facelets.el.ELText;

/**
 *
 * @author Leonardo Uribe
 */
final class DoctypeUnit extends CompilationUnit
{
    private final String alias;

    private final String id;
    
    private final String name;
    
    private final String publicId;
    
    private final String systemId;
    
    private final boolean html5Doctype;

    public DoctypeUnit(String alias, String id, String name, String publicId, String systemId, boolean html5Doctype)
    {
        this.alias = alias;
        this.id = id;
        this.name = name;
        this.publicId = publicId;
        this.systemId = systemId;
        this.html5Doctype = html5Doctype;
    }
    
    public FaceletHandler createFaceletHandler()
    {
        FaceletHandler[] h = new FaceletHandler[2];
        h[0] = new UIInstructionHandler(this.alias, this.id, 
            new Instruction[]{
                new DoctypeInstruction(
                    this.name, 
                    this.publicId, 
                    this.systemId, 
                    this.html5Doctype)
            }, new ELText(""));
        h[1] = this.getNextFaceletHandler();
        return new CompositeFaceletHandler(h);
    }
}
