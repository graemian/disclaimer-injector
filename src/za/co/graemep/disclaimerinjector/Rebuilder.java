package za.co.graemep.disclaimerinjector;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class Rebuilder {
	
	private static Logger log = Logger.getLogger(DisclaimerInjectorMilter.class);
	
	
	private String disclaimerTextFile;
	private String disclaimerHtmlFile;
	private static final String CONTENT_TYPE_TEXT = "text/plain";
	private static final String CONTENT_TYPE_HTML = "text/html";
	private boolean firstText=true;
	private boolean firstHTML=true;
	
	public Rebuilder(String disclaimerHtmlFile, String disclaimerTextFile) {
		this.disclaimerHtmlFile=disclaimerHtmlFile;
		this.disclaimerTextFile=disclaimerTextFile;
	}
	
	
	private void log(String txt) {
		System.out.println(txt);
	}

	public Object rebuildMessage(MimeMessage mm) throws IOException, MessagingException {
		
		message=mm;
		return rebuild(mm);
		
	}
	
	private MimeMessage message;
	
	private Object rebuild(Object part) throws IOException, MessagingException {
	
		log("Object is a ["+part.getClass()+"]");
		
		if (part instanceof Message) {
			
			Message oldMsg=(Message) part;
			MimeMessage newMsg=new MyMimeMessage((MimeMessage) oldMsg);	

			Object oldObj=oldMsg.getContent();			
			Object newObj=rebuild(oldObj);
			
			newMsg.setContent(newObj,oldMsg.getContentType());
			
			return newMsg;
			
			/*newMsg.setSubject(oldMsg.getSubject());
			
			newMsg.setRecipients(RecipientType.TO,oldMsg.getRecipients(RecipientType.TO));
			newMsg.setRecipients(RecipientType.CC,oldMsg.getRecipients(RecipientType.CC));
			newMsg.setRecipients(RecipientType.BCC,oldMsg.getRecipients(RecipientType.BCC));
			newMsg.addFrom(oldMsg.getFrom());	*/	
			
			
		} else if (part instanceof Multipart) {

			log("Processing Multipart");

			Multipart oldMP=(Multipart) part;
			MimeMultipart newMP=new MimeMultipart();
			
			ContentType contentType = new ContentType( oldMP.getContentType() );
			newMP.setSubType(contentType.getSubType());
			
			
			log("Multipart is a ["+oldMP.getContentType()+"] with ["+oldMP.getCount()+"] children");
			
			
			// Substitute into each part
			for (int i=0; i<oldMP.getCount(); i++) {
				
				//log("Processing child ["+(i+1)+"]");
				
				BodyPart oldBP=oldMP.getBodyPart(i);
				BodyPart newBP=(BodyPart) rebuild(oldBP);
				
				newMP.addBodyPart(newBP);
				
			}
			
			return newMP;
			
		} else if (part instanceof BodyPart) {
			
			
			BodyPart bp=(BodyPart) part;

			if (bp.getDisposition()!=null && bp.getDisposition().contains("attachment")) {
			
				return bp;
				
			} else if (contentTypeIsHTML(bp.getContentType())) {
				
								
				
				String html = appendHtmlDisclaimer((String) bp.getContent());
				
				log("New body is ["+html+"]");
				
				MimeBodyPart mbp=new MimeBodyPart();
				mbp.setContent(html, CONTENT_TYPE_HTML);
			
				return mbp;
				
				
			} else if (contentTypeIsText(bp.getContentType()) && firstText) {
					
				
					
					String txt = appendTextDisclaimer((String) bp.getContent());
					
					//log("New body is ["+buf.toString()+"]");
					
					MimeBodyPart mbp=new MimeBodyPart();
					mbp.setContent(txt, CONTENT_TYPE_TEXT);
				
					
					
					return mbp;	
				
			} else if (bp.getContent() instanceof Multipart) {
				
				Multipart newMP=(Multipart) rebuild(bp.getContent());
				
				MimeBodyPart mbp=new MimeBodyPart();
				mbp.setContent(newMP);
				
				return mbp;
				
				
			} else 
				
				return bp; 
			
		} else if (part instanceof String) {
			
			if (contentTypeIsHTML(message.getContentType()))			
				return appendHtmlDisclaimer((String) part);
			
			else if (contentTypeIsText(message.getContentType()))
				return appendTextDisclaimer((String) part);
			
			else
				return part;
			
			
		} /*else if (part==null)
			throw new RuntimeException("Null part");*/
	
		else	
			return part;
			//throw new RuntimeException("Unknown part ["+part.getClass().getName()+"]");
			
			
		
	}
	
	private boolean contentTypeIsHTML(String ct) {
		
		return ct.startsWith(CONTENT_TYPE_HTML);
		
	}

	private boolean contentTypeIsText(String ct) {
		
		return ct.startsWith(CONTENT_TYPE_TEXT);
		
	}

	
	
	private String appendDisclaimer(String c, String disclaimerFile) throws IOException,
			FileNotFoundException {
		StringBuffer buf = new StringBuffer(c);
		
		
		String htmlDisclaimer = IOUtils.toString(new FileInputStream(disclaimerFile));
		buf.append(htmlDisclaimer);
		return buf.toString();
	}
	

	
	private String appendHtmlDisclaimer(String c) throws IOException,
			FileNotFoundException {
		
		return appendDisclaimer(c,disclaimerHtmlFile);
		
	}

	private String appendTextDisclaimer(String c) throws IOException,
			FileNotFoundException {
		
		
		return appendDisclaimer(c,disclaimerTextFile);
		
		
	}

	
}
