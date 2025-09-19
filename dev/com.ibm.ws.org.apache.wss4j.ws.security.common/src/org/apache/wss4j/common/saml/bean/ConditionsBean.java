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

import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Class ConditionsBean represents a SAML Conditions object (can be used to create
 * both SAML v1.1 and v2.0 statements)
 */
public class ConditionsBean {
    private Instant notBefore;
    private Instant notAfter;
    private long tokenPeriodSeconds;
    private List<AudienceRestrictionBean> audienceRestrictions;
    private boolean oneTimeUse;
    private ProxyRestrictionBean proxyRestriction;
    private List<DelegateBean> delegates;

    /**
     * Constructor ConditionsBean creates a new ConditionsBean instance.
     */
    public ConditionsBean() {
    }

    /**
     * Constructor ConditionsBean creates a new ConditionsBean instance.
     *
     * @param notBefore The notBefore instance
     * @param notAfter The notAfter instance
     */
    public ConditionsBean(
        Instant notBefore,
        Instant notAfter
    ) {
        if (notBefore != null) {
            this.notBefore = Date.from(notBefore).toInstant();
        }
        if (notAfter != null) {
            this.notAfter = Date.from(notAfter).toInstant();
        }
    }

    /**
     * Constructor ConditionsBean creates a new ConditionsBean instance.
     *
     * @param tokenPeriodMinutes how long the token is valid for in minutes
     */
    public ConditionsBean(
        int tokenPeriodMinutes
    ) {
        this.tokenPeriodSeconds = tokenPeriodMinutes * 60L;
    }

    /**
     * Get the notBefore instance
     *
     * @return the notBefore instance
     */
    public Instant getNotBefore() {
        return notBefore;
    }

    /**
     * Set the notBefore instance
     *
     * @param notBefore the notBefore instance to set
     */
    public void setNotBefore(Instant notBefore) {
        if (notBefore != null) {
            this.notBefore = Date.from(notBefore).toInstant();
        } else {
            this.notBefore = null;
        }
    }

    /**
     * Get the notAfter instance
     *
     * @return the notAfter instance
     */
    public Instant getNotAfter() {
        return notAfter;
    }

    /**
     * Set the notAfter instance
     *
     * @param notAfter the notAfter instance to set
     */
    public void setNotAfter(Instant notAfter) {
        if (notAfter != null) {
            this.notAfter = Date.from(notAfter).toInstant();
        } else {
            this.notAfter = null;
        }
    }

    /**
     * Get the tokenPeriodMinutes of this object.
     *
     * @return the tokenPeriodMinutes (type int)
     */
    public int getTokenPeriodMinutes() {
        return (int)(tokenPeriodSeconds / 60L);
    }

    /**
     * Set the tokenPeriodMinutes.
     *
     * @param tokenPeriodMinutes the tokenPeriodMinutes to set
     */
    public void setTokenPeriodMinutes(int tokenPeriodMinutes) {
        this.tokenPeriodSeconds = tokenPeriodMinutes * 60L;
    }

    /**
     * Get the tokenPeriodSeconds of this object.
     *
     * @return the tokenPeriodSeconds (type long)
     */
    public long getTokenPeriodSeconds() {
        return tokenPeriodSeconds;
    }

    /**
     * Set the tokenPeriodSeconds.
     *
     * @param tokenPeriodSeconds the tokenPeriodSeconds to set
     */
    public void setTokenPeriodSeconds(long tokenPeriodSeconds) {
        this.tokenPeriodSeconds = tokenPeriodSeconds;
    }

    /**
     * Get the audienceRestrictions instances
     *
     * @return the audienceRestrictions instances
     */
    public List<AudienceRestrictionBean> getAudienceRestrictions() {
        return audienceRestrictions;
    }

    /**
     * Set the audienceRestrictions instance
     *
     * @param audienceRestrictions the audienceRestrictions instance to set
     */
    public void setAudienceRestrictions(List<AudienceRestrictionBean> audienceRestrictions) {
        this.audienceRestrictions = audienceRestrictions;
    }

    /**
     * Get whether to include a OneTimeUse Element or not. Only applies to SAML2.
     * @return whether to include a OneTimeUse Element or not.
     */
    public boolean isOneTimeUse() {
        return oneTimeUse;
    }

    /**
     * Set whether to include a OneTimeUse Element or not. Only applies to SAML2.
     * @param oneTimeUse whether to include a OneTimeUse Element or not.
     */
    public void setOneTimeUse(boolean oneTimeUse) {
        this.oneTimeUse = oneTimeUse;
    }

    public ProxyRestrictionBean getProxyRestriction() {
        return proxyRestriction;
    }

    public void setProxyRestriction(ProxyRestrictionBean proxyRestriction) {
        this.proxyRestriction = proxyRestriction;
    }

    public List<DelegateBean> getDelegates() {
        return delegates;
    }

    public void setDelegates(List<DelegateBean> delegates) {
        this.delegates = delegates;
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
        if (!(o instanceof ConditionsBean)) {
            return false;
        }

        ConditionsBean that = (ConditionsBean) o;

        if (tokenPeriodSeconds != that.tokenPeriodSeconds) {
            return false;
        }

        if (notBefore == null && that.notBefore != null) {
            return false;
        } else if (notBefore != null && !notBefore.equals(that.notBefore)) {
            return false;
        }

        if (notAfter == null && that.notAfter != null) {
            return false;
        } else if (notAfter != null && !notAfter.equals(that.notAfter)) {
            return false;
        }

        if (audienceRestrictions == null && that.audienceRestrictions != null) {
            return false;
        } else if (audienceRestrictions != null
                && !audienceRestrictions.equals(that.audienceRestrictions)) {
            return false;
        }

        if (oneTimeUse != that.oneTimeUse) {
            return false;
        }

        if (proxyRestriction == null && that.proxyRestriction != null) {
            return false;
        } else if (proxyRestriction != null
            && !proxyRestriction.equals(that.proxyRestriction)) {
            return false;
        }

        if (delegates == null && that.delegates != null) {
            return false;
        } else if (delegates != null && !delegates.equals(that.delegates)) {
            return false;
        }

        return true;
    }

    /**
     * @return the hashcode of this object
     */
    @Override
    public int hashCode() {
        int result = (int)tokenPeriodSeconds;
        if (notBefore != null) {
            result = 31 * result + notBefore.hashCode();
        }
        if (notAfter != null) {
            result = 31 * result + notAfter.hashCode();
        }
        if (audienceRestrictions != null) {
            result = 31 * result + audienceRestrictions.hashCode();
        }
        result = 31 * result + (oneTimeUse ? 1 : 0);
        if (proxyRestriction != null) {
            result = 31 * result + proxyRestriction.hashCode();
        }
        if (delegates != null) {
            result = 31 * result + delegates.hashCode();
        }
        return result;
    }

}
