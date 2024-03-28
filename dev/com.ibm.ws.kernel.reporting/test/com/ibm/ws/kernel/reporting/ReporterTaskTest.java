package com.ibm.ws.kernel.reporting;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReporterTaskTest {

	@Test
	public void testGetCause() {
		Throwable message1 = new Exception("TEST1");

		String response = ReporterTask.buildExceptionMessage(message1);

		assertEquals("java.lang.Exception: TEST1", response);
	}

	@Test
	public void testGetMultipeCauses() {
		Throwable message1 = new Exception("TEST1");
		Throwable message2 = new Exception("TEST2", message1);
		Throwable message3 = new Exception("TEST3", message2);

		String response = ReporterTask.buildExceptionMessage(message3);

		assertEquals("java.lang.Exception: TEST3: java.lang.Exception: TEST2: java.lang.Exception: TEST1", response);
	}

}
