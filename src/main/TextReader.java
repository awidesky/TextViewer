package main;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;

import gui.SwingDialogs;

public class TextReader {

	private File readFile;
	private FileReader fr;
	private StringBuilder leftOver = null;
	private SettingData setting;
	
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
			SwingDialogs.error("unable to read the file!", "%e%", e, false);
			return;
		}
		
	}
	
	public String readAll() {
		Main.logger.log(taskID + "start reading the file until reach EOF");
		StringBuilder sb = new StringBuilder((int) readFile.length());
		int read = 0;
		while (true) {
			if ((read = readArray()) == -1) break;
			sb.append(arr, 0, read);
		}
		Main.logger.log(taskID + "Reached EOF");
		return sb.toString();
	}
	

	public String readOnePage() {

		int totalRead = 0;
		
		StringBuilder strBuf = new StringBuilder("");
		String result = null;
		
		int nextRead = Math.min(arr.length, setting.charPerPage);
		while (true) {
			int read = readArray(nextRead);
			if (read == -1) {
				if(totalRead == 0) {
					SwingDialogs.information("No more page to read!", "Reached EOF!", false);
					return null;
				} else { break; }
			} else if(read == -2) { //Exception
				return null; //TODO : Throw empty RuntimeException
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

		return result;
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

	public void close() {
		// TODO Auto-generated method stub
		
	}
	
	
}
