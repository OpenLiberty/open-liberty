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

package com.ibm.ws.security.wim.scim20.model.users;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.websphere.security.wim.scim20.model.users.Address;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
@JsonPropertyOrder(value = { "formatted", "streetAddress", "locality", "region", "postalCode", "country", "type" })
public class AddressImpl implements Address {

    @JsonProperty("country")
    private String country;

    @JsonProperty("formatted")
    private String formatted;

    @JsonProperty("locality")
    private String locality;

    @JsonProperty("postalCode")
    private String postalCode;

    @JsonProperty("region")
    private String region;

    @JsonProperty("streetAddress")
    private String streetAddress;

    @JsonProperty("type")
    private String type;

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
        AddressImpl other = (AddressImpl) obj;
        if (country == null) {
            if (other.country != null) {
                return false;
            }
        } else if (!country.equals(other.country)) {
            return false;
        }
        if (formatted == null) {
            if (other.formatted != null) {
                return false;
            }
        } else if (!formatted.equals(other.formatted)) {
            return false;
        }
        if (locality == null) {
            if (other.locality != null) {
                return false;
            }
        } else if (!locality.equals(other.locality)) {
            return false;
        }
        if (postalCode == null) {
            if (other.postalCode != null) {
                return false;
            }
        } else if (!postalCode.equals(other.postalCode)) {
            return false;
        }
        if (region == null) {
            if (other.region != null) {
                return false;
            }
        } else if (!region.equals(other.region)) {
            return false;
        }
        if (streetAddress == null) {
            if (other.streetAddress != null) {
                return false;
            }
        } else if (!streetAddress.equals(other.streetAddress)) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        return true;
    }

    @Override
    public String getCountry() {
        return this.country;
    }

    @Override
    public String getFormatted() {
        return this.formatted;
    }

    @Override
    public String getLocality() {
        return this.locality;
    }

    @Override
    public String getPostalCode() {
        return this.postalCode;
    }

    @Override
    public String getRegion() {
        return this.region;
    }

    @Override
    public String getStreetAddress() {
        return this.streetAddress;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((country == null) ? 0 : country.hashCode());
        result = prime * result + ((formatted == null) ? 0 : formatted.hashCode());
        result = prime * result + ((locality == null) ? 0 : locality.hashCode());
        result = prime * result + ((postalCode == null) ? 0 : postalCode.hashCode());
        result = prime * result + ((region == null) ? 0 : region.hashCode());
        result = prime * result + ((streetAddress == null) ? 0 : streetAddress.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public void setCountry(String country) {
        this.country = country;
    }

    @Override
    public void setFormatted(String formatted) {
        this.formatted = formatted;
    }

    @Override
    public void setLocality(String locality) {
        this.locality = locality;
    }

    @Override
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    @Override
    public void setRegion(String region) {
        this.region = region;
    }

    @Override
    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    @Override
    public void setType(String type) {
        // TODO Restrict to canonical values? See RFC 7643 section 7.
        this.type = type;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AddressImpl [");
        if (country != null) {
            builder.append("country=");
            builder.append(country);
            builder.append(", ");
        }
        if (formatted != null) {
            builder.append("formatted=");
            builder.append(formatted);
            builder.append(", ");
        }
        if (locality != null) {
            builder.append("locality=");
            builder.append(locality);
            builder.append(", ");
        }
        if (postalCode != null) {
            builder.append("postalCode=");
            builder.append(postalCode);
            builder.append(", ");
        }
        if (region != null) {
            builder.append("region=");
            builder.append(region);
            builder.append(", ");
        }
        if (streetAddress != null) {
            builder.append("streetAddress=");
            builder.append(streetAddress);
            builder.append(", ");
        }
        if (type != null) {
            builder.append("type=");
            builder.append(type);
        }
        builder.append("]");
        return builder.toString();
    }
}
