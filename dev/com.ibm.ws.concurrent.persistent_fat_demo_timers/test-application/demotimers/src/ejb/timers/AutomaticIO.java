package ejb.timers;

import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.annotation.Resource;
import javax.ejb.Schedule;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Timer;

@Singleton
public class AutomaticIO {
	
    @Resource
    private SessionContext sessionContext; //Used to get information about timer
    
    private int count; //Incremented with each execution of timers
    private File file = new File("files/timertestoutput.txt");
    
    /**
     * Cancels timer execution
     */
    public void cancel() {
        for (Timer timer : sessionContext.getTimerService().getTimers())
            timer.cancel();
    }

    /**
     * Get the value of count.
     */
    public int getRunCount() {
        return count;
    }
    
    /**
     * Runs ever other minute. Automatically starts when application starts.
     */
    @Schedule(info = "Performing IO Operations", hour = "*", minute = "*", second = "0")
    public void run(Timer timer) {

    	String output = "Running execution " + ++count + " of timer " + timer.getInfo();
    	
    	if(!file.exists()) {
        	try {
        		file.getParentFile().mkdir();
    			file.createNewFile();
    			System.out.println("File created at location: " + file.getAbsolutePath());
    		} catch (IOException e) {
    			e.printStackTrace();
    			fail(e.getMessage());
    		}
    	}
    	
    	try(BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {        
			writer.append(System.lineSeparator() + output);
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
    	
    	System.out.println(output);
    }
}
