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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
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

import com.ibm.testapp.g3store.exception.AlreadyExistException;
import com.ibm.testapp.g3store.exception.HandleExceptionsAsyncgRPCService;
import com.ibm.testapp.g3store.exception.InvalidArgException;
import com.ibm.testapp.g3store.exception.NotFoundException;
import com.ibm.testapp.g3store.grpcProducer.api.ProducerGrpcServiceClientImpl;
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
@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/producer")
public class ProducerRestEndpoint extends ProducerGrpcServiceClientImpl {

    public static Logger log = Logger.getLogger(ProducerRestEndpoint.class.getName());

    private static String getSysProp(String key) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key));
    }

    private int getPort() {
        String port = getSysProp("bvt.prop.HTTP_default"); // Store server is running on default
        return Integer.valueOf(port);
    }

    private String getHost() {
        return getSysProp("testing.StoreServer.hostname");
    }

    @Context
    HttpHeaders httpHeaders;

    /**
     * @param reqPOJO
     * @param headers
     * @return
     * @throws Exception
     */
    @PUT
    @Path("/create")
    @APIResponses(value = {
                            @APIResponse(responseCode = "400", description = "Check app response", content = @Content(mediaType = "text/plain")),

                            @APIResponse(responseCode = "200", description = "App is created.",
                                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProducerRestResponse.class))) })

    @RequestBody(name = "appStruct", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppStructure.class)), required = true,
                 description = "new app to add")
    @Operation(summary = "Create the app", description = "It returns the application id created by the Store Service.")
    public Response createApp(AppStructure reqPOJO) throws Exception {

        // Get the input parameters from the REST request
        // Each parameter value will have to be transferred to the grpc request object
        log.info("createApp: request to create app has been received by ProducerRestEndpoint " + reqPOJO);

        String authHeader = httpHeaders.getHeaderString("Authorization");

        if (authHeader == null) {
            // create grpc client
            startService_BlockingStub(getHost(), getPort());
        } else {
            // secure
        }

        try {
            String id = createSingleAppinStore(reqPOJO);
            log.info("createApp: request to create app has been completed by ProducerRestEndpoint " + id);
            // return Response
            return Response.ok().entity(new ProducerRestResponse().appID(id)).build();

        } catch (InvalidArgException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (AlreadyExistException e1) {
            return Response.status(Status.BAD_REQUEST).entity(e1.getMessage()).build();
        }

        finally {
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
    @Path("/create/multi")
    @APIResponses(value = {
                            @APIResponse(responseCode = "400", description = "Check app response", content = @Content(mediaType = "text/plain")),

                            @APIResponse(responseCode = "200", description = "Apps are created.",
                                         content = @Content(mediaType = "application/json",
                                                            schema = @Schema(implementation = ProducerRestResponse.class))) })

    @RequestBody(name = "appStructs", content = @Content(mediaType = "application/json",
                                                         schema = @Schema(implementation = MultiAppStructues.class)),
                 required = true, description = "new apps to add")
    @Operation(summary = "Create the multiple apps", description = "It returns the result from the Store Service.")
    public Response createMultiApps(MultiAppStructues reqPOJO) throws Exception {

        log.info("createMultiApps: request to create apps has been received by ProducerRestEndpoint " + reqPOJO);
        // create grpc client
        startService_AsyncStub(getHost(), getPort());
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
    public Response deleteApp(@Parameter(name = "name", description = "name of the app",
                                         required = true, in = ParameterIn.PATH) @PathParam("name") String name) throws Exception {

        if (name == null) {
            log.severe("An invalid argument is reported on deleteApp request, name =" + name);
            return (Response.status(Status.BAD_REQUEST.getStatusCode()).entity("An invalid name argument")).build();
        } else {
            log.info("deleteApp: request to delete app has been received by ProducerRestEndpoint " + name);
        }
        // create grpc client and send the request
        if (startService_BlockingStub(getHost(), getPort())) {
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
    public Response deleteAllApps() throws Exception {

        log.info("deleteAllApps, prodcuer ,request received to remove all apps");

        // create grpc client and send the request
        if (startService_BlockingStub(getHost(), getPort())) {

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

    @POST
    @Path("/streamingA/server")
    @APIResponses(value = {
                            @APIResponse(responseCode = "200", description = "Server Stream test finished", content = @Content(mediaType = MediaType.APPLICATION_JSON)) })
    public Response serverStreamApp() throws Exception {
        // stuff to call code the will be the grpc client logic

        // Get the input parameters from the REST request
        // Each parameter value will have to be transferred to the grpc request object
        log.info("serverStreamApp(): request to run serverStreamApp test received by ProducerRestEndpoint ");

        String authHeader = httpHeaders.getHeaderString("Authorization");

        if (authHeader == null) {
            // create grpc client
            startService_AsyncStub(getHost(), getPort());
        } else {
            // secure
        }

        try {
            String result = grpcServerStreamApp();
            log.info("serverStreamApp(): request to grpcServerStreamApp() has been completed by ProducerRestEndpoint result: " + result);
            return Response.ok().entity(result).build();

        } catch (InvalidArgException e) {
            return Response.ok().entity("failed with exception: " + e.getMessage()).build();
        }

        finally {
            // stop this grpc service
            stopService();
        }
    }

    @POST
    @Path("/streamingA/twoWay")
    @APIResponses(value = {
                            @APIResponse(responseCode = "200", description = "TwoWay Stream test finished", content = @Content(mediaType = MediaType.APPLICATION_JSON)) })
    public Response twoWayStreamApp(boolean asyncThread) throws Exception {

        log.info("twoWayStreamApp(): request to run twoWayStreamApp test received by ProducerRestEndpoint.  asyncThread:  " + asyncThread);

        String authHeader = httpHeaders.getHeaderString("Authorization");
        if (authHeader == null) {
            // create grpc client
            startService_AsyncStub(getHost(), getPort());
        } else {
            // secure
        }

        try {
            String result = grpcTwoWayStreamApp(asyncThread);
            log.info("serverStreamApp(): request to grpcServerStreamApp() has been completed by ProducerRestEndpoint result: " + result);
            return Response.ok().entity(result).build();

        } catch (InvalidArgException e) {
            return Response.ok().entity("failed with exception: " + e.getMessage()).build();
        }

        finally {
            // stop this grpc service
            stopService();
        }
    }

    public final static int CLIENT_STREAM_MAX_STRESS_CONNECTIONS = 100;
    public final static int CLIENT_STREAM_SLEEP_BETWEEN_STARTING_CONNECTIONS_MSEC = 100;
    public final static int CLIENT_STREAM_TIMEOUT_WAITING_FOR_TEST_COMPLETE_SEC = 60;
    public final static int CLIENT_STREAM_NUMBER_OF_CONCURRENT_CONNECTIONS = 1;

    public int numOfConnections = CLIENT_STREAM_NUMBER_OF_CONCURRENT_CONNECTIONS;
    public static CountDownLatch stressLatch = null;

    @POST
    @Path("/streamingA/client")
    @APIResponses(value = {
                            @APIResponse(responseCode = "200", description = "Client Stream test finished", content = @Content(mediaType = MediaType.APPLICATION_JSON)) })
    public Response clientStreamApp() throws Exception {
        ClientStreamThread[] ta = new ClientStreamThread[CLIENT_STREAM_MAX_STRESS_CONNECTIONS];
        int countConnectionsSuccess = 0;
        int countConnectionsFailed = 0;

        if (numOfConnections > CLIENT_STREAM_MAX_STRESS_CONNECTIONS) {
            numOfConnections = CLIENT_STREAM_MAX_STRESS_CONNECTIONS;
        }
        stressLatch = new CountDownLatch(numOfConnections);

        // stuff to call code the will be the grpc client logic

        // Get the input parameters from the REST request
        // Each parameter value will have to be transferred to the grpc request object
        log.info("clientStreamAppStress(): request to run clientStreamAppStress with count: " + numOfConnections);

        String authHeader = httpHeaders.getHeaderString("Authorization");

        if (authHeader == null) {
            // create grpc client
            startService_AsyncStub(getHost(), getPort());
        } else {
            // secure
        }

        try {
            for (int i = 0; i < numOfConnections; i++) {
                ta[i] = new ClientStreamThread(i);
                Thread t = new Thread(ta[i]);
                t.start();
                if (CLIENT_STREAM_SLEEP_BETWEEN_STARTING_CONNECTIONS_MSEC > 0) {
                    Thread.sleep(CLIENT_STREAM_SLEEP_BETWEEN_STARTING_CONNECTIONS_MSEC);
                }
            }

            try {
                stressLatch.await(CLIENT_STREAM_TIMEOUT_WAITING_FOR_TEST_COMPLETE_SEC, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }

            for (int j = 0; j < numOfConnections; j++) {
                if (ta[j].getResult().indexOf("success") != -1) {
                    countConnectionsSuccess++;
                } else {
                    countConnectionsFailed++;
                }
            }

            log.info("clientStreamAppStress: Success Count: " + countConnectionsSuccess + " Failed Count: " + countConnectionsFailed);

            if (stressLatch.getCount() != 0) {
                log.info("clientStreamAppStress: not all threads completed on time.  outstanding count: " + stressLatch.getCount());
            }

            String resultString = "NotSet";
            if (countConnectionsSuccess == numOfConnections) {
                resultString = "success. number of connections " + countConnectionsSuccess;
            } else {
                resultString = "Failed. TotalCount " + numOfConnections + " Worked " + countConnectionsSuccess;
            }
            return Response.ok().entity(resultString).build();

        } finally {
            // stop this grpc service
            stopService();
        }
    }

    class ClientStreamThread implements Runnable {

        int id = -1;
        String result = "NotDone";

        public ClientStreamThread(int in_id) {
            id = in_id;
        }

        @Override
        public void run() {
            try {
                log.info("clientStreamThread: " + id + " started at:   " + System.currentTimeMillis());
                result = grpcClientStreamApp();
                log.info("clientStreamThread: " + id + " completed at: " + System.currentTimeMillis() + " with result: " + result);
            } finally {
                stressLatch.countDown();
            }
        }

        public String getResult() {
            return result;
        }
    }
}
