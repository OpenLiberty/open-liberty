/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.utils.jaeger;

import static java.lang.Byte.toUnsignedInt;
import static java.lang.Integer.toHexString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;

import com.google.protobuf.ByteString;
import com.ibm.websphere.simplicity.log.Log;

import io.grpc.Channel;
import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import io.grpc.TlsChannelCredentials;
import io.jaegertracing.api_v2.Model.Span;
import io.jaegertracing.api_v2.Query.FindTracesRequest;
import io.jaegertracing.api_v2.Query.GetServicesRequest;
import io.jaegertracing.api_v2.Query.GetServicesResponse;
import io.jaegertracing.api_v2.Query.GetTraceRequest;
import io.jaegertracing.api_v2.Query.SpansResponseChunk;
import io.jaegertracing.api_v2.Query.TraceQueryParameters;
import io.jaegertracing.api_v2.QueryServiceGrpc;
import io.jaegertracing.api_v2.QueryServiceGrpc.QueryServiceBlockingStub;

/**
 * A client to query spans from Jaeger
 * <p>
 * This simplifies common test operations when compared with using {@link QueryServiceBlockingStub} directly
 */
public class JaegerQueryClient implements AutoCloseable {

    private static final Class<JaegerQueryClient> c = JaegerQueryClient.class;

    private final String host;
    private final int port;
    private final File certificateFile;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private QueryServiceBlockingStub client;

    public JaegerQueryClient(JaegerContainer container, File certificateFile) {
        this(container.getHost(), container.getQueryGrpcPort(), certificateFile);
    }

    public JaegerQueryClient(String host, int port, File certificateFile) {
        super();
        this.host = host;
        this.port = port;
        this.certificateFile = certificateFile;
    }

    public QueryServiceBlockingStub getRawClient() throws IOException {
        synchronized (this) {
            if (client == null) {
                ChannelCredentials creds = TlsChannelCredentials.newBuilder()
                                                                .trustManager(certificateFile)
                                                                .build();
                ManagedChannel channel = Grpc.newChannelBuilderForAddress(host, port, creds).build();
                client = QueryServiceGrpc.newBlockingStub(channel);
            }
            return client;
        }
    }

    /**
     * Query Jaeger for spans for the given traceId
     * <p>
     * There can be a delay in spans being reported to Jaeger, so usually in tests, you'll want to use {@link #waitForSpansForTraceId(String, Matcher)} instead
     *
     * @param traceId the traceId as a hex string
     * @return the spans
     */
    public List<Span> getSpansForTraceId(String traceId) {
        return getSpansForTraceId(convertTraceId(traceId));
    }

    /**
     * Query Jaeger for spans for the given traceId
     *
     * @param traceId the traceId as a ByteString
     * @return the spans
     */
    public List<Span> getSpansForTraceId(ByteString traceId) {
        return runWithRetryAndTimeout(() -> {
            try {
                Log.info(c, "getSpansForTraceId", "Starting Jaeger query");
                GetTraceRequest req = GetTraceRequest.newBuilder().setTraceId(traceId).build();
                Log.info(c, "GetTraceRequest.newBuilder().setTraceId(" + traceId + ").build()", req.toString());
                Iterator<SpansResponseChunk> result = getRawClient().getTrace(req);
                Log.info(c, "getRawClient().getTrace(req)", result.toString());
                List<Span> spans = consumeChunkedResult(result, chunk -> chunk.getSpansList());
                Log.info(c, "getSpansForTraceId", "Returning spans");
                return spans;
            } catch (StatusRuntimeException ex) {
                // If the traceId was not found, there are no spans for that traceId
                // return an empty list rather than throwing an exception
                if (ex.getStatus().getCode() == Code.NOT_FOUND) {
                    Log.info(c, "getSpansForTraceId", "No spans found");
                    return Collections.emptyList();
                } else {
                    throw ex;
                }
            }
        }, TIMEOUT);
    }

    /**
     * Retrieve all the spans for a given service name
     *
     * @param serviceName the service name
     * @return the list of spans
     */
    public List<Span> getSpansForServiceName(String serviceName) {
        return runWithRetryAndTimeout(() -> {
            Log.info(c, "getSpansForServiceName", "Starting Jaeger query");
            TraceQueryParameters params = TraceQueryParameters.newBuilder().setServiceName(serviceName).build();
            Log.info(c, "getSpansForServiceName", "TraceQueryParameters " + params.toString());
            FindTracesRequest req = FindTracesRequest.newBuilder().setQuery(params).build();
            Log.info(c, "getSpansForServiceName", "FindTracesRequest " + req.toString());
            Iterator<SpansResponseChunk> result = getRawClient().findTraces(req);
            Log.info(c, "getSpansForServiceName", "Iterator<SpansResponseChunk> result " + result.toString());
            List<Span> spans = consumeChunkedResult(result, chunk -> chunk.getSpansList());
            Log.info(c, "getSpansForServiceName", "Returning spans");
            return spans;
        }, TIMEOUT);
    }

    /**
     * Retrieve all service names
     */
    public List<String> getServices() {
        return runWithRetryAndTimeout(() -> {
            Log.info(c, "getServices", "Starting Jaeger query");
            GetServicesRequest req = GetServicesRequest.getDefaultInstance();
            GetServicesResponse resp = getRawClient().getServices(req);
            List<ByteString> services = resp.getServicesList().asByteStringList();
            Log.info(c, "getServices", "Returning service names");
            return services.stream()
                           .map(ByteString::toStringUtf8)
                           .collect(Collectors.toList());
        }, TIMEOUT);
    }

    /**
     * Wait until the list of spans matching the given traceId meets the waitCondition
     * <p>
     * This should be used when waiting for the expected spans from the server to appear in Jaeger.
     * <p>
     * Example:
     *
     * <pre>
     * client.waitForSpansForTraceId(testTraceId, hasSize(3))
     * </pre>
     *
     * @param traceId the traceId as a string of hex characters
     * @param waitCondition the condition to wait for
     * @return the list of spans
     */
    public List<Span> waitForSpansForTraceId(String traceId, Matcher<? super List<Span>> waitCondition) {
        List<Span> result = null;

        int retryCount = 0;

        while (retryCount < 3) {
            retryCount += 1;
            Timeout timeout = new Timeout(Duration.ofSeconds(10));
            try {
                while (true) {
                    result = getSpansForTraceId(convertTraceId(traceId));
                    if (timeout.isExpired()) {
                        // Time is up, retry
                        Log.info(c, "waitForSpansForTraceId", "Spans did not match: " + result);
                        break;
                    }
                    if (waitCondition.matches(result)) {
                        Log.info(c, "waitForSpansForTraceId", "Waited " + timeout.getTimePassed() + " for spans to arrive");

                        // Wait additional time to allow more spans to arrive which would invalidate the match
                        // E.g. if we're waiting for 2 spans, check that we don't end up with 3 after waiting a while longer
                        Thread.sleep(500);
                        assertThat("Spans did not match after waiting additional time", result, waitCondition);

                        return result;
                    }
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                throw new AssertionError("Interrupted while waiting for spans", e);
            }
        }
        assertThat("Spans not found within timeout after 3 retries: " + result, result, waitCondition);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws Exception {
        if (client != null) {
            Channel channel = client.getChannel();
            if (channel instanceof ManagedChannel) {
                ManagedChannel managedChannel = (ManagedChannel) channel;
                managedChannel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Convert a traceId from a string of hex characters to a ByteString
     *
     * @param traceIdHexString the traceId as a hex string
     * @return the traceId as a {@code ByteString}
     */
    public static ByteString convertTraceId(String traceIdHexString) {
        byte[] bytes = new byte[traceIdHexString.length() / 2];
        for (int i = 0; i < traceIdHexString.length(); i += 2) {
            byte b = (byte) Integer.parseInt(traceIdHexString.substring(i, i + 2), 16);
            bytes[i / 2] = b;
        }
        return ByteString.copyFrom(bytes);
    }

    /**
     * Convert a byteString into a string of lowercase hex characters
     * <p>
     * This is the reverse of {@link #convertTraceId(String)}
     *
     * @param byteString the {@code ByteString} to convert
     * @return the bytes as a hex string
     */
    public static String convertByteString(ByteString byteString) {
        ByteBuffer bytes = byteString.asReadOnlyByteBuffer();
        StringBuilder result = new StringBuilder();
        while (bytes.hasRemaining()) {
            byte b = bytes.get();
            String hex = toHexString(toUnsignedInt(b));
            if (hex.length() == 1) {
                result.append("0");
            }
            result.append(hex);
        }
        return result.toString();
    }

    /**
     * Consume a chunked result from the jaeger client
     * <p>
     * This will block until all results are consumed
     *
     * @param <Chunk> the chunk type (e.g. SpansResponseChunk)
     * @param <T> the result type (e.g. Span)
     * @param chunkIterator the iterator to consume chunks from
     * @param extractFunction the function to extract a list of results from a chunk
     * @return the list of results
     */
    private static <Chunk, T> List<T> consumeChunkedResult(Iterator<Chunk> chunkIterator, Function<Chunk, List<T>> extractFunction) {
        List<T> result = new ArrayList<>();
        while (chunkIterator.hasNext()) {
            result.addAll(extractFunction.apply(chunkIterator.next()));
        }
        return result;
    }

    /**
     * Run an action asynchronously with a timeout, retrying up to three times if it times out.
     * <p>
     * Useful for methods which use the raw client to talk to Jaeger
     *
     * @param <T> the return type of the action
     * @param action the action to run
     * @param timeout the timeout for each retry
     * @return the result of the action
     * @throws RuntimeException if the action throws an exception or times out three times
     */
    private static <T> T runWithRetryAndTimeout(Callable<T> action, Duration timeout) {
        int retryCount = 0;

        while (retryCount < 5) {
            Log.info(c, "runWithRetryandTimeout", "retryCount " + retryCount);
            retryCount += 1;
            //get executOR
            ExecutorService e = Executors.newSingleThreadExecutor();
            Future<T> future = e.submit(action);
            //
            try {
                return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException e1) {
                future.cancel(true); //interrupt the task if it's still running
                throw new RuntimeException("Action throw an exception ", e1);
            } catch (TimeoutException e1) {
                future.cancel(true);
            } finally {
                e.shutdown();
            }

        }
        throw new RuntimeException("Action timeout");
    }

    private static class Timeout {
        private final long start;
        private final Duration timeLimit;

        public Timeout(Duration timeout) {
            this.timeLimit = timeout;
            start = System.nanoTime();
        }

        public boolean isExpired() {
            return getTimePassed().compareTo(timeLimit) > 0;
        }

        public Duration getTimePassed() {
            return Duration.ofNanos(System.nanoTime() - start);
        }
    }

}
