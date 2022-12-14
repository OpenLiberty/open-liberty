package batch.fat.web.customlogic;

import java.io.IOException;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import batch.fat.artifacts.EndOfJobNotificationListener;
import batch.fat.common.util.JobWaiter;
import batch.fat.common.util.TestFailureException;

/**
 * Servlet implementation class SimpleServlet
 */
@WebServlet("/ProcessItemExceptionServlet")
public class ProcessItemExceptionServlet extends HttpServlet {

    Logger logger = Logger.getLogger("com.ibm.ws.jbatch.open_fat");

    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public ProcessItemExceptionServlet() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            JobOperator jo = BatchRuntime.getJobOperator();
            long jobId = jo.start("ProcessItemException", null);
            JobExecution jobExecution = jo.getJobExecution(jobId);
            String jobName = jobExecution.getJobName();
            logger.fine("jobId: " + jobId + " jobName: " + jobName);

            new JobWaiter(JobWaiter.FAILED_STATE_ONLY).waitForAfterJobNotificationThenFinalState(EndOfJobNotificationListener.class, jobExecution.getExecutionId());

            response.getWriter().println(jo.getStepExecutions(jobExecution.getExecutionId()).get(0).getExitStatus());
        } catch (TestFailureException e) {
            String errorMsg = "ERROR: " + e.getMessage();
            response.getWriter().println(errorMsg);
        }
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}
