package Video;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.api.UnsupportedFormatException;
import org.jcodec.api.specific.AVCMP4Adaptor;
import org.jcodec.api.specific.ContainerAdaptor;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.JCodecUtil.Format;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;

/**
 * Description:     VideoParser takes in a video file and can extract the frames
 *                  and meta data of the file.
 * 
 * Usage  -         Pulling one Frame at a time:
 * 
 *                  Call getNextFrame() repeatedly to get each frame one at a
 *                  time. You may use getVideoFramesTotal() as an upper bound
 *                  or simply let getNextFrame() return null once it reaches the
 *                  end of the video.
 * 
 *                  Use seekFrameAtSec(int) or seekFrameAtIndex(int) to target 
 *                  the closest frame of the given time and call getNextFrame() 
 *                  repeatedly again to start getting frames from that point in 
 *                  the video. Each frame is given an official index by JCodec.
 * 
 * Usage (old) -    call getAllFrames() to get an ArrayList of all Frames. Be wary
 *                  of OutOfMemory error if you're processing a large file. 
 * 
 * @author Jimmy
 */
public class VideoParser{
    
    
    private ArrayList<Frame> frameList; //used by array list methods only
    
    private FrameGrab frameGrab;    //Jcodec api for pulling frames out of video.
    private MP4Demuxer mp4d;    //the general mp4 parser; Might come in handy later.
    private AbstractMP4DemuxerTrack videoTrack; //the video in mp4 file
    private ContainerAdaptor decoder;   //decoder for videoTrack 
    
    
    
    public VideoParser(String file) throws FileNotFoundException, IOException, JCodecException{
            this(new File(file));
    }
    
    public VideoParser(File file) throws FileNotFoundException, IOException, JCodecException{
        
        //only mp4 supported in jcodec 0.1.9;
        Format detectFormat = JCodecUtil.detectFormat(file);
        if(detectFormat == Format.MOV){

            mp4d = new MP4Demuxer(NIOUtils.readableFileChannel(file));
            videoTrack = mp4d.getVideoTrack();
            decoder = new AVCMP4Adaptor((AbstractMP4DemuxerTrack) videoTrack);
            frameGrab = new FrameGrab(videoTrack, decoder);

        }else{
            
            throw new UnsupportedFormatException("Only MP4 supported at this time");
            
        }
            
    }
    
    
    
    //----------getters for metadata------------
    
    /**
     * Get total frames in this video. The desired frames are from 0 to total-1.
     * @return int - total frames in this video.
     */
    public int getVideoFramesTotal(){
        return videoTrack.getMeta().getTotalFrames();
    }
    
    
    /**
     * Get the video frames per second. This will return 0 if the duration is
     * less than or equal to zero.
     * 
     * @return double - frames per second of video.
     */
    public double getVideoFPS(){
        if(getVideoDuration() <= 0.0) return 0.0;
        return getVideoFramesTotal()/getVideoDuration();
    }
    
    
    public int getVideoHeight(){
        return decoder.getMediaInfo().getDim().getHeight();
    }
    
    
    public int getVideoWidth(){
        return decoder.getMediaInfo().getDim().getWidth();
    }
    
    /**
     * Get the duration of the entire video.
     * @return double - duration of the video.
     */
    public double getVideoDuration(){
        return videoTrack.getMeta().getTotalDuration();
    }
    

    
    /**
     * Get the index that will be given to getNextFrame()'s returning Frame.
     * 
     * @see getNextFrame()
     * @return int - index of current frame waiting to be called by getNextFrame()
     */
    public int getNextFrameIndex(){
        return (int) videoTrack.getCurFrame();
    }
    
    
    /**
     * Set the frame seeking pointer to a video frame at the given time. Call
     * getNextFrame() to retrieve the frame. But do not call this method
     * repeatedly to get the next frame in order, just call getNextFrame() as it
     * will increment on its own.
     * 
     * Throws exception if given time is beyond the video duration.
     * 
     * @see getNextFrame()
     * @param time : int -  seconds
     * @return int - Frame index; 
     * @throws IOException
     * @throws JCodecException 
     */
    public int seekFrameAtSec(int time) throws IOException, JCodecException{
        
        if(time < 0 || time > getVideoDuration())
            throw new IllegalArgumentException("VideoParser.seekFrameAtSec given time is invalid " + time);

        frameGrab.seekToSecondPrecise((double)time);
        return (int) videoTrack.getCurFrame();
        
    }
    
    
    /**
     * Set the frame seeking pointer to the given video frame at the given index.
     * Call getNextFrame() to retrieve the frame. But do not call this method
     * repeatedly to get the next frame in order, just call getNextFrame() as it
     * will increment on its own.
     * 
     * Throws exception if given index is beyond the video frame count.
     * 
     * @throws java.io.IOException
     * @throws org.jcodec.api.JCodecException
     * @see getNextFrame()
     * @param index : int - the frame index with respect to total set of frames in the video.
     */
    public void seekFrameAtIndex(int index) throws IOException, JCodecException{
        
        if(index < 0 || index >= getVideoFramesTotal())
            throw new IllegalArgumentException("VideoParser.seekFrameAtIndex given index is invalid " + index);

        frameGrab.seekToFramePrecise(index);
        
    }
    
    
    /**
     * Gets this current frame and increments to the next frame. Call this method
     * repeatedly to get each subsequent frame one at a time. Use seekFrameAtIndex()
     * or seekFrameAtSec() to change the starting frame reading point.
     * 
     * @throws java.io.IOException
     * @see seekFrameAtIndex(int)
     * @see seekFrameAtSec(int)
     * @return a Frame or null if end of video
     */
    public Frame getNextFrame() throws IOException{
        
        int index = (int) videoTrack.getCurFrame();
        Picture p = frameGrab.getNativeFrame();
        return new Frame(p, index);
        
    }
    
    /**
     * Gets frame at specified index and increments to next frame. This method
     * is a nested call of seekFrameAtIndex(index) and getNextFrame(), but if
     * the given index is the next expected index, then it forward the call to
     * getNextFrame().
     * @param index - The targeted Frame index
     * @return Frame or null
     */
    public Frame getNextFrame(int index) throws IOException, JCodecException{
        
        if(index == videoTrack.getCurFrame()) return getNextFrame();
        seekFrameAtIndex(index);
        return getNextFrame();
        
    }
    
    
    
    //\/\/\/\\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/
    //--------------------------------------------------------------------------------------\\
    //  OLD ARRAYLIST METHODS : BE WARY OF OUTOFMEMORY ERROR WHEN PROCESSING A LARGE FILE   \\
    //---------------(Recommend you use the methods above instead)--------------------------\\
    //\/\/\/\\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/
    
    
    /**
     * Retrieve all the frames in the video file.
     * <p><strong>Beware of OutOfMemory error if processing a large file. </strong></p>
     * 
     * @throws IOException
     * @throws org.jcodec.api.JCodecException
     * @see getListOfFrames()
     * @return an ArrayList of Frames
     */
    public ArrayList<Frame> getAllFrames() throws IOException, JCodecException{
        
        if(frameList == null){

            frameList = new ArrayList<Frame>();
            frameGrab.seekToFrameSloppy(0);

            for(int i = 0; i < getVideoFramesTotal(); i++){
                
                frameList.add(getNextFrame());
                //System.out.println("added Frame: " + i);

            }

        }

        return frameList;
    }
    
    
    /**
     * Retrieve all frames starting at the specified time to the end of the video.
     * <p><strong>Beware of OutOfMemory error if processing a large file. </strong></p>
     * 
     * @throws java.io.IOException
     * @throws org.jcodec.api.JCodecException
     * @param sec - time in video duration to begin getting all the frames.
     * @return ArrayList of Frames starting at the given second.
     */
    public ArrayList<Frame> getAllFramesFromTime(int sec) throws IOException, JCodecException{
        
        ArrayList<Frame> realFrameList = getAllFrames();
        ArrayList<Frame> returningList = new ArrayList<Frame>();

        int i  = seekFrameAtSec(sec);

        returningList.addAll(realFrameList.subList(i+1, realFrameList.size()));

        return returningList;
            
    }
    
}