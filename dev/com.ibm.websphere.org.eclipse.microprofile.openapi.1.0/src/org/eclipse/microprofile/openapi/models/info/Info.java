/**
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 * Copyright 2017 SmartBear Software
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.microprofile.openapi.models.info;

import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.Extensible;

/**
 * This interface represents all the metadata about the API. The metadata may be used by clients if needed, and may be presented in editing or
 * documentation tools.
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.md#infoObject"
 */
public interface Info extends Constructible, Extensible {

    /**
     * Returns the title for the application for this instance of Info
     *
     * @return the title of the application
     **/

    String getTitle();

    /**
     * Sets this Info instance's title for the application to the given title.
     *
     * @param title the title of the application
     */
    void setTitle(String title);

    /**
     * Sets this Info instance's title for the application to the given title and returns this instance of Info
     *
     * @param title the title of the application
     * @return this Info instance
     */
    Info title(String title);

    /**
     * Returns a short description for the application for this Info instance.
     *
     * @return a short description of the application
     **/

    String getDescription();

    /**
     * Sets this Info instance's description for the application to the given description.
     *
     * @param description a short description for the application
     */
    void setDescription(String description);

    /**
     * Sets this Info instance's description for the application to the given description and returns this instance of Info.
     *
     * @param description a short description for the application
     * @return this Info instance
     */
    Info description(String description);

    /**
     * Returns the URL to the Terms of Service for the API for this instance of Info.
     *
     * @return a URL to the Terms of Service for the API
     **/

    String getTermsOfService();

    /**
     * Sets this Info instance's URL to the Terms of Service for the API to the given String. The URL must be in the format of a URL.
     *
     * @param termsOfService the URL to the Terms of Service for the API
     */
    void setTermsOfService(String termsOfService);

    /**
     * Sets this Info instance's URL to the Terms of Service for the API to the given String and returns this instance of Info. The URL must be in the
     * format of a URL.
     *
     * @param termsOfService the URL to the Terms of Service for the API
     * @return this Info instance
     */
    Info termsOfService(String termsOfService);

    /**
     * Returns the contact information for the exposed API from this Info instance.
     *
     * @return the contact information for the exposed API
     **/

    Contact getContact();

    /**
     * Sets this Info instance's contact information for the exposed API.
     *
     * @param contact the contact information for the exposed API
     */
    void setContact(Contact contact);

    /**
     * Sets this Info instance's contact information for the exposed API and returns this instance of Info.
     *
     * @param contact the contact information for the exposed API
     * @return this Info instance
     */
    Info contact(Contact contact);

    /**
     * Returns the license information for the exposed API from this Info instance.
     *
     * @return the license information for the exposed API
     **/

    License getLicense();

    /**
     * Sets this Info's license information for the exposed API.
     *
     * @param license the license information for the exposed API
     */
    void setLicense(License license);

    /**
     * Sets this Info's license information for the exposed API and returns this instance of Info.
     *
     * @param license the license information for the exposed API
     * @return this Info instance
     */
    Info license(License license);

    /**
     * Returns the version of the OpenAPI document for this Info instance.
     *
     * @return the version of the OpenAPI document
     **/

    String getVersion();

    /**
     * Sets the version of the OpenAPI document for this instance of Info to the given version.
     *
     * @param version the version of the OpenAPI document
     */
    void setVersion(String version);

    /**
     * Sets the version of the OpenAPI document for this instance of Info to the given version and returns this instance of Info
     *
     * @param version the version of the OpenAPI document
     * @return this Info instance
     */
    Info version(String version);

}