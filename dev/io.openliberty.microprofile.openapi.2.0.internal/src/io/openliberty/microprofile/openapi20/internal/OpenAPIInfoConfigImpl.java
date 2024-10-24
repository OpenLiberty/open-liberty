/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.internal;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.info.Contact;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.info.License;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.productinfo.ProductInfo;

import io.openliberty.microprofile.openapi20.internal.services.OpenAPIInfoConfig;
import io.openliberty.microprofile.openapi20.internal.utils.MessageConstants;

public class OpenAPIInfoConfigImpl implements OpenAPIInfoConfig {

    private static final TraceComponent tc = Tr.register(OpenAPIInfoConfigImpl.class);

    protected static final String INFO_KEY = "info";
    protected static final String TITLE_KEY = "title";
    protected static final String DESCRIPTION_KEY = "description";
    protected static final String TERMS_OF_SERVICE_KEY = "termsOfService";
    protected static final String CONTACT_NAME_KEY = "contactName";
    protected static final String CONTACT_URL_KEY = "contactUrl";
    protected static final String CONTACT_EMAIL_KEY = "contactEmail";
    protected static final String LICENSE_NAME_KEY = "licenseName";
    protected static final String LICENSE_URL_KEY = "licenseUrl";
    protected static final String LICENSE_IDENTIFIER_KEY = "licenseIdentifier";
    protected static final String SUMMARY_KEY = "summary";
    protected static final String VERSION_KEY = "version";

    private volatile Optional<Info> info;

    @Reference
    protected ConfigurationAdmin configAdmin;

    @Activate
    @Modified
    protected void activate(Map<?, ?> properties) {
        if (!ProductInfo.getBetaEdition()) {
            info = Optional.empty();
            return;
        }

        // Retrieve the info sub-element
        // Following example from https://www.ibm.com/docs/en/was-liberty/nd?topic=service-nesting-configuration-elements
        String infoPid = (String) properties.get(INFO_KEY);
        if (infoPid == null) {
            info = Optional.empty();
            return;
        }

        Configuration infoConfig;
        try {
            infoConfig = configAdmin.getConfiguration(infoPid, null);
        } catch (IOException e) {
            // Error accessing persistent config storage
            // Shouldn't happen in liberty but we'll get an FFDC if it does
            info = Optional.empty();
            return;
        }

        Dictionary<String, Object> infoProperties = infoConfig.getProperties();
        if (infoProperties == null) {
            // Javadoc indicates this is possible during startup
            // We don't expect this case, but check it to ensure we don't NPE
            info = Optional.empty();
            return;
        }

        info = parseInfoProperties(infoProperties);
    }

    /**
     * Parse a dictionary of properties into an {@code Info} object.
     *
     * @param infoProperties a dictionary of properties. This is a {@code Dictionary} because that's what Configuration Admin provides us.
     * @return a populated {@code Info} object, or an empty {@code Optional} if any of the required properties are not set
     */
    protected Optional<Info> parseInfoProperties(Dictionary<String, Object> infoProperties) {
        String title = (String) infoProperties.get(TITLE_KEY);
        String version = (String) infoProperties.get(VERSION_KEY);

        if (title == null || title.trim().isEmpty() || version == null || version.trim().isEmpty()) {
            // Title and version are both required
            Tr.warning(tc, MessageConstants.OPENAPI_INFO_INVALID_SERVERXML_CWWKO1683W);
            return Optional.empty();
        }

        Info info = OASFactory.createInfo();
        info.setTitle(title);
        info.setVersion(version);
        info.setDescription((String) infoProperties.get(DESCRIPTION_KEY));
        info.setTermsOfService((String) infoProperties.get(TERMS_OF_SERVICE_KEY));

        String contactName = (String) infoProperties.get(CONTACT_NAME_KEY);
        String contactUrl = (String) infoProperties.get(CONTACT_URL_KEY);
        String contactEmail = (String) infoProperties.get(CONTACT_EMAIL_KEY);
        if (contactName != null
            || contactUrl != null
            || contactEmail != null) {
            Contact contact = OASFactory.createContact();
            contact.setName(contactName);
            contact.setEmail(contactEmail);
            contact.setUrl(contactUrl);
            info.setContact(contact);
        }

        String licenseName = (String) infoProperties.get(LICENSE_NAME_KEY);
        String licenseUrl = (String) infoProperties.get(LICENSE_URL_KEY);
        if (licenseName != null || licenseUrl != null) {
            License license = OASFactory.createLicense();
            license.setName(licenseName);
            license.setUrl(licenseUrl);
            info.setLicense(license);
        }

        return Optional.of(info);
    }

    @Override
    public Optional<Info> getInfo() {
        return info;
    }

}
