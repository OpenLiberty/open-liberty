package com.ibm.logs;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.logging.hpel.LogRecordContext;

@WebServlet("/ExceptionExtURL")
public class ExceptionExtServlet extends HttpServlet {

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String loggerName = "com.ibm.logs.ExceptionExtServlet";
        String logMessage = "Test Exception Extension Message";

        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(loggerName);

        String key = request.getParameter("key");
        String value = request.getParameter("value");
        String msg = request.getParameter("msg");
        if (msg != null) {
            logMessage = msg;
        }

        //Populating LogRecord with extensions

        if (key == null) {
            LogRecordContext.addExtension("testExtensionException", "testValue");
        } else {
            LogRecordContext.addExtension(key, value);
        }

        logger.logp(java.util.logging.Level.INFO, loggerName, "Method.Info", logMessage);

        try {
            throw new Throwable("Test Throwable");
        } catch (Throwable t) {
            throw new IOException(t.getMessage());
        }

    }
}