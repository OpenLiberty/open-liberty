package com.ibm.test.g3store.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * The service definition.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.36.1)",
    comments = "Source: ProducerStore.proto")
public final class AppProducerServiceGrpc {

  private AppProducerServiceGrpc() {}

  public static final String SERVICE_NAME = "test.g3store.grpc.AppProducerService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.AppRequest,
      com.ibm.test.g3store.grpc.AppResponse> getCreateAppMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "createApp",
      requestType = com.ibm.test.g3store.grpc.AppRequest.class,
      responseType = com.ibm.test.g3store.grpc.AppResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.AppRequest,
      com.ibm.test.g3store.grpc.AppResponse> getCreateAppMethod() {
    io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.AppRequest, com.ibm.test.g3store.grpc.AppResponse> getCreateAppMethod;
    if ((getCreateAppMethod = AppProducerServiceGrpc.getCreateAppMethod) == null) {
      synchronized (AppProducerServiceGrpc.class) {
        if ((getCreateAppMethod = AppProducerServiceGrpc.getCreateAppMethod) == null) {
          AppProducerServiceGrpc.getCreateAppMethod = getCreateAppMethod =
              io.grpc.MethodDescriptor.<com.ibm.test.g3store.grpc.AppRequest, com.ibm.test.g3store.grpc.AppResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "createApp"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.AppRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.AppResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AppProducerServiceMethodDescriptorSupplier("createApp"))
              .build();
        }
      }
    }
    return getCreateAppMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.DeleteRequest,
      com.ibm.test.g3store.grpc.DeleteResponse> getDeleteAppMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "deleteApp",
      requestType = com.ibm.test.g3store.grpc.DeleteRequest.class,
      responseType = com.ibm.test.g3store.grpc.DeleteResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.DeleteRequest,
      com.ibm.test.g3store.grpc.DeleteResponse> getDeleteAppMethod() {
    io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.DeleteRequest, com.ibm.test.g3store.grpc.DeleteResponse> getDeleteAppMethod;
    if ((getDeleteAppMethod = AppProducerServiceGrpc.getDeleteAppMethod) == null) {
      synchronized (AppProducerServiceGrpc.class) {
        if ((getDeleteAppMethod = AppProducerServiceGrpc.getDeleteAppMethod) == null) {
          AppProducerServiceGrpc.getDeleteAppMethod = getDeleteAppMethod =
              io.grpc.MethodDescriptor.<com.ibm.test.g3store.grpc.DeleteRequest, com.ibm.test.g3store.grpc.DeleteResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "deleteApp"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.DeleteRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.DeleteResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AppProducerServiceMethodDescriptorSupplier("deleteApp"))
              .build();
        }
      }
    }
    return getDeleteAppMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.ibm.test.g3store.grpc.DeleteResponse> getDeleteAllAppsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "deleteAllApps",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.ibm.test.g3store.grpc.DeleteResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.ibm.test.g3store.grpc.DeleteResponse> getDeleteAllAppsMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.ibm.test.g3store.grpc.DeleteResponse> getDeleteAllAppsMethod;
    if ((getDeleteAllAppsMethod = AppProducerServiceGrpc.getDeleteAllAppsMethod) == null) {
      synchronized (AppProducerServiceGrpc.class) {
        if ((getDeleteAllAppsMethod = AppProducerServiceGrpc.getDeleteAllAppsMethod) == null) {
          AppProducerServiceGrpc.getDeleteAllAppsMethod = getDeleteAllAppsMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.ibm.test.g3store.grpc.DeleteResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "deleteAllApps"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.DeleteResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AppProducerServiceMethodDescriptorSupplier("deleteAllApps"))
              .build();
        }
      }
    }
    return getDeleteAllAppsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.AppRequest,
      com.ibm.test.g3store.grpc.MultiCreateResponse> getCreateAppsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "createApps",
      requestType = com.ibm.test.g3store.grpc.AppRequest.class,
      responseType = com.ibm.test.g3store.grpc.MultiCreateResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
  public static io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.AppRequest,
      com.ibm.test.g3store.grpc.MultiCreateResponse> getCreateAppsMethod() {
    io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.AppRequest, com.ibm.test.g3store.grpc.MultiCreateResponse> getCreateAppsMethod;
    if ((getCreateAppsMethod = AppProducerServiceGrpc.getCreateAppsMethod) == null) {
      synchronized (AppProducerServiceGrpc.class) {
        if ((getCreateAppsMethod = AppProducerServiceGrpc.getCreateAppsMethod) == null) {
          AppProducerServiceGrpc.getCreateAppsMethod = getCreateAppsMethod =
              io.grpc.MethodDescriptor.<com.ibm.test.g3store.grpc.AppRequest, com.ibm.test.g3store.grpc.MultiCreateResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "createApps"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.AppRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.MultiCreateResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AppProducerServiceMethodDescriptorSupplier("createApps"))
              .build();
        }
      }
    }
    return getCreateAppsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.StreamRequestA,
      com.ibm.test.g3store.grpc.StreamReplyA> getClientStreamAMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "clientStreamA",
      requestType = com.ibm.test.g3store.grpc.StreamRequestA.class,
      responseType = com.ibm.test.g3store.grpc.StreamReplyA.class,
      methodType = io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
  public static io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.StreamRequestA,
      com.ibm.test.g3store.grpc.StreamReplyA> getClientStreamAMethod() {
    io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.StreamRequestA, com.ibm.test.g3store.grpc.StreamReplyA> getClientStreamAMethod;
    if ((getClientStreamAMethod = AppProducerServiceGrpc.getClientStreamAMethod) == null) {
      synchronized (AppProducerServiceGrpc.class) {
        if ((getClientStreamAMethod = AppProducerServiceGrpc.getClientStreamAMethod) == null) {
          AppProducerServiceGrpc.getClientStreamAMethod = getClientStreamAMethod =
              io.grpc.MethodDescriptor.<com.ibm.test.g3store.grpc.StreamRequestA, com.ibm.test.g3store.grpc.StreamReplyA>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "clientStreamA"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.StreamRequestA.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.StreamReplyA.getDefaultInstance()))
              .setSchemaDescriptor(new AppProducerServiceMethodDescriptorSupplier("clientStreamA"))
              .build();
        }
      }
    }
    return getClientStreamAMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.StreamRequestA,
      com.ibm.test.g3store.grpc.StreamReplyA> getServerStreamAMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "serverStreamA",
      requestType = com.ibm.test.g3store.grpc.StreamRequestA.class,
      responseType = com.ibm.test.g3store.grpc.StreamReplyA.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.StreamRequestA,
      com.ibm.test.g3store.grpc.StreamReplyA> getServerStreamAMethod() {
    io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.StreamRequestA, com.ibm.test.g3store.grpc.StreamReplyA> getServerStreamAMethod;
    if ((getServerStreamAMethod = AppProducerServiceGrpc.getServerStreamAMethod) == null) {
      synchronized (AppProducerServiceGrpc.class) {
        if ((getServerStreamAMethod = AppProducerServiceGrpc.getServerStreamAMethod) == null) {
          AppProducerServiceGrpc.getServerStreamAMethod = getServerStreamAMethod =
              io.grpc.MethodDescriptor.<com.ibm.test.g3store.grpc.StreamRequestA, com.ibm.test.g3store.grpc.StreamReplyA>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "serverStreamA"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.StreamRequestA.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.StreamReplyA.getDefaultInstance()))
              .setSchemaDescriptor(new AppProducerServiceMethodDescriptorSupplier("serverStreamA"))
              .build();
        }
      }
    }
    return getServerStreamAMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.StreamRequestA,
      com.ibm.test.g3store.grpc.StreamReplyA> getTwoWayStreamAMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "twoWayStreamA",
      requestType = com.ibm.test.g3store.grpc.StreamRequestA.class,
      responseType = com.ibm.test.g3store.grpc.StreamReplyA.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.StreamRequestA,
      com.ibm.test.g3store.grpc.StreamReplyA> getTwoWayStreamAMethod() {
    io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.StreamRequestA, com.ibm.test.g3store.grpc.StreamReplyA> getTwoWayStreamAMethod;
    if ((getTwoWayStreamAMethod = AppProducerServiceGrpc.getTwoWayStreamAMethod) == null) {
      synchronized (AppProducerServiceGrpc.class) {
        if ((getTwoWayStreamAMethod = AppProducerServiceGrpc.getTwoWayStreamAMethod) == null) {
          AppProducerServiceGrpc.getTwoWayStreamAMethod = getTwoWayStreamAMethod =
              io.grpc.MethodDescriptor.<com.ibm.test.g3store.grpc.StreamRequestA, com.ibm.test.g3store.grpc.StreamReplyA>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "twoWayStreamA"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.StreamRequestA.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.StreamReplyA.getDefaultInstance()))
              .setSchemaDescriptor(new AppProducerServiceMethodDescriptorSupplier("twoWayStreamA"))
              .build();
        }
      }
    }
    return getTwoWayStreamAMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.StreamRequestA,
      com.ibm.test.g3store.grpc.StreamReplyA> getTwoWayStreamAsyncThreadMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "twoWayStreamAsyncThread",
      requestType = com.ibm.test.g3store.grpc.StreamRequestA.class,
      responseType = com.ibm.test.g3store.grpc.StreamReplyA.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.StreamRequestA,
      com.ibm.test.g3store.grpc.StreamReplyA> getTwoWayStreamAsyncThreadMethod() {
    io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.StreamRequestA, com.ibm.test.g3store.grpc.StreamReplyA> getTwoWayStreamAsyncThreadMethod;
    if ((getTwoWayStreamAsyncThreadMethod = AppProducerServiceGrpc.getTwoWayStreamAsyncThreadMethod) == null) {
      synchronized (AppProducerServiceGrpc.class) {
        if ((getTwoWayStreamAsyncThreadMethod = AppProducerServiceGrpc.getTwoWayStreamAsyncThreadMethod) == null) {
          AppProducerServiceGrpc.getTwoWayStreamAsyncThreadMethod = getTwoWayStreamAsyncThreadMethod =
              io.grpc.MethodDescriptor.<com.ibm.test.g3store.grpc.StreamRequestA, com.ibm.test.g3store.grpc.StreamReplyA>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "twoWayStreamAsyncThread"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.StreamRequestA.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.StreamReplyA.getDefaultInstance()))
              .setSchemaDescriptor(new AppProducerServiceMethodDescriptorSupplier("twoWayStreamAsyncThread"))
              .build();
        }
      }
    }
    return getTwoWayStreamAsyncThreadMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static AppProducerServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AppProducerServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AppProducerServiceStub>() {
        @java.lang.Override
        public AppProducerServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AppProducerServiceStub(channel, callOptions);
        }
      };
    return AppProducerServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static AppProducerServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AppProducerServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AppProducerServiceBlockingStub>() {
        @java.lang.Override
        public AppProducerServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AppProducerServiceBlockingStub(channel, callOptions);
        }
      };
    return AppProducerServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static AppProducerServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AppProducerServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AppProducerServiceFutureStub>() {
        @java.lang.Override
        public AppProducerServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AppProducerServiceFutureStub(channel, callOptions);
        }
      };
    return AppProducerServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * The service definition.
   * </pre>
   */
  public static abstract class AppProducerServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     *This will be unary
     *This will not use security
     *This will add the db or dynacache with one entry
     * </pre>
     */
    public void createApp(com.ibm.test.g3store.grpc.AppRequest request,
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.AppResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateAppMethod(), responseObserver);
    }

    /**
     * <pre>
     *This will be unary
     *This will use Baic Auth
     *This will delete the db or dynacache with one entry
     * </pre>
     */
    public void deleteApp(com.ibm.test.g3store.grpc.DeleteRequest request,
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.DeleteResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteAppMethod(), responseObserver);
    }

    /**
     * <pre>
     *This will be server streaming
     *Basic auth
     *This will delete the apps , the server will respond each success or failure of each delete with the app name.
     * </pre>
     */
    public void deleteAllApps(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.DeleteResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteAllAppsMethod(), responseObserver);
    }

    /**
     * <pre>
     *This will be client streaming
     *This will be run from the client to test and populate multiple entries, no REST
     *No security
     *This will be update the db or dynacache with multiple entry
     * </pre>
     */
    public io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.AppRequest> createApps(
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.MultiCreateResponse> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getCreateAppsMethod(), responseObserver);
    }

    /**
     * <pre>
     *Client will stream a changing message over and over to the server.  Size and number of messages streamed
     *can be change in the client code.
     *At the end the server responses with a message containing some of what was streamed for verification
     *No security
     * </pre>
     */
    public io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.StreamRequestA> clientStreamA(
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.StreamReplyA> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getClientStreamAMethod(), responseObserver);
    }

    /**
     */
    public void serverStreamA(com.ibm.test.g3store.grpc.StreamRequestA request,
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.StreamReplyA> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getServerStreamAMethod(), responseObserver);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.StreamRequestA> twoWayStreamA(
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.StreamReplyA> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getTwoWayStreamAMethod(), responseObserver);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.StreamRequestA> twoWayStreamAsyncThread(
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.StreamReplyA> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getTwoWayStreamAsyncThreadMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getCreateAppMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.ibm.test.g3store.grpc.AppRequest,
                com.ibm.test.g3store.grpc.AppResponse>(
                  this, METHODID_CREATE_APP)))
          .addMethod(
            getDeleteAppMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                com.ibm.test.g3store.grpc.DeleteRequest,
                com.ibm.test.g3store.grpc.DeleteResponse>(
                  this, METHODID_DELETE_APP)))
          .addMethod(
            getDeleteAllAppsMethod(),
            io.grpc.stub.ServerCalls.asyncServerStreamingCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.ibm.test.g3store.grpc.DeleteResponse>(
                  this, METHODID_DELETE_ALL_APPS)))
          .addMethod(
            getCreateAppsMethod(),
            io.grpc.stub.ServerCalls.asyncClientStreamingCall(
              new MethodHandlers<
                com.ibm.test.g3store.grpc.AppRequest,
                com.ibm.test.g3store.grpc.MultiCreateResponse>(
                  this, METHODID_CREATE_APPS)))
          .addMethod(
            getClientStreamAMethod(),
            io.grpc.stub.ServerCalls.asyncClientStreamingCall(
              new MethodHandlers<
                com.ibm.test.g3store.grpc.StreamRequestA,
                com.ibm.test.g3store.grpc.StreamReplyA>(
                  this, METHODID_CLIENT_STREAM_A)))
          .addMethod(
            getServerStreamAMethod(),
            io.grpc.stub.ServerCalls.asyncServerStreamingCall(
              new MethodHandlers<
                com.ibm.test.g3store.grpc.StreamRequestA,
                com.ibm.test.g3store.grpc.StreamReplyA>(
                  this, METHODID_SERVER_STREAM_A)))
          .addMethod(
            getTwoWayStreamAMethod(),
            io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
              new MethodHandlers<
                com.ibm.test.g3store.grpc.StreamRequestA,
                com.ibm.test.g3store.grpc.StreamReplyA>(
                  this, METHODID_TWO_WAY_STREAM_A)))
          .addMethod(
            getTwoWayStreamAsyncThreadMethod(),
            io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
              new MethodHandlers<
                com.ibm.test.g3store.grpc.StreamRequestA,
                com.ibm.test.g3store.grpc.StreamReplyA>(
                  this, METHODID_TWO_WAY_STREAM_ASYNC_THREAD)))
          .build();
    }
  }

  /**
   * <pre>
   * The service definition.
   * </pre>
   */
  public static final class AppProducerServiceStub extends io.grpc.stub.AbstractAsyncStub<AppProducerServiceStub> {
    private AppProducerServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AppProducerServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AppProducerServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     *This will be unary
     *This will not use security
     *This will add the db or dynacache with one entry
     * </pre>
     */
    public void createApp(com.ibm.test.g3store.grpc.AppRequest request,
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.AppResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateAppMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *This will be unary
     *This will use Baic Auth
     *This will delete the db or dynacache with one entry
     * </pre>
     */
    public void deleteApp(com.ibm.test.g3store.grpc.DeleteRequest request,
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.DeleteResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteAppMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *This will be server streaming
     *Basic auth
     *This will delete the apps , the server will respond each success or failure of each delete with the app name.
     * </pre>
     */
    public void deleteAllApps(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.DeleteResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getDeleteAllAppsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *This will be client streaming
     *This will be run from the client to test and populate multiple entries, no REST
     *No security
     *This will be update the db or dynacache with multiple entry
     * </pre>
     */
    public io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.AppRequest> createApps(
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.MultiCreateResponse> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncClientStreamingCall(
          getChannel().newCall(getCreateAppsMethod(), getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     *Client will stream a changing message over and over to the server.  Size and number of messages streamed
     *can be change in the client code.
     *At the end the server responses with a message containing some of what was streamed for verification
     *No security
     * </pre>
     */
    public io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.StreamRequestA> clientStreamA(
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.StreamReplyA> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncClientStreamingCall(
          getChannel().newCall(getClientStreamAMethod(), getCallOptions()), responseObserver);
    }

    /**
     */
    public void serverStreamA(com.ibm.test.g3store.grpc.StreamRequestA request,
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.StreamReplyA> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getServerStreamAMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.StreamRequestA> twoWayStreamA(
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.StreamReplyA> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
          getChannel().newCall(getTwoWayStreamAMethod(), getCallOptions()), responseObserver);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.StreamRequestA> twoWayStreamAsyncThread(
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.StreamReplyA> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
          getChannel().newCall(getTwoWayStreamAsyncThreadMethod(), getCallOptions()), responseObserver);
    }
  }

  /**
   * <pre>
   * The service definition.
   * </pre>
   */
  public static final class AppProducerServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<AppProducerServiceBlockingStub> {
    private AppProducerServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AppProducerServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AppProducerServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     *This will be unary
     *This will not use security
     *This will add the db or dynacache with one entry
     * </pre>
     */
    public com.ibm.test.g3store.grpc.AppResponse createApp(com.ibm.test.g3store.grpc.AppRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateAppMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *This will be unary
     *This will use Baic Auth
     *This will delete the db or dynacache with one entry
     * </pre>
     */
    public com.ibm.test.g3store.grpc.DeleteResponse deleteApp(com.ibm.test.g3store.grpc.DeleteRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteAppMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *This will be server streaming
     *Basic auth
     *This will delete the apps , the server will respond each success or failure of each delete with the app name.
     * </pre>
     */
    public java.util.Iterator<com.ibm.test.g3store.grpc.DeleteResponse> deleteAllApps(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getDeleteAllAppsMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<com.ibm.test.g3store.grpc.StreamReplyA> serverStreamA(
        com.ibm.test.g3store.grpc.StreamRequestA request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getServerStreamAMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * The service definition.
   * </pre>
   */
  public static final class AppProducerServiceFutureStub extends io.grpc.stub.AbstractFutureStub<AppProducerServiceFutureStub> {
    private AppProducerServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AppProducerServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AppProducerServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     *This will be unary
     *This will not use security
     *This will add the db or dynacache with one entry
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ibm.test.g3store.grpc.AppResponse> createApp(
        com.ibm.test.g3store.grpc.AppRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateAppMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *This will be unary
     *This will use Baic Auth
     *This will delete the db or dynacache with one entry
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ibm.test.g3store.grpc.DeleteResponse> deleteApp(
        com.ibm.test.g3store.grpc.DeleteRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteAppMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE_APP = 0;
  private static final int METHODID_DELETE_APP = 1;
  private static final int METHODID_DELETE_ALL_APPS = 2;
  private static final int METHODID_SERVER_STREAM_A = 3;
  private static final int METHODID_CREATE_APPS = 4;
  private static final int METHODID_CLIENT_STREAM_A = 5;
  private static final int METHODID_TWO_WAY_STREAM_A = 6;
  private static final int METHODID_TWO_WAY_STREAM_ASYNC_THREAD = 7;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AppProducerServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(AppProducerServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CREATE_APP:
          serviceImpl.createApp((com.ibm.test.g3store.grpc.AppRequest) request,
              (io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.AppResponse>) responseObserver);
          break;
        case METHODID_DELETE_APP:
          serviceImpl.deleteApp((com.ibm.test.g3store.grpc.DeleteRequest) request,
              (io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.DeleteResponse>) responseObserver);
          break;
        case METHODID_DELETE_ALL_APPS:
          serviceImpl.deleteAllApps((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.DeleteResponse>) responseObserver);
          break;
        case METHODID_SERVER_STREAM_A:
          serviceImpl.serverStreamA((com.ibm.test.g3store.grpc.StreamRequestA) request,
              (io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.StreamReplyA>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CREATE_APPS:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.createApps(
              (io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.MultiCreateResponse>) responseObserver);
        case METHODID_CLIENT_STREAM_A:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.clientStreamA(
              (io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.StreamReplyA>) responseObserver);
        case METHODID_TWO_WAY_STREAM_A:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.twoWayStreamA(
              (io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.StreamReplyA>) responseObserver);
        case METHODID_TWO_WAY_STREAM_ASYNC_THREAD:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.twoWayStreamAsyncThread(
              (io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.StreamReplyA>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class AppProducerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    AppProducerServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.ibm.test.g3store.grpc.AppProducerProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("AppProducerService");
    }
  }

  private static final class AppProducerServiceFileDescriptorSupplier
      extends AppProducerServiceBaseDescriptorSupplier {
    AppProducerServiceFileDescriptorSupplier() {}
  }

  private static final class AppProducerServiceMethodDescriptorSupplier
      extends AppProducerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    AppProducerServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (AppProducerServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new AppProducerServiceFileDescriptorSupplier())
              .addMethod(getCreateAppMethod())
              .addMethod(getDeleteAppMethod())
              .addMethod(getDeleteAllAppsMethod())
              .addMethod(getCreateAppsMethod())
              .addMethod(getClientStreamAMethod())
              .addMethod(getServerStreamAMethod())
              .addMethod(getTwoWayStreamAMethod())
              .addMethod(getTwoWayStreamAsyncThreadMethod())
              .build();
        }
      }
    }
    return result;
  }
}
