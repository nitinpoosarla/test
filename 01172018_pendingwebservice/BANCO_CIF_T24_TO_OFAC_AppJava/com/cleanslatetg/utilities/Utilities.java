package com.cleanslatetg.utilities;

import java.io.BufferedReader;
import java.net.URLClassLoader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;

import javax.sql.rowset.serial.SerialBlob;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

public class Utilities {
	
	private static final Logger log=Logger.getLogger(Utilities.class);
	
	public static void main(String[] args)
	{
//		URL[] urls=getClassPaths();
		
	}
	
	public static boolean validateFileContent(String fileContent,String EOF)
	{
		String[] lines=fileContent.split("\r\n");
		String lastLine=lines[lines.length-1];
		log.info("last line ---> "+lastLine+"that needs to be compared with EOF -->"+EOF);
		
		if(lastLine.equalsIgnoreCase(EOF))
			return true;
		else
			return false;
	 }
	
	public static int numberOfCifRecords(String fileContent,String delimiter)
	{
		String[] lines=fileContent.split(delimiter);
		// first line is header which needs to be removed from the count
		int cifRecordCount=lines.length-1;
		log.info("total number of records in cif file are "+cifRecordCount);
		return cifRecordCount;
	}
	
	
	
	public static void printClassPaths()
	{
		ClassLoader cl = ClassLoader.getSystemClassLoader();
        URL[] urls = ((URLClassLoader)cl).getURLs();
        
        for (URL url : urls)
			log.info(url.getFile());

	}
	//generate UUID
	public static String generateUUID()
	{
		UUID uuid = UUID.randomUUID();
        String randomUUIDString = uuid.toString();
        
		return randomUUIDString;
	}
	
	// blob to string 
	public static String blobToString(Blob blob)
	{
		StringBuffer strOut = new StringBuffer();
		String aux;
		BufferedReader br=null;
		try {
			br = new BufferedReader(new InputStreamReader(blob.getBinaryStream()));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			while ((aux=br.readLine())!=null) {
			strOut.append(aux);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return strOut.toString();
	}
	
	public static boolean blobEndsWithEOF(Blob blob,String EOF)
	{
		String blobString=blobToString(blob);
		return blobString.endsWith(EOF);
	}
	
	public static Properties loadProperties(String fileName)
	{
		Properties prop=new Properties();
		log.info("current working directory -"+System.getProperty("user.dir"));
		InputStream input=null;
		try {
			input=new FileInputStream(fileName);
			prop.load(input);
			log.info("sucessfully loaded properties from "+fileName);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			log.error("fileName provided "+fileName+" is not available");
			e.printStackTrace();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error("IO Exception happened while loading the file "+fileName);
			e.printStackTrace();
		}
		
		return prop;
		
	}
	
	public static boolean blobContainsEOF(Blob blob,String EOF)
	{
		String blobString=blobToString(blob);
		return blobString.contains(EOF);
	}
	public static Blob blobFromString(String blobString)
	{
		Blob blob = null;
		try {
			blob = new SerialBlob(blobString.getBytes());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return blob;
	}

}
