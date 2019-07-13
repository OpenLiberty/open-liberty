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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import com.ibm.websphere.security.wim.scim20.model.Meta;
import com.ibm.websphere.security.wim.scim20.model.extensions.EnterpriseUser;
import com.ibm.websphere.security.wim.scim20.model.extensions.WIMUser;
import com.ibm.websphere.security.wim.scim20.model.users.Address;
import com.ibm.websphere.security.wim.scim20.model.users.Email;
import com.ibm.websphere.security.wim.scim20.model.users.Entitlement;
import com.ibm.websphere.security.wim.scim20.model.users.IM;
import com.ibm.websphere.security.wim.scim20.model.users.Name;
import com.ibm.websphere.security.wim.scim20.model.users.PhoneNumber;
import com.ibm.websphere.security.wim.scim20.model.users.Photo;
import com.ibm.websphere.security.wim.scim20.model.users.Role;
import com.ibm.websphere.security.wim.scim20.model.users.User;
import com.ibm.websphere.security.wim.scim20.model.users.UserGroup;
import com.ibm.websphere.security.wim.scim20.model.users.X509Certificate;
import com.ibm.ws.security.wim.scim20.InternalView;
import com.ibm.ws.security.wim.scim20.model.extensions.EnterpriseUserImpl;
import com.ibm.ws.security.wim.scim20.model.extensions.WIMUserImpl;

//TODO Implement FFDCSelfIntrospectable to hide password in FFDC.

@JsonInclude(Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
@JsonPropertyOrder(value = { "schemas", "id", "externalId", "meta", "userName", "name", "displayName", "nickName",
                             "profileUrl", "emails", "addresses", "phoneNumbers", "ims", "photos", "userType", "title", "preferredLanguage",
                             "locale", "timezone", "active", "password", "groups", "entitlements", "roles", "x509Certificates",
                             EnterpriseUserImpl.SCHEMA_URN, WIMUserImpl.SCHEMA_URN })
public class UserImpl implements User {

    /**
     * The URN for the SCIM 2.0 core user schema.
     */
    public static final String SCHEMA_URN = "urn:ietf:params:scim:schemas:core:2.0:User";

    @JsonProperty("active")
    private Boolean active;

    @JsonProperty("addresses")
    private List<Address> addresses;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("emails")
    private List<Email> emails;

    @JsonProperty(EnterpriseUserImpl.SCHEMA_URN)
    private EnterpriseUser enterpriseUser;

    @JsonProperty("entitlements")
    private List<Entitlement> entitlements;

    @JsonProperty("externalId")
    private String externalId;

    @JsonProperty("groups")
    private List<UserGroup> groups;

    @JsonProperty("id")
    private String id;

    @JsonProperty("ims")
    private List<IM> ims;

    @JsonProperty("locale")
    private String locale;

    @JsonProperty("meta")
    private Meta meta;

    @JsonProperty("name")
    private Name name;

    @JsonProperty("nickName")
    private String nickName;

    @JsonView(InternalView.class)
    @JsonProperty("password")
    private String password;

    @JsonProperty("phoneNumbers")
    private List<PhoneNumber> phoneNumbers;

    @JsonProperty("photos")
    private List<Photo> photos;

    @JsonProperty("preferredLanguage")
    private String preferredLanguage;

    @JsonProperty("profileUrl")
    private String profileUrl;

    @JsonProperty("roles")
    private List<Role> roles;

    @JsonProperty("schemas")
    private final List<String> schemas;

    @JsonProperty("timezone")
    private String timezone;

    @JsonProperty("title")
    private String title;

    @JsonProperty("userName")
    private String userName;

    @JsonProperty("userType")
    private String userType;

    @JsonProperty(WIMUserImpl.SCHEMA_URN)
    private WIMUser wimUser;

    @JsonProperty("x509Certificates")
    private List<X509Certificate> x509Certificates;

    public UserImpl() {
        this.schemas = new ArrayList<String>();
        this.schemas.add(SCHEMA_URN);
    }

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
        UserImpl other = (UserImpl) obj;
        if (active == null) {
            if (other.active != null) {
                return false;
            }
        } else if (!active.equals(other.active)) {
            return false;
        }
        if (addresses == null) {
            if (other.addresses != null) {
                return false;
            }
        } else if (!addresses.equals(other.addresses)) {
            return false;
        }
        if (displayName == null) {
            if (other.displayName != null) {
                return false;
            }
        } else if (!displayName.equals(other.displayName)) {
            return false;
        }
        if (emails == null) {
            if (other.emails != null) {
                return false;
            }
        } else if (!emails.equals(other.emails)) {
            return false;
        }
        if (enterpriseUser == null) {
            if (other.enterpriseUser != null) {
                return false;
            }
        } else if (!enterpriseUser.equals(other.enterpriseUser)) {
            return false;
        }
        if (entitlements == null) {
            if (other.entitlements != null) {
                return false;
            }
        } else if (!entitlements.equals(other.entitlements)) {
            return false;
        }
        if (externalId == null) {
            if (other.externalId != null) {
                return false;
            }
        } else if (!externalId.equals(other.externalId)) {
            return false;
        }
        if (groups == null) {
            if (other.groups != null) {
                return false;
            }
        } else if (!groups.equals(other.groups)) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (ims == null) {
            if (other.ims != null) {
                return false;
            }
        } else if (!ims.equals(other.ims)) {
            return false;
        }
        if (locale == null) {
            if (other.locale != null) {
                return false;
            }
        } else if (!locale.equals(other.locale)) {
            return false;
        }
        if (meta == null) {
            if (other.meta != null) {
                return false;
            }
        } else if (!meta.equals(other.meta)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (nickName == null) {
            if (other.nickName != null) {
                return false;
            }
        } else if (!nickName.equals(other.nickName)) {
            return false;
        }
        if (password == null) {
            if (other.password != null) {
                return false;
            }
        } else if (!password.equals(other.password)) {
            return false;
        }
        if (phoneNumbers == null) {
            if (other.phoneNumbers != null) {
                return false;
            }
        } else if (!phoneNumbers.equals(other.phoneNumbers)) {
            return false;
        }
        if (photos == null) {
            if (other.photos != null) {
                return false;
            }
        } else if (!photos.equals(other.photos)) {
            return false;
        }
        if (preferredLanguage == null) {
            if (other.preferredLanguage != null) {
                return false;
            }
        } else if (!preferredLanguage.equals(other.preferredLanguage)) {
            return false;
        }
        if (profileUrl == null) {
            if (other.profileUrl != null) {
                return false;
            }
        } else if (!profileUrl.equals(other.profileUrl)) {
            return false;
        }
        if (roles == null) {
            if (other.roles != null) {
                return false;
            }
        } else if (!roles.equals(other.roles)) {
            return false;
        }
        if (schemas == null) {
            if (other.schemas != null) {
                return false;
            }
        } else if (!schemas.equals(other.schemas)) {
            return false;
        }
        if (timezone == null) {
            if (other.timezone != null) {
                return false;
            }
        } else if (!timezone.equals(other.timezone)) {
            return false;
        }
        if (title == null) {
            if (other.title != null) {
                return false;
            }
        } else if (!title.equals(other.title)) {
            return false;
        }
        if (userName == null) {
            if (other.userName != null) {
                return false;
            }
        } else if (!userName.equals(other.userName)) {
            return false;
        }
        if (userType == null) {
            if (other.userType != null) {
                return false;
            }
        } else if (!userType.equals(other.userType)) {
            return false;
        }
        if (wimUser == null) {
            if (other.wimUser != null) {
                return false;
            }
        } else if (!wimUser.equals(other.wimUser)) {
            return false;
        }
        if (x509Certificates == null) {
            if (other.x509Certificates != null) {
                return false;
            }
        } else if (!x509Certificates.equals(other.x509Certificates)) {
            return false;
        }
        return true;
    }

    @Override
    public Boolean getActive() {
        return this.active;
    }

    @Override
    public List<Address> getAddresses() {
        return this.addresses;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public List<Email> getEmails() {
        return this.emails;
    }

    @Override
    public EnterpriseUser getEnterpriseUser() {
        return enterpriseUser;
    }

    @Override
    public List<Entitlement> getEntitlements() {
        return this.entitlements;
    }

    @Override
    public String getExternalId() {
        return externalId;
    }

    @Override
    public List<UserGroup> getGroups() {
        return this.groups;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public List<IM> getIms() {
        return this.ims;
    }

    @Override
    public String getLocale() {
        return this.locale;
    }

    @Override
    public Meta getMeta() {
        return meta;
    }

    @Override
    public Name getName() {
        return this.name;
    }

    @Override
    public String getNickName() {
        return this.nickName;
    }

    /**
     * Get the user's clear text password.
     *
     * @return The user's clear text password.
     */
    // TODO @Trivial
    public String getPassword() {
        return this.password;
    }

    @Override
    public List<PhoneNumber> getPhoneNumbers() {
        return this.phoneNumbers;
    }

    @Override
    public List<Photo> getPhotos() {
        return this.photos;
    }

    @Override
    public String getPreferredLanguage() {
        return this.preferredLanguage;
    }

    @Override
    public String getProfileUrl() {
        return this.profileUrl;
    }

    @Override
    public List<Role> getRoles() {
        return this.roles;
    }

    @Override
    public List<String> getSchemas() {
        return schemas;
    }

    @Override
    public String getTimezone() {
        return this.timezone;
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public String getUserName() {
        return this.userName;
    }

    @Override
    public String getUserType() {
        return this.userType;
    }

    @Override
    public WIMUser getWIMUser() {
        return wimUser;
    }

    @Override
    public List<X509Certificate> getX509Certificates() {
        return this.x509Certificates;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((active == null) ? 0 : active.hashCode());
        result = prime * result + ((addresses == null) ? 0 : addresses.hashCode());
        result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
        result = prime * result + ((emails == null) ? 0 : emails.hashCode());
        result = prime * result + ((enterpriseUser == null) ? 0 : enterpriseUser.hashCode());
        result = prime * result + ((entitlements == null) ? 0 : entitlements.hashCode());
        result = prime * result + ((externalId == null) ? 0 : externalId.hashCode());
        result = prime * result + ((groups == null) ? 0 : groups.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((ims == null) ? 0 : ims.hashCode());
        result = prime * result + ((locale == null) ? 0 : locale.hashCode());
        result = prime * result + ((meta == null) ? 0 : meta.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((nickName == null) ? 0 : nickName.hashCode());
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + ((phoneNumbers == null) ? 0 : phoneNumbers.hashCode());
        result = prime * result + ((photos == null) ? 0 : photos.hashCode());
        result = prime * result + ((preferredLanguage == null) ? 0 : preferredLanguage.hashCode());
        result = prime * result + ((profileUrl == null) ? 0 : profileUrl.hashCode());
        result = prime * result + ((roles == null) ? 0 : roles.hashCode());
        result = prime * result + ((schemas == null) ? 0 : schemas.hashCode());
        result = prime * result + ((timezone == null) ? 0 : timezone.hashCode());
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        result = prime * result + ((userName == null) ? 0 : userName.hashCode());
        result = prime * result + ((userType == null) ? 0 : userType.hashCode());
        result = prime * result + ((wimUser == null) ? 0 : wimUser.hashCode());
        result = prime * result + ((x509Certificates == null) ? 0 : x509Certificates.hashCode());
        return result;
    }

    @Override
    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public void setEmails(List<Email> emails) {
        this.emails = emails;
    }

    @Override
    public void setEnterpriseUser(EnterpriseUser enterpriseUser) {
        this.enterpriseUser = enterpriseUser;

        /*
         * Update the schema list.
         */
        synchronized (schemas) {
            if (this.enterpriseUser == null) {
                schemas.remove(EnterpriseUserImpl.SCHEMA_URN);
            } else {
                if (!schemas.contains(EnterpriseUserImpl.SCHEMA_URN)) {
                    schemas.add(EnterpriseUserImpl.SCHEMA_URN);
                }
            }
        }
    }

    @Override
    public void setEntitlements(List<Entitlement> entitlements) {
        this.entitlements = entitlements;
    }

    @Override
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    /**
     * Set the list of groups to which the user belongs, either through direct
     * membership, through nested groups, or dynamically calculated.
     *
     * @param groups
     *            The list of groups to which the user belongs.
     */
    public void setGroups(List<UserGroup> groups) {
        this.groups = groups;
    }

    /**
     * Set a unique identifier for a SCIM resource as defined by the service
     * provider.
     *
     * @param id
     *            A unique identifier for a SCIM resource as defined by the
     *            service provider.
     */
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void setIms(List<IM> ims) {
        this.ims = ims;
    }

    @Override
    public void setLocale(String locale) {
        this.locale = locale;
    }

    /**
     * Set the resource's meta data.
     *
     * @param meta
     *            The resource's meta data.
     */
    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    @Override
    public void setName(Name name) {
        this.name = name;
    }

    @Override
    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    @Override
    // TODO @Trivial
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void setPhoneNumbers(List<PhoneNumber> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    @Override
    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
    }

    @Override
    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    @Override
    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    @Override
    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    @Override
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public void setUserType(String userType) {
        this.userType = userType;
    }

    @Override
    public void setWIMUser(WIMUser wimUser) {
        this.wimUser = wimUser;

        /*
         * Update the schema list.
         */
        synchronized (schemas) {
            if (wimUser == null) {
                schemas.remove(WIMUserImpl.SCHEMA_URN);
            } else {
                if (!schemas.contains(WIMUserImpl.SCHEMA_URN)) {
                    schemas.add(WIMUserImpl.SCHEMA_URN);
                }
            }
        }
    }

    @Override
    public void setX509Certificates(List<X509Certificate> x509Certificates) {
        this.x509Certificates = x509Certificates;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UserImpl [");
        if (active != null) {
            builder.append("active=");
            builder.append(active);
            builder.append(", ");
        }
        if (addresses != null) {
            builder.append("addresses=");
            builder.append(addresses);
            builder.append(", ");
        }
        if (displayName != null) {
            builder.append("displayName=");
            builder.append(displayName);
            builder.append(", ");
        }
        if (emails != null) {
            builder.append("emails=");
            builder.append(emails);
            builder.append(", ");
        }
        if (enterpriseUser != null) {
            builder.append("enterpriseUser=");
            builder.append(enterpriseUser);
            builder.append(", ");
        }
        if (entitlements != null) {
            builder.append("entitlements=");
            builder.append(entitlements);
            builder.append(", ");
        }
        if (externalId != null) {
            builder.append("externalId=");
            builder.append(externalId);
            builder.append(", ");
        }
        if (groups != null) {
            builder.append("groups=");
            builder.append(groups);
            builder.append(", ");
        }
        if (id != null) {
            builder.append("id=");
            builder.append(id);
            builder.append(", ");
        }
        if (ims != null) {
            builder.append("ims=");
            builder.append(ims);
            builder.append(", ");
        }
        if (locale != null) {
            builder.append("locale=");
            builder.append(locale);
            builder.append(", ");
        }
        if (meta != null) {
            builder.append("meta=");
            builder.append(meta);
            builder.append(", ");
        }
        if (name != null) {
            builder.append("name=");
            builder.append(name);
            builder.append(", ");
        }
        if (nickName != null) {
            builder.append("nickName=");
            builder.append(nickName);
            builder.append(", ");
        }
        if (password != null) {
            builder.append("password=");
            builder.append("******");
            builder.append(", ");
        }
        if (phoneNumbers != null) {
            builder.append("phoneNumbers=");
            builder.append(phoneNumbers);
            builder.append(", ");
        }
        if (photos != null) {
            builder.append("photos=");
            builder.append(photos);
            builder.append(", ");
        }
        if (preferredLanguage != null) {
            builder.append("preferredLanguage=");
            builder.append(preferredLanguage);
            builder.append(", ");
        }
        if (profileUrl != null) {
            builder.append("profileUrl=");
            builder.append(profileUrl);
            builder.append(", ");
        }
        if (roles != null) {
            builder.append("roles=");
            builder.append(roles);
            builder.append(", ");
        }
        if (schemas != null) {
            builder.append("schemas=");
            builder.append(schemas);
            builder.append(", ");
        }
        if (timezone != null) {
            builder.append("timezone=");
            builder.append(timezone);
            builder.append(", ");
        }
        if (title != null) {
            builder.append("title=");
            builder.append(title);
            builder.append(", ");
        }
        if (userName != null) {
            builder.append("userName=");
            builder.append(userName);
            builder.append(", ");
        }
        if (userType != null) {
            builder.append("userType=");
            builder.append(userType);
            builder.append(", ");
        }
        if (wimUser != null) {
            builder.append("wimUser=");
            builder.append(wimUser);
            builder.append(", ");
        }
        if (x509Certificates != null) {
            builder.append("x509Certificates=");
            builder.append(x509Certificates);
        }
        builder.append("]");
        return builder.toString();
    }
}
