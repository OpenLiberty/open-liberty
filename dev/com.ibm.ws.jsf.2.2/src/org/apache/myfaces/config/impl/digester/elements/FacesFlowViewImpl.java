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
package org.apache.myfaces.config.impl.digester.elements;

/**
 *
 * @author Leonardo Uribe
 */
public class FacesFlowViewImpl extends org.apache.myfaces.config.element.FacesFlowView
{
    private String _id;
    private String _vdlDocument;

    public FacesFlowViewImpl()
    {
    }
    
    @Override
    public String getVdlDocument()
    {
        return _vdlDocument;
    }

    /**
     * @param vdlDocument the vdlDocument to set
     */
    public void setVdlDocument(String vdlDocument)
    {
        this._vdlDocument = vdlDocument;
    }
    
    @Override
    public String getId()
    {
        return _id;
    }

    public void setId(String id)
    {
        this._id = id;
    }

}
