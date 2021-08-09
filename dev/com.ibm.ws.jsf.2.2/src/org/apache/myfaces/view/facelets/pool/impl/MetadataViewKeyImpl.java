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
package org.apache.myfaces.view.facelets.pool.impl;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;

/**
 *
 * @author Leonardo Uribe
 */
public class MetadataViewKeyImpl extends MetadataViewKey implements Serializable
{
    private final Locale locale;
    
    private final String viewId;
    
    private final String[] contracts;
    
    private final String renderKitId;

    public MetadataViewKeyImpl(String viewId, String renderKitId, Locale locale)
    {
        this.viewId = viewId;
        this.renderKitId = renderKitId;
        this.locale = locale;
        this.contracts = null;
    }

    public MetadataViewKeyImpl(String viewId, String renderKitId, Locale locale, String[] contracts)
    {
        this.viewId = viewId;
        this.renderKitId = renderKitId;
        this.locale = locale;
        this.contracts = contracts;
    }

    /**
     * @return the locale
     */
    public Locale getLocale()
    {
        return locale;
    }

    /**
     * @return the viewId
     */
    @Override
    public String getViewId()
    {
        return viewId;
    }

    /**
     * @return the contracts
     */
    public String[] getContracts()
    {
        return Arrays.copyOf(contracts, contracts.length);
    }

    /**
     * @return the renderKitId
     */
    public String getRenderKitId()
    {
        return renderKitId;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 17 * hash + (this.locale != null ? this.locale.hashCode() : 0);
        hash = 17 * hash + (this.viewId != null ? this.viewId.hashCode() : 0);
        hash = 17 * hash + Arrays.deepHashCode(this.contracts);
        hash = 17 * hash + (this.renderKitId != null ? this.renderKitId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final MetadataViewKeyImpl other = (MetadataViewKeyImpl) obj;
        if (this.locale != other.locale && (this.locale == null || !this.locale.equals(other.locale)))
        {
            return false;
        }
        if ((this.viewId == null) ? (other.viewId != null) : !this.viewId.equals(other.viewId))
        {
            return false;
        }
        if (!Arrays.deepEquals(this.contracts, other.contracts))
        {
            return false;
        }
        if ((this.renderKitId == null) ? (other.renderKitId != null) : !this.renderKitId.equals(other.renderKitId))
        {
            return false;
        }
        return true;
    }

}
