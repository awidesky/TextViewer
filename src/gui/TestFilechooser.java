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

public class TestFilechooser extends JFileChooser{
	
	//private final ArrayList<Entry<String, Charset>> charsetList = Charset.availableCharsets().entrySet().stream().collect(Collectors.toCollection(ArrayList::new));
	private final ArrayList<String> charsetNameList = Charset.availableCharsets().keySet().stream().collect(Collectors.toCollection(ArrayList::new));
	private final JComboBox<String> comboBox = new JComboBox<>(new DefaultComboBoxModel<String>(charsetNameList.toArray(new String[] {})));
	
    public TestFilechooser() {
        
    	comboBox.setSelectedIndex(charsetNameList.indexOf("UTF-8"));
    	
    	setMultiSelectionEnabled(false);
    	setFileSelectionMode(FILES_ONLY);
    	addChoosableFileFilter(new FileFilter() {
			public boolean accept(File f) {
				if (f.isDirectory()	|| f.getName().endsWith(".txt"))
					return true;
				else
					return false;
			}
			public String getDescription() {
				return "Text files (*.txt)";
			}
		});

        JPanel panel2 = (JPanel) ((JPanel) this.getComponent(3)).getComponent(3);

        JButton b1 = (JButton)panel2.getComponent(0);//optional used to add the buttons after combobox
        JButton b2 = (JButton)panel2.getComponent(1);//optional used to add the buttons after combobox
        
        
        //Dimension d1 = new Dimension(b1.getPreferredSize().width, b1.getPreferredSize().height);
        //Dimension d2 = new Dimension(b2.getPreferredSize().width, b2.getPreferredSize().height);
        

        
        //b1.setMaximumSize(new Dimension(20,20));
        //b2.setMaximumSize(d2);
        
        panel2.removeAll();
        
        panel2.add(comboBox);
        panel2.add(b1);//optional used to add the buttons after combobox
        panel2.add(b2);//optional used to add the buttons after combobox
        
        //panel2.pack();
        
        /*
        JPanel panel = new JPanel();
        panel.add(comboBox);
        setAccessory(panel);
        */
        
   }
    
    public Charset getSelectedCharset() {
    	return Charset.forName(charsetNameList.get(comboBox.getSelectedIndex()));
    }
}