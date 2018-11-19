package servlet;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ejb.GuestManagementService;
import jpa.GuestEntity;

/**
 * A servlet which uses JPA to persist guest data.
 */
@WebServlet(urlPatterns = "/GuestBook")
public class GuestBookServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @EJB
    private GuestManagementService mas = null;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            // Retrieve a list of all the JPAEntities currently persisted
            List<GuestEntity> guests = new ArrayList<GuestEntity>();
            mas.retrieveAllGuests().map(e -> e).forEach(m -> guests.add(m));

            request.setAttribute("guests", guests);
            request.getRequestDispatcher("/index.jsp").forward(request, response);
        } catch (Exception e) {
            response.getWriter().println("Something went wrong. Caught exception " + e);
            response.getWriter().flush();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String firstName = request.getParameter("firstName");
            String lastName = request.getParameter("lastName");
            if (firstName != null && lastName != null
                && firstName.length() > 0 && lastName.length() > 0) {

                // Create a new JPAEntity based on the incoming content
                GuestEntity guest = new GuestEntity();
                guest.setFirstName(firstName);
                guest.setLastName(lastName);
                guest.setLocalDateTime(LocalDateTime.now());
                mas.signInGuest(guest);
            }
        } catch (Exception e) {
            response.getWriter().println("Something went wrong. Caught exception " + e);
            response.getWriter().flush();
        }

        doGet(request, response);
    }
}
