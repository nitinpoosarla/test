package com.WEBSERVICE_INTERFACEMON_IH;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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

public class WEBSERVICE_INTERFACEMON_IH_SYNC_JCN extends MbJavaComputeNode {

	public void evaluate(MbMessageAssembly inAssembly) throws MbException {
		MbOutputTerminal out = getOutputTerminal("out");
		//MbOutputTerminal alt = getOutputTerminal("alternate");

		MbMessage inMessage = inAssembly.getMessage();
		MbMessageAssembly outAssembly = null;
		PreparedStatement preparedStatement=null;
		Logger log=Logger.getLogger(WEBSERVICE_INTERFACEMON_IH_SYNC_JCN.class);
		String logConfigPath=(String)getUserDefinedAttribute("LogFilePath");
		log.info("LogFilePath------>"+logConfigPath);
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
			log.info("started processing the Replay  message flow");
			MbMessage outMessage = new MbMessage(inMessage);
			outAssembly = new MbMessageAssembly(inAssembly, outMessage);
			
			MbElement xmlnsc= outAssembly.getMessage().getRootElement().getLastChild();
			String UUID= xmlnsc.getLastChild().getFirstElementByPath("./UUID").getValueAsString();
			log.info("UUID--->"+UUID);
			String Filename= xmlnsc.getLastChild().getFirstElementByPath("./FILENAME").getValueAsString();
			log.info("Filename----->"+"Filename");
			outAssembly.getGlobalEnvironment().getRootElement().createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"UUID",UUID);
			outAssembly.getGlobalEnvironment().getRootElement().createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"Filename",Filename);
			//xmlnsc deletion
			xmlnsc.detach();
			String JDBCProvider = (String) getUserDefinedAttribute("JDBCProvider"); 
			String tablename = (String) getUserDefinedAttribute("Tablename");
			Connection con=null;
			try{
				con = this.getJDBCType4Connection(JDBCProvider, JDBC_TransactionType.MB_TRANSACTION_AUTO);
				log.info(UUID+" - connection to the database is successfully made ");
			} catch (Exception e) {
			// TODO: handle exception
				log.error(UUID+" - DB connection error ");
				log.error(UUID+" - "+e.getMessage());
				throw new MbUserException(this, "evaluate()", "", "","DB connection Error",null);
			}
			//Connection con = this.getJDBCType4Connection(JDBCProvider, JDBC_TransactionType.MB_TRANSACTION_AUTO);
			// Statement stmt = con.createStatement();
			// ResultSet rs = stmt.executeQuery("select * from mssusr1.CIF_NNN_n where ");// where date < 24hrs");
			String selectQuery="select FileData from "+tablename+" where UUID=CONVERT(uniqueidentifier,?) and FileName=? and IsCompleteFile=CONVERT(bit,'true')";
			log.info(UUID+" - sql statement that is being used "+selectQuery);
			preparedStatement=con.prepareStatement(selectQuery); 		
			preparedStatement .setString(1,UUID);
			preparedStatement.setString(2, Filename);
			ResultSet rs = preparedStatement.executeQuery(); 
			log.info(UUID+" - effected rows from the prepared statement execution "+rs);
			
			while(rs.next())
			{
			Blob b = rs.getBlob("FileData");
			if(b==null)
			{
				// send file not found in the response for the webservice call
				// through alt message path
			}
			byte[] bdata = b.getBytes(1, (int) b.length());
			String s = new String(bdata);
			log.info("File Data----->"+s);
			MbElement outRoot = outMessage.getRootElement();
			MbElement outParser=outRoot.createElementAsLastChild(MbBLOB.PARSER_NAME);
			outParser.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"BLOB",s.getBytes());
		
			out.propagate(outAssembly);
			}
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
		//out.propagate(outAssembly);

	}

}
