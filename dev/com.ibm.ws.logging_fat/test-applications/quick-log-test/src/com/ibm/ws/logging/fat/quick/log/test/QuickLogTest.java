/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.logging.fat.quick.log.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class QuickLogTestServlet
 * 
 * action=log 		uses logger.warning, else logger.fine
 * threads=n  		uses n concurrent threads for logging
 * stepThreads=true	repeats test for 1,2,4,...n threads where n=threads setting
 * duration=n 		performs action for n seconds 
 * delay=n          waits n ms between consecutive log/trace requests
 */
@WebServlet("/QuickLogTest")
public class QuickLogTest extends HttpServlet {
	private static final long serialVersionUID = 1L;
    private static volatile boolean go = false;
	private static int messageSize;
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public QuickLogTest() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter pw = new PrintWriter(response.getOutputStream());
		
		int threads = Integer.parseInt(request.getParameter("threads"));
		int duration = Integer.parseInt(request.getParameter("duration"));
		messageSize = Integer.parseInt(request.getParameter("messageSize"));
		int delay = Integer.parseInt(request.getParameter("delay"));
		String action = request.getParameter("action");
		String stepThreadsString = request.getParameter("stepThreads");
		boolean stepThreads = Boolean.parseBoolean(stepThreadsString);
		
		runTests(pw, threads, duration, delay, action, stepThreads);
	}
	
	public static void main(String[] args) {
		PrintWriter pw = new PrintWriter(System.out);
		runTests(pw, 8, 5, 100, "log", false);
	}

	private static void runTests(PrintWriter pw, int reqThreads, int duration, int delay, String action, boolean stepThreads) {

		List<RunResult> runResults = new ArrayList<RunResult>();
		
		// set up an array with the number of threads that will be used for each run
		int numThreads[];
		if (stepThreads) {
			numThreads = new int[reqThreads];
			for (int i=1; i<=reqThreads; i++) {
				numThreads[i-1] = i;
			}
		}
		else {
			numThreads = new int[1];
			numThreads[0] = reqThreads;
		}
		
		// set up an array with the action for each run
		String actions[];
		if (action != null && action.equals("log")) {
			actions = new String[1];
			actions[0] = "log";
		}
		else if (action != null && action.equals("trace")) {
			actions = new String[1];
			actions[0] = "trace";
		}
		else {
			actions = new String[2];
			actions[0] = "trace";
			actions[1] = "log";
		}
		
		// run the tests
		for (String a : actions) {
			for (int i : numThreads) {
				System.out.println("threads="+i);
				System.out.println("action="+a);
				runResults.add(runTest(i, duration, delay, a));
			}
		}
		
    	// output the results to the browser
		pw.println("<HTML>");
		pw.println("<BODY>");
		pw.println("<h1>Quick log test 2</h1>");

		pw.println("<table border=\"1\">");
    	pw.println("<th>Action</th>");
    	pw.println("<th>Threads</th>");
    	pw.println("<th>Duration (s)</th>");
    	pw.println("<th>Delay (ms)</th>");
    	pw.println("<th>Actions Completed</th>");
    	pw.println("<th>Actions per Second</th>");
    	for (RunResult rr : runResults) {
    		pw.println("<tr>");
    		pw.println("<td>" + rr.action + "</td>");
    		pw.println("<td>" + rr.reqThreads + "</td>");
    		pw.println("<td>" + rr.duration + "</td>");
    		pw.println("<td>" + rr.delay + "</td>");
    		pw.println("<td>" + rr.actionsCompleted + "</td>");
        	pw.println("<td>" + (int)((float)rr.actionsCompleted / (float)rr.duration) + "</td>");
    		pw.println("</tr>");
    	}
    	pw.println("</table>");

    	pw.println("</BODY>");
		pw.println("</HTML>");
		pw.flush();
	}

	private static RunResult runTest(int reqThreads, int duration, int delay, String action) {

		if (reqThreads <= 0 || duration <= 0 || action == null) 
			return new RunResult(action, reqThreads, duration, delay, 0);
		
		Logger logger = Logger.getLogger("com.ibm.somelogger.QuickLogTest");
		//logger.setLevel(Level.ALL);

		// create the action runnables
        Action actions[] = new Action[reqThreads];
        for (int i = 0; i < actions.length; i++) {
        	if (action.equals("log") && messageSize>0)
            	actions[i] = new LogAction(delay, messageSize);
        	else if (action.equals("log"))
            	actions[i] = new LogAction(delay);
            else if (action.equals("trace") && messageSize>0)
            	actions[i] = new TraceAction(delay, messageSize);
            else if (action.equals("trace"))
            	actions[i] = new TraceAction(delay);
        } 
    	
        sleep(5000);
        
        // create and start the threads
    	Thread threads[] = new Thread[actions.length];
    	for (int i = 0; i < actions.length; i++) {
        	threads[i] = new Thread(actions[i]);
        	threads[i].start();
    	}
    	
    	// get the threads to start logging, and let them continue for specified duration
    	System.out.println("Go...");
    	go = true;
    	for (int i=0; i<duration; i++) {
    		sleep(1000);
    		printSummary(actions);
    	}
    	
    	// stop the threads
    	go = false;
    	System.out.println("Stopping...");
    	for (Thread thread : threads) {
    		try {
				thread.join();
				System.out.println("Thread " + thread + " stopped");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
    	System.out.println("Stopped");
    	
    	// collect the results
    	int totalEntries = 0;
    	for (int i = 0; i < reqThreads; i++) {
        	totalEntries += actions[i].getActionsCompleted();
    	}
    	RunResult runResult = new RunResult(action, reqThreads, duration, delay, totalEntries);
    	
    	return runResult;
	}

	private static void printSummary(Action[] actions) {
		int totalEntries = 0;
		for (Action a: actions) {
			totalEntries += a.getActionsCompleted();
		}
		System.out.println("Total entries: " + totalEntries);
	}
	
	private static void sleep(int i) {
    	try {
			Thread.sleep(i);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
	
	interface Action extends Runnable {
		void warmup();
		
		int getActionsCompleted();
	}
	
	static class LogAction implements Action {
		Logger logger = Logger.getLogger("com.ibm.somelogger.QuickLogTest");
		
		public int actionsCompleted = 0;
		private final int delay;
		private String message="";
		
		public LogAction(int delay) {
			this.delay = delay;
		}
		
		public LogAction(int delay, int messageSize) {
			this.delay = delay;
			for (int i=0;i<messageSize;i++) {
				message+="a";
			}
			message+=" ";
		}
		
		
		@Override
		public void run() {
			while (!go);
			while (go) {
				//logger.logp(Level.FINE, "", "", "hello fine " + actionsCompleted++);
				logger.warning("hello warn " + message + actionsCompleted++);
				if (delay > 0) {
					try {
						Thread.sleep(delay);
					} catch (InterruptedException e) {
					}
				}
				
			}
			
		}

		public void warmup() {
			for (int i=0;i<1000;i++) {
			    logger.warning("hello warn " + i);
			}
		}
		
		public int getActionsCompleted() {
			return actionsCompleted;
		}
	}

	static class TraceAction implements Action {
		Logger logger = Logger.getLogger("com.ibm.somelogger.QuickLogTest");
		
		public int actionsCompleted = 0;
		private final int delay;
		private String message="";
		
		public TraceAction(int delay) {
			this.delay = delay;
		}

		public TraceAction(int delay, int messageSize) {
			this.delay = delay;
			for (int i=0;i<messageSize;i++) {
				message+="a";
			}
			message+=" ";
		}
		
		@Override
		public void run() {
			while (!go);
			while (go) {
				//logger.logp(Level.FINE, "", "", "hello fine " + actionsCompleted++);
				logger.fine("hello fine " + message + actionsCompleted++);
				if (delay > 0) {
					try {
						Thread.sleep(delay);
					} catch (InterruptedException e) {
					}
				}
			}
//			System.out.println("completed " + actionsCompleted + " entries");
		}

		public void warmup() {
			for (int i=0;i<1000;i++) {
			    logger.fine("hello fine " + i);
			}
		}

		public int getActionsCompleted() {
			return actionsCompleted;
		}
	}
	
	static class RunResult {
		String action;
		int reqThreads;
		int duration;
		int delay;
		int actionsCompleted;
		
		public RunResult(String action, int reqThreads, int duration, int delay, int totalEntries) {
	    	this.action = action;
	    	this.reqThreads = reqThreads;
	    	this.duration = duration;
	    	this.delay = delay;
	    	this.actionsCompleted = totalEntries;
		}
		
	}

}