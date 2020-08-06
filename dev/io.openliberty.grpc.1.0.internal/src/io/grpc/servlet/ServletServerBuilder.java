/*
 * Copyright 2018 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.servlet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.ibm.websphere.ras.annotation.Trivial;

import io.grpc.ExperimentalApi;
import io.grpc.InternalChannelz.SocketStats;
import io.grpc.InternalInstrumented;
import io.grpc.InternalLogId;
import io.grpc.Server;
import io.grpc.ServerStreamTracer;
import io.grpc.ServerStreamTracer.Factory;
import io.grpc.Status;
import io.grpc.internal.AbstractServerImplBuilder;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.InternalServer;
import io.grpc.internal.ServerListener;
import io.grpc.internal.ServerTransport;
import io.grpc.internal.ServerTransportListener;
import io.grpc.internal.SharedResourceHolder;
import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
//import javax.annotation.concurrent.NotThreadSafe;

/**
 * Builder to build a gRPC server that can run as a servlet. This is for advanced custom settings.
 * Normally, users should consider extending the out-of-box {@link GrpcServlet} directly instead.
 *
 * <p>The API is experimental. The authors would like to know more about the real usecases. Users
 * are welcome to provide feedback by commenting on
 * <a href=https://github.com/grpc/grpc-java/issues/5066>the tracking issue</a>.
 */
@Trivial
@ExperimentalApi("https://github.com/grpc/grpc-java/issues/5066")
//@NotThreadSafe
public final class ServletServerBuilder extends AbstractServerImplBuilder<ServletServerBuilder> {
  List<? extends ServerStreamTracer.Factory> streamTracerFactories;
  int maxInboundMessageSize = DEFAULT_MAX_MESSAGE_SIZE;

  private ScheduledExecutorService scheduler;
  private boolean internalCaller;
  private boolean usingCustomScheduler;
  private InternalServerImpl internalServer;

// TODO uncomment this when the super method is no longer final
//  /**
//   * Builds a gRPC server that can run as a servlet.
//   *
//   * <p>The returned server will not be started or bound to a port.
//   *
//   * <p>Users should not call this method directly. Instead users should call
//   * {@link #buildServletAdapter()} which internally will call {@code build()} and {@code start()}
//   * appropriately.
//   *
//   * @throws IllegalStateException if this method is called by users directly
//   */
//  @Override
//  public Server build() {
//    checkState(internalCaller, "build() method should not be called directly by an application");
//    return super.build();
//  }

  /**
   * Creates a {@link ServletAdapter}.
   */
  public ServletAdapter buildServletAdapter() {
    return new ServletAdapter(buildAndStart(), streamTracerFactories, maxInboundMessageSize);
  }

  private ServerTransportListener buildAndStart() {
    try {
      internalCaller = true;
      build().start();
    } catch (IOException e) {
      // actually this should never happen
      throw new RuntimeException(e);
    } finally {
      internalCaller = false;
    }

    if (!usingCustomScheduler) {
      scheduler = SharedResourceHolder.get(GrpcUtil.TIMER_SERVICE);
    }

    // Create only one "transport" for all requests because it has no knowledge of which request is
    // associated with which client socket. This "transport" does not do socket connection, the
    // container does.
    ServerTransportImpl serverTransport =
        new ServerTransportImpl(scheduler, usingCustomScheduler);
    return internalServer.serverListener.transportCreated(serverTransport);
  }

  @Override
  protected List<? extends InternalServer> buildTransportServers(
      List<? extends Factory> streamTracerFactories) {
    checkNotNull(streamTracerFactories, "streamTracerFactories");
    this.streamTracerFactories = streamTracerFactories;
    internalServer = new InternalServerImpl();
    return ImmutableList.of(internalServer);
  }

  /**
   * Throws {@code UnsupportedOperationException}. TLS should be configured by the servlet
   * container.
   */
  @Override
  public ServletServerBuilder useTransportSecurity(File certChain, File privateKey) {
    throw new UnsupportedOperationException("TLS should be configured by the servlet container");
  }

  @Override
  public ServletServerBuilder maxInboundMessageSize(int bytes) {
    checkArgument(bytes >= 0, "bytes must be >= 0");
    maxInboundMessageSize = bytes;
    return this;
  }

  /**
   * Provides a custom scheduled executor service to the server builder.
   *
   * @return this
   */
  public ServletServerBuilder scheduledExecutorService(ScheduledExecutorService scheduler) {
    this.scheduler = checkNotNull(scheduler, "scheduler");
    usingCustomScheduler = true;
    return this;
  }

  private static final class InternalServerImpl implements InternalServer {

    ServerListener serverListener;

    InternalServerImpl() {}

    @Override
    public void start(ServerListener listener) {
      serverListener = listener;
    }

    @Override
    public void shutdown() {
      if (serverListener != null) {
        serverListener.serverShutdown();
      }
    }

    @Override
    public SocketAddress getListenSocketAddress() {
      return new SocketAddress() {
        @Override
        public String toString() {
          return "ServletServer";
        }
      };
    }

    @Override
    public InternalInstrumented<SocketStats> getListenSocketStats() {
      // sockets are managed by the servlet container, grpc is ignorant of that
      return null;
    }
  }

  @VisibleForTesting
  static final class ServerTransportImpl implements ServerTransport {

    private final InternalLogId logId = InternalLogId.allocate(ServerTransportImpl.class, null);
    private final ScheduledExecutorService scheduler;
    private final boolean usingCustomScheduler;

    ServerTransportImpl(
        ScheduledExecutorService scheduler, boolean usingCustomScheduler) {
      this.scheduler = checkNotNull(scheduler, "scheduler");
      this.usingCustomScheduler = usingCustomScheduler;
    }

    @Override
    public void shutdown() {
      if (!usingCustomScheduler) {
        SharedResourceHolder.release(GrpcUtil.TIMER_SERVICE, scheduler);
      }
    }

    @Override
    public void shutdownNow(Status reason) {
      shutdown();
    }

    @Override
    public ScheduledExecutorService getScheduledExecutorService() {
      return scheduler;
    }

    @Override
    public ListenableFuture<SocketStats> getStats() {
      // does not support instrumentation
      return null;
    }

    @Override
    public InternalLogId getLogId() {
      return logId;
    }
  }
}
