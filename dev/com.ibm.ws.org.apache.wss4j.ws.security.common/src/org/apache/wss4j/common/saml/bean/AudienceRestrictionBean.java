/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.wss4j.common.saml.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * Class AudienceRestrictionBean represents a SAML AudienceRestriction object
 */
public class AudienceRestrictionBean {
    private final List<String> audienceURIs = new ArrayList<>();

    /**
     * Constructor AudienceRestrictionBean creates a new AudienceRestrictionBean instance.
     */
    public AudienceRestrictionBean() {
    }

    /**
     * Constructor AudienceRestrictionBean creates a new AudienceRestrictionBean instance.
     *
     * @param audienceURIs The audienceURI instances
     */
    public AudienceRestrictionBean(
        List<String> audienceURIs
    ) {
        if (audienceURIs != null) {
            this.audienceURIs.addAll(audienceURIs);
        }
    }

    /**
     * Get the audienceURI instances
     *
     * @return the audienceURI instances
     */
    public List<String> getAudienceURIs() {
        return audienceURIs;
    }

    /**
     * Set the audienceURI instance
     *
     * @param audienceURIs the audienceURI instances to set
     */
    public void setAudienceURIs(List<String> audienceURIs) {
        this.audienceURIs.clear();
        this.audienceURIs.addAll(audienceURIs);
    }

    /**
     * Method equals ...
     *
     * @param o of type Object
     * @return boolean
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AudienceRestrictionBean)) {
            return false;
        }

        AudienceRestrictionBean that = (AudienceRestrictionBean) o;

        return audienceURIs.equals(that.audienceURIs);
    }

    /**
     * @return the hashcode of this object
     */
    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + audienceURIs.hashCode();
        return result;
    }

}
