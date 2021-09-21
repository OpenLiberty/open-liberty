package io.openliberty.netty.internal;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

/**
 * Wrapper for {@link ChannelInitializer} implementations
 *
 */
public abstract class ChannelInitializerWrapper extends ChannelInitializer<Channel> {

    /**
     * invoke initChannel(channel) on this object
     * @param channel
     * @throws Exception
     */
    public void init(Channel channel) throws Exception {
        this.initChannel(channel);
    }
}
