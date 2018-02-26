package com.WEBSERVICE_INTERFACEMON_IH;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbFatalException;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbUserException;
import com.ibm.broker.plugin.MbXMLNSC;

public class WEBSERVICE_INTERFACEMON_IH_SYNC_JMS extends MbJavaComputeNode {

	public void evaluate(MbMessageAssembly inAssembly) throws MbException {
		MbOutputTerminal out = getOutputTerminal("out");
		MbOutputTerminal alt = getOutputTerminal("alternate");

		MbMessage inMessage = inAssembly.getMessage();
		MbMessageAssembly outAssembly = null;
		MbMessageAssembly altAssembly = null;
		Logger log=Logger.getLogger(WEBSERVICE_INTERFACEMON_IH_SYNC_JMS.class);
		String logConfigPath=(String)getUserDefinedAttribute("LogFilePath");
	    String completeFileCode=	(String)getUserDefinedAttribute("CompleteFileCode");
		String completeFileDesc =(String)getUserDefinedAttribute("CompleteFileDescription");
		log.info("logConfigpath---->"+logConfigPath);
		URL logConfigURL=null;
		try {
			logConfigURL = new File(logConfigPath).toURI().toURL();
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if(logConfigURL!=null)
		{
		PropertyConfigurator.configure(logConfigURL);
		}
		try {
			// create new message as a copy of the input
			log.info("started processing the Replay File posting to JMS queue and Response  message flow");
			MbMessage outMessage = new MbMessage();
			outAssembly = new MbMessageAssembly(inAssembly, outMessage);
			//MbMessage altMessage = new MbMessage();
			altAssembly = new MbMessageAssembly(inAssembly, outMessage);
			// ----------------------------------------------------------
			// Add user code below
            copyMessageHeaders(inMessage, outMessage);
            
            MbElement UUID =inAssembly.getGlobalEnvironment().getRootElement().getFirstElementByPath("./UUID");
            log.info("UUID---->"+ UUID.getValueAsString());
            MbElement Filename =inAssembly.getGlobalEnvironment().getRootElement().getFirstElementByPath("./Filename");
            log.info("Filename ---->"+ Filename );
            MbElement parser = outMessage.getRootElement().createElementAsLastChild(MbXMLNSC.PARSER_NAME);
            MbElement response =parser.createElementAsLastChild(MbElement.TYPE_NAME,"int:RESPONSE_MSG",null);
            response.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"STATUS",completeFileDesc);
            response.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"UUID",UUID.getValueAsString());
            out.propagate(outAssembly);
            log.info("Response Message posted to SOAP  Client through out terminal");
            outAssembly.getMessage().getRootElement().getLastChild().delete();
            MbElement parserxml=altAssembly.getMessage().getRootElement().createElementAsLastChild(MbXMLNSC.PARSER_NAME);
 		     MbElement FilePostNotification=parserxml.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"FilePostNotification",null);
 		     FilePostNotification.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"UUID",UUID.getValueAsString());
 		     FilePostNotification.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"FILE_POSTED",completeFileDesc);
 		     FilePostNotification.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"FILE_NAME",Filename.getValueAsString()); // need to change dynamic
 		    log.info(" Message posted to JMS  Queue through  alt terminal");
			// End of user code
			// ----------------------------------------------------------
		} catch (MbException e) {
			// Re-throw to allow Broker handling of MbException
			log.error(e.getMessage());
			throw e;
		} catch (RuntimeException e) {
			// Re-throw to allow Broker handling of RuntimeException
			log.error(e.getMessage());
			throw e;
		} catch (Exception e) {
			// Consider replacing Exception with type(s) thrown by user code
			// Example handling ensures all exceptions are re-thrown to be handled in the flow
			log.error(e.getMessage());
			throw new MbUserException(this, "evaluate()", "", "", e.toString(),
					null);
		}
		// The following should only be changed
		// if not propagating message to the 'out' terminal
		//out.propagate(outAssembly);
		alt.propagate(altAssembly);
	}
	public void copyMessageHeaders(MbMessage inMessage, MbMessage outMessage)
		     throws MbException {
		    MbElement outRoot = outMessage.getRootElement();

		    // iterate though the headers starting with the first child of the root
		    // element
		    MbElement header = inMessage.getRootElement().getFirstChild();
		    while (header != null && header.getNextSibling() != null) // stop before the last child (body)
		    {
		     // copy the header and add it to the out message
		     outRoot.addAsLastChild(header.copy());
		     // move along to next header
		     header = header.getNextSibling();
		    }
		   }
}
