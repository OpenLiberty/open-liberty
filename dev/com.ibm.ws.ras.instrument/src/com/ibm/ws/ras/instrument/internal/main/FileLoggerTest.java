package com.ibm.ws.ras.instrument.internal.main;

import java.io.File;

public class FileLoggerTest {
	
	public static final String LOG_NAME_1 = "test/LTITest1_.log";
	public static final String LOG_PREFIX_1 = "test1: ";
	
	public static final String LOG_NAME_2 = "test/LTITest2_.log";
	public static final String LOG_PREFIX_2 = "test2: ";
	
	public static final String LOG_NAME_3 = "test/LTITest3_.log";
	public static final String LOG_PREFIX_3 = "test3: ";
	
	public static void main(String[] args) {
		FileLogger logger_1 = FileLogger.FileLoggerProperties.create( new File(LOG_NAME_1), LOG_PREFIX_1, FileLogger.AUTOFLUSH);
		test(logger_1, !SHORT_TEST);
		
		FileLogger logger_2 = FileLogger.FileLoggerProperties.create( new File(LOG_NAME_2), LOG_PREFIX_2, !FileLogger.AUTOFLUSH);
		test(logger_2, !SHORT_TEST);

		FileLogger logger_3 = FileLogger.FileLoggerProperties.create( new File(LOG_NAME_3), LOG_PREFIX_3, FileLogger.AUTOFLUSH);
		multiTest(logger_3);		
	}

	private static byte[] populate(int length) {
		byte[] bytes = new byte[length];
		
		for ( int byteNo = 0; byteNo < length; byteNo++ ) {
			bytes[byteNo] = (byte) byteNo;
		}		
		
		return bytes;
	}

	public static void dual(String text, FileLogger logger) {
		System.out.println(text);
		logger.log(text);
	}
	
	public static final byte[] TEST_BYTES_0 = populate(0);
	public static final byte[] TEST_BYTES_1 = populate(256);	
	public static final byte[] TEST_BYTES_2 = populate(35);
	
	public static final String BANNER = "----------------------------------------";
	
	public static final boolean SHORT_TEST = true;
	
	public static void test(FileLogger logger, boolean shortTest) {
		logger.log("Basic Output");
		logger.log(BANNER);
		logger.log("Text 0");
		logger.log("ClassName", "Text 1");
		logger.log("ClassName", "MethodName", "Text 2");
		logger.log("ClassName", "MethodName", "Text 3", "Value");
		logger.log(BANNER);
		
		if ( shortTest ) {
			return;
		}
		
		logger.log("Byte Output");
		logger.log(BANNER);
		logger.dump("Bytes 0", TEST_BYTES_0);
		logger.dump("ClassName", "Bytes 1", TEST_BYTES_1);
		logger.dump("ClassName", "MethodName", "Bytes 2", TEST_BYTES_2);
		logger.log(BANNER);

		logger.log("Stack Output (Internal)");
		logger.log(BANNER);
		logger.logStack("Stack 0");
		logger.logStack("ClassName", "Stack 1");
		logger.logStack("ClassName", "MethodName", "Stack 2");
		logger.log(BANNER);

		Throwable th = new Throwable("Dummy");

		logger.log("Stack Output (External)");
		logger.log(BANNER);
		logger.logStack("Captured 0", th);
		logger.logStack("ClassName", "Captured 1", th);
		logger.logStack("ClassName", "MethodName", "Captured 2", th);
		logger.log(BANNER);
	}

	public static final int BATCH_COUNT = 10000;
	public static final int SHORT_LIMIT = 100;
	
	public static void multiTest(FileLogger logger) {
		String outerBanner = "Concurrent logging with [ " + BATCH_COUNT + " ] batches";
		dual("START: " + outerBanner, logger);

		for ( int batchNo = 0; batchNo < BATCH_COUNT; batchNo++ ) {
			String innerBanner;
			if ( (batchNo % 50) == 0 ) {
				innerBanner = "Batch [ " + batchNo + " ]: Concurrent logging with [ " + THREAD_COUNT + " ] threads";
			} else {
				innerBanner = null;
			}
			
			if ( innerBanner != null ) {
				dual("START: " + innerBanner, logger);
			}
			
			multiTest( logger, batchNo, (batchNo > SHORT_LIMIT) );

			if ( innerBanner != null ) {			
				dual("END: " + innerBanner, logger);
			}
		}
		
		dual("END: " + outerBanner, logger);		
	}
	
	public static final int THREAD_COUNT = 40;
	public static final String THREAD_ID_PREFIX = "Test-Thread-";

	public static void multiTest(FileLogger logger, int batchNo, boolean shortTest) {
		Thread[] testThreads = new Thread[THREAD_COUNT];
		
		for ( int threadNo = 0; threadNo < THREAD_COUNT; threadNo++ ) {
			String threadId =
				THREAD_ID_PREFIX +
				Integer.toHexString(batchNo) +
				'-' + Integer.toHexString(threadNo);

			testThreads[threadNo] = new Thread( () -> { test(logger, shortTest); }, threadId );
		}

		for ( int threadNo = 0; threadNo < THREAD_COUNT; threadNo++ ) {
			testThreads[threadNo].start();
		}

		for ( int threadNo = 0; threadNo < THREAD_COUNT; threadNo++ ) {
			try {
				testThreads[threadNo].join();
			} catch ( InterruptedException e ) {
				// Ignore
			}
		}
	}
}
