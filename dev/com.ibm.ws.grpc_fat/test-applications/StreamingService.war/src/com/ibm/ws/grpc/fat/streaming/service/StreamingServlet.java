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

package com.ibm.ws.grpc.fat.streaming.service;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.grpc.stub.StreamObserver;

/**
 * A simple gRPC streaming service
 */
@WebServlet(urlPatterns = { "/streaming" }, asyncSupported = true)
public class StreamingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static String responseString = "Response from Server: ";

    // implementation of the StreamingServiceImpl service
    public static final class StreamingServiceImpl extends StreamingServiceGrpc.StreamingServiceImplBase {

        // a no-arg constructor is required for Liberty to start this service automatically
        public StreamingServiceImpl() {
        }

        @Override
        public void serverStream(StreamRequest req, StreamObserver<StreamReply> responseObserver) {
            StreamReply reply = StreamReply.newBuilder().setMessage("Hello from ServerStream " + req.getMessage()).build();
            responseObserver.onNext(reply);
            try {
                Thread.sleep(200);
            } catch (Exception x) {
                // do nothing
            }
            StreamReply reply2 = StreamReply.newBuilder().setMessage(" 222222222222222222222 ").build();
            responseObserver.onNext(reply2);
            try {
                Thread.sleep(200);
            } catch (Exception x) {
                // do nothing
            }
            StreamReply reply3 = StreamReply.newBuilder().setMessage(" 333333333333333333333 ").build();
            responseObserver.onNext(reply3);

            responseObserver.onCompleted();
        }

        String lastClientMessage = "Nothing yet";

        @Override
        public StreamObserver<StreamRequest> clientStream(final StreamObserver<StreamReply> responseObserver) {

            // two way streaming, maybe return new StreamObserver<StreamRequest> requestObserver =
            //      new StreamObserver<StreamRequest>() {
            return new StreamObserver<StreamRequest>() {

                @Override
                public void onNext(StreamRequest request) {
                    String s = request.toString();
                    lastClientMessage = s;

                    s = "<br>...(( " + s + " onNext at server called at: " + System.currentTimeMillis() + " ))";
                    // limit string to first 200 characters
                    if (s.length() > 200) {
                        s = s.substring(0, 200);
                    }
                    // System.out.println(s);
                    responseString = responseString + s;
                }

                @Override
                public void onError(Throwable t) {
                }

                @Override
                public void onCompleted() {
                    String s = responseString + "...[[time response sent back to Client: " + System.currentTimeMillis() + "]]";

                    int maxStringLength = 32768 - lastClientMessage.length() - 1;
                    // limit response string to 32K, make sure the last message concatentated at the end
                    if (s.length() > maxStringLength) {
                        s = s.substring(0, maxStringLength);
                        s = s + lastClientMessage;
                    }
                    System.out.println(s);

                    StreamReply reply = StreamReply.newBuilder().setMessage(s).build();
                    responseObserver.onNext(reply);
                    responseObserver.onCompleted();

                }
            };
        }

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.getWriter().println("Hello from doGet of StreamingServlet");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.getWriter().println("Hello from doPost of StreamingServlet");
    }

    @Override
    public void destroy() {
        super.destroy();
    }
}