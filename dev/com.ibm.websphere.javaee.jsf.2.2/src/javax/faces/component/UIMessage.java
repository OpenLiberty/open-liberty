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
package javax.faces.component;

// Generated from class javax.faces.component._UIMessage.
//
// WARNING: This file was automatically generated. Do not edit it directly,
//          or you will lose your changes.
public class UIMessage extends javax.faces.component.UIComponentBase
{

    static public final String COMPONENT_FAMILY =
        "javax.faces.Message";
    static public final String COMPONENT_TYPE =
        "javax.faces.Message";


    public UIMessage()
    {
        setRendererType("javax.faces.Message");
    }

    @Override    
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }



    
    // Property: for

    public String getFor()
    {
        return (String) getStateHelper().eval(PropertyKeys.forVal);
    }
    
    public void setFor(String forParam)
    {
        getStateHelper().put(PropertyKeys.forVal, forParam ); 
    }
    // Property: showDetail

    public boolean isShowDetail()
    {
        return (Boolean) getStateHelper().eval(PropertyKeys.showDetail, true);
    }
    
    public void setShowDetail(boolean showDetail)
    {
        getStateHelper().put(PropertyKeys.showDetail, showDetail ); 
    }
    // Property: showSummary

    public boolean isShowSummary()
    {
        return (Boolean) getStateHelper().eval(PropertyKeys.showSummary, false);
    }
    
    public void setShowSummary(boolean showSummary)
    {
        getStateHelper().put(PropertyKeys.showSummary, showSummary ); 
    }
    // Property: redisplay

    public boolean isRedisplay()
    {
        return (Boolean) getStateHelper().eval(PropertyKeys.redisplay, true);
    }
    
    public void setRedisplay(boolean redisplay)
    {
        getStateHelper().put(PropertyKeys.redisplay, redisplay ); 
    }


    enum PropertyKeys
    {
         forVal("for")
        , showDetail
        , showSummary
        , redisplay
        ;
        String c;
        
        PropertyKeys()
        {
        }
        
        //Constructor needed by "for" property
        PropertyKeys(String c)
        { 
            this.c = c;
        }
        
        public String toString()
        {
            return ((this.c != null) ? this.c : super.toString());
        }
    }

 }
