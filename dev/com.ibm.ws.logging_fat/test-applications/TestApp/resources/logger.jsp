<html>
<head>
<title>HPEL Function Test JSP</title>
</head>
<body>

<%
     
     java.util.logging.Logger logger = java.util.logging.Logger.getLogger("ezlogger","JulMessages");
     
            	 logger.logp(java.util.logging.Level.SEVERE, "ezlogger", "Method.Severe",  "severre test message");
   	             logger.logp(java.util.logging.Level.WARNING, "ezlogger", "Method.Warning",  "warn test message");
   	             logger.logp(java.util.logging.Level.INFO, "ezlogger", "Method.Info",  "info test message");
   	             logger.logp(java.util.logging.Level.FINE, "ezlogger", "Method.Fine",  "fine test message");
   	             logger.logp(java.util.logging.Level.FINER, "ezlogger", "Method.Finer",  "finer test message");
   	             logger.logp(java.util.logging.Level.FINEST, "ezlogger", "Method.Finest",  "finest test message");
   	 

%>
</body>
</html>
