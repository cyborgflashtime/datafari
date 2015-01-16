<%@ page import="java.io.*,java.util.*, javax.servlet.*" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="org.apache.commons.fileupload.*" %>
<%@ page import="org.apache.commons.fileupload.disk.*" %>
<%@ page import="org.apache.commons.fileupload.servlet.*" %>
<%@ page import="org.apache.commons.io.output.*" %>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>

<%    
ServletContext context = pageContext.getServletContext();
String filepath = context.getInitParameter("file-upload");
String filename = "capsules.txt";
response.setContentType("application/octet-stream");
   
String disHeader = "Attachment; Filename=\""+filename+"\"";
response.setHeader("Content-Disposition", disHeader);
File fileToDownload = new File(filepath+filename);

InputStream in = null;
ServletOutputStream outs = response.getOutputStream();

try {
in = new BufferedInputStream
(new FileInputStream(fileToDownload));
int ch;
char caractere;
while ((ch = in.read()) != -1) {
	caractere = (char) ch;
	if (caractere == '\\')
		continue;
		
outs.print(caractere);
}
}
finally {
if (in != null) in.close(); 
}

outs.flush();
outs.close();
in.close();


%>   