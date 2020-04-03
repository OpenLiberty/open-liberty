package com.ibm.ws.logging.hpel.servlet;

import java.io.IOException;
import java.util.logging.FileHandler;

public class MyCustomHandler extends FileHandler {
    public MyCustomHandler(String pattern, int limit, int count, boolean append)
        throws SecurityException, IOException {
        super(pattern, limit, count, append);
    }
}