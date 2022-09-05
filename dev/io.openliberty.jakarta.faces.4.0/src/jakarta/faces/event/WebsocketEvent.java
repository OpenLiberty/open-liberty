/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package jakarta.faces.event;

import java.io.Serializable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;
import jakarta.inject.Qualifier;
import jakarta.websocket.CloseReason;

/**
 *
 */
public final class WebsocketEvent implements Serializable
{
   
    private String channel;
    private Serializable user;
    private CloseReason.CloseCode code;
    
    public WebsocketEvent(String channel, Serializable user, CloseReason.CloseCode code)
    {
        this.channel = channel;
        this.user = user;
        this.code = code;
    }

    public String getChannel()
    {
        return channel;
    }

    public <S extends java.io.Serializable> S getUser()
    {
        return (S) user;
    }

    public CloseReason.CloseCode getCloseCode()
    {
        return code;
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 23 * hash + Objects.hashCode(this.channel);
        hash = 23 * hash + Objects.hashCode(this.user);
        hash = 23 * hash + Objects.hashCode(this.code);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final WebsocketEvent other = (WebsocketEvent) obj;
        if (!Objects.equals(this.channel, other.channel))
        {
            return false;
        }
        if (!Objects.equals(this.user, other.user))
        {
            return false;
        }
        if (!Objects.equals(this.code, other.code))
        {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString()
    {
        return "WebsocketEvent{" + "channel=" + channel + ", user=" + user + ", code=" + code + '}';
    }
    
    @Qualifier
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public static @interface Opened 
    {
    }

    @Qualifier
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public static @interface Closed 
    {
    }
}
