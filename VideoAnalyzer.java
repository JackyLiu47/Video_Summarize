import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import javax.swing.*;
import javax.swing.text.html.BlockView;

/**
* This VideoAnalyzer class helps create the shots by calculating the average 
* color level of each 3 frame, and analyze the luminance of frames to calculate 
* the motion weight of each shot.
* @author Jiaqi Liu
* @author Lisha Xu
*/
public class VideoAnalyzer {
    private double prevAvgLumin = 0;
    private double[][] avgLuminBlock;   //average luminance of each block
    private Set<Integer> breaks;    //breaks calculated by breakDetect()
    private double[] avgpixelMotion;
    private double[] avgBlockMotion;
    private int width = 320;
    private int height = 180;
    private int blockrow = 5;   //how many blocks in a row
    private int blockcol = 5;   //how many blocks in a column
    private double limit = 20000.0;     //Color level threshold to determine if a frame changes shot
    private double LuminThreshold = 1.0;    
    private double LuminBlockThreshold = 1.0;
    private double BlockMoveMaxVal = 7;
    private int[][][] prevLevel;    //array to store color level of previous frame
    private String folderpath;
    //BufferedImage img;
    File folder;
    

/**
 * constructor of VideoAnalyzer class
 * @param   vpath    String  the folder path of video files
 */
    public VideoAnalyzer(String vpath){
        folder = new File(vpath);
        this.folderpath = vpath;
        this.breaks = new HashSet<Integer>();
        prevLevel = new int[4][4][4];
        //avgLumin = new double[folder.listFiles().length/3+1];
        avgLuminBlock = new double[folder.listFiles().length/3+1][400];
		//File[] imgfiles = folder.listFiles();
    }  

    /**
     * public function which create shots of a video by calling breakDetect function 
     * in a loop which went through all of the frames.
     */
    public ArrayList<Integer> createShots(){
        File[] imgfiles = folder.listFiles();
        System.out.println("Dividing the video.");
        breaks.add(0);
        for(int i = 0; i < imgfiles.length; i++){
            breakDetect(i);
        }
        breaks.add(imgfiles.length-1);
        System.out.println("Shots created");
        ArrayList<Integer> breakarray = new ArrayList<>(breaks);
        Collections.sort(breakarray);
        return breakarray;
        //System.out.println(breaks);
    }

    /**
     * private function which check each frame's average color level(divide 256bit color into 4 level) 
     * then compare with the previous frame. If the difference is big enough, add the frame to break array.
     * @param   index   int     frame index
     * @throws  IOException     
     *          FileNotFoundException   
     *          InterruptedException(Only in debug)
     */
    private void breakDetect(int index){
        int [][][] nowLevel = new int[4][4][4];
        try{
            String imgPath = folderpath + "frame" + index + ".rgb";

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

                    int r1 = (int)(r & 0xff)/64;
                    int g1 = (int)(g & 0xff)/64;
                    int b1 = (int)(b & 0xff)/64;

                    nowLevel[r1][g1][b1]++;
                    ind++;
				}
			}
            double diff = 0;
            if (index > 0){
                for(int i = 0; i < 4; i++)
                    for(int j = 0; j < 4; j++)
                        for(int k = 0; k < 4; k++){
                            diff += Math.abs(nowLevel[i][j][k] - prevLevel[i][j][k]);                             
                        }
            }
            ArrayList<Integer> temp = new ArrayList<>(breaks);
            Collections.sort(temp);
            if(diff > limit){
                breaks.add(index);
            }
            Thread.sleep(0);
            for(int i = 0; i < 4; i++)
                    for(int j = 0; j < 4; j++)
                        for(int k = 0; k < 4; k++)
                            prevLevel[i][j][k] = nowLevel[i][j][k]; 

            raf.close();


        }
        catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * check if the index of block is in range
     * @param x int col value
     * @param y int row value
     * @return  boolean
     */
    private boolean inRange(int x,int y){
        return (x>=0 && x <blockrow) && (y>=0 && y<blockcol);
    }

    private double[][] compareblockLumin(double[][] now, double[][] prev){
        double avg = 0;
        double[][] retMat = new double[blockrow][blockcol];     
        for(int i=0; i < blockrow; i++){
            for(int j =0; j < blockcol; j++){
                ArrayList<Double> diff = new ArrayList<Double>();
                for(int x = -1; x <= 1; x++){
                    for(int y = -1; y <= 1; y++){
                        if(inRange(i+x, j+y)){
                            diff.add(Math.abs(now[i][j]-prev[i+x][j+y]));
                        }

                    }
                }
                Collections.sort(diff);
                retMat[i][j] = diff.get(0);
                avg+=diff.get(0);
            }
        } 
        avg/=(blockcol*blockrow);
        return retMat;
    }
    
    /**
     * REMOVEABLE block motion change detect function
     * @param index
     * @return
     */
    private double[][] getblocklumin(int index){
        double[][] blockluminMat = new double[blockcol][blockrow];
        try
		{
            //open frame file with index
            String imgPath = folderpath + "frame" + index + ".rgb";

			int frameLength = width*height*3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);

            //read rgb value, save in 
			double[] sum = new double[blockrow*blockcol];
            
            for(int i =0; i < blockrow * blockcol; i++)
                sum[i] = 0.0;
            int ind = 0;
            for(int i = 0; i < blockrow * blockcol; i++){
                for(int y = 16*i/width*9; y < Math.min(16*i/width*9 + 9,180); y++)
                {
                    for(int x = 16*i % width; x < 16*(i+1)%width; x++)
                    {
                        byte a = 0;
                        byte r = bytes[ind];
                        byte g = bytes[ind+height*width];
                        byte b = bytes[ind+height*width*2]; 
    
                        int r1 = (int)(r & 0xff);
                        int g1 = (int)(g & 0xff);
                        int b1 = (int)(b & 0xff);
                        double lumin = 0.299*r1+0.587*g1+0.114*b1;
                        sum[i]+=lumin;
                        ind++;
                    }
                }
                blockluminMat[i/blockcol][i%blockrow] = sum[i]/((width/blockcol)*(height/blockcol));
                //avgLuminBlock[i] = sum[i]/(16*9);
                
            }
            raf.close();  
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		} 
        return blockluminMat;
    }

    /**
     * public function to call luminance check in order to calculate the motion weight of a shot
     */
    public ArrayList<Double> motionWeight(){
        System.out.println("Start to analyze motion");
        ArrayList<Integer> breakarray = new ArrayList<>(breaks);
        ArrayList<Double> motionWeight = new  ArrayList<>();
        boolean isStart = true;
        Collections.sort(breakarray);

        for(int i = 0; i < breaks.size() - 1; i++){
            double count = 0;
            for(int j=breakarray.get(i); j < breakarray.get(i + 1); j += 2){
                if(j != breakarray.get(i)){
                    isStart = false;
                }
                else
                    isStart = true;
                count += pixelMotion(j, isStart); 
            }
            count/=Math.max(((breakarray.get(i+1)-breakarray.get(i))/2),1);
 
            motionWeight.add(count);
        }
        System.out.println("Motion has analyzed");
        return motionWeight;
    }
   /**
    * private funtion that analyze the average luminance of a frame(average of all pixels)
    * @param    int     index   frame index
    * @param    boolean isStart flag to judge if the frame is the first frame(don't have previous frame to compare)
    * @return   int     motion  if lumincance change over threshold, the motion value is 1, otherwise 0
    * @throws   FileNotFoundException,IOException
    */      
    private int pixelMotion(int index, boolean isStart)
	{
        int motion = 0;
		try
		{
            double avgLumin;
            
            String imgPath = folderpath + "frame" + index + ".rgb";

			int frameLength = width*height*3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);

			double sum = 0.0;
            int ind = 0;
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind+height*width];
					byte b = bytes[ind+height*width*2]; 

                    int r1 = (int)(r & 0xff);
                    int g1 = (int)(g & 0xff);
                    int b1 = (int)(b & 0xff);
                    double lumin = 0.299*r1+0.587*g1+0.114*b1;
                    sum+=lumin;
                    ind++;
				}
			}
            avgLumin = sum/(height*width);
            double diff = 0;
            if(!isStart){
                diff = avgLumin - prevAvgLumin;
                if (diff > LuminThreshold){
                    motion = 1;
                }
            }
            prevAvgLumin = avgLumin;
            raf.close();

		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
        return motion;
	}

    /**
     * private funtion calculate block luminance change(NOT USED)
     */
    private void LuminBlockCalc(int index)
	{
        //divide the picture into 400 blocks. Each blocks' size is 16*9
		try
		{
            String imgPath = folderpath + "frame" + index + ".rgb";

			int frameLength = width*height*3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);

			double[][] sum = new double[index][400];
            for(int i =0; i < 400; i++)
                sum[index][i] = 0.0;
            int ind = 0;
            for(int i = 0; i < 400; i++){
                for(int y = 16*i/width*9; y < Math.min(16*i/width*9 + 9,180); y++)
                {
                    for(int x = 16*i % width; x < 16*(i+1)%width; x++)
                    {
                        byte a = 0;
                        byte r = bytes[ind];
                        byte g = bytes[ind+height*width];
                        byte b = bytes[ind+height*width*2]; 
    
                        int r1 = (int)(r & 0xff);
                        int g1 = (int)(g & 0xff);
                        int b1 = (int)(b & 0xff);
                        double lumin = 0.299*r1+0.587*g1+0.114*b1;
                        sum[index][i]+=lumin;
                        ind++;
                    }
                }
                avgLuminBlock[index][i] = sum[index][i]/(16*9);
            }

            
            double diff = 0;
            if(index > 0){
                for(int i = 0; i<400; i++){
                    diff = avgLuminBlock[index][i] - avgLuminBlock[index - 3][i];
                    if (diff > LuminBlockThreshold){
                        breaks.add(index);
                    }
                }
            }
            raf.close();
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


}
