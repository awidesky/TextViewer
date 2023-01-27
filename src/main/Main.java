package main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.Adler32;
import java.util.zip.CRC32;

import javax.swing.SwingUtilities;

import gui.FontDialog;
import gui.MainFrame;
import gui.SwingDialogs;

public class Main {

	public static final String VERSION = "TextViewer v1.0";
	
	/**
	 * <pre>
	 *  Default constants explanations
	 *  
	 *  <code>bufSize</code> : same as <code>charPerPage</code>
	 *  <code>charPerPage</code> : 1 page of A4 sheet can contain roughly 3000 chars(10pt) at most. 2/3 of that will do
	 *  <code>pageEndsWithNewline</code> : true is usually expected by normal users
	 *  <code>singlePageFileSizeLimit</code> : 1GB would be a good limit for "big" file
	 *  <code>loadedPagesNumber</code> : 1 makes at most 1 pages read in memory(include one that <code>TextViewer</code> is holding for display
	 *  triple buffering(a few KB) won't be considered a huge RAM , and will avoid lag
	 *  </pre> 
	 *  */
	public static SettingData setting = new SettingData(1800, 1800, true, 1024 * 1024 * 1024, 3);
	
	public static LoggerThread logger = null;
	
	private static HashGenerator hasher = HashGenerator.getChecksumHashInstance(new CRC32());
	
	public static void main(String[] args) { 

		boolean verbose = false;
		
		for(int i = 0; i < args.length; i++) {
			if(args[i].equals("--showAllFont")) {
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
				System.out.println("Usage : java -jar TextViewer.jar [options]");
				System.out.println("Options : ");
				System.out.println("\t--showAllFont\tShow whole font in font list in Change font Dialog"); //TODO : help for all options
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
		
		SwingUtilities.invokeLater(() ->{
			logger.log("Starting Mainframe...");
			new MainFrame();
			logger.log("Mainframe loaded!");
		});
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
	

	public static void kill(int errCode) {
		
		logger.log("Kill application with error code : " + errCode);
		logger.kill(3000);
		System.exit(errCode);
		
	}
}