package com.ibm.ws.netty.handler;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.HashedWheelTimer;
import io.openliberty.http.netty.timeout.TimeoutHandler;
import io.openliberty.http.netty.timeout.TimeoutHandler.WriteTimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class TimeoutHandlerTests {

    private HashedWheelTimer timer;
    private EmbeddedChannel channel;
    private HttpChannelConfig config;

    private TimeoutHandler timeoutHandler;
    private SimpleHttpTimeoutTestHandler testHandler;

    @Before
    public void setup(){
        timer = new HashedWheelTimer(50, TimeUnit.MILLISECONDS, 512);
        channel = new EmbeddedChannel();
        channel.config().setAutoRead(false);

        config = mock(HttpChannelConfig.class);
    }

    @After 
    public void teardown(){
        if(channel.isOpen()){
            channel.close();
        }
        timer.stop();
    }

    private void configureHttpOptions(int readTimeout, int writeTimeout, int persistTimeout, boolean keepAliveEnabled){
        when(config.getReadTimeout()).thenReturn(readTimeout);
        when(config.getWriteTimeout()).thenReturn(writeTimeout);
        when(config.getPersistTimeout()).thenReturn(persistTimeout);
        when(config.isKeepAliveEnabled()).thenReturn(keepAliveEnabled);
    }

    private void configureChannel(int readTimeout, int writeTimeout, int persistTimeout, boolean keepAliveEnabled){
        configureHttpOptions(readTimeout, writeTimeout, persistTimeout, keepAliveEnabled);
        timeoutHandler = new TimeoutHandler(timer, config);
        testHandler = new SimpleHttpTimeoutTestHandler();


        channel.pipeline().addLast(timeoutHandler);
        channel.pipeline().addLast(testHandler);
    }

    @Test 
    public void testReadTimeout() throws Exception{
        configureChannel(1, 5, 5, true); // read=1

        channel.pipeline().fireChannelActive();

        Thread.sleep(1500);
        channel.runScheduledPendingTasks();
        channel.runPendingTasks();

        Throwable t = null;
        try {
            channel.checkException();
        } catch (Exception e) {
            t = e;
        }

        assertThat(t, instanceOf(TimeoutHandler.ReadTimeoutException.class));
    }

    @Test 
    public void testReadTimeoutWithData() throws Exception {
        configureChannel(1, 5, 5, true);
        channel.pipeline().fireChannelActive();

        channel.writeInbound(Unpooled.copiedBuffer("testReadTimeoutWithData", StandardCharsets.UTF_8));

        Thread.sleep(1500);
        channel.runScheduledPendingTasks();
        channel.runPendingTasks();

        assertTrue("Channel should be open if data was read", channel.isActive());
        assertNull("No exceptions should be thrown", testHandler.lastException());

        String inboundData = testHandler.getInboundData();
        assertThat(inboundData, containsString("testReadTimeoutWithData"));

    }

    @Test 
    public void testWriteTimeout() throws Exception{
        TimeLog.log("===== BEGIN test Write Timeout =====");
        
        configureChannel(5, 1, 5, true);
        channel.pipeline().addFirst(new StuckWriteHandler());

        channel.pipeline().fireChannelActive();
        channel.writeInbound(Unpooled.copiedBuffer("HTTP Test Request 1", StandardCharsets.UTF_8));
        channel.runPendingTasks();
        TimeLog.log("writeInbound called, about to sleep");

        Thread.sleep(3000);
        TimeLog.log("After sleep of 3 seconds, expecting writeTimeout here");
        channel.runScheduledPendingTasks();
        channel.runPendingTasks();

        assertFalse("Channel closed after write-timeout", channel.isActive());
        Throwable exception = testHandler.lastException();
        assertThat(exception, instanceOf(WriteTimeoutException.class));
        TimeLog.log("===== END test Write Timeout =====");
    }

    @Test
    public void testPersistTimeoutTriggers() throws Exception {
        TimeLog.log("===== BEGIN testPersistTimeoutTriggers =====");

        // read=1, write=1, persist=2, keepAlive=true
        configureChannel(1, 1, 2, true);
        channel.pipeline().fireChannelActive();

        // Send first inbound => skip read-timeout
        channel.writeInbound(Unpooled.copiedBuffer("First Request", StandardCharsets.UTF_8));
        channel.runPendingTasks();

        // The SimpleHttpTimeoutTestHandler writes a response and calls beginPersistRead().
        // Persist-timeout=2s is now ticking.

        TimeLog.log("Sleeping 3 seconds => expecting persist timeout to fire (2s) if no second request arrives");
        Thread.sleep(3000);

        channel.runScheduledPendingTasks();
        channel.runPendingTasks();

        // The channel should close due to persist-timeout
        assertFalse("Channel closed after persist-timeout", channel.isActive());

        // Check exception
        Throwable exception = testHandler.lastException();
        TimeLog.log("persistTimeout exception=" + exception);
        assertThat("Expected PersistTimeoutExeption", exception,
                   instanceOf(TimeoutHandler.PersistTimeoutExeption.class));

        TimeLog.log("===== END testPersistTimeoutTriggers =====");
    }

    @Test
    public void testPersistTimeoutCancelledIfNextRequestArrives() throws Exception {
        TimeLog.log("===== BEGIN testPersistTimeoutCancelledIfNextRequestArrives =====");

        // read=1, write=1, persist=2, keepAlive=true
        configureChannel(1, 1, 2, true);
        channel.pipeline().fireChannelActive();

        // Send first inbound => skip read-timeout
        channel.writeInbound(Unpooled.copiedBuffer("First Request", StandardCharsets.UTF_8));
        channel.runPendingTasks();

        // The handler writes response & calls beginPersistRead => 2s persist-timeout

        TimeLog.log("Sleeping 1 second (less than persist=2s) then sending second request");
        Thread.sleep(1000);

        // send second inbound request => cancels persist-timeout
        channel.writeInbound(Unpooled.copiedBuffer("Second Request", StandardCharsets.UTF_8));
        channel.runPendingTasks();

        // Sleep longer than 2s total from first
        // If persist-timeout was truly cancelled by second request, no exception
        TimeLog.log("Sleeping another 2.5s => expecting NO persist-timeout since second request arrived");
        Thread.sleep(2500);

        channel.runScheduledPendingTasks();
        channel.runPendingTasks();

        // Should remain open
        assertTrue("Channel should remain open, persist-timeout cancelled by second request", channel.isActive());
        assertNull("No exception expected", testHandler.lastException());

        // Confirm inbound data arrived
        String inbound = testHandler.getInboundData();
        assertThat(inbound, containsString("First Request"));
        assertThat(inbound, containsString("Second Request"));

        TimeLog.log("===== END testPersistTimeoutCancelledIfNextRequestArrives =====");
    }

    @Test
    public void testMultipleSequentialRequests() throws Exception {
        configureChannel(1, 1, 1, true); // read=1, write=1, persist=1
        channel.pipeline().fireChannelActive();

        channel.writeInbound(Unpooled.copiedBuffer("Req#1", StandardCharsets.UTF_8));
        channel.runPendingTasks();
        Thread.sleep(500);
        channel.runScheduledPendingTasks();
        channel.runPendingTasks();

        channel.writeInbound(Unpooled.copiedBuffer("Req#2", StandardCharsets.UTF_8));
        channel.runPendingTasks();
        Thread.sleep(500);
        channel.runScheduledPendingTasks();
        channel.runPendingTasks();

        channel.writeInbound(Unpooled.copiedBuffer("Req#3", StandardCharsets.UTF_8));
        channel.runPendingTasks();
        Thread.sleep(500);
        channel.runScheduledPendingTasks();
        channel.runPendingTasks();

        Thread.sleep(1200);
        channel.runScheduledPendingTasks();
        channel.runPendingTasks();

        assertFalse("Channel closed after no 4th request => persist-timeout", channel.isActive());
        Throwable ex = testHandler.lastException();
        assertThat(ex, instanceOf(TimeoutHandler.PersistTimeoutException.class));

        String inbound = testHandler.getInboundData();
        assertThat(inbound, containsString("Req#1"));
        assertThat(inbound, containsString("Req#2"));
        assertThat(inbound, containsString("Req#3"));
    }

    @Test
    public void testPartialMultipleWrites() throws Exception {
        configureChannel(5, 2, 5, true); 
        channel.pipeline().addAfter("timeoutHandler", "stuckWrite", new StuckWriteHandler());

        channel.pipeline().fireChannelActive();

        channel.writeOutbound(Unpooled.copiedBuffer("FirstWriteCompletes", StandardCharsets.UTF_8));
        channel.flushOutbound(); // ensures the promise completes
        Thread.sleep(200);
        channel.runScheduledPendingTasks();
        channel.runPendingTasks();

        channel.writeOutbound(Unpooled.copiedBuffer("SecondWriteStuck", StandardCharsets.UTF_8));
        channel.runPendingTasks();

        Thread.sleep(2500);
        channel.runScheduledPendingTasks();
        channel.runPendingTasks();

        assertFalse("Channel closed by second write-timeout", channel.isActive());
        Throwable ex = testHandler.lastException();
        assertThat(ex, instanceOf(TimeoutHandler.WriteTimeoutException.class));
    }

    @Test
    public void testMixedReadWriteTimeout() throws Exception {
        configureChannel(1, 1, 5, true);
        channel.pipeline().addAfter("timeoutHandler", "stuckWrite", new StuckWriteHandler());

        channel.pipeline().fireChannelActive();

        channel.writeInbound(Unpooled.copiedBuffer("Inbound Data", StandardCharsets.UTF_8));
        channel.runPendingTasks();

        Thread.sleep(1500);
        channel.runScheduledPendingTasks();
        channel.runPendingTasks();

        assertFalse("Channel closed by write-timeout", channel.isActive());
        Throwable ex = testHandler.lastException();
        assertThat(ex, instanceOf(TimeoutHandler.WriteTimeoutException.class));
    }

    @Test
    public void testReadTimeoutInfinite() throws Exception {
        configureChannel(-1, 1, 5, true);
        channel.pipeline().fireChannelActive();

        Thread.sleep(1200);
        channel.runScheduledPendingTasks();
        channel.runPendingTasks();

        assertTrue("Channel remains open => read=-1 => infinite", channel.isActive());
        assertNull(testHandler.lastException());
    }

    @Test
    public void testReadTimeoutZero() throws Exception {
        configureChannel(0, 5, 5, true);
        channel.pipeline().fireChannelActive();

        Thread.sleep(1200);
        channel.runScheduledPendingTasks();
        channel.runPendingTasks();

        assertTrue("Channel remains open => read=0 => no read-timeout", channel.isActive());
        assertNull(testHandler.lastException());
    }

    @Test
    public void testOverlappingPersistAndRead() throws Exception {

        configureChannel(1, 1, 1, true);
        channel.pipeline().fireChannelActive();

        channel.writeInbound(Unpooled.copiedBuffer("FirstRequest", StandardCharsets.UTF_8));
        channel.runPendingTasks();

        timeoutHandler.beginRead(channel.pipeline().firstContext());
        Thread.sleep(500);
        channel.runScheduledPendingTasks();
        channel.runPendingTasks();

        channel.writeInbound(Unpooled.EMPTY_BUFFER);
        channel.runPendingTasks();

        Thread.sleep(1200);
        channel.runScheduledPendingTasks();
        channel.runPendingTasks();

        assertFalse("Channel closed by persist-timeout eventually", channel.isActive());
        Throwable ex = testHandler.lastException();
        assertThat(ex, instanceOf(TimeoutHandler.PersistTimeoutException.class));
    }

    @Test
    public void testChannelCloseMidReadTimeout() throws Exception {
        configureChannel(2, 2, 2, true);
        channel.pipeline().fireChannelActive();

        // Wait 1s => forcibly close => read-timeout is never triggered
        Thread.sleep(1000);
        channel.close();

        channel.runScheduledPendingTasks();
        channel.runPendingTasks();

        assertFalse("Channel is closed by forced close", channel.isActive());
        assertNull("No exception => we closed ourselves", testHandler.lastException());
    }

    @Test
    public void testPartialReadChunks() throws Exception {
        configureChannel(1, 5, 5, true);
        channel.pipeline().fireChannelActive();

        // chunk 1
        channel.writeInbound(Unpooled.copiedBuffer("Part1", StandardCharsets.UTF_8));
        channel.runPendingTasks();
        Thread.sleep(500);
        channel.runScheduledPendingTasks();
        channel.runPendingTasks();

        // chunk 2
        channel.writeInbound(Unpooled.copiedBuffer("Part2", StandardCharsets.UTF_8));
        channel.runPendingTasks();
        // Wait >1 => but we keep resetting read-timeout with each chunk
        Thread.sleep(1200);
        channel.runScheduledPendingTasks();
        channel.runPendingTasks();

        assertTrue("Channel open => partial read resets read-timeout", channel.isActive());
        assertNull(testHandler.lastException());
        String data = testHandler.getInboundData();
        assertThat(data, containsString("Part1"));
        assertThat(data, containsString("Part2"));
    }

    private class SimpleHttpTimeoutTestHandler extends ChannelInboundHandlerAdapter {

        private Throwable lastException;
        private StringBuilder inboundData = new StringBuilder();

        @Override
        public void channelActive(ChannelHandlerContext context) throws Exception {
            TimeLog.log("[SimpleHttpTimeoutTestHandler] channelActive -> beginRead()");
            getTimeoutHandler(context).beginRead(context);
        }

        @Override
        public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
            TimeLog.log("[SimpleHttpTimeoutTestHandler] channelRead -> writing response + beginPersistRead()");
            if(message instanceof ByteBuf){
                ByteBuf buffer = (ByteBuf) message;
                inboundData.append(buffer.toString(StandardCharsets.UTF_8));
            } 
            ChannelFuture future = context.writeAndFlush("HTTP/1.1 200 OK\r\n\r\nTest Handler!");
            future.addListener(f -> {
                if(f.isSuccess() && config.isKeepAliveEnabled()){
                    getTimeoutHandler(context).beginPersistRead(context);
                    super.channelRead(context, message);
                }
            });
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
            TimeLog.log("[SimpleHttpTimeoutTestHandler] exceptionCaught -> " + cause);
            this.lastException = cause;

            // We close here so we can see if the channel actually gets closed:
            TimeLog.log("[SimpleHttpTimeoutTestHandler] closing channel due to exception");
            context.close();
        }

        public Throwable lastException() {
            return lastException;
        }

        private TimeoutHandler getTimeoutHandler(ChannelHandlerContext context) {
            return context.pipeline().get(TimeoutHandler.class);
        }

        public String getInboundData(){
            return inboundData.toString();
        }



    }

    public class StuckWriteHandler extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        System.out.println("[StuckWriteHandler] => blocking the write to simulate a slow/hung write");
        
    }
}

    private static class TimeLog {
            private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
            public static void log(String message){
                String now = LocalDateTime.now().format(FORMATTER);
                System.out.println("["+now+"] " + message );
            }
        }
}
