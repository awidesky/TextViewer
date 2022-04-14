package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import main.LargeFileHandlingRule;
import main.ReferenceDTO;

public class MainFrame extends JFrame {

	private static final long serialVersionUID = 4096788492544078810L;
	
	private JScrollPane sp;
	private JTextArea ta = new JTextArea();
	private File lastOpened = new File(System.getProperty("user.home"));
	private File lastSaved = new File(System.getProperty("user.home"));
	private Charset lastedOpenedCharset = null;
	private String version = "TextViewer v1.0";
	
	private TestFilechooser f = new TestFilechooser();
	
	private LargeFileHandlingRule lfhRule = new LargeFileHandlingRule(2L * 1024 * 1024 * 1024, false, 500000); //about 500000 line in 100mb DISM log file
	private JMenu pageMenu = new JMenu("Pages");
	private boolean paged = false;
	
	private JMenuBar menuBar;
	private JMenu fileMenu;
	private JMenuItem openFile;
	private JMenuItem saveFile;
	private JMenu formatMenu;
	private JMenuItem largeSetting;
	private JMenuItem font;
	private JCheckBoxMenuItem editable;
	private JMenuItem next;
	private JMenuItem reRead;

	private boolean newFileReading = false;
	
	
	public MainFrame() {
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		setTitle(version);
		setSize(800, 700);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				
				if(!saveBeforeClose()) return; 

				e.getWindow().dispose();
				System.exit(0);

			}

		});
		
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(dim.width/2-getSize().width/2, dim.height/2-getSize().height/2);
		
		
		addMenubar();
		
		ta.setBackground(Color.LIGHT_GRAY);
		ta.setEditable(false);
		ta.getDocument().addDocumentListener(new DocumentListener() {

	        @Override
	        public void removeUpdate(DocumentEvent e) {
	        	if(newFileReading) { //new file is just read. user didn't type anything.
	        		return;
	        	}
	        	if(!getTitle().startsWith("*")) setTitle("*" + getTitle());
	        }

	        @Override
	        public void insertUpdate(DocumentEvent e) {
	        	if(newFileReading) { //new file is just read. user didn't type anything.
	        		return;
	        	}
	        	if(!getTitle().startsWith("*")) setTitle("*" + getTitle());
	        }

	        @Override
	        public void changedUpdate(DocumentEvent arg0) {
  	        	if(newFileReading) { //new file is just read. user didn't type anything.
	        		return;
	        	}
	        	if(!getTitle().startsWith("*")) setTitle("*" + getTitle());
	        }
	    });
		sp = new JScrollPane(ta, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		sp.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		sp.setBackground(Color.WHITE);
		
		add(sp);
		
		setVisible(true);
		
	}
	
	/**
	 * 
	 * Ask save the file before closing file.
	 * 
	 * @return <code>true</code> if current file is OK to close. <code>false</code> if it's not OK.
	 * 
	 * */
	protected boolean saveBeforeClose() {
		if(getTitle().startsWith("*")) {
			switch(JOptionPane.showConfirmDialog(null, "Save changed content?", "Save change?", JOptionPane.YES_NO_CANCEL_OPTION)) {
			case JOptionPane.YES_OPTION:
				return saveFile();
			case JOptionPane.CANCEL_OPTION:
			case JOptionPane.CLOSED_OPTION:
				return false;
			}
		}
		if(getTitle().startsWith("*")) setTitle(getTitle().substring(1));
		return true;
	}

	private void addMenubar() {
		
		menuBar = new JMenuBar();


		fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		fileMenu.getAccessibleContext().setAccessibleDescription("File menu");

		openFile = new JMenuItem("Open file...", KeyEvent.VK_O);
		openFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.ALT_MASK));
		openFile.getAccessibleContext().setAccessibleDescription("Open a file");
		openFile.addActionListener((e) -> {
			
			if(!saveBeforeClose()) {
				return;
			}
			
			/** Read file in EDT */
			String s = readSelectedFile();
			newFileReading = true;
			if(s != null) ta.setText(s);
			ta.setCaretPosition(0);
			newFileReading = false;
			
		});
		saveFile = new JMenuItem("Save file in another encoding...", KeyEvent.VK_S);
		saveFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
		saveFile.getAccessibleContext().setAccessibleDescription("Save file in another encoding");
		saveFile.addActionListener((e) -> {
			
			/** Read file in EDT */
			saveFile();
			
		});
		fileMenu.add(openFile);
		fileMenu.add(saveFile);

		
		
		formatMenu = new JMenu("Setting");
		formatMenu.setMnemonic(KeyEvent.VK_T);
		formatMenu.getAccessibleContext().setAccessibleDescription("Setting menu");
		
		largeSetting = new JMenuItem("Large file handling", KeyEvent.VK_L);
		largeSetting.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
		largeSetting.getAccessibleContext().setAccessibleDescription("Large file handling setting");
		largeSetting.addActionListener((e) -> {
			ReferenceDTO<LargeFileHandlingRule> ref = new ReferenceDTO<>(lfhRule);
			new LargeFileSettingDialog(ref);
			lfhRule = ref.get();
			
			if(paged) reReadPagedFile();
		});
		font = new JMenuItem("Change font", KeyEvent.VK_C);
		font.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));
		font.getAccessibleContext().setAccessibleDescription("Change font type or size");
		font.addActionListener((e) -> {
			ReferenceDTO<Font> ref = new ReferenceDTO<>(ta.getFont());
			new FontDialog(ref, ta.getFont());
			ta.setFont(ref.get());
		});
		editable = new JCheckBoxMenuItem("Editable");
		editable.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.ALT_MASK));
		editable.getAccessibleContext().setAccessibleDescription("Set this file editable in viewer");
		editable.addActionListener((e) -> {
			
			if(paged) {
				JOptionPane.showMessageDialog(null, "Paged file is not editable!", "Try reduce size limit for paged files.", JOptionPane.ERROR_MESSAGE);
			} else {
				ta.setEditable(!ta.isEditable());
			}
			
		});
		formatMenu.add(largeSetting);
		formatMenu.add(font);
		formatMenu.add(editable);
		

		formatMenu.setMnemonic(KeyEvent.VK_P);
		formatMenu.getAccessibleContext().setAccessibleDescription("Pages menu");
		
		next = new JMenuItem("Next page", KeyEvent.VK_N);
		next.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.ALT_MASK));
		next.getAccessibleContext().setAccessibleDescription("Show next page");
		next.addActionListener((e) -> {
			try {
				String s = lfhRule.readOnce(null);
				if(s != null) {
					ta.setText(s);
				} else {
					JOptionPane.showMessageDialog(null, "No more page to read!", "Reached EOF!", JOptionPane.INFORMATION_MESSAGE);
					disableNextPageMenu();
				}
			} catch (IOException e1) {
				JOptionPane.showMessageDialog(null, e1.getMessage(), "unable to read the file!", JOptionPane.ERROR_MESSAGE);
				e1.printStackTrace();
			}
		});
		reRead = new JMenuItem("Restart from begining", KeyEvent.VK_R);
		reRead.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));
		reRead.getAccessibleContext().setAccessibleDescription("Re-read from first page");
		reRead.addActionListener((e) -> {
			reReadPagedFile();
		});
		pageMenu.add(next);
		pageMenu.add(reRead);
		pageMenu.setEnabled(false);
		
		menuBar.add(fileMenu);
		menuBar.add(formatMenu);
		menuBar.add(pageMenu);
		
		setJMenuBar(menuBar);
		
	}
	
	/**
	 *  This method saves content to the file.
	 * 	
	 * 	File may be written in another thread, or in EDT.
	 *  
	 *  @return <code>true</code> if successfully saved. if canceled/failed, <code>false</code>
	 *  
	 *  */
	private boolean saveFile() {
		
		try {
			BufferedWriter bw = selectSaveLocation();
			if(bw == null) return false;
			if(paged) {
				BufferedReader br = new BufferedReader(new FileReader(lastOpened, lastedOpenedCharset));
				br.transferTo(bw);
				br.close();
			} else {
				bw.write(ta.getText());
			}
			bw.close();
			if(getTitle().startsWith("*")) setTitle(getTitle().substring(1));
			return true;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), "unable to save the file!", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return false;
		}
		
	}

	/**
	 *  This method returns content of the file.
	 * 	
	 * 	File may be read in another thread, or in EDT.
	 *  
	 *  */
	private String readSelectedFile() {

		String result = "";
		StringBuilder buff = new StringBuilder();
		
		try {
			BufferedReader br = selectFile();
			if(br == null) return null;
			
			if(paged) {
				result = lfhRule.readOnce(br);
			} else {
				String line = null;
				while((line = br.readLine()) != null) {
					buff.append(line);
					buff.append("\n");
				}
				result = buff.toString();
			}
			br.close();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), "unable to read the file!", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
		
		if(result.equals("")) {
			JOptionPane.showMessageDialog(null, "There's nothing to read!", "File is empty!", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		
		return result;
		
	}

	public BufferedReader selectFile() throws IOException {
		f.setDialogTitle("Select file to read...");
		f.setSelectedFile(lastOpened);
	    f.setCurrentDirectory(lastOpened.getParentFile());
	    if (f.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
	    	return null;
	    
	    lastOpened = f.getSelectedFile();
	    if(lastOpened.length() > lfhRule.getFileSizeLimit()) {
	    	paged = true;
	    	enableNextPageMenu();
	    } else {
	    	paged = false;
	    	disableNextPageMenu();
	    }
	    setTitle(version + " - \"" + f.getSelectedFile().getAbsolutePath() + "\" (" + formatFileSize(f.getSelectedFile().length()) + ((paged) ? ", paged" : "") + ")  in " + f.getSelectedCharset().name());
	    return new BufferedReader(new FileReader(lastOpened, (lastedOpenedCharset = f.getSelectedCharset())));
	}


	public BufferedWriter selectSaveLocation() throws IOException {
		f.setDialogTitle("Save file at...");
		f.setSelectedFile(lastOpened);
		f.setCurrentDirectory(lastSaved.getParentFile());
		if (f.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) 
	    	return null;
		
		lastSaved = f.getSelectedFile();
		if(lastSaved.exists() && JOptionPane.showConfirmDialog(null, "replace file?", lastSaved.getName() + " already exists!", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
			return null;
		}
		
	    return new BufferedWriter(new FileWriter(lastSaved, f.getSelectedCharset()));
	}
	
	private void reReadPagedFile() {
		
		try {
			String s = lfhRule.readOnce(new BufferedReader(new FileReader(lastOpened, lastedOpenedCharset)));
			if(s != null) ta.setText(s);
			else JOptionPane.showMessageDialog(null, "There's nothing to read!", "File is empty!", JOptionPane.ERROR_MESSAGE);
		} catch (IOException e1) {
			JOptionPane.showMessageDialog(null, e1.getMessage(), "unable to read the file!", JOptionPane.ERROR_MESSAGE);
			e1.printStackTrace();
		}
		
	}
	
	private void enableNextPageMenu() {
		pageMenu.setEnabled(true);
	}
	
	private void disableNextPageMenu() {
		pageMenu.setEnabled(false);
	}

	public static String formatFileSize(long length) {
		
		if(length == 0L) return "0.00byte";
		
		switch ((int)(Math.log(length) / Math.log(1024))) {
		
		case 0:
			return String.format("%.2f", length) + "byte";
		case 1:
			return String.format("%.2f", length / 1024.0) + "KB";
		case 2:
			return String.format("%.2f", length / (1024.0 * 1024)) + "MB";
		case 3:
			return String.format("%.2f", length / (1024.0 * 1024 * 1024)) + "GB";
		}
		return String.format("%.2f", length / (1024.0 * 1024 * 1024 * 1024)) + "TB";
	}

}
