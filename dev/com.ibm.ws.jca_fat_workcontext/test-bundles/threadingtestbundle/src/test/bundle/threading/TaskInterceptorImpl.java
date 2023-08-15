package test.bundle.threading;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.Callable;

import com.ibm.wsspi.threading.ExecutorServiceTaskInterceptor;
import com.ibm.wsspi.threading.WorkContext;
import com.ibm.wsspi.threading.WorkContextService;

public class TaskInterceptorImpl implements ExecutorServiceTaskInterceptor {
    WorkContextService workContextService;

    protected void setWorkContextService(WorkContextService workContextService) {
        this.workContextService = workContextService;
    }

    protected void unsetWorkContextService(WorkContextService workContextService) {
        if (this.workContextService == workContextService) {
            this.workContextService = null;
        }
    }

    @Override
    public Runnable wrap(final Runnable oldTask) {
        final WorkContext workContext = workContextService.getWorkContext();
        final boolean context = workContext != null;
        final String fullContext;

        System.out.println(" -- JCA TaskInterceptor Runnable wrap Current Thread -- " + Thread.currentThread().toString());

        if (context) {
            fullContext = workContext.getWorkType();
            System.out.println(" -- JCA TaskInterceptor Runnable wrap, context detected, whats the Worktype ? " + fullContext);

        } else {
            fullContext = "";
        }
        System.out.println("TaskInterceptor Runnable wrap context= " + context + " WorkType= " + fullContext);
        final StringBuilder r = new StringBuilder();
        if (context && fullContext.equals("JCA")) {
            for (Map.Entry<String, Serializable> entry : workContext.entrySet()) {
                String key = entry.getKey();
                String value = (String) entry.getValue();
                r.append(key + " : " + value);
                r.append(System.getProperty("line.separator"));
            }
        }
        Runnable newTask = new Runnable() {
            @Override
            public void run() {
                System.out.println(" -- TaskInterceptor Runnable wrap run Thread -- " + Thread.currentThread().toString());
                System.out.println("ALVIN-DEBUG TaskInterceptor Runnable in run method for Worktype= " + fullContext);
                if (context && fullContext.equals("JCA")) {
                    System.out.println("This runnable has work context. The type is " + fullContext + ".");
                    System.out.println("Entry Map:");
                    System.out.println(r.toString());
                }
                if (workContextService.getWorkContext() != null) {
                    System.out.println("Runnable newTask found workContext" + workContextService.getWorkContext().toString());
                }
                try {
                    oldTask.run();
                } finally {
                }
            }
        };
        return newTask;
    }

    @Override
    public <T> Callable<T> wrap(final Callable<T> oldTask) {

        // new code 07-28-23
        final WorkContext workContext = workContextService.getWorkContext();
        final boolean context = workContext != null;
        final String fullContext;

        System.out.println(" -- TaskInterceptor Callable<T> wrap Thread -- " + Thread.currentThread().toString());

        if (context) {
            fullContext = workContext.getWorkType();
            System.out.println(" -- JCA TaskInterceptor Callable wrap, context detected, whats the Worktype ? " + fullContext);

        } else {
            fullContext = "";
        }
        System.out.println("Callable wrap context= " + context + " WorkType= " + fullContext);
        final StringBuilder r = new StringBuilder();
        if (context && fullContext.equals("JCA")) {
            for (Map.Entry<String, Serializable> entry : workContext.entrySet()) {
                String key = entry.getKey();
                String value = (String) entry.getValue();
                r.append(key + " : " + value);
                r.append(System.getProperty("line.separator"));
            }
        }

        // end new code

        Callable<T> newTask = new Callable<T>() {
            @Override
            public T call() {
                //System.out.println("ALVIN-DEBUG Callable<T> wrap");
                System.out.println(" -- TaskInterceptor newTask Callable<T> call Thread -- " + Thread.currentThread().toString());

                // ** original code
                //if (workContextService.getWorkContext() != null) {
                //    System.out.println("ALVIN-DEBUG found workContext" + workContextService.getWorkContext());
                //} else if (workContextService.getWorkContext() == null) {
                //    System.out.println("ALVIN-DEBUG Callable call did not find workContext ");
                //}
                // ** end original code

                // ** New replacement code
                System.out.println("TaskInterceptor newTask call method for Worktype= " + fullContext);
                if (context && fullContext.equals("JCA")) {
                    System.out.println("This runnable has work context. The type is " + fullContext + ".");
                    System.out.println("Entry Map:");
                    System.out.println(r.toString());
                }
                if (workContextService.getWorkContext() != null) {
                    System.out.println("Callable newTask found workContext" + workContextService.getWorkContext().toString());
                }
                // ** New end code 

                try {
                    System.out.println("com.ibm.ws.threading_fat_beforeTask");
                    return oldTask.call();
                } catch (Exception e) {
                    return null;
                } finally {
                    System.out.println("com.ibm.ws.threading_fat_afterTask");
                }
            }
        };
        return newTask;
    }
}