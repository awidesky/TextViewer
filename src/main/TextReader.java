package main;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;

import gui.SwingDialogs;

public class TextReader implements AutoCloseable{

	private File readFile;
	private FileReader fr;
	private StringBuilder leftOver = null;
	private SettingData setting;
	private long nextPageNum = 1L;
	private String taskID = null;
	
	private char[] arr;
	
	public TextReader(SettingData setting, File readFile, Charset readAs, String taskID) {
		
		this.setting = setting;
		this.readFile = readFile;
		this.arr = new char[setting.charBufSize];
		this.taskID = taskID;
		if (setting.pageEndsWithNewline) { leftOver = new StringBuilder(""); }
		
		try {
			this.fr = new FileReader(readFile, readAs);
		} catch (IOException e) {
			SwingDialogs.error("unable to read the file!", "%e%", e, true);
			return;
		}
		
	}
	
	
	public long getNextPageNum() { return nextPageNum; }
	
	
	public Page readAll() throws IOException {
		Main.logger.log(taskID + "start reading the file until reach EOF");
		StringBuilder sb = new StringBuilder((int) readFile.length());
		int read = 0;
		while (true) {
			if ((read = readArray()) == -1) break;
			sb.append(arr, 0, read);
		}
		Main.logger.log(taskID + "Reached EOF");
		return new Page(sb.toString(), -1);
	}
	

	/**
	 * Read a <code>Page</code>.
	 * @return <code>Page.EOF</code> when EOF reached, <code>null</code> if the end of the stream has been reached without reading any characters
	 * @throws IOException 
	 * */
	public Page readOnePage() throws IOException {

		int totalRead = 0;
		
		StringBuilder strBuf = new StringBuilder("");
		String result = null;
		
		int nextRead = Math.min(arr.length, setting.charPerPage);
		while (true) {
			int read = readArray(nextRead);
			if (read == -1) {
				if(totalRead == 0) {
					Main.logger.log("Reached EOF, No more page to read!");
					return null;
				} else { break; }
			}
			strBuf.append(arr, 0, read);
			totalRead += read;
			if(totalRead == setting.charPerPage) break;
			
			nextRead = Math.min(arr.length, setting.charPerPage - totalRead);
		}
		
		if (setting.pageEndsWithNewline) {
			int lastLineFeedIndex = strBuf.lastIndexOf(System.lineSeparator()); // TODO : 문자열이 너무 작아서 newLine 없으면

			result = leftOver.append(strBuf.substring(0, lastLineFeedIndex)).toString();
			leftOver = new StringBuilder("");
			leftOver.append(strBuf.substring(lastLineFeedIndex + System.lineSeparator().length()));
		} else {
			result = strBuf.toString();
		}

		return new Page(result, nextPageNum++);
	}
	
	

	/** Fills the array by reading <code>fr</code>
	 *  This method makes sure that <code>array</code> is fully filled unless EOF is read during the reading. 
	 *  @throws IOException 
	 * */
	private int readArray() throws IOException {
		return readArray(arr.length);
	}

	/** Fills the array by reading <code>fr</code>
	 *  This method makes sure that <code>array</code> is fully filled unless EOF is read during the reading.
	 *  
	 *  @param len amount of chars to read
	 *  
	 *  @return How many char(s) read<p>-1 when EOF reached<p>-2 when Exception occured
	 *  @throws IOException 
	 *  
	 * */
	private int readArray(int len) throws IOException {

		Main.logger.logVerbose(taskID + "Try reading " + len + " char(s)...");
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
			if (read == -1)
				Main.logger.logVerbose(taskID + "EOF reached!");
		}

		Main.logger.logVerbose(taskID + "total read char(s) : " + totalRead);
		return totalRead;

	}

	@Override
	public void close() throws IOException {
		if(fr != null) fr.close();
	}
	
	
}
