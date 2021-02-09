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
package com.ibm.ws.jaxrs21.fat.JAXRS21ReactiveSample.reactive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;

import com.ibm.ws.jaxrs21.fat.JAXRS21ReactiveSample.server.Customer;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.ResourceSubscriber;


@Path("/collecteddatastore")
public class CollectedDataStore {

   public static final int RETRY_COUNTER = 50;
   public static final int DATA_LOOP = 50000;
   private static Map<String, ArrayList<String>> store = Collections.synchronizedMap(new HashMap<String, ArrayList<String>>());

   @GET
   @Path("/rxget/{customerName}")
   @Produces("application/json")
   public void getCollectedDataList(@Suspended AsyncResponse async, @PathParam("customerName") String customerName) {

      List<String> ids = null;

      synchronized (store) {
         ids = store.get(customerName);
         store.put(customerName, new ArrayList<String>(Arrays.asList(customerName)));
      }

      async.resume(new GenericEntity<List<String>>(ids) {});
   }


   @POST
   @Path("/postCustomer")
   @Consumes("application/json")
   public Response addCustomer(Customer customer) {
      String customerName = customer.getName();
      System.out.println("addCustomer: " + customerName);
      synchronized (store) {
         store.putIfAbsent(customerName, new ArrayList<String>(Arrays.asList(customerName)));
      }

      Executors.newFixedThreadPool(1).submit(() -> collectData(customer));

      return Response.ok(customerName).build();
   }


   private void collectData(Customer customer) {

      String customerName = customer.getName();
      int backPressure = Customer.FREE_MAX;

      if (customer.getServiceLevel().equals(Customer.BEST_SERVICE_LEVEL)) {
         backPressure = Customer.BEST_MAX;
      } else if (customer.getServiceLevel().equals(Customer.BETTER_SERVICE_LEVEL)) {
         backPressure = Customer.BETTER_MAX;
      }

      System.out.println("collectData: " + customerName + " backPressure " + backPressure);

      Flowable<Integer> flowable = Flowable.create(new FlowableOnSubscribe<Integer>() {
                                                      @Override
                                                      public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                                                         for (int i =0;i<DATA_LOOP;i++) {
                                                            e.onNext(i);
                                                         }
                                                         e.onComplete();
                                                      }}, BackpressureStrategy.BUFFER);

      flowable.onBackpressureBuffer(backPressure)
              .observeOn(Schedulers.computation(), true, backPressure)
              .retryWhen(errors -> {
                  AtomicInteger counter = new AtomicInteger();
                  return errors.takeWhile(e -> counter.getAndIncrement() != RETRY_COUNTER)
                               .flatMap(e -> {
                                    System.out.println("collectData: " + customerName + " delay retry :" + counter.get());
                                    return Flowable.timer(1, TimeUnit.SECONDS);
                               });
                  })
              .subscribe(new ResourceSubscriber<Integer>() {
                    @Override
                    public void onNext(Integer id) {
                       synchronized (store) {
                          ArrayList<String> ids = store.get(customerName);
                          ids.add(String.valueOf(id));
                       }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                       System.out.println("collectData: " + customer.getName() + " onError " + throwable.getMessage());
                       dispose();
                    }

                    @Override
                    public void onComplete() {
                       System.out.println("collectData: " + customer.getName() + " onComplete ");
                       dispose();
                    }
                 });
   }
}
