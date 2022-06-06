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
  
	public static int bufferSize = 1024 * 8; //TODO : setting���� �̰͵� ���ϱ�(line������ limit ���� ������)

	public static LoggerThread logger;
	
	public static void main(String[] args) { //TODO : logger �߰�
	 
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
		
		
		
		SwingUtilities.invokeLater(() ->{
			new MainFrame();
		});
	 
    }

	public static void kill(int errCode) {
		
		logger.kill(3000);
		System.exit(errCode);
		
	}
}