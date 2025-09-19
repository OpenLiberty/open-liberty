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

/**
 * Class DelegateBean represents a SAML 2.0 Delegate object. Only NameIDs are supported for now, not
 * BaseID or EncryptedIDs.
 *
 * See:
 * http://docs.oasis-open.org/security/saml/Post2.0/sstc-saml-delegation-cs-01.pdf
 */
public class DelegateBean {
    private Instant delegationInstant;
    private String confirmationMethod;
    private NameIDBean nameIDBean;

    public Instant getDelegationInstant() {
        return delegationInstant;
    }

    public void setDelegationInstant(Instant delegationInstant) {
        if (delegationInstant != null) {
            this.delegationInstant = Date.from(delegationInstant).toInstant();
        } else {
            this.delegationInstant = null;
        }
    }

    public String getConfirmationMethod() {
        return confirmationMethod;
    }

    public void setConfirmationMethod(String confirmationMethod) {
        this.confirmationMethod = confirmationMethod;
    }

    public NameIDBean getNameIDBean() {
        return nameIDBean;
    }

    public void setNameIDBean(NameIDBean nameIDBean) {
        this.nameIDBean = nameIDBean;
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
        if (!(o instanceof DelegateBean)) {
            return false;
        }

        DelegateBean that = (DelegateBean) o;

        if (delegationInstant == null && that.delegationInstant != null) {
            return false;
        } else if (delegationInstant != null && !delegationInstant.equals(that.delegationInstant)) {
            return false;
        }

        if (confirmationMethod == null && that.confirmationMethod != null) {
            return false;
        } else if (confirmationMethod != null && !confirmationMethod.equals(that.confirmationMethod)) {
            return false;
        }

        if (nameIDBean == null && that.nameIDBean != null) {
            return false;
        } else if (nameIDBean != null && !nameIDBean.equals(that.nameIDBean)) {
            return false;
        }

        return true;
    }

    /**
     * @return the hashcode of this object
     */
    @Override
    public int hashCode() {
        int result = 0;
        if (delegationInstant != null) {
            result = 31 * result + delegationInstant.hashCode();
        }
        if (confirmationMethod != null) {
            result = 31 * result + confirmationMethod.hashCode();
        }
        if (nameIDBean != null) {
            result = 31 * result + nameIDBean.hashCode();
        }
        return result;
    }

}
