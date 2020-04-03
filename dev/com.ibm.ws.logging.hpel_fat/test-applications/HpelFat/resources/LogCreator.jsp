<html>
<head>
<title>HPEL Function Test JSP</title>
</head>
<body>
<%@page 
  import="java.util.*, java.io.*, com.ibm.ejs.ras.*, java.security.*"
%>
<h1 align="Center" >
<br>Date: <%= new java.util.Date() %>
</h1>

<%
String actionToPerform =  request.getParameter( "ActionParam" );
//make the default action Logs
if (actionToPerform == null) actionToPerform = "Logs";

if (actionToPerform.equals("Help")) {
	out.println("<table border=\"1\">");
    out.println("<tr><td>Parmeters:</td><td>&nbsp;</td><td>&nbsp;</td></tr>");
    out.println("<tr><td>Iterations</td><td><b>Default: 50</b></td><td>Will log the  message <i><b>iterations</b></i> times</td</tr>");
    out.println("<tr><td>LoggerName</td><td><b>Default: com.ibm.ws.fat.hpel.tests.Default_HPELTestLogger</b></td><td>Will create a logger with the specificed name</td</tr>");
    out.println("<tr><td>Level</td><td><b>Default: ALL</b></td><td>Will log messages Only to the specificed level [SEVERE, WARNING, INFO, FINE, FINER, FINEST]</td</tr>");
    out.println("<tr><td>Message</td><td><b>Default: Default HPEL Message</b></td><td>Will log the message passed to the JSP</td></tr>");
    out.println("<tr><td>Sleep</td><td><b>Default: -1</b></td><td>No sleep delay between logging</td></tr>");
    out.println("</table>");
} else {
     String parmStr = request.getParameter("Iterations") ;
     int numIterations = (parmStr == null) ? 49 : Integer.parseInt(parmStr) ;

     parmStr = request.getParameter("Message");
     String logMessage = (parmStr == null) ? "Default HPEL Message" : parmStr;

     parmStr = request.getParameter("LoggerName");
     String loggerName = (parmStr == null) ? "com.ibm.ws.fat.hpel.tests.Default_HPELTestLogger" : parmStr;
     
     parmStr = request.getParameter("Level");
     String level = (parmStr == null) ? "ALL" : parmStr;
     
     parmStr = request.getParameter("Sleep");
     int secondsToSleep = (parmStr == null) ? -1 : Integer.parseInt(parmStr);
     
     java.util.logging.Level passedLevel = null;
     if (!level.equals("ALL")) {
    	 if (level.toLowerCase().equals("severe")) passedLevel = java.util.logging.Level.SEVERE;
    	 else if (level.toLowerCase().equals("warning")) passedLevel = java.util.logging.Level.WARNING;
    	 else if (level.toLowerCase().equals("info")) passedLevel = java.util.logging.Level.INFO;
    	 else if (level.toLowerCase().equals("fine")) passedLevel = java.util.logging.Level.FINE;
    	 else if (level.toLowerCase().equals("finer")) passedLevel = java.util.logging.Level.FINER;
    	 else if (level.toLowerCase().equals("finest")) passedLevel = java.util.logging.Level.FINEST;
     }
     
     out.println("<h3>Start time of JSP load: " + new java.util.Date() + "</h3>");

     out.println("<table border=\"1\">");
     out.println("<tr> <th> Name </th><th> Value </th></tr>");
     out.println("<tr> <td colspan=2> ******Parms specific to this run****** </td> </tr>");
     out.println("<tr> <td> Iterations </td><td>" + numIterations + "</td></tr>");
     out.println("<tr> <td> LoggerName </td><td>" + loggerName + "</td></tr>");
     out.println("<tr> <td> Level </td><td>" + level + "</td></tr>");
     out.println("<tr> <td> Message </td><td>" + logMessage + "</td></tr>");
     out.println("<tr> <td> Action </td><td>" + actionToPerform + "</td></tr>");
     out.println("<tr> <td> Sleep </td><td>" + secondsToSleep + " milliseconds</td></tr>");
     out.println("</table>") ;
     
     java.util.logging.Logger logger = java.util.logging.Logger.getLogger(loggerName);
     
     out.println("Action to perform: " + actionToPerform);
     if (actionToPerform.equals("Logs")) {
    	 out.println("Action performed: Logs");
       	 if (level.equals("ALL")) {
       		out.println("Level: ALL");
   	         for (int j = 0; j < numIterations; j++) {
   	        	 //only do this once for each iteration
   	             if (secondsToSleep > 0) Thread.sleep(secondsToSleep);
   	        	 logger.logp(java.util.logging.Level.SEVERE, loggerName, "Method.Severe",  logMessage);
   	             logger.logp(java.util.logging.Level.WARNING, loggerName, "Method.Warning",  logMessage);
   	             logger.logp(java.util.logging.Level.INFO, loggerName, "Method.Info",  logMessage);
   	         }
       	 } else {
       		    out.println("Level: " + passedLevel);
                for (int i = 0; i < numIterations; i++) {
    	        	//only do this once for each iteration
    	            if (secondsToSleep > 0) Thread.sleep(secondsToSleep);
                    logger.logp(passedLevel, loggerName, passedLevel.getName(),  logMessage);
       	     }
            }
     } else if (actionToPerform.equals("Trace")) {
    	 out.println("Action performed: Trace");
       	 if (level.equals("ALL")) {
       		out.println("Level: ALL");
   	         for (int j = 0; j < numIterations; j++) {
   	        	 //only do this once for each iteration
   	             if (secondsToSleep > 0) Thread.sleep(secondsToSleep);
   	             logger.logp(java.util.logging.Level.FINE, loggerName, "Method.Fine",  logMessage);
   	             logger.logp(java.util.logging.Level.FINER, loggerName, "Method.Finer",  logMessage);
   	             logger.logp(java.util.logging.Level.FINEST, loggerName, "Method.Finest",  logMessage);
   	         }
       	 } else {
       		    out.println("Level: " + passedLevel);
                for (int i = 0; i < numIterations; i++) {
      	        	//only do this once for each iteration
      	            if (secondsToSleep > 0) Thread.sleep(secondsToSleep);
                    logger.logp(passedLevel, loggerName, passedLevel.getName(),  logMessage);
       	     }
         }
     } else if (actionToPerform.equals("LogsAndTrace")) {
    	 out.println("Action performed: LogsAndTrace");
       	 if (level.equals("ALL")) {
       		 out.println("Level: ALL");
   	         for (int j = 0; j < numIterations; j++) {
   	        	 //only do this once for each iteration
   	             if (secondsToSleep > 0) Thread.sleep(secondsToSleep);
   	             logger.logp(java.util.logging.Level.FINE, loggerName, "Method.Fine",  logMessage);
   	             logger.logp(java.util.logging.Level.FINER, loggerName, "Method.Finer",  logMessage);
   	             logger.logp(java.util.logging.Level.FINEST, loggerName, "Method.Finest",  logMessage);
   	             logger.logp(java.util.logging.Level.SEVERE, loggerName, "Method.Severe",  logMessage);
   	             logger.logp(java.util.logging.Level.WARNING, loggerName, "Method.Warning",  logMessage);
   	             logger.logp(java.util.logging.Level.INFO, loggerName, "Method.Info",  logMessage);
   	         }
       	 } else {
       		    out.println("Level: " + passedLevel);
                for (int i = 0; i < numIterations; i++) {
      	        	//only do this once for each iteration
      	            if (secondsToSleep > 0) Thread.sleep(secondsToSleep);
                    logger.logp(passedLevel, loggerName, passedLevel.getName(),  logMessage) ;
                }
         }
     } else {
    	 out.println("Action performed: NOTHING");
     }
     out.println("<h3>End time of JSP load: " + new java.util.Date() + "</h3>");
}
%>
</body>
</html>
