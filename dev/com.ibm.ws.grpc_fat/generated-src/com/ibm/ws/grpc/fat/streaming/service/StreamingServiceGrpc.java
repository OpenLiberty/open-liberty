package com.ibm.ws.grpc.fat.streaming.service;

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
 * The streaming service definition.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.28.1)",
    comments = "Source: Streaming.proto")
public final class StreamingServiceGrpc {

  private StreamingServiceGrpc() {}

  public static final String SERVICE_NAME = "streaming.StreamingService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.ibm.ws.grpc.fat.streaming.service.StreamRequest,
      com.ibm.ws.grpc.fat.streaming.service.StreamReply> getClientStreamMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ClientStream",
      requestType = com.ibm.ws.grpc.fat.streaming.service.StreamRequest.class,
      responseType = com.ibm.ws.grpc.fat.streaming.service.StreamReply.class,
      methodType = io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
  public static io.grpc.MethodDescriptor<com.ibm.ws.grpc.fat.streaming.service.StreamRequest,
      com.ibm.ws.grpc.fat.streaming.service.StreamReply> getClientStreamMethod() {
    io.grpc.MethodDescriptor<com.ibm.ws.grpc.fat.streaming.service.StreamRequest, com.ibm.ws.grpc.fat.streaming.service.StreamReply> getClientStreamMethod;
    if ((getClientStreamMethod = StreamingServiceGrpc.getClientStreamMethod) == null) {
      synchronized (StreamingServiceGrpc.class) {
        if ((getClientStreamMethod = StreamingServiceGrpc.getClientStreamMethod) == null) {
          StreamingServiceGrpc.getClientStreamMethod = getClientStreamMethod =
              io.grpc.MethodDescriptor.<com.ibm.ws.grpc.fat.streaming.service.StreamRequest, com.ibm.ws.grpc.fat.streaming.service.StreamReply>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ClientStream"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.ws.grpc.fat.streaming.service.StreamRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.ws.grpc.fat.streaming.service.StreamReply.getDefaultInstance()))
              .setSchemaDescriptor(new StreamingServiceMethodDescriptorSupplier("ClientStream"))
              .build();
        }
      }
    }
    return getClientStreamMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.ibm.ws.grpc.fat.streaming.service.StreamRequest,
      com.ibm.ws.grpc.fat.streaming.service.StreamReply> getServerStreamMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ServerStream",
      requestType = com.ibm.ws.grpc.fat.streaming.service.StreamRequest.class,
      responseType = com.ibm.ws.grpc.fat.streaming.service.StreamReply.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.ibm.ws.grpc.fat.streaming.service.StreamRequest,
      com.ibm.ws.grpc.fat.streaming.service.StreamReply> getServerStreamMethod() {
    io.grpc.MethodDescriptor<com.ibm.ws.grpc.fat.streaming.service.StreamRequest, com.ibm.ws.grpc.fat.streaming.service.StreamReply> getServerStreamMethod;
    if ((getServerStreamMethod = StreamingServiceGrpc.getServerStreamMethod) == null) {
      synchronized (StreamingServiceGrpc.class) {
        if ((getServerStreamMethod = StreamingServiceGrpc.getServerStreamMethod) == null) {
          StreamingServiceGrpc.getServerStreamMethod = getServerStreamMethod =
              io.grpc.MethodDescriptor.<com.ibm.ws.grpc.fat.streaming.service.StreamRequest, com.ibm.ws.grpc.fat.streaming.service.StreamReply>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ServerStream"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.ws.grpc.fat.streaming.service.StreamRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.ws.grpc.fat.streaming.service.StreamReply.getDefaultInstance()))
              .setSchemaDescriptor(new StreamingServiceMethodDescriptorSupplier("ServerStream"))
              .build();
        }
      }
    }
    return getServerStreamMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.ibm.ws.grpc.fat.streaming.service.StreamRequest,
      com.ibm.ws.grpc.fat.streaming.service.StreamReply> getHelloMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Hello",
      requestType = com.ibm.ws.grpc.fat.streaming.service.StreamRequest.class,
      responseType = com.ibm.ws.grpc.fat.streaming.service.StreamReply.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.ibm.ws.grpc.fat.streaming.service.StreamRequest,
      com.ibm.ws.grpc.fat.streaming.service.StreamReply> getHelloMethod() {
    io.grpc.MethodDescriptor<com.ibm.ws.grpc.fat.streaming.service.StreamRequest, com.ibm.ws.grpc.fat.streaming.service.StreamReply> getHelloMethod;
    if ((getHelloMethod = StreamingServiceGrpc.getHelloMethod) == null) {
      synchronized (StreamingServiceGrpc.class) {
        if ((getHelloMethod = StreamingServiceGrpc.getHelloMethod) == null) {
          StreamingServiceGrpc.getHelloMethod = getHelloMethod =
              io.grpc.MethodDescriptor.<com.ibm.ws.grpc.fat.streaming.service.StreamRequest, com.ibm.ws.grpc.fat.streaming.service.StreamReply>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Hello"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.ws.grpc.fat.streaming.service.StreamRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ibm.ws.grpc.fat.streaming.service.StreamReply.getDefaultInstance()))
              .setSchemaDescriptor(new StreamingServiceMethodDescriptorSupplier("Hello"))
              .build();
        }
      }
    }
    return getHelloMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static StreamingServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StreamingServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StreamingServiceStub>() {
        @java.lang.Override
        public StreamingServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StreamingServiceStub(channel, callOptions);
        }
      };
    return StreamingServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static StreamingServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StreamingServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StreamingServiceBlockingStub>() {
        @java.lang.Override
        public StreamingServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StreamingServiceBlockingStub(channel, callOptions);
        }
      };
    return StreamingServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static StreamingServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StreamingServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StreamingServiceFutureStub>() {
        @java.lang.Override
        public StreamingServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StreamingServiceFutureStub(channel, callOptions);
        }
      };
    return StreamingServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * The streaming service definition.
   * </pre>
   */
  public static abstract class StreamingServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public io.grpc.stub.StreamObserver<com.ibm.ws.grpc.fat.streaming.service.StreamRequest> clientStream(
        io.grpc.stub.StreamObserver<com.ibm.ws.grpc.fat.streaming.service.StreamReply> responseObserver) {
      return asyncUnimplementedStreamingCall(getClientStreamMethod(), responseObserver);
    }

    /**
     */
    public void serverStream(com.ibm.ws.grpc.fat.streaming.service.StreamRequest request,
        io.grpc.stub.StreamObserver<com.ibm.ws.grpc.fat.streaming.service.StreamReply> responseObserver) {
      asyncUnimplementedUnaryCall(getServerStreamMethod(), responseObserver);
    }

    /**
     */
    public void hello(com.ibm.ws.grpc.fat.streaming.service.StreamRequest request,
        io.grpc.stub.StreamObserver<com.ibm.ws.grpc.fat.streaming.service.StreamReply> responseObserver) {
      asyncUnimplementedUnaryCall(getHelloMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getClientStreamMethod(),
            asyncClientStreamingCall(
              new MethodHandlers<
                com.ibm.ws.grpc.fat.streaming.service.StreamRequest,
                com.ibm.ws.grpc.fat.streaming.service.StreamReply>(
                  this, METHODID_CLIENT_STREAM)))
          .addMethod(
            getServerStreamMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                com.ibm.ws.grpc.fat.streaming.service.StreamRequest,
                com.ibm.ws.grpc.fat.streaming.service.StreamReply>(
                  this, METHODID_SERVER_STREAM)))
          .addMethod(
            getHelloMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.ibm.ws.grpc.fat.streaming.service.StreamRequest,
                com.ibm.ws.grpc.fat.streaming.service.StreamReply>(
                  this, METHODID_HELLO)))
          .build();
    }
  }

  /**
   * <pre>
   * The streaming service definition.
   * </pre>
   */
  public static final class StreamingServiceStub extends io.grpc.stub.AbstractAsyncStub<StreamingServiceStub> {
    private StreamingServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StreamingServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StreamingServiceStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<com.ibm.ws.grpc.fat.streaming.service.StreamRequest> clientStream(
        io.grpc.stub.StreamObserver<com.ibm.ws.grpc.fat.streaming.service.StreamReply> responseObserver) {
      return asyncClientStreamingCall(
          getChannel().newCall(getClientStreamMethod(), getCallOptions()), responseObserver);
    }

    /**
     */
    public void serverStream(com.ibm.ws.grpc.fat.streaming.service.StreamRequest request,
        io.grpc.stub.StreamObserver<com.ibm.ws.grpc.fat.streaming.service.StreamReply> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getServerStreamMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void hello(com.ibm.ws.grpc.fat.streaming.service.StreamRequest request,
        io.grpc.stub.StreamObserver<com.ibm.ws.grpc.fat.streaming.service.StreamReply> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getHelloMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * The streaming service definition.
   * </pre>
   */
  public static final class StreamingServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<StreamingServiceBlockingStub> {
    private StreamingServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StreamingServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StreamingServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public java.util.Iterator<com.ibm.ws.grpc.fat.streaming.service.StreamReply> serverStream(
        com.ibm.ws.grpc.fat.streaming.service.StreamRequest request) {
      return blockingServerStreamingCall(
          getChannel(), getServerStreamMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.ibm.ws.grpc.fat.streaming.service.StreamReply hello(com.ibm.ws.grpc.fat.streaming.service.StreamRequest request) {
      return blockingUnaryCall(
          getChannel(), getHelloMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * The streaming service definition.
   * </pre>
   */
  public static final class StreamingServiceFutureStub extends io.grpc.stub.AbstractFutureStub<StreamingServiceFutureStub> {
    private StreamingServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StreamingServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StreamingServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ibm.ws.grpc.fat.streaming.service.StreamReply> hello(
        com.ibm.ws.grpc.fat.streaming.service.StreamRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getHelloMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SERVER_STREAM = 0;
  private static final int METHODID_HELLO = 1;
  private static final int METHODID_CLIENT_STREAM = 2;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final StreamingServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(StreamingServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SERVER_STREAM:
          serviceImpl.serverStream((com.ibm.ws.grpc.fat.streaming.service.StreamRequest) request,
              (io.grpc.stub.StreamObserver<com.ibm.ws.grpc.fat.streaming.service.StreamReply>) responseObserver);
          break;
        case METHODID_HELLO:
          serviceImpl.hello((com.ibm.ws.grpc.fat.streaming.service.StreamRequest) request,
              (io.grpc.stub.StreamObserver<com.ibm.ws.grpc.fat.streaming.service.StreamReply>) responseObserver);
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
        case METHODID_CLIENT_STREAM:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.clientStream(
              (io.grpc.stub.StreamObserver<com.ibm.ws.grpc.fat.streaming.service.StreamReply>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class StreamingServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    StreamingServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.ibm.ws.grpc.fat.streaming.service.StreamingProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("StreamingService");
    }
  }

  private static final class StreamingServiceFileDescriptorSupplier
      extends StreamingServiceBaseDescriptorSupplier {
    StreamingServiceFileDescriptorSupplier() {}
  }

  private static final class StreamingServiceMethodDescriptorSupplier
      extends StreamingServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    StreamingServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (StreamingServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new StreamingServiceFileDescriptorSupplier())
              .addMethod(getClientStreamMethod())
              .addMethod(getServerStreamMethod())
              .addMethod(getHelloMethod())
              .build();
        }
      }
    }
    return result;
  }
}
