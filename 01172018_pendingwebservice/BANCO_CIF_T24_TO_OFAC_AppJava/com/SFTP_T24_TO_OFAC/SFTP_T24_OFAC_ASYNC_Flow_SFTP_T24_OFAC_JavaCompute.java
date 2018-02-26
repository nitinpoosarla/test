package com.SFTP_T24_TO_OFAC;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
//import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
//import java.sql.Timestamp;
//import java.text.SimpleDateFormat;
//import java.util.Date;

import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbUserException;

import com.cleanslatetg.utilities.Utilities;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
//import org.apache.log4j.xml.DOMConfigurator;
//import org.joda.time.DateTime;
//import org.joda.time.format.DateTimeFormat;
//import org.joda.time.format.DateTimeFormatter;


public class SFTP_T24_OFAC_ASYNC_Flow_SFTP_T24_OFAC_JavaCompute extends
		MbJavaComputeNode {

	public void evaluate(MbMessageAssembly inAssembly) throws MbException {
		MbOutputTerminal out = getOutputTerminal("out");
		//MbOutputTerminal alt = getOutputTerminal("alternate");

		MbMessage inMessage = inAssembly.getMessage();
		MbMessageAssembly outAssembly = null;

		PreparedStatement preparedStatement = null;
		
		Boolean bFilevalid = true;
		Logger log=Logger.getLogger(SFTP_T24_OFAC_ASYNC_Flow_SFTP_T24_OFAC_JavaCompute.class);
		String logConfigPath=(String)getUserDefinedAttribute("LogFilePath");
		
		URL logConfigURL=null;
		try {
			logConfigURL = new File(logConfigPath).toURI().toURL();
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		log.info("LogFilePath------>"+logConfigPath);
		
		if(logConfigURL!=null)
		{
		PropertyConfigurator.configure(logConfigURL);
		}
		
		try {
			// create new message as a copy of the input
			log.info("started processing the sftp main message flow");
			MbMessage outMessage = new MbMessage(inMessage);
			outAssembly = new MbMessageAssembly(inAssembly, outMessage);
			// ----------------------------------------------------------
			// init DB 
			String JDBCProvider = (String) getUserDefinedAttribute("JDBCProvider");
			String tableName     = (String) getUserDefinedAttribute("TableName");
			//generate UUID for the current session
			String uuid=Utilities.generateUUID();
			
			MbMessage globalEnv = inAssembly.getGlobalEnvironment();
			globalEnv.getRootElement().createElementAsLastChild(MbElement.TYPE_NAME, "MSGID", uuid);
			
			MbElement inRoot=inMessage.getRootElement();
			byte[] messageBodyByteArray=(byte[]) (inRoot.getFirstElementByPath("/BLOB/BLOB").getValue());
	        String messageBodyString=new String(messageBodyByteArray);
	        String statusCode = "";
	        String statusDescription="";
            log.info(uuid+" - "+messageBodyString); 
	        
	        //get the UDP in this flow 
	        String EOF=(String)getUserDefinedAttribute("EOF");
	        log.info(uuid + " - "+EOF);
	        
	        if(Utilities.validateFileContent(messageBodyString, EOF))
	        {
	        	log.info(uuid+"-valid file being sent to out");
	        	statusCode=(String)getUserDefinedAttribute("AcceptedFileCode");
	        	statusDescription=(String)getUserDefinedAttribute("AcceptedFileDescription");
	        }
	        else
	        {
	        	log.warn(uuid+"-file is invalid as EOF is not matched");
	        	bFilevalid = false;
	        	statusCode = (String)getUserDefinedAttribute("InvalidFileCode");
	        	statusDescription=(String)getUserDefinedAttribute("InvalidFileDescription");
	        	outMessage.clearMessage();
	        }
	        
	        int numberofRecords=0;// = Utilities.Numberofrecords(messageBodyString,"\r\n");
	        
	        globalEnv.getRootElement().createElementAsLastChild(MbElement.TYPE_NAME, "Filevalid", bFilevalid);
	        String inputpayload = "";
//	        Boolean incompleteFileFlag=true;
	      
	        if(messageBodyString.contains(EOF))
	        {
	        	inputpayload =messageBodyString.replace("\r\n"+EOF,"");
	        	MbElement outRoot=outMessage.getRootElement();
	        	//commiting the file transformation to the payload 
	        	outRoot.getFirstElementByPath("/BLOB/BLOB").setValue(inputpayload.getBytes());
	        	log.info("successfully committed the file transformations to the outgoing payload");
	        	bFilevalid=true;
	        	
	        	 
	        }else
	        {
	        	inputpayload = messageBodyString;
	        	bFilevalid=false;
	        	
	        }
	        numberofRecords=Utilities.numberOfCifRecords(inputpayload, "\r\n");
	        log.info("total number of records present in current file"+numberofRecords);
        	
	        
	        //MbMessage File = inAssembly.getLocalEnvironment();
	        MbElement File = inAssembly.getLocalEnvironment().getRootElement().getFirstElementByPath("./File");
//	        MbElement TimeStamp = File.getFirstElementByPath("./TimeStamp");
	        MbElement Filename = File.getFirstElementByPath("./Name");
//	        globalEnv.getRootElement().createElementAsLastChild(MbElement.TYPE_NAME, "TimeStamp", TimeStamp.getValueAsString());
	        globalEnv.getRootElement().createElementAsLastChild(MbElement.TYPE_NAME, "Filename", Filename.getValueAsString());
	        
	        String fileName=File.getFirstElementByPath("./Name").getValueAsString();
	        log.info(uuid+" - file name that is being picked up "+fileName);
	        Connection connection=null;
			//String JDBCProviders = (String) getUserDefinedAttribute("jdbcProviders");
			try{
				connection = this.getJDBCType4Connection(JDBCProvider, JDBC_TransactionType.MB_TRANSACTION_AUTO);
				log.info(uuid+" - connection to the database is successfully made ");
			} catch (Exception e) {
			// TODO: handle exception
				log.error(uuid+" - DB connection error ");
				log.error(uuid+" - "+e.getMessage());
				throw new MbUserException(this, "evaluate()", "", "","DB connection Error",null);
			}
			
						
			//String insertTableSQL = "INSERT INTO "+tableName+" (CifID,FileName,FileData,NumberOfRecords,Status,Description) "+" VALUES(?,?,?,?,?,?)";
			String insertTableSQL = "INSERT INTO "+tableName+" (CifID,numberofRecords,status,description,filedata,Filename,isCompleteFile) "+" VALUES(?,?,?,?,?,?,?)";
			
			log.info(uuid+" - sql statement that is being used "+insertTableSQL);
			preparedStatement = connection.prepareStatement(insertTableSQL);
			preparedStatement.setString(1,uuid);
			preparedStatement.setInt(2,numberofRecords);
			preparedStatement.setString(3,statusCode);
			preparedStatement.setString(4,statusDescription);
			preparedStatement.setBytes(5,inputpayload.getBytes());
			log.info(uuid+" - final saved version of file - "+inputpayload);
			preparedStatement.setString(6,fileName);
	     	preparedStatement.setBoolean(7, bFilevalid);
			int result=preparedStatement.executeUpdate();
			//write the row immediately to the db by calling a commit 
			connection.commit();
			log.info(uuid+" - effected rows from the prepared statement execution "+result);
	       
			
			
//			MbMessage LocalEnv = outAssembly.getLocalEnvironment();
//			MbElement Destination = LocalEnv.getRootElement().getFirstElementByPath("./Destination");
//			MbElement DestinationMb =null;
//		
			
			// End of user code
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
		log.info("value of bFileValid "+bFilevalid);
		if (bFilevalid)
		{
		 out.propagate(outAssembly);
		 log.info(" successfully moved file to FileOutput node ");
		}
	}
}