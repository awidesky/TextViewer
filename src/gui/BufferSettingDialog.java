package gui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

import main.ReferenceDTO;

public class BufferSettingDialog extends JDialog {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4298961566242537824L;
	private JLabel label1 = new JLabel("Prefered Buffer size :");
	private JTextField tf1 = new JTextField();
	private JLabel label2 = new JLabel("Characters per page :");
	private JTextField tf2 = new JTextField();
	private JLabel chars = new JLabel("char(s)");
	private JButton done = new JButton("done");
	
	public BufferSettingDialog(ReferenceDTO<Integer> bufSize, ReferenceDTO<Integer> charPerPage) {
		
		super((Window)null);
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setModal(true);
		setTitle("Buffer size setting");
		setSize(250, 130);
		setLocation(dim.width/2-getSize().width/2, dim.height/2-getSize().height/2);
		setResizable(false);
		setLayout(null);
		
		label1.setBounds(5, 8, label1.getPreferredSize().width, label1.getPreferredSize().height);
		label2.setBounds(5, 38, label2.getPreferredSize().width, label2.getPreferredSize().height);
		
		tf1.setBounds(8 + label1.getPreferredSize().width, 5, 60, tf1.getPreferredSize().height);
		tf1.setText("" + bufSize.get());
		chars.setBounds(70 + label1.getPreferredSize().width, 5, chars.getPreferredSize().width, 22);
		
		tf2.setBounds(8 + label2.getPreferredSize().width, 35, 80, tf2.getPreferredSize().height);
		tf2.setText("" + charPerPage.get());
		
		done.setBounds(getSize().width/2 - done.getPreferredSize().width/2 - 10, 25 + label2.getY(), done.getPreferredSize().width, done.getPreferredSize().height);
		done.addActionListener((e) -> {
			try { //TODO : no negatives
				bufSize.set(Integer.valueOf(tf1.getText()));
				charPerPage.set(Integer.valueOf(tf2.getText()));
			} catch (NumberFormatException err) {
				SwingDialogs.error("Invalid number!", "%e%", err, false);
				return;
			}
			setVisible(false);
			dispose();
		});
		
		add(label1);
		add(label2);
		add(tf1);
		add(tf2);
		add(chars);
		add(done);
		
		setVisible(true);
		
	}
	
}
