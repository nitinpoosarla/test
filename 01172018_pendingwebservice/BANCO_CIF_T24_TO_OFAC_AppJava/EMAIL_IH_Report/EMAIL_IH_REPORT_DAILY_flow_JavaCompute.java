package EMAIL_IH_Report;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.MbBLOB;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbUserException;
import org.apache.log4j.Logger;

public class EMAIL_IH_REPORT_DAILY_flow_JavaCompute extends MbJavaComputeNode {

	public void evaluate(MbMessageAssembly inAssembly) throws MbException {
		  MbOutputTerminal out = getOutputTerminal("out");
		  MbOutputTerminal alt = getOutputTerminal("alternate");
		  
		//  final Logger log=Logger.getLogger(EMAIL_IH_REPORT_DAILY_flow_JavaCompute.class);
		//  log.info("last line-->");
		  
		  //PreparedStatement stmt = null;
		  MbMessage inMessage = inAssembly.getMessage();
		  MbMessageAssembly outAssembly = null;
		  String email = "";
		  try {
		   // create new message as a copy of the input
		   MbMessage outMessage = new MbMessage(inMessage);
		   outAssembly = new MbMessageAssembly(inAssembly, outMessage);
		  
		     String jdbcProviders = (String) getUserDefinedAttribute("jdbcProviders");
		     String Tablename = (String) getUserDefinedAttribute("Tablename");
		      try {
				   Connection con = this.getJDBCType4Connection(jdbcProviders, JDBC_TransactionType.MB_TRANSACTION_AUTO);
				   Statement stmt = con.createStatement();
				   String gg ="hestt";
				   ResultSet  rs = stmt.executeQuery("select * from " + Tablename +" where DATATIME >= current timestamp - 24 HOUR - MINUTE(current timestamp)MINUTES - SECOND(current timestamp)SECONDS - MICROSECOND(current timestamp)MICROSECONDS");
				  while (rs.next())
				  {        
					  String UUID = rs.getString("UUID");
					  int records =   rs.getInt("NumberOfRecords");
					  String  Status = rs.getString("Status");
					  String Description = rs.getString("Description");
					  Blob b   = rs.getBlob("FileData");
					  byte[] bdata = b.getBytes(1, (int) b.length());
					  String s = new String(bdata);
					 /* String Exception= "<html><body><p style=font-size:12px;>***Integration Bus Generated E-mail***</p>"
					    		+	"<table width= '750' border='2' cellspacing='4' cellpadding='8'>"
					    		+	"<th colspan='2' bgcolor='#D8D8D8' style='align:CENTER;font-family:arial;font-size:16px;'>INTEGRATION BUS NOTIFICATION</th>"
					    		+	"<tr><td style='width=30%;align:left;font-family:timesnewroman;font-size:14px;'><i>FlowName</i></td>"
					    		+	"<td width='70%'>" + UUID + "</td></tr>"
					    		+	"<tr bgcolor='#F8F8F8'><td><i>FIlestatus</i></td><td>" + Status + "</td></tr>"
					   
					    		
					    		
					    		+   "<tr><td><b>Integration Node</b></td><td>" + getBroker().getName().toString() + "</td></tr>"
					    		
					    		+	"</table>"
					    		+ "<br/>"
					    		+	"</body></html>";*/
					  
					  email   = email + UUID + "|" + records + "|" + Status + "|" + Description + "|" + s  + "\r\n";
				       }
				    } catch (SQLException e) {
				    	    // TODO Auto-generated catch block
				    	    e.printStackTrace();
				    	   } 
				      
		        MbElement outRoot = outMessage.getRootElement();
			    MbElement outParser=outRoot.createElementAsLastChild(MbBLOB.PARSER_NAME);
	            outParser.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"BLOB",email.getBytes());
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
		// The following should only be changed
		// if not propagating message to the 'out' terminal
		out.propagate(outAssembly);
	}

}
