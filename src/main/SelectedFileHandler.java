package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import gui.SwingDialogs;
import gui.TitleGeneartor;

public class SelectedFileHandler {

	private File readFile;
	private Charset readAs;
	private FileReader fr;
	private FileWriter fw;
	private boolean paged;
	private LinkedBlockingQueue<Consumer<String>> readCallbackQueue = new LinkedBlockingQueue<>();
	private StringBuilder leftOver = null;
	
	private ConcurrentMap<Long, String> changes = new ConcurrentHashMap<>();
	/** page number starts from 1, not 0! */
	private long pageNum = 1L;
	private Thread readingThread;
	private String taskID = null;
	/** are we re-reading file now? */
	private boolean reReading = false;
	/** Is this SelectedFileHandler closed?? */
	private boolean closed = false;
	
	public static long singlePageFileSizeLimit = 1L * 1024 * 1024 * 1024;
	
	private static int maxCharPerPage = 500000; //TODO : 바꿀 수 있게
	
	private char[] arr;
	

	public SelectedFileHandler() { //write-only instance
		this.paged = false;
		this.arr = new char[Main.bufferSize];
	}
	
	public SelectedFileHandler(File readFile, Charset readAs) { 

		this.readFile = readFile;
		this.readAs = readAs;
		this.paged = readFile.length() > singlePageFileSizeLimit; 
		this.arr = new char[Main.bufferSize];
		
	}	
	
	public boolean isPaged() { return paged; }
	
	
	public void startNewRead(LinkedBlockingQueue<Consumer<String>> readCallbackQueue) {
		
		pageNum = 1L;
		this.readCallbackQueue = readCallbackQueue;
		
		try {
			this.fr = new FileReader(readFile, readAs);
		} catch (IOException e) {
			SwingDialogs.error("unable to read the file!", "%e%", e, false);
			return;
		}
		
		(readingThread = new Thread(this::readTask)).start();
		
	}
	
	private void readTask() {
		
		taskID = "[" + Thread.currentThread().getName() + " - " + Thread.currentThread().getId() + "] ";
		Main.logger.log(taskID + "Reading file " + readFile.getAbsolutePath());
		Main.logger.log(taskID + "File is " + (paged ? "" : "not ") + "paged because it's " + (paged ? "bigger" : "smaller ") + " than " + Main.formatFileSize(singlePageFileSizeLimit));
		Main.logger.log(taskID + "Buffer size : " + arr.length);
		
		if(paged) {
			leftOver = new StringBuilder(arr.length);
			pagedFileReadLoop();
		} else {
			StringBuilder sb = new StringBuilder((int) readFile.length());
			int read = 0;
			while (true) {
				if ((read = readArray()) == -1) break;
				sb.append(arr, 0, read);
			}
			try {
				readCallbackQueue.take().accept(sb.toString());
			} catch (InterruptedException e) {
				SwingDialogs.error(taskID + "cannot read file!", "%e%", e, false);
			}
		
		}
		
		try {
			fr.close();
		} catch (IOException e) {
			SwingDialogs.error(taskID + "unable to close file in " + Thread.currentThread().getName() + " - " + Thread.currentThread().getId(), "%e%", e, false);
		}
		
		Main.logger.log(taskID + "Task completed!\n");

	}
	
	private void pagedFileReadLoop() {

		
		readFile:
		while (true) {

			StringBuilder strBuf = new StringBuilder("");
			String result;
			
			Main.logger.log("\n" + taskID + "start reading a page #" + pageNum);
			if (changes.containsKey(pageNum)) {
				result = changes.get(pageNum);
			} else {

				int totalRead = 0;
				
				int nextRead = Math.min(arr.length, maxCharPerPage);
				while (true) {
					int read = readArray(nextRead);
					if (read == -1) break readFile;

					strBuf.append(arr, 0, read);
					totalRead += read;
					if(totalRead == maxCharPerPage) break;
					
					nextRead = Math.min(arr.length, maxCharPerPage - totalRead); //TODO: line feed 단위로 안 자르는 옵션("Use line separator for page delimiter")
				}
				
				int lastLineFeedIndex = strBuf.lastIndexOf(System.lineSeparator());
				
				result = leftOver.append(strBuf.substring(0, lastLineFeedIndex)).toString();
				leftOver = new StringBuilder("");
				leftOver.append(strBuf.substring(lastLineFeedIndex + System.lineSeparator().length()));

			}

			try {
				readCallbackQueue.take().accept(result);
			} catch (InterruptedException e) {
				if(reReading) {
					Main.logger.log(taskID + "Re-reading the file. closing Thread : " + Thread.currentThread().getName() + " - " + Thread.currentThread().getId());
					reReading = false;
					return;
				} else if(closed) {
					Main.logger.log(taskID + "File reading task has canceled.");
					return;
				} else {
					SwingDialogs.error(taskID + "cannot read file!", "%e%", e, false);
				}
			}
			
			TitleGeneartor.pageNum(pageNum);
			Main.logger.log(taskID + "page #" + pageNum++ + " is consumed!");
		}
		
		try {
			readCallbackQueue.take().accept(null);
		} catch (InterruptedException e) {
			SwingDialogs.error("cannot read file!", "%e%", e, false);
		}
		

	}
	

	/**
	 * @param text Text of the <code>JTextArea</code> if the file is not paged. if the file is paged, this argument is not used. 
	 * */
	public boolean write(File writeTo, Charset writeAs, String text) { //TODO : log
		
		if(paged) {
			return pagedFileWriteLoop(writeTo, writeAs);
		} else {
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(writeTo, writeAs));
				bw.write(text.replaceAll("\\R", System.lineSeparator()));
				bw.close();
				return true;
			} catch (IOException e) {
				SwingDialogs.error("unable to write file!", "%e%", e, false);
				e.printStackTrace();
				return false;
			}
		}
		
	}
	
	private boolean pagedFileWriteLoop(File writeTo, Charset writeAs) {
		
		try {
			this.fr = new FileReader(readFile, readAs);
			this.fw = new FileWriter(writeTo, writeAs);
			
			if(changes.isEmpty()) {
		        int nRead;
		        while ((nRead = fr.read(arr, 0, arr.length)) >= 0) {
		            fw.write(arr, 0, nRead);
		        }
			} else {
				for (long i = 0L; true; i++) {
					int read = readArray();

					if (read == -1)
						break;

					if (changes.containsKey(i)) {
						fw.write(changes.get(i).replaceAll("\\R", System.lineSeparator()));
					} else {
						fw.write(arr, 0, read);
					}
				}
			}
			fr.close();
			fw.close();

			return true;
			
		} catch (IOException e) {
			SwingDialogs.error("unable to open&write I/O stream!", "%e%", e, false);
			return false;
		}
		
	}
	
	
	public void pageEdited(String edited) {
		changes.put(pageNum, edited);
	}
	

	/** Fills the array by reading <code>fr</code>
	 *  This method makes sure that <code>array</code> is fully filled unless EOF is read during the reading. 
	 * */
	private int readArray() {
		return readArray(arr.length);
	}

	/** Fills the array by reading <code>fr</code>
	 *  This method makes sure that <code>array</code> is fully filled unless EOF is read during the reading.
	 *  
	 *  @param len amount of chars to read
	 * */
	private int readArray(int len) {

		Main.logger.logVerbose(taskID + "Try reading " + len + " char(s)...");
		try {
			int totalRead = fr.read(arr, 0, len);
			Main.logger.logVerbose(taskID + "Read " + totalRead + " char(s)");
			if (totalRead == -1)
				return -1;

			if (totalRead != len) {
				Main.logger.logVerbose(taskID + "Buffer not full, try reading more...");
				int read;
				while ((read = fr.read(arr, totalRead, len - totalRead)) != -1) {
					Main.logger.logVerbose(taskID + "Read " + read + " char(s), total : " + totalRead);
					totalRead += read;
					if (totalRead == len) {
						Main.logger.logVerbose(taskID + "Buffer is full!");						
						break;
					}
				}
				if (read == -1) Main.logger.logVerbose(taskID + "EOF reached!");
			}
			
			Main.logger.logVerbose(taskID + "total read char(s) : " + totalRead);
			return totalRead;
		} catch (IOException e) {
			SwingDialogs.error("unable to read the file!", "%e%", e, false);
			e.printStackTrace();
		}
		
		return -1;
		
	}
	
//	public void killNow() {
//		worker.interrupt();
//		try {
//			if(fr != null) fr.close();
//			if(fw != null) fw.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

	public void reRead(LinkedBlockingQueue<Consumer<String>> readCallbackQueue) {
		reReading = true;
		readingThread.interrupt();
		startNewRead(readCallbackQueue);
	}

	public void close() {
		closed = true;
		readingThread.interrupt();
	}
}

