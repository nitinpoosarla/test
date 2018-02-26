package com.JMS_OFAC_IH;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbUserException;

public class JMS_OFAC_IH_ASYNC_flow_JMS_OFAC_IH extends MbJavaComputeNode {

	public void evaluate(MbMessageAssembly inAssembly) throws MbException {
		MbOutputTerminal out = getOutputTerminal("out");
		//MbOutputTerminal alt = getOutputTerminal("alternate");

		PreparedStatement preparedStatement = null;
		MbMessage inMessage = inAssembly.getMessage();
		MbMessageAssembly outAssembly = null;
		
		String failedFileCode=(String)getUserDefinedAttribute("FailedFileCode");
		String failedFileDescription=(String)getUserDefinedAttribute("FailedFileDescription");
		String completeFileCode=(String)getUserDefinedAttribute("CompleteFileCode");
		String completeFileDescription=(String)getUserDefinedAttribute("CompleteFileDescription");
		String jmsSuccessCode=(String)getUserDefinedAttribute("JMSSuccessStatus");
		Logger log=Logger.getLogger(JMS_OFAC_IH_ASYNC_flow_JMS_OFAC_IH.class);
		String logConfigPath=(String)getUserDefinedAttribute("LogFilePath");
		log.info("LogFilepath------>"+logConfigPath);
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
			MbMessage outMessage = new MbMessage(inMessage);
			outAssembly = new MbMessageAssembly(inAssembly, outMessage);

			log.info("started processing the JMS  message flow");
			String fileProcessedMessageType=(String)getUserDefinedAttribute("JMSFileProcessedMessageType");
			
			
			MbElement UUID = inAssembly.getMessage().getRootElement().getLastChild().getFirstElementByPath("./"+fileProcessedMessageType+"/UUID");
			log.info("UUID received from JMS queue in message "+fileProcessedMessageType+" is  ---->" + UUID.getValueAsString());
			
			MbElement FILE_NAME = inAssembly.getMessage().getRootElement().getLastChild().getFirstElementByPath("./"+fileProcessedMessageType+"/FILE_NAME");
			log.info("FILE_NAME received from JMS queue in message "+fileProcessedMessageType+" is  ---->" + FILE_NAME.getValueAsString());
			
			MbElement STATUS = inAssembly.getMessage().getRootElement().getLastChild().getFirstElementByPath("./"+fileProcessedMessageType+"/STATUS");
			log.info("STATUS received from JMS queue in message "+fileProcessedMessageType+" is  ---->" + STATUS.getValueAsString());
			
			
			log.info("Database connection initiated");
			
			
			
			String jdbcProvider = (String) getUserDefinedAttribute("JDBCProvider");
			String tableName = (String) getUserDefinedAttribute("TableName");
			Connection connection=null;
				
	  		try{
				connection = this.getJDBCType4Connection(jdbcProvider, JDBC_TransactionType.MB_TRANSACTION_AUTO);
				log.info(UUID.getValueAsString()+" - connection to the database is successfully made for updating the status received from JMS listener");
			} catch (Exception e) {
			// TODO: handle exception
				log.error(UUID.getValueAsString()+" - DB connection error ");
				log.error(UUID.getValueAsString()+" - "+e.getMessage());
				throw new MbUserException(this, "evaluate()", "", "","DB connection Error",null);
			}
				 String xmlAuditTableUpdateQuery ="UPDATE "+tableName+" SET Status = ?,Description = ?  WHERE CifID =CONVERT(uniqueidentifier,?) and FileName=?";
				 log.info(UUID.getValueAsString()+" - sql statement that is being used "+xmlAuditTableUpdateQuery); 
				    
				   preparedStatement = connection.prepareStatement(xmlAuditTableUpdateQuery);
				   if(STATUS.getValueAsString().contains(jmsSuccessCode))
				   {
				    preparedStatement.setString(1, completeFileCode);
				    preparedStatement.setString(2, completeFileDescription);
				   }
				   else 
				   {
					    preparedStatement.setString(1, failedFileCode);
					    preparedStatement.setString(2, failedFileDescription);   
					   
				   }
				   preparedStatement.setString(3, UUID.getValueAsString());
				    preparedStatement.setString(4, FILE_NAME.getValueAsString());
				    
				    preparedStatement.executeUpdate();
				    connection.commit();
			       int result=preparedStatement.executeUpdate();
					log.info(UUID.getValueAsString()+" - effected rows from the prepared statement execution "+result);
					log.info(UUID.getValueAsString()+" - updated the status received from JMS into the database");
			       
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
	//	out.propagate(outAssembly);
	}
}
