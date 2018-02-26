package com.SFTP_T24_TO_OFAC;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
//import java.sql.Timestamp;
//import java.util.Calendar;

import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbUserException;
import com.ibm.broker.plugin.MbXMLNSC;

public class SFTP_T24_OFAC_ASYNC_Flow_FilePostNotification extends
		MbJavaComputeNode {

	public void evaluate(MbMessageAssembly inAssembly) throws MbException {
		MbOutputTerminal out = getOutputTerminal("out");
		//MbOutputTerminal alt = getOutputTerminal("alternate");
//		PreparedStatement statement = null;
		MbMessage inMessage = inAssembly.getMessage();
		MbMessageAssembly outAssembly = null;
		PreparedStatement preparedStatement = null;
		Logger log=Logger.getLogger(SFTP_T24_OFAC_ASYNC_Flow_FilePostNotification.class);
		String logConfigPath=(String)getUserDefinedAttribute("LogFilePath");
		log.info("LogFilePath---->"+logConfigPath);
		String JDBCProvider = (String) getUserDefinedAttribute("JDBCProvider");
		String tableName     = (String) getUserDefinedAttribute("TableName");
		String pendingFileCode=(String) getUserDefinedAttribute("PendingFileCode");
		String pendingFileCodeDescription=(String) getUserDefinedAttribute("PendingFileDescription");
		String notifiedFileDescription     = (String) getUserDefinedAttribute("NotifiedFileDescription");
		String notifiedFileCode     = (String) getUserDefinedAttribute("NotifiedFileCode");
		
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
			log.info("started processing the SFTP File successful post Flow");
			MbMessage outMessage = new MbMessage();
			outAssembly = new MbMessageAssembly(inAssembly, outMessage);
			// ----------------------------------------------------------
			// init DB 
			copyMessageHeaders(inMessage,outMessage);
			
			 MbMessage globalEnv = inAssembly.getGlobalEnvironment();
			 Connection connection=null;
			 
			 MbElement UUID = globalEnv.getRootElement().getFirstElementByPath("MSGID");
			 //boolean mbFileValid =(boolean) globalEnv.getRootElement().getFirstElementByPath("./Filevalid").getValue();
			 log.info("UUID Value------>" + UUID.getValueAsString());
			 MbElement File = inAssembly.getLocalEnvironment().getRootElement().getFirstElementByPath("./File");
		     MbElement Filename = File.getFirstElementByPath("./Name");
		     log.info("FileName ------->"+ Filename.getValue().toString());
//		     MbElement writtenfileTimestamp = inAssembly.getLocalEnvironment().getRootElement().getFirstElementByPath("./WrittenDestination/File/Timestamp");
		     String filePostNotificationMessageName=(String)getUserDefinedAttribute("JMSFilePostMessageType");
			 MbElement parser=outAssembly.getMessage().getRootElement().createElementAsLastChild(MbXMLNSC.PARSER_NAME);
  		     MbElement FilePostNotification=parser.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,filePostNotificationMessageName,null);
  		     FilePostNotification.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"UUID",UUID.getValueAsString());
  		     FilePostNotification.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"FILE_NAME",Filename.getValueAsString()); // need to change dynamic
			 FilePostNotification.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"FILE_POSTED","true");
//  		     log.info("FilePosted succesfully and Status posted to JMS Consumer");
//  		     String JDBCProviders = (String) getUserDefinedAttribute("jdbcProviders");
  		     
			try{
				connection = this.getJDBCType4Connection(JDBCProvider, JDBC_TransactionType.MB_TRANSACTION_AUTO);
				log.info(UUID.getValueAsString() +" - connection to the database is successfully made ");
			} catch (Exception e) {
			// TODO: handle exception
				log.error(UUID.getValueAsString() +" - DB connection error ");
				log.error(UUID.getValueAsString() +" - "+e.getMessage());
				throw new MbUserException(this, "evaluate()", "", "","DB connection Error",null);
			}
			
			// log the status of file being posted successfully 
			 String xmlAuditTableUpdateQuery = "UPDATE " + tableName +  " SET Status = ?,Description = ?  WHERE CifID =CONVERT(uniqueidentifier,?)";
			 
			 log.info(UUID.getValueAsString() +" - sql statement that is being used "+xmlAuditTableUpdateQuery);
			 preparedStatement  = connection.prepareStatement(xmlAuditTableUpdateQuery);
			 preparedStatement .setString(1,pendingFileCode);
			 preparedStatement .setString(2,pendingFileCodeDescription);
			 preparedStatement .setString(3, UUID.getValueAsString());
		     int result=preparedStatement.executeUpdate();
		     connection.commit();

		     log.info(UUID.getValueAsString() +" - effected rows from the prepared statement execution "+result);
		     log.info(UUID.getValueAsString()+" - updated database with pending file processing status update " );

		     // 
		     xmlAuditTableUpdateQuery = "UPDATE " + tableName +  " SET Status = ?,Description = ?  WHERE CifID =CONVERT(uniqueidentifier,?)";
			 
			 log.info(UUID.getValueAsString() +" - sql statement that is being used "+xmlAuditTableUpdateQuery);
			 preparedStatement  = connection.prepareStatement(xmlAuditTableUpdateQuery);
			 preparedStatement .setString(1,notifiedFileCode);
			 preparedStatement .setString(2,notifiedFileDescription);
			 preparedStatement .setString(3, UUID.getValueAsString());
		     result=preparedStatement.executeUpdate();
		     connection.commit();


		     log.info(UUID.getValueAsString() +" - effected rows from the prepared statement execution "+result);
		     log.info(UUID.getValueAsString()+" - updated database with notified file processing status update " );

			// ----------------------------------------------------------
		} catch (MbException e) {
			// Re-throw to allow Broker handling of MbException
			throw e;
		} catch (RuntimeException e) {
			// Re-throw to allow Broker handling of RuntimeException
			throw e;
		} catch (Exception e) {
			// Consider replacing Exception with type(s) thrown by user code
			// Example handling ensures all exceptions are re-thrown to be handled in the flow
			throw new MbUserException(this, "evaluate()", "", "", e.toString(),
					null);
		}
		finally{
			if(preparedStatement!=null)
				try {
					preparedStatement.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					log.error("prepared statement exception");
					log.error(e.getMessage());
					e.printStackTrace();
				}
		}
		// The following should only be changed
		// if not propagating message to the 'out' terminal
		
		out.propagate(outAssembly);
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
