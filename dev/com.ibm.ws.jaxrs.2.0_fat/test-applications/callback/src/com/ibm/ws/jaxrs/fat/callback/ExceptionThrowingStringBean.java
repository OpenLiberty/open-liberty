package com.ibm.ws.jaxrs.fat.callback;


import java.io.IOException;

public class ExceptionThrowingStringBean extends StringBean {

   public ExceptionThrowingStringBean(String header) {
      super(header);
   }

   @Override
   public String get() {
      throw new RuntimeException(new IOException(super.get()));
   }

}
