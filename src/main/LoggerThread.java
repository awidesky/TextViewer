package main;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * In logging Thread, <code>isStop</code> is not written, only read.
 * This makes declaring <code>isStop</code> as <code>volatile</code> is OK
 * 
 * Be careful not to modify <code>isStop</code> in multiple threads
 * */
public class LoggerThread extends Thread {

	private boolean verbose = false;
	
	private PrintWriter logTo;
	private LinkedBlockingQueue<Runnable> loggerQueue = new LinkedBlockingQueue<>();
	
	private volatile boolean isStop = false;
	
	public LoggerThread() { 
		this(false);
	}
	
	public LoggerThread(boolean verbose) {
		this(System.out);
		this.verbose = verbose;
	}
	
	public LoggerThread(OutputStream os) {
		logTo = new PrintWriter(os, true);
	}


	@Override
	public void run() {  

		while (true) {

			if (loggerQueue.isEmpty() && isStop) {
				break;
			}

			try {
				loggerQueue.take().run();
			} catch (InterruptedException e) {
				logTo.println("LoggerThread Interrupted! : " + e.getMessage());
				logTo.println("Closing logger..");
				break;
			}
		}
		
		logTo.close();

	}
	
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public void log(String data) {

		loggerQueue.offer(() -> {
			logTo.println(data.replaceAll("\\R", System.lineSeparator()));
		});
		
	}
	
	public void logVerbose(String data) {

		if(verbose) log(data);
		
	}
	
	public void log(Exception e) {
		
		loggerQueue.offer(() -> {
			e.printStackTrace(logTo);
		});
		
	}

	/**
	 * Kill LoggerThread in <code>timeOut</code> ms.
	 * */
	public void kill(int timeOut) {
		
		isStop = true;
		
		try {
			this.join(timeOut);
		} catch (InterruptedException e) {
			logTo.println("Failed to join logger thread!");
			e.printStackTrace(logTo);
		}
		
		logTo.close();
		
	}
}
