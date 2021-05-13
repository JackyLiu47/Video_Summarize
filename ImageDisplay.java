import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.awt.event.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.util.Timer;
import java.util.TimerTask;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
 
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

//TODO: pause function: Add keyboard listening function, use static bool variable in class ImageDisplay to determin if the thread was paused(detect before calling Timertask.run())

/**
* Read the rgb picture in the directory and display the frame by Timer delay and the audio position
* @author Jiaqi Liu
* @author Lisha Xu
*/
public class ImageDisplay implements Runnable{

	JFrame frame;
	JLabel lbIm1;
	BufferedImage imgOne;
	int width = 320;
	int height = 180;
	public static int f_index = 0;
	public static double offset;
	public long prevtime;
	String path;
	PlaySound audio;
	String afileName;
	File audioFile;
	double VFPS;
	private JButton btnPause;
	// private Thread runThread;


	/**
	 * Constructor
	 * @param folderPath: the path of folder that save the original rgb frames
	 * @param audio: the original audio path
	 */
	public ImageDisplay(String folderPath, PlaySound audio, String afileName) throws PlayWaveException{
		this.path = folderPath;
		this.audio = audio;
		this.afileName = afileName;
		this.audioFile = new File(afileName);
		try {
			FileInputStream waveStream = new FileInputStream(afileName);

            //audioInputStream = AudioSystem.getAudioInputStream(this.waveStream);
            
            //add buffer for mark/reset support, modified by Jian
            InputStream bufferedIn = new BufferedInputStream(waveStream);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bufferedIn);
			AudioFormat audioFormat = audioInputStream.getFormat();
			double sampleSize = audioFormat.getFrameSize();
			double Framerate = audioFormat.getFrameRate();
			int time = (int)Math.round(audioFile.length()/(sampleSize*Framerate));
			File folder = new File(folderPath);
			VFPS = folder.listFiles().length/time;
			//System.out.println(time);
        } catch (UnsupportedAudioFileException e1) {
            throw new PlayWaveException(e1);
        } catch (IOException e1) {
            throw new PlayWaveException(e1);
        }
	}

    public void run(){
        this.play();
    }
	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void readImageRGB(int width, int height, String imgPath, BufferedImage img)
	{
		try
		{
			int frameLength = width*height*3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);

			int ind = 0;
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind+height*width];
					byte b = bytes[ind+height*width*2]; 

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					img.setRGB(x,y,pix);
					ind++;
				}
			}
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}	

	/**
	 * Create the JFrame to show the picture
	 */
	public void play(){
		File folder = new File(path);
		File[] imgfiles = folder.listFiles();
		imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		lbIm1 = new JLabel();
		btnPause= new JButton("PLAY/PAUSE");

		GridBagLayout gLayout = new GridBagLayout();
			frame.getContentPane().setLayout(gLayout);
			
			GridBagConstraints c = new GridBagConstraints();
			// frame.getContentPane().setLayout(new BorderLayout(5, 5));
			// frame.getContentPane().add(btnPause, BorderLayout.EAST);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.anchor = GridBagConstraints.CENTER;
			c.weightx = 0.5;
			c.gridx = 0;
			c.gridy = 0;

			c.anchor = GridBagConstraints.NORTHWEST;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.gridx = 0;
			c.gridy = 1;
			gLayout.setConstraints(btnPause, c);
			frame.getContentPane().add(btnPause,c);
			btnPause.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					audio.togglestatus();
				}	
			});
			// Use label to display the image
			
			// lbIm1 = new JLabel(new ImageIcon(imgOne));
			offset = 0;
			prevtime = 0;
			myTimerTask tmtsk = new myTimerTask(path,audio,frame,c,lbIm1);
			//frame.getContentPane().add(lbIm1, c);
			Timer timer = new Timer();
			timer.schedule(tmtsk, 0, (long)(1000.0/VFPS));
			// frame.pack();
			// frame.setVisible(true);

	}
/**
 * To set the time delay and sychronize with the audio
 */
class myTimerTask extends TimerTask{
	
	private int i;
	private String path;
	private GridBagConstraints c;
	private BufferedImage imgOne;
	private	JFrame frame;
	private	JLabel lbIm1;
	public int width = 320;
	public int height = 180;
	PlaySound audio;
	// JFrame frame,JLabel lbIm1,BufferedImage imgOne,String path, GridBagConstraints c, int i
	public myTimerTask(String path, PlaySound audio, JFrame frame, GridBagConstraints c, JLabel lbIm1){
		// this.i = i;
		// this.path = path;
		// this.c = c;
		// this.frame = frame;
		// this.lbIm1 = lbIm1;
		// this.imgOne = imgOne;
		this.path = path;
		this.imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		this.frame = frame;
		//this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.lbIm1 = lbIm1;
		this.c = c;
		this.audio = audio;

	}

	@Override
	public void run(){
		File folder = new File(path);

		//layout
		GridBagLayout gLayout = new GridBagLayout();
	
			frame.getContentPane().setLayout(gLayout);
			
			
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			GridBagConstraints c = new GridBagConstraints();
			//c.fill = GridBagConstraints.HORIZONTAL;
			//c.gridwidth = GridBagConstraints.REMAINDER;
			// c.anchor = GridBagConstraints.NORTHWEST;
			// c.gridwidth = 1;
			// c.gridheight = 1;
			// c.gridx = 0;
			// c.gridy = 0;
			// gLayout.setConstraints(btnPause, c);
			// frame.getContentPane().add(btnPause,c);
			// btnPause.addActionListener(new ActionListener(){
			// 	public void actionPerformed(ActionEvent e) {
			// 		audio.togglestatus();
			// 	}	
			// });
			// c.fill = GridBagConstraints.HORIZONTAL;
			// c.gridx = 0;
			// c.gridy = 1;

		String imagePath = path+"frame"+f_index+".rgb";
		readImageRGB(width, height, imagePath, imgOne);
		ImageIcon imgi = new ImageIcon();
		//Image newimg = imgi.getImage();
		Image newimg2 = imgOne.getScaledInstance(1280, 720,  java.awt.Image.SCALE_SMOOTH);
		imgi = new ImageIcon(newimg2);
		lbIm1.setIcon(imgi);
		frame.getContentPane().add(lbIm1, c);
		
		// lbIm1.setIcon(new ImageIcon(imgOne));

		//System.out.println( Math.round(audio.getTime()*30/1000000));
		System.out.println("Frame"+f_index);
		if(f_index<folder.listFiles().length){
			//offset = (audio.getTime()/1000000)/29.0;
			f_index = (int)((audio.getTime()*VFPS/1000000)+offset);
			if(prevtime != audio.getTime()){
				//offset+=10.0/272;
			}
			prevtime = audio.getTime();
			if(f_index>=folder.listFiles().length){
				f_index = folder.listFiles().length-1;
			}
		}
		else f_index = folder.listFiles().length-1;
		//offset+=1.0/30;
		//f_index++;
		frame.pack();
		frame.setVisible(true);


        // Obtain the information about the AudioInputStream

	}
}	
}
