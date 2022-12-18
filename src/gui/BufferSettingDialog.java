package gui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

import main.Main;
import main.ReferenceDTO;

public class BufferSettingDialog extends JDialog {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7072653495837828247L;
	private JLabel label = new JLabel("Prefered Buffer size :");
	private JTextField tf = new JTextField();
	private JComboBox<String> cb = new JComboBox<String>(new String[] { "byte", "KB", "MB", "GB" });
	private JButton done = new JButton("done");
	
	public BufferSettingDialog(ReferenceDTO<Integer> ref) {
		
		super((Window)null);
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setModal(true);
		setTitle("Buffer size setting...");
		setSize(250, 100);
		setLocation(dim.width/2-getSize().width/2, dim.height/2-getSize().height/2);
		setResizable(false);
		setLayout(null);
		
		label.setBounds(5, 8, label.getPreferredSize().width, label.getPreferredSize().height);
		
		tf.setBounds(8 + label.getPreferredSize().width, 5, 50, tf.getPreferredSize().height);
		tf.setText("" + ref.get());
		cb.setSelectedIndex(0);
		cb.setBounds(61 + label.getPreferredSize().width, 5, cb.getPreferredSize().width, 22);
		
		done.setBounds(getSize().width/2 - done.getPreferredSize().width/2, 15 + label.getPreferredSize().height, done.getPreferredSize().width, done.getPreferredSize().height);
		done.addActionListener((e) -> {
			try {
				ref.set(Main.getByteSize(tf.getText() + cb.getSelectedItem()));
			} catch (NumberFormatException err) {
				SwingDialogs.error("Invalid Buffer size!", "%e%", err, false);
				return;
			}
			setVisible(false);
			dispose();
		});
		
		add(label);
		add(tf);
		add(cb);
		add(done);
		
		setVisible(true);
		
		
		
	}
	
}
