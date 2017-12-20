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
import java.util.Map;

import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.servers.Server;

/**
 * Operation
 * <p>
 * Describes a single API operation on a path.
 * <p>
 * 
 * @see <a href= "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#operationObject">OpenAPI Specification Operation
 *      Object</a>
 */
public interface Operation extends Constructible, Extensible {

    /**
     * Returns the tags property from an Operation instance.
     *
     * @return a list of the operation's tags
     **/
    List<String> getTags();

    /**
     * Sets this Operation's tags property to the given tags.
     *
     * @param tags a list of tags for API documentation control
     **/
    void setTags(List<String> tags);

    /**
     * Sets this Operation's tags property to the given tags.
     *
     * @param tags a list of tags for API documentation control
     * @return the current Operation object
     **/
    Operation tags(List<String> tags);

    /**
     * Adds the given tag to this Operation's list of tags.
     *
     * @param tag a tag for API documentation control
     * @return the current Operation object
     **/
    Operation addTag(String tag);

    /**
     * Returns the summary property from an Operation instance.
     *
     * @return a short summary of what the operation does
     **/
    String getSummary();

    /**
     * Sets this Operation's summary property to the given string.
     *
     * @param summary a short summary of what the operation does
     **/
    void setSummary(String summary);

    /**
     * Sets this Operation's summary property to the given string.
     *
     * @param summary a short summary of what the operation does
     * @return the current Operation object
     **/
    Operation summary(String summary);

    /**
     * Returns the description property from an Operation instance.
     *
     * @return a verbose explanation of the operation behavior
     **/
    String getDescription();

    /**
     * Sets this Operation's description property to the given string.
     *
     * @param description a verbose explanation of the operation behavior
     **/
    void setDescription(String description);

    /**
     * Sets this Operation's description property to the given string.
     *
     * @param description a verbose explanation of the operation behavior
     * @return the current Operation object
     **/
    Operation description(String description);

    /**
     * Returns the externalDocs property from an Operation instance.
     *
     * @return additional external documentation for this operation
     **/
    ExternalDocumentation getExternalDocs();

    /**
     * Sets this Operation's externalDocs property to the given object.
     *
     * @param externalDocs additional external documentation for this operation
     **/
    void setExternalDocs(ExternalDocumentation externalDocs);

    /**
     * Sets this Operation's externalDocs property to the given object.
     *
     * @param externalDocs additional external documentation for this operation
     * @return the current Operation object
     **/
    Operation externalDocs(ExternalDocumentation externalDocs);

    /**
     * Returns the operationId property from an Operation instance.
     *
     * @return unique string used to identify the operation
     **/
    String getOperationId();

    /**
     * Sets this Operation's operationId property to the given string.
     *
     * @param operationId unique string used to identify the operation
     **/
    void setOperationId(String operationId);

    /**
     * Sets this Operation's operationId property to the given string.
     *
     * @param operationId unique string used to identify the operation
     * @return the current Operation object
     **/
    Operation operationId(String operationId);

    /**
     * Returns the parameters property from an Operation instance.
     *
     * @return a list of parameters that are applicable for this operation
     **/
    List<Parameter> getParameters();

    /**
     * Sets this Operation's parameters property to the given parameter list.
     *
     * @param parameters a list of parameters that are applicable for this operation
     **/
    void setParameters(List<Parameter> parameters);

    /**
     * Sets this Operation's parameters property to the given parameter list.
     *
     * @param parameters a list of parameters that are applicable for this operation
     * @return the current Operation object
     **/
    Operation parameters(List<Parameter> parameters);

    /**
     * Adds the given parameter item to this Operation's list of parameters.
     *
     * @param parameter a parameter that is applicable for this operation
     * @return the current Operation object
     **/
    Operation addParameter(Parameter parameter);

    /**
     * Returns the requestBody property from an Operation instance.
     *
     * @return the request body applicable for this operation
     **/
    RequestBody getRequestBody();

    /**
     * Sets this Operation's requestBody property to the given object.
     *
     * @param requestBody the request body applicable for this operation
     **/
    void setRequestBody(RequestBody requestBody);

    /**
     * Sets this Operation's requestBody property to the given object.
     *
     * @param requestBody the request body applicable for this operation
     * @return the current Operation object
     **/
    Operation requestBody(RequestBody requestBody);

    /**
     * Returns the responses property from an Operation instance.
     *
     * @return collection of possible responses from executing this operation
     **/
    APIResponses getResponses();

    /**
     * Sets this Operation's responses property to the given responses.
     *
     * @param responses collection of possible responses from executing this operation
     **/
    void setResponses(APIResponses responses);

    /**
     * Sets this Operation's responses property to the given responses.
     *
     * @param responses collection of possible responses from executing this operation
     * @return the current Operation object
     **/
    Operation responses(APIResponses responses);

    /**
     * Returns the callbacks property from an Operation instance.
     *
     * @return map of possible out-of-band callbacks related to the operation
     **/
    Map<String, Callback> getCallbacks();

    /**
     * Sets this Operation's callbacks property to the given map.
     *
     * @param callbacks map of possible out-of-band callbacks related to the operation. The key value must be the correct format for this field.
     **/
    void setCallbacks(Map<String, Callback> callbacks);

    /**
     * Sets this Operation's callbacks property to the given map.
     *
     * @param callbacks map of possible out-of-band callbacks related to the operation. The key value must be the correct format for this field.
     * @return the current Operation object
     **/
    Operation callbacks(Map<String, Callback> callbacks);

    /**
     * Returns the deprecated property from an Operation instance.
     *
     * @return declaration whether this operation is deprecated
     **/
    Boolean getDeprecated();

    /**
     * Sets this Operation's deprecated property to the given value.
     *
     * @param deprecated declaration whether this operation is deprecated
     **/
    void setDeprecated(Boolean deprecated);

    /**
     * Sets this Operation's deprecated property to the given value.
     *
     * @param deprecated declaration whether this operation is deprecated
     * @return the current Operation object
     **/
    Operation deprecated(Boolean deprecated);

    /**
     * Returns the security property from an Operation instance.
     *
     * @return a list of which security mechanisms can be used for this operation
     **/
    List<SecurityRequirement> getSecurity();

    /**
     * Sets this Operation's security property to the given list.
     *
     * @param security list of which security mechanisms can be used for this operation
     **/
    void setSecurity(List<SecurityRequirement> security);

    /**
     * Sets this Operation's security property to the given list.
     *
     * @param security list of which security mechanisms can be used for this operation
     * @return the current Operation object
     **/
    Operation security(List<SecurityRequirement> security);

    /**
     * Adds the given security requirement item to this Operation's list of security mechanisms.
     *
     * @param securityRequirement security mechanism which can be used for this operation
     * @return the current Operation object
     **/
    Operation addSecurityRequirement(SecurityRequirement securityRequirement);

    /**
     * Returns the servers property from an Operation instance.
     *
     * @return a list of servers to service this operation
     **/
    List<Server> getServers();

    /**
     * Sets this Operation's servers property to the given list.
     *
     * @param servers list of servers to service this operation
     **/
    void setServers(List<Server> servers);

    /**
     * Sets this Operation's servers property to the given list.
     *
     * @param servers list of servers to service this operation
     * @return the current Operation object
     **/
    Operation servers(List<Server> servers);

    /**
     * Adds the given server to this Operation's list of servers.
     *
     * @param server server which can service this operation
     * @return the current Operation object
     **/
    Operation addServer(Server server);

}