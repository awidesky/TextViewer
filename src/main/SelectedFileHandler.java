package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JOptionPane;

public class SelectedFileHandler {

	private File readFile;
	private Charset readAs;
	private FileReader fr;
	private FileWriter fw;
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
	
	public SelectedFileHandler() { }	
	
	public boolean isPaged() { return paged; }
	
	public void setCallback(Consumer<String> callback) {
		this.callback = callback;
	}
	
	public void startRead(File readFile, Charset readAs) {
		
		this.readFile = readFile;
		this.readAs = readAs;
		this.paged = readFile.length() > singlePageFileSizeLimit; 
		this.arr = saparatePageByLine ? new char[limit] : new char[Main.bufferSize];
		
		try {
			this.fr = new FileReader(readFile, readAs);
		} catch (IOException e) {
			fr = null;
			JOptionPane.showMessageDialog(null, e.getMessage(), "unable to read the file!", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
		new Thread(this::readTask).start();
		
	}
	
	/**
	 * @param text Text of the <code>JTextArea</code> if the file is not paged. if the file is paged, this argument is not used. 
	 * */
	public void startWrite(File writeFile, Charset writeAs, String text) {
		new Thread(() -> writeTask(writeFile, writeAs, text)).start();
	}
	
	private void readTask() {
		
		if(fr == null) return;
		
		if(paged) {
			leftOver = new StringBuilder("");
			pagedFileReadLoop();
		} else {
			StringBuilder sb = new StringBuilder((int) readFile.length());
			int read = 0;
			while (read != -1) {
				read = readArray(fr, arr);
				sb.append(arr, 0, read);
			}
			callback.accept(sb.toString());
		}
		
	}
	
	private void pagedFileReadLoop() {

		String result = null;
		while (true) {
			int read = readArray(fr, arr);

			if (read == -1)
				break;
			
			result = String.valueOf(arr, 0, read);
			
			if (saparatePageByLine) {
				String temp = leftOver.append(result.substring(0, result.lastIndexOf(System.lineSeparator()) - (System.lineSeparator().length() - 1))).toString();
				leftOver = new StringBuilder("");
				leftOver.append(result.substring(result.lastIndexOf(System.lineSeparator()) + System.lineSeparator().length()));
				result = temp;
			}
			
			lf.thisPageEndsAt = lf.thisPageStartsFrom + result.length();
			callback.accept(result); //callback에서 "edit된 page 저장? 물어보거나 확인. 저장할때 thisPageEndsAt포함까지는 다 버리기
			lf.thisPageStartsFrom += result.length() + 1;

		}


	}

	/**
	 * @param text Text of the <code>JTextArea</code> if the file is not paged. if the file is paged, this argument is not used. 
	 * */
	private void writeTask(File writeTo, Charset writeAs, String text) {
		
		if(paged) {
			pagedFileWriteLoop(writeTo, writeAs);
		} else {
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(writeTo, writeAs));
				bw.write(text);
				bw.close();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, e.getMessage(), "unable to write file!", JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			}
		}
		
	}
	
	private void pagedFileWriteLoop(File writeTo, Charset writeAs) {
		
		try {
			this.fr = new FileReader(readFile, readAs);
			this.fw = new FileWriter(writeTo, writeAs);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), "unable to open I/O stream!", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return;
		}
		
		//그냥 다음 thisPageStartsFrom까지 계속 읽으면서 쓰다가 
		
	}
	
	
	public void pageEdited(int offset, int len, String edited) {
		lf.addEditCheckpoint(edited);
	}
	

	/** Fills the array by reading <code>fr</code> */
	private int readArray(FileReader fr, char[] array) {
		return readArray(fr, array, 0);
	}

	/** Fills the array by reading <code>fr</code>
	 *  This method makes sure that <code>array</code> is fully filled unless EOF is read during the reading. 
	 * */
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
		
		public List<EditedPageData> changes = new ArrayList<>();
		/** the first (char/line) of the page now shown is <code>thisPageStartsFrom</code>th char of the file. */
		public long thisPageStartsFrom = 0L;
		/** the first (char/line) of the page now shown is <code>thisPageStartsFrom</code>th char of the file. */
		public long thisPageEndsAt= 0L;
		public void addEditCheckpoint(String edited) {
			changes.add(new EditedPageData(thisPageStartsFrom, thisPageEndsAt, edited));
		}
		
	}
	
}


class EditedPageData {
	public long startsFrom;
	public long endsAt;
	public String edited;
	
	public EditedPageData(long startsFrom, long endsAt, String edited) {
		this.startsFrom = startsFrom;
		this.endsAt = endsAt;
		this.edited = edited;
	}
}