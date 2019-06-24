/*===============================================================================
Copyright (c) 2019 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.engine.CoreSamples.app.ImageTargets;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Vector;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import android.widget.Button;

import com.vuforia.Device;
import com.vuforia.DeviceTrackableResult;
import com.vuforia.ImageTargetResult;
import com.vuforia.Matrix44F;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.Trackable;
import com.vuforia.TrackableResult;
import com.vuforia.TrackableResultList;
import com.vuforia.Vuforia;
import com.vuforia.engine.CoreSamples.R;
import com.vuforia.engine.CoreSamples.app.ImageTargets.OBJParser.ObjParser;
import com.vuforia.engine.SampleApplication.SampleAppRenderer;
import com.vuforia.engine.SampleApplication.SampleAppRendererControl;
import com.vuforia.engine.SampleApplication.SampleRendererBase;
import com.vuforia.engine.SampleApplication.SampleApplicationSession;
import com.vuforia.engine.SampleApplication.utils.CubeObject;
import com.vuforia.engine.SampleApplication.utils.CubeShaders;
import com.vuforia.engine.SampleApplication.utils.LoadingDialogHandler;
import com.vuforia.engine.SampleApplication.utils.MeshObject;
import com.vuforia.engine.SampleApplication.utils.SampleApplication3DModel;
import com.vuforia.engine.SampleApplication.utils.SampleMath;
import com.vuforia.engine.SampleApplication.utils.SampleUtils;
import com.vuforia.engine.SampleApplication.utils.Teapot;
import com.vuforia.engine.SampleApplication.utils.Texture;

import static com.vuforia.HINT.HINT_MAX_SIMULTANEOUS_IMAGE_TARGETS;


/**
 * The renderer class for the Image Targets sample.
 *
 * In the renderFrame() function you can render augmentations to display over the Target
 */
public class ImageTargetRenderer extends SampleRendererBase implements SampleAppRendererControl
{
    private static final String LOGTAG = "ImageTargetRenderer";
    public static int flower_st = 0;
    public static int animal_st = 0;
    public static int apple_st=0;
    public static float z_move=0.0f;

    private final WeakReference<ImageTargets> mActivityRef;


    private int shaderProgramID;
    private int vertexHandle;
    private int textureCoordHandle;
    private int mvpMatrixHandle;
    private int texSampler2DHandle;
    private int number = 20;
    private int a=0;

    private float birdmove=0.0f;
    private float cloudmove1=0.0f;
    private float cloudmove2=0.0f;
    private float tutrlemove1=0.0f;
    private float applemove=2.5f;
    private float applemove2=2.5f;
    private float applemove3=2.5f;
    private float applemove4=2.5f;
    private float applemove5=2.5f;
    private float applemove6=2.5f;


    // Object to be rendered
    private Teapot mTeapot;
    private CubeObject mCube;
    private OBJLoader[] mObj = new OBJLoader[number];
    static public int objectSel = 0;

    private static final float BUILDING_SCALE = 0.012f;
    private SampleApplication3DModel mBuildingsModel;

    private boolean mModelIsLoaded = false;
    private boolean mIsTargetCurrentlyTracked = false;

    private static final float OBJECT_SCALE_FLOAT = 0.003f;

    ImageTargetRenderer(ImageTargets activity, SampleApplicationSession session)
    {
        mActivityRef = new WeakReference<>(activity);
        vuforiaAppSession = session;

        // SampleAppRenderer used to encapsulate the use of RenderingPrimitives setting
        // the device mode AR/VR and stereo mode
        mSampleAppRenderer = new SampleAppRenderer(this, mActivityRef.get(), Device.MODE.MODE_AR, vuforiaAppSession.getVideoMode(),
                false, 0.01f , 5f);
    }


    public void updateRenderingPrimitives()
    {
        mSampleAppRenderer.updateRenderingPrimitives();
    }


    public void setActive(boolean active)
    {
        mSampleAppRenderer.setActive(active);
    }


    // The render function.
    // This function is called from the SampleAppRenderer by using the RenderingPrimitives views.
    // The state is owned by SampleAppRenderer which is controlling its lifecycle.
    // NOTE: State should not be cached outside this method.
    public void renderFrame(State state, float[] projectionMatrix)
    {
        // Renders video background replacing Renderer.DrawVideoBackground()
        mSampleAppRenderer.renderVideoBackground(state);

        // Set the device pose matrix as identity
        Matrix44F devicePoseMatrix = SampleMath.Matrix44FIdentity();
        Matrix44F modelMatrix;

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glFrontFace(GLES20.GL_CCW);   // Back camera

        // Read device pose from the state and create a corresponding view matrix (inverse of the device pose)
        if (state.getDeviceTrackableResult() != null)
        {
            int statusInfo = state.getDeviceTrackableResult().getStatusInfo();
            int trackerStatus = state.getDeviceTrackableResult().getStatus();

            mActivityRef.get().checkForRelocalization(statusInfo);

            if (trackerStatus != TrackableResult.STATUS.NO_POSE)
            {
                modelMatrix = Tool.convertPose2GLMatrix(state.getDeviceTrackableResult().getPose());

                // We transpose here because Matrix44FInverse returns a transposed matrix
                devicePoseMatrix = SampleMath.Matrix44FTranspose(SampleMath.Matrix44FInverse(modelMatrix));
            }
        }

        TrackableResultList trackableResultList = state.getTrackableResults();

        // Determine if target is currently being tracked
        setIsTargetCurrentlyTracked(trackableResultList);

        // Iterate through trackable results and render any augmentations
        for (TrackableResult result : trackableResultList)
        {
            Trackable trackable = result.getTrackable();

            if (result.isOfType(ImageTargetResult.getClassType()))
            {
                int textureIndex;
                modelMatrix = Tool.convertPose2GLMatrix(result.getPose());

                textureIndex = trackable.getName().equalsIgnoreCase("stones") ? 0
                        : 1;
                textureIndex = trackable.getName().equalsIgnoreCase("tarmac") ? 2
                        : textureIndex;

                textureIndex = mActivityRef.get().isDeviceTrackingActive() ? 3 : textureIndex;


                // textureIndex = objectSel == 1 ? 4 : textureIndex;
                if(objectSel == 1) textureIndex = 4;
                else if(objectSel == 2) textureIndex = 5;

                renderModel(projectionMatrix, devicePoseMatrix.getData(), modelMatrix.getData(), textureIndex);

                SampleUtils.checkGLError("Image Targets renderFrame");
            }
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
    }

    @Override
    public void initRendering()
    {
        Vuforia.setHint(HINT_MAX_SIMULTANEOUS_IMAGE_TARGETS , 4);
        if (mTextures == null)
        {
            return;
        }

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
                : 1.0f);

        for (Texture t : mTextures)
        {
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, t.mData);
        }

        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
                CubeShaders.CUBE_MESH_VERTEX_SHADER,
                CubeShaders.CUBE_MESH_FRAGMENT_SHADER);

        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexPosition");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "modelViewProjectionMatrix");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "texSampler2D");

        if(!mModelIsLoaded)
        {
            mTeapot = new Teapot();
            mCube = new CubeObject();
            ObjParser objParser1 = new ObjParser(mActivityRef.get());
            try{
                objParser1.parse(R.raw.tree);
            }
            catch(IOException e){
                e.printStackTrace();
            }
            mObj[0] = new OBJLoader(objParser1);

            ObjParser objParser2 = new ObjParser(mActivityRef.get());
            try{
                objParser2.parse(R.raw.flower);
            }
            catch(IOException e){
                e.printStackTrace();
            }
            mObj[1] = new OBJLoader(objParser2);

            ObjParser objParser3 = new ObjParser(mActivityRef.get());
            try{
                objParser3.parse(R.raw.bird);
            }
            catch(IOException e){
                e.printStackTrace();
            }
            mObj[2] = new OBJLoader(objParser3);

            ObjParser objParser4 = new ObjParser(mActivityRef.get());
            try{
                objParser4.parse(R.raw.cloud);
            }
            catch(IOException e){
                e.printStackTrace();
            }
            mObj[3] = new OBJLoader(objParser4);

            ObjParser objParser5 = new ObjParser(mActivityRef.get());
            try{
                objParser5.parse(R.raw.cloud2);
            }
            catch(IOException e){
                e.printStackTrace();
            }
            mObj[4] = new OBJLoader(objParser5);

            ObjParser objParser6 = new ObjParser(mActivityRef.get());
            try{
                objParser6.parse(R.raw.dog);
            }
            catch(IOException e){
                e.printStackTrace();
            }
            mObj[5] = new OBJLoader(objParser6);

            ObjParser objParser7 = new ObjParser(mActivityRef.get());
            try{
                objParser7.parse(R.raw.apple);
            }
            catch(IOException e){
                e.printStackTrace();
            }
            mObj[6] = new OBJLoader(objParser7);

            ObjParser objParser8 = new ObjParser(mActivityRef.get());
            try{
                objParser8.parse(R.raw.sea);
            }
            catch(IOException e){
                e.printStackTrace();
            }
            mObj[7] = new OBJLoader(objParser8);

            ObjParser objParser9 = new ObjParser(mActivityRef.get());
            try{
                objParser9.parse(R.raw.tutrle);
            }
            catch(IOException e){
                e.printStackTrace();
            }
            mObj[8] = new OBJLoader(objParser9);

            ObjParser objParser10 = new ObjParser(mActivityRef.get());
            try{
                objParser10.parse(R.raw.boat);
            }
            catch(IOException e){
                e.printStackTrace();
            }
            mObj[9] = new OBJLoader(objParser10);

            try {
                mBuildingsModel = new SampleApplication3DModel();
                mBuildingsModel.loadModel(mActivityRef.get().getResources().getAssets(),
                        "ImageTargets/Buildings.txt");
                mModelIsLoaded = true;
            } catch (IOException e)
            {
                Log.e(LOGTAG, "Unable to load buildings");
            }

            // Hide the Loading Dialog
            mActivityRef.get().loadingDialogHandler
                    .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
        }
    }


    private void renderModel(float[] projectionMatrix, float[] viewMatrix, float[] modelMatrix, int textureIndex)
    {
        MeshObject model;
        float[] modelViewProjection = new float[16];
        float[] translationMatrix = new float[16];
        float[] modelViewMatrix_flower = new float[16];
        float[] tmp_modelMatrix = new float[16];

        // Apply local transformation to our model
        if (mActivityRef.get().isDeviceTrackingActive())
        {
            Matrix.translateM(modelMatrix, 0, 0, -0.06f, 0);
            Matrix.rotateM(modelMatrix, 0, 90.0f, 1.0f, 0, 0);
            Matrix.scaleM(modelMatrix, 0, BUILDING_SCALE, BUILDING_SCALE, BUILDING_SCALE);

            model = mBuildingsModel;
        }
        else
        {
            if(objectSel == 0) {
                Matrix.translateM(modelMatrix, 0, 0, 0, OBJECT_SCALE_FLOAT);
                Matrix.scaleM(modelMatrix, 0, OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT);
                model = mTeapot;
            }else if(objectSel == 1) {
                Matrix.translateM(modelMatrix, 0, 0, 0, 0.01f);
                Matrix.scaleM(modelMatrix, 0, 0.01f, 0.01f, 0.01f);
                model = mCube;
            } else {
                Matrix.scaleM(modelMatrix, 0, 0.02f, 0.02f, 0.02f);
                Matrix.rotateM(modelMatrix,0,90,0.01f,0,0);
                model = mObj[0];
            }

        }

        // Combine device pose (view matrix) with model matrix
        Matrix.multiplyMM(modelMatrix, 0, viewMatrix, 0, modelMatrix, 0);

        // Do the final combination with the projection matrix
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelMatrix, 0);

        // Activate the shader program and bind the vertex and tex coords
        GLES20.glUseProgram(shaderProgramID);

        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, model.getVertices());
        GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, model.getTexCoords());

        GLES20.glEnableVertexAttribArray(vertexHandle);
        GLES20.glEnableVertexAttribArray(textureCoordHandle);

        // Activate texture 0, bind it, pass to shader
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(textureIndex).mTextureID[0]);
        GLES20.glUniform1i(texSampler2DHandle, 0);

        // Pass the model view matrix to the shader
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);

        // Finally draw the model
        if (mActivityRef.get().isDeviceTrackingActive())
        {
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, model.getNumObjectVertex());
        }
        else
        {
                Log.d("draw","draw1");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, model.getNumObjectVertex());

            if(flower_st == 1){
                textureIndex++;
                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.translateM(translationMatrix, 0, 3f, 0.5f, 0f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[1].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[1].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(textureIndex).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[1].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.translateM(translationMatrix, 0, -3f, 0.5f, 0f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[1].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[1].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(textureIndex).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[1].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.translateM(translationMatrix, 0, -2f, 0.5f, 3f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[1].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[1].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(textureIndex).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[1].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.translateM(translationMatrix, 0, 2f, 0.5f, 3f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[1].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[1].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(textureIndex).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[1].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.translateM(translationMatrix, 0, 2f, 0.5f, -3f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[1].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[1].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(textureIndex).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[1].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.translateM(translationMatrix, 0, -2f, 0.5f, -3f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[1].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[1].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(textureIndex).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[1].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.translateM(translationMatrix, 0, 0f, 0.5f, 2f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[1].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[1].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(textureIndex).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[1].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.translateM(translationMatrix, 0, 0.5f, 0.5f, 1f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[1].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[1].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(textureIndex).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[1].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.translateM(translationMatrix, 0, -0.3f, 0.5f, -0.7f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[1].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[1].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(textureIndex).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[1].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.translateM(translationMatrix, 0, 0.7f, 0.5f, -0.6f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[1].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[1].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(textureIndex).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[1].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                textureIndex++;
                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.translateM(translationMatrix, 0, 3.5f, 0.5f, 3.5f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[1].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[1].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(textureIndex).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[1].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.translateM(translationMatrix, 0, -3.5f, 0.5f, -3.5f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[1].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[1].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(textureIndex).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[1].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.translateM(translationMatrix, 0, 3.5f, 0.5f, -3.5f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[1].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[1].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(textureIndex).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[1].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.translateM(translationMatrix, 0, -3.5f, 0.5f, 3.5f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[1].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[1].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(textureIndex).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[1].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.rotateM(modelMatrix,0,90,0f,0.01f,0f);
                Matrix.translateM(translationMatrix, 0, -2.5f, 7f, -3.5f+cloudmove1);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[3].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[3].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(9).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[3].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.rotateM(modelMatrix,0,90,0.01f,0f,0f);
                Matrix.translateM(translationMatrix, 0, -2.5f+cloudmove2, 2.5f, -6.5f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[4].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[4].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(9).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[4].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.rotateM(modelMatrix,0,90,0.01f,0f,0f);
                Matrix.translateM(translationMatrix, 0, -2.5f+cloudmove2, -15f, -6.5f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[4].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[4].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(9).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[4].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.rotateM(modelMatrix,0,90,0.01f,0f,0f);
                Matrix.translateM(translationMatrix, 0, 2.5f+cloudmove2, -15f, -6.5f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[4].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[4].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(9).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[4].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.rotateM(modelMatrix,0,90,0f,0.01f,0f);
                Matrix.translateM(translationMatrix, 0, 15f, 7f, -3.5f+cloudmove1);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[3].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[3].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(9).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[3].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.rotateM(modelMatrix,0,90,0f,0.01f,0f);
                Matrix.translateM(translationMatrix, 0, 15f, 7f, 5f+cloudmove1);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[3].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[3].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(9).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[3].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                //Matrix.rotateM(modelMatrix,0,90,0.01f,0f,0f);
                Matrix.translateM(translationMatrix, 0, 0f, 0f, -5f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.6f, 0.6f, 0.6f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[7].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[7].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(16).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[7].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                //Matrix.rotateM(modelMatrix,0,90,0.01f,0f,0f);
                Matrix.translateM(translationMatrix, 0, 0f, 0f, -15f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.6f, 0.6f, 0.6f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[7].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[7].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(15).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[7].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                //Matrix.rotateM(modelMatrix,0,90,0.01f,0f,0f);
                Matrix.translateM(translationMatrix, 0, z_move, 0f, -15f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.6f, 0.6f, 0.6f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[9].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[9].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(18).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[9].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                if(a==0){
                    cloudmove1 += 0.08f;
                    if (cloudmove1 >= 5.0f) {
                        a=1;
                    }
                }
                else{
                    cloudmove1 -= 0.08f;
                    if(cloudmove1 <= 0f){
                        a=0;
                    }
                }

                if(a==0){
                    cloudmove2 += 0.03f;
                    if (cloudmove2 >= 20.0f) {
                        a=1;
                    }
                }
                else{
                    cloudmove2 -= 0.03f;
                    if(cloudmove2 <= 0f){
                        a=0;
                    }
                }
            }
            if(animal_st==1){
                //textureIndex++;

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.rotateM(modelMatrix,0,90,0f,0.01f,0f);
                Matrix.translateM(translationMatrix, 0, 17f, 0.5f, -4f+tutrlemove1);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.5f, 0.5f, 0.5f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[8].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[8].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(17).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dolphin","dolphin");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[8].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.rotateM(modelMatrix,0,90,0f,0.01f,0f);
                Matrix.translateM(translationMatrix, 0, 14f, 0.5f, -4f+tutrlemove1);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.3f, 0.3f, 0.3f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[8].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[8].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(17).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dolphin","dolphin");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[8].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.translateM(translationMatrix, 0, 2f, 0.5f, 1f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.9f, 0.9f, 0.9f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[5].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[5].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(10).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[5].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.translateM(translationMatrix, 0, 2.5f, 0.5f, 1.2f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.7f, 0.7f, 0.7f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[5].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[5].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(11).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[5].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.translateM(translationMatrix, 0, 2.7f, 0.5f, 1.4f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.5f, 0.5f, 0.5f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[5].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[5].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(12).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[5].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.translateM(translationMatrix, 0, 2.9f, 0.5f, 1.6f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.3f, 0.3f, 0.3f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[5].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[5].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(13).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[5].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.rotateM(modelMatrix,0,birdmove,0f,-0.01f,0);
                Matrix.translateM(translationMatrix, 0, 3f, 3.5f, 3f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[2].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[2].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(8).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[2].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                birdmove+=2.0f;
                if (birdmove >= 360.0f) {
                    birdmove -= 360.0f;
                }
                tutrlemove1 += 0.05f;
                if (cloudmove1 >= 5.0f) {
                    tutrlemove1 = 0f;
                }
            }
            if(apple_st >=1){
                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.translateM(translationMatrix, 0, -1.3f, applemove, 0.7f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[6].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[6].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(14).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[6].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

               applemove-=0.2f;
                if (applemove <= 0.5f) {
                    applemove =0.5f;
                }
            }
            if(apple_st >=2){
                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.translateM(translationMatrix, 0, -1.0f, applemove2, 0f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[6].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[6].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(14).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[6].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                applemove2-=0.2f;
                if (applemove2 <= 0.5f) {
                    applemove2 =0.5f;
                }
            }

            if(apple_st >=3){
                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.translateM(translationMatrix, 0, -0.5f, applemove3, 0.9f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[6].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[6].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(14).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[6].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                applemove3-=0.2f;
                if (applemove3 <= 0.5f) {
                    applemove3 =0.5f;
                }
            }

            if(apple_st >=4){
                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.translateM(translationMatrix, 0, -0.2f, applemove4, 0.2f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[6].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[6].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(14).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[6].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                applemove4-=0.2f;
                if (applemove4 <= 0.5f) {
                    applemove4 =0.5f;
                }
            }

            if(apple_st >=5){
                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.translateM(translationMatrix, 0, 0.4f, applemove5, 1f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[6].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[6].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(14).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[6].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                applemove5-=0.2f;
                if (applemove5 <= 0.5f) {
                    applemove5 =0.5f;
                }
            }

            if(apple_st >=6){
                System.arraycopy(modelMatrix, 0, tmp_modelMatrix, 0, modelMatrix.length);
                Matrix.setIdentityM(translationMatrix,0);
                Matrix.translateM(translationMatrix, 0, 0.9f, applemove6, 0.1f);
                Matrix.multiplyMM(modelViewMatrix_flower,0,modelMatrix,0,translationMatrix,0);
                Matrix.scaleM(modelViewMatrix_flower, 0, 0.4f, 0.4f, 0.4f);
                Matrix.multiplyMM(modelViewProjection,0,projectionMatrix,0,modelViewMatrix_flower,0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, mObj[6].getVertices());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mObj[6].getTexCoords());
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(14).mTextureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjection, 0);
                Log.d("dra2","draw2");
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[6].getNumObjectVertex());
                System.arraycopy(tmp_modelMatrix, 0, modelMatrix, 0, tmp_modelMatrix.length);

                applemove6-=0.2f;
                if (applemove6 <= 0.5f) {
                    applemove6 =0.5f;
                }
            }
        }

        // Disable the enabled arrays
        GLES20.glDisableVertexAttribArray(vertexHandle);
        GLES20.glDisableVertexAttribArray(textureCoordHandle);
    }


    public void setTextures(Vector<Texture> textures)
    {
        mTextures = textures;
    }


    private void setIsTargetCurrentlyTracked(TrackableResultList trackableResultList)
    {
        for(TrackableResult result : trackableResultList)
        {
            // Check the tracking status for result types
            // other than DeviceTrackableResult. ie: ImageTargetResult
            if (!result.isOfType(DeviceTrackableResult.getClassType()))
            {
                int currentStatus = result.getStatus();
                int currentStatusInfo = result.getStatusInfo();

                // The target is currently being tracked if the status is TRACKED|NORMAL
                if (currentStatus == TrackableResult.STATUS.TRACKED
                        || currentStatusInfo == TrackableResult.STATUS_INFO.NORMAL)
                {
                    mIsTargetCurrentlyTracked = true;
                    return;
                }
            }
        }

        mIsTargetCurrentlyTracked = false;
    }


    boolean isTargetCurrentlyTracked()
    {
        return mIsTargetCurrentlyTracked;
    }
}