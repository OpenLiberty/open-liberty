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
 * @author Mathias Broekelmann (latest modification by $Author: lu4242 $)
 * @version $Revision: 1537800 $ $Date: 2013-11-01 01:55:08 +0000 (Fri, 01 Nov 2013) $
 */
public class ResourceBundleImpl extends org.apache.myfaces.config.element.ResourceBundle implements Serializable
{
    private String baseName;
    private String var;
    private String displayName;

    /**
     * @return the baseName
     */
    public String getBaseName()
    {
        return baseName;
    }

    /**
     * @param baseName
     *            the baseName to set
     */
    public void setBaseName(String baseName)
    {
        this.baseName = baseName;
    }

    /**
     * @return the var
     */
    public String getVar()
    {
        return var;
    }

    /**
     * @param var
     *            the var to set
     */
    public void setVar(String var)
    {
        this.var = var;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }
}
