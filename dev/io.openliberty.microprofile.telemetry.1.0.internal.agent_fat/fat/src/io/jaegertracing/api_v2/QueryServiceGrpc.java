package io.jaegertracing.api_v2;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler",
    comments = "Source: query.proto")
public final class QueryServiceGrpc {

  private QueryServiceGrpc() {}

  public static final String SERVICE_NAME = "jaeger.api_v2.QueryService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.jaegertracing.api_v2.Query.GetTraceRequest,
      io.jaegertracing.api_v2.Query.SpansResponseChunk> getGetTraceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetTrace",
      requestType = io.jaegertracing.api_v2.Query.GetTraceRequest.class,
      responseType = io.jaegertracing.api_v2.Query.SpansResponseChunk.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<io.jaegertracing.api_v2.Query.GetTraceRequest,
      io.jaegertracing.api_v2.Query.SpansResponseChunk> getGetTraceMethod() {
    io.grpc.MethodDescriptor<io.jaegertracing.api_v2.Query.GetTraceRequest, io.jaegertracing.api_v2.Query.SpansResponseChunk> getGetTraceMethod;
    if ((getGetTraceMethod = QueryServiceGrpc.getGetTraceMethod) == null) {
      synchronized (QueryServiceGrpc.class) {
        if ((getGetTraceMethod = QueryServiceGrpc.getGetTraceMethod) == null) {
          QueryServiceGrpc.getGetTraceMethod = getGetTraceMethod =
              io.grpc.MethodDescriptor.<io.jaegertracing.api_v2.Query.GetTraceRequest, io.jaegertracing.api_v2.Query.SpansResponseChunk>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetTrace"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.jaegertracing.api_v2.Query.GetTraceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.jaegertracing.api_v2.Query.SpansResponseChunk.getDefaultInstance()))
              .setSchemaDescriptor(new QueryServiceMethodDescriptorSupplier("GetTrace"))
              .build();
        }
      }
    }
    return getGetTraceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.jaegertracing.api_v2.Query.ArchiveTraceRequest,
      io.jaegertracing.api_v2.Query.ArchiveTraceResponse> getArchiveTraceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ArchiveTrace",
      requestType = io.jaegertracing.api_v2.Query.ArchiveTraceRequest.class,
      responseType = io.jaegertracing.api_v2.Query.ArchiveTraceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.jaegertracing.api_v2.Query.ArchiveTraceRequest,
      io.jaegertracing.api_v2.Query.ArchiveTraceResponse> getArchiveTraceMethod() {
    io.grpc.MethodDescriptor<io.jaegertracing.api_v2.Query.ArchiveTraceRequest, io.jaegertracing.api_v2.Query.ArchiveTraceResponse> getArchiveTraceMethod;
    if ((getArchiveTraceMethod = QueryServiceGrpc.getArchiveTraceMethod) == null) {
      synchronized (QueryServiceGrpc.class) {
        if ((getArchiveTraceMethod = QueryServiceGrpc.getArchiveTraceMethod) == null) {
          QueryServiceGrpc.getArchiveTraceMethod = getArchiveTraceMethod =
              io.grpc.MethodDescriptor.<io.jaegertracing.api_v2.Query.ArchiveTraceRequest, io.jaegertracing.api_v2.Query.ArchiveTraceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ArchiveTrace"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.jaegertracing.api_v2.Query.ArchiveTraceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.jaegertracing.api_v2.Query.ArchiveTraceResponse.getDefaultInstance()))
              .setSchemaDescriptor(new QueryServiceMethodDescriptorSupplier("ArchiveTrace"))
              .build();
        }
      }
    }
    return getArchiveTraceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.jaegertracing.api_v2.Query.FindTracesRequest,
      io.jaegertracing.api_v2.Query.SpansResponseChunk> getFindTracesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "FindTraces",
      requestType = io.jaegertracing.api_v2.Query.FindTracesRequest.class,
      responseType = io.jaegertracing.api_v2.Query.SpansResponseChunk.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<io.jaegertracing.api_v2.Query.FindTracesRequest,
      io.jaegertracing.api_v2.Query.SpansResponseChunk> getFindTracesMethod() {
    io.grpc.MethodDescriptor<io.jaegertracing.api_v2.Query.FindTracesRequest, io.jaegertracing.api_v2.Query.SpansResponseChunk> getFindTracesMethod;
    if ((getFindTracesMethod = QueryServiceGrpc.getFindTracesMethod) == null) {
      synchronized (QueryServiceGrpc.class) {
        if ((getFindTracesMethod = QueryServiceGrpc.getFindTracesMethod) == null) {
          QueryServiceGrpc.getFindTracesMethod = getFindTracesMethod =
              io.grpc.MethodDescriptor.<io.jaegertracing.api_v2.Query.FindTracesRequest, io.jaegertracing.api_v2.Query.SpansResponseChunk>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "FindTraces"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.jaegertracing.api_v2.Query.FindTracesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.jaegertracing.api_v2.Query.SpansResponseChunk.getDefaultInstance()))
              .setSchemaDescriptor(new QueryServiceMethodDescriptorSupplier("FindTraces"))
              .build();
        }
      }
    }
    return getFindTracesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.jaegertracing.api_v2.Query.GetServicesRequest,
      io.jaegertracing.api_v2.Query.GetServicesResponse> getGetServicesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetServices",
      requestType = io.jaegertracing.api_v2.Query.GetServicesRequest.class,
      responseType = io.jaegertracing.api_v2.Query.GetServicesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.jaegertracing.api_v2.Query.GetServicesRequest,
      io.jaegertracing.api_v2.Query.GetServicesResponse> getGetServicesMethod() {
    io.grpc.MethodDescriptor<io.jaegertracing.api_v2.Query.GetServicesRequest, io.jaegertracing.api_v2.Query.GetServicesResponse> getGetServicesMethod;
    if ((getGetServicesMethod = QueryServiceGrpc.getGetServicesMethod) == null) {
      synchronized (QueryServiceGrpc.class) {
        if ((getGetServicesMethod = QueryServiceGrpc.getGetServicesMethod) == null) {
          QueryServiceGrpc.getGetServicesMethod = getGetServicesMethod =
              io.grpc.MethodDescriptor.<io.jaegertracing.api_v2.Query.GetServicesRequest, io.jaegertracing.api_v2.Query.GetServicesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetServices"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.jaegertracing.api_v2.Query.GetServicesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.jaegertracing.api_v2.Query.GetServicesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new QueryServiceMethodDescriptorSupplier("GetServices"))
              .build();
        }
      }
    }
    return getGetServicesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.jaegertracing.api_v2.Query.GetOperationsRequest,
      io.jaegertracing.api_v2.Query.GetOperationsResponse> getGetOperationsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetOperations",
      requestType = io.jaegertracing.api_v2.Query.GetOperationsRequest.class,
      responseType = io.jaegertracing.api_v2.Query.GetOperationsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.jaegertracing.api_v2.Query.GetOperationsRequest,
      io.jaegertracing.api_v2.Query.GetOperationsResponse> getGetOperationsMethod() {
    io.grpc.MethodDescriptor<io.jaegertracing.api_v2.Query.GetOperationsRequest, io.jaegertracing.api_v2.Query.GetOperationsResponse> getGetOperationsMethod;
    if ((getGetOperationsMethod = QueryServiceGrpc.getGetOperationsMethod) == null) {
      synchronized (QueryServiceGrpc.class) {
        if ((getGetOperationsMethod = QueryServiceGrpc.getGetOperationsMethod) == null) {
          QueryServiceGrpc.getGetOperationsMethod = getGetOperationsMethod =
              io.grpc.MethodDescriptor.<io.jaegertracing.api_v2.Query.GetOperationsRequest, io.jaegertracing.api_v2.Query.GetOperationsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetOperations"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.jaegertracing.api_v2.Query.GetOperationsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.jaegertracing.api_v2.Query.GetOperationsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new QueryServiceMethodDescriptorSupplier("GetOperations"))
              .build();
        }
      }
    }
    return getGetOperationsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.jaegertracing.api_v2.Query.GetDependenciesRequest,
      io.jaegertracing.api_v2.Query.GetDependenciesResponse> getGetDependenciesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetDependencies",
      requestType = io.jaegertracing.api_v2.Query.GetDependenciesRequest.class,
      responseType = io.jaegertracing.api_v2.Query.GetDependenciesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.jaegertracing.api_v2.Query.GetDependenciesRequest,
      io.jaegertracing.api_v2.Query.GetDependenciesResponse> getGetDependenciesMethod() {
    io.grpc.MethodDescriptor<io.jaegertracing.api_v2.Query.GetDependenciesRequest, io.jaegertracing.api_v2.Query.GetDependenciesResponse> getGetDependenciesMethod;
    if ((getGetDependenciesMethod = QueryServiceGrpc.getGetDependenciesMethod) == null) {
      synchronized (QueryServiceGrpc.class) {
        if ((getGetDependenciesMethod = QueryServiceGrpc.getGetDependenciesMethod) == null) {
          QueryServiceGrpc.getGetDependenciesMethod = getGetDependenciesMethod =
              io.grpc.MethodDescriptor.<io.jaegertracing.api_v2.Query.GetDependenciesRequest, io.jaegertracing.api_v2.Query.GetDependenciesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetDependencies"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.jaegertracing.api_v2.Query.GetDependenciesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.jaegertracing.api_v2.Query.GetDependenciesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new QueryServiceMethodDescriptorSupplier("GetDependencies"))
              .build();
        }
      }
    }
    return getGetDependenciesMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static QueryServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QueryServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QueryServiceStub>() {
        @java.lang.Override
        public QueryServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QueryServiceStub(channel, callOptions);
        }
      };
    return QueryServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static QueryServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QueryServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QueryServiceBlockingStub>() {
        @java.lang.Override
        public QueryServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QueryServiceBlockingStub(channel, callOptions);
        }
      };
    return QueryServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static QueryServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<QueryServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<QueryServiceFutureStub>() {
        @java.lang.Override
        public QueryServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new QueryServiceFutureStub(channel, callOptions);
        }
      };
    return QueryServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class QueryServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void getTrace(io.jaegertracing.api_v2.Query.GetTraceRequest request,
        io.grpc.stub.StreamObserver<io.jaegertracing.api_v2.Query.SpansResponseChunk> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetTraceMethod(), responseObserver);
    }

    /**
     */
    public void archiveTrace(io.jaegertracing.api_v2.Query.ArchiveTraceRequest request,
        io.grpc.stub.StreamObserver<io.jaegertracing.api_v2.Query.ArchiveTraceResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getArchiveTraceMethod(), responseObserver);
    }

    /**
     */
    public void findTraces(io.jaegertracing.api_v2.Query.FindTracesRequest request,
        io.grpc.stub.StreamObserver<io.jaegertracing.api_v2.Query.SpansResponseChunk> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getFindTracesMethod(), responseObserver);
    }

    /**
     */
    public void getServices(io.jaegertracing.api_v2.Query.GetServicesRequest request,
        io.grpc.stub.StreamObserver<io.jaegertracing.api_v2.Query.GetServicesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetServicesMethod(), responseObserver);
    }

    /**
     */
    public void getOperations(io.jaegertracing.api_v2.Query.GetOperationsRequest request,
        io.grpc.stub.StreamObserver<io.jaegertracing.api_v2.Query.GetOperationsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetOperationsMethod(), responseObserver);
    }

    /**
     */
    public void getDependencies(io.jaegertracing.api_v2.Query.GetDependenciesRequest request,
        io.grpc.stub.StreamObserver<io.jaegertracing.api_v2.Query.GetDependenciesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetDependenciesMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getGetTraceMethod(),
            io.grpc.stub.ServerCalls.asyncServerStreamingCall(
              new MethodHandlers<
                io.jaegertracing.api_v2.Query.GetTraceRequest,
                io.jaegertracing.api_v2.Query.SpansResponseChunk>(
                  this, METHODID_GET_TRACE)))
          .addMethod(
            getArchiveTraceMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                io.jaegertracing.api_v2.Query.ArchiveTraceRequest,
                io.jaegertracing.api_v2.Query.ArchiveTraceResponse>(
                  this, METHODID_ARCHIVE_TRACE)))
          .addMethod(
            getFindTracesMethod(),
            io.grpc.stub.ServerCalls.asyncServerStreamingCall(
              new MethodHandlers<
                io.jaegertracing.api_v2.Query.FindTracesRequest,
                io.jaegertracing.api_v2.Query.SpansResponseChunk>(
                  this, METHODID_FIND_TRACES)))
          .addMethod(
            getGetServicesMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                io.jaegertracing.api_v2.Query.GetServicesRequest,
                io.jaegertracing.api_v2.Query.GetServicesResponse>(
                  this, METHODID_GET_SERVICES)))
          .addMethod(
            getGetOperationsMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                io.jaegertracing.api_v2.Query.GetOperationsRequest,
                io.jaegertracing.api_v2.Query.GetOperationsResponse>(
                  this, METHODID_GET_OPERATIONS)))
          .addMethod(
            getGetDependenciesMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                io.jaegertracing.api_v2.Query.GetDependenciesRequest,
                io.jaegertracing.api_v2.Query.GetDependenciesResponse>(
                  this, METHODID_GET_DEPENDENCIES)))
          .build();
    }
  }

  /**
   */
  public static final class QueryServiceStub extends io.grpc.stub.AbstractAsyncStub<QueryServiceStub> {
    private QueryServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QueryServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QueryServiceStub(channel, callOptions);
    }

    /**
     */
    public void getTrace(io.jaegertracing.api_v2.Query.GetTraceRequest request,
        io.grpc.stub.StreamObserver<io.jaegertracing.api_v2.Query.SpansResponseChunk> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGetTraceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void archiveTrace(io.jaegertracing.api_v2.Query.ArchiveTraceRequest request,
        io.grpc.stub.StreamObserver<io.jaegertracing.api_v2.Query.ArchiveTraceResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getArchiveTraceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void findTraces(io.jaegertracing.api_v2.Query.FindTracesRequest request,
        io.grpc.stub.StreamObserver<io.jaegertracing.api_v2.Query.SpansResponseChunk> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getFindTracesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getServices(io.jaegertracing.api_v2.Query.GetServicesRequest request,
        io.grpc.stub.StreamObserver<io.jaegertracing.api_v2.Query.GetServicesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetServicesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getOperations(io.jaegertracing.api_v2.Query.GetOperationsRequest request,
        io.grpc.stub.StreamObserver<io.jaegertracing.api_v2.Query.GetOperationsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetOperationsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getDependencies(io.jaegertracing.api_v2.Query.GetDependenciesRequest request,
        io.grpc.stub.StreamObserver<io.jaegertracing.api_v2.Query.GetDependenciesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetDependenciesMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class QueryServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<QueryServiceBlockingStub> {
    private QueryServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QueryServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QueryServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public java.util.Iterator<io.jaegertracing.api_v2.Query.SpansResponseChunk> getTrace(
        io.jaegertracing.api_v2.Query.GetTraceRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGetTraceMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.jaegertracing.api_v2.Query.ArchiveTraceResponse archiveTrace(io.jaegertracing.api_v2.Query.ArchiveTraceRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getArchiveTraceMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<io.jaegertracing.api_v2.Query.SpansResponseChunk> findTraces(
        io.jaegertracing.api_v2.Query.FindTracesRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getFindTracesMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.jaegertracing.api_v2.Query.GetServicesResponse getServices(io.jaegertracing.api_v2.Query.GetServicesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetServicesMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.jaegertracing.api_v2.Query.GetOperationsResponse getOperations(io.jaegertracing.api_v2.Query.GetOperationsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetOperationsMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.jaegertracing.api_v2.Query.GetDependenciesResponse getDependencies(io.jaegertracing.api_v2.Query.GetDependenciesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetDependenciesMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class QueryServiceFutureStub extends io.grpc.stub.AbstractFutureStub<QueryServiceFutureStub> {
    private QueryServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected QueryServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new QueryServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.jaegertracing.api_v2.Query.ArchiveTraceResponse> archiveTrace(
        io.jaegertracing.api_v2.Query.ArchiveTraceRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getArchiveTraceMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.jaegertracing.api_v2.Query.GetServicesResponse> getServices(
        io.jaegertracing.api_v2.Query.GetServicesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetServicesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.jaegertracing.api_v2.Query.GetOperationsResponse> getOperations(
        io.jaegertracing.api_v2.Query.GetOperationsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetOperationsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.jaegertracing.api_v2.Query.GetDependenciesResponse> getDependencies(
        io.jaegertracing.api_v2.Query.GetDependenciesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetDependenciesMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_TRACE = 0;
  private static final int METHODID_ARCHIVE_TRACE = 1;
  private static final int METHODID_FIND_TRACES = 2;
  private static final int METHODID_GET_SERVICES = 3;
  private static final int METHODID_GET_OPERATIONS = 4;
  private static final int METHODID_GET_DEPENDENCIES = 5;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final QueryServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(QueryServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_TRACE:
          serviceImpl.getTrace((io.jaegertracing.api_v2.Query.GetTraceRequest) request,
              (io.grpc.stub.StreamObserver<io.jaegertracing.api_v2.Query.SpansResponseChunk>) responseObserver);
          break;
        case METHODID_ARCHIVE_TRACE:
          serviceImpl.archiveTrace((io.jaegertracing.api_v2.Query.ArchiveTraceRequest) request,
              (io.grpc.stub.StreamObserver<io.jaegertracing.api_v2.Query.ArchiveTraceResponse>) responseObserver);
          break;
        case METHODID_FIND_TRACES:
          serviceImpl.findTraces((io.jaegertracing.api_v2.Query.FindTracesRequest) request,
              (io.grpc.stub.StreamObserver<io.jaegertracing.api_v2.Query.SpansResponseChunk>) responseObserver);
          break;
        case METHODID_GET_SERVICES:
          serviceImpl.getServices((io.jaegertracing.api_v2.Query.GetServicesRequest) request,
              (io.grpc.stub.StreamObserver<io.jaegertracing.api_v2.Query.GetServicesResponse>) responseObserver);
          break;
        case METHODID_GET_OPERATIONS:
          serviceImpl.getOperations((io.jaegertracing.api_v2.Query.GetOperationsRequest) request,
              (io.grpc.stub.StreamObserver<io.jaegertracing.api_v2.Query.GetOperationsResponse>) responseObserver);
          break;
        case METHODID_GET_DEPENDENCIES:
          serviceImpl.getDependencies((io.jaegertracing.api_v2.Query.GetDependenciesRequest) request,
              (io.grpc.stub.StreamObserver<io.jaegertracing.api_v2.Query.GetDependenciesResponse>) responseObserver);
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
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class QueryServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    QueryServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.jaegertracing.api_v2.Query.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("QueryService");
    }
  }

  private static final class QueryServiceFileDescriptorSupplier
      extends QueryServiceBaseDescriptorSupplier {
    QueryServiceFileDescriptorSupplier() {}
  }

  private static final class QueryServiceMethodDescriptorSupplier
      extends QueryServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    QueryServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (QueryServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new QueryServiceFileDescriptorSupplier())
              .addMethod(getGetTraceMethod())
              .addMethod(getArchiveTraceMethod())
              .addMethod(getFindTracesMethod())
              .addMethod(getGetServicesMethod())
              .addMethod(getGetOperationsMethod())
              .addMethod(getGetDependenciesMethod())
              .build();
        }
      }
    }
    return result;
  }
}
