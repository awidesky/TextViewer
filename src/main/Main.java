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
	
	public static int bufferSize = 1024 * 8;

	//TODO : paged 읽다가 중간에 다른 파일 읽으면 queue 버려주기
	
	
	public static LoggerThread logger;
	
	public static void main(String[] args) {
	 
		for(int i = 0; i < args.length; i++) {
			if(args[i].equals("--ShowAllFont")) {
				FontDialog.showAll = true;
			} else {
				System.out.println("Usage : java -jar TextViewer.jar [options]");
				System.out.println("Options : ");
				System.out.println("\t--ShowAllFont\tShow whole font in font list in Change font Dialog");
				return;
			}
		}
		
		try {
			
			File logFolder = new File("." + File.separator + "logs");
			File logFile = new File(logFolder.getAbsolutePath() + File.separator + "log-" + new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss").format(new Date()) + ".txt");
			logFolder.mkdirs();
			logFile.createNewFile();
			
			logger = new LoggerThread(new FileOutputStream(logFile));
			
		} catch (IOException e) {

			logger = new LoggerThread();
			//GUI.error("Error when creating log flie", "%e%", e, false);
			
		} finally {
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
		else
			throw new NumberFormatException("\"" + lengthText + "\" is invalid!");
		
		if((int)result <= 0) throw new NumberFormatException("Buffer size should be positive!");
		
		return (int)result;
		
	}

	public static void kill(int errCode) {
		
		logger.log("Kill application with error code : " + errCode);
		logger.kill(3000);
		System.exit(errCode);
		
	}
}