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
    value = "by gRPC proto compiler (version 1.31.1)",
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

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.ibm.test.g3store.grpc.NameResponse> getGetAllAppNamesAuthHeaderViaCallCredMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getAllAppNames_AuthHeader_Via_CallCred",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.ibm.test.g3store.grpc.NameResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.ibm.test.g3store.grpc.NameResponse> getGetAllAppNamesAuthHeaderViaCallCredMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.ibm.test.g3store.grpc.NameResponse> getGetAllAppNamesAuthHeaderViaCallCredMethod;
    if ((getGetAllAppNamesAuthHeaderViaCallCredMethod = AppConsumerServiceGrpc.getGetAllAppNamesAuthHeaderViaCallCredMethod) == null) {
      synchronized (AppConsumerServiceGrpc.class) {
        if ((getGetAllAppNamesAuthHeaderViaCallCredMethod = AppConsumerServiceGrpc.getGetAllAppNamesAuthHeaderViaCallCredMethod) == null) {
          AppConsumerServiceGrpc.getGetAllAppNamesAuthHeaderViaCallCredMethod = getGetAllAppNamesAuthHeaderViaCallCredMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.ibm.test.g3store.grpc.NameResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getAllAppNames_AuthHeader_Via_CallCred"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.NameResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AppConsumerServiceMethodDescriptorSupplier("getAllAppNames_AuthHeader_Via_CallCred"))
              .build();
        }
      }
    }
    return getGetAllAppNamesAuthHeaderViaCallCredMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.ibm.test.g3store.grpc.NameResponse> getGetAllAppNamesAuthHeaderViaClientInterceptorMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getAllAppNames_AuthHeader_Via_ClientInterceptor",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.ibm.test.g3store.grpc.NameResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.ibm.test.g3store.grpc.NameResponse> getGetAllAppNamesAuthHeaderViaClientInterceptorMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.ibm.test.g3store.grpc.NameResponse> getGetAllAppNamesAuthHeaderViaClientInterceptorMethod;
    if ((getGetAllAppNamesAuthHeaderViaClientInterceptorMethod = AppConsumerServiceGrpc.getGetAllAppNamesAuthHeaderViaClientInterceptorMethod) == null) {
      synchronized (AppConsumerServiceGrpc.class) {
        if ((getGetAllAppNamesAuthHeaderViaClientInterceptorMethod = AppConsumerServiceGrpc.getGetAllAppNamesAuthHeaderViaClientInterceptorMethod) == null) {
          AppConsumerServiceGrpc.getGetAllAppNamesAuthHeaderViaClientInterceptorMethod = getGetAllAppNamesAuthHeaderViaClientInterceptorMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.ibm.test.g3store.grpc.NameResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getAllAppNames_AuthHeader_Via_ClientInterceptor"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.NameResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AppConsumerServiceMethodDescriptorSupplier("getAllAppNames_AuthHeader_Via_ClientInterceptor"))
              .build();
        }
      }
    }
    return getGetAllAppNamesAuthHeaderViaClientInterceptorMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.ibm.test.g3store.grpc.NameResponse> getGetAppNameSetBadRolesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getAppNameSetBadRoles",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.ibm.test.g3store.grpc.NameResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.ibm.test.g3store.grpc.NameResponse> getGetAppNameSetBadRolesMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.ibm.test.g3store.grpc.NameResponse> getGetAppNameSetBadRolesMethod;
    if ((getGetAppNameSetBadRolesMethod = AppConsumerServiceGrpc.getGetAppNameSetBadRolesMethod) == null) {
      synchronized (AppConsumerServiceGrpc.class) {
        if ((getGetAppNameSetBadRolesMethod = AppConsumerServiceGrpc.getGetAppNameSetBadRolesMethod) == null) {
          AppConsumerServiceGrpc.getGetAppNameSetBadRolesMethod = getGetAppNameSetBadRolesMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.ibm.test.g3store.grpc.NameResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getAppNameSetBadRoles"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.NameResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AppConsumerServiceMethodDescriptorSupplier("getAppNameSetBadRoles"))
              .build();
        }
      }
    }
    return getGetAppNameSetBadRolesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.ibm.test.g3store.grpc.NameResponse> getGetNameCookieJWTHeaderMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getNameCookieJWTHeader",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.ibm.test.g3store.grpc.NameResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.ibm.test.g3store.grpc.NameResponse> getGetNameCookieJWTHeaderMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.ibm.test.g3store.grpc.NameResponse> getGetNameCookieJWTHeaderMethod;
    if ((getGetNameCookieJWTHeaderMethod = AppConsumerServiceGrpc.getGetNameCookieJWTHeaderMethod) == null) {
      synchronized (AppConsumerServiceGrpc.class) {
        if ((getGetNameCookieJWTHeaderMethod = AppConsumerServiceGrpc.getGetNameCookieJWTHeaderMethod) == null) {
          AppConsumerServiceGrpc.getGetNameCookieJWTHeaderMethod = getGetNameCookieJWTHeaderMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.ibm.test.g3store.grpc.NameResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getNameCookieJWTHeader"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.NameResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AppConsumerServiceMethodDescriptorSupplier("getNameCookieJWTHeader"))
              .build();
        }
      }
    }
    return getGetNameCookieJWTHeaderMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.ibm.test.g3store.grpc.NameResponse> getGetAppSetBadRoleCookieJWTHeaderMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getAppSetBadRoleCookieJWTHeader",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.ibm.test.g3store.grpc.NameResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.ibm.test.g3store.grpc.NameResponse> getGetAppSetBadRoleCookieJWTHeaderMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.ibm.test.g3store.grpc.NameResponse> getGetAppSetBadRoleCookieJWTHeaderMethod;
    if ((getGetAppSetBadRoleCookieJWTHeaderMethod = AppConsumerServiceGrpc.getGetAppSetBadRoleCookieJWTHeaderMethod) == null) {
      synchronized (AppConsumerServiceGrpc.class) {
        if ((getGetAppSetBadRoleCookieJWTHeaderMethod = AppConsumerServiceGrpc.getGetAppSetBadRoleCookieJWTHeaderMethod) == null) {
          AppConsumerServiceGrpc.getGetAppSetBadRoleCookieJWTHeaderMethod = getGetAppSetBadRoleCookieJWTHeaderMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.ibm.test.g3store.grpc.NameResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getAppSetBadRoleCookieJWTHeader"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.test.g3store.grpc.NameResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AppConsumerServiceMethodDescriptorSupplier("getAppSetBadRoleCookieJWTHeader"))
              .build();
        }
      }
    }
    return getGetAppSetBadRoleCookieJWTHeaderMethod;
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

    /**
     * <pre>
     * List of all App names available on server 
     * </pre>
     */
    public void getAllAppNamesAuthHeaderViaCallCred(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.NameResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAllAppNamesAuthHeaderViaCallCredMethod(), responseObserver);
    }

    /**
     * <pre>
     * List of all App names available on server 
     * </pre>
     */
    public void getAllAppNamesAuthHeaderViaClientInterceptor(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.NameResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAllAppNamesAuthHeaderViaClientInterceptorMethod(), responseObserver);
    }

    /**
     */
    public void getAppNameSetBadRoles(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.NameResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAppNameSetBadRolesMethod(), responseObserver);
    }

    /**
     * <pre>
     * List all app names where JWT token is propagated as Cookie header
     * </pre>
     */
    public void getNameCookieJWTHeader(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.NameResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetNameCookieJWTHeaderMethod(), responseObserver);
    }

    /**
     * <pre>
     * List all app names where JWT token is propagated as Cookie header but bad role
     * </pre>
     */
    public void getAppSetBadRoleCookieJWTHeader(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.NameResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetAppSetBadRoleCookieJWTHeaderMethod(), responseObserver);
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
          .addMethod(
            getGetAllAppNamesAuthHeaderViaCallCredMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.ibm.test.g3store.grpc.NameResponse>(
                  this, METHODID_GET_ALL_APP_NAMES_AUTH_HEADER_VIA_CALL_CRED)))
          .addMethod(
            getGetAllAppNamesAuthHeaderViaClientInterceptorMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.ibm.test.g3store.grpc.NameResponse>(
                  this, METHODID_GET_ALL_APP_NAMES_AUTH_HEADER_VIA_CLIENT_INTERCEPTOR)))
          .addMethod(
            getGetAppNameSetBadRolesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.ibm.test.g3store.grpc.NameResponse>(
                  this, METHODID_GET_APP_NAME_SET_BAD_ROLES)))
          .addMethod(
            getGetNameCookieJWTHeaderMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.ibm.test.g3store.grpc.NameResponse>(
                  this, METHODID_GET_NAME_COOKIE_JWTHEADER)))
          .addMethod(
            getGetAppSetBadRoleCookieJWTHeaderMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.ibm.test.g3store.grpc.NameResponse>(
                  this, METHODID_GET_APP_SET_BAD_ROLE_COOKIE_JWTHEADER)))
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

    /**
     * <pre>
     * List of all App names available on server 
     * </pre>
     */
    public void getAllAppNamesAuthHeaderViaCallCred(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.NameResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAllAppNamesAuthHeaderViaCallCredMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List of all App names available on server 
     * </pre>
     */
    public void getAllAppNamesAuthHeaderViaClientInterceptor(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.NameResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAllAppNamesAuthHeaderViaClientInterceptorMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getAppNameSetBadRoles(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.NameResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAppNameSetBadRolesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List all app names where JWT token is propagated as Cookie header
     * </pre>
     */
    public void getNameCookieJWTHeader(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.NameResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetNameCookieJWTHeaderMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List all app names where JWT token is propagated as Cookie header but bad role
     * </pre>
     */
    public void getAppSetBadRoleCookieJWTHeader(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.NameResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetAppSetBadRoleCookieJWTHeaderMethod(), getCallOptions()), request, responseObserver);
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

    /**
     * <pre>
     * List of all App names available on server 
     * </pre>
     */
    public com.ibm.test.g3store.grpc.NameResponse getAllAppNamesAuthHeaderViaCallCred(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getGetAllAppNamesAuthHeaderViaCallCredMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List of all App names available on server 
     * </pre>
     */
    public com.ibm.test.g3store.grpc.NameResponse getAllAppNamesAuthHeaderViaClientInterceptor(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getGetAllAppNamesAuthHeaderViaClientInterceptorMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.ibm.test.g3store.grpc.NameResponse getAppNameSetBadRoles(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getGetAppNameSetBadRolesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List all app names where JWT token is propagated as Cookie header
     * </pre>
     */
    public com.ibm.test.g3store.grpc.NameResponse getNameCookieJWTHeader(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getGetNameCookieJWTHeaderMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List all app names where JWT token is propagated as Cookie header but bad role
     * </pre>
     */
    public com.ibm.test.g3store.grpc.NameResponse getAppSetBadRoleCookieJWTHeader(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getGetAppSetBadRoleCookieJWTHeaderMethod(), getCallOptions(), request);
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

    /**
     * <pre>
     * List of all App names available on server 
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ibm.test.g3store.grpc.NameResponse> getAllAppNamesAuthHeaderViaCallCred(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAllAppNamesAuthHeaderViaCallCredMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List of all App names available on server 
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ibm.test.g3store.grpc.NameResponse> getAllAppNamesAuthHeaderViaClientInterceptor(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAllAppNamesAuthHeaderViaClientInterceptorMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ibm.test.g3store.grpc.NameResponse> getAppNameSetBadRoles(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAppNameSetBadRolesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List all app names where JWT token is propagated as Cookie header
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ibm.test.g3store.grpc.NameResponse> getNameCookieJWTHeader(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getGetNameCookieJWTHeaderMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List all app names where JWT token is propagated as Cookie header but bad role
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ibm.test.g3store.grpc.NameResponse> getAppSetBadRoleCookieJWTHeader(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getGetAppSetBadRoleCookieJWTHeaderMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_ALL_APP_NAMES = 0;
  private static final int METHODID_GET_APP_INFO = 1;
  private static final int METHODID_PURCHASE_RETAIL_APP = 2;
  private static final int METHODID_GET_ALL_APP_NAMES_AUTH_HEADER_VIA_CALL_CRED = 3;
  private static final int METHODID_GET_ALL_APP_NAMES_AUTH_HEADER_VIA_CLIENT_INTERCEPTOR = 4;
  private static final int METHODID_GET_APP_NAME_SET_BAD_ROLES = 5;
  private static final int METHODID_GET_NAME_COOKIE_JWTHEADER = 6;
  private static final int METHODID_GET_APP_SET_BAD_ROLE_COOKIE_JWTHEADER = 7;
  private static final int METHODID_GET_COUNT = 8;
  private static final int METHODID_GET_PRICES = 9;

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
        case METHODID_GET_ALL_APP_NAMES_AUTH_HEADER_VIA_CALL_CRED:
          serviceImpl.getAllAppNamesAuthHeaderViaCallCred((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.NameResponse>) responseObserver);
          break;
        case METHODID_GET_ALL_APP_NAMES_AUTH_HEADER_VIA_CLIENT_INTERCEPTOR:
          serviceImpl.getAllAppNamesAuthHeaderViaClientInterceptor((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.NameResponse>) responseObserver);
          break;
        case METHODID_GET_APP_NAME_SET_BAD_ROLES:
          serviceImpl.getAppNameSetBadRoles((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.NameResponse>) responseObserver);
          break;
        case METHODID_GET_NAME_COOKIE_JWTHEADER:
          serviceImpl.getNameCookieJWTHeader((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.NameResponse>) responseObserver);
          break;
        case METHODID_GET_APP_SET_BAD_ROLE_COOKIE_JWTHEADER:
          serviceImpl.getAppSetBadRoleCookieJWTHeader((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.NameResponse>) responseObserver);
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
              .addMethod(getGetAllAppNamesAuthHeaderViaCallCredMethod())
              .addMethod(getGetAllAppNamesAuthHeaderViaClientInterceptorMethod())
              .addMethod(getGetAppNameSetBadRolesMethod())
              .addMethod(getGetNameCookieJWTHeaderMethod())
              .addMethod(getGetAppSetBadRoleCookieJWTHeaderMethod())
              .build();
        }
      }
    }
    return result;
  }
}
