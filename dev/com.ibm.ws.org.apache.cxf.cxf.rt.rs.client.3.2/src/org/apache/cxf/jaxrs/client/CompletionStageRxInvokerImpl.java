/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.jaxrs.client;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

//Liberty code change start
import org.eclipse.microprofile.context.ManagedExecutor;
import com.ibm.ws.jaxrs21.clientconfig.JAXRSClientCompletionStageFactoryConfig;

import com.ibm.ws.concurrent.mp.spi.CompletionStageFactory;

//Liberty code change end
public class CompletionStageRxInvokerImpl implements CompletionStageRxInvoker {
    private WebClient wc;
    private ExecutorService ex;    
  //Liberty code change start
    private boolean isManagedExecutor = false;
    private CompletionStageFactory completionStageFactory = JAXRSClientCompletionStageFactoryConfig.getCompletionStageFactory();
        
    CompletionStageRxInvokerImpl(WebClient wc, ExecutorService ex) {
        this.ex = ex;        
        if (this.ex instanceof ManagedExecutor) {            
            isManagedExecutor = true;
        }
        this.wc = wc;
    }   
  //Liberty code change end

    @Override
    public CompletionStage<Response> get() {
        return get(Response.class);
    }

    @Override
    public <T> CompletionStage<T> get(Class<T> responseType) {
        return method(HttpMethod.GET, responseType);
    }

    @Override
    public <T> CompletionStage<T> get(GenericType<T> responseType) {
        return method(HttpMethod.GET, responseType);
    }

    @Override
    public CompletionStage<Response> put(Entity<?> entity) {
        return put(entity, Response.class);
    }

    @Override
    public <T> CompletionStage<T> put(Entity<?> entity, Class<T> responseType) {
        return method(HttpMethod.PUT, entity, responseType);
    }

    @Override
    public <T> CompletionStage<T> put(Entity<?> entity, GenericType<T> responseType) {
        return method(HttpMethod.PUT, entity, responseType);
    }

    @Override
    public CompletionStage<Response> post(Entity<?> entity) {
        return post(entity, Response.class);
    }

    @Override
    public <T> CompletionStage<T> post(Entity<?> entity, Class<T> responseType) {
        return method(HttpMethod.POST, entity, responseType);
    }

    @Override
    public <T> CompletionStage<T> post(Entity<?> entity, GenericType<T> responseType) {
        return method(HttpMethod.POST, entity, responseType);
    }

    @Override
    public CompletionStage<Response> delete() {
        return delete(Response.class);
    }

    @Override
    public <T> CompletionStage<T> delete(Class<T> responseType) {
        return method(HttpMethod.DELETE, responseType);
    }

    @Override
    public <T> CompletionStage<T> delete(GenericType<T> responseType) {
        return method(HttpMethod.DELETE, responseType);
    }

    @Override
    public CompletionStage<Response> head() {
        return method(HttpMethod.HEAD);
    }

    @Override
    public CompletionStage<Response> options() {
        return options(Response.class);
    }

    @Override
    public <T> CompletionStage<T> options(Class<T> responseType) {
        return method(HttpMethod.OPTIONS, responseType);
    }

    @Override
    public <T> CompletionStage<T> options(GenericType<T> responseType) {
        return method(HttpMethod.OPTIONS, responseType);
    }

    @Override
    public CompletionStage<Response> trace() {
        return trace(Response.class);
    }

    @Override
    public <T> CompletionStage<T> trace(Class<T> responseType) {
        return method("TRACE", responseType);
    }

    @Override
    public <T> CompletionStage<T> trace(GenericType<T> responseType) {
        return method("TRACE", responseType);
    }

    @Override
    public CompletionStage<Response> method(String name) {
        return method(name, Response.class);
    }

    @Override
    public CompletionStage<Response> method(String name, Entity<?> entity) {
        return method(name, entity, Response.class);
    }

    @Override
    public <T> CompletionStage<T> method(String name, Entity<?> entity, Class<T> responseType) {
      //Liberty code change start
        return supplyAsync( () -> wc.sync().method(name, entity, responseType) );
      //Liberty code change end

    }

    @Override
    public <T> CompletionStage<T> method(String name, Entity<?> entity, GenericType<T> responseType) {
      //Liberty code change start
        return supplyAsync( () -> wc.sync().method(name, entity, responseType) );
      //Liberty code change end
    }

    @Override
    public <T> CompletionStage<T> method(String name, Class<T> responseType) {
      //Liberty code change start
        return supplyAsync( () -> wc.sync().method(name, responseType) );
      //Liberty code change end
    }

    @Override
    public <T> CompletionStage<T> method(String name, GenericType<T> responseType) {
      //Liberty code change start
        return supplyAsync( () -> wc.sync().method(name, responseType) );
      //Liberty code change end
    }

  //Liberty code change start
    private <T> CompletionStage<T> supplyAsync(Supplier<T> supplier) {      
         if (isManagedExecutor) {
             return ((ManagedExecutor) ex).supplyAsync(supplier);
         }        
        return completionStageFactory.supplyAsync(supplier, ex); 
    }
  //Liberty code change end
}
