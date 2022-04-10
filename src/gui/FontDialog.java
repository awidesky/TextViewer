package gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class FontDialog extends JDialog {

	private static final long serialVersionUID = -737932341920503748L;
	
	private JLabel name = new JLabel("Name :");
	private JLabel size = new JLabel("Size :");
	private JLabel pt = new JLabel("pt");
	private JComboBox<String> fontName = new JComboBox<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
	private JCheckBox bold;
	private JCheckBox italic;
	private JTextField fontSize;
	private JButton done = new JButton("Done");
	
	private int fontStyle = Font.PLAIN;
	
	public FontDialog(AtomicReference<Font> ref, Font now) {
		
		super((Window)null);
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(dim.width/2-getSize().width/2, dim.height/2-getSize().height/2);
		setModal(true);
		setTitle("Change font...");
		setSize(350, 150);
		setLayout(null);
		
		fontName.setSelectedItem(now.getFamily());
		bold = new JCheckBox("bold", now.isBold());
		italic = new JCheckBox("italic", now.isItalic());
		fontSize = new JTextField("" + now.getSize()); //default font size
		
		name.setBounds(14, 10, name.getPreferredSize().width, name.getPreferredSize().height);
		size.setBounds(265, 14, size.getPreferredSize().width, size.getPreferredSize().height);
		pt.setBounds(305, 39, pt.getPreferredSize().width, pt.getPreferredSize().height);
		
		fontName.setBounds(14, 35, 175, 22);
		
		bold.setBounds(200, 10, bold.getPreferredSize().width, bold.getPreferredSize().height);
		bold.addItemListener((e) -> {
			if(e.getStateChange() == ItemEvent.SELECTED) {
				if(fontStyle == Font.PLAIN) fontStyle = Font.BOLD;
				else fontStyle |= Font.BOLD;
			}
		});
		italic.setBounds(200, 35, italic.getPreferredSize().width, italic.getPreferredSize().height);
		italic.addItemListener((e) -> {
			if(e.getStateChange() == ItemEvent.SELECTED) {
				if(fontStyle == Font.PLAIN) fontStyle = Font.ITALIC;
				else fontStyle |= Font.ITALIC;
			}
		});
		
		fontSize.setBounds(265, 35, 37, 22);
		
		done.setBounds(144, 70, done.getPreferredSize().width, done.getPreferredSize().height);
		done.addActionListener((e) -> {
			ref.set(new Font(fontName.getSelectedItem().toString(), fontStyle, Integer.parseInt(fontSize.getText())));
			setVisible(false);
			dispose();
		});
		
		
		add(name);
		add(size);
		add(pt);
		add(fontName);
		add(bold);
		add(italic);
		add(fontSize);
		add(done);
		
		//addWindowListener(null);
		setVisible(true);
		
	}
}
