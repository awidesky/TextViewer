package main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.Provider.Service;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.Adler32;
import java.util.zip.CRC32;

import javax.swing.SwingUtilities;

import gui.FontDialog;
import gui.MainFrame;
import gui.SwingDialogs;

public class Main {

	public static final String VERSION = "TextViewer v1.0"; //TODO : change version number + add license comment to top
	
	/**
	 * <pre>
	 *  Default constants explanations
	 *  
	 *  <code>bufSize</code> : same as <code>charPerPage</code>
	 *  <code>charPerPage</code> : 1 page of A4 sheet can contain roughly 3000 chars(10pt) at most. 2/3 of that will do
	 *  <code>pageEndsWithNewline</code> : true is usually expected by normal users
	 *  <code>singlePageFileSizeLimit</code> : 1GB would be a good limit for "big" file
	 *  <code>loadedPagesNumber</code	> : 1 makes at most 1 pages read in memory(include one that <code>TextViewer</code> is holding for display
	 *  triple buffering(a few KB) won't be considered a huge RAM , and will avoid lag
	 *  </pre> 
	 *  */
	public static SettingData setting = new SettingData(1800, 1800, true, 1024 * 1024 * 1024, 3, LineSeparator.getDefault());
	
	private static MainFrame mf;
	public static LoggerThread logger = null;
	
	private static HashGenerator hasher = HashGenerator.getChecksumHashInstance(new CRC32());
	
	public static void main(String[] args) { 

		boolean verbose = false;
		
		for(int i = 0; i < args.length; i++) {
			if(args[i].equals("--help")) {
				printConsoleHelp();
				return;
			} else if(args[i].equals("--showAllFont")) {
				FontDialog.showAll = true;
			} else if(args[i].equals("--verbose")) {
				verbose = true;
			} else if(args[i].equals("--logConsole")) {
				logger = new LoggerThread();
			} else if (args[i].startsWith("--pageHash=")) {
				String hashAlgo = args[i].split("=")[1];
				if ("Adler32".equalsIgnoreCase(hashAlgo)) {
					hasher = HashGenerator.getChecksumHashInstance(new Adler32());
				} else if ("CRC32".equalsIgnoreCase(hashAlgo)) {
					hasher = HashGenerator.getChecksumHashInstance(new CRC32());
				} else if ("RAW".equalsIgnoreCase(hashAlgo)) {
					hasher = HashGenerator.getRawCompareInstance();
				} else {
					try {
						hasher = HashGenerator.getCryptoHashInstance(MessageDigest.getInstance(hashAlgo));
					} catch (NoSuchAlgorithmException e) {
						SwingDialogs.error("Error while initiating!", "Hash algorithm : " + hashAlgo + " is not available!\n%e%", e, true);
						kill(-1);
					}
				}
			} else {
				System.err.println("Invalid Argument : " + args[i] + "\n");
				printConsoleHelp();
				return;
			}
		}
		
		try {
			if (logger == null) {
				File logFolder = new File("." + File.separator + "logs");
				File logFile = new File(logFolder.getAbsolutePath() + File.separator + "log-"
						+ new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss").format(new Date()) + ".txt");
				logFolder.mkdirs();
				logFile.createNewFile();

				logger = new LoggerThread(new FileOutputStream(logFile));
			}
		} catch (IOException e) {

			logger = new LoggerThread();
			//GUI.error("Error when creating log flie", "%e%", e, false);
			
		} finally {
			logger.setVerbose(verbose);
			logger.start();
		}
		
		logger.log("Setup done!");
		
		/** Set Default Uncaught Exception Handlers */
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			try {
				SwingDialogs.error("Unhandled exception in thread " + t.getName() + " : " + ((Exception)e).getClass().getName(), "%e%", (Exception)e , true);
				mf.closeFile();
			} catch(Exception err) {
				err.printStackTrace();
			}
		});
		SwingUtilities.invokeLater(() -> {
			Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
				try {
					SwingDialogs.error("Unhandled exception in EDT : " + ((Exception) e).getClass().getName(), "%e%", (Exception) e, true);
					mf.closeFile();
				} catch (Exception err) {
					err.printStackTrace();
				}
			});
		});
		
		SwingUtilities.invokeLater(() ->{
			logger.log("Starting Mainframe...");
			mf = new MainFrame();
			logger.log("Mainframe loaded!");
		});
    }
	

	private static void printConsoleHelp() {

		System.out.println("Usage : java -jar TextViewer.jar [options]");
		System.out.println("Options : ");
		System.out.println("\t--help\tShow this help message\n");
		System.out.println("\t--showAllFont\tShow all fonts(even if it can't display texts in the editor) in font list in \"Change font\" Dialog\n");
		System.out.println("\t--verbose\tLog verbose/debugging information\n");
		System.out.println("\t--logConsole\tLog at system console, not in a file\n");
		System.out.print("\t--pageHash=<HashAlgorithm>\tUse designated hash algorithm when checking if a page is edited.\n"
								+ "\t\t\t\t\tIn default, CRC32 checksum will be used. you can use various hash algorithm like SHA,\n"
								+ "\t\t\t\t\tor you may use whole text value as a \"hash\" by --pageHash=RAW\n"
								+ "\t\t\t\t\tSince hash values of all read pages are stored in memory, this will cause every page that have read in memory,\n"
								+ "\t\t\t\t\teventually put whole file in memory.\n" 
								+ "\t\t\t\t\tAvailable <HashAlgorithm> options are below :\n\n"
								+ "\t\t\t\t\t\tRAW\n"
								+ "\t\t\t\t\t\tAdler32\n"
								+ "\t\t\t\t\t\tCRC32\n"
								+ "\t\t\t\t\t\t");
	    System.out.println(Arrays.stream(Security.getProviders())
	    							.map(Provider::getServices)
	    							.flatMap(Set::stream)
	    							.filter(s -> "MessageDigest".equalsIgnoreCase(s.getType()))
	    							.map(Service::getAlgorithm)
	    							.collect(Collectors.joining("\n\t\t\t\t\t\t")));
		
	}


	public static String formatFileSize(long fileSize) {
		
		if(fileSize == 0L) return "0.00byte";
		
		switch ((int)(Math.log(fileSize) / Math.log(1024))) {
		
		case 0:
			return String.format("%d", fileSize) + "byte";
		case 1:
			return String.format("%.2f", fileSize / 1024.0) + "KB";
		case 2:
			return String.format("%.2f", fileSize / (1024.0 * 1024)) + "MB";
		case 3:
			return String.format("%.2f", fileSize / (1024.0 * 1024 * 1024)) + "GB";
		}
		return String.format("%.2f", fileSize / (1024.0 * 1024 * 1024 * 1024)) + "TB";
		
	}
	
	/**
	 * Result will be integer number only
	 * */
	public static String formatExactFileSize(long fileSize) {
		
		if(fileSize == 0L) return "0byte";
		
		
		String arr[] = {"B", "KB", "MB", "GB"};
		
		for(String prefix : arr) {
			if(fileSize % 1024 != 0) {
				return fileSize + prefix;
			} else { fileSize /= 1024; }
		}
		
		return fileSize + "TB";
		
	}
	
	public static long getExactByteSize(String lengthText) {
		
		if(lengthText.matches("[0-9]+")) return 0;
		
		long result;
		
		if(lengthText.endsWith("KB"))
			result = (Long.parseLong(lengthText.replace("KB", "")) * 1024);
		else if(lengthText.endsWith("MB"))
			result = (Long.parseLong(lengthText.replace("MB", "")) * 1024 * 1024);
		else if(lengthText.endsWith("GB"))
			result = (Long.parseLong(lengthText.replace("GB", "")) * 1024 * 1024 * 1024);
		else if(lengthText.endsWith("TB"))
			result = (Long.parseLong(lengthText.replace("TB", "")) * 1024 * 1024 * 1024 * 1024);
		else if(lengthText.endsWith("B"))
			result = (Long.parseLong(lengthText.replace("B", "")));
		else
			throw new NumberFormatException("\"" + lengthText + "\" is invalid!");
		
		return result;
		
	}
	
	/**
	 * Get a hash of <code>String</code> object.
	 * in default, this method will use <code>CRC32</code>
	 * if <code>pageHash</code> option is set to <code>original</code>, whole page Text will be used as the "hash".
	 * This may cause high memory usage, but if you're overly paranoid about hash collision, this would be a way.
	 * */
	public static String getHash(String src) {
		return hasher.getHash(src);
	}
	

	public static MainFrame getMainFrame() {
		return mf;
	}
	
	public static void kill(int errCode) {
		
		logger.log("Kill application with error code : " + errCode);
		logger.kill(3000);
		System.exit(errCode);
		
	}
}