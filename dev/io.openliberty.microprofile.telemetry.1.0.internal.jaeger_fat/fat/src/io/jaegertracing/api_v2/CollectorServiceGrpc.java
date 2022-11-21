package io.jaegertracing.api_v2;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler",
    comments = "Source: collector.proto")
public final class CollectorServiceGrpc {

  private CollectorServiceGrpc() {}

  public static final String SERVICE_NAME = "jaeger.api_v2.CollectorService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.jaegertracing.api_v2.Collector.PostSpansRequest,
      io.jaegertracing.api_v2.Collector.PostSpansResponse> getPostSpansMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PostSpans",
      requestType = io.jaegertracing.api_v2.Collector.PostSpansRequest.class,
      responseType = io.jaegertracing.api_v2.Collector.PostSpansResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.jaegertracing.api_v2.Collector.PostSpansRequest,
      io.jaegertracing.api_v2.Collector.PostSpansResponse> getPostSpansMethod() {
    io.grpc.MethodDescriptor<io.jaegertracing.api_v2.Collector.PostSpansRequest, io.jaegertracing.api_v2.Collector.PostSpansResponse> getPostSpansMethod;
    if ((getPostSpansMethod = CollectorServiceGrpc.getPostSpansMethod) == null) {
      synchronized (CollectorServiceGrpc.class) {
        if ((getPostSpansMethod = CollectorServiceGrpc.getPostSpansMethod) == null) {
          CollectorServiceGrpc.getPostSpansMethod = getPostSpansMethod =
              io.grpc.MethodDescriptor.<io.jaegertracing.api_v2.Collector.PostSpansRequest, io.jaegertracing.api_v2.Collector.PostSpansResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PostSpans"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.jaegertracing.api_v2.Collector.PostSpansRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.jaegertracing.api_v2.Collector.PostSpansResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CollectorServiceMethodDescriptorSupplier("PostSpans"))
              .build();
        }
      }
    }
    return getPostSpansMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static CollectorServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CollectorServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CollectorServiceStub>() {
        @java.lang.Override
        public CollectorServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CollectorServiceStub(channel, callOptions);
        }
      };
    return CollectorServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static CollectorServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CollectorServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CollectorServiceBlockingStub>() {
        @java.lang.Override
        public CollectorServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CollectorServiceBlockingStub(channel, callOptions);
        }
      };
    return CollectorServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static CollectorServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CollectorServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CollectorServiceFutureStub>() {
        @java.lang.Override
        public CollectorServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CollectorServiceFutureStub(channel, callOptions);
        }
      };
    return CollectorServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class CollectorServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void postSpans(io.jaegertracing.api_v2.Collector.PostSpansRequest request,
        io.grpc.stub.StreamObserver<io.jaegertracing.api_v2.Collector.PostSpansResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPostSpansMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getPostSpansMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                io.jaegertracing.api_v2.Collector.PostSpansRequest,
                io.jaegertracing.api_v2.Collector.PostSpansResponse>(
                  this, METHODID_POST_SPANS)))
          .build();
    }
  }

  /**
   */
  public static final class CollectorServiceStub extends io.grpc.stub.AbstractAsyncStub<CollectorServiceStub> {
    private CollectorServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CollectorServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CollectorServiceStub(channel, callOptions);
    }

    /**
     */
    public void postSpans(io.jaegertracing.api_v2.Collector.PostSpansRequest request,
        io.grpc.stub.StreamObserver<io.jaegertracing.api_v2.Collector.PostSpansResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPostSpansMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class CollectorServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<CollectorServiceBlockingStub> {
    private CollectorServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CollectorServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CollectorServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public io.jaegertracing.api_v2.Collector.PostSpansResponse postSpans(io.jaegertracing.api_v2.Collector.PostSpansRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPostSpansMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class CollectorServiceFutureStub extends io.grpc.stub.AbstractFutureStub<CollectorServiceFutureStub> {
    private CollectorServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CollectorServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CollectorServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.jaegertracing.api_v2.Collector.PostSpansResponse> postSpans(
        io.jaegertracing.api_v2.Collector.PostSpansRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPostSpansMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_POST_SPANS = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final CollectorServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(CollectorServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_POST_SPANS:
          serviceImpl.postSpans((io.jaegertracing.api_v2.Collector.PostSpansRequest) request,
              (io.grpc.stub.StreamObserver<io.jaegertracing.api_v2.Collector.PostSpansResponse>) responseObserver);
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

  private static abstract class CollectorServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    CollectorServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.jaegertracing.api_v2.Collector.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("CollectorService");
    }
  }

  private static final class CollectorServiceFileDescriptorSupplier
      extends CollectorServiceBaseDescriptorSupplier {
    CollectorServiceFileDescriptorSupplier() {}
  }

  private static final class CollectorServiceMethodDescriptorSupplier
      extends CollectorServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    CollectorServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (CollectorServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new CollectorServiceFileDescriptorSupplier())
              .addMethod(getPostSpansMethod())
              .build();
        }
      }
    }
    return result;
  }
}
