/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.scim20.model.users;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;

import org.junit.Test;

import com.ibm.websphere.security.wim.scim20.model.users.Address;
import com.ibm.websphere.security.wim.scim20.model.users.Email;
import com.ibm.websphere.security.wim.scim20.model.users.Entitlement;
import com.ibm.websphere.security.wim.scim20.model.users.IM;
import com.ibm.websphere.security.wim.scim20.model.users.PhoneNumber;
import com.ibm.websphere.security.wim.scim20.model.users.Photo;
import com.ibm.websphere.security.wim.scim20.model.users.Role;
import com.ibm.websphere.security.wim.scim20.model.users.User;
import com.ibm.websphere.security.wim.scim20.model.users.UserGroup;
import com.ibm.websphere.security.wim.scim20.model.users.X509Certificate;
import com.ibm.ws.security.wim.scim20.SCIMUtil;
import com.ibm.ws.security.wim.scim20.model.MetaImplTest;
import com.ibm.ws.security.wim.scim20.model.extensions.EnterpriseUserImpl;
import com.ibm.ws.security.wim.scim20.model.extensions.EnterpriseUserImplTest;
import com.ibm.ws.security.wim.scim20.model.extensions.WIMUserImpl;
import com.ibm.ws.security.wim.scim20.model.extensions.WIMUserImplTest;

public class UserImplTest {
    @Test
    public void serialize() throws Exception {

        UserImpl user = getTestInstance();

        /*
         * The expected serialized JSON string.
         */
        StringBuffer expected = new StringBuffer();
        expected.append("{");
        expected.append("\"schemas\":[\"" + UserImpl.SCHEMA_URN + "\",\"" + EnterpriseUserImpl.SCHEMA_URN + "\",\""
                        + WIMUserImpl.SCHEMA_URN + "\"],");
        expected.append("\"id\":\"id\",");
        expected.append("\"externalId\":\"externalId\",");
        expected.append("\"meta\":{");
        expected.append("\"resourceType\":\"resourceType\",");
        expected.append("\"created\":0,");
        expected.append("\"lastModified\":0,");
        expected.append("\"location\":\"location\",");
        expected.append("\"version\":\"version\"");
        expected.append("},");
        expected.append("\"userName\":\"userName\",");
        expected.append("\"name\":{");
        expected.append("\"formatted\":\"formatted\",");
        expected.append("\"familyName\":\"familyName\",");
        expected.append("\"givenName\":\"givenName\",");
        expected.append("\"middleName\":\"middleName\",");
        expected.append("\"honorificPrefix\":\"honorificPrefix\",");
        expected.append("\"honorificSuffix\":\"honorificSuffix\"");
        expected.append("},");
        expected.append("\"displayName\":\"displayName\",");
        expected.append("\"nickName\":\"nickName\",");
        expected.append("\"profileUrl\":\"profileUrl\",");
        expected.append(
                        "\"emails\":[{\"value\":\"value\",\"display\":\"display\",\"type\":\"type\",\"primary\":false}],");
        expected.append("\"addresses\":[{");
        expected.append("\"formatted\":\"formatted\",");
        expected.append("\"streetAddress\":\"streetAddress\",");
        expected.append("\"locality\":\"locality\",");
        expected.append("\"region\":\"region\",");
        expected.append("\"postalCode\":\"postalCode\",");
        expected.append("\"country\":\"country\",");
        expected.append("\"type\":\"type\"");
        expected.append("}],");
        expected.append(
                        "\"phoneNumbers\":[{\"value\":\"value\",\"display\":\"display\",\"type\":\"type\",\"primary\":false}],");
        expected.append("\"ims\":[{\"value\":\"value\",\"display\":\"display\",\"type\":\"type\",\"primary\":false}],");
        expected.append("\"photos\":[{");
        expected.append("\"value\":\"value\",");
        expected.append("\"display\":\"display\",");
        expected.append("\"type\":\"type\",");
        expected.append("\"primary\":true");
        expected.append("}],");
        expected.append("\"userType\":\"userType\",");
        expected.append("\"title\":\"title\",");
        expected.append("\"preferredLanguage\":\"preferredLanguage\",");
        expected.append("\"locale\":\"locale\",");
        expected.append("\"timezone\":\"timezone\",");
        expected.append("\"active\":true,");
        expected.append("\"password\":\"password\",");
        expected.append(
                        "\"groups\":[{\"value\":\"value\",\"$ref\":\"$ref\",\"display\":\"display\",\"type\":\"type\"}],");
        expected.append(
                        "\"entitlements\":[{\"value\":\"value\",\"display\":\"display\",\"type\":\"type\",\"primary\":false}],");
        expected.append(
                        "\"roles\":[{\"value\":\"value\",\"display\":\"display\",\"type\":\"type\",\"primary\":false}],");
        expected.append(
                        "\"x509Certificates\":[{\"value\":\"AAECAw==\",\"display\":\"display\",\"type\":\"type\",\"primary\":false}],");
        expected.append("\"" + EnterpriseUserImpl.SCHEMA_URN + "\":{");
        expected.append("\"employeeNumber\":\"employeeNumber\",");
        expected.append("\"costCenter\":\"costCenter\",");
        expected.append("\"organization\":\"organization\",");
        expected.append("\"division\":\"division\",");
        expected.append("\"department\":\"department\",");
        expected.append("\"manager\":{");
        expected.append("\"value\":\"value\",");
        expected.append("\"$ref\":\"$ref\",");
        expected.append("\"displayName\":\"displayName\"");
        expected.append("}");
        expected.append("},");
        expected.append("\"" + WIMUserImpl.SCHEMA_URN + "\":{");
        expected.append("\"identifier\":{");
        expected.append("\"uniqueId\":\"uniqueId\",");
        expected.append("\"uniqueName\":\"uniqueName\",");
        expected.append("\"externalId\":\"externalId\",");
        expected.append("\"externalName\":\"externalName\",");
        expected.append("\"repositoryId\":\"repositoryId\"");
        expected.append("},");
        expected.append("\"uid\":\"uid\",");
        expected.append("\"myProperty1\":\"myValue1\",");
        expected.append("\"myProperty2\":\"myValue2\"");
        expected.append("}");
        expected.append("}");

        /*
         * Serialize.
         */
        String serialized = SCIMUtil.serialize(user);
        assertEquals(expected.toString(), serialized);

        /*
         * Deserialize.
         */
        User deserialized = SCIMUtil.deserialize(serialized, User.class);
        assertEquals(user, deserialized);
    }

    @Test
    public void serialize_obfuscate_password() throws Exception {

        UserImpl user = getTestInstance();

        /*
         * Serialize.
         */
        String serialized = SCIMUtil.serializeForTrace(user);
        assertFalse(serialized.contains("password"));
    }

    public static UserImpl getTestInstance() {

        UserImpl user = new UserImpl();
        user.setActive(true);
        user.setAddresses(Arrays.asList(new Address[] { AddressImplTest.getTestInstance() }));
        user.setDisplayName("displayName");
        user.setEmails(Arrays.asList(new Email[] { EmailImplTest.getTestInstance() }));
        user.setEnterpriseUser(EnterpriseUserImplTest.getTestInstance());
        user.setEntitlements(Arrays.asList(new Entitlement[] { EntitlementImplTest.getTestInstance() }));
        user.setExternalId("externalId");
        user.setGroups(Arrays.asList(new UserGroup[] { UserGroupImplTest.getTestInstance() }));
        user.setId("id");
        user.setIms(Arrays.asList(new IM[] { IMImplTest.getTestInstance() }));
        user.setLocale("locale");
        user.setMeta(MetaImplTest.getTestInstance());
        user.setName(NameImplTest.getTestInstance());
        user.setNickName("nickName");
        user.setPassword("password");
        user.setPhoneNumbers(Arrays.asList(new PhoneNumber[] { PhoneNumberImplTest.getTestInstance() }));
        user.setPhotos(Arrays.asList(new Photo[] { PhotoImplTest.getTestInstance() }));
        user.setPreferredLanguage("preferredLanguage");
        user.setProfileUrl("profileUrl");
        user.setRoles(Arrays.asList(new Role[] { RoleImplTest.getTestInstance() }));
        user.setTimezone("timezone");
        user.setTitle("title");
        user.setUserName("userName");
        user.setUserType("userType");
        user.setWIMUser(WIMUserImplTest.getTestInstance());
        user.setX509Certificates(Arrays.asList(new X509Certificate[] { X509CertificateImplTest.getTestInstance() }));

        return user;
    }
}
