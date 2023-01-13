package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import gui.SwingDialogs;

public class SelectedFileHandler {

	private File readFile;
	private Charset readAs;
	private boolean paged;
	private BlockingQueue<Page> fileContentQueue = new LinkedBlockingQueue<>();
	
	private ArrayList<Page> changes = new ArrayList<>();
	private ExecutorService readingThread = Executors.newSingleThreadExecutor();
	private Future<?> readTaskFuture = null;
	private String taskID = null;
	/** are we re-reading file now? */
	private boolean reReading = false;
	/** Is reading task of this SelectedFileHandler closed?? */
	private boolean readingClosed = false;
	private boolean reachedEOF = false;
	
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
	
	public boolean isReachedEOF() { return reachedEOF; }
	
	public long getLoadedPagesNumber() { return setting.loadedPagesNumber; }
	
	public void startNewRead(BlockingQueue<Page> fileContentQueue2) {
		
		this.fileContentQueue = fileContentQueue2;
		readTaskFuture = readingThread.submit(this::readTask);
		
	}
	
	private void readTask() {
		
		reachedEOF = false;
		
		taskID = "[" + Thread.currentThread().getName() + "(" + Thread.currentThread().getId() + ") - " + (int)(Math.random()*100) + "] ";
		Main.logger.log(taskID + "Read task started at - " + new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()));
		Main.logger.log(taskID + "Reading file " + readFile.getAbsolutePath());
		Main.logger.log(taskID + "File is " + (paged ? "" : "not ") + "paged because it's " + (paged ? "bigger" : "smaller") + " than " + Main.formatFileSize(setting.singlePageFileSizeLimit));
		Main.logger.log(taskID + "Buffer size : " + arr.length + "(chars)");
		long startTime = System.currentTimeMillis();
		
		try(TextReader reader = new TextReader(setting, readFile, readAs, taskID)) {
			if (paged) {
				pagedFileReadLoop(reader);
			} else {
				fileContentQueue.put(reader.readAll());
			}
		} catch (InterruptedException e) {
			SwingDialogs.error(taskID + "cannot submit text to GUI!", "%e%", e, true);
		} catch (IOException e) {
			SwingDialogs.error(taskID + "cannot read file!!", "%e%\n\nFile : " + readFile.getAbsolutePath(), e, true);
		}
		
		Main.logger.log(taskID + "Read task completed in " + (System.currentTimeMillis()- startTime) + "ms");

	}
	
	private void pagedFileReadLoop(TextReader reader) throws IOException {

		readFile:
		while (true) {

			long nowPage = reader.getNextPageNum();
			Main.logger.newLine();
			Main.logger.log(taskID + "start reading a page #" + nowPage);
			long startTime = System.currentTimeMillis();
			
			Page result = Optional.ofNullable(getIfEditedPage(nowPage)).orElse(reader.readOnePage());
			if(result == null) {
				reachedEOF = true;
				Main.logger.log(taskID + "No more page to read!");
				break readFile; //EOF
			} else if(result.isLastPage) {
				reachedEOF = true;
			}
			
			Main.logger.log(taskID + "reading page #" + nowPage + " is completed in " + (System.currentTimeMillis() - startTime) + "ms");
			startTime = System.currentTimeMillis();
			try {
				fileContentQueue.put(result);
				/** if only one page can be loaded in memory, wait until GUI requests new page */
				if(setting.loadedPagesNumber == 1) {
					result = null;
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
					SwingDialogs.error(taskID + "cannot read file!", "%e%", e, true);
				}
			}

		} //readFile:
		
		try {
			fileContentQueue.put(Page.EOF);
		} catch (InterruptedException e) {
			SwingDialogs.error("cannot read file!", "%e%", e, true);
		}
		Main.logger.log(taskID + "File reading completed");

	}

	
	/**
	 * @param text Text of the <code>JTextArea</code> if the file is not paged. if the file is paged, this argument is not used.
	 * @return <code>true</code> if successfully saved. if canceled/failed, <code>false</code>
	 * */
	public boolean write(File writeTo, Charset writeAs, String text) {
		
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
				SwingDialogs.error("unable to write file!", "%e%\n\nFile : " + writeTo.getAbsolutePath(), e, true);
				e.printStackTrace();
				ret = false;
			}
		}
		
		Main.logger.log(taskID + "Write task completed in " + (System.currentTimeMillis()- startTime) + "ms");
		return ret;
		
	}
	
	private boolean pagedFileWriteLoop(File writeTo, Charset writeAs) { 

		Main.logger.newLine();
		
		try (TextReader reader = new TextReader(setting, readFile, readAs, taskID);
				FileWriter fw = new FileWriter(writeTo, writeAs);) {

			Page page = reader.readOnePage();
			do {
				Main.logger.log(taskID + "start reading a page #" + reader.getNextPageNum());
				page = reader.readOnePage();
				Main.logger.log(taskID + "start writing a page #" + (reader.getNextPageNum() - 1));
				fw.write(Optional.ofNullable(getIfEditedPage(page.pageNum)).orElse(page).text);
			} while (page != null);

			Main.logger.log(taskID + "Reached EOF!");
			return true;
		} catch (IOException e) {
			SwingDialogs.error("Unable to open&write I/O stream!", "%e%\n\nFile : " + writeTo.getAbsolutePath(), e, true);
			return false;
		}
		
	}
	
	
	public void pageEdited(Page newPage) { 
		changes.add(newPage);
	}

	public Page getIfEditedPage(long pageNum) {
		if(changes.isEmpty()) return null;
		for(Page p : changes) {
			if(p.pageNum == pageNum) return p;
		}
		return null;
	}

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
	}
}

