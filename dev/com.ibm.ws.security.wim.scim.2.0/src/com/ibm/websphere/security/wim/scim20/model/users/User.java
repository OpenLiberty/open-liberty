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

package com.ibm.websphere.security.wim.scim20.model.users;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ibm.websphere.security.wim.scim20.model.Resource;
import com.ibm.websphere.security.wim.scim20.model.extensions.EnterpriseUser;
import com.ibm.websphere.security.wim.scim20.model.extensions.WIMUser;
import com.ibm.ws.security.wim.scim20.model.users.UserImpl;

@JsonDeserialize(as = UserImpl.class)
public interface User extends Resource {

    /**
     * Get a Boolean value indicating the user's administrative status.
     *
     * @return A Boolean value indicating the user's administrative status.
     */
    public Boolean getActive();

    /**
     * Get the list of physical mailing addresses for this user.
     *
     * @return the list of physical mailing addresses for this user.
     */
    public List<Address> getAddresses();

    /**
     * Get the name of the user, suitable for display to end-users. The name
     * should be the full name of the user being described, if known.
     *
     * @return The name of the user, suitable for display to end-users.
     */
    public String getDisplayName();

    /**
     * Get the list of email addresses for the user.
     *
     * @return The list of email addresses for the user.
     */
    public List<Email> getEmails();

    /**
     * Get the collection of user properties defined in the enterprise user
     * extension schema.
     *
     * @return The collection of user properties defined in the enterprise user
     *         extension schema.
     */
    public EnterpriseUser getEnterpriseUser();

    /**
     * Get the list of entitlements for the user that represent a thing the user
     * has.
     *
     * @return The list of entitlements for the user.
     */
    public List<Entitlement> getEntitlements();

    /**
     * Get the list of groups to which the user belongs, either through direct
     * membership, through nested groups, or dynamically calculated.
     *
     * @return The list of groups to which the user belongs.
     */
    public List<UserGroup> getGroups();

    /**
     * Get the instant messaging addresses for the user.
     *
     * @return The instant messaging addresses for the user.
     */
    public List<IM> getIms();

    /**
     * Get the user's default location for purposes of localizing items such as
     * currency, date time format, or numerical representations.
     *
     * @return The user's default location.
     */
    public String getLocale();

    /**
     * Get the components of the user's real name.
     *
     * @return The components of the user's real name.
     */
    public Name getName();

    /**
     * Get the casual way to address the user in real life, e.g., 'Bob' or
     * 'Bobby' instead of 'Robert'. This attribute SHOULD NOT be used to
     * represent a user's username (e.g., 'bjensen' or 'mpepperidge').
     *
     * @return The casual way to address the user in real life.
     */
    public String getNickName();

    /**
     * Get the list of phone numbers for the user.
     *
     * @return The list of phone numbers for the user.
     */
    public List<PhoneNumber> getPhoneNumbers();

    /**
     * Get the list of URLs of photos of the user.
     *
     * @return The list of URLs of photos of the user.
     */
    public List<Photo> getPhotos();

    /**
     * Get the user's preferred written or spoken language. Generally used for
     * selecting a localized user interface; e.g., 'en_US' specifies the
     * language English and country US.
     *
     * @return The user's preferred written or spoken language.
     */
    public String getPreferredLanguage();

    /**
     * Get the fully qualified URL pointing to a page representing the user's
     * online profile.
     *
     * @return The fully qualified URL pointing to a page representing the
     *         user's online profile.
     */
    public String getProfileUrl();

    /**
     * Get the list of roles for the user that collectively represent who the
     * user is, e.g., 'Student', 'Faculty'.
     *
     * @return The list of roles for the user.
     */
    public List<Role> getRoles();

    /**
     * Get the User's time zone in the 'Olson' time zone database format, e.g.,
     * 'America/Los_Angeles'.
     *
     * @return The user's time zone in the 'Olson' time zone database format.
     */
    public String getTimezone();

    /**
     * Get the user's title, such as "Vice President."
     *
     * @return The user's title.
     */
    public String getTitle();

    /**
     * Get the unique identifier for the user, typically used by the user to
     * directly authenticate to the service provider. Each user MUST include a
     * non-empty userName value. This identifier MUST be unique across the
     * service provider's entire set of users.
     *
     * @return The unique identifier for the user.
     */
    public String getUserName();

    /**
     * Get a value used to identify the relationship between the organization
     * and the user. Typical values used might be 'Contractor', 'Employee',
     * 'Intern', 'Temp', 'External', and 'Unknown', but any value may be used.
     *
     * @return A value used to identify the relationship between the
     *         organization and the user
     */
    public String getUserType();

    /**
     * Get the collection of user properties defined in the WIM user extension
     * schema.
     *
     * @return The collection of user properties defined in the WIM user
     *         extension schema.
     */
    public WIMUser getWIMUser();

    /**
     * Set the collection of user properties defined in the WIM user extension
     * schema.
     *
     * @param wimUser
     *            The collection of user properties defined in the WIM user
     *            extension schema.
     */
    public void setWIMUser(WIMUser wimUser);

    /**
     * Get a list of certificates issued to the user.
     *
     * @return A list of certificates issued to the user.
     */
    public List<X509Certificate> getX509Certificates();

    /**
     * Set a Boolean value indicating the user's administrative status.
     *
     * @param active
     *            A Boolean value indicating the user's administrative status.
     */
    public void setActive(Boolean active);

    /**
     * Set the list of physical mailing addresses for this user.
     *
     * @return The list of physical mailing addresses for this user.
     */
    public void setAddresses(List<Address> addresses);

    /**
     * Set the name of the user, suitable for display to end-users. The name
     * should be the full name of the user being described, if known.
     *
     * @param displayName
     *            The name of the user, suitable for display to end-users.
     */
    public void setDisplayName(String displayName);

    /**
     * Set the list of email addresses for the user.
     *
     * @param emails
     *            The list of email addresses for the user.
     */
    public void setEmails(List<Email> emails);

    /**
     * Set the collection of user properties defined in the enterprise user
     * extension schema.
     *
     * @param enterpriseUser
     *            The collection of user properties defined in the enterprise
     *            user extension schema.
     */
    public void setEnterpriseUser(EnterpriseUser enterpriseUser);

    /**
     * Set the list of entitlements for the user that represent a thing the user
     * has.
     *
     * @param entitlements
     *            The list of entitlements for the user.
     */
    public void setEntitlements(List<Entitlement> entitlements);

    /**
     * Set an identifier for the resource as defined by the provisioning client.
     *
     * @param externalId
     *            An identifier for the resource as defined by the provisioning
     *            client.
     */
    public void setExternalId(String externalId);

    /**
     * Set the list of instant messaging addresses for the user.
     *
     * @param ims
     *            The list of instant messaging addresses for the user.
     */
    public void setIms(List<IM> ims);

    /**
     * Set the user's default location for purposes of localizing items such as
     * currency, date time format, or numerical representations.
     *
     * @param locale
     *            The user's default location.
     */
    public void setLocale(String locale);

    /**
     * Set the components of the user's real name.
     *
     * @param name
     *            The components of the user's real name.
     */
    public void setName(Name name);

    /**
     * Set the casual way to address the user in real life, e.g., 'Bob' or
     * 'Bobby' instead of 'Robert'. This attribute SHOULD NOT be used to
     * represent a user's username (e.g., 'bjensen' or 'mpepperidge').
     *
     * @param nickName
     *            The casual way to address the user in real life.
     */
    public void setNickName(String nickName);

    /**
     * Set the user's clear text password. This attribute is intended to be used
     * as a means to specify an initial password when creating a new user or to
     * reset an existing user's password.
     *
     * @param password
     *            The user's clear text password.
     */
    public void setPassword(String password);

    /**
     * Set the list of phone numbers for the user.
     *
     * @param phoneNumbers
     *            The list of phone numbers for the user.
     */
    public void setPhoneNumbers(List<PhoneNumber> phoneNumbers);

    /**
     * Set the list of URLs of photos of the user.
     *
     * @param photos
     *            The list of URLs of photos of the user.
     */
    public void setPhotos(List<Photo> photos);

    /**
     * Set the user's preferred written or spoken language. Generally used for
     * selecting a localized user interface; e.g., 'en_US' specifies the
     * language English and country US.
     *
     * @param preferredLanguage
     *            The user's preferred written or spoken language.
     */
    public void setPreferredLanguage(String preferredLanguage);

    /**
     * Set the fully qualified URL pointing to a page representing the user's
     * online profile.
     *
     * @param profileUrl
     *            The fully qualified URL pointing to a page representing the
     *            user's online profile.
     */
    public void setProfileUrl(String profileUrl);

    /**
     * Set the list of roles for the user that collectively represent who the
     * user is, e.g., 'Student', 'Faculty'.
     *
     * @param roles
     *            The list of roles for the user.
     */
    public void setRoles(List<Role> roles);

    /**
     * Set the user's time zone in the 'Olson' time zone database format, e.g.,
     * 'America/Los_Angeles'.
     *
     * @param timezone
     *            The user's time zone in the 'Olson' time zone database format.
     */
    public void setTimezone(String timezone);

    /**
     * Set the user's title, such as "Vice President."
     *
     * @param title
     *            The user's title.
     */
    public void setTitle(String title);

    /**
     * Set the unique identifier for the user, typically used by the user to
     * directly authenticate to the service provider. Each user MUST include a
     * non-empty userName value. This identifier MUST be unique across the
     * service provider's entire set of users.
     *
     * @param userName
     *            The unique identifier for the user.
     */
    public void setUserName(String userName);

    /**
     * Set a value used to identify the relationship between the organization
     * and the user. Typical values used might be 'Contractor', 'Employee',
     * 'Intern', 'Temp', 'External', and 'Unknown', but any value may be used.
     *
     * @param userType
     *            A value used to identify the relationship between the
     *            organization and the user
     */
    public void setUserType(String userType);

    /**
     * Set a list of certificates issued to the user.
     *
     * @param x509Certificates
     *            A list of certificates issued to the user.
     */
    public void setX509Certificates(List<X509Certificate> x509Certificates);

}
