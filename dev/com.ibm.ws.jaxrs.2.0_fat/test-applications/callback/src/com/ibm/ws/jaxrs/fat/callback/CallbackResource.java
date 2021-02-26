package com.ibm.ws.jaxrs.fat.callback;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;

@Path("resource")
public class CallbackResource extends Resource {

   @GET
   @Path("register")
   public String registerObject(@QueryParam("stage") String stage) {
      AsyncResponse async = takeAsyncResponse(stage);
      boolean b = async.register(new SettingCompletionCallback()).isEmpty();
      addResponse(async, stage);
      return b ? TRUE : FALSE;
   }

   @GET
   @Path("registerclass")
   public String registerClass(@QueryParam("stage") String stage) {
      AsyncResponse async = takeAsyncResponse(stage);
      boolean b = async.register(SettingCompletionCallback.class).isEmpty();
      addResponse(async, stage);
      return b ? TRUE : FALSE;
   }

   @GET
   @Path("registerobjects")
   public String registerObjectObject(@QueryParam("stage") String stage) {
      AsyncResponse async = takeAsyncResponse(stage);
      boolean b = async.register(new SettingCompletionCallback(),
              new SecondSettingCompletionCallback()).isEmpty();
      addResponse(async, stage);
      return b ? TRUE : FALSE;
   }

   @GET
   @Path("registerclasses")
   public String registerClasses(@QueryParam("stage") String stage) {
      AsyncResponse async = takeAsyncResponse(stage);
      boolean b = async.register(SettingCompletionCallback.class,
              SecondSettingCompletionCallback.class).isEmpty();
      addResponse(async, stage);
      return b ? TRUE : FALSE;
   }

   @GET
   @Path("registerthrows")
   public String registerObjectThrowsNpe(@QueryParam("stage") String stage) {
      AsyncResponse async = takeAsyncResponse(stage);
      try {
         SettingCompletionCallback callback = null;
         async.register(callback);
      } catch (NullPointerException e) {
         return TRUE;
      } catch (Exception e) {
         return "Threw " + e.getClass().getName();
      }
      return FALSE;
   }

   @GET
   @Path("registerclassthrows")
   public String registerClassThrowsNpe(@QueryParam("stage") String stage) {
      AsyncResponse async = takeAsyncResponse(stage);
      try {
         Class<SettingCompletionCallback> callback = null;
         async.register(callback);
      } catch (NullPointerException e) {
         return TRUE;
      } catch (Exception e) {
         return "Threw " + e.getClass().getName();
      }
      return FALSE;
   }

   @GET
   @Path("registerobjectsthrows1")
   public String registerObjectsThrowsNpe1(@QueryParam("stage") String stage) {
      AsyncResponse async = takeAsyncResponse(stage);
      try {
         SettingCompletionCallback callback = null;
         async.register(callback, new SecondSettingCompletionCallback());
      } catch (NullPointerException e) {
         return TRUE;
      } catch (Exception e) {
         return "Threw " + e.getClass().getName();
      }
      return FALSE;
   }

   @GET
   @Path("registerobjectsthrows2")
   public String registerObjectsThrowsNpe2(@QueryParam("stage") String stage) {
      AsyncResponse async = takeAsyncResponse(stage);
      try {
         SecondSettingCompletionCallback callback = null;
         async.register(new SettingCompletionCallback(), callback);
      } catch (NullPointerException e) {
         return TRUE;
      } catch (Exception e) {
         return "Threw " + e.getClass().getName();
      }
      return FALSE;
   }

   @GET
   @Path("registerclassesthrows1")
   public String registerClassesThrowsNpe1(@QueryParam("stage") String stage) {
      AsyncResponse async = takeAsyncResponse(stage);
      try {
         Class<SettingCompletionCallback> callback = null;
         async.register(callback, SecondSettingCompletionCallback.class);
      } catch (NullPointerException e) {
         return TRUE;
      } catch (Exception e) {
         return "Threw " + e.getClass().getName();
      }
      return FALSE;
   }

   @GET
   @Path("registerclassesthrows2")
   public String registerClassesThrowsNpe2(@QueryParam("stage") String stage) {
      AsyncResponse async = takeAsyncResponse(stage);
      try {
         Class<SecondSettingCompletionCallback> callback = null;
         async.register(SettingCompletionCallback.class, callback);
      } catch (NullPointerException e) {
         return TRUE;
      } catch (Exception e) {
    	  System.out.println(e.getMessage());
    	  System.out.println(e.getMessage());
         return "Threw " + e.getClass().getName();
      }
      return FALSE;
   }

   @GET
   @Path("exception")
   public String throwExceptionOnAsyncResponse(
           @QueryParam("stage") String stage) {
      System.out.println("********throwExceptionOnAsyncResponse");
      AsyncResponse async = takeAsyncResponse(stage);
      System.out.println("*****async is not null: " + async);
      boolean b = async.resume(new ExceptionThrowingStringBean(
              "throw exception"));
      System.out.println("execuet finished for resume!!!");
      addResponse(async, stage);
      return b ? TRUE : FALSE;
   }

   @GET
   @Path("error")
   public String getErrorValue() {
      String name = SettingCompletionCallback.getLastThrowableName();
      return name;
   }

   @GET
   @Path("seconderror")
   public String getSecondErrorValue() {
      System.out.println("In getSecondErrorValue()");
      String name = SecondSettingCompletionCallback.getLastThrowableName();
      System.out.println("CallbackResource.getSecondErrorValue() name = " + name);
      return name;
   }

   @GET
   @Path("reset")
   public void resetErrorValue() {
      System.out.println("CallbackResource.resetErrorValue()");
      SettingCompletionCallback.resetLastThrowableName();
      SecondSettingCompletionCallback.resetLastThrowableName();
   }

   // ////////////////////////////////////////////////////////////////////////

}
