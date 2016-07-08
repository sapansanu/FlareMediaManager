/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package flaremediamanager;

import Iso14496.IsoFile;
import Video.Frame;
import Video.VideoParser;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.jcodec.api.JCodecException;
import sun.applet.Main;

/**
 *
 * @author mac
 */
public class FlareMediaManager {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, FileNotFoundException, JCodecException {

        File videoFile = new File("../sample.mp4");

        if (!videoFile.exists()) {
            try {
                throw new FileNotFoundException("File not found");
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (!videoFile.canRead()) {
            throw new IllegalStateException("No read permissions to file not found");
        }



        //Need id generating system, but okay for now
        String uniqueID = UUID.randomUUID().toString();
        File uniqueDir = new File(uniqueID);
        boolean successful = uniqueDir.mkdir();

        if (successful) {
            // creating the directory succeeded
            
            System.out.println("directory was created successfully");
            VideoParser videoParser = new VideoParser(videoFile);
            int totalFrames = videoParser.getVideoFramesTotal();

            PrintWriter writer = new PrintWriter(uniqueDir + "/meta.txt", "UTF-8");
            writer.println(totalFrames);
            writer.println(videoParser.getVideoWidth());
            writer.println(videoParser.getVideoHeight());
            writer.println(videoParser.getVideoFPS());
            writer.println(videoParser.getVideoDuration());
            writer.close();
            
            IsoFile isoFile = new IsoFile(videoFile);
            IsoFile rippedAudio = IsoFile.ripAudio(isoFile);
            
            

            Frame currentFrame = null;
            BufferedImage img = null;
            File outputFile = null;
            for (int n = 0; n < totalFrames; n++) {

                currentFrame = videoParser.getNextFrame(n); // current frame
                img = currentFrame.getBufferedImage(); // current buff image
                outputFile = new File(uniqueDir + "/frame" + n + ".jpg");
                ImageIO.write(img, "png" , outputFile );

            }

            
            
        }

    }

}
