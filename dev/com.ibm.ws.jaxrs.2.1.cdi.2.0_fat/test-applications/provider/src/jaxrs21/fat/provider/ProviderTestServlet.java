/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs21.fat.provider;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/ProviderTestServlet")
public class ProviderTestServlet extends FATServlet {

    private static final int HTTP_PORT = Integer.getInteger("bvt.prop.HTTP_default", 8010);
    private Client client;

    @Override
    public void init() throws ServletException {
        client = ClientBuilder.newBuilder().build();
    }

    @Override
    public void destroy() {
        client.close();
    }

    @Test
    public void testInjectionWorkswithMBRMBW(HttpServletRequest request, HttpServletResponse resp) {
        
        TestEntity requestTestEntity = new TestEntity();
        requestTestEntity.setData1("data1");
        requestTestEntity.setData2(1);
        requestTestEntity.setData3(true);
        System.out.println("testInjectionWorkswithMBRMBW requestTestEntity: " + requestTestEntity.toString());       
        TestEntity responseTestEntity  = null;
        
        ClientBuilder cb = ClientBuilder.newBuilder();
        // Client code needs to register the MBR and MBW
        cb.register(TestEntityMessageBodyReader.class);
        cb.register(TestEntityMessageBodyWriter.class);
        Client c = cb.build();
        WebTarget t = c.target("http://" + request.getServerName() + ":" + HTTP_PORT + "/provider/test1/post1");
        Builder builder = t.request();
        builder.accept(MediaType.APPLICATION_JSON);      
        CompletionStageRxInvoker completionStageRxInvoker = builder.rx();        
        CompletionStage<TestEntity> completionStage = completionStageRxInvoker.post(Entity.json(requestTestEntity),TestEntity.class);        
        CompletableFuture<TestEntity> completableFuture = completionStage.toCompletableFuture();        
        
        try {
            responseTestEntity = completableFuture.get();
            System.out.println("testInjectionWorkswithMBRMBW responseTestEntity: " + responseTestEntity.toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }        

        assertTrue("Expected responseTestEntity Data1: " + responseTestEntity.getData1(), responseTestEntity.getData1().equals(requestTestEntity.getData1()));
        assertTrue("Expected responseTestEntity Data2: " + responseTestEntity.getData2(), responseTestEntity.getData2().equals(requestTestEntity.getData2()));
        assertTrue("Expected responseTestEntity Data3: " + responseTestEntity.getData3(), responseTestEntity.getData3().equals(requestTestEntity.getData3()));
       
        c.close();       
    }
}