package io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import main.Main;
import main.SettingData;
import util.SwingDialogs;
import util.TaskLogger;

public class SelectedFileHandler {

	private TaskLogger logger;
	
	private TextFile readFile;
	private boolean paged;
	private BlockingQueue<Page> fileContentQueue = new LinkedBlockingQueue<>();
	
	private Map<Long, Page> changes = new HashMap<>();
	private ArrayList<String> hashes = new ArrayList<>();
	private ExecutorService readingThread = Executors.newSingleThreadExecutor();
	private Future<?> readTaskFuture = null;
	private String taskID = null;
	/** are we re-reading file now? */
	private boolean reReading = false;
	/** Is reading task of this SelectedFileHandler closed?? */
	private boolean readingClosed = false;
	private boolean reachedEOF = false;
	private boolean failed = false;
	
	/** Even if <code>Main.setting</code> changes, current instance of <code>setting</code> will not affected. */
	private final SettingData setting = new SettingData(Main.setting);
	
	private char[] arr;
	

	/**
	 *  write-only instance
	 * */
	public SelectedFileHandler() {
		logger = Main.getLogger("[FileHandler | \"Untitled\"]");
		this.paged = false;
		this.arr = new char[setting.getCharBufSize()];
	}
	
	public SelectedFileHandler(TextFile read) {
		logger = Main.getLogger("[FileHandler | " + read.file.getName() + "]");
		this.readFile = read;
		this.paged = readFile.file.length() > setting.getSinglePageFileSizeLimit(); 
		this.arr = new char[setting.getCharBufSize()];
	}	
	
	public boolean isPaged() { return paged; }
	public boolean isReachedEOF() { return reachedEOF; }
	public boolean isFailed() { return failed; }
	public long getLoadedPagesNumber() { return setting.getLoadedPagesNumber(); }
	
	public void startNewRead(BlockingQueue<Page> fileContentQueue2) {
		
		this.fileContentQueue = fileContentQueue2;
		if(readingThread == null) readingThread = Executors.newSingleThreadExecutor();
		readTaskFuture = readingThread.submit(this::readTask);
		
	}
	
	private void readTask() {
		
		reachedEOF = false;
		
		taskID = "[" + Thread.currentThread().getName() + "(" + Thread.currentThread().getId() + ") - reader - " + (int)(Math.random()*100) + "] ";
		logger.log(taskID + "Read task started at - " + new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()));
		logger.log(taskID + "Reading file " + readFile.file.getAbsolutePath());
		logger.log(taskID + "File is " + (paged ? "" : "not ") + "paged because it's " + (paged ? "bigger" : "smaller") + " than " + Main.formatExactFileSize(setting.getSinglePageFileSizeLimit()));
		logger.log(taskID + "Buffer size : " + arr.length + "(chars)");
		long startTime = System.currentTimeMillis();
		
		try(TextReader reader = new TextReader(setting, readFile, taskID)) {
			if (paged) {
				pagedFileReadLoop(reader);
			} else {
				fileContentQueue.put(reader.readAll());
			}
		} catch (InterruptedException e) {
			SwingDialogs.error(taskID + "cannot submit text to GUI!", "%e%", e, true);
			failed = true;
		} catch (IOException e) {
			SwingDialogs.error(taskID + "Error while closing I/O stream", "%e%\nFile : " + readFile.file.getAbsolutePath(), e, true);
			failed = true;
		}
		
		if(failed) SwingUtilities.invokeLater(Main.getMainFrame()::closeFile);
		logger.log(taskID + "Read task " + (failed ? "failed" : "completed") + " in " + (System.currentTimeMillis()- startTime) + "ms");

	}
	
	private void pagedFileReadLoop(TextReader reader) {

		readFile:
		while (true) {

			long nowPage = reader.getNextPageNum();
			logger.newLine();
			logger.log(taskID + "start reading a page #" + nowPage);
			long startTime = System.currentTimeMillis();
			
			Page result;
			try {
				result = changes.getOrDefault(nowPage, reader.readOnePage());
			} catch (IOException e1) {
				SwingDialogs.error(taskID + "I/O Failed!", "%e%", e1, true);
				failed = true;
				break readFile; //Error
			}
			
			if(result == Page.EOF) {
				reachedEOF = true;
				logger.log(taskID + "No more page to read!");
				break readFile; //EOF
			} else if(result.isLastPage()) {
				reachedEOF = true;
			}
			
			logger.log(taskID + "reading page #" + nowPage + " is completed in " + (System.currentTimeMillis() - startTime) + "ms");
			
			if(hashes.size() < result.pageNum()) hashes.add(Main.getHash(result.text));
			
			try {
				fileContentQueue.put(result);
				/** if only one page can be loaded in memory, wait until GUI requests new page */
				if(setting.getLoadedPagesNumber() == 1) {
					result = null;
					synchronized (this) {
						wait();
					}
				}
			} catch (InterruptedException e) {
				if(reReading) {
					logger.log(taskID + "Re-reading the file. Thread " + Thread.currentThread().getName() + " - " + Thread.currentThread().getId() + " interrupted");
					reReading = false;
					return; // EDT is not waiting for the queue. it's executing ActionListener of reRead
				} else if(readingClosed) {
					logger.log(taskID + "File reading task has canceled.");
					return; // EDT is not waiting for the queue. it's executing ActionListener of closeFile
				} else {
					SwingDialogs.error(taskID + "Cannot read file!", "%e%", e, true);
				}
				failed = true;
				break readFile; //Error
			}

		} //readFile:
		
		try {
			fileContentQueue.put(failed ? Page.ERR : Page.EOF);
		} catch (InterruptedException e) {
			SwingDialogs.error(taskID + "Cannot read file!", "%e%", e, true);
			for(int i = 0; !fileContentQueue.offer(Page.ERR) && i < 1000; i++);
		}
		logger.log(taskID + "Paged file reading " + (failed ? "failed" : "completed"));

	}

	
	/**
	 * @param text Text of the <code>JTextArea</code> if the file is not paged. if the file is paged, this argument is not used.
	 * @return <code>true</code> if successfully saved. if canceled/failed, <code>false</code>
	 * */
	public boolean write(TextFile writeTo, String text) {
		
		boolean ret = true;
		
		taskID = "[" + Thread.currentThread().getName() + "(" + Thread.currentThread().getId() + ") - writer - " + (int)(Math.random()*100) + "] ";
		logger.log(taskID + "Write task started at - " + new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()));
		logger.log(taskID + "Writing file " + writeTo.file.getAbsolutePath() + " as encoding : " + writeTo.encoding.name());
		logger.log(taskID + "File is " + (paged ? "" : "not ") + "paged because it's " + (paged ? "bigger" : "smaller") + " than " + Main.formatFileSize(setting.getSinglePageFileSizeLimit()));
		
		long startTime = System.currentTimeMillis();
		
		boolean overWriteSameFile = writeTo.equals(readFile);
		File outputFile = writeTo.file;
		if (overWriteSameFile) {
			logger.log(taskID + "Output file is same as original file! creating temporary file....");
			try {
				outputFile = File.createTempFile(writeTo.file.getName() + new SimpleDateFormat("yyyyMMddkkmmss").format(new Date()), ".txt");
				logger.log(taskID + "Temp File created at : " + outputFile.getAbsolutePath());
			} catch (IOException e) {
				SwingDialogs.error("Failed to make temp file!", "%e%", e, false);
				ret = false;
			}
		}

		if (ret) { //if not already failed, write
			if (paged) {
				ret = pagedFileWriteLoop(new TextFile(outputFile, writeTo.encoding, setting.getLineSeparator()));
			} else {
				try {
					BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), writeTo.encoding));
					bw.write(text.replaceAll("\\R", System.lineSeparator()));
					bw.close();
					ret = true;
				} catch (IOException e) {
					SwingDialogs.error("unable to write file!", "%e%\n\nFile : " + outputFile.getAbsolutePath(), e, false);
					ret = false;
				}
			}

			if (overWriteSameFile) {
				try {
					Files.copy(outputFile.toPath(), writeTo.file.toPath(), StandardCopyOption.REPLACE_EXISTING);
					if (!outputFile.delete())
						outputFile.deleteOnExit();
				} catch (IOException e) {
					SwingDialogs.error("Failed to move temp file do destination!",
							"%e%\nTemp file that (probably) holding the text you tried to save is in : "
									+ outputFile.getAbsolutePath(), e, false);
					ret = false;
				}
			}
		}
		
		logger.log(taskID + "Write task " + (ret ? "completed" : "failed") + " in " + (System.currentTimeMillis()- startTime) + "ms");
		return ret;
		
	}
	
	private boolean pagedFileWriteLoop(TextFile writeTo) { 

		logger.newLine();
		logger.log(taskID + "Original file is  : " + readFile.file.getAbsolutePath() + " as encoding : " + readFile.encoding.name());
		
		try (TextReader reader = new TextReader(setting, readFile, taskID);
				OutputStreamWriter ow = new OutputStreamWriter(new FileOutputStream(writeTo.file), writeTo.encoding);) {

			 while (true) {
				logger.log(taskID + "start reading a page #" + reader.getNextPageNum());
				Page page = reader.readOnePage();
				if (page == Page.EOF) {
					logger.log(taskID + "page #" + (reader.getNextPageNum() - 1) + " is EOF!");
					break;
				}
				logger.log(taskID + "start writing a page #" + (reader.getNextPageNum() - 1));
				ow.write(changes.getOrDefault(page.pageNum(), page).text.replaceAll("\\R", System.lineSeparator()));  //TODO
				if (page.lastNewlineRemoved() && setting.getPageEndsWithNewline()) ow.write(System.lineSeparator()); //last lane separator at the end of a page is eliminated
			}

			logger.log(taskID + "Reached EOF!");
			return true;
		} catch (IOException e) {
			SwingDialogs.error("Unable to open&write I/O stream!", "%e%\nFile : " + writeTo.file.getAbsolutePath(), e, true);
			return false;
		}
		
	}
	
	
	public void pageEdited(Page newPage) { 
		changes.put(newPage.pageNum(), newPage);
	}

	/** This method is called when content in TextArea is identical with original text from file (happens when user discarded change manually) */
	public void pageNotChanged(long pageNum) {
		changes.remove(pageNum);
	}
	
	public boolean isPageEdited(long pageNum, String hash) {
		try {
			String read = hashes.get((int) (pageNum - 1));
			boolean ret = !read.equals(hash);

			logger.log("\nComparing hashes of page #" + pageNum);
			logger.log("Original hash : " + read);
			logger.log("TextArea hash : " + hash);
			logger.log("Edited : " + ret + "\n");

			return ret;
		} catch (IndexOutOfBoundsException e) {
			SwingDialogs.error("wrong index!", "Page #" + pageNum + " does not exist or read yet!\nOnly " + hashes.size() + "page(s) have read\n\n%e%", e, true);
			return true;
		}
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
		if(readingThread == null) return;
		readingThread.shutdownNow();
		try {
			readingThread.awaitTermination(5000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			logger.log(e);
		}
		readingThread = null;
		
	}
	
	private void closeReading() {
		readingClosed = true;
		if(readTaskFuture != null) readTaskFuture.cancel(true);
	}

	public Charset getReadCharset() {
		return readFile.encoding;
	}

}

