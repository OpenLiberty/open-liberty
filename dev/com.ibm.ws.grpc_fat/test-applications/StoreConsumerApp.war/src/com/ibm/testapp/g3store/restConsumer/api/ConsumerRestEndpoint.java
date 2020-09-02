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
package com.ibm.testapp.g3store.restConsumer.api;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

import com.ibm.testapp.g3store.exception.HandleExceptionsFromgRPCService;
import com.ibm.testapp.g3store.exception.InvalidArgException;
import com.ibm.testapp.g3store.exception.NotFoundException;
import com.ibm.testapp.g3store.exception.UnauthException;
import com.ibm.testapp.g3store.grpcConsumer.api.ConsumerGrpcServiceClientImpl;
import com.ibm.testapp.g3store.restConsumer.model.AppListWithPricesPOJO;
import com.ibm.testapp.g3store.restConsumer.model.AppNameList;
import com.ibm.testapp.g3store.restConsumer.model.AppNamewPriceListPOJO;
import com.ibm.testapp.g3store.restConsumer.model.AppStructureConsumer;
import com.ibm.testapp.g3store.utilsConsumer.ConsumerUtils;

/**
 * @author anupag
 *
 *         This class is REST server implementation of consuner APIs. Each REST
 *         API will further create a gRPC server connection to StoreApp and send
 *         gRPC requests.
 *
 */
@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/consumer")
public class ConsumerRestEndpoint extends ConsumerGrpcServiceClientImpl {

    private static Logger log = Logger.getLogger(ConsumerRestEndpoint.class.getName());

    @Context
    HttpHeaders httpHeaders;

    /**
     * The Authorization header with JWT token
     * will be propagated using grpcClient for this API.
     *
     * Only uncomment RolesAllowed here if need to test security for RestEndpoint.
     *
     * @return
     */
    @GET
    @Path("/appNames/{testMethodName}")
//    @RolesAllowed({ "Administrator", "students" })
//    @SecurityRequirement(name = "JWTAuthorization")
    @APIResponses(value = {
                            @APIResponse(responseCode = "400", description = "Bad Request — Client sent an invalid request", content = @Content(mediaType = "text/plain")),
                            @APIResponse(responseCode = "404", description = "Not Found — The requested resources does not exist.", content = @Content(mediaType = "text/plain")),
                            @APIResponse(responseCode = "200", description = "The list of app names are returned.",
                                         content = @Content(mediaType = "application/json", schema = @Schema(type = SchemaType.OBJECT, implementation = AppNameList.class))) })
    public Response getAllAppNames(@Parameter(name = "testMethodName", description = "test name", required = true,
                                              in = ParameterIn.PATH) @PathParam("testMethodName") String testMethodName) {

        String m = testMethodName;

        log.info(m + ": Received request to get all available AppNames");

        String authHeader = httpHeaders.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (log.isLoggable(Level.FINE)) {
            log.finest(m + ": this authHeader will be added to grpc request = " + authHeader);
        }

        if (m.equalsIgnoreCase("testGetAppName_CookieAuth_GrpcTarget")) {
            String cookieHeader = httpHeaders.getRequestHeaders().getFirst(HttpHeaders.COOKIE);
            if (log.isLoggable(Level.FINE)) {
                log.finest(m + ": this cookie Header will be added to grpc request = " + cookieHeader);
            }
        }

        // Authorization header will be passed with grpcTarget
        // connect to gRPC service running in Store server
        startService_BlockingStub(ConsumerUtils.getStoreServerHost(), ConsumerUtils.getStoreServerPort());

        try {
            // call the gRPC API and get Result
            List<String> nameList = getAllAppNameList(m);

            log.info(m + ": request to get appName has been completed by ConsumerRestEndpoint ");
            // respond as JSON
            return Response.ok().entity(nameList).build();

        } catch (NotFoundException e) {
            return Response.status(Status.NOT_FOUND).build();
        } catch (UnauthException e) {

            if ((testMethodName.equalsIgnoreCase("getAppName_NullJWTAuth_GrpcTarget")) ||
                (testMethodName.equalsIgnoreCase("testGetAppName_BadServerRoles_GrpcTarget")) ||
                (testMethodName.equalsIgnoreCase("testGetAppName_BadRole_CookieAuth_GrpcTarget"))) {
                return Response.status(Status.OK).entity(e.getMessage()).build();
            } else {
                return Response.status(Status.UNAUTHORIZED).build();
            }
        } finally {
            // shurdown the gRPC connection
            stopService();
        }
    }

    /**
     * The Authorization header with JWT token
     * will be added in Metadata using CallCredential API
     *
     *
     * @return
     */
    @GET
    @Path("/appNames_CC")
    @APIResponses(value = {
                            @APIResponse(responseCode = "400", description = "Bad Request — Client sent an invalid request", content = @Content(mediaType = "text/plain")),
                            @APIResponse(responseCode = "404", description = "Not Found — The requested resources does not exist.", content = @Content(mediaType = "text/plain")),
                            @APIResponse(responseCode = "200", description = "The list of app names are returned.",
                                         content = @Content(mediaType = "application/json", schema = @Schema(type = SchemaType.OBJECT, implementation = AppNameList.class))) })
    public Response getAllAppNames_Auth_CallCred() {

        String m = "getAllAppNames_Auth_CallCred";

        log.info(m + ": Received request to get all available AppNames");

        String authHeader = httpHeaders.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (log.isLoggable(Level.FINE)) {
            log.finest(m + ": authHeader " + authHeader);
        }

        // Add Auth header using CallCred and connect to gRPC service
        // connect to gRPC service running in Store server
        this.startService_Auth_CallCred_BlockingStub(ConsumerUtils.getStoreServerHost(), ConsumerUtils.getStoreServerPort(), authHeader);

        try {
            // call the gRPC API and get Result
            List<String> nameList = getAllAppNameList_Auth_CallCred();

            log.info(m + ": request to get appName has been completed by ConsumerRestEndpoint ");
            // respond as JSON
            return Response.ok().entity(nameList).build();

        } catch (NotFoundException e) {
            return Response.status(Status.NOT_FOUND).build();
        } catch (UnauthException e) {
            return Response.status(Status.UNAUTHORIZED).build();
        } finally {
            // shurdown the gRPC connection
            stopService();
        }
    }

    /**
     * The Authorization header with Basic
     * will be propagated using grpcClient for this API.
     *
     * This will also test the security for this RESTEndpoint.
     *
     * @param appName
     * @return
     */
    @Path("/appInfo/{appName}")
    @RolesAllowed({ "students", "Administrator" })
    @SecurityRequirement(name = "ConsumerBasicHttp")
    @GET
    @APIResponses(value = {
                            @APIResponse(responseCode = "400", description = "Incorrect input", content = @Content(mediaType = "text/plain")),
                            @APIResponse(responseCode = "200", description = "App name list is returned.",
                                         content = @Content(mediaType = "application/json",
                                                            schema = @Schema(type = SchemaType.OBJECT, implementation = AppStructureConsumer.class))) })
    public Response getAppInfo(
                               @Parameter(name = "appName", description = "name of the app", required = true, in = ParameterIn.PATH) @PathParam("appName") String appName) {

        String m = "getAppInfo";

        log.info(m + ": request to get appInfo has been received by ConsumerRestEndpoint " + appName);

        String authHeader = httpHeaders.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (log.isLoggable(Level.FINE)) {
            log.finest("getAppInfo: authHeader " + authHeader);
        }

        // connect to gRPC service
        startService_BlockingStub(ConsumerUtils.getStoreServerHost(), ConsumerUtils.getStoreServerPort());

        // call the gRPC API
        String appInfo_JSONString = null;
        try {
            appInfo_JSONString = getAppJSONStructure(appName, m);

            log.info(m + ": request to get appInfo has been completed by ConsumerRestEndpoint " + appInfo_JSONString);
        } catch (InvalidArgException e) {
            return Response.status(Status.BAD_REQUEST).build();
        } catch (UnauthException e) {
            return Response.status(Status.UNAUTHORIZED).build();
        } finally {
            // shurdown the gRPC connection
            stopService();
        }

        // return JSON response
        return Response.ok().entity(appInfo_JSONString).build();

    }

    /**
     * The Authorization header with Bad Basic Auth values
     * will be propagated using grpcClient for this API.
     * Test if correct grpc return code are sent from the server.
     *
     *
     * @param appName
     * @return
     */
    @Path("/appInfo_BadAuth/{appName}")
    @GET
    @APIResponses(value = {
                            @APIResponse(responseCode = "400", description = "Incorrect input", content = @Content(mediaType = "text/plain")),
                            @APIResponse(responseCode = "200", description = "App name list is returned.",
                                         content = @Content(mediaType = "application/json",
                                                            schema = @Schema(type = SchemaType.OBJECT, implementation = AppStructureConsumer.class))) })
    public Response getAppInfo_BadAuth(
                                       @Parameter(name = "appName", description = "name of the app", required = true, in = ParameterIn.PATH) @PathParam("appName") String appName) {

        String m = "getAppInfo_BadAuth";

        log.info(m + ": request to get appInfo has been received by ConsumerRestEndpoint " + appName);

        String authHeader = httpHeaders.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (log.isLoggable(Level.FINE)) {
            log.finest(m + ": authHeader " + authHeader);
        }

        // connect to gRPC service
        startService_BlockingStub(ConsumerUtils.getStoreServerHost(), ConsumerUtils.getStoreServerPort());

        // call the gRPC API
        String appInfo_JSONString = null;
        try {
            appInfo_JSONString = getAppJSONStructure(appName, m);

            log.info(m + ": request to get appInfo has been completed by ConsumerRestEndpoint " + appInfo_JSONString);
        } catch (InvalidArgException e) {
            return Response.status(Status.BAD_REQUEST).build();
        } catch (UnauthException e) {
            return Response.status(Status.OK).entity(e.getMessage()).build();
        } finally {
            // shurdown the gRPC connection
            stopService();
        }

        // return JSON response
        return Response.ok().entity(appInfo_JSONString).build();

    }

    // send request with one appName and server will respond with priceList for each
    // appName.
    // stream the apoNames and server will respond the stream of priceLists
    // when client is done , server will also complete.

    // rpc getPrices(stream AppNameRequest) returns (stream PriceResponse) {};

    // The REST client does not support streaming so grab all the streamed responses

    /**
     * @param appNames
     * @return
     */
    @GET
    @Path("/priceQuery")
    @APIResponses(value = {
                            @APIResponse(responseCode = "400", description = "Incorrect input", content = @Content(mediaType = "text/plain")),
                            @APIResponse(responseCode = "200", description = "App name list is returned.",
                                         content = @Content(mediaType = "application/json",
                                                            schema = @Schema(type = SchemaType.OBJECT, implementation = AppListWithPricesPOJO.class))) })
    public Response getPrices(@QueryParam("appName") List<String> appNames) {

        log.info("getPrices: Received request to get Prices for the appNames " + appNames);

        // connect to service using Async Stub
        startService_AsyncStub(ConsumerUtils.getStoreServerHost(), ConsumerUtils.getStoreServerPort());

        HandleExceptionsFromgRPCService handleException = new HandleExceptionsFromgRPCService();
        //call the gRPC API and get response
        List<AppNamewPriceListPOJO> listOfAppNames_w_PriceList = getAppswPrices(appNames);

        Response resp = null;

        if (handleException.getArgException() != null || (handleException.getNfException() != null)) {

            resp = Response.status(Status.BAD_REQUEST).build();

        }

        // stop Service
        stopService();
        // POJO to return
        AppListWithPricesPOJO listResponse = new AppListWithPricesPOJO(); // bean for response
        // set in grpc response in POJO
        listResponse.setAppNameswPrice(listOfAppNames_w_PriceList);

        log.info("getPrices: Completed request to get Prices for the appNames " + appNames);

        resp = Response.ok().entity(listResponse).build();

        return resp;

    }

}
