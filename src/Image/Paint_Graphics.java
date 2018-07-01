package Image;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class Paint_Graphics  extends JFrame{
	
	
	public static void paint(File[] pathGraphic) {
		int index = 0;
		while(index < pathGraphic.length) {
			Paint_Graphics Graph = new Paint_Graphics(pathGraphic[index].getAbsolutePath());
			int reply1 =JOptionPane.showConfirmDialog(null,"Do you want to see next graph? ("+(index+1)+" of "+pathGraphic.length+")", "Attention", JOptionPane.YES_NO_OPTION);
			if(reply1==JOptionPane.YES_OPTION) {
				Graph.setVisible(false);
				index++;
				continue;
			}else {
				Graph.setVisible(false);
				break;
			}
		}
	}
	
	public Paint_Graphics(String pathGraphic) {
		//super("Test Panel");
		Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
		this.setSize(screenResolution.width, screenResolution.height);

		try { 
			File file = new File(pathGraphic); 
			BufferedImage image = ImageIO.read(file);
			JLabel label = new JLabel(new ImageIcon(image));
			JScrollPane pane = new JScrollPane(label);
			this.getContentPane().add(pane, BorderLayout.CENTER);
		}catch (Exception e) {
			this.getContentPane().add(new JTextArea(e.getMessage()));
		}

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE );
		this.setVisible(true);
	}

}
