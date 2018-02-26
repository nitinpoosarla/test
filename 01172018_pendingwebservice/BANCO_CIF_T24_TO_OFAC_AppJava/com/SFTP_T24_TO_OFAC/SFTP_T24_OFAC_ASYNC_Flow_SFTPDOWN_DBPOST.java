package com.SFTP_T24_TO_OFAC;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.MbBLOB;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbUserException;

public class SFTP_T24_OFAC_ASYNC_Flow_SFTPDOWN_DBPOST extends
		MbJavaComputeNode {

	@SuppressWarnings("rawtypes")
	public void evaluate(MbMessageAssembly inAssembly) throws MbException {
		MbOutputTerminal out = getOutputTerminal("out");
		//MbOutputTerminal alt = getOutputTerminal("alternate");

		MbMessage inMessage = inAssembly.getMessage();
		MbMessageAssembly outAssembly = null;
		PreparedStatement preparedStatement = null;

		Logger log=Logger.getLogger(SFTP_T24_OFAC_ASYNC_Flow_SFTPDOWN_DBPOST.class);
		String logConfigPath=(String)getUserDefinedAttribute("LogFilePath");
		log.info("FileLogPath----->"+logConfigPath);
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
			log.info("started processing the sftp file post failed message flow");
			MbMessage outMessage = new MbMessage();
			outAssembly = new MbMessageAssembly(inAssembly, outMessage);
			// ----------------------------------------------------------
			// Add user code below
			MbMessage globalEnv = inAssembly.getGlobalEnvironment();
			 MbElement UUID = globalEnv.getRootElement().getFirstElementByPath("MSGID");
			// init DB 
				String JDBCProvider = (String) getUserDefinedAttribute("JDBCProvider");
				String tableName     = (String) getUserDefinedAttribute("TableName");
				String fileFailedPostDesc     = (String) getUserDefinedAttribute("FilePostFailDescription");
				String fileFailedStatusCode  = (String) getUserDefinedAttribute("FilePostFailCode");  
			log.info("Handling the Exception for file Failed to post into Server");	
			copyMessageHeaders(inMessage, outMessage);
			String Description=" ";
	           boolean bText =true;
	           String Text="";
				@SuppressWarnings("rawtypes")
				List Number_Of_Elements=new ArrayList();
				int i = 0;	
				
				MbElement RecoverableException=(MbElement)inAssembly.getExceptionList().getRootElement().getLastChild();
				 while(RecoverableException.getValueAsString()== null)
				 {
					 Number_Of_Elements=(List)RecoverableException.evaluateXPath("*");
					 if(Number_Of_Elements.size()==2)
					 {
						    @SuppressWarnings("unchecked")
							List <MbElement> local =(List<MbElement>)RecoverableException.getParent().evaluateXPath("*");
						    String find="Text";
						    for ( i = 0; i < local.size(); i++)
						    {
						        if (find.equals(local.get(i).getName()))
						             break;
						    } 
						    
						 	while(i<local.size())
						 	{
						 		if (!RecoverableException.getFirstElementByPath("./Text").getValueAsString().isEmpty())
						 		{
						 			bText = false;
						 		    Description = Description + ":" + RecoverableException.getFirstElementByPath("./Text").getValueAsString()+"=>"+Text;
						 		}

						 		if(i!=(local.size()-1))
						 		{
						 			if (!local.get(i).getNextSibling().getLastChild().getValueAsString().isEmpty())
						 			{
						 				Text=local.get(i).getNextSibling().getLastChild().getValueAsString();
						 			}
						 		}
						 		i++;
						 	}
					 }
					 else
					 {
						 if (!Description.contains(RecoverableException.getFirstElementByPath("./Text").getValueAsString()))
						 Description=RecoverableException.getFirstElementByPath("./Text").getValueAsString()+","+" "+ Description;
					 }
				 
					 if(Number_Of_Elements.size()>2)
					 { 
						 String Name=null;	  
						 Name=RecoverableException.getFirstElementByPath("./Name").getValueAsString();
						 if((Name.isEmpty())!=true)
						 {   
							 @SuppressWarnings("unused")
							int EndIndex=RecoverableException.getFirstElementByPath("./Name").getValueAsString().indexOf("#");
						 }	 
						 Name=RecoverableException.getFirstElementByPath("./Label").getValueAsString();
						 if((Name.isEmpty())!=true) {
						}
					 }
					 RecoverableException=RecoverableException.getLastChild();
				 
				 	}
				 
				 if (bText)
					 Description = Description + ":=>"+Text;
				 
				 MbElement file = inAssembly.getLocalEnvironment().getRootElement().getFirstElementByPath("./File");
			     MbElement Filename = file.getFirstElementByPath("./Name");
			log.info("sftp post failed for the Filename--->" + Filename.getValueAsString());        
			
//			String EOF=(String)getUserDefinedAttribute("EOF");
            MbElement outRoot = outMessage.getRootElement();
		    MbElement outParser=outRoot.createElementAsLastChild(MbBLOB.PARSER_NAME);
//            outParser.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"BLOB",email_text.getBytes());
//            log.info("Email Data to Post--->" + email_text.getBytes());
            ////database updation fileposting failed
            
          //  String JDBCProviders = (String) getUserDefinedAttribute("jdbcProviders");
 		     Connection connection=null;
 		    try{
				connection = this.getJDBCType4Connection(JDBCProvider, JDBC_TransactionType.MB_TRANSACTION_AUTO);
				log.info(UUID.getValueAsString()+" - connection to the database is successfully made ");
			} catch (Exception e) {
			// TODO: handle exception
				log.error(UUID.getValueAsString() +" - DB connection error ");
				log.error(UUID.getValueAsString() +" - "+e.getMessage());
				throw new MbUserException(this, "evaluate()", "", "","DB connection Error",null);
			}
			 
		     String xmlAuditTableUpdateQuery = "UPDATE "+tableName+"  SET Status = ?,Description = ? WHERE CifID =CONVERT(uniqueidentifier,?)";
			 log.info(UUID.getValueAsString()+" - sql statement that is being used "+ xmlAuditTableUpdateQuery);
			 preparedStatement  = connection.prepareStatement(xmlAuditTableUpdateQuery);
		     preparedStatement .setString(1,fileFailedStatusCode);
			 preparedStatement .setString(2,fileFailedPostDesc);
			 preparedStatement .setString(3, UUID.getValueAsString());
			 int result=preparedStatement.executeUpdate();
			 connection.commit();
				log.info(UUID.getValueAsString()+" - effected rows from the prepared statement execution "+result);
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
		log.info("completed the sftp file posted failure message flow");
		// The following should only be changed
		// if not propagating message to the 'out' terminal
//		out.propagate(outAssembly);

	}
	public void copyMessageHeaders(MbMessage inMessage, MbMessage outMessage)
		     throws MbException {
		    MbElement outRoot = outMessage.getRootElement();

		    // iterate though the headers starting with the first child of the root
		    // element
		    MbElement header = inMessage.getRootElement().getFirstChild();
		    while (header != null && header.getNextSibling() != null) // stop before
		                   // the last
		                   // child
		                   // (body)
		    {
		     // copy the header and add it to the out message
		     outRoot.addAsLastChild(header.copy());
		     // move along to next header
		     header = header.getNextSibling();
		    }
		   }
}
