/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.sun.com/cddl/cddl.html or
 * install_dir/legal/LICENSE
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at install_dir/legal/LICENSE.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id$
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.tools;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.Context;
import com.sun.faban.harness.tools.Configure;
import com.sun.faban.harness.tools.Postprocess;
import com.sun.faban.harness.tools.Start;
import com.sun.faban.harness.tools.Stop;

import com.sun.faban.harness.tools.ToolContext;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mysqlstats implements a tool used for gathering the statistics from a
 * MySQL instance.
 */
public class Mysqlstats {

    private static Logger logger =
            Logger.getLogger(Mysqlstats.class.getName());

    /** The injected tool context. */
    @Context public ToolContext ctx;

    Command cmd;
    CommandHandle processRef;
    ArrayList<String> toolCmd;
    String toolName;
    String logfile, logfile1, logfile2;
    /**
     * Configures the MySQLStats.
     */
    @Configure public void config() {
        toolName = ctx.getToolName();

        String mysqlHome = ctx.getServiceProperty("serverHome");
        String mysqlUser = ctx.getServiceProperty("user");
        String mysqlPass = ctx.getServiceProperty("password");

        if (mysqlHome.endsWith(File.separator))
            mysqlHome = mysqlHome.substring(0, mysqlHome.length() -
                                              File.separator.length());

        toolCmd = new ArrayList<String>();
        String mysqlCmd = "mysql";
        if (mysqlHome != null)
            mysqlCmd = mysqlHome + File.separator + "bin" + File.separator + mysqlCmd;
        toolCmd.add(mysqlCmd);
		if (mysqlUser != null)
            toolCmd.add("-u" + mysqlUser);
        if (mysqlPass != null)
            toolCmd.add("-p" + mysqlPass);
        toolCmd.add("-B");
        toolCmd.add("-e");
        toolCmd.add("show global status;");
        logger.info("Setting up mysql command: " + toolCmd);
        cmd = new Command(toolCmd);
        logfile = ctx.getOutputFile();
        // We need two intermediate files for the begin/end snapshots
		logfile1 = logfile + "_1";
		logfile2 = logfile + "_2";
        logger.info(toolName + " Configured with toolCmd " + toolCmd);

    }

    /**
     * Starts the MySQLStats.
     * @throws IOException Cannot execute the needed command
     * @throws InterruptedException Interrupted waiting for the stats commmand
     */
    @Start public void start() throws IOException, InterruptedException {
        logger.info("Calling mysql show status at start");
        cmd.setOutputFile(Command.STDOUT, logfile1);
        cmd.setSynchronous(false);

        processRef = ctx.exec(cmd);
        logger.info(toolName + " Started with Cmd = " + toolCmd + " in start method");
    }

    /**
     * Stops the MySQLStats.
     */
    @Stop public void stop() {
        try {
            if(ctx.getToolStatus() == 1){
                logger.info("Calling mysql show status at stop");
                cmd.setOutputFile(Command.STDOUT, logfile2);
                cmd.setSynchronous(false);
                processRef = ctx.exec(cmd);
                logger.info(toolName + " Started with Cmd = " + toolCmd + " in stop method");
            }
            logger.info("Stopping tool " + this.toolCmd);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Get final report by diffing the two logfiles.
     */
    @Postprocess public void getReport() {
		String c = "mysql_diff_status.sh " + logfile1 + " " + logfile2 + " " + logfile;
        logger.info("Calling " + c);
	    Command diffCommand = new Command(c);
	    try {
	        ctx.exec(diffCommand);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error executing mysql_diff_status.sh", e);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Interrupted executing mysql_diff_status.sh", e);
        }
    }
}