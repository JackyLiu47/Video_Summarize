import java.awt.event.KeyEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.awt.event.KeyListener;
import javax.swing.JFrame;

public class videoDisplay extends JFrame implements KeyListener{
    private static Thread t1;
    private static Thread t2;
    private static String vpath;
    private static String apath;

    public videoDisplay(String vpath, String apath){
        this.vpath = vpath;
        this.apath = apath;
    }


    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            System.out.println("space released");
        }
    }
    @Override
    public void keyPressed(KeyEvent e) {

        int key = e.getKeyCode();
        try{
            if (key == KeyEvent.VK_SPACE) {
                t1.wait();
                t2.wait();
            }
        }catch(InterruptedException e1){
            e1.printStackTrace();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    /**
     * Main method for videoPlayback
     * @param args Command line arguments
     */
    public static void main(String[] args) throws PlayWaveException{
        try {
            // get the command line parameters
            videoDisplay display = new videoDisplay(args[0], args[1]);
            display.addKeyListener(display);
            
            String folderPath = vpath;
            String afilename = apath;
            
            // opens the inputStream
            FileInputStream inputStream = new FileInputStream(afilename);

            
            // initializes the playSound and imageReader Objects
            PlaySound audio = new PlaySound(inputStream);
            ImageDisplay playImage = new ImageDisplay(folderPath,audio, afilename);
            t1 = new Thread(audio);
            t2 = new Thread(playImage);

            t1.start();
            t2.start();


        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (PlayWaveException e1) {
            throw new PlayWaveException(e1);
        }
    }
}