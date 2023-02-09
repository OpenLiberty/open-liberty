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
        if (context) {
            fullContext = workContext.getWorkType();
        } else {
            fullContext = "";
        }
        System.out.println("ALVIN-DEBUG Runnable wrap context=" + context + fullContext);
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
                System.out.println("ALVIN-DEBUG Runnable wrap " + fullContext);
                if (context && fullContext.equals("JCA")) {
                    System.out.println("This runnable has work context. The type is " + fullContext + ".");
                    System.out.println("Entry Map:");
                    System.out.println(r.toString());
                }
                if (workContextService.getWorkContext() != null) {
                    System.out.println("ALVIN-DEBUG found workContext" + workContextService.getWorkContext().toString());
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
        Callable<T> newTask = new Callable<T>() {
            @Override
            public T call() {
                System.out.println("ALVIN-DEBUG Callable<T> wrap");
                if (workContextService.getWorkContext() != null) {
                    System.out.println("ALVIN-DEBUG found workContext" + workContextService.getWorkContext());
                } else if (workContextService.getWorkContext() == null) {
                }
                try {
                    return oldTask.call();
                } catch (Exception e) {
                    return null;
                } finally {
                }
            }
        };
        return newTask;
    }
}