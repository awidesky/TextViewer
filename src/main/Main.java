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
	
	public static int bufferSize = 1024 * 8; //TODO : setting���� �̰͵� ���ϱ�(line������ limit ���� ������)

	//TODO : menu disable�ŵ� ����Ű�� �Գ�?
	//TODO : paged �дٰ� �߰��� �ٸ� ���� ������ queue �����ֱ�
	
	
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

	public static void kill(int errCode) {
		
		logger.log("Kill application with error code : " + errCode);
		logger.kill(3000);
		System.exit(errCode);
		
	}
}