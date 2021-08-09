/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.scim20.model.serviceprovider;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.websphere.security.wim.scim20.model.serviceprovider.AuthenticationScheme;
import com.ibm.websphere.security.wim.scim20.model.serviceprovider.Bulk;
import com.ibm.websphere.security.wim.scim20.model.serviceprovider.ChangePassword;
import com.ibm.websphere.security.wim.scim20.model.serviceprovider.ETag;
import com.ibm.websphere.security.wim.scim20.model.serviceprovider.Filter;
import com.ibm.websphere.security.wim.scim20.model.serviceprovider.Patch;
import com.ibm.websphere.security.wim.scim20.model.serviceprovider.ServiceProviderConfig;
import com.ibm.websphere.security.wim.scim20.model.serviceprovider.Sort;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
@JsonPropertyOrder(value = { "documentationUri", "patch", "bulk", "filter", "changePassword", "sort", "etag",
                             "authenticationSchemes" })
public class ServiceProviderConfigImpl implements ServiceProviderConfig {

    @JsonProperty("authenticationSchemes")
    private List<AuthenticationScheme> authenticationSchemes;

    @JsonProperty("bulk")
    private Bulk bulk;

    @JsonProperty("changePassword")
    private ChangePassword changePassword;

    @JsonProperty("documentationUri")
    private String documentationUri;

    @JsonProperty("etag")
    private ETag etag;

    @JsonProperty("filter")
    private Filter filter;

    @JsonProperty("patch")
    private Patch patch;

    @JsonProperty("sort")
    private Sort sort;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ServiceProviderConfigImpl other = (ServiceProviderConfigImpl) obj;
        if (authenticationSchemes == null) {
            if (other.authenticationSchemes != null) {
                return false;
            }
        } else if (!authenticationSchemes.equals(other.authenticationSchemes)) {
            return false;
        }
        if (bulk == null) {
            if (other.bulk != null) {
                return false;
            }
        } else if (!bulk.equals(other.bulk)) {
            return false;
        }
        if (changePassword == null) {
            if (other.changePassword != null) {
                return false;
            }
        } else if (!changePassword.equals(other.changePassword)) {
            return false;
        }
        if (documentationUri == null) {
            if (other.documentationUri != null) {
                return false;
            }
        } else if (!documentationUri.equals(other.documentationUri)) {
            return false;
        }
        if (etag == null) {
            if (other.etag != null) {
                return false;
            }
        } else if (!etag.equals(other.etag)) {
            return false;
        }
        if (filter == null) {
            if (other.filter != null) {
                return false;
            }
        } else if (!filter.equals(other.filter)) {
            return false;
        }
        if (patch == null) {
            if (other.patch != null) {
                return false;
            }
        } else if (!patch.equals(other.patch)) {
            return false;
        }
        if (sort == null) {
            if (other.sort != null) {
                return false;
            }
        } else if (!sort.equals(other.sort)) {
            return false;
        }
        return true;
    }

    @Override
    public List<AuthenticationScheme> getAuthenticationSchemes() {
        return this.authenticationSchemes;
    }

    @Override
    public Bulk getBulk() {
        return this.bulk;
    }

    @Override
    public ChangePassword getChangePassword() {
        return this.changePassword;
    }

    @Override
    public String getDocumentationUri() {
        return this.documentationUri;
    }

    @Override
    public ETag getEtag() {
        return this.etag;
    }

    @Override
    public Filter getFilter() {
        return this.filter;
    }

    @Override
    public Patch getPatch() {
        return this.patch;
    }

    @Override
    public Sort getSort() {
        return this.sort;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((authenticationSchemes == null) ? 0 : authenticationSchemes.hashCode());
        result = prime * result + ((bulk == null) ? 0 : bulk.hashCode());
        result = prime * result + ((changePassword == null) ? 0 : changePassword.hashCode());
        result = prime * result + ((documentationUri == null) ? 0 : documentationUri.hashCode());
        result = prime * result + ((etag == null) ? 0 : etag.hashCode());
        result = prime * result + ((filter == null) ? 0 : filter.hashCode());
        result = prime * result + ((patch == null) ? 0 : patch.hashCode());
        result = prime * result + ((sort == null) ? 0 : sort.hashCode());
        return result;
    }

    /**
     * Set the list of supported authentication schemes.
     *
     * @param authenticationSchemes
     *            The list of supported authentication schemes.
     */
    public void setAuthenticationSchemes(List<AuthenticationScheme> authenticationSchemes) {
        this.authenticationSchemes = authenticationSchemes;
    }

    /**
     * Set bulk configuration options.
     *
     * @param bulk
     *            Bulk configuration options
     */
    public void setBulk(Bulk bulk) {
        this.bulk = bulk;
    }

    /**
     * Set configuration options related to changing a password.
     *
     * @param changePassword
     *            Configuration options related to changing a password.
     */
    public void setChangePassword(ChangePassword changePassword) {
        this.changePassword = changePassword;
    }

    /**
     * Set an HTTP-addressable URL pointing to the service provider's
     * human-consumable help documentation.
     *
     * @param documentationUri
     *            An HTTP-addressable URL pointing to the service provider's
     *            human-consumable help documentation.
     */
    public void setDocumentationUri(String documentationUri) {
        this.documentationUri = documentationUri;
    }

    /**
     * Set ETag configuration options.
     *
     * @param etag
     *            ETag configuration options.
     */
    public void setEtag(ETag etag) {
        this.etag = etag;
    }

    /**
     * Set filter options
     *
     * @param filter
     *            Filter options
     */
    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    /**
     * Set PATCH configuration options.
     *
     * @param patch
     *            PATCH configuration options.
     */
    public void setPatch(Patch patch) {
        this.patch = patch;
    }

    /**
     * Set sort configuration options.
     *
     * @param sort
     *            Sort configuration options.
     */
    public void setSort(Sort sort) {
        this.sort = sort;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ServiceProviderConfigImpl [");
        if (authenticationSchemes != null) {
            builder.append("authenticationSchemes=");
            builder.append(authenticationSchemes);
            builder.append(", ");
        }
        if (bulk != null) {
            builder.append("bulk=");
            builder.append(bulk);
            builder.append(", ");
        }
        if (changePassword != null) {
            builder.append("changePassword=");
            builder.append(changePassword);
            builder.append(", ");
        }
        if (documentationUri != null) {
            builder.append("documentationUri=");
            builder.append(documentationUri);
            builder.append(", ");
        }
        if (etag != null) {
            builder.append("etag=");
            builder.append(etag);
            builder.append(", ");
        }
        if (filter != null) {
            builder.append("filter=");
            builder.append(filter);
            builder.append(", ");
        }
        if (patch != null) {
            builder.append("patch=");
            builder.append(patch);
            builder.append(", ");
        }
        if (sort != null) {
            builder.append("sort=");
            builder.append(sort);
        }
        builder.append("]");
        return builder.toString();
    }
}
