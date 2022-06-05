package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.swing.JOptionPane;

public class SelectedFileHandler {

	private File readFile;
	private Charset readAs;
	private FileReader fr;
	private FileWriter fw;
	private boolean paged;
	private ArrayBlockingQueue<String> textQueue;
	private StringBuilder leftOver = null;
	
	private ConcurrentMap<Long, String> changes = new ConcurrentHashMap<>();
	private long pageNum = 0L;
	private Thread readingThread;
	
	public static long singlePageFileSizeLimit = 2L * 1024 * 1024 * 1024;
	/** If <code>true</code>, a page always starts/ends as a whole line,
	 *  If <code>false</code>, a page is always <code>limit</code> length of <code>char</code>s. */
	public static boolean saparatePageByLine = false; //TODO : paged 파일이 열려 있는 동안에는 변경 못하게 하기. Main.bufferSize는 파일 읽는 도중에 변경 못하게, Main.bufferSize으 설정창에서 변경..?
	/** Limit of <code>char</code>s. */
	public static int limit = 500000; 
	
	
	private char[] arr;
	
	public SelectedFileHandler(File readFile, Charset readAs) {

		this.readFile = readFile;
		this.readAs = readAs;
		this.paged = readFile.length() > singlePageFileSizeLimit; 
		this.arr = saparatePageByLine ? new char[limit] : new char[Main.bufferSize];
		
	}	
	
	public boolean isPaged() { return paged; }
	
	
	public void startRead(ArrayBlockingQueue<String> textQueue) {
		
		pageNum = 0L;
		this.textQueue = textQueue;
		
		try {
			this.fr = new FileReader(readFile, readAs);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), "unable to read the file!", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return;
		}
		(readingThread = new Thread(this::readTask)).start();
		
	}
	
	private void readTask() {
		
		if(paged) {
			leftOver = new StringBuilder(arr.length);
			pagedFileReadLoop();
		} else {
			StringBuilder sb = new StringBuilder((int) readFile.length());
			int read = 0;
			while (read != -1) {
				read = readArray(fr, arr);
				sb.append(arr, 0, read);
			}
			textQueue.offer(sb.toString());
		}
		
		try {
			fr.close();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), "unable to read file!", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}

		try {
			textQueue.put("");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	private void pagedFileReadLoop() {

		String result = "";
		while (true) {

			if (changes.containsKey(pageNum + 1)) {
				result = changes.get(pageNum + 1);
			} else {

				int read = readArray(fr, arr);

				if (read == -1)
					break;

				result = String.valueOf(arr, 0, read);

				if (saparatePageByLine) {
					String temp = leftOver
							.append(result.substring(0,
									result.lastIndexOf(System.lineSeparator()) - (System.lineSeparator().length() - 1)))
							.toString();
					leftOver = new StringBuilder(arr.length);
					leftOver.append(result
							.substring(result.lastIndexOf(System.lineSeparator()) + System.lineSeparator().length()));
					result = temp;
				}

			}
			
			try {
				textQueue.put(result);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			pageNum++;

		}
		
		try {
			textQueue.put("");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
				bw.write(text.replace(System.lineSeparator(), "\n").replace("\n", System.lineSeparator()));
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
					int read = readArray(fr, arr);

					if (read == -1)
						break;

					if (changes.containsKey(i)) {
						fw.write(changes.get(i).replace(System.lineSeparator(), "\n").replace("\n", System.lineSeparator()));
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
	
//	public void killNow() {
//		worker.interrupt();
//		try {
//			if(fr != null) fr.close();
//			if(fw != null) fw.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

	public void reRead(ArrayBlockingQueue<String> newTextQueue) {
		readingThread.interrupt();
		startRead(newTextQueue);
	}
}

