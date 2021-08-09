package com.ibm.ws.jaxrs.fat.callback;


import java.util.concurrent.ArrayBlockingQueue;

import javax.ws.rs.container.AsyncResponse;

public class AsyncResponseBlockingQueue extends ArrayBlockingQueue<AsyncResponse> {

   private static final long serialVersionUID = -2445906740359075621L;

   public AsyncResponseBlockingQueue(int capacity) {
      super(capacity);
   }

}
