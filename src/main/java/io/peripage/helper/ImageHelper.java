package io.peripage.helper;

import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;

/**
 * Helper class for image manipulation.
 * */
public class ImageHelper {

    /**
     * Convert an image to a black and white image with inverted colors.
     * @param img The image to convert
     * @param rowWidth The width of printer row
     * @return The converted image
     */
    public static BufferedImage convertToReversedBlackAndWhite(BufferedImage img, int rowWidth) {
        img = ImageHelper.convertToGrayscaleImage(ImageHelper.resizeImage(img, Math.min(img.getWidth(), rowWidth)));
        img = DitheringHelper.dithering(img);
        img = ImageHelper.reverseGrayscaleImage(img);
        return img;
    }

    /**
     * Resize an image to a new width and keep the aspect ratio.
     * @param img The image to resize
     * @param newWidth The new width
     * @return The resized image
     */
    public static BufferedImage resizeImage(BufferedImage img, int newWidth) {
        int newHeight = (int) ((double) newWidth / img.getWidth() * img.getHeight());
        Image tmp = img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return resizedImage;
    }

    /**
     * Returns the raw image data from a BufferedImage.
     * @param image The image to get the raw data from
     * @return The raw image data
     */
    public static byte[] getRawImageData(BufferedImage image) {
        DataBuffer dataBuffer = image.getRaster().getDataBuffer();
        if (dataBuffer instanceof DataBufferByte dataBufferByte) {
            return dataBufferByte.getData();
        }
        return null;
    }

    /**
     * Pad an image to center it with white pixels to the desired width around it.
     * @param img The image to pad
     * @param desiredWidth The resulting image desired width
     * @return The padded image
     */
    public static BufferedImage centerPadImage(BufferedImage img, int desiredWidth) {
        int xOffset = (desiredWidth - img.getWidth()) / 2;
        return padImage(img, desiredWidth, xOffset);
    }

    public static BufferedImage leftPadImage(BufferedImage img, int desiredWidth) {
        return padImage(img, desiredWidth, 0);
    }

    public static BufferedImage rightPadImage(BufferedImage img, int desiredWidth) {
        int xOffset = desiredWidth - img.getWidth();
        return padImage(img, desiredWidth, xOffset);
    }

    /**
     * Place an image on the page with white pixels to the left of it defined by the offset.
     * @param img The image to pad
     * @param desiredWidth The resulting image desired width
     * @return The padded image
     */
    public static BufferedImage padImage(BufferedImage img, int desiredWidth, int offset) {
        int originalHeight = img.getHeight();

        BufferedImage paddedImage = new BufferedImage(desiredWidth, originalHeight, BufferedImage.TYPE_BYTE_BINARY);

        Graphics2D g2d = paddedImage.createGraphics();
        g2d.drawImage(img, offset, 0, null);
        g2d.dispose();

        return paddedImage;
    }

    /**
     * Convert a BufferedImage to grayscale.
     * @param img The image to convert
     * @return The grayscale image
     */
    static BufferedImage convertToGrayscaleImage(BufferedImage img) {
        BufferedImage grayscaleImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

        ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        op.filter(img, grayscaleImage);

        return grayscaleImage;
    }


    /**
     * Reverse the grayscale so black becomes white and white becomes black.
     * @param grayscaleImage The grayscale image to reverse
     * @return The reversed grayscale image
     */
    static BufferedImage reverseGrayscaleImage(BufferedImage grayscaleImage) {
        // Create a lookup table to invert the grayscale values.
        byte[] invertTable = new byte[256];
        for (int i = 0; i < 256; i++) {
            invertTable[i] = (byte) (255 - i);
        }

        // Create a lookup operation to apply the invert table.
        LookupOp invertOp = new LookupOp(new ByteLookupTable(0, invertTable), null);

        // Create a new buffered image to store the reversed grayscale image.
        BufferedImage reversedGrayscaleImage = new BufferedImage(grayscaleImage.getWidth(), grayscaleImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

        // Apply the lookup operation to the grayscale image to reverse it.
        invertOp.filter(grayscaleImage, reversedGrayscaleImage);

        return reversedGrayscaleImage;
    }

    private static JFrame frame;
    private static JLabel label;
    public static void display(BufferedImage image){
        if(frame==null){
            frame=new JFrame();
            frame.setTitle("stained_image");
            frame.setSize(image.getWidth(), image.getHeight());
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            label=new JLabel();
            label.setIcon(new ImageIcon(image));
            frame.getContentPane().add(label,BorderLayout.CENTER);
            frame.setLocationRelativeTo(null);
            frame.pack();
            frame.setVisible(true);
        } else label.setIcon(new ImageIcon(image));
    }

}
