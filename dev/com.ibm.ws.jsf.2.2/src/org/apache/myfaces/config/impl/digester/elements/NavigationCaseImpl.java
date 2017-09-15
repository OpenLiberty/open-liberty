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

import java.io.Serializable;





/**
 * @author <a href="mailto:oliver@rossmueller.com">Oliver Rossmueller</a>
 */
public class NavigationCaseImpl extends org.apache.myfaces.config.element.NavigationCase implements Serializable
{

    private String fromAction;
    private String fromOutcome;
    private String ifValue;
    private String toViewId;
    private org.apache.myfaces.config.element.Redirect redirect;


    public String getFromAction()
    {
        return fromAction;
    }


    public void setFromAction(String fromAction)
    {
        this.fromAction = fromAction;
    }


    public String getFromOutcome()
    {
        return fromOutcome;
    }


    public void setFromOutcome(String fromOutcome)
    {
        this.fromOutcome = fromOutcome;
    }
    
    public String getIf ()
    {
        return ifValue;
    }
    
    public void setIf (String ifValue)
    {
        this.ifValue = ifValue;
    }
    
    public String getToViewId()
    {
        return toViewId;
    }


    public void setToViewId(String toViewId)
    {
        this.toViewId = toViewId;
    }


    public void setRedirect(org.apache.myfaces.config.element.Redirect redirect)
    {
        this.redirect = redirect;
    }


    public org.apache.myfaces.config.element.Redirect getRedirect()
    {
        return redirect;
    }
}
