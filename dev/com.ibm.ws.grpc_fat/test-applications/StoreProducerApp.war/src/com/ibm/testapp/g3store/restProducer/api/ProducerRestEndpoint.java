/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.testapp.g3store.restProducer.api;

import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import com.ibm.testapp.g3store.exception.HandleExceptionsAsyncgRPCService;
import com.ibm.testapp.g3store.exception.InvalidArgException;
import com.ibm.testapp.g3store.exception.NotFoundException;
import com.ibm.testapp.g3store.grpcProducer.api.ProducergRPCServiceClientImpl;
import com.ibm.testapp.g3store.restProducer.model.AppStructure;
import com.ibm.testapp.g3store.restProducer.model.DeleteAllRestResponse;
import com.ibm.testapp.g3store.restProducer.model.MultiAppStructues;
import com.ibm.testapp.g3store.restProducer.model.ProducerRestResponse;

/**
 * @author anupag
 *
 *         This class is REST server implementation of producer APIs. Each REST
 *         API will further create a gRPC server connection to StoreApp and send
 *         gRPC requests.
 *
 */
@SuppressWarnings("restriction")
@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/producer")
public class ProducerRestEndpoint extends ProducergRPCServiceClientImpl {

    private static Logger log = Logger.getLogger(ProducerRestEndpoint.class.getName());

    /**
     * @param reqPOJO
     * @param headers
     * @return
     * @throws Exception
     */
    @PUT
    @Path("create")
    @APIResponses(value = {
                            @APIResponse(responseCode = "400", description = "Check app response", content = @Content(mediaType = "text/plain")),

                            @APIResponse(responseCode = "200", description = "App is created.",
                                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProducerRestResponse.class))) })

    @RequestBody(name = "appStruct", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppStructure.class)), required = true,
                 description = "new app to add")
    @Operation(summary = "Create the app", description = "It returns the application id created by the Store Service.")
    public Response createApp(AppStructure reqPOJO, @Context HttpHeaders headers) throws Exception {

        // Get the input parameters from the REST request
        // Each parameter value will have to be transferred to the grpc request object
        log.info("createApp: request to create app has been received by ProducerRestEndpoint " + reqPOJO);

        // create grpc client
        startService_BlockingStub("localhost", 9080);

        try {
            String id = createSingleAppinStore(reqPOJO);
            log.info("createApp: request to create app has been completed by ProducerRestEndpoint " + id);
            // return Response
            return Response.ok().entity(new ProducerRestResponse().appID(id)).build();

        } catch (InvalidArgException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } finally {
            // stop the grpc service
            stopService();
        }
    }

    /**
     * @param reqPOJO
     * @param headers
     * @return
     * @throws Exception
     */
    @PUT
    @Path("createAll")
    @APIResponses(value = {
                            @APIResponse(responseCode = "400", description = "Check app response", content = @Content(mediaType = "text/plain")),

                            @APIResponse(responseCode = "200", description = "Apps are created.",
                                         content = @Content(mediaType = "application/json",
                                                            schema = @Schema(implementation = ProducerRestResponse.class))) })

    @RequestBody(name = "appStructs", content = @Content(mediaType = "application/json",
                                                         schema = @Schema(implementation = MultiAppStructues.class)),
                 required = true, description = "new apps to add")
    @Operation(summary = "Create the multiple apps", description = "It returns the result from the Store Service.")
    public Response createMultiApps(MultiAppStructues reqPOJO, @Context HttpHeaders headers) throws Exception {

        log.info("createMultiApps: request to create apps has been received by ProducerRestEndpoint " + reqPOJO);
        // create grpc client
        startService_AsyncStub("localhost", 9080);
        HandleExceptionsAsyncgRPCService handleException = new HandleExceptionsAsyncgRPCService();
        try {
            ProducerRestResponse response = createMultiAppsinStore(reqPOJO, handleException);
            // check if there was any exception thrown
            if ((handleException.getAlExException() != null) || (handleException.getArgException() != null)) {

                ProducerRestResponse errorResponse = new ProducerRestResponse();

                if (handleException.getAlExException() != null) {
                    errorResponse.setCreateResult(handleException.getAlExException().getMessage());
                }
                if ((handleException.getArgException() != null)) {
                    errorResponse.setCreateResult(handleException.getArgException().getMessage());
                }

                log.severe("createMultiApps: ProducerRestEndpoint request to create apps has errors = " + errorResponse.getCreateResult());

                return Response.status(Status.BAD_REQUEST).entity(errorResponse.getCreateResult()).build();
            } else {
                log.info("createMultiApps: request to create apps has been completed by ProducerRestEndpoint = " + response.getCreateResult());
                return Response.ok().entity(response.getCreateResult()).build();
            }

        } finally {
            // stop the grpc service
            stopService();
        }
    }

    /**
     * @param name
     * @param httpHeaders
     * @return
     */
    @Path("/remove/{name}")
    @DELETE
    @APIResponses(value = {
                            @APIResponse(responseCode = "400", description = "Incorrect input", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
                            @APIResponse(responseCode = "200", description = "App is deleted.", content = @Content(mediaType = MediaType.APPLICATION_JSON)) })
    public Response deleteApp(
                              @Parameter(name = "name", description = "name of the app", required = true, in = ParameterIn.PATH) @PathParam("name") String name,
                              @Context HttpHeaders httpHeaders) throws Exception {

        if (name == null) {
            log.severe("An invalid argument is reported on deleteApp request, name =" + name);
            return (Response.status(Status.BAD_REQUEST.getStatusCode()).entity("An invalid name argument")).build();
        } else {
            log.info("deleteApp: request to delete app has been received by ProducerRestEndpoint " + name);
        }
        // create grpc client and send the request
        if (startService_BlockingStub("localhost", 9080)) {
            try {
                String appStruct = deleteSingleAppinStore(name);
                log.info("deleteApp, request to delete app has been completed by ProducerRestEndpoint, result =  "
                         + appStruct);

                return Response.ok().entity(appStruct).build();
            } catch (NotFoundException e) {
                return (Response.status(Status.NOT_FOUND.getStatusCode(), e.getMessage())).build();
            } finally {
                stopService();
            }
        } else {
            return (Response.status(Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Issues with creating gRPC service"))
                            .build();
        }

    }

    /**
     * @param httpHeaders
     * @return
     */
    @Path("/remove/all")
    @DELETE
    @APIResponses(value = {
                            @APIResponse(responseCode = "400", description = "Incorrect input", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
                            @APIResponse(responseCode = "200", description = "Apps are deleted.", content = @Content(mediaType = MediaType.APPLICATION_JSON)) })
    public Response deleteAllApps(@Context HttpHeaders httpHeaders) throws Exception {

        log.info("deleteAllApps, prodcuer ,request received to remove all apps");

        // create grpc client and send the request
        if (startService_BlockingStub("localhost", 9080)) {

            DeleteAllRestResponse response = deleteMultiAppsinStore();
            stopService();
            log.info("deleteAllApps, request to delete apps has been completed by ProducerRestEndpoint, result =  "
                     + response.getDeleteResult());

            return Response.ok().entity(response.getDeleteResult()).build();
        } else {
            return (Response.status(Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Issues with creating gRPC service"))
                            .build();
        }

    }

}
