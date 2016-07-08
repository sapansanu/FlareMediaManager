/**
 * @author      Jose Ortiz Costa
 * Date         04/15/2016
 * Modified     04/15/2016
 * Package      video
 * File         Frame.java
 * Description  This class takes a Picture object representing a raw frame from the
 *              Video Parser as a parameter, and also it takes the position of the
 *              frame in the video.
 *              This class represents an actual Frame already parsed and transformed
 *              to a BufferedImage object, so, you can create an actual file in any
 *              format from the BufferedImage i.e: BMP, PNG, JPEG... or, instead you
 *              can get the data in bytes from this frame.
 *              This class constains very useful methods that extract metadata from
 *              a frame such as its height, width, position, modelColor...etc
 * See:         VideoParser and VideoTransmission classes
 *
 * Usage:       Single Frame ----> Frame f = new Frame (picFrame, positionInVideo)
 *              Multiple Frames --->   ArrayList <Frame> = ObjectVideoParser.getAllFrames();
 *              Where ObjectVideoParser is an object created from the class 
 *              VideoParser.
 */
package Video;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform;

public class Frame {
    // static constants formats you can add more if you need

    public static final String BMP = "bmp";
    public static final String PNG = "png";
    public static final String JPEG = "jpeg";

    private BufferedImage frame; // format required for image frame
    private int indexInVideo; // position in video

    /**
     * Constructor
     *
     * @param _framePic Primitive or raw state of a frame returned by VideoParser
     * @param _indexInVideo The position of this frame in its video source.
     */
    public Frame(Picture _framePic, int _indexInVideo) {
        this.frame = transform(_framePic);
        this.indexInVideo = _indexInVideo;
    }
    
    public int getIndex(){
        return indexInVideo;
    }

    /**
     *
     * @return BufferedImage object
     */
    public BufferedImage getBufferedImage() {
        return frame;
    }

    /**
     *
     * @return the height of the frame
     */
    public int getHeight() {

        return this.frame.getHeight();
    }

    /**
     *
     * @return the width of the frame
     */
    public int getWidth() {
        return this.frame.getWidth();
    }

    /**
     *
     * @return the colorModel of the frame
     */
    public ColorModel getColorModel() {

        return this.frame.getColorModel();
    }

    /**
     *
     * @return the level of transparence of the frame
     */
    public int getTransparence() {
        return this.frame.getTransparency();
    }

    /**
     *
     * @return the acceleration priority
     */
    public float getAccelerationPriority() {

        return this.frame.getAccelerationPriority();

    }

    /**
     * Transform a BufferedImage object ( frame ) in a stream of bytes
     *
     * @return a stream of bytes representing the frame
     */
    public byte[] getData() {
        return ((DataBufferByte) this.frame.getData().getDataBuffer()).getData();
    }

    /**
     * Creates a image of this frame in disk in the desired format
     *
     * @param imageName
     * @param format
     * @param appendPositionInImageName
     * @throws IOException
     */
    public void createImage(String imageName, String format, boolean appendPositionInImageName) throws IOException {
        if (appendPositionInImageName) {
            imageName = imageName + "_" + this.indexInVideo;
        }
        ImageIO.write(this.frame, format, new File(imageName + "." + format));
    }

    /**
     * Transform a Picture object into a BufferedImage Object
     *
     * @param framePic Picture object
     * @return a BufferedImage object (frame)
     */
    private BufferedImage transform(Picture framePic) {
        Transform transform = ColorUtil.getTransform(framePic.getColor(), ColorSpace.RGB);
        Picture rgb = Picture.create(framePic.getWidth(), framePic.getHeight(), ColorSpace.RGB);
        transform.transform(framePic, rgb);
        return toBufferedImage(rgb);

    }

    /**
     * Helper method to transform a Picture object into a BufferedImage
     *
     * @param framePic Picture Object
     * @return a BufferedImage object
     */
    private BufferedImage toBufferedImage(Picture framePic) {
        if (framePic.getColor() != ColorSpace.RGB) {
            Transform transform = ColorUtil.getTransform(framePic.getColor(), ColorSpace.RGB);
            Picture rgb = Picture.create(framePic.getWidth(), framePic.getHeight(), ColorSpace.RGB, framePic.getCrop());
            transform.transform(framePic, rgb);
            framePic = rgb;
        }

        BufferedImage dst = new BufferedImage(framePic.getCroppedWidth(), framePic.getCroppedHeight(),
                BufferedImage.TYPE_3BYTE_BGR);

        if (framePic.getCrop() == null) {
            toBufferedImage(framePic, dst);
        } else {
            toBufferedImageCropped(framePic, dst);
        }

        return dst;

    }

    /**
     * Helper method that Overloads the method toBufferedImage (Picture) in
     * order to add a new parameter BufferedImage
     *
     * @param src Picture object
     * @param dst BufferedImafe object
     */
    private static void toBufferedImage(Picture src, BufferedImage dst) {
        byte[] data = ((DataBufferByte) dst.getRaster().getDataBuffer()).getData();
        int[] srcData = src.getPlaneData(0);
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) srcData[i];
        }

    }

    /**
     * Helper method that convert a Picture Object into a BufferedImage cropped
     * object. Very useful for unusual frame formats.
     *
     * @param src Picture Object
     * @param dst BufferedImage object
     */
    private void toBufferedImageCropped(Picture src, BufferedImage dst) {
        byte[] data = ((DataBufferByte) dst.getRaster().getDataBuffer()).getData();
        int[] srcData = src.getPlaneData(0);
        int dstStride = dst.getWidth() * 3;
        int srcStride = src.getWidth() * 3;
        for (int line = 0, srcOff = 0, dstOff = 0; line < dst.getHeight(); line++) {
            for (int id = dstOff, is = srcOff; id < dstOff + dstStride; id += 3, is += 3) {
                data[id] = (byte) srcData[is];
                data[id + 1] = (byte) srcData[is + 1];
                data[id + 2] = (byte) srcData[is + 2];
            }
            srcOff += srcStride;
            dstOff += dstStride;
        }
    }

}
