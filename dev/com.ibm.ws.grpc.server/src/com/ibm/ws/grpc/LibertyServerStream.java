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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.http.ee8.Http2Connection;
import com.ibm.wsspi.http.ee8.Http2Stream;
import com.ibm.wsspi.http.ee8.Http2StreamHandler;
import com.ibm.websphere.channelfw.osgi.CHFWBundle;

import io.grpc.InternalMetadata;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.internal.AbstractServerStream;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.StatsTraceContext;
import io.grpc.internal.TransportTracer;
import io.grpc.internal.WritableBuffer;
import io.perfmark.PerfMark;
import io.perfmark.Tag;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server stream for a Liberty HTTP2 transport.
 */
class LibertyServerStream extends AbstractServerStream implements Http2StreamHandler {
	private static final Logger log = Logger.getLogger(LibertyServerStream.class.getName());

	private final Http2Stream h2sp;
	private final Http2Connection h2connection;

	private final Sink sink;
	private final String authority;

	private TransportState transportState;

	private volatile CountDownLatch WriteWaitLatch = new CountDownLatch(1) {};

	public LibertyServerStream(final Http2Stream stream, final Http2Connection connection) {

		super(new LibertyWritableBufferAllocator(), StatsTraceContext.NOOP);
		this.h2sp = stream;
		this.h2connection = connection;
		this.authority = h2connection.getAuthority();

		sink = new Sink(stream);
		transportState = new TransportState(h2sp, Integer.MAX_VALUE, statsTraceContext(), new TransportTracer(),
				"LibertyServerStream");
	}

	@Override
	protected Sink abstractServerStreamSink() {
		return sink;
	}

	@Override
	public String getAuthority() {
		return authority;
	}

	private class Sink implements AbstractServerStream.Sink {

		final Http2Stream stream;

		public Sink(Http2Stream s) {
			stream = s;
		}

		@Override
		public void request(final int numMessages) {
			try {
				WriteWaitLatch.await();
				transportState.requestMessagesFromDeframer(numMessages);
			} catch (InterruptedException e) {
				log.log(Level.FINE, "caught InterruptedException waiting for data read", e);
			}
		}

		@Override
		public void writeHeaders(Metadata headers) {

			headers.discardAll(GrpcUtil.CONTENT_TYPE_KEY);
			headers.discardAll(GrpcUtil.TE_HEADER);
			headers.discardAll(GrpcUtil.USER_AGENT_KEY);

			Map<String, String> pseudoHeaderMap = new HashMap<String, String>();
			pseudoHeaderMap.put(Utils.STATUS, Utils.STATUS_OK);

			Map<String, String> headerMap = new HashMap<String, String>();
			headerMap.put(Utils.CONTENT_TYPE_HEADER, Utils.CONTENT_TYPE_GRPC);

			byte[][] data = InternalMetadata.serialize(headers);
			for (int i = 0; i < data.length; i += 2) {
				String headerName = new String(data[i]);
				String headerValue = new String(data[i + 1]);
				headerMap.put(headerName, headerValue);
			}
			stream.writeHeaders(pseudoHeaderMap, headerMap, false, true);
		}

		@Override
		public void writeFrame(WritableBuffer frame, boolean flush, final int numMessages) {
			Preconditions.checkArgument(numMessages >= 0);
			// TODO: we're flushing immediately, should we queue?
			if (frame == null) {
				return;
			}
			LibertyWritableBuffer bytebuf = ((LibertyWritableBuffer) frame);
			final int numBytes = bytebuf.readableBytes();
			// Add the bytes to outbound flow control.
			onSendingBytes(numBytes);
			byte[] toSend = new byte[numBytes];
			bytebuf.bytebuf().flip();
			bytebuf.bytebuf().get(toSend);
			stream.writeData(toSend, false);
		}

		@Override
		public void writeTrailers(Metadata trailers, boolean headersSent, Status status) {
			if (trailers != null) {
				Map<String, String> headers = new HashMap<String, String>();
				byte[][] data = InternalMetadata.serialize(trailers);
				for (int i = 0; i < data.length; i += 2) {
					String headerName = new String(data[i]);
					String headerValue = new String(data[i + 1]);
					headers.put(headerName, headerValue);
				}
				stream.writeTrailers(headers);
			}
		}

		@Override
		public void cancel(Status status) {
			stream.cancel(status.asException(), status.getCode().value());
		}
	}

	public void onDataRead(int streamId, WsByteBuffer data, boolean endOfStream) {
		try {
			PerfMark.startTask("LibertyServerHandler.onDataRead", transportState.tag());
			try {
				transportState.inboundDataReceived(data, endOfStream);
			} finally {
				PerfMark.stopTask("LibertyServerHandler.onDataRead", transportState.tag());
			}
		} catch (Throwable e) {
			// TODO: close stream
			log.log(Level.FINE, "inboundDataReceived caught an excpetion", e);
		}
	}

	/** This should only called from the transport thread. */
	public static class TransportState extends AbstractServerStream.TransportState {

		private final Http2Stream http2Stream;
		private final Tag tag;

		public TransportState(Http2Stream http2Stream, int maxMessageSize, StatsTraceContext statsTraceCtx,
				TransportTracer transportTracer, String methodName) {
			super(maxMessageSize, statsTraceCtx, transportTracer);
			this.http2Stream = checkNotNull(http2Stream, "http2Stream");
			this.tag = PerfMark.createTag(methodName, http2Stream.getId());
		}

		@Override
		public void runOnTransportThread(final Runnable r) {
			CHFWBundle.getScheduledExecutorService().execute(r);
		}

		@Override
		public void bytesRead(int processedBytes) {
		}

		@Override
		public void deframeFailed(Throwable cause) {
			log.log(Level.FINE, "Error processing message", cause);
			int errorCode = Status.fromThrowable(cause).getCode().value();
			this.http2Stream.cancel(new Exception(cause), errorCode);
		}

		void inboundDataReceived(WsByteBuffer frame, boolean endOfStream) {
			super.inboundDataReceived(new LibertyReadableBuffer(frame), endOfStream);
		}

		public int id() {
			return http2Stream.getId();
		}

		public Tag tag() {
			return this.tag;
		}
	}

	@Override
	public int streamId() {
		return h2sp.getId();
	}

	@Override
	protected TransportState transportState() {
		return this.transportState;
	}

	@Override
	public void headersReady() {
	}

	@Override
	public void dataReady(WsByteBuffer buffer, boolean endOfStream) {
		try {
		    this.onDataRead(h2sp.getId(), buffer, endOfStream);
		} finally {
			WriteWaitLatch.countDown();
		}
	}

	@Override
	public void writeHeaders(Map<String, String> headers, boolean endOfHeaders, boolean endOfStream) {
	}
}
