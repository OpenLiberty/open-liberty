/**
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.openapi;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.tags.Tag;

/**
 * This interface allows application developers to filter different parts of the OpenAPI model tree.
 * 
 * A common scenario is to dynamically augment (update or remove) OpenAPI elements based on the environment
 * that the application is currently in.  
 * 
 * The registration of this filter is controlled by setting the key <b>mp.openapi.filter</b> using
 * one of the configuration sources specified in <a href="https://github.com/eclipse/microprofile-config">MicroProfile Config</a>.
 * The value is the fully qualified name of the filter implementation, which needs to be visible to the application's classloader.
 *
 */
public interface OASFilter {    

    /** 
     * Allows filtering of a particular PathItem.  Implementers of this method can choose to update the given PathItem,
     * pass it back as-is, or return null if removing this PathItem.
     * 
     * @param pathItem the current PathItem element
     * @return the PathItem to be used or null 
     */
    default PathItem filterPathItem(PathItem pathItem){
        return pathItem;
    }
    
    /**
     * Allows filtering of a particular Operation.  Implementers of this method can choose to update the given Operation,
     * pass it back as-is, or return null if removing this Operation.
     * 
     * @param operation the current Operation element
     * @return the Operation to be used or null 
     */
    default Operation filterOperation(Operation operation) {
        return operation;
    }
    
    /**
     * Allows filtering of a particular Parameter.  Implementers of this method can choose to update the given Parameter,
     * pass it back as-is, or return null if removing this Parameter.
     * 
     * @param parameter the current Parameter element
     * @return the Parameter to be used or null 
     */
    default Parameter filterParameter(Parameter parameter) {
        return parameter;
    }
    
    /**
     * Allows filtering of a particular Header.  Implementers of this method can choose to update the given Header,
     * pass it back as-is, or return null if removing this Header.
     * 
     * @param header the current Header element
     * @return the Header to be used or null 
     */
    default Header filterHeader(Header header) {
        return header;
    }
    
    /**
     * Allows filtering of a particular RequestBody.  Implementers of this method can choose to update the given RequestBody,
     * pass it back as-is, or return null if removing this RequestBody.
     * 
     * @param requestBody the current RequestBody element
     * @return the RequestBody to be used or null 
     */
    default RequestBody filterRequestBody(RequestBody requestBody) {
        return requestBody;
    }
    
    /**
     * Allows filtering of a particular APIResponse.  Implementers of this method can choose to update the given APIResponse,
     * pass it back as-is, or return null if removing this APIResponse.
     * 
     * @param apiResponse the current APIResponse element
     * @return the APIResponse to be used or null 
     */
    default APIResponse filterAPIResponse(APIResponse apiResponse) {
        return apiResponse;
    }
    
    /**
     * Allows filtering of a particular Schema.  Implementers of this method can choose to update the given Schema,
     * pass it back as-is, or return null if removing this Schema.
     * 
     * @param schema the current Schema element
     * @return the Schema to be used or null 
     */
    default Schema filterSchema(Schema schema) {
        return schema;
    }
    
    /**
     * Allows filtering of a particular SecurityScheme.  Implementers of this method can choose to update the given SecurityScheme,
     * pass it back as-is, or return null if removing this SecurityScheme.
     * 
     * @param securityScheme the current SecurityScheme element
     * @return the SecurityScheme to be used or null 
     */
    default SecurityScheme filterSecurityScheme(SecurityScheme securityScheme) {
        return securityScheme;
    }
    
    /**
     * Allows filtering of a particular Server.  Implementers of this method can choose to update the given Server,
     * pass it back as-is, or return null if removing this Server.
     * 
     * @param server the current Server element
     * @return the Server to be used or null 
     */
    default Server filterServer(Server server) {
        return server;
    }
    
    /**
     * Allows filtering of a particular Tag.  Implementers of this method can choose to update the given Tag,
     * pass it back as-is, or return null if removing this Tag.
     * 
     * @param tag the current Tag element
     * @return the Tag to be used or null 
     */
    default Tag filterTag(Tag tag) {
        return tag;
    }
    
    /**
     * Allows filtering of a particular Link.  Implementers of this method can choose to update the given Link,
     * pass it back as-is, or return null if removing this Link.
     * 
     * @param link the current Link element
     * @return the Link to be used or null 
     */
    default Link filterLink(Link link) {
        return link;
    }
    
    /**
     * Allows filtering of a particular Callback.  Implementers of this method can choose to update the given Callback,
     * pass it back as-is, or return null if removing this Callback.
     * 
     * @param callback the current Callback element
     * @return the Callback to be used or null 
     */
    default Callback filterCallback(Callback callback) {
        return callback;
    }
    
    /**
     * Allows filtering of the singleton OpenAPI element.  Implementers of this method can choose to update this element, or
     * do nothing if no change is required.  Note that one cannot remove this element from the model tree, hence the return type
     * of void. This is the last method called for a given filter, therefore it symbolizes the end of processing by the vendor
     * framework.
     * 
     * @param openAPI the current OpenAPI element
     */
    default void filterOpenAPI(OpenAPI openAPI) {}
}
