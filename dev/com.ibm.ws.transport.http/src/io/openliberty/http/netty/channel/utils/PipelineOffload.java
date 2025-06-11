package io.openliberty.http.netty.channel.utils;

import java.util.concurrent.ExecutorService;

import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;

import io.netty.channel.ChannelHandlerContext;

public final class PipelineOffload {

    private static final ExecutorService POOL = HttpDispatcher.getExecutorService();

    private PipelineOffload(){}

    public static void run(ChannelHandlerContext context, Runnable task){
        if(context.executor().inEventLoop()){
            POOL.execute(() -> {
                try{
                    if(task != null) task.run();
                } catch(Throwable t){
                    context.executor().execute(() -> context.fireExceptionCaught(t));
                }
            });
        }else{
            task.run();
        }
    }

}
