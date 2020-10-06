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
package com.ibm.ws.grpc.fat.helloworld.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;

@WebServlet(name = "grpcClient", urlPatterns = "/grpcClient")
public class HelloWorldClientServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    ManagedChannel channel = null;
    private GreeterGrpc.GreeterBlockingStub greetingService;

    private void startService(String address, int port, boolean useTls, String serverPath) throws SSLException {
        System.out.println("connecting to helloworld gRPC service at " + address + ":" + port);
        System.out.println("TLS enabled: " + useTls);

        if (!useTls) {
            channel = ManagedChannelBuilder.forAddress(address, port).usePlaintext().build();
        } else {
            channel = ManagedChannelBuilder.forAddress(address, port).build();
        }
        greetingService = GreeterGrpc.newBlockingStub(channel);
    }

    private void stopService() {
        boolean terminated = false;
        try {
            channel.shutdownNow();
            terminated = channel.awaitTermination(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
        System.out.println("Channel has been closed: " + terminated);
    }

    @Override
    protected void doGet(HttpServletRequest reqest, HttpServletResponse response) throws ServletException, IOException {

        // set response headers
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        // create HTML form
        PrintWriter writer = response.getWriter();
        writer.append("<!DOCTYPE html>\r\n")
                        .append("<html>\r\n")
                        .append("               <head>\r\n")
                        .append("                       <title>gRPC Client</title>\r\n")
                        .append("               </head>\r\n")
                        .append("               <body>\r\n")
                        .append("                       <h3>gRPC helloworld client example</h3>\r\n")
                        .append("                       <form action=\"grpcClient\" method=\"POST\" name=\"form1\">\r\n")
                        .append("                               Enter your name: \r\n")
                        .append("                               <input type=\"text\" name=\"user\" />\r\n\r\n")
                        .append("                               <br/>")
                        .append("                               Enter the address of the target service: \r\n")
                        .append("                               <input type=\"text\" value=\"localhost\" name=\"address\" />\r\n\r\n")
                        .append("                               <br/>")
                        .append("                               Enter the port of the target service: \r\n")
                        .append("                               <input type=\"text\" value=\"9080\" name=\"port\" />\r\n\r\n")
                        .append("                               <br/>")
                        .append("                               Use TLS: \r\n")
                        .append("                               <input type=\"text\" value=\"false\" name=\"useTls\" />\r\n\r\n")
                        .append("                               <br/>")
                        .append("                               server path: \r\n")
                        .append("                               <input type=\"text\" value=\"\" name=\"serverPath\" />\r\n\r\n")
                        .append("                               <br/>")
                        .append("                               <br/>")
                        .append("                               <input type=\"submit\" value=\"Submit\" name=\"submit\" />\r\n")
                        .append("                       </form>\r\n")
                        .append("               </body>\r\n")
                        .append("</html>\r\n");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String user = request.getParameter("user");
        String address = request.getParameter("address");
        int port = Integer.parseInt(request.getParameter("port"));
        boolean useTls = Boolean.parseBoolean(request.getParameter("useTls"));
        String serverPath = request.getParameter("serverPath");

        try {
            startService(address, port, useTls, serverPath);

            // client side of the gRPC service is accessed via this servlet
            // create a gRPC User message to send to the server side service
            // the User class is derived from the HelloWorld.proto file being compiled into java code
            HelloRequest person = HelloRequest.newBuilder().setName(user).build();

            // greetingService class is derived from HelloWorld.proto file being compiled into java code
            // Remote Procedure Call the greetUser method on the greetingService, more than one gRPC
            // message can be return, so read all responses into an Iterator
            HelloReply greeting = greetingService.sayHello(person);

            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");

            // create HTML response
            PrintWriter writer = response.getWriter();
            writer.append("<!DOCTYPE html>\r\n")
                            .append("<html>\r\n")
                            .append("               <head>\r\n")
                            .append("                       <title>Welcome message</title>\r\n")
                            .append("               </head>\r\n")
                            .append("               <body>\r\n");
            if (user != null && !user.trim().isEmpty()) {
                writer.append("<h3>gRPC service response</h3>\r\n");
                writer.append(greeting.toString());
            } else {
                writer.append("     You did not enter a name!\r\n");
            }
            writer.append("\r\n")
                            .append("                               <br/>\r\n")
                            .append("                               <br/>\r\n")
                            .append("<form><input type=\"button\" value=\"Go back!\" onclick=\"history.back()\"></form>")
                            .append("               </body>\r\n")
                            .append("</html>\r\n");
        } finally {
            stopService();
        }
    }
}
