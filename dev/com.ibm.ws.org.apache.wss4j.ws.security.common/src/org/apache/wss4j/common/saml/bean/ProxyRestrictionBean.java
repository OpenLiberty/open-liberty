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
 * Class ProxyRestrictionBean represents a SAML 2.0 ProxyRestrictionBean object
 */
public class ProxyRestrictionBean {
    private int count;
    private final List<String> audienceURIs = new ArrayList<>();

    /**
     * Constructor ProxyRestrictionBean creates a new ProxyRestrictionBean instance.
     */
    public ProxyRestrictionBean() {
    }

    /**
     * Constructor ProxyRestrictionBean creates a new ProxyRestrictionBean instance.
     *
     * @param count The count instance
     * @param audienceURIs The audienceURI instances
     */
    public ProxyRestrictionBean(
        int count,
        List<String> audienceURIs
    ) {
        this.setCount(count);
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

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
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
        if (!(o instanceof ProxyRestrictionBean)) {
            return false;
        }

        ProxyRestrictionBean that = (ProxyRestrictionBean) o;

        if (count != that.count) {
            return false;
        }

        return audienceURIs.equals(that.audienceURIs);
    }

    /**
     * @return the hashcode of this object
     */
    @Override
    public int hashCode() {
        int result = count;
        result = 31 * result + audienceURIs.hashCode();
        return result;
    }

}
