/*
 * #%L
 * LaTeX Track Changes (LTC) allows collaborators on a version-controlled LaTeX writing project to view and query changes in the .tex documents.
 * %%
 * Copyright (C) 2009 - 2012 SRI International
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package com.sri.ltc.server;

import com.apple.eawt.AboutHandler;
import com.apple.eawt.AppEvent;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;
import com.sri.ltc.CommonUtils;
import com.sri.ltc.logging.LevelOptionHandler;
import com.sri.ltc.logging.LogConfiguration;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import javax.servlet.ServletException;
import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Main class to run base system of Latex Track Changes as a server.
 *
 * @author linda
 */
public final class LTC {

    /**
     * --- begin of singleton pattern --------------------------------------
     * Nested class to implement thread-safe singleton with deferred
     * instantiation.  We want to defer creation of instance until the call to
     * getInstance().
     * <p>
     * Using patterns in
     * http://c2.com/cgi/wiki?JavaSingleton
     * http://www.javaworld.com/javaworld/jw-05-2003/jw-0530-letters.html
     */
    private static final class LTCHolder {
        static final LTC INSTANCE = new LTC();
    }

    /**
     * Obtains singleton instance of this class.
     * @return singleton instance of this class
     */
    public static synchronized LTC getInstance() {
        return LTCHolder.INSTANCE;
    }

    // private constructor to prevent multiple instantiations
    private LTC() {
        init();
    }

    // --- end of singleton pattern ---------------------------------------- //

    static {
        // first thing is to configure Mac OS X before AWT gets loaded:
        final String NAME = "LTC Server";
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", NAME);
        System.setProperty("apple.awt.showGrowBox", "true");

        // print NOTICE on command line
        System.out.println(CommonUtils.getNotice()); // output notice

        // default configuration for logging
        try {
            LogManager.getLogManager().readConfiguration(new LogConfiguration().asInputStream());
            Logger.getLogger(LTC.class.getName()).fine("Default logging configuration complete");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static final Logger logger = Logger.getLogger(LTC.class.getName());

    private void init() {
        logger.config("LTC version: " + CommonUtils.getVersion() + " (" + CommonUtils.getBuildInfo() + ")");

        try {
            // set up RPC server - this will enable us to receive XML-RPC calls
            Server rpcserver = new Server(
                    LTCserverInterface.class,
                    LTCserverImpl.class,
                    LTCOptions.port);
            rpcserver.start();
            logger.info("Started RPC server on port "+ LTCOptions.port+".");
        } catch (ServletException e) {
            logger.log(Level.SEVERE, "Cannot start RPC server", e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Cannot start RPC server", e);
        }
    }

    private static void printUsage(PrintStream out, CmdLineParser parser) {
        out.println("usage: java -cp ... com.sri.ltc.server.LTC [options...]");
        out.println("   or: java -jar LTC-<...>.jar [options...]");
        out.println("with");
        parser.printUsage(out);
    }

    public static void main(String[] args) {
        // parse arguments
        CmdLineParser.registerHandler(Level.class, LevelOptionHandler.class);
        LTCOptions options = new LTCOptions();
        CmdLineParser parser = new CmdLineParser(options);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            printUsage(System.err, parser);
            return;
        }

        if (options.displayHelp) {
            printUsage(System.out, parser);
            return;
        }

        if (options.displayLicense) {
            System.out.println("LTC is licensed under:\n\n" + CommonUtils.getLicense());
            return;
        }

        // configure logging
        try {
            LogConfiguration logConfig = new LogConfiguration();
            logConfig.setProperty("java.util.logging.ConsoleHandler.level",options.consoleLogLevel.getName());
            logConfig.setProperty("java.util.logging.FileHandler.level",options.consoleLogLevel.getName());
            LogManager.getLogManager().readConfiguration(logConfig.asInputStream());
            logger.config("Logging configured to level " + options.consoleLogLevel.getName());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Cannot configure logging", e);
        }

        // customize for Mac OS X:
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "setting UI look & feel", e);
        }
        String osName = System.getProperty("os.name").toLowerCase();
        boolean IS_MAC = osName.startsWith("mac os x");
        if (IS_MAC) {
            URL imageURL = Console.class.getResource("/images/LTC-icon.png");
            if (imageURL != null) {
                ImageIcon icon = new ImageIcon(imageURL);
                com.apple.eawt.Application application = com.apple.eawt.Application.getApplication();
                application.setDockIconImage(icon.getImage());
                application.setAboutHandler(new AboutHandler() {
                    @Override
                    public void handleAbout(AppEvent.AboutEvent aboutEvent) {
                        // display copyright/license information
                        JOptionPane.showMessageDialog(null,
                                CommonUtils.getNotice(),
                                "About LaTeX Track Changes (LTC)",
                                JOptionPane.PLAIN_MESSAGE);
                    }
                });
                application.setQuitHandler(new QuitHandler() {
                    @Override
                    public void handleQuitRequestWith(AppEvent.QuitEvent quitEvent, QuitResponse quitResponse) {
                        // TODO: do anything here?
                        quitResponse.performQuit();
                    }
                });
                // TODO: enable preferences and set handler?
            }
        }

        LTC.getInstance(); // start up server (if not already running)
    }

    static class LTCOptions {
        @Option(name="-l",usage="set console log level\nSEVERE, WARNING, INFO, CONFIG (default), FINE, FINER, FINEST")
        Level consoleLogLevel = Level.CONFIG;

        @Option(name="-h",usage="display usage and exit")
        boolean displayHelp = false;

        @Option(name="-c",usage="display copyright/license information and exit")
        boolean displayLicense = false;

        @Option(name="-p",usage="port on localhost used for XML-RPC")
        static int port = LTCserverInterface.PORT;
    }
}
