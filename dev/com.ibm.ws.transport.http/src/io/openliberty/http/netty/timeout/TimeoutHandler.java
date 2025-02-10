package io.openliberty.http.netty.timeout;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.Timeout;
import io.netty.util.Timer;

public class TimeoutHandler extends ChannelDuplexHandler{

    private final Timer timer;
    private final int readTimeoutSeconds;
    private final int writeTimeoutSeconds;
    private final int persistTimeoutSeconds;

    private boolean useKeepAlive = true;

    private final Map<TimeoutType, Timeout> timeoutMap = new EnumMap<>(TimeoutType.class);

    private AtomicBoolean reading = new AtomicBoolean(false);
    private AtomicBoolean writing = new AtomicBoolean(false);
    private AtomicBoolean waitingForNextRequest = new AtomicBoolean(false);

    public enum TimeoutType {
        READ,
        WRITE, 
        PERSIST
    }

    public TimeoutHandler(Timer timer, HttpChannelConfig config){
        
        this.timer = timer;
        this.useKeepAlive = config.isKeepAliveEnabled();

        this.readTimeoutSeconds = config.getReadTimeout();
        this.writeTimeoutSeconds = config.getWriteTimeout();
        this.persistTimeoutSeconds = config.getPersistTimeout();    

        System.out.println("[TimeoutHandler] Constructor => read=" + readTimeoutSeconds
                           + ", write=" + writeTimeoutSeconds
                           + ", persist=" + persistTimeoutSeconds
                           + ", keepAlive=" + useKeepAlive);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext context) throws Exception{
        if(context.channel().config().isAutoRead()){
            throw new IllegalStateException("TimeoutHandler requires the pipeline to have autoRead disabled.");
        }
        super.handlerAdded(context);
        System.out.println("[TimeoutHandler] handlerAdded => channelId="
                           + context.channel().id());
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception{
        if(writing.compareAndSet(false, true)){
            System.out.println("[TimeoutHandler] => scheduling WRITE timeout");
            startTimeoutTimer(context, writeTimeoutSeconds, TimeoutType.WRITE);
        }
        super.write(context, message, promise);

        promise.addListener((ChannelFutureListener) future -> {
            cancelTimeout(TimeoutType.WRITE);
            writing.set(false);
            System.out.println("[TimeoutHandler] => write complete, canceled WRITE timeout");
        });
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        System.out.println("[TimeoutHandler] channelRead() => cancel READ & PERSIST timeouts");

        boolean hasData = isNonEmptyData(message);
        
        if(hasData){

            cancelTimeout(TimeoutType.READ);
            reading.set(false);
            System.out.println("[TimeoutHandler] => Non-empty data arrived, read-timeout canceled");
            
        } else {
            System.out.println("[TimeoutHandler] => Inbound read was empty or irrelevant, read-timeout NOT canceled");
        }
        
        cancelTimeout(TimeoutType.PERSIST);
        waitingForNextRequest.set(false);
        super.channelRead(context, message);
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
        System.out.println("[TimeoutHandler] channelInactive() => cancel ALL timeouts");
        for(TimeoutType type : TimeoutType.values()){
            cancelTimeout(type);
        }
        super.channelInactive(context);
    }

    @Override 
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception{
        System.out.println("[TimeoutHandler] exceptionCaught => " + cause + " => fireExceptionCaught");
        context.fireExceptionCaught(cause);
    }

    public void beginRead(ChannelHandlerContext context) {
        System.out.println("[TimeoutHandler] beginRead() => reading=" + reading.get());
        if (reading.compareAndSet(false, true)) {
            System.out.println("[TimeoutHandler] => scheduling READ timeout for " + readTimeoutSeconds + "s");
            startTimeoutTimer(context, readTimeoutSeconds, TimeoutType.READ);
            context.read();
        }
    }

    public void beginPersistRead(ChannelHandlerContext context) {
        System.out.println("[TimeoutHandler] beginPersistRead() => keepAlive=" + useKeepAlive);
        if (!useKeepAlive) {
            System.out.println("[TimeoutHandler] => keepAlive is " + useKeepAlive + " => not setting persist read, just return!");
            return;
        }

        if (waitingForNextRequest.compareAndSet(false, true)) {
            System.out.println("[TimeoutHandler] => scheduling PERSIST timeout for " + persistTimeoutSeconds + "s");
            startTimeoutTimer(context, persistTimeoutSeconds, TimeoutType.PERSIST);
        }
    }

    private void startTimeoutTimer(ChannelHandlerContext context, int seconds, TimeoutType type){
        if(seconds <= 0){
            System.out.println("[TimeoutHandler] startTimeoutTimer => " + type + " <=0 => skipping");
            return;

        }
        cancelTimeout(type);
        System.out.println("[TimeoutHandler] => scheduling " + type + " in " + seconds + "s");

        Timeout timeout = timer.newTimeout(
            timeoutFuture -> {
                if (!timeoutFuture.isCancelled()){
                    System.out.println("[TimeoutHandler] => " + type + " triggered => firing exception");
                    context.fireExceptionCaught(createTimeoutException(type, seconds));
                }else{
                    System.out.println("[TimeoutHandler] => " + type + " was cancelled => no action");
                }
            }, 
            seconds, 
            TimeUnit.SECONDS);

        timeoutMap.put(type, timeout);
    }

    private void cancelTimeout(TimeoutType type){
        Timeout current = timeoutMap.remove(type);
        if(current != null){
            current.cancel();
            System.out.println("[TimeoutHandler] => canceled " + type + " timeout");
        }
    }

    private Throwable createTimeoutException(TimeoutType type, int seconds){
        System.out.println("[TimeoutHandler] => creating timeoutException");
        switch(type){
            case READ: return new ReadTimeoutException("No read data in " + seconds + " seconds.");
            case WRITE: return new WriteTimeoutException("Write did not complete within configured " + seconds + " seconds.");
            case PERSIST: return new PersistTimeoutExeption("No new request arrived within configured persist time of " + seconds + "seconds.");

            default: return new RuntimeException("Unsupported timeout type caught " + type);
        }
    }

    private boolean isNonEmptyData(Object message){
        if(message instanceof ByteBuf){
            return  ((ByteBuf) message).isReadable();
        }
        return false;
    }

    public static class ReadTimeoutException extends IOException {
        public ReadTimeoutException(String arg0){super(arg0);}
    }
    public static class WriteTimeoutException extends IOException{
        public WriteTimeoutException(String arg0){super(arg0);}
    }
    public static class PersistTimeoutExeption extends IOException{
        public PersistTimeoutExeption(String arg0){super(arg0);}
    }
}
