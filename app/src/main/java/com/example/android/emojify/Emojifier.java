package com.example.android.emojify;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.security.PrivilegedAction;

import timber.log.Timber;

/**
 * Created by UrJasWitK on 29-Apr-17.
 */

public class Emojifier {

    //private static final String LOG_TAG = Emojifier.class.getCanonicalName();

    private static final double SMILING_PROB_THRESHOLD = 0.15;
    private static final double EYE_OPEN_PROB_THRESHOLD = .5;
    private static final float EMOJI_SCALE_FACTOR = .9f;

    private enum Emoji{
        FROWN,
        CLOSED_EYE_FROWN,
        CLOSED_EYE_SMILE,
        LEFT_WINK,
        RIGHT_WINK,
        SMILE,
        RIGHT_WINK_FROWN,
        LEFT_WINK_FROWN
    }

    public static Bitmap detectFacesAndOverlayEmoji(Context context, Bitmap img){
        FaceDetector detector = new FaceDetector.Builder(context)
                .setTrackingEnabled(false)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();
        Frame frame = new Frame.Builder().setBitmap(img).build();

        SparseArray<Face> faces = detector.detect(frame);

        Bitmap resultBitmap  = img;

        Timber.v("no. of faces is: "+ faces.size());
        //Log.v(LOG_TAG, "no.of faces is: " + faces.size());

        if (faces.size() == 0)
            Toast.makeText(context, "no faces detected", Toast.LENGTH_SHORT).show();
        for (int i=0; i < faces.size(); i++){
            Bitmap emojiBitmap;
            switch (whichEmoji(context, faces.get(i))){
                case LEFT_WINK: emojiBitmap = BitmapFactory.decodeResource(
                        context.getResources(), R.drawable.leftwink);
                    break;
                case RIGHT_WINK: emojiBitmap = BitmapFactory.decodeResource(
                        context.getResources(), R.drawable.rightwink);
                    break;
                case SMILE: emojiBitmap = BitmapFactory.decodeResource(
                        context.getResources(), R.drawable.smile);
                    break;
                case FROWN: emojiBitmap = BitmapFactory.decodeResource(
                        context.getResources(), R.drawable.frown);
                    break;
                case LEFT_WINK_FROWN: emojiBitmap = BitmapFactory.decodeResource(
                        context.getResources(), R.drawable.leftwinkfrown);
                    break;
                case RIGHT_WINK_FROWN: emojiBitmap = BitmapFactory.decodeResource(
                        context.getResources(), R.drawable.rightwinkfrown);
                    break;
                case CLOSED_EYE_FROWN: emojiBitmap = BitmapFactory.decodeResource(
                        context.getResources(), R.drawable.closed_frown);
                    break;
                case CLOSED_EYE_SMILE: emojiBitmap = BitmapFactory.decodeResource(
                        context.getResources(), R.drawable.closed_smile);
                    break;
                default: emojiBitmap=null;
            }
            resultBitmap =addBitmapToFace(resultBitmap, emojiBitmap, faces.get(i));
        }
        detector.release();
        return resultBitmap;
    }

    /**
     * Combines the original picture with the emoji bitmaps
     *
     * @param backgroundBitmap The original picture
     * @param emojiBitmap      The chosen emoji
     * @param face             The detected face
     * @return The final bitmap, including the emojis over the faces
     */
    private static Bitmap addBitmapToFace(Bitmap backgroundBitmap, Bitmap emojiBitmap, Face face) {

        // Initialize the results bitmap to be a mutable copy of the original image
        Bitmap resultBitmap = Bitmap.createBitmap(backgroundBitmap.getWidth(),
                backgroundBitmap.getHeight(), backgroundBitmap.getConfig());

        // Scale the emoji so it looks better on the face
        float scaleFactor = EMOJI_SCALE_FACTOR;

        // Determine the size of the emoji to match the width of the face and preserve aspect ratio
        int newEmojiWidth = (int) (face.getWidth() * scaleFactor);
        int newEmojiHeight = (int) (emojiBitmap.getHeight() *
                newEmojiWidth / emojiBitmap.getWidth() * scaleFactor);


        // Scale the emoji
        emojiBitmap = Bitmap.createScaledBitmap(emojiBitmap, newEmojiWidth, newEmojiHeight, false);

        // Determine the emoji position so it best lines up with the face
        float emojiPositionX =
                (face.getPosition().x + face.getWidth() / 2) - emojiBitmap.getWidth() / 2;
        float emojiPositionY =
                (face.getPosition().y + face.getHeight() / 2) - emojiBitmap.getHeight() / 3;

        // Create the canvas and draw the bitmaps to it
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        canvas.drawBitmap(emojiBitmap, emojiPositionX, emojiPositionY, null);

        return resultBitmap;
    }

    public static Emoji whichEmoji(Context context, Face face){
        boolean leftEyeOpen = false, rightEyeOpen = false, smilingFace = false;
        if (face.getIsLeftEyeOpenProbability() > EYE_OPEN_PROB_THRESHOLD){
            leftEyeOpen = true;
            Toast.makeText(context, "Left eye open", Toast.LENGTH_SHORT).show();
        }
        if (face.getIsRightEyeOpenProbability() > EYE_OPEN_PROB_THRESHOLD){
            rightEyeOpen = true;
            Toast.makeText(context, "Right eye open", Toast.LENGTH_SHORT).show();
        }
        if (face.getIsSmilingProbability() > SMILING_PROB_THRESHOLD){
            smilingFace = true;
            Toast.makeText(context, "Smiling face", Toast.LENGTH_SHORT).show();
        }
       // Log.v(LOG_TAG, "left eye open probability is: " + face.getIsLeftEyeOpenProbability());
        //Log.v(LOG_TAG, "right eye open probability is: " + face.getIsRightEyeOpenProbability());
        //Log.v(LOG_TAG, "Smiling probability is: " + face.getIsSmilingProbability());

        Timber.v("left eye open probability is: " + face.getIsLeftEyeOpenProbability());
        Timber.v("right eye open probability is: " + face.getIsRightEyeOpenProbability());
        Timber.v("Smiling probability is: " + face.getIsSmilingProbability());

        Emoji emoji;

        if (leftEyeOpen && !rightEyeOpen)
            emoji = (smilingFace)? Emoji.RIGHT_WINK: Emoji.RIGHT_WINK_FROWN ;
        else if (!leftEyeOpen && rightEyeOpen)
            emoji = (smilingFace)? Emoji.LEFT_WINK: Emoji.LEFT_WINK_FROWN ;
        else if (!leftEyeOpen)
            emoji = (smilingFace)? Emoji.CLOSED_EYE_SMILE: Emoji.CLOSED_EYE_FROWN ;
        else
            emoji = (smilingFace)? Emoji.SMILE: Emoji.FROWN;

        //Log.v(LOG_TAG, "Emoji selected : " + emoji.name());
        Timber.v("Emoji selected : " + emoji.name());
        return emoji;
    }
}
