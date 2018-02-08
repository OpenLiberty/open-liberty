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
 * This interface represents the Contact information for the exposed API.
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.md#contactObject"
 */
public interface Contact extends Constructible, Extensible {

    /**
     * Returns the identifying name of this Contact instance.
     *
     * @return the name of the contact person/organization
     **/
    String getName();

    /**
     * Sets the identifying name of this Contact instance.
     *
     * @param name the name of the contact person/organization
     */
    void setName(String name);

    /**
     * Sets this Contact instance's identifying name to the given name and returns this instance of Contact.
     *
     * @param name the name of the contact person/organization
     * @return this Contact instance
     */
    Contact name(String name);

    /**
     * Returns the URL pointing to the contact information for this Contact instance.
     *
     * @return the URL pointing to the contact information
     **/

    String getUrl();

    /**
     * Sets this Contact instance's URL pointing to the contact information. The URL must be in the format of a URL.
     *
     * @param url the URL pointing to the contact information
     */
    void setUrl(String url);

    /**
     * Sets this Contact instance's URL pointing to the contact information and returns this instance of Contact. The URL must be in the format of a
     * URL.
     *
     * @param url the url pointing to the contact information
     * @return this Contact instance
     */
    Contact url(String url);

    /**
     * Returns the contact email of this Contact instance.
     *
     * @return the email of the contact person/organization
     **/

    String getEmail();

    /**
     * Sets the contact email of this instance of Contact.
     *
     * @param email the email of the contact person/organization
     */
    void setEmail(String email);

    /**
     * Sets this Contact instance's contact email to the given email and returns this instance of Contact
     *
     * @param email the email of the contact person/organization
     * @return this Contact instance
     */
    Contact email(String email);

}