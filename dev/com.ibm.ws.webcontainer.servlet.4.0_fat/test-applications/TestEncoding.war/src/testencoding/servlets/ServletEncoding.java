package testencoding.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
 * Servlet 4.0: test new setting in request and response encoding
 * 
 * context set/get API test is in TestServlet40.war app using SCI
 *
 */

@WebServlet("/ServletEncoding")
public class ServletEncoding extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public ServletEncoding() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String encoding = request.getCharacterEncoding();
        String testType, expectedCharSet, display, explicitEnc;
        PrintWriter writer;

        explicitEnc = request.getHeader("Explicit-ReqEnc");
        if (explicitEnc != null) {
            request.setCharacterEncoding(explicitEnc);
        }

        testType = request.getParameter("type");

        if (testType.equals("request")) {
            expectedCharSet = request.getParameter("expected");
            writer = response.getWriter();
            encoding = request.getCharacterEncoding();

            display = "Test [" + testType + "].  Expected encoding [" + expectedCharSet + "].  Found [" + encoding + "]";
            System.out.println(display);
            writer.println(display + (expectedCharSet.equalsIgnoreCase(encoding) ? " PASS " : " FAIL "));

        } else if (testType.equals("response")) {
            /*
             * test 1. default response encoding from the web.xml
             * 2. app explicitly setCharacterEncoding MUST override the old encoding
             */
            String old_response_enc, new_response_enc;
            boolean setRespEncExplicitly = false;

            expectedCharSet = request.getParameter("expected");
            old_response_enc = response.getCharacterEncoding(); // from the web.xml

            explicitEnc = request.getHeader("Explicit-RespEnc");
            if (explicitEnc != null) {
                response.setCharacterEncoding(explicitEnc);
                setRespEncExplicitly = true;
            }
            new_response_enc = response.getCharacterEncoding();

            display = "Test [" + testType + "]. Old response enc : [" + old_response_enc
                      + "], new response enc : [" + new_response_enc + "], expecting [" + expectedCharSet + "]";

            System.out.println(display);

            writer = response.getWriter();

            writer.println(display + (expectedCharSet.equalsIgnoreCase(new_response_enc) ? " PASS " : " FAIL "));

            if (setRespEncExplicitly) {
                writer.println("Old resp encoding is different than new resp encoding : " + (old_response_enc.equalsIgnoreCase(new_response_enc) ? " FAIL " : " PASS "));
            } else {
                writer.println("Old resp encoding is as same as new resp encoding : " + (old_response_enc.equalsIgnoreCase(new_response_enc) ? " PASS " : " FAIL "));
            }

        } else {
            ServletOutputStream sos = response.getOutputStream();
            sos.println("Hello World. FAIL");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
