import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect.Type;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.DataLine.Info;

/**
 * Save the result frame and the audio to other directory and 
 * complete the function which could add shots between highlight shots
 * @author Jiaqi Liu
 * @author Lisha Xu
 */
public class ComposeVideo {
    private ArrayList<Integer> shots;
    private ArrayList<Integer> breaks;
    private String imgpath;
    private String newPath;
    private String audiopath;
    private double sampleSize;
    private double VFPS;
    private double bytesperVframe;
    private File newFolder;
    
    /**
     * Constructor function
     * @param shots: Highlight shots which will be displayed. save the index of the breaks.
     * @param breaks: save the break frame number in the arrayList
     * @param imgpath: the directory path that save the original img
     * @param audiopath: the path that save the audio file
     * 
     */
    public ComposeVideo(ArrayList<Integer> shots, ArrayList<Integer> breaks, String imgpath, String audiopath, double VFPS){
        this.shots = shots;
        this.breaks = breaks;
        this.imgpath = imgpath;
        this.audiopath = audiopath;
        this.VFPS = VFPS;
        this.newPath = imgpath.substring(0,imgpath.length()-1)+"summarize/";
        newFolder = new File(newPath);
    }

    public void startFolder(){
        if(newFolder.exists())
            deleteFile(newFolder);
    }
    public boolean deleteFile(File dirFile) {
        if (!dirFile.exists()) {
            return false;
        }
    
        if (dirFile.isFile()) {
            return dirFile.delete();
        } else {
    
            for (File file : dirFile.listFiles()) {
                deleteFile(file);
            }
        }
    
        return dirFile.delete();
    }

    /**
     * Create the new directory and save the frames that need to be displayed.
     * The directory name is original name + summerize
     */
    public void writeRGB(){
        File folder = new File(imgpath);
        // File[] imgfiles = folder.listFiles();
        
        if(!newFolder.exists())
            newFolder.mkdirs();
        int count = 0;
        for(int i = 0; i < shots.size(); i++){
            int startFrame = breaks.get(shots.get(i));
            System.out.print("highlight shot:"+shots.get(i)+" ");
            int endFrame = breaks.get(shots.get(i)+1);
            System.out.println("start:"+startFrame+" end:"+endFrame);
            for(int j = startFrame; j < endFrame; j++){
                String videoFramePath = imgpath+ "frame" + j + ".rgb";
                fileCopy(videoFramePath, newPath+"frame" + count + ".rgb");
                //System.out.println("frame"+count);
                count++;
            }
        }
    }

    /**
     * Copy the file from one directory to another
     */
    private void fileCopy(String sFile, String tFile){
        FileInputStream fi = null;
        FileOutputStream fo = null;
        FileChannel in = null;
        FileChannel out = null;
        File s = new File(sFile);
        File t = new File(tFile);
        if(s.exists()&&s.isFile()){
            try{
                fi = new FileInputStream(s);
                fo = new FileOutputStream(t);
                in = fi.getChannel();
                out = fo.getChannel();
                in.transferTo(0,in.size(), out);
            }
            catch(IOException e){
                e.printStackTrace();
            }
            finally{
                try{
                    fi.close();
                    in.close();
                    fo.close();
                    out.close();
                }
                catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * get the audio content that in highlight shots and save them to another wav file.
     */
    public void writeWav() throws PlayWaveException{
        //File audio = new File(audiopath);
        
        try {
            FileInputStream waveStream = new FileInputStream(audiopath);
            AudioInputStream audioInputStream = null;
            //audioInputStream = AudioSystem.getAudioInputStream(this.waveStream);
            
            //add buffer for mark/reset support, modified by Jian
            InputStream bufferedIn = new BufferedInputStream(waveStream);
            audioInputStream = AudioSystem.getAudioInputStream(bufferedIn);
        	// Obtain the information about the AudioInputStream
            AudioFormat audioFormat = audioInputStream.getFormat();
            Info info = new Info(SourceDataLine.class, audioFormat);
            sampleSize = audioFormat.getFrameSize();
            double Framerate = audioFormat.getFrameRate();
            bytesperVframe = sampleSize*Framerate/VFPS;

            ArrayList<Byte> temp = new ArrayList<>();
            int index = 0;
            for(int i = 0; i < breaks.size()-1;i++){
                int shotlen = (breaks.get(i+1)-breaks.get(i))*(int)bytesperVframe;
                byte[] audioBuffer = new byte[shotlen];
                audioInputStream.read(audioBuffer, 0, audioBuffer.length);
                if(shots.get(index) == i){
                    for(int j = 0; j<audioBuffer.length;j++){
                        temp.add(audioBuffer[j]);
                    }
                    index++;
                }
                if(index>=shots.size())
                    break;
            }
            System.out.println("hightlight shots number"+shots.size());
            System.out.println("total shots number"+breaks.size());


            byte[] highlightaudio = new byte[temp.size()];
            for(int i = 0;i<temp.size();i++){
                highlightaudio[i] = temp.get(i);
            }
            InputStream ain = new ByteArrayInputStream(highlightaudio);

            AudioInputStream stream = new AudioInputStream(ain, audioFormat, temp.size());
            File file = new File(imgpath.substring(0,imgpath.length()-1)+"Audio.wav");
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, file);
        } catch (UnsupportedAudioFileException e) {
            throw new PlayWaveException(e);
        } catch (IOException e) {
            throw new PlayWaveException(e);
        }
    }
    /**
     * add one shots between highlight
     */
    public void addshots(){
        Set<Integer> tempshots = new HashSet<>();
        for(int i = 0; i < shots.size(); i++){
            tempshots.add(shots.get(i));
            if(i<shots.size()-1 && shots.get(i+1)-shots.get(i) <= 5){
                for(int j = shots.get(i); j<shots.get(i+1); j++)
                    tempshots.add(j);
            }
        }
        shots.clear();
        shots = new ArrayList<>(tempshots);
        Collections.sort(shots);
    }
}