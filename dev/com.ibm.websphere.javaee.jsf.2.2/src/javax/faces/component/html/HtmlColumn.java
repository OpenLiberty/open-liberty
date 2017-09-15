/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package javax.faces.component.html;

// Generated from class javax.faces.component.html._HtmlColumn.
//
// WARNING: This file was automatically generated. Do not edit it directly,
//          or you will lose your changes.
public class HtmlColumn extends javax.faces.component.UIColumn
{

    static public final String COMPONENT_FAMILY =
        "javax.faces.Column";
    static public final String COMPONENT_TYPE =
        "javax.faces.HtmlColumn";


    public HtmlColumn()
    {
        setRendererType(null);
    }

    @Override    
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }



    
    // Property: headerClass

    public String getHeaderClass()
    {
        return (String) getStateHelper().eval(PropertyKeys.headerClass);
    }
    
    public void setHeaderClass(String headerClass)
    {
        getStateHelper().put(PropertyKeys.headerClass, headerClass ); 
    }
    // Property: footerClass

    public String getFooterClass()
    {
        return (String) getStateHelper().eval(PropertyKeys.footerClass);
    }
    
    public void setFooterClass(String footerClass)
    {
        getStateHelper().put(PropertyKeys.footerClass, footerClass ); 
    }
    // Property: rowHeader

    public boolean isRowHeader()
    {
        return (Boolean) getStateHelper().eval(PropertyKeys.rowHeader, false);
    }
    
    public void setRowHeader(boolean rowHeader)
    {
        getStateHelper().put(PropertyKeys.rowHeader, rowHeader ); 
    }


    protected enum PropertyKeys
    {
         headerClass
        , footerClass
        , rowHeader
    }

 }
