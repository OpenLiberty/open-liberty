package io.jaegertracing.api_v2;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * SamplingManager defines service for the remote sampler.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler",
    comments = "Source: sampling.proto")
public final class SamplingManagerGrpc {

  private SamplingManagerGrpc() {}

  public static final String SERVICE_NAME = "jaeger.api_v2.SamplingManager";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.jaegertracing.api_v2.Sampling.SamplingStrategyParameters,
      io.jaegertracing.api_v2.Sampling.SamplingStrategyResponse> getGetSamplingStrategyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetSamplingStrategy",
      requestType = io.jaegertracing.api_v2.Sampling.SamplingStrategyParameters.class,
      responseType = io.jaegertracing.api_v2.Sampling.SamplingStrategyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.jaegertracing.api_v2.Sampling.SamplingStrategyParameters,
      io.jaegertracing.api_v2.Sampling.SamplingStrategyResponse> getGetSamplingStrategyMethod() {
    io.grpc.MethodDescriptor<io.jaegertracing.api_v2.Sampling.SamplingStrategyParameters, io.jaegertracing.api_v2.Sampling.SamplingStrategyResponse> getGetSamplingStrategyMethod;
    if ((getGetSamplingStrategyMethod = SamplingManagerGrpc.getGetSamplingStrategyMethod) == null) {
      synchronized (SamplingManagerGrpc.class) {
        if ((getGetSamplingStrategyMethod = SamplingManagerGrpc.getGetSamplingStrategyMethod) == null) {
          SamplingManagerGrpc.getGetSamplingStrategyMethod = getGetSamplingStrategyMethod =
              io.grpc.MethodDescriptor.<io.jaegertracing.api_v2.Sampling.SamplingStrategyParameters, io.jaegertracing.api_v2.Sampling.SamplingStrategyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetSamplingStrategy"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.jaegertracing.api_v2.Sampling.SamplingStrategyParameters.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.jaegertracing.api_v2.Sampling.SamplingStrategyResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SamplingManagerMethodDescriptorSupplier("GetSamplingStrategy"))
              .build();
        }
      }
    }
    return getGetSamplingStrategyMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static SamplingManagerStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SamplingManagerStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SamplingManagerStub>() {
        @java.lang.Override
        public SamplingManagerStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SamplingManagerStub(channel, callOptions);
        }
      };
    return SamplingManagerStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static SamplingManagerBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SamplingManagerBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SamplingManagerBlockingStub>() {
        @java.lang.Override
        public SamplingManagerBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SamplingManagerBlockingStub(channel, callOptions);
        }
      };
    return SamplingManagerBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static SamplingManagerFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SamplingManagerFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SamplingManagerFutureStub>() {
        @java.lang.Override
        public SamplingManagerFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SamplingManagerFutureStub(channel, callOptions);
        }
      };
    return SamplingManagerFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * SamplingManager defines service for the remote sampler.
   * </pre>
   */
  public static abstract class SamplingManagerImplBase implements io.grpc.BindableService {

    /**
     */
    public void getSamplingStrategy(io.jaegertracing.api_v2.Sampling.SamplingStrategyParameters request,
        io.grpc.stub.StreamObserver<io.jaegertracing.api_v2.Sampling.SamplingStrategyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetSamplingStrategyMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getGetSamplingStrategyMethod(),
            io.grpc.stub.ServerCalls.asyncUnaryCall(
              new MethodHandlers<
                io.jaegertracing.api_v2.Sampling.SamplingStrategyParameters,
                io.jaegertracing.api_v2.Sampling.SamplingStrategyResponse>(
                  this, METHODID_GET_SAMPLING_STRATEGY)))
          .build();
    }
  }

  /**
   * <pre>
   * SamplingManager defines service for the remote sampler.
   * </pre>
   */
  public static final class SamplingManagerStub extends io.grpc.stub.AbstractAsyncStub<SamplingManagerStub> {
    private SamplingManagerStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SamplingManagerStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SamplingManagerStub(channel, callOptions);
    }

    /**
     */
    public void getSamplingStrategy(io.jaegertracing.api_v2.Sampling.SamplingStrategyParameters request,
        io.grpc.stub.StreamObserver<io.jaegertracing.api_v2.Sampling.SamplingStrategyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetSamplingStrategyMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * SamplingManager defines service for the remote sampler.
   * </pre>
   */
  public static final class SamplingManagerBlockingStub extends io.grpc.stub.AbstractBlockingStub<SamplingManagerBlockingStub> {
    private SamplingManagerBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SamplingManagerBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SamplingManagerBlockingStub(channel, callOptions);
    }

    /**
     */
    public io.jaegertracing.api_v2.Sampling.SamplingStrategyResponse getSamplingStrategy(io.jaegertracing.api_v2.Sampling.SamplingStrategyParameters request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetSamplingStrategyMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * SamplingManager defines service for the remote sampler.
   * </pre>
   */
  public static final class SamplingManagerFutureStub extends io.grpc.stub.AbstractFutureStub<SamplingManagerFutureStub> {
    private SamplingManagerFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SamplingManagerFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SamplingManagerFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.jaegertracing.api_v2.Sampling.SamplingStrategyResponse> getSamplingStrategy(
        io.jaegertracing.api_v2.Sampling.SamplingStrategyParameters request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetSamplingStrategyMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_SAMPLING_STRATEGY = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final SamplingManagerImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(SamplingManagerImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_SAMPLING_STRATEGY:
          serviceImpl.getSamplingStrategy((io.jaegertracing.api_v2.Sampling.SamplingStrategyParameters) request,
              (io.grpc.stub.StreamObserver<io.jaegertracing.api_v2.Sampling.SamplingStrategyResponse>) responseObserver);
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

  private static abstract class SamplingManagerBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    SamplingManagerBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.jaegertracing.api_v2.Sampling.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("SamplingManager");
    }
  }

  private static final class SamplingManagerFileDescriptorSupplier
      extends SamplingManagerBaseDescriptorSupplier {
    SamplingManagerFileDescriptorSupplier() {}
  }

  private static final class SamplingManagerMethodDescriptorSupplier
      extends SamplingManagerBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    SamplingManagerMethodDescriptorSupplier(String methodName) {
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
      synchronized (SamplingManagerGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new SamplingManagerFileDescriptorSupplier())
              .addMethod(getGetSamplingStrategyMethod())
              .build();
        }
      }
    }
    return result;
  }
}
