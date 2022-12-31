package gui;

import static main.Main.logger;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicBoolean;

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
import main.Page;
import main.ReferenceDTO;
import main.SelectedFileHandler;

public class MainFrame extends JFrame {

	private static final long serialVersionUID = 4096788492544078810L;
	
	private JScrollPane sp;
	private JTextArea ta = new JTextArea();
	private UndoManager undoManager = new UndoManager();;
	private File lastOpened = new File(System.getProperty("user.home"));
	private File lastSaved = new File(System.getProperty("user.home"));
	
	private TextFilechooser fileChooser = new TextFilechooser();
	
	private BlockingQueue<Page> fileContentQueue = null;
	
	private SelectedFileHandler fileHandle = null;

	private JMenuBar menuBar;
	private JMenu fileMenu;
	private JMenuItem openFile;
	private JMenuItem saveFile;
	private JMenu editMenu;
	private JMenuItem undo;
	private JMenuItem redo;
	private JMenu formatMenu;
	private JMenuItem bufSetting;
	private JMenuItem font;
	private JCheckBoxMenuItem editable;
	private JMenu pageMenu;
	private JMenuItem next;
	private JMenuItem reRead;

	/** <code>true</code> if new content is being written in TextArea */
	private AtomicBoolean newPageReading = new AtomicBoolean(false);
	
	
	public MainFrame() {
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			SwingDialogs.error("Error while setting window look&feel", "%e%", e, false);
			e.printStackTrace();
		}
		
		TitleGeneartor.titleConsumer(this::setTitle);	
		
		setTitle(Main.VERSION);
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
  	        	if(newPageReading.get()) { //new file is just read. user didn't type anything.
	        		return;	
	        	}
  	        	
  	        	undo.setEnabled(undoManager.canUndo());
  				redo.setEnabled(undoManager.canRedo());
	        	if(!getTitle().startsWith("*")) {
	        		logger.log("File Edited! : " + e.getType().toString());
	        		TitleGeneartor.edited(true);
	        	}
	        }
	    });
		new DropTarget(ta, new DropTargetListener(){
            public void dragEnter(DropTargetDragEvent e) {}
            
            public void dragExit(DropTargetEvent e) {}
            
            public void dragOver(DropTargetDragEvent e){}
            
            public void dropActionChanged(DropTargetDragEvent e){}
            
			public void drop(DropTargetDropEvent e) {
				File dropped = null;
				try {
					Transferable tr = e.getTransferable();
					DataFlavor[] flavors = tr.getTransferDataFlavors();

					if (flavors[0].isFlavorJavaFileListType()) {
						
						e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

						@SuppressWarnings("unchecked")
						List<File> list = (List<File>) tr.getTransferData(flavors[0]);

						if(list.size() > 1) {
							SwingDialogs.error("Drag & Drop error!", "Do not drag & drop more than one file!", null, false);
							return;
						}
						 
						dropped = list.get(0);
					}
				} catch (Exception ex) {
					SwingDialogs.error("Drag & Drop error!", "%e%", ex, false);
				}
				openFile(dropped);
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
			
			//open chosen file
			openFile(null);
			saveFile.setEnabled(true);
			
		});
		saveFile = new JMenuItem("Save file in another encoding...", KeyEvent.VK_S);
		saveFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
		saveFile.getAccessibleContext().setAccessibleDescription("Save file in another encoding");
		saveFile.addActionListener((e) -> {
			
			/** Read file in EDT */
			saveFile();
			
		});
		saveFile.setEnabled(false);
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
			if(!undoManager.canUndo()) TitleGeneartor.edited(false);
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
		editMenu.setEnabled(false);
		
		
		
		formatMenu = new JMenu("Setting");
		formatMenu.setMnemonic(KeyEvent.VK_T);
		formatMenu.getAccessibleContext().setAccessibleDescription("Setting menu");
		
		bufSetting = new JMenuItem("Setting", KeyEvent.VK_B);
		bufSetting.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.ALT_MASK));
		bufSetting.getAccessibleContext().setAccessibleDescription("Buffer size setting");
		bufSetting.addActionListener((e) -> {
			new SettingDialog(Main.setting);
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
			
			saveFile.setEnabled(true);
			editable(!ta.isEditable());
			
		});
		formatMenu.add(bufSetting);
		formatMenu.add(font);
		formatMenu.add(editable);
		

		pageMenu = new JMenu("Pages");
		pageMenu.setMnemonic(KeyEvent.VK_P);
		pageMenu.getAccessibleContext().setAccessibleDescription("Pages menu");
		
		next = new JMenuItem("Next page", KeyEvent.VK_N);
		next.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.ALT_MASK));
		next.getAccessibleContext().setAccessibleDescription("Show next page");
		next.addActionListener((e) -> {
			nextPage();
		});
		reRead = new JMenuItem("Restart from begining", KeyEvent.VK_R);
		reRead.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));
		reRead.getAccessibleContext().setAccessibleDescription("Re-read from first page");
		reRead.addActionListener((e) -> {
			makeNewQueue();
			fileHandle.reRead(fileContentQueue);
			nextPage();
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
	
	private void makeNewQueue() {
		if(Main.setting.loadedPagesNumber < 3) {
			fileContentQueue = new SynchronousQueue<>();
		} else {
			fileContentQueue = new LinkedBlockingQueue<>(Main.setting.loadedPagesNumber - 1);
		}
	}
	
	private void editable(boolean flag) {
		ta.setEditable(flag);
		editMenu.setEnabled(flag);
	}
	
	private void openFile(File file) {

		if(!saveBeforeClose()) {
			return;
		}
		
		if (file != null) {
			lastOpened = file;
		} else {
			fileChooser.setDialogTitle("Select file to read...");
			fileChooser.setCurrentDirectory(lastOpened.getParentFile());
			fileChooser.getActionMap().get("viewTypeDetails").actionPerformed(null);
			if (fileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
				return;

			lastOpened = fileChooser.getSelectedFile();
		}
		
		if(fileHandle != null) fileHandle.close();
		fileHandle = new SelectedFileHandler(lastOpened, fileChooser.getSelectedCharset());
		makeNewQueue();
		
		if(fileHandle.isPaged()) {
			enableNextPageMenu();
			bufSetting.setEnabled(false);
		} else {
			disableNextPageMenu();
			bufSetting.setEnabled(true);
		}
		TitleGeneartor.reset(lastOpened.getAbsolutePath(), Main.formatFileSize(lastOpened.length()), fileHandle.isPaged(), fileChooser.getSelectedCharset().name(), false, true, 1L);
		
		fileHandle.startNewRead(fileContentQueue);
		displyNewPage();
		
	}
	
	/**
	 *  This method saves content to the file.
	 * 	
	 * 	File is written in EDT.
	 *  
	 *  @return <code>true</code> if successfully saved. if canceled/failed, <code>false</code>
	 *  
	 *  */
	private boolean saveFile() {
		
		fileChooser.setDialogTitle("Save file at...");
		fileChooser.setSelectedFile(lastOpened);
		fileChooser.setCurrentDirectory(lastSaved.getParentFile());
		if (fileChooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) 
	    	return false;
		
		lastSaved = fileChooser.getSelectedFile();
		if(fileChooser.getFileFilter().equals(TextFilechooser.TEXTFILEFILTER) && !lastSaved.getName().endsWith(".txt")) {
		    lastSaved = new File(lastSaved.getParentFile(), lastSaved.getName() + ".txt");
		}
		
		if(lastSaved.exists() && JOptionPane.showConfirmDialog(null, "replace file?", lastSaved.getName() + " already exists!", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
			return false;
		}
		
		TitleGeneartor.edited(false);
		if(fileHandle == null) fileHandle = new SelectedFileHandler();
		boolean ret = fileHandle.write(lastSaved, fileChooser.getSelectedCharset(), ta.getText());
		TitleGeneartor.reset(lastSaved.getAbsolutePath(), Main.formatFileSize(lastSaved.length()), false, fileChooser.getSelectedCharset().name(), false, false, 1L);
		return ret;
			
	}
	
	
	private void nextPage() {
		
		if (!pageMenu.isEnabled()) return;
		displyNewPage();
		
	}

	private void displyNewPage() {
		
		Page content = null;
		TitleGeneartor.loading(true);
		boolean originVal = ta.isEditable();
		editable(false);
		newPageReading.set(true);
		try {
			/**
			 * only one page can be loaded in memory, so <code>fileHandle</code> is waiting, now we wake it up.
			 * */
			if(fileHandle.isPaged() && fileHandle.getLoadedPagesNumber() == 1) {
				ta.setText(null);
				synchronized (fileHandle) {
					fileHandle.notify();
				}
			}
			content = fileContentQueue.take();
		} catch (InterruptedException e1) {
			SwingDialogs.error("interrupted while loading this page!!", "%e%", e1, false);
			content = new Page("", -1);
		}


		if(fileHandle.isPaged()) {
			Main.logger.log("[" + Thread.currentThread().getName() + "(" + Thread.currentThread().getId() + ")] page #" + content.pageNum + " is consumed and displayed");
			TitleGeneartor.pageNum(content.pageNum);
		}
		
		if (content != null) {
			ta.setText(content.text);
			ta.setCaretPosition(0);
			sp.getVerticalScrollBar().setValue(0);
			undoManager.discardAllEdits();
			TitleGeneartor.loading(false);
			editable(originVal);
		} else {
			disableNextPageMenu();
			bufSetting.setEnabled(true);
		}
		newPageReading.set(false);
		
	}

	private void enableNextPageMenu() {
		pageMenu.setEnabled(true);
	}
	
	private void disableNextPageMenu() {
		pageMenu.setEnabled(false);
	}


}
