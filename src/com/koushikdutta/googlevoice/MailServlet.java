package com.koushikdutta.googlevoice;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@SuppressWarnings("serial")
public class MailServlet extends HttpServlet {
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
    
    static String reference = "References: <+12065528017.6050c85e7783925a0d0bf04952acd441f9cfaae3@txt.voice.google.com>\n"+ 
    "Message-ID: <+12065528017.b009c6f6991968bfdf8c787bf4929748b434da6c@txt.voice.google.com>\n"+ 
    "Date: Wed, 17 Nov 2010 23:49:19 +0000 \n"+
    "Subject: SMS from Koushik Dutta [(206) 552-8017] \n"+
    "From: \"Koushik Dutta (SMS)\" <12065528017.12065528017.48VR2Fz7N9@txt.voice.google.com>\n"+ 
    "To: koush@koushikdutta.com \n"+
    "Content-Type: text/plain; charset=ISO-8859-1; format=flowed; delsp=yes \n\n"+

    "test sms again\n" +
            "test sms again\n" +
            "test sms again";
            
    static Pattern smsPattern = Pattern.compile("SMS from (.*?) \\[(.*?)\\][\\w\\W]*?Content-Type.*?$([\\w\\W]*?)\\z", Pattern.MULTILINE);
    static Pattern smsPattern2 = Pattern.compile("SMS from (.*?)$[\\w\\W]*?Content-Type.*?$([\\w\\W]*?)\\z", Pattern.MULTILINE);
    static Pattern activationPattern = Pattern.compile("confirm the request:([\\w\\W]*?)If");
    private void doRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logger.info("Received an email!");
        DataInputStream dis = new DataInputStream(req.getInputStream());
        byte[] buf = new byte[dis.available()];
        dis.readFully(buf);
        String mail = new String(buf);
        logger.info(mail);
        
        logger.info("Checking to see if it is an activation mail");
        Matcher m = activationPattern.matcher(mail);
        if (m != null && m.find()) {
            String activationLink = m.group(1);
            URL url = new URL(activationLink);
            dis = new DataInputStream(url.openConnection().getInputStream());
            buf = new byte[dis.available()];
            dis.readFully(buf);
            String activationResult = new String(buf);
            logger.info(activationResult);
            logger.info(mail);
            return;
        }
        
        String sender;
        String phone;
        String message;

        m = smsPattern.matcher(mail);
        logger.info("Checking match");
        if (m == null || !m.find()) {
            m = smsPattern2.matcher(mail);
            if (!m.find()) {
                logger.info("Not an sms");
                return;
            }
            sender = m.group(1);
            phone = m.group(1);
            message = m.group(2);
        }
        else {
            sender = m.group(1);
            phone = m.group(2);
            message = m.group(3);
        }
        logger.info("Path info: " + req.getPathInfo());
        int emailIndex = req.getPathInfo().lastIndexOf('/');
        int domainIndex = req.getPathInfo().lastIndexOf('@');
        if (emailIndex == -1 || domainIndex == -1 || emailIndex >= domainIndex) {
            logger.info("Could not parse out email address");
            return;
        }
        logger.info("Sending sms");
        String clientId = req.getPathInfo().substring(emailIndex + 1, domainIndex);
        clientId = clientId.toUpperCase();
        
        URL url = new URL("https://tickleservice.appspot.com/toast");
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput (true);
        conn.setUseCaches(false);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        // send a toast
        logger.info("Sending toast");
        String toast = message.trim();
        logger.info("Message is: " + message);
        //String title = String.format("%s [%s]", sender, phone);
        String title = sender;
        String params = String.format("toast=%s&title=%s&clientId=%s&applicationId=ClockworkModGoogleVoice", toast, title, clientId);
        DataOutputStream printout;
        printout = new DataOutputStream (conn.getOutputStream ());
        printout.writeBytes (params);
        printout.flush ();
        printout.close ();
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

        resp.getWriter().println(conn.getResponseCode());
        String line;
        while (null != (line = in.readLine())) {
            resp.getWriter().println(line);
        }
        in.close();
        
        
        // prepare a badge/tile
        logger.info("Sending badge");
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query query = pm.newQuery(Registration.class);
        query.setFilter("clientId == clientIdParam");
        query.declareParameters("String clientIdParam");
        List<Registration> registrations = (List<Registration>)query.execute(clientId);
        Registration registration;
        if (registrations.size() > 0) {
            registration = registrations.get(0);
        }
        else {
            logger.info("no registrations found for device");
            registration = new Registration();
            registration.setClientId(clientId);
        }
        
        Integer unreadText = registration.getUnreadTextCount();
        if (unreadText == null)
            unreadText = 0;
        unreadText++;
        registration.setUnreadTextCount(unreadText);
        pm.makePersistent(registration);
        pm.close();
        
        // send a badge/tile update
        params = String.format("title=GVoice&clientId=%s&applicationId=ClockworkModGoogleVoice&count=%d&background=/Background.png", clientId, unreadText);
        url = new URL("https://tickleservice.appspot.com/badge");
        conn = (HttpURLConnection)url.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput (true);
        conn.setUseCaches(false);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        printout = new DataOutputStream (conn.getOutputStream ());
        printout.writeBytes (params);
        printout.flush ();
        printout.close ();
        in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

        resp.getWriter().println(conn.getResponseCode());
        while (null != (line = in.readLine())) {
            resp.getWriter().println(line);
        }
        in.close();
    }
}
