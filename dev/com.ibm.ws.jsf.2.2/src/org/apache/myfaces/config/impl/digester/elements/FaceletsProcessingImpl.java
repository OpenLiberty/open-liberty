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
 * @since 2.1.0
 */
public class FaceletsProcessingImpl extends org.apache.myfaces.config.element.FaceletsProcessing 
{
    /**
     * 
     */
    private static final long serialVersionUID = 7692451499973040255L;

    private String fileExtension;
    
    private String processAs;
    
    private String oamCompressSpaces;

    public String getFileExtension()
    {
        return fileExtension;
    }

    public String getProcessAs()
    {
        return processAs;
    }

    public void setFileExtension(String fileExtension)
    {
        this.fileExtension = fileExtension;
    }

    public void setProcessAs(String processAs)
    {
        this.processAs = processAs;
    }

    /**
     * @return the oamCompressSpaces
     */
    @Override
    public String getOamCompressSpaces()
    {
        return oamCompressSpaces;
    }

    /**
     * @param oamCompressSpaces the oamCompressSpaces to set
     */
    public void setOamCompressSpaces(String oamCompressSpaces)
    {
        this.oamCompressSpaces = oamCompressSpaces;
    }
}
