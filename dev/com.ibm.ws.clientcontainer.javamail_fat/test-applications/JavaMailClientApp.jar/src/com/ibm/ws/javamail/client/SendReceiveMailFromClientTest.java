package com.ibm.ws.javamail.client;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Map.Entry;

import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SendReceiveMailFromClientTest  {
	PrintStream out = System.out;
	
	Class<?> c = SendReceiveMailFromClientTest.class;
	public static int messageNumber = 1;
	
	
	private void logSession(Session ses, PrintStream stream) {
	    out.println("\nSession: " + ses);
	    for ( Entry<Object, Object> entry: ses.getProperties().entrySet()) {
	    	out.println("    Key=" + entry.getKey() + ", Value=" + entry.getValue());
	    }
	    out.println("\n");
	}
	
	/**
	 * Send and receive email using passed mail session. Will generate a unique subject line and then 
	 * check for that  subject when receiving email. Prints out success messages to log on send and receive.
	 * The log can be scanned later to verify that the test passsed.
	 *  
	 * @param mailSessionToUse
	 * @throws IOException
	 */
	void sendReceive(Session mailSessionToUse, String testName) throws  Exception {
		
		final String METHOD = "sendReceive()";
		out.println("Entering " + c + "." + METHOD + " Session: " + mailSessionToUse +
				". TestName: " + testName);
        logSession(mailSessionToUse, out);
		try {
			// As part of the test, all sessions will be configured with the below 
			// address for the 'from' and checked when retrieving email.
			String fromUser = "fat@testserver.com";
			
			String toUser = "user@testserver.com";
			String subject = "Client container test message " + messageNumber++ + 
					" sent at time: " + System.currentTimeMillis();
			
			InternetAddress address = new InternetAddress(toUser);
			Message msg = new MimeMessage(mailSessionToUse);
			msg.setSubject(subject);
			msg.setText("Hello!");
			msg.setFrom();
			
			Transport t = mailSessionToUse.getTransport("smtp" );
			if (mailSessionToUse.getProperty("my.bypass.user")!=null && 
					mailSessionToUse.getProperty("my.bypass.password")!=null) {
				t.connect(mailSessionToUse.getProperty("my.bypass.user"), mailSessionToUse.getProperty("my.bypass.password"));
			}
			else {
			    t.connect();
			}
			t.sendMessage(msg, new Address[]{address});
			
			//print message that FAT controller will check for as part of condition that a test passed
            out.println(testName + ": Test Application client called Transport.sendMessage()");
            
			//retrieve the message just sent.			
			Store store = mailSessionToUse.getStore();
			if (mailSessionToUse.getProperty("my.bypass.user")!=null && 
					mailSessionToUse.getProperty("my.bypass.password")!=null) {
				store.connect(mailSessionToUse.getProperty("my.bypass.user"), mailSessionToUse.getProperty("my.bypass.password"));
			}
			else {
			    store.connect();
			}
			Folder inbox = store.getFolder("INBOX");
			inbox.open(Folder.READ_WRITE);
			msg = inbox.getMessage(inbox.getMessageCount());  
			
			out.println("=========== Start message headers ===========");   
            out.println("Message count: " + inbox.getMessageCount()); 
			Enumeration en = msg.getAllHeaders();
            boolean foundExpectedFromHeader=false, foundExpectedSubjectHeader=false;
            
			while(en.hasMoreElements()){
				Header h = (Header)en.nextElement();
				out.println(h.getName() + " <---> "+ h.getValue());
				if ("From".equals(h.getName()) && fromUser.equals(h.getValue())) {
					foundExpectedFromHeader=true;
				}
				if ("Subject".equals(h.getName()) && subject.equals(h.getValue())) {
					foundExpectedSubjectHeader=true;
				}
			}
			out.println("============= end message headers =================");
			
			if (foundExpectedFromHeader && foundExpectedSubjectHeader) {
				//print message that FAT controller will search for in output
				out.println(testName + 
                    ": Application client received email with expected 'From' and 'Subject' headers.");
			}
			store.close();
		} catch (Exception mex) {
			mex.printStackTrace(out);
		}
	}
}
