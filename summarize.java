import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;


/**
* summarize class which read in video and audio path and  call analyzer clasese to 
* analyze the video, then call the composeVideo class to output result
* @author Jiaqi Liu
* @author Lisha Xu
*/

public class summarize {


    public static void main(String[] args) throws PlayWaveException{
        ArrayList<Integer> breaksArray;
        ArrayList<Double> videoweight;
        ArrayList<Double> audioweight;
        ArrayList<Double> sumweight = new ArrayList<>();
        ArrayList<Integer> highlightframe = new ArrayList<>();
        double VFPS;

        try{
            if(args.length<3)
                System.out.println("run with three args: videodirpath audiopath.wav summarizelevel(0/1/2)");
            String videopath = args[0];
            String audiopath = args[1];
            String divideLevel = args[2];
            int level = Integer.valueOf(divideLevel);
            // opens the inputStream
            FileInputStream inputStream = new FileInputStream(audiopath);

            FileInputStream waveStream = new FileInputStream(audiopath);

            //audioInputStream = AudioSystem.getAudioInputStream(this.waveStream);
            
            //add buffer for mark/reset support, modified by Jian
            File audioFile = new File(audiopath);
            InputStream bufferedIn = new BufferedInputStream(waveStream);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bufferedIn);
            AudioFormat audioFormat = audioInputStream.getFormat();
            double sampleSize = audioFormat.getFrameSize();
            double Framerate = audioFormat.getFrameRate();
            int time = (int)Math.round(audioFile.length()/(sampleSize*Framerate));
            File folder = new File(videopath);
            VFPS = folder.listFiles().length/time;
            System.out.println("VFPS is"+VFPS);

            //call Video analyzer to divide the shots and calculate video weight
            VideoAnalyzer va = new VideoAnalyzer(videopath);
            breaksArray = va.createShots();
            videoweight = va.motionWeight();

            //call Audio analyzer to calculate audio weight 
            audioAnalyze aa = new audioAnalyze(inputStream, breaksArray, VFPS);
            audioweight = aa.audioWeight();

            //divide the weights with max value of each shot to help combine the values
            double videoMax = Collections.max(videoweight);
            double audioMax = Collections.max(audioweight);
            System.out.println("VideoMax:"+videoMax+"AudioMax:"+audioMax);
            for(int i = 0; i < breaksArray.size()-1; i++){
                sumweight.add(0.5*videoweight.get(i)/videoMax + 0.5 * audioweight.get(i)/audioMax);
                System.out.println(sumweight.get(i));
                
                if(sumweight.get(i)>=0.5+0.05*level){
                    highlightframe.add(i);
                }
            }
            //Analyze an = new Analyze(videopath, audiopath);
            //ArrayList<Integer> highlights = an.highlights;
            writecsv(videoweight,"./vweight.csv");
            writecsv(audioweight,"./aweight.csv");
            writecsv(sumweight, "./sweight.csv");
            ComposeVideo cv = new ComposeVideo(highlightframe,breaksArray, videopath,audiopath,VFPS);
            cv.addshots();
            cv.startFolder();
            cv.writeRGB();
            cv.writeWav();
        }
        catch(Exception e){
            e.printStackTrace(); 
        } 
        // catch (UnsupportedAudioFileException e1) {
        //         throw new PlayWaveException(e1);
        // } catch (IOException e1) {
        //         throw new PlayWaveException(e1);
        // }
        return;
    }

    /**
     * helping function to output weights into csv files
     * @param ArrayList<Double> weight,  arraylist that need to be saved to files
     * @param path  output file path
     * @throws FileNotFoundException,IOException
     */
    public static void writecsv(ArrayList<Double> weight,String path){   
        try{        
            File writefile = new File(path);
            writefile.createNewFile();

            BufferedWriter writetext = new BufferedWriter(new FileWriter(writefile));
            for(int i = 0 ;i<weight.size();i++){
                writetext.newLine();
                writetext.write(String.valueOf(weight.get(i)));
            }
            writetext.flush();
            writetext.close();
        }
        catch (FileNotFoundException e){
            e.printStackTrace();
            System.out.println("File not find");
        }
        catch(IOException e){
            System.out.println("Write Error");
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}
