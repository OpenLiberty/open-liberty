package com.ibm.test.g3store.grpc;

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
 * The service definition.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.28.1)",
    comments = "Source: ConsumerStore.proto")
public final class AppConsumerServiceGrpc {

  private AppConsumerServiceGrpc() {}

  public static final String SERVICE_NAME = "test.g3store.grpc.AppConsumerService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.ibm.test.g3store.grpc.NameResponse> getGetAllAppNamesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getAllAppNames",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.ibm.test.g3store.grpc.NameResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.ibm.test.g3store.grpc.NameResponse> getGetAllAppNamesMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.ibm.test.g3store.grpc.NameResponse> getGetAllAppNamesMethod;
    if ((getGetAllAppNamesMethod = AppConsumerServiceGrpc.getGetAllAppNamesMethod) == null) {
      synchronized (AppConsumerServiceGrpc.class) {
        if ((getGetAllAppNamesMethod = AppConsumerServiceGrpc.getGetAllAppNamesMethod) == null) {
          AppConsumerServiceGrpc.getGetAllAppNamesMethod = getGetAllAppNamesMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.ibm.test.g3store.grpc.NameResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getAllAppNames"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.NameResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AppConsumerServiceMethodDescriptorSupplier("getAllAppNames"))
              .build();
        }
      }
    }
    return getGetAllAppNamesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.AppNameRequest,
      com.ibm.test.g3store.grpc.RetailAppResponse> getGetAppInfoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getAppInfo",
      requestType = com.ibm.test.g3store.grpc.AppNameRequest.class,
      responseType = com.ibm.test.g3store.grpc.RetailAppResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.AppNameRequest,
      com.ibm.test.g3store.grpc.RetailAppResponse> getGetAppInfoMethod() {
    io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.AppNameRequest, com.ibm.test.g3store.grpc.RetailAppResponse> getGetAppInfoMethod;
    if ((getGetAppInfoMethod = AppConsumerServiceGrpc.getGetAppInfoMethod) == null) {
      synchronized (AppConsumerServiceGrpc.class) {
        if ((getGetAppInfoMethod = AppConsumerServiceGrpc.getGetAppInfoMethod) == null) {
          AppConsumerServiceGrpc.getGetAppInfoMethod = getGetAppInfoMethod =
              io.grpc.MethodDescriptor.<com.ibm.test.g3store.grpc.AppNameRequest, com.ibm.test.g3store.grpc.RetailAppResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getAppInfo"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.AppNameRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.RetailAppResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AppConsumerServiceMethodDescriptorSupplier("getAppInfo"))
              .build();
        }
      }
    }
    return getGetAppInfoMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.GenreRequest,
      com.ibm.test.g3store.grpc.GenreCountResponse> getGetCountMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getCount",
      requestType = com.ibm.test.g3store.grpc.GenreRequest.class,
      responseType = com.ibm.test.g3store.grpc.GenreCountResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
  public static io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.GenreRequest,
      com.ibm.test.g3store.grpc.GenreCountResponse> getGetCountMethod() {
    io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.GenreRequest, com.ibm.test.g3store.grpc.GenreCountResponse> getGetCountMethod;
    if ((getGetCountMethod = AppConsumerServiceGrpc.getGetCountMethod) == null) {
      synchronized (AppConsumerServiceGrpc.class) {
        if ((getGetCountMethod = AppConsumerServiceGrpc.getGetCountMethod) == null) {
          AppConsumerServiceGrpc.getGetCountMethod = getGetCountMethod =
              io.grpc.MethodDescriptor.<com.ibm.test.g3store.grpc.GenreRequest, com.ibm.test.g3store.grpc.GenreCountResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getCount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.GenreRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.GenreCountResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AppConsumerServiceMethodDescriptorSupplier("getCount"))
              .build();
        }
      }
    }
    return getGetCountMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.AppNameRequest,
      com.ibm.test.g3store.grpc.PriceResponse> getGetPricesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getPrices",
      requestType = com.ibm.test.g3store.grpc.AppNameRequest.class,
      responseType = com.ibm.test.g3store.grpc.PriceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.AppNameRequest,
      com.ibm.test.g3store.grpc.PriceResponse> getGetPricesMethod() {
    io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.AppNameRequest, com.ibm.test.g3store.grpc.PriceResponse> getGetPricesMethod;
    if ((getGetPricesMethod = AppConsumerServiceGrpc.getGetPricesMethod) == null) {
      synchronized (AppConsumerServiceGrpc.class) {
        if ((getGetPricesMethod = AppConsumerServiceGrpc.getGetPricesMethod) == null) {
          AppConsumerServiceGrpc.getGetPricesMethod = getGetPricesMethod =
              io.grpc.MethodDescriptor.<com.ibm.test.g3store.grpc.AppNameRequest, com.ibm.test.g3store.grpc.PriceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getPrices"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.AppNameRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.PriceResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AppConsumerServiceMethodDescriptorSupplier("getPrices"))
              .build();
        }
      }
    }
    return getGetPricesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.PurchaseRequest,
      com.ibm.test.g3store.grpc.RetailAppResponse> getPurchaseRetailAppMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "purchaseRetailApp",
      requestType = com.ibm.test.g3store.grpc.PurchaseRequest.class,
      responseType = com.ibm.test.g3store.grpc.RetailAppResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.PurchaseRequest,
      com.ibm.test.g3store.grpc.RetailAppResponse> getPurchaseRetailAppMethod() {
    io.grpc.MethodDescriptor<com.ibm.test.g3store.grpc.PurchaseRequest, com.ibm.test.g3store.grpc.RetailAppResponse> getPurchaseRetailAppMethod;
    if ((getPurchaseRetailAppMethod = AppConsumerServiceGrpc.getPurchaseRetailAppMethod) == null) {
      synchronized (AppConsumerServiceGrpc.class) {
        if ((getPurchaseRetailAppMethod = AppConsumerServiceGrpc.getPurchaseRetailAppMethod) == null) {
          AppConsumerServiceGrpc.getPurchaseRetailAppMethod = getPurchaseRetailAppMethod =
              io.grpc.MethodDescriptor.<com.ibm.test.g3store.grpc.PurchaseRequest, com.ibm.test.g3store.grpc.RetailAppResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "purchaseRetailApp"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.PurchaseRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.RetailAppResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AppConsumerServiceMethodDescriptorSupplier("purchaseRetailApp"))
              .build();
        }
      }
    }
    return getPurchaseRetailAppMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static AppConsumerServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AppConsumerServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AppConsumerServiceStub>() {
        @java.lang.Override
        public AppConsumerServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AppConsumerServiceStub(channel, callOptions);
        }
      };
    return AppConsumerServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static AppConsumerServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AppConsumerServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AppConsumerServiceBlockingStub>() {
        @java.lang.Override
        public AppConsumerServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AppConsumerServiceBlockingStub(channel, callOptions);
        }
      };
    return AppConsumerServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static AppConsumerServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AppConsumerServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AppConsumerServiceFutureStub>() {
        @java.lang.Override
        public AppConsumerServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AppConsumerServiceFutureStub(channel, callOptions);
        }
      };
    return AppConsumerServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * The service definition.
   * </pre>
   */
  public static abstract class AppConsumerServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * List of all App names available on server 
     * </pre>
     */
    public void getAllAppNames(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.NameResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAllAppNamesMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get App Structure for each app
     * </pre>
     */
    public void getAppInfo(com.ibm.test.g3store.grpc.AppNameRequest request,
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.RetailAppResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAppInfoMethod(), responseObserver);
    }

    /**
     * <pre>
     * send one Gnere at a time , server will keep count, and when client is done , server will send Total count 
     * </pre>
     */
    public io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.GenreRequest> getCount(
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.GenreCountResponse> responseObserver) {
      return asyncUnimplementedStreamingCall(getGetCountMethod(), responseObserver);
    }

    /**
     * <pre>
     * send one App name at a time, server will send the prices of each app in the response until client is done  
     * </pre>
     */
    public io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.AppNameRequest> getPrices(
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.PriceResponse> responseObserver) {
      return asyncUnimplementedStreamingCall(getGetPricesMethod(), responseObserver);
    }

    /**
     * <pre>
     * TODO
     * </pre>
     */
    public void purchaseRetailApp(com.ibm.test.g3store.grpc.PurchaseRequest request,
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.RetailAppResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getPurchaseRetailAppMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getGetAllAppNamesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.ibm.test.g3store.grpc.NameResponse>(
                  this, METHODID_GET_ALL_APP_NAMES)))
          .addMethod(
            getGetAppInfoMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.ibm.test.g3store.grpc.AppNameRequest,
                com.ibm.test.g3store.grpc.RetailAppResponse>(
                  this, METHODID_GET_APP_INFO)))
          .addMethod(
            getGetCountMethod(),
            asyncClientStreamingCall(
              new MethodHandlers<
                com.ibm.test.g3store.grpc.GenreRequest,
                com.ibm.test.g3store.grpc.GenreCountResponse>(
                  this, METHODID_GET_COUNT)))
          .addMethod(
            getGetPricesMethod(),
            asyncBidiStreamingCall(
              new MethodHandlers<
                com.ibm.test.g3store.grpc.AppNameRequest,
                com.ibm.test.g3store.grpc.PriceResponse>(
                  this, METHODID_GET_PRICES)))
          .addMethod(
            getPurchaseRetailAppMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.ibm.test.g3store.grpc.PurchaseRequest,
                com.ibm.test.g3store.grpc.RetailAppResponse>(
                  this, METHODID_PURCHASE_RETAIL_APP)))
          .build();
    }
  }

  /**
   * <pre>
   * The service definition.
   * </pre>
   */
  public static final class AppConsumerServiceStub extends io.grpc.stub.AbstractAsyncStub<AppConsumerServiceStub> {
    private AppConsumerServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AppConsumerServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AppConsumerServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * List of all App names available on server 
     * </pre>
     */
    public void getAllAppNames(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.NameResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAllAppNamesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get App Structure for each app
     * </pre>
     */
    public void getAppInfo(com.ibm.test.g3store.grpc.AppNameRequest request,
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.RetailAppResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAppInfoMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * send one Gnere at a time , server will keep count, and when client is done , server will send Total count 
     * </pre>
     */
    public io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.GenreRequest> getCount(
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.GenreCountResponse> responseObserver) {
      return asyncClientStreamingCall(
          getChannel().newCall(getGetCountMethod(), getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     * send one App name at a time, server will send the prices of each app in the response until client is done  
     * </pre>
     */
    public io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.AppNameRequest> getPrices(
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.PriceResponse> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(getGetPricesMethod(), getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     * TODO
     * </pre>
     */
    public void purchaseRetailApp(com.ibm.test.g3store.grpc.PurchaseRequest request,
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.RetailAppResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getPurchaseRetailAppMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * The service definition.
   * </pre>
   */
  public static final class AppConsumerServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<AppConsumerServiceBlockingStub> {
    private AppConsumerServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AppConsumerServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AppConsumerServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * List of all App names available on server 
     * </pre>
     */
    public com.ibm.test.g3store.grpc.NameResponse getAllAppNames(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getGetAllAppNamesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get App Structure for each app
     * </pre>
     */
    public com.ibm.test.g3store.grpc.RetailAppResponse getAppInfo(com.ibm.test.g3store.grpc.AppNameRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetAppInfoMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * TODO
     * </pre>
     */
    public com.ibm.test.g3store.grpc.RetailAppResponse purchaseRetailApp(com.ibm.test.g3store.grpc.PurchaseRequest request) {
      return blockingUnaryCall(
          getChannel(), getPurchaseRetailAppMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * The service definition.
   * </pre>
   */
  public static final class AppConsumerServiceFutureStub extends io.grpc.stub.AbstractFutureStub<AppConsumerServiceFutureStub> {
    private AppConsumerServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AppConsumerServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AppConsumerServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * List of all App names available on server 
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ibm.test.g3store.grpc.NameResponse> getAllAppNames(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAllAppNamesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get App Structure for each app
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ibm.test.g3store.grpc.RetailAppResponse> getAppInfo(
        com.ibm.test.g3store.grpc.AppNameRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAppInfoMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * TODO
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ibm.test.g3store.grpc.RetailAppResponse> purchaseRetailApp(
        com.ibm.test.g3store.grpc.PurchaseRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getPurchaseRetailAppMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_ALL_APP_NAMES = 0;
  private static final int METHODID_GET_APP_INFO = 1;
  private static final int METHODID_PURCHASE_RETAIL_APP = 2;
  private static final int METHODID_GET_COUNT = 3;
  private static final int METHODID_GET_PRICES = 4;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AppConsumerServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(AppConsumerServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_ALL_APP_NAMES:
          serviceImpl.getAllAppNames((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.NameResponse>) responseObserver);
          break;
        case METHODID_GET_APP_INFO:
          serviceImpl.getAppInfo((com.ibm.test.g3store.grpc.AppNameRequest) request,
              (io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.RetailAppResponse>) responseObserver);
          break;
        case METHODID_PURCHASE_RETAIL_APP:
          serviceImpl.purchaseRetailApp((com.ibm.test.g3store.grpc.PurchaseRequest) request,
              (io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.RetailAppResponse>) responseObserver);
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
        case METHODID_GET_COUNT:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.getCount(
              (io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.GenreCountResponse>) responseObserver);
        case METHODID_GET_PRICES:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.getPrices(
              (io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.PriceResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class AppConsumerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    AppConsumerServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.ibm.test.g3store.grpc.AppConsumerProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("AppConsumerService");
    }
  }

  private static final class AppConsumerServiceFileDescriptorSupplier
      extends AppConsumerServiceBaseDescriptorSupplier {
    AppConsumerServiceFileDescriptorSupplier() {}
  }

  private static final class AppConsumerServiceMethodDescriptorSupplier
      extends AppConsumerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    AppConsumerServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (AppConsumerServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new AppConsumerServiceFileDescriptorSupplier())
              .addMethod(getGetAllAppNamesMethod())
              .addMethod(getGetAppInfoMethod())
              .addMethod(getGetCountMethod())
              .addMethod(getGetPricesMethod())
              .addMethod(getPurchaseRetailAppMethod())
              .build();
        }
      }
    }
    return result;
  }
}
