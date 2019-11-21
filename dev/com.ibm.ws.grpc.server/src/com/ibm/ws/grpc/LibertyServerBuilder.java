/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.grpc;

import static com.google.common.base.Preconditions.checkArgument;
import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;
import static io.grpc.internal.GrpcUtil.DEFAULT_SERVER_KEEPALIVE_TIMEOUT_NANOS;
import static io.grpc.internal.GrpcUtil.DEFAULT_SERVER_KEEPALIVE_TIME_NANOS;
import static io.grpc.internal.GrpcUtil.SERVER_KEEPALIVE_TIME_NANOS_DISABLED;

import io.grpc.ServerStreamTracer.Factory;
import io.grpc.internal.AbstractServerImplBuilder;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.KeepAliveManager;
import java.io.File;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;

/**
 * A builder to help simplify the construction of a Liberty-based GRPC server.
 */
public final class LibertyServerBuilder extends AbstractServerImplBuilder<LibertyServerBuilder> {
	public static final int DEFAULT_FLOW_CONTROL_WINDOW = 1048576; // 1MiB

	static final long MAX_CONNECTION_IDLE_NANOS_DISABLED = Long.MAX_VALUE;
	static final long MAX_CONNECTION_AGE_NANOS_DISABLED = Long.MAX_VALUE;
	static final long MAX_CONNECTION_AGE_GRACE_NANOS_INFINITE = Long.MAX_VALUE;

	private static final long MIN_KEEPALIVE_TIME_NANO = TimeUnit.MILLISECONDS.toNanos(1L);
	private static final long MIN_KEEPALIVE_TIMEOUT_NANO = TimeUnit.MICROSECONDS.toNanos(499L);
	private static final long MIN_MAX_CONNECTION_IDLE_NANO = TimeUnit.SECONDS.toNanos(1L);
	private static final long MIN_MAX_CONNECTION_AGE_NANO = TimeUnit.SECONDS.toNanos(1L);
	private static final long AS_LARGE_AS_INFINITE = TimeUnit.DAYS.toNanos(1000L);

	private SocketAddress address;
	private SSLContext sslContext;
	private int maxConcurrentCallsPerConnection = Integer.MAX_VALUE;
	private int flowControlWindow = DEFAULT_FLOW_CONTROL_WINDOW;
	private int maxMessageSize = DEFAULT_MAX_MESSAGE_SIZE;
	private int maxHeaderListSize = GrpcUtil.DEFAULT_MAX_HEADER_LIST_SIZE;
	private long keepAliveTimeInNanos = DEFAULT_SERVER_KEEPALIVE_TIME_NANOS;
	private long keepAliveTimeoutInNanos = DEFAULT_SERVER_KEEPALIVE_TIMEOUT_NANOS;
	private long maxConnectionIdleInNanos = MAX_CONNECTION_IDLE_NANOS_DISABLED;
	private long maxConnectionAgeInNanos = MAX_CONNECTION_AGE_NANOS_DISABLED;
	private long maxConnectionAgeGraceInNanos = MAX_CONNECTION_AGE_GRACE_NANOS_INFINITE;
	private boolean permitKeepAliveWithoutCalls;
	private long permitKeepAliveTimeInNanos = TimeUnit.MINUTES.toNanos(5);

	/**
	 * Creates a server builder that will bind to the given port.
	 *
	 * @param port the port on which the server is to be bound.
	 * @return the server builder.
	 */
	public static LibertyServerBuilder forPort(int port) {
		return new LibertyServerBuilder();
	}

	/**
	 * Creates a server builder configured with the given {@link SocketAddress}.
	 *
	 * @param address the socket address on which the server is to be bound.
	 * @return the server builder
	 */
	public static LibertyServerBuilder forAddress(SocketAddress address) {
		return new LibertyServerBuilder(address);
	}

	public LibertyServerBuilder() {

	}

	public LibertyServerBuilder(SocketAddress address) {
		this.address = address;
	}

	@Override
	protected void setTracingEnabled(boolean value) {
		super.setTracingEnabled(value);
	}

	@Override
	protected void setStatsEnabled(boolean value) {
		super.setStatsEnabled(value);
	}

	@Override
	protected void setStatsRecordStartedRpcs(boolean value) {
		super.setStatsRecordStartedRpcs(value);
	}

	/**
	 * The maximum number of concurrent calls permitted for each incoming
	 * connection. Defaults to no limit.
	 */
	public LibertyServerBuilder maxConcurrentCallsPerConnection(int maxCalls) {
		checkArgument(maxCalls > 0, "max must be positive: %s", maxCalls);
		this.maxConcurrentCallsPerConnection = maxCalls;
		return this;
	}

	/**
	 * Sets the HTTP/2 flow control window. If not called, the default value is
	 * {@link #DEFAULT_FLOW_CONTROL_WINDOW}).
	 */
	public LibertyServerBuilder flowControlWindow(int flowControlWindow) {
		checkArgument(flowControlWindow > 0, "flowControlWindow must be positive");
		this.flowControlWindow = flowControlWindow;
		return this;
	}

	/**
	 * Sets the maximum message size allowed to be received on the server. If not
	 * called, defaults to 4 MiB. The default provides protection to services who
	 * haven't considered the possibility of receiving large messages while trying
	 * to be large enough to not be hit in normal usage.
	 *
	 * @deprecated Call {@link #maxInboundMessageSize} instead. This method will be
	 *             removed in a future release.
	 */
	@Deprecated
	public LibertyServerBuilder maxMessageSize(int maxMessageSize) {
		return maxInboundMessageSize(maxMessageSize);
	}

	/** {@inheritDoc} */
	@Override
	public LibertyServerBuilder maxInboundMessageSize(int bytes) {
		checkArgument(bytes >= 0, "bytes must be >= 0");
		this.maxMessageSize = bytes;
		return this;
	}

	public int getMaxInboundMessageSize() {
		return this.maxMessageSize;
	}

	/**
	 * Sets the maximum size of header list allowed to be received. This is
	 * cumulative size of the headers with some overhead, as defined for
	 * <a href="http://httpwg.org/specs/rfc7540.html#rfc.section.6.5.2"> HTTP/2's
	 * SETTINGS_MAX_HEADER_LIST_SIZE</a>. The default is 8 KiB.
	 */
	public LibertyServerBuilder maxHeaderListSize(int maxHeaderListSize) {
		checkArgument(maxHeaderListSize > 0, "maxHeaderListSize must be > 0");
		this.maxHeaderListSize = maxHeaderListSize;
		return this;
	}

	/**
	 * Sets a custom keepalive time, the delay time for sending next keepalive ping.
	 * An unreasonably small value might be increased, and {@code Long.MAX_VALUE}
	 * nano seconds or an unreasonably large value will disable keepalive.
	 *
	 * @since 1.3.0
	 */
	public LibertyServerBuilder keepAliveTime(long keepAliveTime, TimeUnit timeUnit) {
		checkArgument(keepAliveTime > 0L, "keepalive time must be positive");
		keepAliveTimeInNanos = timeUnit.toNanos(keepAliveTime);
		keepAliveTimeInNanos = KeepAliveManager.clampKeepAliveTimeInNanos(keepAliveTimeInNanos);
		if (keepAliveTimeInNanos >= AS_LARGE_AS_INFINITE) {
			// Bump keepalive time to infinite. This disables keep alive.
			keepAliveTimeInNanos = SERVER_KEEPALIVE_TIME_NANOS_DISABLED;
		}
		if (keepAliveTimeInNanos < MIN_KEEPALIVE_TIME_NANO) {
			// Bump keepalive time.
			keepAliveTimeInNanos = MIN_KEEPALIVE_TIME_NANO;
		}
		return this;
	}

	/**
	 * Sets a custom keepalive timeout, the timeout for keepalive ping requests. An
	 * unreasonably small value might be increased.
	 *
	 * @since 1.3.0
	 */
	public LibertyServerBuilder keepAliveTimeout(long keepAliveTimeout, TimeUnit timeUnit) {
		checkArgument(keepAliveTimeout > 0L, "keepalive timeout must be positive");
		keepAliveTimeoutInNanos = timeUnit.toNanos(keepAliveTimeout);
		keepAliveTimeoutInNanos = KeepAliveManager.clampKeepAliveTimeoutInNanos(keepAliveTimeoutInNanos);
		if (keepAliveTimeoutInNanos < MIN_KEEPALIVE_TIMEOUT_NANO) {
			// Bump keepalive timeout.
			keepAliveTimeoutInNanos = MIN_KEEPALIVE_TIMEOUT_NANO;
		}
		return this;
	}

	/**
	 * Sets a custom max connection idle time, connection being idle for longer than
	 * which will be gracefully terminated. Idleness duration is defined since the
	 * most recent time the number of outstanding RPCs became zero or the connection
	 * establishment. An unreasonably small value might be increased.
	 * {@code Long.MAX_VALUE} nano seconds or an unreasonably large value will
	 * disable max connection idle.
	 *
	 * @since 1.4.0
	 */
	public LibertyServerBuilder maxConnectionIdle(long maxConnectionIdle, TimeUnit timeUnit) {
		checkArgument(maxConnectionIdle > 0L, "max connection idle must be positive");
		maxConnectionIdleInNanos = timeUnit.toNanos(maxConnectionIdle);
		if (maxConnectionIdleInNanos >= AS_LARGE_AS_INFINITE) {
			maxConnectionIdleInNanos = MAX_CONNECTION_IDLE_NANOS_DISABLED;
		}
		if (maxConnectionIdleInNanos < MIN_MAX_CONNECTION_IDLE_NANO) {
			maxConnectionIdleInNanos = MIN_MAX_CONNECTION_IDLE_NANO;
		}
		return this;
	}

	/**
	 * Sets a custom max connection age, connection lasting longer than which will
	 * be gracefully terminated. An unreasonably small value might be increased. A
	 * random jitter of +/-10% will be added to it. {@code Long.MAX_VALUE} nano
	 * seconds or an unreasonably large value will disable max connection age.
	 *
	 * @since 1.3.0
	 */
	public LibertyServerBuilder maxConnectionAge(long maxConnectionAge, TimeUnit timeUnit) {
		checkArgument(maxConnectionAge > 0L, "max connection age must be positive");
		maxConnectionAgeInNanos = timeUnit.toNanos(maxConnectionAge);
		if (maxConnectionAgeInNanos >= AS_LARGE_AS_INFINITE) {
			maxConnectionAgeInNanos = MAX_CONNECTION_AGE_NANOS_DISABLED;
		}
		if (maxConnectionAgeInNanos < MIN_MAX_CONNECTION_AGE_NANO) {
			maxConnectionAgeInNanos = MIN_MAX_CONNECTION_AGE_NANO;
		}
		return this;
	}

	/**
	 * Sets a custom grace time for the graceful connection termination. Once the
	 * max connection age is reached, RPCs have the grace time to complete. RPCs
	 * that do not complete in time will be cancelled, allowing the connection to
	 * terminate. {@code Long.MAX_VALUE} nano seconds or an unreasonably large value
	 * are considered infinite.
	 *
	 * @see #maxConnectionAge(long, TimeUnit)
	 * @since 1.3.0
	 */
	public LibertyServerBuilder maxConnectionAgeGrace(long maxConnectionAgeGrace, TimeUnit timeUnit) {
		checkArgument(maxConnectionAgeGrace >= 0L, "max connection age grace must be non-negative");
		maxConnectionAgeGraceInNanos = timeUnit.toNanos(maxConnectionAgeGrace);
		if (maxConnectionAgeGraceInNanos >= AS_LARGE_AS_INFINITE) {
			maxConnectionAgeGraceInNanos = MAX_CONNECTION_AGE_GRACE_NANOS_INFINITE;
		}
		return this;
	}

	/**
	 * Specify the most aggressive keep-alive time clients are permitted to
	 * configure. The server will try to detect clients exceeding this rate and when
	 * detected will forcefully close the connection. The default is 5 minutes.
	 *
	 * <p>
	 * Even though a default is defined that allows some keep-alives, clients must
	 * not use keep-alive without approval from the service owner. Otherwise, they
	 * may experience failures in the future if the service becomes more
	 * restrictive. When unthrottled, keep-alives can cause a significant amount of
	 * traffic and CPU usage, so clients and servers should be conservative in what
	 * they use and accept.
	 *
	 * @see #permitKeepAliveWithoutCalls(boolean)
	 * @since 1.3.0
	 */
	public LibertyServerBuilder permitKeepAliveTime(long keepAliveTime, TimeUnit timeUnit) {
		checkArgument(keepAliveTime >= 0, "permit keepalive time must be non-negative");
		permitKeepAliveTimeInNanos = timeUnit.toNanos(keepAliveTime);
		return this;
	}

	/**
	 * Sets whether to allow clients to send keep-alive HTTP/2 PINGs even if there
	 * are no outstanding RPCs on the connection. Defaults to {@code false}.
	 *
	 * @see #permitKeepAliveTime(long, TimeUnit)
	 * @since 1.3.0
	 */
	public LibertyServerBuilder permitKeepAliveWithoutCalls(boolean permit) {
		permitKeepAliveWithoutCalls = permit;
		return this;
	}

	@Override
	protected List<? extends LibertyServer> buildTransportServers(List<? extends Factory> streamTracerFactories) {

		// TODO: set the ports we're actually listening on
		List<SocketAddress> listenAddresses = new ArrayList<SocketAddress>();
		listenAddresses.add(address);

		List<LibertyServer> transportServers = new ArrayList<LibertyServer>(1);
		for (SocketAddress listenAddress : listenAddresses) {
			transportServers.add(new LibertyServer(listenAddress));
		}
		return Collections.unmodifiableList(transportServers);
	}

	@Override
	public LibertyServerBuilder useTransportSecurity(File certChain, File privateKey) {
		return null;
	}
}
