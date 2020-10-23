package com.francelabs.datafari.servlets.admin;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.FileUtils;
import org.json.simple.JSONObject;

import com.francelabs.datafari.exception.CodesReturned;
import com.francelabs.datafari.servlets.constants.OutputConstants;
import com.francelabs.datafari.utils.*;

@WebServlet("/admin/ReinitCertificate")
public class ReinitCertificate extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private String env;
  private final static Logger LOGGER = LogManager.getLogger(ReinitCertificate.class.getName());




  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    final JSONObject jsonResponse = new JSONObject();
    request.setCharacterEncoding("utf8");
    response.setContentType("application/json");
    String environnement = System.getenv("DATAFARI_HOME");
    if (environnement == null) { // If in development environment
      environnement = ExecutionEnvironment.getDevExecutionEnvironment();
    }
    env = environnement + "/ssl-keystore/customerCertificates";

    try {


      final String scriptname = "reinitApacheCertificates.sh";
      String scriptEnvironment = env + "/../apache/config";

      final String[] command = { "/bin/bash", "-c", "cd " + scriptEnvironment + " && bash " + scriptname };
      final ProcessBuilder p = new ProcessBuilder(command);
      final Process p2 = p.start();

      final BufferedReader stdInput = new BufferedReader(new InputStreamReader(p2.getInputStream()));

      final BufferedReader stdError = new BufferedReader(new InputStreamReader(p2.getErrorStream()));

      // read the output from the command
      String s = null;
      String errorCode = null;
      while ((s = stdInput.readLine()) != null) {
        LOGGER.info(s);
      }

      // read any errors from the attempted command
      // TODO too verbose, display messages that are not errors
      while ((s = stdError.readLine()) != null) {
        LOGGER.warn(s);
        errorCode = s;
      }

      
      final String elkscriptname = "reinitElkCertificates.sh";
      String elkscriptEnvironment = env + "/../elk/config";

      final String[] elkcommand = { "/bin/bash", "-c", "cd " + elkscriptEnvironment + " && bash " + elkscriptname };
      final ProcessBuilder elkp = new ProcessBuilder(elkcommand);
      final Process elkp2 = elkp.start();

      final BufferedReader elkstdInput = new BufferedReader(new InputStreamReader(elkp2.getInputStream()));

      final BufferedReader elkstdError = new BufferedReader(new InputStreamReader(elkp2.getErrorStream()));

      // read the output from the command
      String elks = null;
      String elkerrorCode = null;
      while ((elks = elkstdInput.readLine()) != null) {
        LOGGER.info(elks);
      }

      // read any errors from the attempted command
      // TODO too verbose, display messages that are not errors
      while ((elks = elkstdError.readLine()) != null) {
        LOGGER.warn(elks);
        errorCode = elks;
      }




    } catch (Exception e) {
      LOGGER.error("general exception");
      LOGGER.error(e);
      jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
    }
    
    try {


        final String scriptname = "reinitApacheCertificates.sh";
        String scriptEnvironment = env + "/../apache/config";

        final String[] command = { "/bin/bash", "-c", "cd " + scriptEnvironment + " && bash " + scriptname };
        final ProcessBuilder p = new ProcessBuilder(command);
        final Process p2 = p.start();

        final BufferedReader stdInput = new BufferedReader(new InputStreamReader(p2.getInputStream()));

        final BufferedReader stdError = new BufferedReader(new InputStreamReader(p2.getErrorStream()));

        // read the output from the command
        String s = null;
        String errorCode = null;
        while ((s = stdInput.readLine()) != null) {
          LOGGER.info(s);
        }

        // read any errors from the attempted command
        // TODO too verbose, display messages that are not errors
        while ((s = stdError.readLine()) != null) {
          LOGGER.warn(s);
          errorCode = s;
        }




      } catch (Exception e) {
        LOGGER.error("general exception");
        LOGGER.error(e);
        jsonResponse.put(OutputConstants.CODE, CodesReturned.GENERALERROR.getValue());
      }
    
    
    jsonResponse.put(OutputConstants.CODE, CodesReturned.ALLOK.getValue());
    
    LOGGER.info(jsonResponse.toJSONString());
    final PrintWriter out = response.getWriter();
    out.print(jsonResponse);

  } 


}
