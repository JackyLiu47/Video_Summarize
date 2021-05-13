import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Set;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.DataLine.Info;

/**
* This audioAnalyzer class helps calculate the audio weight of each shots by the audio strength
* @author Jiaqi Liu
* @author Lisha Xu
*/
public class audioAnalyze{

    private InputStream waveStream;
    private SourceDataLine dataLine;
    private ArrayList<Integer> breaks;
    private final int EXTERNAL_BUFFER_SIZE = 524288; // 128Kb
    private double sampleSize;
    private AudioInputStream audioInputStream;
    private AudioFormat audioFormat;
    private double VFPS;
    private double bytesperVframe;
    
    /**
     * CONSTRUCTOR
     * @param waveStream: Audio Input stream
     * @param breaks: the arraylist that saved shots break position
     */
    public audioAnalyze(InputStream waveStream, ArrayList<Integer> breaks, double VFPS) throws PlayWaveException {
	    this.waveStream = waveStream;
        this.breaks = breaks;
        this.audioInputStream = null;
        this.VFPS = VFPS;
        try {
            //audioInputStream = AudioSystem.getAudioInputStream(this.waveStream);
            
            //add buffer for mark/reset support, modified by Jian
            InputStream bufferedIn = new BufferedInputStream(this.waveStream);
            this.audioInputStream = AudioSystem.getAudioInputStream(bufferedIn);
            
        } catch (UnsupportedAudioFileException e1) {
            throw new PlayWaveException(e1);
        } catch (IOException e1) {
            throw new PlayWaveException(e1);
        }
        // Obtain the information about the AudioInputStream
        audioFormat = audioInputStream.getFormat();
        sampleSize = audioFormat.getFrameSize();
        double Framerate = audioFormat.getFrameRate();
        bytesperVframe = sampleSize*Framerate/VFPS;
    }

    /**
     * Calculate the audio weight of each shot by calculate the avg strength / max strength in that shot
     * This could get the relative weights of this shot(without influence by the higher or lower value in other shots)
     */
    public ArrayList<Double> audioWeight()  throws PlayWaveException{
        System.out.println("Start to analyze audio");
        int readBytes = 0;
        //byte[] audioBuffer = new byte[this.EXTERNAL_BUFFER_SIZE];
        //byte[] prevBuffer = new byte[this.EXTERNAL_BUFFER_SIZE];
        ArrayList<Double> audioweight = new ArrayList<>();
        try {
            for(int i = 0; i < breaks.size()-1;i++){
                int shotlen = (breaks.get(i+1)-breaks.get(i))*(int)bytesperVframe;
                byte[] audioBuffer = new byte[shotlen];
                audioInputStream.read(audioBuffer, 0, audioBuffer.length);
                double avg = 0;
                double maxValue = Double.NEGATIVE_INFINITY;
                double minValue = Double.POSITIVE_INFINITY;
                for(int j = 0; j < audioBuffer.length; j+=2){
                    avg+=audioBuffer[(int)j];
                    if(audioBuffer[(int)j] > maxValue)
                        maxValue = audioBuffer[(int)j];
                    if(audioBuffer[(int)j] < minValue)
                        minValue = audioBuffer[(int)j];
                }
                if((maxValue + Math.abs(minValue))!=0){
                    avg += Math.abs(minValue) * (audioBuffer.length/2);
                    avg/=(audioBuffer.length/2);
                    avg/=(maxValue + Math.abs(minValue));
                }
                else{
                    avg = 0;
                }
                // avg /= maxValue;
                audioweight.add(avg);
                if(i==290){
                    System.out.println("max:"+maxValue+" min:"+minValue+" length:"+audioBuffer.length);
                    //System.out.println("shot index:"+i+" avg:"+ avg+" div1:"+(audioBuffer.length/2)+" div2:"+(maxValue + Math.abs(minValue)));
                }
            }
        } catch (IOException e1) {
            throw new PlayWaveException(e1);
        } 
        System.out.println("audio has analyzed");
        // for(int i = 0; i < audioweight.size(); i++)
            // System.out.println("audioAnalyze audioweight"+audioweight.get(i));
        return audioweight;
    }
}
