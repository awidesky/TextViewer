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
	/** Is reading task of this SelectedFileHandler closed?? */
	private boolean readingClosed = false;
	
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

		TextReader reader = new TextReader(setting, readFile, readAs, taskID);
		
		taskID = "[" + Thread.currentThread().getName() + "(" + Thread.currentThread().getId() + ") - " + (int)(Math.random()*100) + "] ";
		Main.logger.log(taskID + "Read task started at - " + new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()));
		Main.logger.log(taskID + "Reading file " + readFile.getAbsolutePath());
		Main.logger.log(taskID + "File is " + (paged ? "" : "not ") + "paged because it's " + (paged ? "bigger" : "smaller") + " than " + Main.formatFileSize(setting.singlePageFileSizeLimit));
		Main.logger.log(taskID + "Buffer size : " + arr.length + "(chars)");
		long startTime = System.currentTimeMillis();
		
		if(paged) {
			pagedFileReadLoop(reader);
		} else {
			try {
				fileContentQueue.put(new Page(reader.readAll(), -1));
			} catch (InterruptedException e) {
				SwingDialogs.error(taskID + "cannot submit text to GUI!", "%e%", e, false);
			}

		}
		
		try {
			reader.close();
		} catch (IOException e) {
			SwingDialogs.error(taskID + "unable to close file in " + Thread.currentThread().getName() + " - " + Thread.currentThread().getId(), "%e%", e, false);
		}
		
		Main.logger.log(taskID + "Read task completed in " + (System.currentTimeMillis()- startTime) + "ms");

	}
	
	private void pagedFileReadLoop(TextReader reader) {

		readFile:
		while (true) {

			String result;
			
			Main.logger.newLine();
			Main.logger.log(taskID + "start reading a page #" + pageNum);
			long startTime = System.currentTimeMillis();
			if (changes.containsKey(pageNum)) {
				result = changes.get(pageNum);
			} else {
				if((result = reader.readOnePage()) == null) break readFile;
			}

			Main.logger.log(taskID + "reading page #" + pageNum + " is completed in " + (System.currentTimeMillis() - startTime) + "ms");
			startTime = System.currentTimeMillis();
			try {
				fileContentQueue.put(new Page(result, pageNum));
				
				/** if only one page can be loaded in memory, wait until GUI requests new page */
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
				} else if(readingClosed) {
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
	 * @return <code>true</code> if successfully saved. if canceled/failed, <code>false</code>
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
	
	private boolean pagedFileWriteLoop(File writeTo, Charset writeAs) { //TODO : 쓰기용 fr은 따로여야 함 - 읽던 도중에 저장하고, 다음 페이지 읽으면???
		
		try {
			
			TextReader reader = new TextReader(setting, readFile, readAs, taskID);
			
			if(fw != null) fw.close();
			fw = new FileWriter(writeTo, writeAs);
			
			if(changes.isEmpty()) {
		        int nRead;
		        while ((nRead = fr.read(arr, 0, arr.length)) >= 0) {
		            fw.write(arr, 0, nRead);
		        }
			} else {
				for (long i = 1L; true; i++) {
					String page = reader.readOnePage();
					if(page == null) break;
					if (changes.containsKey(i)) {
						fw.write(changes.get(i).replaceAll("\\R", System.lineSeparator()));
					} else {
						fw.write(page);
					}
				}
			}
			reader.close();
			fw.close();

			return true;
			
		} catch (IOException e) {
			SwingDialogs.error("Unable to open&write I/O stream!", "%e%", e, false);
			return false;
		}
		
	}
	
	
	public void pageEdited(String edited) { //TODO : use this in nextpage!!
		changes.put(pageNum, edited);
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
		
		closeReading();
		readingThread.shutdownNow();
		try {
			readingThread.awaitTermination(5000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Main.logger.log(e);
		}
	}
	
	private void closeReading() {
		readingClosed = true;
		if(readTaskFuture != null) readTaskFuture.cancel(true);
		if(fr != null) {
			try {
				fr.close();
			} catch (IOException e) {
				SwingDialogs.error("Exception while closing FileReader!", "%e%", e, false);
			}
		}
	}
}

