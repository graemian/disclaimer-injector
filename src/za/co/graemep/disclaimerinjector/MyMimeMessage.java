package za.co.graemep.disclaimerinjector;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

public class MyMimeMessage extends MimeMessage {
 
     private String messageID;
     
     public MyMimeMessage(Session session) {
         super(session);
     }
     
     public MyMimeMessage(MimeMessage msg) throws MessagingException {
         super(msg);
     }
     
     @Override
     protected void updateMessageID() throws MessagingException {
     
         setHeader("Message-ID", messageID);
     }
 
     public String getMessageID() {
         return messageID;
     }

     public void setMessageID(String messageID) {
         this.messageID = messageID;
     }
 }
