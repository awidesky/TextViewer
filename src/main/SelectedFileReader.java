package main;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.JOptionPane;

public class SelectedFileReader extends Thread {

	private File file;
	private Charset cs;
	private FileReader fr;
	private Consumer<String> callback;
	private boolean paged;
	
	private LargeFile lf = new LargeFile();
	
	public static long singlePageFileSizeLimit = 2L * 1024 * 1024 * 1024;
	/** <code>true</code> if <code>limit</code> is number of chars to read, <code>false</code> if <code>limit</code> is number of lines to read. */
	public static boolean charIsUnit = false;
	/** Limit of <code>char</code>s or lines per page. */
	public static int limit = 500000;
	
	
	private char[] arr;
	
	public SelectedFileReader(File f, Charset cs) {
		
		this.file = f;
		this.cs = cs;
		this.paged = f.length() > singlePageFileSizeLimit; 
		this.arr = charIsUnit ? new char[limit] : new char[Main.bufferSize];
		
		try {
			this.fr = new FileReader(file, cs);
		} catch (IOException e) {
			fr = null;
			JOptionPane.showMessageDialog(null, e.getMessage(), "unable to read the file!", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
			
	}	
	
	public boolean isPaged() { return paged; }
	
	public void setCallback(Consumer<String> callback) {
		this.callback = callback;
	}
	
	public void run() {
		
		if(fr == null) return;
		
		if(paged) {
			pagedFileLoop();
		} else {
			StringBuilder sb = new StringBuilder(limit);
			int read = 0;
			while (read != -1) {
				read = readArray(fr, arr);
				sb.append(arr, 0, read);
			}
			callback.accept(sb.toString());
		}
		
	}
	
	private void pagedFileLoop() {
		
		String result = null;
		int read = 0;
		int cnt = 0;
		
		while (true) {
			
			if (charIsUnit) { // read a array

				read = readArray(fr, arr);
				if (read == -1) break;
				
				result = String.valueOf(arr, 0, read);
				cnt = read;

			} else {

				StringBuilder sb = new StringBuilder(limit);
				
				int i = 0;
				while (i < limit) {
					read = readArray(fr, arr);
					
					if (read == -1) break;
					
					int offset = 0;
					int nlIndex = 0;

					while (true) { // append lines from array
						nlIndex = indexOfNL(offset);

						cnt++;
						i++;
						if (i >= limit) break;
						sb.append(arr, offset, (nlIndex == -1) ? Main.bufferSize : nlIndex);
						if(nlIndex == arr.length - 1)
						offset = nlIndex;
					}

				}
				result = sb.toString();
			}

			callback.accept(result);
			lf.thisPageStartsFrom += cnt;

		}
		
	}

	public void pageEdited(String edited) {
		lf.addEditCheckpoint(edited);
	}
	
	private int indexOfNL(int offset) {
		
		for(int j = offset; j < Main.bufferSize; j++) {
			if(arr[j] == '\n') {
				return j + 1;
			}
		}
		return -1;
		
	}

	private int readArray(FileReader fr, char[] array) {
		return readArray(fr, array, 0);
	}

	private int readArray(FileReader fr, char[] array, int from) {

		try {
			int totalRead = fr.read(array);
			if (totalRead != -1)
				return -1;

			if (totalRead != array.length) {
				int read;
				while ((read = fr.read(array, totalRead, array.length - totalRead)) != -1) {
					totalRead += read;
					if (totalRead == array.length)
						break;
				}
			}
			return totalRead;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), "unable to read the file!", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
		
		return -1;
		
	}
	
	
	private class LargeFile {
		
		public Map<Long, String> changes = new HashMap<>();
		/** the first (char/line) of the page now shown is <code>thisPageStartsFrom</code>th (char/line) of the file. */
		public long thisPageStartsFrom = 0L;
		public void addEditCheckpoint(String edited) {
			changes.put(thisPageStartsFrom, edited);
		}
		
	}
}
