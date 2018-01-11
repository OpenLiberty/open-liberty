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

package org.eclipse.microprofile.openapi.models.links;

import java.util.Map;

import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.Extensible;
import org.eclipse.microprofile.openapi.models.Reference;
import org.eclipse.microprofile.openapi.models.servers.Server;

/**
 * Link
 * <p>
 * The Link object represents a possible design-time link for a response. The presence of a link does not guarantee the caller's ability to
 * successfully invoke it, rather it provides a known relationship and traversal mechanism between responses and other operations.
 * <p>
 * For computing links, and providing instructions to execute them, a runtime expression is used for accessing values in an operation and using them
 * as parameters while invoking the linked operation.
 * <p>
 * A linked operation MUST be identified using either an operationRef or operationId. In the case of an operationId, it MUST be unique and resolved in
 * the scope of the OAS document. Because of the potential for name clashes, the operationRef syntax is preferred for specifications with external
 * references.
 * <p>
 * 
 * @see <a href= "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#linkObject"> OpenAPI Specification Link Object</a>
 */
public interface Link extends Constructible, Extensible, Reference<Link> {

    /**
     * Returns the server property from a Link instance.
     *
     * @return a server object to be used by the target operation
     **/
    Server getServer();

    /**
     * Sets this Link's server property to the given object.
     *
     * @param server a server object to be used by the target operation
     */
    void setServer(Server server);

    /**
     * Sets this Link's server property to the given object.
     *
     * @param server a server object to be used by the target operation
     * @return the current Link instance
     */
    Link server(Server server);

    /**
     * Returns the operationRef property from a Link instance.
     *
     * @return a relative or absolute reference to an OAS operation
     **/
    String getOperationRef();

    /**
     * Sets this Link's operationRef property to the given string. This field is mutually exclusive of the operationId field.
     *
     * @param operationRef a relative or absolute reference to an OAS operation
     */
    void setOperationRef(String operationRef);

    /**
     * Sets this Link's operationRef property to the given string. This field is mutually exclusive of the operationId field.
     *
     * @param operationRef a relative or absolute reference to an OAS operation
     * @return the current Link instance
     */
    Link operationRef(String operationRef);

    /**
     * Returns the requestBody property from a Link instance.
     *
     * @return a literal value or runtime expression to use as a request body when calling the target operation
     **/
    Object getRequestBody();

    /**
     * Sets this Link's requestBody property to the given object.
     *
     * @param requestBody a literal value or runtime expression to use as a request body when calling the target operation
     */
    void setRequestBody(Object requestBody);

    /**
     * Sets this Link's requestBody property to the given object.
     *
     * @param requestBody a literal value or runtime expression to use as a request body when calling the target operation
     * @return the current Link instance
     */
    Link requestBody(Object requestBody);

    /**
     * Returns the operationId property for this instance of Link.
     *
     * @return the name of an existing, resolvable OAS operation
     */
    String getOperationId();

    /**
     * Sets this Link's operationId property to the given string. This field is mutually exclusive of the operationRef field.
     *
     * @param operationId the name of an existing, resolvable OAS operation
     */
    void setOperationId(String operationId);

    /**
     * Sets this Link's operationId property to the given string. This field is mutually exclusive of the operationRef field.
     *
     * @param operationId the name of an existing, resolvable OAS operation
     * @return the current Link instance
     */
    Link operationId(String operationId);

    /**
     * Returns the parameters property from this instance of Link. The key is the parameter name and the value is a constant or a runtime expression
     * to be passed to the linked operation.
     *
     * @return a map representing parameters to pass to this link's operation
     **/
    Map<String, Object> getParameters();

    /**
     * Sets this Link's parameters property to the given map.
     *
     * @param parameters a map representing parameters to pass to this link's operation as specified with operationId or identified via operationRef
     */
    void setParameters(Map<String, Object> parameters);

    /**
     * Sets this Link's parameters property to the given map and returns the modified Link instance.
     *
     * @param parameters a map representing parameters to pass to this link's operation as specified with operationId or identified via operationRef
     * @return current link instance
     */
    Link parameters(Map<String, Object> parameters);

    /**
     * Add a new parameter to the parameters property of this instance of Link.
     *
     * @param name The name of the parameter. Can be qualified using the parameter location [{in}.]{name} for operations that use the same parameter
     *            name in different locations (e.g. path.id).
     * @param parameter a constant or an expression to be evaluated at runtime and passed to the linked operation
     * @return the current Link instance
     */
    Link addParameter(String name, Object parameter);

    /**
     * Returns the description property from a Link instance.
     *
     * @return a description of the link
     **/
    String getDescription();

    /**
     * Sets this Link's description property to the given string.
     *
     * @param description a description of the link
     **/
    void setDescription(String description);

    /**
     * Sets this Link's description property to the given string.
     *
     * @param description a description of the link
     * @return the current Link instance
     */
    Link description(String description);

}