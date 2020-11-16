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

import static com.google.common.base.Preconditions.checkNotNull;
import static io.grpc.internal.GrpcUtil.TIMEOUT_KEY;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINEST;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.io.BaseEncoding;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.annotation.Trivial;

import io.grpc.Attributes;
import io.grpc.ExperimentalApi;
import io.grpc.Grpc;
import io.grpc.InternalLogId;
import io.grpc.InternalMetadata;
import io.grpc.Metadata;
import io.grpc.ServerStreamTracer;
import io.grpc.Status;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.ReadableBuffers;
import io.grpc.internal.ServerTransportListener;
import io.grpc.internal.StatsTraceContext;
import io.openliberty.grpc.internal.GrpcMessages;
import io.openliberty.grpc.internal.security.GrpcServerSecurity;
import io.openliberty.grpc.internal.servlet.GrpcServerComponent;
import io.openliberty.grpc.internal.servlet.GrpcServletUtils;

/**
 * An adapter that transforms {@link HttpServletRequest} into gRPC request and lets a gRPC server
 * process it, and transforms the gRPC response into {@link HttpServletResponse}. An adapter can be
 * instantiated by {@link ServletServerBuilder#buildServletAdapter()}.
 *
 * <p>In a servlet, calling {@link #doPost(HttpServletRequest, HttpServletResponse)} inside {@link
 * javax.servlet.http.HttpServlet#doPost(HttpServletRequest, HttpServletResponse)} makes the servlet
 * backed by the gRPC server associated with the adapter. The servlet must support Asynchronous
 * Processing and must be deployed to a container that supports servlet 4.0 and enables HTTP/2.
 *
 * <p>The API is experimental. The authors would like to know more about the real usecases. Users
 * are welcome to provide feedback by commenting on
 * <a href=https://github.com/grpc/grpc-java/issues/5066>the tracking issue</a>.
 */
@Trivial
@ExperimentalApi("https://github.com/grpc/grpc-java/issues/5066")
public final class ServletAdapter {

  private static final String CLASS_NAME = ServletAdapter.class.getName();
  static final Logger logger = Logger.getLogger(CLASS_NAME);
  private static TraceNLS nls = TraceNLS.getTraceNLS(ServletAdapter.class , GrpcMessages.GRPC_BUNDLE);
  
  private final ServerTransportListener transportListener;
  private final List<? extends ServerStreamTracer.Factory> streamTracerFactories;
  private final int maxInboundMessageSize;
  private final Attributes attributes;

  ServletAdapter(
      ServerTransportListener transportListener,
      List<? extends ServerStreamTracer.Factory> streamTracerFactories,
      int maxInboundMessageSize) {
    this.transportListener = transportListener;
    this.streamTracerFactories = streamTracerFactories;
    this.maxInboundMessageSize = maxInboundMessageSize;
    attributes = transportListener.transportReady(Attributes.EMPTY);
  }

  /**
   * Call this method inside {@link javax.servlet.http.HttpServlet#doGet(HttpServletRequest,
   * HttpServletResponse)} to serve gRPC GET request.
   *
   * <p>This method is currently not impelemented.
   *
   * <p>Note that in rare case gRPC client sends GET requests.
   *
   * <p>Do not modify {@code req} and {@code resp} before or after calling this method. However,
   * calling {@code resp.setBufferSize()} before invocation is allowed.
   */
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    // TODO(zdapeng)
  }

  /**
   * Call this method inside {@link javax.servlet.http.HttpServlet#doPost(HttpServletRequest,
   * HttpServletResponse)} to serve gRPC POST request.
   *
   * <p>Do not modify {@code req} and {@code resp} before or after calling this method. However,
   * calling {@code resp.setBufferSize()} before invocation is allowed.
   */
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		InternalLogId logId = InternalLogId.allocate(ServletAdapter.class, null);
		logger.log(FINE, "[{0}] RPC started", logId);

		if (!ServletAdapter.isGrpc(req)) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "the request is not a gRPC request");
			return;
		}

		String method = req.getRequestURI().substring(1); // remove the leading "/"

		// Liberty change: remove application context root from path
		// then perform authentication/authorization
		method = GrpcServletUtils.translateLibertyPath(method);

		if (resp.isCommitted()) {
			// security error might have already occurred
			logger.log(Level.SEVERE, nls.getFormattedMessage("response.already.committed", new Object[] { method },
					"The response has already been committed."));
			logger.log(FINE, "[{0}] RPC exited for service [{1}]", new Object[] { logId, method });
			return;
		}

		boolean libertyAuth = true;
		if (GrpcServerComponent.isSecurityEnabled()) {
			libertyAuth = GrpcServerSecurity.doServletAuth(req, resp, method);
		}

		AsyncContext asyncCtx = req.startAsync(req, resp);

		if (logger.isLoggable(FINEST)) {
			logger.log(FINE, "Liberty inbound gRPC request path translated to {0}", method);
		}

		Metadata headers = getHeaders(req, libertyAuth);

		if (logger.isLoggable(FINEST)) {
			logger.log(FINEST, "[{0}] method: {1}", new Object[] { logId, method });
			logger.log(FINEST, "[{0}] headers: {1}", new Object[] { logId, headers });
		}

		Long timeoutNanos = headers.get(TIMEOUT_KEY);
		if (timeoutNanos == null) {
			timeoutNanos = 0L;
		}
		asyncCtx.setTimeout(TimeUnit.NANOSECONDS.toMillis(timeoutNanos));
		StatsTraceContext statsTraceCtx = StatsTraceContext.newServerContext(streamTracerFactories, method, headers);

		ServletServerStream stream = new ServletServerStream(asyncCtx, statsTraceCtx, maxInboundMessageSize, attributes
				.toBuilder()
				.set(Grpc.TRANSPORT_ATTR_REMOTE_ADDR, new InetSocketAddress(req.getRemoteHost(), req.getRemotePort()))
				.set(Grpc.TRANSPORT_ATTR_LOCAL_ADDR, new InetSocketAddress(req.getLocalAddr(), req.getLocalPort()))
				.build(), getAuthority(req), logId);

		if (logger.isLoggable(FINEST)) {
			logger.log(FINE, "set the listeners on async request {0}", asyncCtx.getRequest());
		}

		asyncCtx.getRequest().getInputStream().setReadListener(new GrpcReadListener(stream, asyncCtx, logId));

		asyncCtx.addListener(new GrpcAsycListener(stream, logId));

		if (logger.isLoggable(FINEST)) {
			logger.log(FINE, "[{0}] the listeners set on async request {1}", new Object[] { logId, asyncCtx.getRequest()});
		}

		transportListener.streamCreated(stream, method, headers);
		stream.transportState().runOnTransportThread(stream.transportState()::onStreamAllocated);
		stream.setWriteListener();
	}

  private static Metadata getHeaders(HttpServletRequest req, boolean libertyAuth) {
    Enumeration<String> headerNames = req.getHeaderNames();
    checkNotNull(
        headerNames, "Servlet container does not allow HttpServletRequest.getHeaderNames()");
    List<byte[]> byteArrays = new ArrayList<>();
    while (headerNames.hasMoreElements()) {
      String headerName = headerNames.nextElement();
      Enumeration<String> values = req.getHeaders(headerName);
      if (values == null) {
        continue;
      }
      while (values.hasMoreElements()) {
        String value = values.nextElement();
        if (headerName.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
          byteArrays.add(headerName.getBytes(StandardCharsets.US_ASCII));
          byteArrays.add(BaseEncoding.base64().decode(value));
        } else {
          byteArrays.add(headerName.getBytes(StandardCharsets.US_ASCII));
          byteArrays.add(value.getBytes(StandardCharsets.US_ASCII));
        }
      }
    }

    // liberty change: add result of authorization to headers
    GrpcServerSecurity.addLibertyAuthHeader(byteArrays, req, libertyAuth);

    return InternalMetadata.newMetadata(byteArrays.toArray(new byte[][]{}));
  }

  private static String getAuthority(HttpServletRequest req) {
    try {
      return new URI(req.getRequestURL().toString()).getAuthority();
    } catch (URISyntaxException e) {
      logger.log(FINE, "Error getting authority from the request URL {0}" + req.getRequestURL());
      return req.getServerName() + ":" + req.getServerPort();
    }
  }

  /**
   * Call this method when the adapter is no longer needed. The gRPC server will be terminated.
   */
  public void destroy() {
    transportListener.transportTerminated();
  }

  private static final class GrpcAsycListener implements AsyncListener {
    final InternalLogId logId;
    final ServletServerStream stream;

    GrpcAsycListener(ServletServerStream stream, InternalLogId logId) {
      this.stream = stream;
      this.logId = logId;
    }

    @Override
    public void onComplete(AsyncEvent event) {}

    @Override
    public void onTimeout(AsyncEvent event) {
      if (logger.isLoggable(FINE)) {
        logger.log(FINE, String.format("[{%s}] Timeout: ", logId), event.getThrowable());
      }
      // If the resp is not committed, cancel() to avoid being redirected to an error page.
      // Else, the container will send RST_STREAM in the end.
      if (!event.getAsyncContext().getResponse().isCommitted()) {
        stream.cancel(Status.DEADLINE_EXCEEDED);
      } else {
        stream.transportState().runOnTransportThread(
            () -> stream.transportState().transportReportStatus(Status.DEADLINE_EXCEEDED));
      }
    }

    @Override
    public void onError(AsyncEvent event) {
      if (logger.isLoggable(FINE)) {
        logger.log(FINE, String.format("[{%s}] Error: ", logId), event.getThrowable());
      }

      // If the resp is not committed, cancel() to avoid being redirected to an error page.
      // Else, the container will send RST_STREAM at the end.
      if (!event.getAsyncContext().getResponse().isCommitted()) {
        stream.cancel(Status.fromThrowable(event.getThrowable()));
      } else {
        stream.transportState().runOnTransportThread(
            () -> stream.transportState().transportReportStatus(
                Status.fromThrowable(event.getThrowable())));
      }
    }

    @Override
    public void onStartAsync(AsyncEvent event) {}
  }

  private static final class GrpcReadListener implements ReadListener {
    final ServletServerStream stream;
    final AsyncContext asyncCtx;
    final ServletInputStream input;
    final InternalLogId logId;

    GrpcReadListener(
        ServletServerStream stream,
        AsyncContext asyncCtx,
        InternalLogId logId) throws IOException {
      this.stream = stream;
      this.asyncCtx = asyncCtx;
      input = asyncCtx.getRequest().getInputStream();
      this.logId = logId;
    }

    final byte[] buffer = new byte[4 * 1024];
    
    @Override
    public void onDataAvailable() throws IOException {
      logger.log(FINEST, "[{0}] onDataAvailable: ENTRY", logId);

      while (input.isReady()) {
        int length = input.read(buffer);
        if (length == -1) {
          logger.log(FINEST, "[{0}] inbound data: read end of stream", logId);
          return;
        } else {
          if (logger.isLoggable(FINEST)) {
            logger.log(
                FINEST,
                "[{0}] inbound data: length = {1}, bytes = {2}",
                new Object[] {logId, length, ServletServerStream.toHexString(buffer, length)});
          }
           byte[] copy = Arrays.copyOf(buffer, length);
          stream.transportState().runOnTransportThread(
              () -> stream.transportState().inboundDataReceived(ReadableBuffers.wrap(copy), false));
        }
      }
      logger.log(FINEST, "[{0}] onDataAvailable: EXIT", logId);
    }

    @Override
    public void onAllDataRead() {
      logger.log(FINE, "[{0}] onAllDataRead", logId);
      stream.transportState().runOnTransportThread(() ->
          stream.transportState().inboundDataReceived(ReadableBuffers.wrap(new byte[] {}), true));
    }

    @Override
    public void onError(Throwable t) {
      if (logger.isLoggable(FINE)) {
        logger.log(FINE, String.format("[{%s}] Error: ", logId), t);
      }
      // If the resp is not committed, cancel() to avoid being redirected to an error page.
      // Else, the container will send RST_STREAM at the end.
      if (!asyncCtx.getResponse().isCommitted()) {
        stream.cancel(Status.fromThrowable(t));
      } else {
        stream.transportState().runOnTransportThread(
            () -> stream.transportState()
                .transportReportStatus(Status.fromThrowable(t)));
      }
    }
  }

  /**
   * Checks whether an incoming {@code HttpServletRequest} may come from a gRPC client.
   *
   * @return true if the request comes from a gRPC client
   */
  public static boolean isGrpc(HttpServletRequest request) {
    return request.getContentType() != null
        && request.getContentType().contains(GrpcUtil.CONTENT_TYPE_GRPC);
  }
}
