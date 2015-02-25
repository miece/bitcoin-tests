import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class TextAreaFrame extends JFrame implements ActionListener {
	
	
  static CreateAddress cr = new CreateAddress();
  static CreateWallet cw = new CreateWallet();

  static Trans tr = new Trans();
  
  private JButton CreateAddress = new JButton("Create Address");

  private JButton CreateWallet = new JButton("Create Wallet");

  private JButton GetGenesisBlock = new JButton("Get Genesis Block");
  
  private JButton Transaction = new JButton("Test Transaction");

  private JTextArea textArea = new JTextArea(8, 40);

  private JScrollPane scrollPane = new JScrollPane(textArea);

  public TextAreaFrame() {
    JPanel p = new JPanel();

    p.add(CreateAddress);
    CreateAddress.addActionListener(this);

    p.add(CreateWallet);
    CreateWallet.addActionListener(this);

    p.add(GetGenesisBlock);
    GetGenesisBlock.addActionListener(this);
    
    p.add(Transaction);
    Transaction.addActionListener(this);

    getContentPane().add(p, "South");

    getContentPane().add(scrollPane, "Center");

    setTitle("TextAreaTest");
    setSize(800, 600);
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });
  }

  public void actionPerformed(ActionEvent evt) {
    Object source = evt.getSource();
    if (source == CreateAddress)
    {
    	textArea.setText("");
      //textArea
      //    .append("The quick brown fox jumps over the lazy dog. The quick brown fox jumps over the lazy dog.");
    	textArea.setText(cr.show());
    	textArea.setLineWrap(true);
    }
    else if (source == CreateWallet) {
      textArea.setLineWrap(true);
      textArea.setText(cw.show());
      scrollPane.validate();
    } else if (source == GetGenesisBlock) {
    	textArea.setLineWrap(true);
        textArea.setText("Not Working. Should display genesis block information");
        scrollPane.validate();
    }
    else if (source == Transaction) {
    	textArea.setLineWrap(true);
        try {
			textArea.setText(tr.show());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        scrollPane.validate();
      }
  }

  public static void main(String[] args) {
    JFrame f = new TextAreaFrame();
    
    f.show();
  }

}
