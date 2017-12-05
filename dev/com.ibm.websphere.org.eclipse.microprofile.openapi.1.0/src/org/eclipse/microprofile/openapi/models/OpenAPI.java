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

package org.eclipse.microprofile.openapi.models;

import java.util.List;

import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.tags.Tag;

/**
 * OpenAPI
 * <p>
 * This is the root document object of the OpenAPI document. It contains required and optional fields.
 * 
 * @see <a href= "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#openapi-object">OpenAPI Specification OpenAPI Object</a>
 */
@SuppressWarnings("rawtypes")
public interface OpenAPI extends Constructible, Extensible {

    /**
     * Returns the openapi property from an OpenAPI instance.
     *
     * @return the semantic version number of the OpenAPI Specification version that the OpenAPI document uses
     **/
    String getOpenapi();

    /**
     * Sets this OpenAPI instance's openapi property to the given string.
     *
     * @param openapi the semantic version number of the OpenAPI Specification version that the OpenAPI document uses
     */
    void setOpenapi(String openapi);

    /**
     * Sets this OpenAPI instance's openapi property to the given string.
     *
     * @param openapi the semantic version number of the OpenAPI Specification version that the OpenAPI document uses
     * @return the current OpenAPI object
     */
    OpenAPI openapi(String openapi);

    /**
     * Returns the info property from an OpenAPI instance.
     *
     * @return metadata about the API
     **/
    Info getInfo();

    /**
     * Sets this OpenAPI instance's info property to the given object.
     *
     * @param info metadata about the API
     */
    void setInfo(Info info);

    /**
     * Sets this OpenAPI instance's info property to the given object.
     *
     * @param info metadata about the API
     * @return the current OpenAPI object
     */
    OpenAPI info(Info info);

    /**
     * Returns the externalDocs property from an OpenAPI instance.
     *
     * @return additional external documentation
     **/
    ExternalDocumentation getExternalDocs();

    /**
     * Sets this OpenAPI instance's externalDocs property to the given object.
     *
     * @param externalDocs additional external documentation.
     */
    void setExternalDocs(ExternalDocumentation externalDocs);

    /**
     * Sets this OpenAPI instance's externalDocs property to the given object.
     *
     * @param externalDocs additional external documentation
     * @return the current OpenAPI object
     */
    OpenAPI externalDocs(ExternalDocumentation externalDocs);

    /**
     * Returns the Servers defined in the API
     *
     * @return Server objects which provide connectivity information to target servers
     **/
    List<Server> getServers();

    /**
     * Sets this OpenAPI instance's servers property to the given servers.
     *
     * @param servers Server objects which provide connectivity information to target servers
     */
    void setServers(List<Server> servers);

    /**
     * Sets this OpenAPI instance's servers property to the given servers.
     *
     * @param servers Server objects which provide connectivity information to target servers
     * @return the current OpenAPI object
     */
    OpenAPI servers(List<Server> servers);

    /**
     * Adds the given server to this OpenAPI instance's list of servers.
     *
     * @param server Server object which provides connectivity information to a target server
     * @return the current OpenAPI object
     */
    OpenAPI addServer(Server server);

    /**
     * Returns the security property from an OpenAPI instance.
     *
     * @return which security mechanisms can be used across the API
     **/
    List<SecurityRequirement> getSecurity();

    /**
     * Sets this OpenAPI instance's security property to the given list.
     *
     * @param security which security mechanisms can be used across the API
     */
    void setSecurity(List<SecurityRequirement> security);

    /**
     * Sets this OpenAPI instance's security property to the given list.
     *
     * @param security which security mechanisms can be used across the API
     * @return the current OpenAPI object
     */
    OpenAPI security(List<SecurityRequirement> security);

    /**
     * Adds the given security requirement to this OpenAPI instance's list of security requirements.
     *
     * @param securityRequirement security mechanism which can be used across the API
     * @return the current OpenAPI object
     */
    OpenAPI addSecurityRequirement(SecurityRequirement securityRequirement);

    /**
     * Returns the tags property from an OpenAPI instance.
     *
     * @return tags used by the specification
     **/

    List<Tag> getTags();

    /**
     * Sets this OpenAPI instance's tags property to the given Tags.
     *
     * @param tags tags used by the specification with additional metadata
     */
    void setTags(List<Tag> tags);

    /**
     * Sets this OpenAPI instance's tags property to the given tags.
     *
     * @param tags tags used by the specification with additional metadata
     * @return the current OpenAPI object
     */
    OpenAPI tags(List<Tag> tags);

    /**
     * Adds the given tag to this OpenAPI instance's list of tags.
     *
     * @param tag a tag used by the specification with additional metadata
     * @return the current OpenAPI object
     */
    OpenAPI addTag(Tag tag);

    /**
     * Returns the paths property from an OpenAPI instance.
     *
     * @return the available paths and operations for the API
     **/
    Paths getPaths();

    /**
     * Sets this OpenAPI instance's paths property to the given paths.
     *
     * @param paths the available paths and operations for the API
     */
    void setPaths(Paths paths);

    /**
     * Sets this OpenAPI instance's paths property to the given paths.
     *
     * @param paths the available paths and operations for the API
     * @return the current OpenAPI object
     */
    OpenAPI paths(Paths paths);

    /**
     * Adds the given path item to this OpenAPI instance's list of paths
     * 
     * @param name a path name in the format valid for a Paths object
     * @param path the path item added to the list of paths
     * @return the current OpenAPI object
     */
    OpenAPI path(String name, PathItem path);

    /**
     * Returns the components property from an OpenAPI instance.
     *
     * @return schemas used in the specification
     **/
    Components getComponents();

    /**
     * Sets this OpenAPI instance's components property to the given components.
     *
     * @param components a set of reusable objects used in the API specification
     */
    void setComponents(Components components);

    /**
     * Sets this OpenAPI instance's components property to the given components.
     *
     * @param components a set of reusable objects used in the API specification
     * @return the current OpenAPI object
     */
    OpenAPI components(Components components);

}