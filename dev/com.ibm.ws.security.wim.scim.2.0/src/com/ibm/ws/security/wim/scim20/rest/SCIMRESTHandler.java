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
package com.ibm.ws.security.wim.scim20.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.ibm.websphere.security.wim.scim20.SCIMService;
import com.ibm.websphere.security.wim.scim20.exceptions.AuthorizationException;
import com.ibm.websphere.security.wim.scim20.exceptions.InvalidFilterException;
import com.ibm.websphere.security.wim.scim20.exceptions.InvalidSyntaxException;
import com.ibm.websphere.security.wim.scim20.exceptions.InvalidValueException;
import com.ibm.websphere.security.wim.scim20.exceptions.InvalidVersionException;
import com.ibm.websphere.security.wim.scim20.exceptions.MutabilityException;
import com.ibm.websphere.security.wim.scim20.exceptions.NotFoundException;
import com.ibm.websphere.security.wim.scim20.exceptions.NotImplementedException;
import com.ibm.websphere.security.wim.scim20.exceptions.SCIMException;
import com.ibm.websphere.security.wim.scim20.exceptions.TooManyResultsException;
import com.ibm.websphere.security.wim.scim20.exceptions.UniquenessException;
import com.ibm.websphere.security.wim.scim20.model.ListResponse;
import com.ibm.websphere.security.wim.scim20.model.SearchRequest;
import com.ibm.websphere.security.wim.scim20.model.groups.Group;
import com.ibm.websphere.security.wim.scim20.model.users.User;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.management.security.ManagementSecurityConstants;
import com.ibm.ws.security.wim.scim20.SCIMServiceV20;
import com.ibm.ws.security.wim.scim20.SCIMUtil;
import com.ibm.ws.security.wim.scim20.model.ErrorImpl;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/*
 * TODO SEE COMMENTS BELOW.
 *
 * In order to service both SCIM 1.0 and SCIM 2.0 requests, we would really need to present
 * this as a separate service that gets spun up when either SCIM feature is requested.
 *
 * The SCIM 1.0 and SCIM 2.0 features would each present their own service that this class
 * would depend on. Unfortunately, SCIM 1.0 depends on Java 7 and SCIM 2.0 will depend on Java 8
 * (especially if we replace Jackson with JSONB). In September 2019, I believe Java 7 support
 * ends at which time SCIM 1.0 would move up to (at least) Java 8.
 */
@Component(service = { RESTHandler.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM",
                                                                                                                             RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=/scim",
                                                                                                                             RESTHandler.PROPERTY_REST_HANDLER_CUSTOM_SECURITY + "=true" })
public class SCIMRESTHandler implements RESTHandler {

    private static final String ENDPOINT_GROUPS = "Groups";

    private static final String ENDPOINT_USERS = "Users";

    private static final String ENDPOINT_RESOURCE_TYPES = "ResourceTypes";

    private static final String ENDPOINT_SCHEMAS = "Users";

    private static final String ENDPOINT_ME = "Me";

    private static final String ENDPOINT_SERVICE_PROVIDER_CONFIG = "ServiceProviderConfig";

    private static final String ENDPOINT_SEARCH = ".search";

    /**
     * Roles that would be sufficient to perform a read.
     */
    private static final Set<String> REQUIRED_ROLES_READ = new HashSet<String>(Arrays.asList(new String[] { ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME,
                                                                                                            ManagementSecurityConstants.READER_ROLE_NAME }));
    /**
     * Roles that would be sufficient to perform a write.
     */
    private static final Set<String> REQUIRED_ROLES_WRITE = new HashSet<String>(Arrays.asList(new String[] { ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME }));

    /** The SCIM 2.0 service. */
    // TODO When we support SCIM 1.0 and 2.0 in this handler, we will want to rely on the OSGi service.
    public final SCIMService serviceV20 = new SCIMServiceV20();

    /**
     * Handle the incoming DELETE request.
     *
     * @param service The {@link SCIMService} to call.
     * @param request The incoming request.
     * @param response The outgoing response.
     * @throws InvalidSyntaxException If there was no resource ID in the URL.
     * @throws NotFoundException If the specified endpoint does not exist.
     * @throws AuthorizationException If the user does not have authorization to perform the requested operation.
     */
    private static void handleDelete(SCIMService service, RESTRequest request, RESTResponse response) throws InvalidSyntaxException, NotFoundException, AuthorizationException {

        /*
         * Check if the authenticated user is allowed to perform a write operation.
         */
        checkHasWriteRole(request); // TODO Will need to check in SCIMService for local API.

        /*
         * Deleting requires a resource ID. If there is no resource ID, throw an error.
         */
        String resourceId = RESTUtil.getResourceId(request.getPath());
        if (resourceId == null || resourceId.trim().isEmpty()) {
            throw new InvalidSyntaxException("No resource ID was found for the DELETE operation.");
        }

        /*
         * Delete the resource using the SCIMService.
         */
        String endpoint = RESTUtil.getEnpoint(request.getPath());
        if (ENDPOINT_GROUPS.equals(endpoint)) {
            service.deleteGroup(resourceId);
        } else if (ENDPOINT_USERS.equals(endpoint)) {
            service.deleteUser(resourceId);
        } else {
            throw new NotFoundException("The endpoint '" + endpoint + "' does not exist.");
        }
    }

    /**
     * Handle the incoming GET request.
     *
     * @param service The {@link SCIMService} to call.
     * @param request The incoming request.
     * @param response The outgoing response.
     * @return The retrieved resource.
     * @throws NotFoundException If the specified endpoint does not exist.
     * @throws InvalidValueException
     * @throws InvalidFilterException
     * @throws TooManyResultsException
     * @throws AuthorizationException If the user does not have authorization to perform the requested operation.
     */
    private static Object handleGet(SCIMService service, RESTRequest request,
                                    RESTResponse response) throws NotFoundException, InvalidValueException, InvalidFilterException, TooManyResultsException, AuthorizationException {

        /*
         * Check if the authenticated user is allowed to perform a read operation.
         */
        checkHasReadRole(request); // TODO Will need to check in SCIMService for local API.

        // TODO Need to handle confidential fields in filter (here or in the SCIMService layer).

        String path = request.getPath();
        String endpoint = RESTUtil.getEnpoint(path);
        String resourceId = RESTUtil.getResourceId(path);

        /*
         * Get the resource using the SCIMService.
         */
        if (ENDPOINT_GROUPS.equals(endpoint)) {
            if (resourceId != null && !resourceId.trim().isEmpty()) {
                return service.getGroup(resourceId, RESTUtil.getAttributes(request));
            } else {
                return service.getGroups(RESTUtil.getFilter(request), RESTUtil.getAttributes(request), RESTUtil.getExcludedAttributes(request));
            }
        } else if (ENDPOINT_RESOURCE_TYPES.equals(endpoint)) {
            if (resourceId != null && !resourceId.trim().isEmpty()) {
                return service.getResourceTypes();
            } else {
                return service.getResourceType(resourceId);
            }
        } else if (ENDPOINT_SCHEMAS.equals(endpoint)) {
            if (resourceId != null && !resourceId.trim().isEmpty()) {
                return service.getSchemas(resourceId);
            } else {
                return service.getSchemas();
            }
        } else if (ENDPOINT_SERVICE_PROVIDER_CONFIG.equals(endpoint)) {
            return service.getServiceProviderConfig();
        } else if (ENDPOINT_ME.equals(endpoint)) {
            return service.getMe(RESTUtil.getAttributes(request), RESTUtil.getExcludedAttributes(request));
        } else if (ENDPOINT_USERS.equals(endpoint)) {
            if (resourceId != null && !resourceId.trim().isEmpty()) {
                return service.getUser(resourceId, RESTUtil.getAttributes(request), RESTUtil.getExcludedAttributes(request));
            } else {
                return service.getUsers(RESTUtil.getFilter(request), RESTUtil.getAttributes(request), RESTUtil.getExcludedAttributes(request));
            }
        } else {
            throw new NotFoundException("The endpoint '" + endpoint + "' does not exist.");
        }
    }

    /**
     * Handle the incoming POST operation.
     *
     * @param service The {@link SCIMService} to call.
     * @param request The incoming request.
     * @param response The outgoing response.
     * @return The new resource or the {@link ListResponse} containing the resource searched for.
     * @throws InvalidSyntaxException
     * @throws NotFoundException
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws UniquenessException
     * @throws InvalidValueException
     * @throws IOException
     * @throws InvalidFilterException
     * @throws TooManyResultsException
     * @throws AuthorizationException If the user does not have authorization to perform the requested operation.
     */
    private static Object handlePost(SCIMService service, RESTRequest request,
                                     RESTResponse response) throws InvalidSyntaxException, NotFoundException, JsonParseException, JsonMappingException, UniquenessException, InvalidValueException, IOException, InvalidFilterException, TooManyResultsException, AuthorizationException {

        String endpoint = RESTUtil.getEnpoint(request.getPath());
        String resourceId = RESTUtil.getResourceId(request.getPath());

        /*
         * For POST, we can either create a resource OR search for a set of resources, depending on the
         * URL.
         */
        if (ENDPOINT_SEARCH.equals(endpoint) || ENDPOINT_SEARCH.equals(resourceId)) {
            /*
             * Support searches for "/Groups/.search", "/Users/.search", and ".search".
             */
            if (ENDPOINT_GROUPS.equals(endpoint) || ENDPOINT_USERS.equals(endpoint) || ENDPOINT_SEARCH.equals(endpoint)) {
                /*
                 * Check if the authenticated user is allowed to perform a read operation.
                 */
                checkHasReadRole(request); // TODO Will need to check in SCIMService for local API.

                return service.getResources(endpoint, SCIMUtil.deserialize(getContent(request), SearchRequest.class));
            } else {
                throw new NotFoundException("The endpoint '" + endpoint + "' does not exist.");
            }
        } else {
            /*
             * Check if the authenticated user is allowed to perform a write operation.
             */
            checkHasWriteRole(request); // TODO Will need to check in SCIMService for local API.

            if (ENDPOINT_GROUPS.equals(endpoint)) {
                return service.createGroup(SCIMUtil.deserialize(getContent(request), Group.class));
            } else if (ENDPOINT_USERS.equals(endpoint)) {
                return service.createUser(SCIMUtil.deserialize(getContent(request), User.class));
            } else {
                throw new NotFoundException("The endpoint '" + endpoint + "' does not exist.");
            }
        }
    }

    /**
     * Handle the incoming PUT operation.
     *
     * @param service The {@link SCIMService} to call.
     * @param request The incoming request.
     * @param response The outgoing response.
     * @return The updated resource.
     * @throws InvalidSyntaxException
     * @throws NotFoundException
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws MutabilityException
     * @throws InvalidValueException
     * @throws IOException
     * @throws AuthorizationException If the user does not have authorization to perform the requested operation.
     */
    private static Object handlePut(SCIMService service, RESTRequest request,
                                    RESTResponse response) throws InvalidSyntaxException, NotFoundException, JsonParseException, JsonMappingException, MutabilityException, InvalidValueException, IOException, AuthorizationException {

        /*
         * Check if the authenticated user is allowed to perform a write operation.
         */
        checkHasWriteRole(request); // TODO Will need to check in SCIMService for local API.

        /*
         * Get the resource ID to update.
         */
        String resourceId = RESTUtil.getResourceId(request.getPath());
        if (resourceId == null || resourceId.trim().isEmpty()) {
            throw new InvalidSyntaxException("The SCIM PUT operation requires a resource ID in the URL.");
        }

        /*
         * Create the resource using the SCIMService.
         */
        String endpoint = RESTUtil.getEnpoint(request.getPath());
        if (ENDPOINT_GROUPS.equals(endpoint)) {
            return service.updateGroup(resourceId, SCIMUtil.deserialize(getContent(request), Group.class));
        } else if (ENDPOINT_USERS.equals(endpoint)) {
            return service.updateUser(resourceId, SCIMUtil.deserialize(getContent(request), User.class));
        } else {
            throw new NotFoundException("The endpoint '" + endpoint + "' does not exist.");
        }
    }

    /**
     * Get the content from the {@link RESTRequest}.
     *
     * @param request The incoming request.
     * @return The {@link String} containing the JSON
     * @throws InvalidSyntaxException If there was no content on the request.
     */
    private static String getContent(RESTRequest request) throws InvalidSyntaxException {

        // TODO Should we check the content-type header?

        String content = null;

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8));
            String line = null;
            StringBuffer sb = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            content = sb.toString();
        } catch (IOException e) {
            // TODO This would be an issue.
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Not much we can do. FFDC will catch this.
                }
            }
        }

        if (content == null || content.trim().isEmpty()) {
            throw new InvalidSyntaxException("It was expected that the request contained content, but no content was found on the request.");
        }

        return content;
    }

    /**
     * Get the {@link SCIMService} that should be used for this request.
     *
     * @param request The {@link RESTRequest} for this SCIM request.
     * @return The {@link SCIMService} to use to process this request.
     * @throws InvalidVersionException If the requested SCIM version is not supported (or available).
     */
    private SCIMService getServiceForRequest(RESTRequest request) throws InvalidVersionException {

        /*
         * Validate the SCIM version requested.
         */
        RESTUtil.getApiVersion(request.getPath());

        // TODO When we support SCIM 1.0 in the handler we would want to use the returned version to determine which service to use.
        return serviceV20;
    }

    @Override
    @FFDCIgnore(SCIMException.class)
    public void handleRequest(RESTRequest request, RESTResponse response) throws IOException {

        int status = 500;
        String jsonResponse = null;

        try {
            /*
             * Get the SCIM service to use for this request. We do this first instead of
             * in the handle* methods because if the service is unavailable, we want that
             * to be the error that is returned since correcting any other error is still
             * going to result in failure.
             */
            SCIMService service = getServiceForRequest(request);

            /*
             * Use the REST requests HTTP method to determin what operation to perform.
             */
            String method = request.getMethod();
            if ("GET".equalsIgnoreCase(method)) {
                Object obj = handleGet(service, request, response);

                status = 200;
                jsonResponse = SCIMUtil.serialize(obj);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                handleDelete(service, request, response);

                status = 204;
            } else if ("POST".equalsIgnoreCase(method)) {
                Object obj = handlePost(service, request, response);

                if (obj instanceof ListResponse<?>) {
                    status = 200; // We did a search
                } else {
                    status = 201; // We created something.
                }
                jsonResponse = SCIMUtil.serialize(obj);
            } else if ("PUT".equalsIgnoreCase(method)) {
                Object obj = handlePut(service, request, response);

                status = 200;
                jsonResponse = SCIMUtil.serialize(obj);
            } else {
                throw new NotImplementedException("The SCIM service does not supported the request operation: '" + (method == null ? "null" : method.toUpperCase() + "'"));
            }

        } catch (SCIMException e) {
            /*
             * If we have an SCIMException, we can simply generate the JSON response
             * from the exception.
             */
            status = e.getHttpCode();
            jsonResponse = e.asJson();
        } catch (Exception e) {
            /*
             * Unanticipated exception.
             */
            status = 500;
            jsonResponse = "{\"schemas\" : \"" + ErrorImpl.SCHEMA_URI + "\"";
            jsonResponse += "\"detail\" : \"" + e.getMessage() + "\"";
            jsonResponse += "\"status\" : 500}";
        }

        /*
         * Write the response back to the stream.
         */
        response.setStatus(status);
        if (jsonResponse != null) {
            response.getWriter().write(jsonResponse);
        }
        response.getWriter().flush();
        response.getWriter().close();
    }

    /**
     * Check if the currently authenticated user has a role that would allow them to perform a write operation.
     *
     * @param request The HTTP request.
     * @throws AuthorizationException If the authenticated user does NOT have permission to perform a write operation.
     */
    private static void checkHasWriteRole(RESTRequest request) throws AuthorizationException {
        if (!request.isUserInRole(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME)) {
            // TODO Probably need to make sure this message doesn't make it to the end-user in the HTTP response.
            throw new AuthorizationException("The 'Administrator' role is required to create, delete, or update a resource.", REQUIRED_ROLES_WRITE);
        }
    }

    /**
     * Check if the currently authenticated user has a role that would allow them to perform a read operation.
     *
     * @param request The HTTP request.
     * @throws AuthorizationException If the authenticated user does NOT have permission to perform a read operation.
     */
    private static void checkHasReadRole(RESTRequest request) throws AuthorizationException {
        if (!request.isUserInRole(ManagementSecurityConstants.READER_ROLE_NAME) && !request.isUserInRole(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME)) {
            // TODO Probably need to make sure this message doesn't make it to the end-user in the HTTP response.
            throw new AuthorizationException("The 'Administrator' or 'Reader' role is required to read a resource.", REQUIRED_ROLES_READ);
        }
    }
}
