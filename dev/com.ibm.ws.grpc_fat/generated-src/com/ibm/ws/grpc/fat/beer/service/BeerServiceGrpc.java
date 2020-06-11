package com.ibm.ws.grpc.fat.beer.service;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 * <pre>
 * The beer service definition.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.28.1)",
    comments = "Source: Beer.proto")
public final class BeerServiceGrpc {

  private BeerServiceGrpc() {}

  public static final String SERVICE_NAME = "beer.BeerService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.ibm.ws.grpc.fat.beer.service.Beer,
      com.ibm.ws.grpc.fat.beer.service.BeerResponse> getAddBeerMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AddBeer",
      requestType = com.ibm.ws.grpc.fat.beer.service.Beer.class,
      responseType = com.ibm.ws.grpc.fat.beer.service.BeerResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.ibm.ws.grpc.fat.beer.service.Beer,
      com.ibm.ws.grpc.fat.beer.service.BeerResponse> getAddBeerMethod() {
    io.grpc.MethodDescriptor<com.ibm.ws.grpc.fat.beer.service.Beer, com.ibm.ws.grpc.fat.beer.service.BeerResponse> getAddBeerMethod;
    if ((getAddBeerMethod = BeerServiceGrpc.getAddBeerMethod) == null) {
      synchronized (BeerServiceGrpc.class) {
        if ((getAddBeerMethod = BeerServiceGrpc.getAddBeerMethod) == null) {
          BeerServiceGrpc.getAddBeerMethod = getAddBeerMethod =
              io.grpc.MethodDescriptor.<com.ibm.ws.grpc.fat.beer.service.Beer, com.ibm.ws.grpc.fat.beer.service.BeerResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AddBeer"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.ws.grpc.fat.beer.service.Beer.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.ws.grpc.fat.beer.service.BeerResponse.getDefaultInstance()))
              .setSchemaDescriptor(new BeerServiceMethodDescriptorSupplier("AddBeer"))
              .build();
        }
      }
    }
    return getAddBeerMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.ibm.ws.grpc.fat.beer.service.Beer,
      com.ibm.ws.grpc.fat.beer.service.BeerResponse> getDeleteBeerMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteBeer",
      requestType = com.ibm.ws.grpc.fat.beer.service.Beer.class,
      responseType = com.ibm.ws.grpc.fat.beer.service.BeerResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.ibm.ws.grpc.fat.beer.service.Beer,
      com.ibm.ws.grpc.fat.beer.service.BeerResponse> getDeleteBeerMethod() {
    io.grpc.MethodDescriptor<com.ibm.ws.grpc.fat.beer.service.Beer, com.ibm.ws.grpc.fat.beer.service.BeerResponse> getDeleteBeerMethod;
    if ((getDeleteBeerMethod = BeerServiceGrpc.getDeleteBeerMethod) == null) {
      synchronized (BeerServiceGrpc.class) {
        if ((getDeleteBeerMethod = BeerServiceGrpc.getDeleteBeerMethod) == null) {
          BeerServiceGrpc.getDeleteBeerMethod = getDeleteBeerMethod =
              io.grpc.MethodDescriptor.<com.ibm.ws.grpc.fat.beer.service.Beer, com.ibm.ws.grpc.fat.beer.service.BeerResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteBeer"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.ws.grpc.fat.beer.service.Beer.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.ws.grpc.fat.beer.service.BeerResponse.getDefaultInstance()))
              .setSchemaDescriptor(new BeerServiceMethodDescriptorSupplier("DeleteBeer"))
              .build();
        }
      }
    }
    return getDeleteBeerMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.ibm.ws.grpc.fat.beer.service.RequestedBeerType,
      com.ibm.ws.grpc.fat.beer.service.Beer> getGetBestBeerMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetBestBeer",
      requestType = com.ibm.ws.grpc.fat.beer.service.RequestedBeerType.class,
      responseType = com.ibm.ws.grpc.fat.beer.service.Beer.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.ibm.ws.grpc.fat.beer.service.RequestedBeerType,
      com.ibm.ws.grpc.fat.beer.service.Beer> getGetBestBeerMethod() {
    io.grpc.MethodDescriptor<com.ibm.ws.grpc.fat.beer.service.RequestedBeerType, com.ibm.ws.grpc.fat.beer.service.Beer> getGetBestBeerMethod;
    if ((getGetBestBeerMethod = BeerServiceGrpc.getGetBestBeerMethod) == null) {
      synchronized (BeerServiceGrpc.class) {
        if ((getGetBestBeerMethod = BeerServiceGrpc.getGetBestBeerMethod) == null) {
          BeerServiceGrpc.getGetBestBeerMethod = getGetBestBeerMethod =
              io.grpc.MethodDescriptor.<com.ibm.ws.grpc.fat.beer.service.RequestedBeerType, com.ibm.ws.grpc.fat.beer.service.Beer>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetBestBeer"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.ws.grpc.fat.beer.service.RequestedBeerType.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.ws.grpc.fat.beer.service.Beer.getDefaultInstance()))
              .setSchemaDescriptor(new BeerServiceMethodDescriptorSupplier("GetBestBeer"))
              .build();
        }
      }
    }
    return getGetBestBeerMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.ibm.ws.grpc.fat.beer.service.Beer> getGetBeersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetBeers",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.ibm.ws.grpc.fat.beer.service.Beer.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.ibm.ws.grpc.fat.beer.service.Beer> getGetBeersMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.ibm.ws.grpc.fat.beer.service.Beer> getGetBeersMethod;
    if ((getGetBeersMethod = BeerServiceGrpc.getGetBeersMethod) == null) {
      synchronized (BeerServiceGrpc.class) {
        if ((getGetBeersMethod = BeerServiceGrpc.getGetBeersMethod) == null) {
          BeerServiceGrpc.getGetBeersMethod = getGetBeersMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.ibm.ws.grpc.fat.beer.service.Beer>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetBeers"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.ws.grpc.fat.beer.service.Beer.getDefaultInstance()))
              .setSchemaDescriptor(new BeerServiceMethodDescriptorSupplier("GetBeers"))
              .build();
        }
      }
    }
    return getGetBeersMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static BeerServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<BeerServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<BeerServiceStub>() {
        @java.lang.Override
        public BeerServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new BeerServiceStub(channel, callOptions);
        }
      };
    return BeerServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static BeerServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<BeerServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<BeerServiceBlockingStub>() {
        @java.lang.Override
        public BeerServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new BeerServiceBlockingStub(channel, callOptions);
        }
      };
    return BeerServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static BeerServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<BeerServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<BeerServiceFutureStub>() {
        @java.lang.Override
        public BeerServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new BeerServiceFutureStub(channel, callOptions);
        }
      };
    return BeerServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * The beer service definition.
   * </pre>
   */
  public static abstract class BeerServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Add a beer to the collection - unary
     * </pre>
     */
    public void addBeer(com.ibm.ws.grpc.fat.beer.service.Beer request,
        io.grpc.stub.StreamObserver<com.ibm.ws.grpc.fat.beer.service.BeerResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getAddBeerMethod(), responseObserver);
    }

    /**
     * <pre>
     * Delete a beer from the collection - unary
     * </pre>
     */
    public void deleteBeer(com.ibm.ws.grpc.fat.beer.service.Beer request,
        io.grpc.stub.StreamObserver<com.ibm.ws.grpc.fat.beer.service.BeerResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteBeerMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get the best beer of this type
     * </pre>
     */
    public void getBestBeer(com.ibm.ws.grpc.fat.beer.service.RequestedBeerType request,
        io.grpc.stub.StreamObserver<com.ibm.ws.grpc.fat.beer.service.Beer> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBestBeerMethod(), responseObserver);
    }

    /**
     * <pre>
     *Get a list of all the beers
     * </pre>
     */
    public void getBeers(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.ibm.ws.grpc.fat.beer.service.Beer> responseObserver) {
      asyncUnimplementedUnaryCall(getGetBeersMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getAddBeerMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.ibm.ws.grpc.fat.beer.service.Beer,
                com.ibm.ws.grpc.fat.beer.service.BeerResponse>(
                  this, METHODID_ADD_BEER)))
          .addMethod(
            getDeleteBeerMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.ibm.ws.grpc.fat.beer.service.Beer,
                com.ibm.ws.grpc.fat.beer.service.BeerResponse>(
                  this, METHODID_DELETE_BEER)))
          .addMethod(
            getGetBestBeerMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.ibm.ws.grpc.fat.beer.service.RequestedBeerType,
                com.ibm.ws.grpc.fat.beer.service.Beer>(
                  this, METHODID_GET_BEST_BEER)))
          .addMethod(
            getGetBeersMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.ibm.ws.grpc.fat.beer.service.Beer>(
                  this, METHODID_GET_BEERS)))
          .build();
    }
  }

  /**
   * <pre>
   * The beer service definition.
   * </pre>
   */
  public static final class BeerServiceStub extends io.grpc.stub.AbstractAsyncStub<BeerServiceStub> {
    private BeerServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected BeerServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new BeerServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Add a beer to the collection - unary
     * </pre>
     */
    public void addBeer(com.ibm.ws.grpc.fat.beer.service.Beer request,
        io.grpc.stub.StreamObserver<com.ibm.ws.grpc.fat.beer.service.BeerResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAddBeerMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Delete a beer from the collection - unary
     * </pre>
     */
    public void deleteBeer(com.ibm.ws.grpc.fat.beer.service.Beer request,
        io.grpc.stub.StreamObserver<com.ibm.ws.grpc.fat.beer.service.BeerResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteBeerMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get the best beer of this type
     * </pre>
     */
    public void getBestBeer(com.ibm.ws.grpc.fat.beer.service.RequestedBeerType request,
        io.grpc.stub.StreamObserver<com.ibm.ws.grpc.fat.beer.service.Beer> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetBestBeerMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Get a list of all the beers
     * </pre>
     */
    public void getBeers(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.ibm.ws.grpc.fat.beer.service.Beer> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getGetBeersMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * The beer service definition.
   * </pre>
   */
  public static final class BeerServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<BeerServiceBlockingStub> {
    private BeerServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected BeerServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new BeerServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Add a beer to the collection - unary
     * </pre>
     */
    public com.ibm.ws.grpc.fat.beer.service.BeerResponse addBeer(com.ibm.ws.grpc.fat.beer.service.Beer request) {
      return blockingUnaryCall(
          getChannel(), getAddBeerMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete a beer from the collection - unary
     * </pre>
     */
    public com.ibm.ws.grpc.fat.beer.service.BeerResponse deleteBeer(com.ibm.ws.grpc.fat.beer.service.Beer request) {
      return blockingUnaryCall(
          getChannel(), getDeleteBeerMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get the best beer of this type
     * </pre>
     */
    public com.ibm.ws.grpc.fat.beer.service.Beer getBestBeer(com.ibm.ws.grpc.fat.beer.service.RequestedBeerType request) {
      return blockingUnaryCall(
          getChannel(), getGetBestBeerMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Get a list of all the beers
     * </pre>
     */
    public java.util.Iterator<com.ibm.ws.grpc.fat.beer.service.Beer> getBeers(
        com.google.protobuf.Empty request) {
      return blockingServerStreamingCall(
          getChannel(), getGetBeersMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * The beer service definition.
   * </pre>
   */
  public static final class BeerServiceFutureStub extends io.grpc.stub.AbstractFutureStub<BeerServiceFutureStub> {
    private BeerServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected BeerServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new BeerServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Add a beer to the collection - unary
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ibm.ws.grpc.fat.beer.service.BeerResponse> addBeer(
        com.ibm.ws.grpc.fat.beer.service.Beer request) {
      return futureUnaryCall(
          getChannel().newCall(getAddBeerMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Delete a beer from the collection - unary
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ibm.ws.grpc.fat.beer.service.BeerResponse> deleteBeer(
        com.ibm.ws.grpc.fat.beer.service.Beer request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteBeerMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get the best beer of this type
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ibm.ws.grpc.fat.beer.service.Beer> getBestBeer(
        com.ibm.ws.grpc.fat.beer.service.RequestedBeerType request) {
      return futureUnaryCall(
          getChannel().newCall(getGetBestBeerMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_ADD_BEER = 0;
  private static final int METHODID_DELETE_BEER = 1;
  private static final int METHODID_GET_BEST_BEER = 2;
  private static final int METHODID_GET_BEERS = 3;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final BeerServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(BeerServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_ADD_BEER:
          serviceImpl.addBeer((com.ibm.ws.grpc.fat.beer.service.Beer) request,
              (io.grpc.stub.StreamObserver<com.ibm.ws.grpc.fat.beer.service.BeerResponse>) responseObserver);
          break;
        case METHODID_DELETE_BEER:
          serviceImpl.deleteBeer((com.ibm.ws.grpc.fat.beer.service.Beer) request,
              (io.grpc.stub.StreamObserver<com.ibm.ws.grpc.fat.beer.service.BeerResponse>) responseObserver);
          break;
        case METHODID_GET_BEST_BEER:
          serviceImpl.getBestBeer((com.ibm.ws.grpc.fat.beer.service.RequestedBeerType) request,
              (io.grpc.stub.StreamObserver<com.ibm.ws.grpc.fat.beer.service.Beer>) responseObserver);
          break;
        case METHODID_GET_BEERS:
          serviceImpl.getBeers((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.ibm.ws.grpc.fat.beer.service.Beer>) responseObserver);
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

  private static abstract class BeerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    BeerServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.ibm.ws.grpc.fat.beer.service.BeerProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("BeerService");
    }
  }

  private static final class BeerServiceFileDescriptorSupplier
      extends BeerServiceBaseDescriptorSupplier {
    BeerServiceFileDescriptorSupplier() {}
  }

  private static final class BeerServiceMethodDescriptorSupplier
      extends BeerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    BeerServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (BeerServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new BeerServiceFileDescriptorSupplier())
              .addMethod(getAddBeerMethod())
              .addMethod(getDeleteBeerMethod())
              .addMethod(getGetBestBeerMethod())
              .addMethod(getGetBeersMethod())
              .build();
        }
      }
    }
    return result;
  }
}
