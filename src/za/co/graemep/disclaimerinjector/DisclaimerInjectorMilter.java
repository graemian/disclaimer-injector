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

import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import com.sendmail.jilter.JilterEOMActions;
import com.sendmail.jilter.JilterHandler;
import com.sendmail.jilter.JilterHandlerAdapter;
import com.sendmail.jilter.JilterStatus;
import com.sun.mail.smtp.SMTPMessage;

public class DisclaimerInjectorMilter extends JilterHandlerAdapter {

	static Logger cat = Logger.getLogger(DisclaimerInjectorMilter.class);
	
	private static final String X_HAS_DUOTRONIC_DISCLAIMER = "X-Has-Duotronic-Disclaimer";
	private ByteArrayOutputStream body;
	private InternetHeaders headers;
	private Rebuilder rebuilder;
	
	private ByteArrayOutputStream rawHeaders;
	private ByteArrayOutputStream rawBody;
	
	private InternetAddress fromAddr;
	private List<InternetAddress> rcpt;

	private String from;
	
	
	//private boolean hasDisclaimer;
	
	
	public DisclaimerInjectorMilter() {
		
		headers=new InternetHeaders();
		body=new ByteArrayOutputStream();
		rawHeaders=new ByteArrayOutputStream();
		rawBody=new ByteArrayOutputStream();
		
		rebuilder=new Rebuilder();
		
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
	
	@Override
	public JilterStatus envfrom(String[] argv, Properties properties) {
		
		try {
			
			from = argv[0];
			cat.info("From ["+from+"]");
			
			fromAddr=new InternetAddress(argv[0]);
			
			
			
			if (!fromAddr.getAddress().toLowerCase().contains("@duotronic.co.za"))
			//	if (!(from.getAddress().equalsIgnoreCase("renier@duotronic.co.za") ||  from.getAddress().equalsIgnoreCase("simon@duotronic.co.za") || from.getAddress().equalsIgnoreCase("Joe.Soap@duotronic.co.za")))
				return JilterStatus.SMFIS_ACCEPT;
			
			return JilterStatus.SMFIS_CONTINUE;
			
		} catch (AddressException e) {
			cat.error("Problem with address, accepting", e);
			return JilterStatus.SMFIS_ACCEPT;
			
		}
	}
	
	
	
	@Override
	public JilterStatus envrcpt(String[] argv, Properties properties) {
		
		try {
			rcpt.add(new InternetAddress(argv[0]));
			
			return JilterStatus.SMFIS_CONTINUE;
			
		} catch (AddressException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public JilterStatus header(String headerf, String headerv) {
		// TODO Auto-generated method stub
		
		try {
			headers.addHeader(headerf, headerv);
			
			rawHeaders.write(headerf.getBytes());
			rawHeaders.write(": ".getBytes());
			rawHeaders.write(headerv.getBytes());
			rawHeaders.write("\n".getBytes());
			
			//System.out.println(headerf);
			
			
			if (headerf.equals(X_HAS_DUOTRONIC_DISCLAIMER)) {
				return JilterStatus.SMFIS_ACCEPT;
				
			}
			
			return JilterStatus.SMFIS_CONTINUE;
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		
	}
	
	
	
	@Override
	public JilterStatus body(ByteBuffer bodyp) {
		
		try {
			body.write(bodyp.array());
			rawBody.write(bodyp.array());
			
			return JilterStatus.SMFIS_CONTINUE;
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	
	
	@Override
	public JilterStatus eom(JilterEOMActions eomActions, Properties properties) {
		
		
		try {
			
			
			//if (!hasDisclaimer) {
				
				Properties p=new Properties();
				//p.put("mail.smtp.host", "smtp.afrihost.co.za");
				
				
				Session session = Session.getDefaultInstance(p);
				   
				
				ByteArrayOutputStream rawMessage=new ByteArrayOutputStream();
				rawMessage.write(rawHeaders.toByteArray());
				rawMessage.write("\n".getBytes());
				rawMessage.write(rawBody.toByteArray());
				
				MimeMessage mm=new MimeMessage(session, new ByteArrayInputStream(rawMessage.toByteArray()));
				
				mm=(MimeMessage) rebuilder.rebuildMessage(mm);
				
				mm.setHeader(X_HAS_DUOTRONIC_DISCLAIMER, "Yes");	
				
				StringBuffer b=new StringBuffer();
				
				for (InternetAddress ia: rcpt) {
					b.append(ia);
					b.append(" ");
				}
			
				//SMTPMessage smtpMsg=new SMTPMessage(mm);
			
				//smtpMsg.setEnvelopeFrom(from);
					
				
				cat.info("["+fromAddr+"] to ["+b.toString()+"]");
				
				Transport.send(mm, rcpt.toArray(new InternetAddress[0]));
				//Transport.send(smtpMsg, rcpt.toArray(new InternetAddress[0]));
				
							
			    return JilterStatus.SMFIS_DISCARD;   
				
								
			//} else
			
			//	return JilterStatus.SMFIS_CONTINUE;
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		
		} catch (MessagingException e) {
			
			
			if (e.getCause() instanceof UnsupportedDataTypeException) {
				
				cat.error(e);
				cat.info("Not adding disclaimer, allowing original message to pass");
				
				return JilterStatus.SMFIS_ACCEPT;
			
				
			} else			
				throw new RuntimeException(e);
			
		}
	}
	
/*	@Override
	public JilterStatus close() {
	
		return JilterStatus.SMFIS_ACCEPT;
	}*/

}
