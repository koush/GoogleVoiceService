package com.koushikdutta.googlevoice;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ResetServlet extends HttpServlet {
    protected static final Logger logger = Logger.getLogger(MailServlet.class.getSimpleName());
    
    protected static void log(String s, Object... args) {
        logger.info(String.format(s, args));
    }
    
    protected void logResponse(PrintWriter resp, String s, Object... args) {
        String full = String.format(s, args);
        logger.info(full);
        resp.println(full);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");
        doRequest(req, resp);
    }
    
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doRequest(req, resp);
    }
    
    private void doRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String clientId = req.getParameter("clientId");
        if (clientId == null) {
            resp.sendError(500, "clientId must not be null");
            return;
        }
        clientId = clientId.toUpperCase();
        
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query query = pm.newQuery(Registration.class);
        query.setFilter("clientId == clientIdParam");
        query.declareParameters("String clientIdParam");
        List<Registration> registrations = (List<Registration>)query.execute(clientId);
        if (registrations.size() == 0) {
            resp.sendError(500, "no registrations found for device");
            return;
        }
        
        
        logger.info("Resetting device counter..");
        Registration registration = registrations.get(0);
        registration.setUnreadTextCount(0);
        registration.setUnreadVoicemailCount(0);
        pm.makePersistent(registration);
        pm.close();
        
        String params = String.format("title=GVoice&clientId=%s&applicationId=ClockworkModGoogleVoice&count=0&background=/Background.png", clientId);
        URL url = new URL("https://tickleservice.appspot.com/badge");
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput (true);
        conn.setUseCaches(false);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        DataOutputStream printout = new DataOutputStream (conn.getOutputStream ());
        printout.writeBytes (params);
        printout.flush ();
        printout.close ();
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

        String line;
        resp.getWriter().println(conn.getResponseCode());
        while (null != (line = in.readLine())) {
            resp.getWriter().println(line);
        }
        in.close();

    }
}
