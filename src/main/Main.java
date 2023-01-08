package main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.SwingUtilities;

import gui.FontDialog;
import gui.MainFrame;

public class Main {

	public static final String VERSION = "TextViewer v1.0";
	
	/**
	 * <pre>
	 *  Default constants explanations
	 *  
	 *  <code>bufSize</code> : 16KB
	 *  <code>charPerPage</code> : 1 page of A4 sheet can contain roughly 3000 chars(10pt) at most. 2/3 of that will do
	 *  <code>pageEndsWithNewline</code> : true is usually expected by normal users
	 *  <code>singlePageFileSizeLimit</code> : <code>Windows notepad</code> seems it can't handle files larger than 45 or 55KB. that might be a good limit for paged file
	 *  <code>contentQueueLength</code> : 1 makes at most 3 pages read in memory
	 *  (one is displaying, one is on the queue, one is read by SelectedFileHandler and ready to be put)
	 *  triple buffering(48KB) won't be considered a huge RAM , and will avoid lag
	 *  </pre> 
	 *  */
	public static SettingData setting = new SettingData(1024 * 16, 1800, true, 55 * 1024, 3);
	
	public static LoggerThread logger = null;
	
	public static void main(String[] args) { 
		
		//TODO : known bugs/problems below
		/**
		 * very small buffer/pagelimit size
		 * */
	 
		boolean verbose = false;
		
		for(int i = 0; i < args.length; i++) {
			if(args[i].equals("--ShowAllFont")) {
				FontDialog.showAll = true;
			} else if(args[i].equals("--Verbose")) {
				verbose = true;
			} else if(args[i].equals("--Logconsole")) {
				logger = new LoggerThread();
			} else {
				System.out.println("Usage : java -jar TextViewer.jar [options]");
				System.out.println("Options : ");
				System.out.println("\t--ShowAllFont\tShow whole font in font list in Change font Dialog");
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
	

	public static String formatFileSize(long length) {
		
		if(length == 0L) return "0.00byte";
		
		switch ((int)(Math.log(length) / Math.log(1024))) {
		
		case 0:
			return String.format("%d", length) + "byte";
		case 1:
			return String.format("%.2f", length / 1024.0) + "KB";
		case 2:
			return String.format("%.2f", length / (1024.0 * 1024)) + "MB";
		case 3:
			return String.format("%.2f", length / (1024.0 * 1024 * 1024)) + "GB";
		}
		return String.format("%.2f", length / (1024.0 * 1024 * 1024 * 1024)) + "TB";
		
	}
	
	
	public static int getByteSize(String lengthText) {
		
		if(lengthText.matches("[0-9]+(\\.[0-9]+)?")) return 0;
		
		double result;
		
		if(lengthText.endsWith("byte"))
			result = (Double.parseDouble(lengthText.replace("byte", "")));
		else if(lengthText.endsWith("KB"))
			result = (Double.parseDouble(lengthText.replace("KB", "")) * 1024);
		else if(lengthText.endsWith("MB"))
			result = (Double.parseDouble(lengthText.replace("MB", "")) * 1024 * 1024);
		else if(lengthText.endsWith("GB"))
			result = (Double.parseDouble(lengthText.replace("GB", "")) * 1024 * 1024 * 1024);
		else if(lengthText.endsWith("B"))
			result = (Double.parseDouble(lengthText.replace("B", "")));
		else
			throw new NumberFormatException("\"" + lengthText + "\" is invalid!");
		
		return (int)result;
		
	}

	public static void kill(int errCode) {
		
		logger.log("Kill application with error code : " + errCode);
		logger.kill(3000);
		System.exit(errCode);
		
	}
}