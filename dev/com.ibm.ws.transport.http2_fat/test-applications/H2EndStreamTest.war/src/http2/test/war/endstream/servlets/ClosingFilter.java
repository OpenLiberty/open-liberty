package http2.test.war.endstream.servlets;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

@WebFilter(urlPatterns = { "/large-body", "/large-body-flush" })
public class ClosingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("[ClosingFilter] initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpResp = (HttpServletResponse) response;
        WrappedResponse wrapper = new WrappedResponse(httpResp);

        System.out.println("[ClosingFilter] before chain.doFilter()");
        chain.doFilter(request, wrapper);
        System.out.println("[ClosingFilter] after chain.doFilter(), closing output stream");

        try {
            wrapper.closeOutput();
            System.out.println("[ClosingFilter] output stream closed successfully (no bug)");
        } catch (IOException e) {
            System.out.println("[ClosingFilter] *** BUG TRIGGERED *** IOException on close(): " + e.getMessage());
            System.out.println("[ClosingFilter] Cause: " + (e.getCause() != null ? e.getCause().getClass().getName() + ": " + e.getCause().getMessage() : "none"));
            try {
                httpResp.setHeader("X-Close-Error", "true");
            } catch (Exception ignored) {}
            throw e;
        }
    }

    @Override
    public void destroy() {}

    private static class WrappedResponse extends HttpServletResponseWrapper {
        private DelegatingOutputStream wrappedStream;

        WrappedResponse(HttpServletResponse response) { super(response); }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (wrappedStream == null) {
                wrappedStream = new DelegatingOutputStream(super.getOutputStream());
            }
            return wrappedStream;
        }

        void closeOutput() throws IOException {
            if (wrappedStream != null) { wrappedStream.close(); }
        }
    }

    private static class DelegatingOutputStream extends ServletOutputStream {
        private final ServletOutputStream delegate;
        DelegatingOutputStream(ServletOutputStream delegate) { this.delegate = delegate; }

        @Override public void write(int b) throws IOException { delegate.write(b); }
        @Override public void write(byte[] b) throws IOException { delegate.write(b); }
        @Override public void write(byte[] b, int off, int len) throws IOException { delegate.write(b, off, len); }
        @Override public void flush() throws IOException { delegate.flush(); }
        @Override public void close() throws IOException {
            System.out.println("[DelegatingOutputStream] close() called");
            delegate.close();
        }
        @Override public boolean isReady() { return delegate.isReady(); }
        @Override public void setWriteListener(WriteListener wl) { delegate.setWriteListener(wl); }
    }
}
