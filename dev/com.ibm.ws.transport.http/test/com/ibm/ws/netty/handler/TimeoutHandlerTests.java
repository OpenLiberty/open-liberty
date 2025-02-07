package com.ibm.ws.netty.handler;

import java.util.concurrent.TimeUnit;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.HashedWheelTimer;
import io.openliberty.http.netty.timeout.TimeoutHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class TimeoutHandlerTests {

    private HashedWheelTimer timer;
    private EmbeddedChannel channel;
    private HttpChannelConfig config;

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

    private void configureHttpOptions(int readTimeout, int writeTimeout, int persistTimeout, boolean KeepAliveEnabled){
        when(config.getReadTimeout()).thenReturn(readTimeout);
        when(config.getWriteTimeout()).thenReturn(writeTimeout);
        when(config.getPersistTimeout()).thenReturn(persistTimeout);
        when(config.isKeepAliveEnabled()).thenReturn(KeepAliveEnabled);
    }

    private void configureChannel(){
        TimeoutHandler timeoutHandler = new TimeoutHandler(timer, config);
        channel.pipeline().addLast("timeoutHandler", timeoutHandler);
        channel.pipeline().addLast("testHandler", new SimpleHttpTimeoutTestHandler());
    }

    @Test 
    public void testReadTimeout() throws Exception{
        configureHttpOptions(1, 5, 5, true); // read=1
        configureChannel();

        channel.pipeline().fireChannelActive();
        // => watch the logs to ensure beginRead is called, scheduling a 1s read-timeout

        Thread.sleep(1200);
        channel.runScheduledPendingTasks();
        channel.runPendingTasks();

        System.out.println("[TEST] After 1.2s => channel active=" + channel.isActive());
        assertThat("channel should close by read timeout", channel.isActive(), is(false));

        SimpleHttpTimeoutTestHandler testHandler = channel.pipeline().get(SimpleHttpTimeoutTestHandler.class);
        Throwable cause = testHandler.lastException();
        System.out.println("[TEST] lastException=" + cause);
        assertThat(cause, instanceOf(TimeoutHandler.ReadTimeoutException.class));
    }

    //@Test 
    public void testWriteTimeout() throws Exception{
        
        configureHttpOptions(5, 1, 5, true);
        configureChannel();

        channel.pipeline().fireChannelActive();
        channel.writeInbound("HTTP Test Request 1");
        channel.runPendingTasks();

        Thread.sleep(1200);
        channel.runPendingTasks();

        assertThat("Channel closed after write-timeout", channel.isActive(), is(false));
        verify(config, atLeastOnce()).getWriteTimeout();
    }

    private class SimpleHttpTimeoutTestHandler extends ChannelInboundHandlerAdapter {

        private Throwable lastException;

        @Override
        public void channelActive(ChannelHandlerContext context) throws Exception {
            System.out.println("[SimpleHttpTimeoutTestHandler] channelActive -> beginRead()");
            getTimeoutHandler(context).beginRead(context);
        }

        @Override
        public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
            System.out.println("[SimpleHttpTimeoutTestHandler] channelRead -> writing response + beginPersistRead()");
            context.writeAndFlush("HTTP/1.1 200 OK\r\n\r\nTest Handler!");
            getTimeoutHandler(context).beginPersistRead(context);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
            System.out.println("[SimpleHttpTimeoutTestHandler] exceptionCaught -> " + cause);
            this.lastException = cause;

            // We close here so we can see if the channel actually gets closed:
            System.out.println("[SimpleHttpTimeoutTestHandler] closing channel due to exception");
            context.close();
        }

        public Throwable lastException() {
            return lastException;
        }

        private TimeoutHandler getTimeoutHandler(ChannelHandlerContext context) {
            return context.pipeline().get(TimeoutHandler.class);
        }

    }
}
