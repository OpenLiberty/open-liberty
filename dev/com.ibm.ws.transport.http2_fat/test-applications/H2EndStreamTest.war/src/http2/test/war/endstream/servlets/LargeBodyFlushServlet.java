package http2.test.war.endstream.servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/large-body-flush")
public class LargeBodyFlushServlet extends HttpServlet {

    private static final int CHUNK_SIZE = 8 * 1024;
    private static final int TOTAL_SIZE = 48 * 1024;
    private static final int NUM_CHUNKS = TOTAL_SIZE / CHUNK_SIZE;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/octet-stream");
        resp.setContentLength(TOTAL_SIZE);

        byte[] block = new byte[CHUNK_SIZE];
        for (int i = 0; i < block.length; i++) {
            block[i] = (byte) ('A' + (i % 26));
        }

        ServletOutputStream out = resp.getOutputStream();
        for (int i = 0; i < NUM_CHUNKS; i++) {
            out.write(block);
            out.flush();
        }
    }
}
