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

import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.servers.Server;

/**
 * PathItem
 * <p>
 * Describes the operations available on a single path. A Path Item MAY be empty, due to
 * <a href= "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#security-filtering">security constraints</a>. In that case the
 * path itself is still exposed to the documentation viewer but you will not know which operations and parameters are available.
 * <p>
 * 
 * @see <a href= "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#pathItemObject"> OpenAPI Specification Path Item
 *      Object</a>
 */
public interface PathItem extends Constructible, Extensible, Reference<PathItem> {

    /**
     * All of the possible types of HTTP operations for this path
     **/
    enum HttpMethod {
        POST, GET, PUT, PATCH, DELETE, HEAD, OPTIONS, TRACE
    }

    /**
     * Returns the summary property from a PathItem instance.
     *
     * @return a short summary of what the path item represents
     **/
    String getSummary();

    /**
     * Sets this PathItem's summary property to the given string.
     *
     * @param summary short summary of what the path item represents
     **/
    void setSummary(String summary);

    /**
     * Sets this PathItem's summary property to the given string.
     *
     * @param summary short summary of what the path item represents
     * @return the current PathItem instance
     **/
    PathItem summary(String summary);

    /**
     * Returns the description property from a PathItem instance.
     *
     * @return a detailed description of what the path item represents
     **/
    String getDescription();

    /**
     * Sets this PathItem's description property to the given string.
     *
     * @param description detailed description of what the path item represents
     **/
    void setDescription(String description);

    /**
     * Sets this PathItem's description property to the given string.
     *
     * @param description detailed description of what the path item represents
     * @return the current PathItem instance
     **/
    PathItem description(String description);

    /**
     * Returns the get property from a PathItem instance.
     *
     * @return definition of a GET operation on this path
     **/
    Operation getGET();

    /**
     * Sets this PathItem's get property to the given operation.
     *
     * @param get definition of a GET operation
     **/
    void setGET(Operation get);

    /**
     * Sets this PathItem's get property to the given operation.
     *
     * @param get definition of a GET operation
     * @return the current PathItem instance
     **/
    PathItem GET(Operation get);

    /**
     * Returns the put property from a PathItem instance.
     *
     * @return definition of a PUT operation on this path
     **/
    Operation getPUT();

    /**
     * Sets this PathItem's put property to the given operation.
     *
     * @param put definition of a PUT operation
     **/
    void setPUT(Operation put);

    /**
     * Sets this PathItem's put property to the given operation.
     *
     * @param put definition of a PUT operation
     * @return the current PathItem instance
     **/
    PathItem PUT(Operation put);

    /**
     * Returns the post property from a PathItem instance.
     *
     * @return definition of a POST operation on this path
     **/
    Operation getPOST();

    /**
     * Sets this PathItem's post property to the given operation.
     *
     * @param post definition of a PUT operation
     **/
    void setPOST(Operation post);

    /**
     * Sets this PathItem's post property to the given operation.
     *
     * @param post definition of a PUT operation
     * @return the current PathItem instance
     **/
    PathItem POST(Operation post);

    /**
     * Returns the delete property from a PathItem instance.
     *
     * @return definition of a DELETE operation on this path
     **/
    Operation getDELETE();

    /**
     * Sets this PathItem's delete property to the given operation.
     *
     * @param delete definition of a DELETE operation
     **/
    void setDELETE(Operation delete);

    /**
     * Sets this PathItem's delete property to the given operation.
     *
     * @param delete definition of a DELETE operation
     * @return the current PathItem instance
     **/
    PathItem DELETE(Operation delete);

    /**
     * Returns the options property from a PathItem instance.
     *
     * @return definition of an OPTIONS operation on this path
     **/
    Operation getOPTIONS();

    /**
     * Sets this PathItem's options property to the given operation.
     *
     * @param options definition of an OPTIONS operation
     **/
    void setOPTIONS(Operation options);

    /**
     * Sets this PathItem's options property to the given operation.
     *
     * @param options definition of an OPTIONS operation
     * @return the current PathItem instance
     **/
    PathItem OPTIONS(Operation options);

    /**
     * Returns the head property from a PathItem instance.
     *
     * @return definition of a HEAD operation on this path
     **/
    Operation getHEAD();

    /**
     * Sets this PathItem's head property to the given operation.
     *
     * @param head definition of a HEAD operation
     **/
    void setHEAD(Operation head);

    /**
     * Sets this PathItem's head property to the given operation.
     *
     * @param head definition of a HEAD operation
     * @return the current PathItem instance
     **/
    PathItem HEAD(Operation head);

    /**
     * Returns the patch property from a PathItem instance.
     *
     * @return definition of a PATCH operation on this path
     **/
    Operation getPATCH();

    /**
     * Sets this PathItem's patch property to the given operation.
     *
     * @param patch definition of a PATCH operation
     **/
    void setPATCH(Operation patch);

    /**
     * Sets this PathItem's patch property to the given operation.
     *
     * @param patch definition of a PATCH operation
     * @return the current PathItem instance
     **/
    PathItem PATCH(Operation patch);

    /**
     * Returns the trace property from a PathItem instance.
     *
     * @return definition of a TRACE operation on this path
     **/
    Operation getTRACE();

    /**
     * Sets this PathItem's trace property to the given operation.
     *
     * @param trace definition of a TRACE operation
     **/
    void setTRACE(Operation trace);

    /**
     * Sets this PathItem's trace property to the given operation.
     *
     * @param trace definition of a TRACE operation
     * @return the current PathItem instance
     **/
    PathItem TRACE(Operation trace);

    /**
     * Returns a list of all the operations for this path item.
     * 
     * @return a list of all the operations for this path item
     **/
    List<Operation> readOperations();

    /**
     * Returns a map with all the operations for this path where the keys are HttpMethods.
     * 
     * @return a map with all the operations for this path where the keys are HttpMethods
     **/
    Map<PathItem.HttpMethod, Operation> readOperationsMap();

    /**
     * Returns the servers property from a PathItem instance.
     *
     * @return a list of all the servers defined in this path item
     **/
    List<Server> getServers();

    /**
     * Sets this PathItem's servers property to the given list.
     *
     * @param servers a list of the servers to service operations in this path item
     **/
    void setServers(List<Server> servers);

    /**
     * Sets this PathItem's servers property to the given list.
     *
     * @param servers a list of the servers to service operations in this path item
     * @return the current PathItem instance
     **/
    PathItem servers(List<Server> servers);

    /**
     * Adds the given server to this PathItem's list of servers.
     *
     * @param server a server to service operations in this path item
     * @return the current PathItem instance
     **/
    PathItem addServer(Server server);

    /**
     * Returns the parameters property from this PathItem instance.
     *
     * @return a list of parameters that are applicable to all the operations described under this path
     **/
    List<Parameter> getParameters();

    /**
     * Sets this PathItem's parameters property to the given list.
     *
     * @param parameters a list of parameters that are applicable to all the operations described under this path
     **/
    void setParameters(List<Parameter> parameters);

    /**
     * Sets this PathItem's parameters property to the given list.
     *
     * @param parameters a list of parameters that are applicable to all the operations described under this path
     * @return the current PathItem instance
     **/
    PathItem parameters(List<Parameter> parameters);

    /**
     * Adds the given parameter to this PathItem's list of parameters.
     *
     * @param parameter a parameter that is applicable to all the operations described under this path
     * @return the current PathItem instance
     **/
    PathItem addParameter(Parameter parameter);

}