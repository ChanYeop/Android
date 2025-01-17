/*===============================================================================
Copyright (c) 2019 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.engine.CoreSamples.app.CylinderTargets;

import java.io.IOException;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.vuforia.CylinderTargetResult;
import com.vuforia.Device;
import com.vuforia.Matrix44F;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.TrackableResult;
import com.vuforia.TrackableResultList;
import com.vuforia.Vuforia;
import com.vuforia.engine.SampleApplication.SampleAppRenderer;
import com.vuforia.engine.SampleApplication.SampleAppRendererControl;
import com.vuforia.engine.SampleApplication.SampleApplicationSession;
import com.vuforia.engine.SampleApplication.SampleRendererBase;
import com.vuforia.engine.SampleApplication.utils.CubeShaders;
import com.vuforia.engine.SampleApplication.utils.LoadingDialogHandler;
import com.vuforia.engine.SampleApplication.utils.SampleApplication3DModel;
import com.vuforia.engine.SampleApplication.utils.SampleUtils;
import com.vuforia.engine.SampleApplication.utils.Texture;


/**
 * The renderer class for the CylinderTargets sample.
 *
 * In the renderFrame() function you can render augmentations to display over the Target
 */
public class CylinderTargetRenderer extends SampleRendererBase implements SampleAppRendererControl
{
    private static final String LOGTAG = "CylinderTargetRenderer";
    
    // Reference to main activity
    private final CylinderTargets mActivity;

    private int shaderProgramID;
    private int vertexHandle;
    private int textureCoordHandle;
    private int mvpMatrixHandle;
    private int texSampler2DHandle;
    
    private Renderer mRenderer;

    // Cylinder model which we use to rotate an object around
    private CylinderModel mCylinderModel;
    
    // dimensions of the cylinder (as set in the TMS tool)
    private final float kCylinderHeight = 0.095f;
    private final float kCylinderTopDiameter = 0.065f;
    private final float kCylinderBottomDiameter = 0.065f;
    
    // the height of the tea pot
    private final float kObjectHeight = 1.0f;
    
    // we want the object to be the 1/3 of the height of the cylinder
    private final float kRatioCylinderObjectHeight = 3.0f;
    
    // Scaling of the object to match the ratio we want
    private final float kObjectScale = kCylinderHeight
        / (kRatioCylinderObjectHeight * kObjectHeight);

    // scaling of the cylinder model to fit the actual cylinder
    private final float kCylinderScaleX = kCylinderBottomDiameter / 2.0f;
    private final float kCylinderScaleY = kCylinderBottomDiameter / 2.0f;
    private final float kCylinderScaleZ = kCylinderHeight;

    // Ball that we use to rotate around the cylinder
    private SampleApplication3DModel mSphereModel;
    
    private double prevTime;
    private float rotateBallAngle;

    private boolean mModelIsLoaded = false;
    
    
    CylinderTargetRenderer(CylinderTargets activity,
        SampleApplicationSession session)
    {
        mActivity = activity;
        vuforiaAppSession = session;

        // SampleAppRenderer used to encapsulate the use of RenderingPrimitives setting
        // the device mode AR/VR and stereo mode
        mSampleAppRenderer = new SampleAppRenderer(this, mActivity, Device.MODE.MODE_AR,
                vuforiaAppSession.getVideoMode(), false, 0.010f, 5f);
    }


    public void setActive(boolean active)
    {
        mSampleAppRenderer.setActive(active);
    }


    @Override
    public void initRendering()
    {
        // Define clear color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
            : 1.0f);

        mRenderer = Renderer.getInstance();

        // Now generate the OpenGL texture objects and add settings
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
        SampleUtils.checkGLError("CylinderTargets GLInitRendering");

        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
            CubeShaders.CUBE_MESH_VERTEX_SHADER,
            CubeShaders.CUBE_MESH_FRAGMENT_SHADER);
        SampleUtils.checkGLError("GLInitRendering");
        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
            "vertexPosition");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
            "vertexTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
            "modelViewProjectionMatrix");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
            "texSampler2D");
        SampleUtils.checkGLError("GLInitRendering due");
        SampleUtils
            .checkGLError("CylinderTargets GLInitRendering getting location att and unif");

        if(!mModelIsLoaded)
        {
            try {
                mSphereModel = new SampleApplication3DModel();
                mSphereModel.loadModel(mActivity.getResources().getAssets(),
                        "CylinderTargets/Sphere.txt");
                mModelIsLoaded = true;
            } catch (IOException e) {
                Log.e(LOGTAG, "Unable to load soccer ball");
            }

            prevTime = System.currentTimeMillis();
            rotateBallAngle = 0;

            // ratio between top and bottom diameter
            // used to generate the model of the cylinder
            float kCylinderTopRadiusRatio = kCylinderTopDiameter / kCylinderBottomDiameter;

            mCylinderModel = new CylinderModel(kCylinderTopRadiusRatio);

            // Hide the Loading Dialog
            mActivity.loadingDialogHandler
                    .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
        }
    }


    public void updateRenderingPrimitives()
    {
        mSampleAppRenderer.updateRenderingPrimitives();
    }


    // The render function.
    // This function is called from the SampleAppRenderer by using the RenderingPrimitives views.
    // The state is owned by SampleAppRenderer which is controlling its lifecycle.
    // NOTE: State should not be cached outside this method.
    public void renderFrame(State state, float[] projectionMatrix)
    {
        // Renders video background replacing Renderer.DrawVideoBackground()
        mSampleAppRenderer.renderVideoBackground(state);
        
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        SampleUtils.checkGLError("CylinderTargets drawVideoBackground");
        
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);

        // Did we find any trackables this frame?
        TrackableResultList trackableResultList = state.getTrackableResults();
        for (TrackableResult result : trackableResultList)
        {
            if (!result.isOfType(CylinderTargetResult.getClassType()))
                continue;
            
            Matrix44F modelViewMatrix_Vuforia;
            float[] modelViewProjection = new float[16];
            
            // prepare the cylinder
            modelViewMatrix_Vuforia = Tool.convertPose2GLMatrix(result
                .getPose());
            float[] modelViewMatrix = modelViewMatrix_Vuforia.getData();
            
            Matrix.scaleM(modelViewMatrix, 0, kCylinderScaleX, kCylinderScaleY,
                kCylinderScaleZ);
            Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewMatrix, 0);
            SampleUtils.checkGLError("CylinderTargets prepareCylinder");
            
            GLES20.glUseProgram(shaderProgramID);
            
            // Draw the cylinder:
            
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            GLES20.glCullFace(GLES20.GL_BACK);

            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                false, 0, mCylinderModel.getVertices());
            GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                GLES20.GL_FLOAT, false, 0, mCylinderModel.getTexCoords());
            
            GLES20.glEnableVertexAttribArray(vertexHandle);
            GLES20.glEnableVertexAttribArray(textureCoordHandle);
            
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                mTextures.get(0).mTextureID[0]);
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                modelViewProjection, 0);
            GLES20.glUniform1i(texSampler2DHandle, 0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                mCylinderModel.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                mCylinderModel.getIndices());
            
            GLES20.glDisable(GLES20.GL_CULL_FACE);
            SampleUtils.checkGLError("CylinderTargets drawCylinder");
            
            // prepare the object
            modelViewMatrix = Tool.convertPose2GLMatrix(result.getPose())
                .getData();

            // draw the anchored object
            animateObject(modelViewMatrix);
            
            // we move away the object from the target
            Matrix.translateM(modelViewMatrix, 0, 1.0f * kCylinderTopDiameter,
                0.0f, kObjectScale);
            Matrix.scaleM(modelViewMatrix, 0, kObjectScale, kObjectScale,
                kObjectScale);
            
            Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewMatrix, 0);
            
            GLES20.glUseProgram(shaderProgramID);
            
            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                false, 0, mSphereModel.getVertices());
            GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                GLES20.GL_FLOAT, false, 0, mSphereModel.getTexCoords());
            
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                mTextures.get(1).mTextureID[0]);
            GLES20.glUniform1i(texSampler2DHandle, 0);
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                modelViewProjection, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0,
                mSphereModel.getNumObjectVertex());
            
            GLES20.glDisableVertexAttribArray(vertexHandle);
            GLES20.glDisableVertexAttribArray(textureCoordHandle);
            
            SampleUtils.checkGLError("CylinderTargets renderFrame");
        }
        
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        
        mRenderer.end();
    }
    
    
    private void animateObject(float[] modelViewMatrix)
    {
        double time = System.currentTimeMillis();             // Get real time difference
        float dt = (float) (time - prevTime) / 1000;          // from frame to frame
        
        rotateBallAngle += dt * 180.0f / 3.1415f;     // Animate angle based on time
        rotateBallAngle %= 360;
        
        Matrix.rotateM(modelViewMatrix, 0, rotateBallAngle, 0.0f, 0.0f, 1.0f);
        
        prevTime = time;
    }
    
    
    public void setTextures(Vector<Texture> textures)
    {
        mTextures = textures;
        
    }
}
