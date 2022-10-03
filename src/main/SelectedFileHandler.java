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

import javax.swing.JOptionPane;

import gui.SwingDialogs;

public class SelectedFileHandler {

	private File readFile;
	private Charset readAs;
	private FileReader fr;
	private FileWriter fw;
	private boolean paged;
	private LinkedBlockingQueue<Consumer<String>> readCallbackQueue;
	private StringBuilder leftOver = null;
	
	private ConcurrentMap<Long, String> changes = new ConcurrentHashMap<>();
	/** page number starts from 1, not 0! */
	private long pageNum = 0L;
	private Thread readingThread;
	
	public static long singlePageFileSizeLimit = 1L * 1024 * 1024 * 1024;
	
	private static int maxCharPerPage = 500000; 
	
	private char[] arr;
	

	public SelectedFileHandler() { //write-only instance
		this.paged = false;
		this.arr = new char[Main.bufferSize];
	}
	
	public SelectedFileHandler(File readFile, Charset readAs) { 

		this.readFile = readFile;
		this.readAs = readAs;
		this.paged = readFile.length() > singlePageFileSizeLimit; 
		this.arr = new char[paged ? maxCharPerPage : Main.bufferSize];
		
	}	
	
	public boolean isPaged() { return paged; }
	
	
	public void startRead(LinkedBlockingQueue<Consumer<String>> readCallbackQueue) {
		
		pageNum = 0L;
		this.readCallbackQueue = readCallbackQueue;
		
		try {
			this.fr = new FileReader(readFile, readAs);
		} catch (IOException e) {
			SwingDialogs.error("unable to read the file!", "%e%", e, false);
			return;
		}
		Main.logger.log("\nReading file " + readFile.getAbsolutePath());
		(readingThread = new Thread(this::readTask)).start();
		
	}
	
	private void readTask() {
		
		Main.logger.log("File is " + (paged ? "" : "not ") + "paged");
		Main.logger.log("Buffer size : " + arr.length + "\n");
		
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
				SwingDialogs.error("cannot read file!", "%e%", e, false);
			}
		
		}
		
		Main.logger.log("File read completed!\n");
		
		try {
			fr.close();
		} catch (IOException e) {
			SwingDialogs.error("unable to close file!", "%e%", e, false);
		}

	}
	
	private void pagedFileReadLoop() {

		
		readFile:
		while (true) {

			StringBuilder strBuf = new StringBuilder("");
			String result;
			
			Main.logger.log("start reading a page #" + pageNum);
			if (changes.containsKey(pageNum + 1)) {
				result = changes.get(pageNum + 1);
			} else {

				int totalRead = 0;
				
				int nextRead = Math.min(arr.length, maxCharPerPage);
				while (true) {
					int read = readArray(nextRead);
					if (read == -1) break readFile;

					strBuf.append(arr, 0, read);
					totalRead += read;
					if(totalRead == maxCharPerPage) break;
					
					nextRead = Math.min(arr.length, maxCharPerPage - totalRead); //readTask에서도 min메소드?
				}
				
				int lastLineFeedIndex = strBuf.lastIndexOf(System.lineSeparator());
				
				result = leftOver.append(strBuf.substring(0, lastLineFeedIndex)).toString();
				leftOver = new StringBuilder("");
				leftOver.append(strBuf.substring(lastLineFeedIndex + System.lineSeparator().length()));

			}

			try {
				readCallbackQueue.take().accept(result);
			} catch (InterruptedException e) {
				SwingDialogs.error("cannot read file!", "%e%", e, false);
			}
			
			pageNum++;
			Main.logger.log("page #" + pageNum + " is read!");
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
	public boolean write(File writeTo, Charset writeAs, String text) {
		
		if(paged) {
			return pagedFileWriteLoop(writeTo, writeAs);
		} else {
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(writeTo, writeAs));
				bw.write(text.replaceAll("\\R", System.lineSeparator()));
				bw.close();
				return true;
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, e.getMessage(), "unable to write file!", JOptionPane.ERROR_MESSAGE);
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
			JOptionPane.showMessageDialog(null, e.getMessage(), "unable to open&write I/O stream!", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
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

		Main.logger.log("Try reading " + len + " char(s)...");
		try {
			int totalRead = fr.read(arr, 0, len);
			Main.logger.log("Read " + totalRead + " char(s)");
			if (totalRead == -1)
				return -1;

			if (totalRead != len) {
				Main.logger.log("Buffer not full, try reading more...");
				int read;
				while ((read = fr.read(arr, totalRead, len - totalRead)) != -1) {
					Main.logger.log("Read " + read + " char(s), total : " + totalRead);
					totalRead += read;
					if (totalRead == len) {
						Main.logger.log("Buffer is full!");						
						break;
					}
				}
				if (read == -1) Main.logger.log("EOF reached!");
			}
			
			Main.logger.log("total read char(s) : " + totalRead + "\n");
			return totalRead;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), "unable to read the file!", JOptionPane.ERROR_MESSAGE);
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
		readingThread.interrupt();
		startRead(readCallbackQueue);
	}
}

