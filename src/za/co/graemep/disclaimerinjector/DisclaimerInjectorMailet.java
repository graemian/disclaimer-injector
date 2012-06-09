package za.co.graemep.disclaimerinjector;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.GenericSignatureFormatError;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeMessage.RecipientType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.Mailet;
import org.apache.mailet.MailetConfig;


// Deploy: (cd ~/workspace-cramers/duotronic-disclaimer-injector/bin; jar cf /tmp/duotronic-disclaimer-injector.jar .); scp -P 2288 /tmp/duotronic-disclaimer-injector.jar root@bubbles.duotronic.co.za:/opt/james-2.3.2/apps/james/SAR-INF/lib


public class DisclaimerInjectorMailet extends org.apache.mailet.base.GenericMailet {
	


	
	

	private Rebuilder rebuilder=new Rebuilder();
	
	
		
	
	
	
	@Override
	public void service(Mail mail) throws MessagingException {
		

		
		log("Inject!");
		
		try {
						
			MimeMessage oldMsg=mail.getMessage();			
			MimeMessage newMsg=(MimeMessage) rebuilder.rebuildMessage(oldMsg);			
			
			List<Address> a=new LinkedList<Address>();
			
			for (Object maObj: mail.getRecipients()) {
			
				MailAddress ma=(MailAddress) maObj;
				a.add(ma.toInternetAddress());
				
			}
			
			newMsg.addHeader("Has-Duotronic-Disclaimer", "yes");
			
			Transport.send(newMsg,a.toArray(new Address[0]));			
			
			mail.setState(Mail.GHOST);
			
			
		} catch (Exception e) {
			
			log(e.getMessage());
			
			if (e.getCause()!=null)
				log(e.getCause().getMessage());
					
			throw new RuntimeException(e);
			
		}

		
	}
	
	

}
