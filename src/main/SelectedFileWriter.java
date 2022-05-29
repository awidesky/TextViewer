package main;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.JOptionPane;

import main.SelectedFileHandler.LargeFile;

public class SelectedFileWriter extends Thread {

	private File file;
	private FileReader fr;
	private Consumer<String> callback;
	private boolean paged;
	
	private StringBuilder leftOver = null;
	
	private LargeFile lf = new LargeFile();
	
	public static long singlePageFileSizeLimit = 2L * 1024 * 1024 * 1024;
	/** If <code>true</code>, a page always starts/ends as a whole line,
	 *  If <code>false</code>, a page is always <code>limit</code> length of <code>char</code>s. */
	public static boolean saparatePageByLine = false;
	/** Limit of <code>char</code>s. */
	public static int limit = 500000;
	
	
	private char[] arr;
	
	public SelectedFileWriter(File f, Charset cs, Map<Long, String> changes) {
		
		this.file = f;
		this.paged = f.length() > singlePageFileSizeLimit; 
		this.arr = saparatePageByLine ? new char[limit] : new char[Main.bufferSize];
		
		try {
			this.fr = new FileReader(file, cs);
		} catch (IOException e) {
			fr = null;
			JOptionPane.showMessageDialog(null, e.getMessage(), "unable to read the file!", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
			
	}
}
