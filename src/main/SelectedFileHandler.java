package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import gui.SwingDialogs;

public class SelectedFileHandler {

	private File readFile;
	private Charset readAs;
	private FileReader fr;
	private FileWriter fw;
	private boolean paged;
	private BlockingQueue<Page> fileContentQueue = new LinkedBlockingQueue<>();
	private StringBuilder leftOver = null;
	
	private ConcurrentMap<Long, String> changes = new ConcurrentHashMap<>();
	/** page number starts from 1, not 0! */
	private long pageNum = 1L;
	private ExecutorService readingThread = Executors.newSingleThreadExecutor();
	private Future<?> readTaskFuture = null;
	private String taskID = null;
	/** are we re-reading file now? */
	private boolean reReading = false;
	/** Is this SelectedFileHandler closed?? */
	private boolean closed = false;
	
	/** Even if <code>Main.setting</code> changes, current instance of <code>setting</code> will not affected. */
	private final SettingData setting = new SettingData(Main.setting);
	
	private char[] arr;
	

	/**
	 *  write-only instance
	 * */
	public SelectedFileHandler() {
		this.paged = false;
		this.arr = new char[setting.charBufSize];
	}
	
	public SelectedFileHandler(File readFile, Charset readAs) { 

		this.readFile = readFile;
		this.readAs = readAs;
		this.paged = readFile.length() > setting.singlePageFileSizeLimit; 
		this.arr = new char[setting.charBufSize];
		
	}	
	
	public boolean isPaged() { return paged; }
	
	public long getPageNum() { return pageNum; }
	public long getLoadedPagesNumber() { return setting.loadedPagesNumber; }
	
	public void startNewRead(BlockingQueue<Page> fileContentQueue2) {
		
		this.fileContentQueue = fileContentQueue2;
		readTaskFuture = readingThread.submit(this::readTask);
		
	}
	
	private void readTask() {
		
		pageNum = 1L;
		
		try {
			this.fr = new FileReader(readFile, readAs);
		} catch (IOException e) {
			SwingDialogs.error("unable to read the file!", "%e%", e, false);
			return;
		}
		
		taskID = "[" + Thread.currentThread().getName() + "(" + Thread.currentThread().getId() + ") - " + (int)(Math.random()*100) + "] ";
		Main.logger.log(taskID + "Read task started at - " + new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()));
		Main.logger.log(taskID + "Reading file " + readFile.getAbsolutePath());
		Main.logger.log(taskID + "File is " + (paged ? "" : "not ") + "paged because it's " + (paged ? "bigger" : "smaller") + " than " + Main.formatFileSize(setting.singlePageFileSizeLimit));
		Main.logger.log(taskID + "Buffer size : " + arr.length + "(chars)");
		long startTime = System.currentTimeMillis();
		
		if(paged) {
			if (setting.pageEndsWithNewline) { leftOver = new StringBuilder(arr.length); }
			pagedFileReadLoop();
		} else {
			Main.logger.log(taskID + "start reading the file until reach EOF");
			StringBuilder sb = new StringBuilder((int) readFile.length());
			int read = 0;
			while (true) {
				if ((read = readArray()) == -1) break;
				sb.append(arr, 0, read);
			}
			Main.logger.log(taskID + "Reached EOF");
			try {
				fileContentQueue.put(new Page(sb.toString(), -1));
			} catch (InterruptedException e) {
				SwingDialogs.error(taskID + "cannot submit text to GUI!", "%e%", e, false);
			}

		}
		
		try {
			fr.close();
		} catch (IOException e) {
			SwingDialogs.error(taskID + "unable to close file in " + Thread.currentThread().getName() + " - " + Thread.currentThread().getId(), "%e%", e, false);
		}
		
		Main.logger.log(taskID + "Read task completed in " + (System.currentTimeMillis()- startTime) + "ms");

	}
	
	private void pagedFileReadLoop() {

		readFile:
		while (true) {

			StringBuilder strBuf = new StringBuilder("");
			String result;
			
			Main.logger.newLine();
			Main.logger.log(taskID + "start reading a page #" + pageNum);
			long startTime = System.currentTimeMillis();
			if (changes.containsKey(pageNum)) {
				result = changes.get(pageNum);
			} else {

				int totalRead = 0;
				
				int nextRead = Math.min(arr.length, setting.charPerPage);
				while (true) {
					int read = readArray(nextRead);
					if (read == -1) {
						if(totalRead == 0) {
							SwingDialogs.information("No more page to read!", "Reached EOF!", false);
							break readFile;
						} else { break; }
					} else if(read == -2) { //Exception
						break readFile;
					}

					strBuf.append(arr, 0, read);
					totalRead += read;
					if(totalRead == setting.charPerPage) break;
					
					nextRead = Math.min(arr.length, setting.charPerPage - totalRead);
				}
				
				if (setting.pageEndsWithNewline) {
					int lastLineFeedIndex = strBuf.lastIndexOf(System.lineSeparator());

					result = leftOver.append(strBuf.substring(0, lastLineFeedIndex)).toString();
					leftOver = new StringBuilder("");
					leftOver.append(strBuf.substring(lastLineFeedIndex + System.lineSeparator().length()));
				} else {
					result = strBuf.toString();
				}

			}

			Main.logger.log(taskID + "reading page #" + pageNum + " is completed in " + (System.currentTimeMillis() - startTime) + "ms");
			startTime = System.currentTimeMillis();
			try {
				fileContentQueue.put(new Page(result, pageNum));
				/**
				 * only one page can be loaded in memory, wait until GUI requests new page
				 * */
				if(setting.loadedPagesNumber == 1) {
					synchronized (this) {
						wait();
					}
				}
			} catch (InterruptedException e) {
				if(reReading) {
					Main.logger.log(taskID + "Re-reading the file. Thread " + Thread.currentThread().getName() + " - " + Thread.currentThread().getId() + " interrupted");
					reReading = false;
					return;
				} else if(closed) {
					Main.logger.log(taskID + "File reading task has canceled.");
					return;
				} else {
					SwingDialogs.error(taskID + "cannot read file!", "%e%", e, false);
				}
			}

			pageNum++;
		} //readFile:
		
		Main.logger.log(taskID + "No more page to read!");
		try {
			fileContentQueue.put(null);
		} catch (InterruptedException e) {
			SwingDialogs.error("cannot read file!", "%e%", e, false);
		}
		Main.logger.log(taskID + "File reading completed");

	}
	

	/**
	 * @param text Text of the <code>JTextArea</code> if the file is not paged. if the file is paged, this argument is not used. 
	 * */
	public boolean write(File writeTo, Charset writeAs, String text) { //TODO : log
		
		taskID = "[" + Thread.currentThread().getName() + "(" + Thread.currentThread().getId() + ") - " + (int)(Math.random()*100) + "] ";
		Main.logger.log(taskID + "Write task started at - " + new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()));
		Main.logger.log(taskID + "Writing file " + writeTo.getAbsolutePath() + " as encoding : " + writeAs.name());
		Main.logger.log(taskID + "File is " + (paged ? "" : "not ") + "paged because it's " + (paged ? "bigger" : "smaller") + " than " + Main.formatFileSize(setting.singlePageFileSizeLimit));
		long startTime = System.currentTimeMillis();
		
		boolean ret;
		if(paged) {
			ret = pagedFileWriteLoop(writeTo, writeAs);
		} else {
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(writeTo, writeAs));
				bw.write(text.replaceAll("\\R", System.lineSeparator()));
				bw.close();
				ret = true;
			} catch (IOException e) {
				SwingDialogs.error("unable to write file!", "%e%", e, false);
				e.printStackTrace();
				ret = false;
			}
		}
		
		Main.logger.log(taskID + "Write task completed in " + (System.currentTimeMillis()- startTime) + "ms");
		return ret;
		
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
	 *  
	 *  @return How many char(s) read<p>-1 when EOF reached<p>-2 when Exception occured
	 *  
	 * */
	private int readArray(int len) {

		Main.logger.logVerbose(taskID + "Try reading " + len + " char(s)...");
		try {
			int totalRead = fr.read(arr, 0, len);
			Main.logger.logVerbose(taskID + "Read " + totalRead + " char(s)");
			if (totalRead == -1) {
				Main.logger.logVerbose(taskID + "File pointer position is at EOF");
				return -1;
			}
			
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
			return -2;
		}
		
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

	public void reRead(BlockingQueue<Page> fileContentQueue2) {
		reReading = true;
		readTaskFuture.cancel(true);
		startNewRead(fileContentQueue2);
	}

	/**
	 * close the handle, shutting down Worker Thread.
	 * waits 5000ms for Worker to shutdown
	 * */
	public void close() {
		closed = true;
		if(readTaskFuture != null) readTaskFuture.cancel(true);
		readingThread.shutdownNow();
		try {
			readingThread.awaitTermination(5000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Main.logger.log(e);
		}
	}
}

