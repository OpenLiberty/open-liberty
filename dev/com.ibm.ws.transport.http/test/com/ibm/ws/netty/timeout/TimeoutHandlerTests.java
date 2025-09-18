// /*******************************************************************************
//  * Copyright (c) 2025 IBM Corporation and others.
//  * All rights reserved. This program and the accompanying materials
//  * are made available under the terms of the Eclipse Public License 2.0
//  * which accompanies this distribution, and is available at
//  * http://www.eclipse.org/legal/epl-2.0/
//  * 
//  * SPDX-License-Identifier: EPL-2.0
//  *******************************************************************************/
// package com.ibm.ws.netty.timeout;

// import io.netty.buffer.Unpooled;
// import io.netty.channel.ChannelFuture;
// import io.netty.channel.embedded.EmbeddedChannel;
// import io.netty.handler.codec.http.DefaultFullHttpResponse;
// import io.netty.handler.codec.http.HttpResponseStatus;
// import io.netty.handler.codec.http.HttpVersion;
// import io.netty.handler.timeout.IdleState;
// import io.netty.handler.timeout.IdleStateEvent;
// import io.openliberty.http.netty.timeout.TimeoutEventHandler;
// import io.openliberty.http.netty.timeout.TimeoutHandler;
// import io.openliberty.http.netty.timeout.TimeoutType;
// import io.openliberty.http.netty.timeout.exception.H2IdleTimeoutException;
// import io.openliberty.http.netty.timeout.exception.PersistTimeoutException;
// import io.openliberty.http.netty.timeout.exception.ReadTimeoutException;
// import io.openliberty.http.netty.timeout.exception.UnknownTimeoutException;
// import io.openliberty.http.options.TcpOption;

// import java.lang.reflect.Field;
// import java.util.concurrent.TimeUnit;
// import java.util.concurrent.atomic.AtomicReference;

// import static org.junit.Assert.*;
// import org.junit.Test;
// import static org.mockito.Mockito.*;

// import com.ibm.ws.http.channel.internal.HttpChannelConfig;
// import com.ibm.ws.http.netty.NettyHttpChannelConfig;
// import com.ibm.ws.http.netty.NettyHttpConstants;

// public class TimeoutHandlerTests {

//     @Test
//     public void readTimeoutExceptionMessageTest(){
//         ReadTimeoutException exception = new ReadTimeoutException(5, TimeUnit.SECONDS);
//         //TODO warning code -> assertEquals("", exception.getMessage());
//         assertTrue(exception.getMessage().contains("5 seconds"));
//     }

//     @Test
//     public void idleHandlerReadTimeout(){
//         TimeoutEventHandler handler = new TimeoutEventHandler(TimeoutType.READ, 1, TimeUnit.SECONDS);
//         EmbeddedChannel channel = new EmbeddedChannel(handler);

//         channel.pipeline().fireUserEventTriggered(IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT);
//         Throwable t = extractException(channel);

//         assertNotNull("Handler should throw exception", t);
//         assertTrue(t instanceof ReadTimeoutException);
//         channel.close();
//     }

//     @Test
//     public void readTimeoutHandlerOnActive(){
//         NettyHttpChannelConfig config = mock(NettyHttpChannelConfig.class);
//         when(config.get(TcpOption.INACTIVITY_TIMEOUT)).thenReturn(0);
//         when(config.getReadTimeout()).thenReturn(1000);
//         when(config.getPersistTimeout()).thenReturn(1000);

//         TimeoutHandler handler = new TimeoutHandler(config);
//         EmbeddedChannel channel = new EmbeddedChannel(handler);
//         assertNotNull("Request Idle handler is missing.", channel.pipeline().get(TimeoutHandler.class));
//     }

//     @Test
//     public void timeoutHandlerSwitchToPersist() throws Exception{
//         NettyHttpChannelConfig config = mock(NettyHttpChannelConfig.class);
//         when(config.get(TcpOption.INACTIVITY_TIMEOUT)).thenReturn(0);
//         when(config.getReadTimeout()).thenReturn(2000);
//         when(config.getPersistTimeout()).thenReturn(1000);
//         TimeoutHandler handler = new TimeoutHandler(config);
//         EmbeddedChannel channel = new EmbeddedChannel(handler);

//         DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER);
//         ChannelFuture future = channel.writeAndFlush(response);
//         channel.runPendingTasks();
//         assertTrue(future.isSuccess());
        
//         TimeoutType phase = (TimeoutType) getPrivate(handler, "phase");
//         assertEquals("Expected handler to be in PERSIST phase", TimeoutType.PERSIST, phase);
//         channel.close();
//     }

//     @Test
//     public void switchToH2WithAttribute(){
//         NettyHttpChannelConfig config = mock(NettyHttpChannelConfig.class);
//         when(config.getReadTimeout()).thenReturn(2000);
//         when(config.getPersistTimeout()).thenReturn(1000);
//         when(config.getH2ConnectionIdleTimeout()).thenReturn(5000);
//         EmbeddedChannel channel = new EmbeddedChannel(new TimeoutHandler(config));

//         channel.attr(NettyHttpConstants.PROTOCOL).set(NettyHttpConstants.ProtocolName.HTTP2.name());

//         channel.writeInbound(Unpooled.EMPTY_BUFFER);
//         channel.advanceTimeBy(6000, TimeUnit.MILLISECONDS);
//         channel.runScheduledPendingTasks();


//         Throwable t = extractException(channel);
//         System.out.println(">>> Exception is: " + t);
//         assertTrue(t instanceof H2IdleTimeoutException);
//         channel.close();
        
//     }


//     private static Throwable extractException(EmbeddedChannel channel){
//         try{
//             channel.checkException();
//             return null;

//         }catch(Throwable t){
//             return t;
//         }
//     }

//     private static Object getPrivate(Object object, String field) throws ReflectiveOperationException{
//         Field f = object.getClass().getDeclaredField(field);
//         f.setAccessible(true);
//         return f.get(object);
//     }


// }
