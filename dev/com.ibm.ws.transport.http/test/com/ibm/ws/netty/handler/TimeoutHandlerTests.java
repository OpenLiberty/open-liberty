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
