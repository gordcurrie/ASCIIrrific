package com.currie.asciirrific;

import java.io.File;
import java.io.FileOutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.hardware.Camera.Size;
import android.os.Environment;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout.LayoutParams;

public class AsciiView extends View {

    private static float fontSize = 10f;
    private static final float FONT_WIDTH_PERCENTAGE = 0.6f;
    public static File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES), "AsciirifficJpg");
    public static File textStorageDir = new File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES), "AsciirifficTxt");
    private Paint paint;
    private byte[] rawImageData;
    private int counter;
    private Size cameraSize;
    private char[] asciiCharNegative = { ' ', '.', ',', ':', ';', 'i', '1', 't', 'f', 'L', 'C', 'G', '0', '8', '@' };
    private char[] asciiChar = { '@', '8', '0', 'G', 'C', 'L', 'f', 't', '1', 'i', ';', ':', ',', '.', ' ' };
    private static int called = 0;
    private boolean showAsNegative = false;
    private StringBuilder outPutBuilder = new StringBuilder();
    private File textfile;
    private static int minValue;
    private static int maxValue;

    public AsciiView(Context context) {
        super(context);
        paint = new Paint();
    }
    
    public AsciiView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
    }
    

    @Override
    protected void onDraw(Canvas canvas) {
        if (rawImageData != null) {
            //reset the StringBuilder each time onDraw is called
            outPutBuilder.setLength(0);
            //
            minValue = maxValue = 300;
            called = counter = 0;
            paint.setColor(Color.WHITE);
            paint.setStyle(Style.FILL);
            paint.setTypeface(Typeface.MONOSPACE);
            canvas.drawPaint(paint);
            int[] rgbImageData = convertNv21ToRgb(rawImageData, cameraSize.width, cameraSize.height);
            paint.setColor(Color.BLACK);
            paint.setTextSize(fontSize);
            float x = 0, y = 0;
            // height of the letter equals ascent + decent
            float letterHeight = Math.abs(paint.ascent()) + Math.abs(paint.descent());
            //loop for each line of characters in image.
            for (called = 0; called < cameraSize.height / letterHeight; called++) {
                y += letterHeight;
                String asciiText = buildAsciiText(canvas, paint, rgbImageData);
                canvas.drawText(asciiText, (int) x, (int) y, paint);
                //adding line for text file
                outPutBuilder.append(asciiText).append('\n');
            }
        }
        super.onDraw(canvas);
    }
    
    /** 
     * Create a File for saving image 
     */
    private File getOutputMediaFile(){
        //creating directories if needed
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        if (! textStorageDir.exists()){
            if (! textStorageDir.mkdirs()){
                return null;
            }
        }
        // Creating file
        Time today = new Time(Time.getCurrentTimezone());
        today.setToNow();
        String dateTime = today.format2445();
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                "asciirific_"+ dateTime + ".jpg");
        
        textfile = new File(textStorageDir.getPath() + File.separator +
                "asciirific_"+ dateTime + ".txt");
       
        return mediaFile;
    }
    /**
     * saves the image on canvas as a jpeg and text vile
     */
    public void savePicture() {
        setDrawingCacheEnabled(true);
        setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        File file = getOutputMediaFile();
        FileOutputStream ostream;
        try {
            //saves the bitmap
            Bitmap bitmap = Bitmap.createBitmap(getDrawingCache());
            destroyDrawingCache();
            ostream = new FileOutputStream(file);
            bitmap.compress(CompressFormat.JPEG, 80, ostream);
            ostream.flush();
            //saves the text file
            ostream = new FileOutputStream(textfile);
            byte[] textfileBytes = outPutBuilder.toString().getBytes();
            ostream.write(textfileBytes);
            ostream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * takes row of int data representing darkness of pixels and converts to ascii
     * characters.
     */
    private String buildAsciiText(Canvas canvas, Paint paint, int[] rgbImageData) {
        counter = (int) (called * fontSize * cameraSize.width);
        StringBuilder builder = new StringBuilder();
        // seems like the width of the character is 60% of the height
        float textWidth = fontSize * FONT_WIDTH_PERCENTAGE;
        for (int k = 0; k < (cameraSize.width / textWidth); k++) {
            int ch = Math.abs(rgbImageData[counter] - minValue);
            int whichChar = (int) Math.floor(ch * (asciiChar.length - 1) / maxValue);
            char thisChar = showAsNegative ? asciiCharNegative[whichChar] : asciiChar[whichChar];
            builder.append(thisChar);
            counter += Math.floor(textWidth);
        }
        return builder.toString();
    }

    /**
     * sets the image data and size of the asciiView
     */
    public void setImageData(byte[] imageData) {
        this.rawImageData = imageData;
        View asciiView = (AsciiView) findViewById(R.id.asciiView);
        //sets the size of view relative to camera preview size
        asciiView.setLayoutParams(new LayoutParams(cameraSize.width, cameraSize.height));
    }

    /**
     * takes array of byte data nv21 format created by camera preview and converts it
     * to array of int representing the combine rgb value of pixel ignores u and v 
     * data as it is not required
     */
    public static int[] convertNv21ToRgb(byte[] yuv, int width, int height) {
        final int frameSize = width * height;
        int[] convertedData = new int[frameSize];

        int a = 0;
        for (int i = 0; i < frameSize; ++i) {
            int y = (0xff & ((int) yuv[a]));
            y = y < 16 ? 16 : y;

            int r = (int) (1.164f * (y - 16) + 1.596f);
            int g = (int) (1.164f * (y - 16) - 0.813f);
            int b = (int) (1.164f * (y - 16) + 2.018f);

            r = r > 255 ? 255 : r;
            g = g > 255 ? 255 : g;
            b = b > 255 ? 255 : b;
            int combinedValue = r + g + b;
            if (combinedValue > maxValue) {
                maxValue = combinedValue;
            } else if (combinedValue < minValue) {
                minValue = combinedValue;
            }
            convertedData[a++] = combinedValue;
        }
        return convertedData;
    }
    
    public static void setFontSize(float fontSize) {
        AsciiView.fontSize = fontSize;
    }
    
    public static float getFontSize() {
        return fontSize;
    }

    public void setCameraSize(Size cameraSize) {
        this.cameraSize = cameraSize;
    }

    public static File getMediaStorageDir() {
        return mediaStorageDir;
    }

    public static void setMediaStorageDir(File mediaStorageDir) {
        AsciiView.mediaStorageDir = mediaStorageDir;
    }
}
