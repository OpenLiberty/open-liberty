package com.ibm.logs;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.logging.hpel.*;

@WebServlet("/ExtURL")
public class ExtensionServlet extends HttpServlet {

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String loggerName = "com.ibm.logs.ExtensionServlet";
        String logMessage = "Test Extension Message";

        Map<String, String> map = new HashMap<String, String>();

        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(loggerName);

        //Populating LogRecord with extensions

        LogRecordContext.addExtension("correctBooleanExtension_bool", "true");
        LogRecordContext.addExtension("correctBooleanExtension2_bool", "false");
        LogRecordContext.addExtension("correctIntExtension_int", "12345");
        LogRecordContext.addExtension("correctIntExtension2_int", "-12345");
        LogRecordContext.addExtension("correctStringExtension", "Testing string 1234");
        LogRecordContext.addExtension("correctFloatExtension_float", "100.123");
        LogRecordContext.addExtension("correctFloatExtension2_float", "-100.123");

        LogRecordContext.addExtension("wrongExtensionBoolean_bool", "12345");
        LogRecordContext.addExtension("wrongExtensionBoolean2_bool", "wrongBool");
        LogRecordContext.addExtension("wrongExtensionInt_int", "Testing");
        LogRecordContext.addExtension("wrongExtensionInt2_int", "false");
        LogRecordContext.addExtension("wrongExtensionInt3_int", "123.123");
        LogRecordContext.addExtension("wrongExtensionFloat_float", "Testing string");
        LogRecordContext.addExtension("wrongExtensionFloat2_float", "false");

        logger.logp(java.util.logging.Level.INFO, loggerName, "Method.Info", logMessage);
        logger.logp(java.util.logging.Level.FINE, loggerName, "Method.Info", logMessage);

        LogRecordContext.removeExtension("TESTEXT_int");

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body>Simple extension servlet</body></html>");
    }
}