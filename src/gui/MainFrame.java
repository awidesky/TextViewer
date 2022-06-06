package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;

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
import javax.swing.undo.UndoManager;

import main.Main;
import main.ReferenceDTO;
import main.SelectedFileHandler;
import static main.Main.logger;

public class MainFrame extends JFrame {

	private static final long serialVersionUID = 4096788492544078810L;
	
	private JScrollPane sp;
	private JTextArea ta = new JTextArea();
	private UndoManager undoManager = new UndoManager();;
	private File lastOpened = new File(System.getProperty("user.home"));
	private File lastSaved = new File(System.getProperty("user.home"));
	private String version = "TextViewer v1.0";
	
	private TestFilechooser f = new TestFilechooser();
	
	private ArrayBlockingQueue<String> textQueue = new ArrayBlockingQueue<>(1);
	
	private SelectedFileHandler fileHandle;
	private JMenu pageMenu = new JMenu("Pages");
	private JMenuBar menuBar;
	private JMenu fileMenu;
	private JMenuItem openFile;
	private JMenuItem saveFile;
	private JMenu editMenu;
	private JMenuItem undo;
	private JMenuItem redo;
	private JMenu formatMenu;
	private JMenuItem largeSetting;
	private JMenuItem font;
	private JCheckBoxMenuItem editable;
	private JMenuItem next;
	private JMenuItem reRead;

	/** <code>true</code> if new content is being written in TextArea */
	private boolean newPageReading = false;
	
	
	public MainFrame() {
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			SwingDialogs.error("Error while setting window look&feel", "%e%", e, false);
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
				Main.kill(0);

			}

		});
		
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(dim.width/2-getSize().width/2, dim.height/2-getSize().height/2);
		
		
		addMenubar();
		
		ta.setBackground(Color.LIGHT_GRAY);
		ta.setEditable(false);
		ta.getDocument().addUndoableEditListener(undoManager);
		ta.getDocument().addDocumentListener(new DocumentListener() {

	        @Override
	        public void removeUpdate(DocumentEvent e) {
	        	changed(e);
	        }

	        @Override
	        public void insertUpdate(DocumentEvent e) {
	        	changed(e);
	        }

	        @Override
	        public void changedUpdate(DocumentEvent arg0) {
	        	changed(arg0);
	        }
	        
	        private void changed(DocumentEvent e) {
  	        	if(newPageReading) { //new file is just read. user didn't type anything.
	        		return;	
	        	}
  	        	
  	        	undo.setEnabled(undoManager.canUndo());
  				redo.setEnabled(undoManager.canRedo());
	        	if(!getTitle().startsWith("*")) {
	        		logger.log("File Edited!");
	        		setTitle("*" + getTitle());
	        	}
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
			
			try {
				readSelectedFile();
			} catch (InterruptedException excep) {
				SwingDialogs.error("Cannot read seleceted file!", "Thread Iterrupted when reading : %e%", excep, true);
			}
			
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

		
		
		editMenu = new JMenu("Edit");
		editMenu.getAccessibleContext().setAccessibleDescription("Edit menu");
		
		undo = new JMenuItem("Undo", KeyEvent.VK_Z);
		undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
		undo.getAccessibleContext().setAccessibleDescription("Undo");
		undo.addActionListener((e) -> {
			undoManager.undo();
			undo.setEnabled(undoManager.canUndo());
			redo.setEnabled(undoManager.canRedo());
			if(!undoManager.canUndo() && getTitle().startsWith("*")) setTitle(getTitle().substring(1));
		});
		undo.setEnabled(false);
		redo = new JMenuItem("Redo", KeyEvent.VK_Y);
		redo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, ActionEvent.CTRL_MASK));
		redo.getAccessibleContext().setAccessibleDescription("Undo");
		redo.addActionListener((e) -> {
			undoManager.redo();
			undo.setEnabled(undoManager.canUndo());
			redo.setEnabled(undoManager.canRedo());
		});
		redo.setEnabled(false);
		editMenu.add(undo);
		editMenu.add(redo);
		
		
		
		formatMenu = new JMenu("Setting");
		formatMenu.setMnemonic(KeyEvent.VK_T);
		formatMenu.getAccessibleContext().setAccessibleDescription("Setting menu");
		
		largeSetting = new JMenuItem("Large file handling", KeyEvent.VK_L);
		largeSetting.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
		largeSetting.getAccessibleContext().setAccessibleDescription("Large file handling setting");
		largeSetting.addActionListener((e) -> {
			
			new LargeFileSettingDialog();

		});
		font = new JMenuItem("Change font", KeyEvent.VK_C);
		font.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));
		font.getAccessibleContext().setAccessibleDescription("Change font type or size");
		font.addActionListener((e) -> {
			ReferenceDTO<Font> ref = new ReferenceDTO<>(ta.getFont());
			new FontDialog(ref, ta.getFont(), ta.getText());
			ta.setFont(ref.get());
		});
		editable = new JCheckBoxMenuItem("Editable");
		editable.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.ALT_MASK));
		editable.getAccessibleContext().setAccessibleDescription("Set this file editable in viewer");
		editable.addActionListener((e) -> {
			
			ta.setEditable(!ta.isEditable());
			
		});
		formatMenu.add(largeSetting);
		formatMenu.add(font);
		formatMenu.add(editable);	
		

		pageMenu.setMnemonic(KeyEvent.VK_P);
		pageMenu.getAccessibleContext().setAccessibleDescription("Pages menu");
		
		next = new JMenuItem("Next page", KeyEvent.VK_N);
		next.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.ALT_MASK));
		next.getAccessibleContext().setAccessibleDescription("Show next page");
		next.addActionListener((e) -> {
			String s = textQueue.poll();
			if (s.equals("")) {
				ta.setText(s);
				undoManager.discardAllEdits();
			} else {
				JOptionPane.showMessageDialog(null, "No more page to read!", "Reached EOF!",
						JOptionPane.INFORMATION_MESSAGE);
				disableNextPageMenu();
			}
		});
		reRead = new JMenuItem("Restart from begining", KeyEvent.VK_R);
		reRead.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));
		reRead.getAccessibleContext().setAccessibleDescription("Re-read from first page");
		reRead.addActionListener((e) -> {
			textQueue = new ArrayBlockingQueue<>(1);
			fileHandle.reRead(textQueue);
		});
		pageMenu.add(next);
		pageMenu.add(reRead);
		pageMenu.setEnabled(false);
		
		menuBar.add(fileMenu);
		menuBar.add(editMenu);
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
		
		f.setDialogTitle("Save file at...");
		f.setSelectedFile(lastOpened);
		f.setCurrentDirectory(lastSaved.getParentFile());
		if (f.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) 
	    	return false;
		
		lastSaved = f.getSelectedFile();
		if(lastSaved.exists() && JOptionPane.showConfirmDialog(null, "replace file?", lastSaved.getName() + " already exists!", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
			return false;
		}
		
		if(getTitle().startsWith("*")) setTitle(getTitle().substring(1));
		return fileHandle.write(lastSaved, f.getSelectedCharset(), ta.getText());
			
	}
	

	
	
	/**
	 * 	
	 * 	File may be read in another thread, but EDT waits for the Thread to offer first String.
	 * @throws InterruptedException when <code>textQueue.take()</code> interrupted.
	 *  
	 *  */
	private void readSelectedFile() throws InterruptedException { //TODO : drag & drop open? 

		if(!selectFile()) return;
		
		fileHandle.startRead(textQueue);
		
		newPageReading = true;
		ta.setText(textQueue.take());
		ta.setCaretPosition(0);
		newPageReading = false;
		
	}

	public boolean selectFile() {
		f.setDialogTitle("Select file to read...");
		f.setSelectedFile(lastOpened);
	    f.setCurrentDirectory(lastOpened.getParentFile());
	    f.getActionMap().get("viewTypeDetails").actionPerformed(null);
	    if (f.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
	    	return false;

	    lastOpened = f.getSelectedFile();
	    
	    fileHandle = new SelectedFileHandler(lastOpened, f.getSelectedCharset());
	    
	    largeSetting.setEnabled(!fileHandle.isPaged());
	    if(fileHandle.isPaged()) {
	    	enableNextPageMenu();
	    } else {
	    	disableNextPageMenu();
	    }
	    setTitle(version + " - \"" + f.getSelectedFile().getAbsolutePath() + "\" (" + formatFileSize(f.getSelectedFile().length()) + (fileHandle.isPaged() ? ", paged" : "") + ")  in " + f.getSelectedCharset().name());
	    return true;
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
			return String.format("%d", length) + "byte";
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
