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
import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.ibm.websphere.ras.annotation.Trivial;

import io.grpc.InternalChannelz.SocketStats;
import io.grpc.InternalInstrumented;
import io.grpc.InternalLogId;
import io.grpc.ServerBuilder;
import io.grpc.ServerStreamTracer;
import io.grpc.ServerStreamTracer.Factory;
import io.grpc.Status;
import io.grpc.internal.AbstractServerImplBuilder;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.InternalServer;
import io.grpc.internal.ServerImplBuilder;
import io.grpc.internal.ServerImplBuilder.ClientTransportServersBuilder;
import io.grpc.internal.ServerListener;
import io.grpc.internal.ServerTransport;
import io.grpc.internal.ServerTransportListener;
import io.grpc.internal.SharedResourceHolder;
import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Builder to build a gRPC server that can run as a servlet.
 * See https://github.com/grpc/grpc-java/issues/5066
 */
@Trivial
public final class ServletServerBuilder extends AbstractServerImplBuilder<ServletServerBuilder> {
  List<? extends ServerStreamTracer.Factory> streamTracerFactories;
  int maxInboundMessageSize = DEFAULT_MAX_MESSAGE_SIZE;

  private ScheduledExecutorService scheduler;
  private boolean usingCustomScheduler;
  private InternalServerImpl internalServer;
  private ServerImplBuilder builder;

  public ServletServerBuilder() {
    builder = new ServerImplBuilder(
      new ClientTransportServersBuilder() {
        @Override
        public InternalServer buildClientTransportServers(
            List<? extends ServerStreamTracer.Factory> streamTracerFactories) {
          return buildTransportServers(streamTracerFactories);
        }
      }
    );
  }

  @Override
  protected ServerBuilder<?> delegate() {
    return builder;
  }

  /**
   * Creates a {@link ServletAdapter}.
   */
  public ServletAdapter buildServletAdapter() {
    return new ServletAdapter(buildAndStart(), streamTracerFactories, maxInboundMessageSize);
  }

  private ServerTransportListener buildAndStart() {
    try {
      builder.build().start();
    } catch (IOException e) {
      throw new RuntimeException(e);
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

  private InternalServer buildTransportServers(
      List<? extends Factory> streamTracerFactories) {
    checkNotNull(streamTracerFactories, "streamTracerFactories");
    this.streamTracerFactories = streamTracerFactories;
    return internalServer = new InternalServerImpl();
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


    @Override
    public List<? extends SocketAddress> getListenSocketAddresses() {
        return Collections.singletonList(getListenSocketAddress());
    }

    @Override
    public List<InternalInstrumented<SocketStats>> getListenSocketStatsList() {
        // sockets are managed by the servlet container
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
