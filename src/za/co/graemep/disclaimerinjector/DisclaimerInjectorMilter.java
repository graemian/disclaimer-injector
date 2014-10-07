package za.co.graemep.disclaimerinjector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.activation.UnsupportedDataTypeException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePartDataSource;
import javax.mail.internet.MimeMessage.RecipientType;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import com.sendmail.jilter.JilterEOMActions;
import com.sendmail.jilter.JilterHandler;
import com.sendmail.jilter.JilterHandlerAdapter;
import com.sendmail.jilter.JilterStatus;
import com.sun.mail.smtp.SMTPMessage;


public class DisclaimerInjectorMilter extends JilterHandlerAdapter {

	private static Logger log = Logger.getLogger(DisclaimerInjectorMilter.class);
	
	private static final String X_HAS_MY_DISCLAIMER = "X-Has-My-Disclaimer";
	//private ByteArrayOutputStream body;
	//private InternetHeaders headers;
	
	private ByteArrayOutputStream rawHeaders;
	private ByteArrayOutputStream rawBody;
	
	private InternetAddress fromAddr;
	private List<InternetAddress> rcpt;

	private String from;
	
	//private boolean hasDisclaimer;
	
	public DisclaimerInjectorMilter() {
		
		//headers=new InternetHeaders();
		//body=new ByteArrayOutputStream();
		
		rawHeaders=new ByteArrayOutputStream();
		rawBody=new ByteArrayOutputStream();
		
		//hasDisclaimer=false;
		
		rcpt=new LinkedList<InternetAddress>();
		
	}
	
	@Override
	public int getRequiredModifications() {
		// TODO Auto-generated method stub
		return JilterHandler.SMFIF_CHGBODY | JilterHandler.SMFIF_ADDHDRS | JilterHandler.SMFIF_CHGHDRS;
	}
	
	@Override
	public int getSupportedProcesses() {
		// TODO Auto-generated method stub
		return JilterHandler.PROCESS_BODY | JilterHandler.PROCESS_HEADER | JilterHandler.PROCESS_ENVFROM | JilterHandler.PROCESS_ENVRCPT;
	}
	
	private enum FromDomain {DUOTRONIC, STREAMLINECNC};
	private FromDomain fromDomain;
	
	@Override
	public JilterStatus envfrom(String[] argv, Properties properties) {
		
		try {
			
			from = argv[0];
			log.info("Got message from ["+from+"]");
			
			fromAddr=new InternetAddress(argv[0]);
			
			if (fromAddr.getAddress().toLowerCase().contains("@duotronic.co.za"))
				fromDomain=FromDomain.DUOTRONIC;
			
			else if (fromAddr.getAddress().toLowerCase().contains("@streamlinecnc.co.za")) 
				fromDomain=FromDomain.STREAMLINECNC;
			
			else
				fromDomain=null;
			
			//	if (!(from.getAddress().equalsIgnoreCase("renier@duotronic.co.za") ||  from.getAddress().equalsIgnoreCase("simon@duotronic.co.za") || from.getAddress().equalsIgnoreCase("Joe.Soap@duotronic.co.za")))
			
			if (fromDomain==null || fromAddr.getAddress().toLowerCase().contains("root@"))
				return JilterStatus.SMFIS_ACCEPT;
			else
				return JilterStatus.SMFIS_CONTINUE;
			
		} catch (AddressException e) {
			
			log.error("Problem with address, accepting", e);
			return JilterStatus.SMFIS_ACCEPT;
			
		}
	}
	
	@Override
	public JilterStatus envrcpt(String[] argv, Properties properties) {
		
		try {
			
			rcpt.add(new InternetAddress(argv[0]));
			
			return JilterStatus.SMFIS_CONTINUE;
			
		} catch (AddressException e) {
			
			log.error("Problem with address, accepting", e);
			return JilterStatus.SMFIS_ACCEPT;
			
		}
	}
	
	@Override
	public JilterStatus header(String headerf, String headerv) {
		// TODO Auto-generated method stub
		
		try {
			//headers.addHeader(headerf, headerv);
			
			rawHeaders.write(headerf.getBytes());
			rawHeaders.write(": ".getBytes());
			rawHeaders.write(headerv.getBytes());
			rawHeaders.write("\n".getBytes());
			
			//System.out.println(headerf);
			
			if (headerf.equals(X_HAS_MY_DISCLAIMER)) {
				return JilterStatus.SMFIS_ACCEPT;
				
			}
			
			return JilterStatus.SMFIS_CONTINUE;
			
		} catch (IOException e) {
			
			log.error("IO problem, accepting", e);
			return JilterStatus.SMFIS_ACCEPT;
			
		}
		
	}
	
	@Override
	public JilterStatus body(ByteBuffer bodyp) {
		
		try {
			
			rawBody.write(bodyp.array());
			
			return JilterStatus.SMFIS_CONTINUE;
			
		} catch (IOException e) {
			
			log.error("IO problem, accepting", e);
			return JilterStatus.SMFIS_ACCEPT;
			
		}
		
	}
	
	
	@Override
	public JilterStatus eom(JilterEOMActions eomActions, Properties properties) {
		
		try {
			
			Properties p=new Properties();
			//p.put("mail.smtp.host", "smtp.afrihost.co.za");
			
			Session session = Session.getDefaultInstance(p);
			
			ByteArrayOutputStream rawMessage=new ByteArrayOutputStream();
			rawMessage.write(rawHeaders.toByteArray());
			rawMessage.write("\n".getBytes());
			rawMessage.write(rawBody.toByteArray());
			
			log.info("Size is ["+rawMessage.size()+"]");
			
			MimeMessage oldMsg = new MimeMessage(session, new ByteArrayInputStream(rawMessage.toByteArray()));
			
			Rebuilder rebuilder;
			
			if (fromDomain==FromDomain.DUOTRONIC)
				rebuilder=new Rebuilder("/etc/disclaimer-duotronic.html","/etc/disclaimer-duotronic.txt");
			
			else if (fromDomain==FromDomain.STREAMLINECNC)
				rebuilder=new Rebuilder("/etc/disclaimer-streamlinecnc.html","/etc/disclaimer-streamlinecnc.txt");
			
			else
				throw new RuntimeException("Can't rebuild message for unknown domain");
			
			MyMimeMessage newMsg = (MyMimeMessage) rebuilder.rebuildMessage(oldMsg);
			
			newMsg.setHeader(X_HAS_MY_DISCLAIMER, "Yes");	
			
			
			StringBuffer b=new StringBuffer();
			
			for (InternetAddress ia: rcpt) {
				b.append(ia);
				b.append(" ");
			}
		
			log.info("Added disclaimer to message from ["+fromAddr+"] to ["+b.toString()+"]");
		
			String fromStr = fromAddr.toString().toLowerCase();
			
			if (fromStr.contains("joe.soap@duotronic.co.za") || fromStr.contains("simon@duotronic.co.za")) {
			
				// This doesn't work - replacement body is still the same as original body. WTF?
				
//				log.info("Sending is Joe Soap, using experimental body replacement stuff");
//				
//				mm.saveChanges();
//				
//				byte[] body = IOUtils.toByteArray( mm.getRawInputStream() );
//				
//				log.info("Replacement body is [" + new String(body) + "]");
//				
//				eomActions.replacebody( ByteBuffer.wrap( body ) );
//							
//			    return JilterStatus.SMFIS_CONTINUE;   
//				
			    
			    String messageID = oldMsg.getMessageID();
			    log.info("Old message ID is [" + messageID + "]");
				
			    newMsg.setMessageID(messageID);
				
			    Transport.send(newMsg, rcpt.toArray(new InternetAddress[0]));
				
			    messageID = newMsg.getMessageID();
			    log.info("New message ID is [" + messageID + "]");
				
			    return JilterStatus.SMFIS_DISCARD;   
				
				
			} else {
				
				Transport.send(newMsg, rcpt.toArray(new InternetAddress[0]));
							
			    return JilterStatus.SMFIS_DISCARD;   
				
			}
								
			
		} catch (IOException e) {

			log.error("IO problem, accepting", e);
			return JilterStatus.SMFIS_ACCEPT;

		} catch (MessagingException e) {
			
//			if (e.getCause() instanceof UnsupportedDataTypeException) {
				
			log.error(e);
			log.info("Not adding disclaimer, allowing original message to pass");
			
			return JilterStatus.SMFIS_ACCEPT;
				
		}
	}
	
/*	@Override
	public JilterStatus close() {
	
		return JilterStatus.SMFIS_ACCEPT;
	}*/

}
