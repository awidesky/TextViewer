package gui;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.stream.Collectors;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import io.TextFile;

public class TextFilechooser extends JFileChooser {
	
	private static final long serialVersionUID = 6120225700667641868L;
	
	//private final ArrayList<Entry<String, Charset>> charsetList = Charset.availableCharsets().entrySet().stream().collect(Collectors.toCollection(ArrayList::new));
	public static final ArrayList<String> charsetNameList = Charset.availableCharsets().keySet().stream().collect(Collectors.toCollection(ArrayList::new));
	public static final String defaultCharset = "UTF-8";
	private static final JComboBox<String> comboBox = getCharsetChooseComboBox();
	
	public static final FileFilter TEXTFILEFILTER = new FileFilter() {
		public boolean accept(File f) {
			if (f.isDirectory()	|| f.getName().endsWith(".txt"))
				return true;
			else
				return false;
		}
		public String getDescription() {
			return "Text files (*.txt)";
		}
	};
	
	public TextFilechooser() {

		comboBox.setSelectedIndex(charsetNameList.indexOf(defaultCharset));

		setMultiSelectionEnabled(false);
		setFileSelectionMode(JFileChooser.FILES_ONLY);
		addChoosableFileFilter(TEXTFILEFILTER);

		JPanel panel2 = (JPanel) ((JPanel) this.getComponent(3)).getComponent(3);

		JButton b1 = (JButton) panel2.getComponent(0);// optional used to add the buttons after combobox
		JButton b2 = (JButton) panel2.getComponent(1);// optional used to add the buttons after combobox

		// Dimension d1 = new Dimension(b1.getPreferredSize().width,
		// b1.getPreferredSize().height);
		// Dimension d2 = new Dimension(b2.getPreferredSize().width,
		// b2.getPreferredSize().height);

		// b1.setMaximumSize(new Dimension(20,20));
		// b2.setMaximumSize(d2);

		panel2.removeAll();

		panel2.add(comboBox);
		panel2.add(b1);// optional used to add the buttons after combobox
		panel2.add(b2);// optional used to add the buttons after combobox

		// panel2.pack();

		/*
		 * JPanel panel = new JPanel(); panel.add(comboBox); setAccessory(panel);
		 */

	}
	
	public void setLastOpendFile(TextFile lastOpened) {
		setSelectedFile(lastOpened.file.getParentFile());
		comboBox.setSelectedIndex(charsetNameList.indexOf(lastOpened.encoding.name()));
	}
	

	public Charset getSelectedCharset() {
    	comboBox.setSelectedIndex(comboBox.getSelectedIndex());
    	return Charset.forName(charsetNameList.get(comboBox.getSelectedIndex()));
    }
	
	public static JComboBox<String> getCharsetChooseComboBox() {
		return new JComboBox<>(new DefaultComboBoxModel<String>(charsetNameList.toArray(new String[] {})));
	}
}