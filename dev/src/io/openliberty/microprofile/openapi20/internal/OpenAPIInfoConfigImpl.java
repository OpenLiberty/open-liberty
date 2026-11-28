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

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
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
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.microprofile.openapi20.internal.merge.ModelEquality;
import io.openliberty.microprofile.openapi20.internal.services.OpenAPIInfoConfig;
import io.openliberty.microprofile.openapi20.internal.services.OpenAPIModelOperations;
import io.openliberty.microprofile.openapi20.internal.utils.Constants;
import io.openliberty.microprofile.openapi20.internal.utils.MessageConstants;
import io.smallrye.openapi.runtime.OpenApiRuntimeException;

public class OpenAPIInfoConfigImpl implements OpenAPIInfoConfig {

    private static final TraceComponent tc = Tr.register(OpenAPIInfoConfigImpl.class);

    private enum WarningMode {
        LOG_WARNINGS,
        SUPPRESS_WARNINGS
    }

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
    private Optional<Info> lastMpConfigInfo;
    private Optional<String> lastMpConfigString;

    @Reference
    protected ConfigurationAdmin configAdmin;

    @Reference
    protected OpenAPIModelOperations modelOps;

    @Activate
    @Modified
    protected void activate(Map<?, ?> properties) {
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

        Optional<Info> newInfo = parseInfoProperties(infoProperties);
        if (!equals(info, newInfo)) {
            info = newInfo;
            // Only check if MP Config is being ignored if info in server.xml has actually changed
            info.ifPresent(info -> warnIfMpConfigIgnored(info, ConfigProvider.getConfig()));
        }
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

    private static boolean equals(Optional<Info> a, Optional<Info> b) {
        // Check if either is null
        if (a == null) {
            return b == null;
        } else {
            if (b == null) {
                return false;
            }
        }
    
        // Check if either is not present
        if (!a.isPresent()) {
            return !b.isPresent();
        } else {
            if (!b.isPresent()) {
                return false;
            }
        }
    
        // Both are present, test contents
        return ModelEquality.equals(a.get(), b.get());
    }

    /**
     * Read the {@code Info} object configured using MP Config.
     * <p>
     * Logs a warning if the MP Config property is set, but doesn't parse as a valid Info object.
     *
     * @param mpConfig the Config to read from
     * @return the {@code Info} parsed from config, or an empty Optional if a valid Info object is not configured
     */
    private Optional<Info> readMpConfig(Config mpConfig) {
        Optional<String> mpConfigString = mpConfig.getOptionalValue(Constants.MERGE_INFO_CONFIG, String.class);
        synchronized (this) {
            if (!mpConfigString.equals(lastMpConfigString)) {
                // Only re-read if string in MP Config has changed
                // to ensure we only warn on an invalid config string once
                lastMpConfigString = mpConfigString;
                lastMpConfigInfo = mpConfigString.flatMap(string -> readJsonString(string, WarningMode.LOG_WARNINGS));
            }
            return lastMpConfigInfo;
        }
    }

    /**
     * Parse a JSON string into an {@code Info} object.
     * <p>
     * Can optionally raise warnings if the string is not valid JSON or does not represent a valid {@code Info} object.
     *
     * @param infoJson the JSON string to parse
     * @param warningMode whether to log warnings
     * @return the parsed {@code Info} object, or an empty {@code Optional} if the string could not be parsed into a valid {@code Info} object
     */
    @FFDCIgnore(OpenApiRuntimeException.class)
    private Optional<Info> readJsonString(String infoJson, WarningMode warningMode) {
        try {
            Info info = modelOps.parseInfo(infoJson);
            if (info.getTitle() != null && info.getVersion() != null) {
                return Optional.of(info);
            } else {
                if (warningMode == WarningMode.LOG_WARNINGS) {
                    Tr.warning(tc, MessageConstants.OPENAPI_MERGE_INFO_INVALID_CWWKO1664W, Constants.MERGE_INFO_CONFIG, infoJson);
                }
                return Optional.empty();
            }
        } catch (OpenApiRuntimeException ex) {
            if (warningMode == WarningMode.LOG_WARNINGS) {
                Tr.warning(tc, MessageConstants.OPENAPI_MERGE_INFO_PARSE_ERROR_CWWKO1665W, Constants.MERGE_INFO_CONFIG, infoJson, ex.toString());
            }
            return Optional.empty();
        }
    }

    /**
     * Log a warning if equivalent or conflicting config is configured in MP Config
     *
     * @param infoFromServerXml the info read from server.xml
     * @param mpConfig MicroProfile Config
     */
    private void warnIfMpConfigIgnored(Info infoFromServerXml, Config mpConfig) {
        mpConfig.getOptionalValue(Constants.MERGE_INFO_CONFIG, String.class).ifPresent(json -> {
            // Suppress any parsing warnings here because we know we're going to ignore the value from MP Config,
            // we're only interested in whether the MP Config value is equivalent to the server.xml value
            Optional<Info> mpConfigInfo = readJsonString(json, WarningMode.SUPPRESS_WARNINGS);
            if (mpConfigInfo.isPresent() && ModelEquality.equals(infoFromServerXml, mpConfigInfo.get())) {
                // MP config was set and parsed as an identical Info object
                Tr.info(tc, MessageConstants.OPENAPI_MP_CONFIG_REDUNDANT_CWWKO1685I, Constants.MERGE_INFO_CONFIG);
            } else {
                // MP Config was set and either didn't parse, or parsed as a different Info object
                Tr.warning(tc, MessageConstants.OPENAPI_MP_CONFIG_CONFLICTS_CWWKO1686W, Constants.MERGE_INFO_CONFIG);
            }
        });
    }

    @Override
    public Optional<Info> getInfo() {
        Optional<Info> serverXmlInfo = info;
        if (serverXmlInfo.isPresent()) {
            return serverXmlInfo;
        } else {
            return readMpConfig(ConfigProvider.getConfig());
        }
    }

}
