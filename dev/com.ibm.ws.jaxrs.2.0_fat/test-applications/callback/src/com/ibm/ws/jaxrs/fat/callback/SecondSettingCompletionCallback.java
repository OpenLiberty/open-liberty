package com.ibm.ws.jaxrs.fat.callback;


import javax.ws.rs.container.CompletionCallback;

public class SecondSettingCompletionCallback implements CompletionCallback {

   private static String throwableName;
   public static final String NULL = "NULL";
   public static final String OUTOFORDER = "SecondSettingCompletionCallback is not second";
   public static final String NONAME = "No name has been set yet";

   @Override
   public void onComplete(Throwable throwable) {
      System.out.println("execute secondsettingcompletioncallback's onComplete method!!!");
      throwableName = throwable == null ? NULL : throwable.getClass()
              .getName();
      if (!SettingCompletionCallback.getLastThrowableName().equals(throwableName))
         throwableName = throwableName + OUTOFORDER;
      System.out.println("SecondSettingCompletionCallback throwableName = " + throwableName);
   }

   public static final String getLastThrowableName() {
      System.out.println("SecondSettingCompletionCallback.getLastThrowableName = " + throwableName);
      return throwableName;
   }

   public static final void resetLastThrowableName() {
      throwableName = NONAME;
      System.out.println("SecondSettingCompletionCallback resetting throwableName:  " + NONAME);
   }

}
