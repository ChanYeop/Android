/*===============================================================================
Copyright (c) 2019 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.engine.CoreSamples.app.ImageTargets;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.vuforia.CameraDevice;
import com.vuforia.DataSet;
import com.vuforia.DeviceTracker;
import com.vuforia.ObjectTracker;
import com.vuforia.PositionalDeviceTracker;
import com.vuforia.STORAGE_TYPE;
import com.vuforia.State;
import com.vuforia.Trackable;
import com.vuforia.TrackableList;
import com.vuforia.TrackableResult;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.Vuforia;
import com.vuforia.engine.CoreSamples.ui.SampleAppMessage;
import com.vuforia.engine.SampleApplication.SampleActivityBase;
import com.vuforia.engine.SampleApplication.utils.SampleAppTimer;
import com.vuforia.engine.SampleApplication.SampleApplicationControl;
import com.vuforia.engine.SampleApplication.SampleApplicationException;
import com.vuforia.engine.SampleApplication.SampleApplicationSession;
import com.vuforia.engine.SampleApplication.utils.LoadingDialogHandler;
import com.vuforia.engine.SampleApplication.utils.SampleApplicationGLView;
import com.vuforia.engine.SampleApplication.utils.Texture;
import com.vuforia.engine.CoreSamples.R;
import com.vuforia.engine.CoreSamples.ui.SampleAppMenu.SampleAppMenu;
import com.vuforia.engine.CoreSamples.ui.SampleAppMenu.SampleAppMenuGroup;
import com.vuforia.engine.CoreSamples.ui.SampleAppMenu.SampleAppMenuInterface;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Vector;

/**
 * The main activity for the ImageTargets sample.
 * Image Targets allows users to create 2D targets for detection and tracking
 *
 * This class does high-level handling of the Vuforia lifecycle and any UI updates
 *
 * For ImageTarget-specific rendering, check out ImageTargetRenderer.java
 * For the low-level Vuforia lifecycle code, check out SampleApplicationSession.java
 */
public class ImageTargets extends SampleActivityBase implements SampleApplicationControl,
        SampleAppMenuInterface
{
    private static final String LOGTAG = "ImageTargets";

    private SampleApplicationSession vuforiaAppSession;

    private DataSet mCurrentDataset;
    private int mCurrentDatasetSelectionIndex = 0;
    private int mStartDatasetsIndex = 0;
    private int mDatasetsNumber = 0;
    private final ArrayList<String> mDatasetStrings = new ArrayList<>();

    private SampleApplicationGLView mGlView;

    private ImageTargetRenderer mRenderer;
    private ImageTargetRenderer tare;

    private GestureDetector mGestureDetector;

    // The textures we will use for rendering:
    private Vector<Texture> mTextures;

    // Menu option flags
    private boolean mSwitchDatasetAsap = false;
    private boolean mFlash = false;
    private boolean mContAutofocus = true;
    private boolean mDeviceTracker = false;

    private View mFocusOptionView;
    private View mFlashOptionView;

    private RelativeLayout mUILayout;

    private SampleAppMenu mSampleAppMenu;
    ArrayList<View> mSettingsAdditionalViews = new ArrayList<>();

    private SampleAppMessage mSampleAppMessage;
    private SampleAppTimer mRelocalizationTimer;
    private SampleAppTimer mStatusDelayTimer;

    private int mCurrentStatusInfo;

    final LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(this);

    // Alert Dialog used to display SDK errors
    private AlertDialog mErrorDialog;

    private boolean mIsDroidDevice = false;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);


        vuforiaAppSession = new SampleApplicationSession(this);

        startLoadingAnimation();
        mDatasetStrings.add("StonesAndChips.xml");
        mDatasetStrings.add("Tarmac.xml");
        mDatasetStrings.add("MobileAR.xml");

        vuforiaAppSession
                .initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mGestureDetector = new GestureDetector(getApplicationContext(), new GestureListener(this));

        // Load any sample specific textures:
        mTextures = new Vector<>();
        loadTextures();

        mIsDroidDevice = android.os.Build.MODEL.toLowerCase().startsWith("droid");

        // Relocalization timer and message
        mSampleAppMessage = new SampleAppMessage(this, mUILayout, mUILayout.findViewById(R.id.topbar_layout), false);
        mRelocalizationTimer = new SampleAppTimer(10000, 1000)
        {
            @Override
            public void onFinish()
            {
                if (vuforiaAppSession != null)
                {
                    vuforiaAppSession.resetDeviceTracker();
                }

                super.onFinish();
            }
        };

        mStatusDelayTimer = new SampleAppTimer(1000, 1000)
        {
            @Override
            public void onFinish()
            {
                if (mRenderer.isTargetCurrentlyTracked())
                {
                    super.onFinish();
                    return;
                }

                if (!mRelocalizationTimer.isRunning())
                {
                    mRelocalizationTimer.startTimer();
                }

                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mSampleAppMessage.show(getString(R.string.instruct_relocalize));
                    }
                });

                super.onFinish();
            }
        };

        //tare = new ImageTargetRenderer(this, vuforiaAppSession);
        Button button1= (Button) findViewById(R.id.button1);
        Button button2= (Button) findViewById(R.id.button2);
        Button button3=(Button) findViewById(R.id.button3);
        Button button4=(Button) findViewById(R.id.button4);
        Button button5=(Button) findViewById(R.id.button5);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId())
                {
                    case R.id.button1: {
                        if (ImageTargetRenderer.flower_st == 1) {
                            Log.d("Onclick","OnClick1");
                            ImageTargetRenderer.flower_st = 0;
                            break;
                        } else {
                            Log.d("Onclick2","OnClick2");
                            ImageTargetRenderer.flower_st = 1;
                            break;
                        }
                    }
                    case R.id.button2: {
                        if (ImageTargetRenderer.animal_st == 1) {
                            Log.d("Onclick3","OnClick3");
                            ImageTargetRenderer.animal_st = 0;
                            break;
                        } else {
                            Log.d("Onclick4","OnClick4");
                            ImageTargetRenderer.animal_st = 1;
                            break;
                        }
                    }
                    case R.id.button3: {
                            ImageTargetRenderer.apple_st +=1;
                            break;
                    }
                    case R.id.button4: {
                        onHandler1();
                        break;
                    }
                    case R.id.button5: {
                        onHandler2();
                        break;
                    }
                }
            }
        };
        button1.setOnClickListener(listener);
        button2.setOnClickListener(listener);
        button3.setOnClickListener(listener);

    }

    private Handler mHandler;
    private Runnable r;

    private void onHandler1() {
        mHandler = new Handler();
        r = new Runnable() {
            @Override
            public void run() {
                ImageTargetRenderer.z_move-=0.4f;
                mHandler.postDelayed(r, 50);
            }
        };
        mHandler.postDelayed(r, 50);
    }
    private void onHandler2() {
        mHandler = new Handler();
        r = new Runnable() {
            @Override
            public void run() {
                ImageTargetRenderer.z_move+=0.4f;
                mHandler.postDelayed(r, 50);
            }
        };
        mHandler.postDelayed(r, 50);
    }


    private class GestureListener extends
            GestureDetector.SimpleOnGestureListener
    {
        // Used to set autofocus one second after a manual focus is triggered
        private final Handler autofocusHandler = new Handler();

        private WeakReference<ImageTargets> activityRef;


        private GestureListener(ImageTargets activity)
        {
            activityRef = new WeakReference<>(activity);
        }

        @Override
        public boolean onDown(MotionEvent e)
        {
            return true;
        }


        // Process Single Tap event to trigger autofocus
        @Override
        public boolean onSingleTapUp(MotionEvent e)
        {
            boolean result = CameraDevice.getInstance().setFocusMode(
                    CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO);
            if (!result)
                Log.e("SingleTapUp", "Unable to trigger focus");

            // Generates a Handler to trigger continuous auto-focus
            // after 1 second
            autofocusHandler.postDelayed(new Runnable()
            {
                public void run()
                {
                    if (activityRef.get().mContAutofocus)
                    {
                        final boolean autofocusResult = CameraDevice.getInstance().setFocusMode(
                                CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

                        if (!autofocusResult)
                            Log.e("SingleTapUp", "Unable to re-enable continuous auto-focus");
                    }
                }
            }, 1000L);

            return true;
        }
    }


    // Load specific textures from the APK, which we will later use for rendering.
    private void loadTextures()
    {
        mTextures.add(Texture.loadTextureFromApk("TextureTeapotBrass.png",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("TextureTeapotBlue.png",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("TextureTeapotRed.png",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("ImageTargets/Buildings.png",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("ImageTargets/box.jpg",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("ImageTargets/tree.jpg",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("ImageTargets/flower.jpg",
            getAssets()));
        mTextures.add(Texture.loadTextureFromApk("ImageTargets/flower2.jpg",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("ImageTargets/bird.jpg",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("ImageTargets/cloud.jpg",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("ImageTargets/blue.jpg",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("ImageTargets/indigo.jpg",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("ImageTargets/yello.jpg",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("ImageTargets/orange.jpg",
            getAssets()));
        mTextures.add(Texture.loadTextureFromApk("ImageTargets/apple.jpg",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("ImageTargets/sea.jpg",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("ImageTargets/sand.jpg",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("ImageTargets/tutrle.png",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("ImageTargets/boat.jpg",
                getAssets()));
        mTextures.add(Texture.loadTextureFromApk("ImageTargets/bluetang_texture.jpg",
                getAssets()));

    }


    @Override
    protected void onResume()
    {
        Log.d(LOGTAG, "onResume");
        super.onResume();

        showProgressIndicator(true);

        // This is needed for some Droid devices to force portrait
        if (mIsDroidDevice)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        vuforiaAppSession.onResume();
    }


    // Called whenever the device orientation or screen resolution changes
    @Override
    public void onConfigurationChanged(Configuration config)
    {
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);

        vuforiaAppSession.onConfigurationChanged();
    }


    @Override
    protected void onPause()
    {
        Log.d(LOGTAG, "onPause");
        super.onPause();

        if (mGlView != null)
        {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }

        // Turn off the flash
        if (mFlashOptionView != null && mFlash)
        {
            // OnCheckedChangeListener is called upon changing the checked state
            setMenuToggle(mFlashOptionView, false);
        }

        vuforiaAppSession.onPause();
    }


    @Override
    protected void onDestroy()
    {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();

        try
        {
            vuforiaAppSession.stopAR();
        } catch (SampleApplicationException e)
        {
            Log.e(LOGTAG, e.getString());
        }

        // Unload texture:
        mTextures.clear();
        mTextures = null;

        System.gc();
    }


    private void initApplicationAR()
    {
        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

        mGlView = new SampleApplicationGLView(getApplicationContext());
        mGlView.init(translucent, depthSize, stencilSize);

        mRenderer = new ImageTargetRenderer(this, vuforiaAppSession);
        mRenderer.setTextures(mTextures);
        mGlView.setRenderer(mRenderer);
        mGlView.setPreserveEGLContextOnPause(true);

        setRendererReference(mRenderer);
    }


    private void startLoadingAnimation()
    {
        mUILayout = (RelativeLayout) View.inflate(getApplicationContext(), R.layout.camera_overlay, null);

        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.BLACK);

        RelativeLayout topbarLayout = mUILayout.findViewById(R.id.topbar_layout);
        topbarLayout.setVisibility(View.VISIBLE);

        TextView title = mUILayout.findViewById(R.id.topbar_title);
        title.setText(getText(R.string.feature_image_targets));

        mSettingsAdditionalViews.add(topbarLayout);

        // Gets a reference to the loading dialog
        loadingDialogHandler.mLoadingDialogContainer = mUILayout
                .findViewById(R.id.loading_indicator);

        // Shows the loading indicator at start
        loadingDialogHandler
                .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);

        // Adds the inflated layout to the view
        addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

    }


    @Override
    public boolean doLoadTrackersData()
    {
        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
        {
            return false;
        }

        if (mCurrentDataset == null)
        {
            mCurrentDataset = objectTracker.createDataSet();
        }

        if (mCurrentDataset == null)
        {
            return false;
        }

        if (!mCurrentDataset.load(
                mDatasetStrings.get(mCurrentDatasetSelectionIndex),
                STORAGE_TYPE.STORAGE_APPRESOURCE))
        {
            return false;
        }

        if (!objectTracker.activateDataSet(mCurrentDataset))
        {
            return false;
        }

        TrackableList trackableList = mCurrentDataset.getTrackables();
        for (Trackable trackable : trackableList)
        {
            String name = "Current Dataset : " + trackable.getName();
            trackable.setUserData(name);
            Log.d(LOGTAG, "UserData:Set the following user data "
                    + trackable.getUserData());
        }

        return true;
    }


    @Override
    public boolean doUnloadTrackersData()
    {
        // Indicate if the trackers were unloaded correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
                .getTracker(ObjectTracker.getClassType());

        if (objectTracker == null)
        {
            return false;
        }

        if (mCurrentDataset != null && mCurrentDataset.isActive())
        {
            if (objectTracker.getActiveDataSets().at(0).equals(mCurrentDataset)
                    && !objectTracker.deactivateDataSet(mCurrentDataset))
            {
                result = false;
            }
            else if (!objectTracker.destroyDataSet(mCurrentDataset))
            {
                result = false;
            }

            mCurrentDataset = null;
        }

        return result;
    }


    @Override
    public void onVuforiaResumed()
    {
        if (mGlView != null)
        {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }
    }


    @Override
    public void onVuforiaStarted()
    {
        mRenderer.updateRenderingPrimitives();

        if (mContAutofocus)
        {
            if(!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO))
            {
                // If continuous autofocus mode fails, attempt to set to a different mode
                if(!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO))
                {
                    CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);
                }

                setMenuToggle(mFocusOptionView, false);
            }
            else
            {
                setMenuToggle(mFocusOptionView, true);
            }
        }
        else
        {
            setMenuToggle(mFocusOptionView, false);
        }

        showProgressIndicator(false);
    }


    private void showProgressIndicator(boolean show)
    {
        if (show)
        {
            loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);
        }
        else
        {
            loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
        }
    }


    // Called once Vuforia has been initialized or
    // an error has caused Vuforia initialization to stop
    @Override
    public void onInitARDone(SampleApplicationException exception)
    {
        if (exception == null)
        {
            initApplicationAR();

            mRenderer.setActive(true);

            // Now add the GL surface view. It is important
            // that the OpenGL ES surface view gets added
            // BEFORE the camera is started and video
            // background is configured.
            addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));

            // Sets the UILayout to be drawn in front of the camera
            mUILayout.bringToFront();

            mUILayout.setBackgroundColor(Color.TRANSPARENT);

            mSampleAppMenu = new SampleAppMenu(this, this, "Image Targets",
                    mGlView, mUILayout, mSettingsAdditionalViews);
            setSampleAppMenuSettings();

            vuforiaAppSession.startAR();
        }
        else
        {
            Log.e(LOGTAG, exception.getString());
            showInitializationErrorMessage(exception.getString());
        }
    }


    private void showInitializationErrorMessage(String message)
    {
        final String errorMessage = message;
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                if (mErrorDialog != null)
                {
                    mErrorDialog.dismiss();
                }

                // Generates an Alert Dialog to show the error message
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        ImageTargets.this);
                builder
                        .setMessage(errorMessage)
                        .setTitle(getString(R.string.INIT_ERROR))
                        .setCancelable(false)
                        .setIcon(0)
                        .setPositiveButton(getString(R.string.button_OK),
                                new DialogInterface.OnClickListener()
                                {
                                    public void onClick(DialogInterface dialog, int id)
                                    {
                                        finish();
                                    }
                                });

                mErrorDialog = builder.create();
                mErrorDialog.show();
            }
        });
    }


    // Called every frame
    @Override
    public void onVuforiaUpdate(State state)
    {
        if (mSwitchDatasetAsap)
        {
            mSwitchDatasetAsap = false;
            TrackerManager tm = TrackerManager.getInstance();
            ObjectTracker ot = (ObjectTracker) tm.getTracker(ObjectTracker
                    .getClassType());
            if (ot == null || mCurrentDataset == null
                    || ot.getActiveDataSets().at(0) == null)
            {
                Log.d(LOGTAG, "Failed to swap datasets");
                return;
            }

            doUnloadTrackersData();
            doLoadTrackersData();
        }
    }


    @Override
    public boolean doInitTrackers()
    {
        // Indicate if the trackers were initialized correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();

        Tracker tracker = tManager.initTracker(ObjectTracker.getClassType());
        if (tracker == null)
        {
            Log.e(
                    LOGTAG,
                    "Tracker not initialized. Tracker already initialized or the camera is already started");
            result = false;
        } else
        {
            Log.i(LOGTAG, "Tracker successfully initialized");
        }

        // Initialize the Positional Device Tracker
        DeviceTracker deviceTracker = (PositionalDeviceTracker)
                tManager.initTracker(PositionalDeviceTracker.getClassType());

        if (deviceTracker != null)
        {
            Log.i(LOGTAG, "Successfully initialized Device Tracker");
        }
        else
        {
            Log.e(LOGTAG, "Failed to initialize Device Tracker");
        }

        return result;
    }


    @Override
    public boolean doStartTrackers()
    {
        // Indicate if the trackers were started correctly
        boolean result = true;

        TrackerManager trackerManager = TrackerManager.getInstance();

        Tracker objectTracker = trackerManager.getTracker(ObjectTracker.getClassType());

        if (objectTracker != null && objectTracker.start())
        {
            Log.i(LOGTAG, "Successfully started Object Tracker");
        }
        else
        {
            Log.e(LOGTAG, "Failed to start Object Tracker");
            result = false;
        }

        if (isDeviceTrackingActive())
        {
            PositionalDeviceTracker deviceTracker = (PositionalDeviceTracker) trackerManager
                    .getTracker(PositionalDeviceTracker.getClassType());

            if (deviceTracker != null && deviceTracker.start())
            {
                Log.i(LOGTAG, "Successfully started Device Tracker");
            }
            else
            {
                Log.e(LOGTAG, "Failed to start Device Tracker");
            }
        }

        return result;
    }


    @Override
    public boolean doStopTrackers()
    {
        // Indicate if the trackers were stopped correctly
        boolean result = true;

        TrackerManager trackerManager = TrackerManager.getInstance();

        Tracker objectTracker = trackerManager.getTracker(ObjectTracker.getClassType());
        if (objectTracker != null)
        {
            objectTracker.stop();
            Log.i(LOGTAG, "Successfully stopped object tracker");
        }
        else
        {
            Log.e(LOGTAG, "Failed to stop object tracker");
            result = false;
        }

        // Stop the device tracker
        if(isDeviceTrackingActive())
        {

            Tracker deviceTracker = trackerManager.getTracker(PositionalDeviceTracker.getClassType());

            if (deviceTracker != null)
            {
                deviceTracker.stop();
                Log.i(LOGTAG, "Successfully stopped device tracker");
            }
            else
            {
                Log.e(LOGTAG, "Could not stop device tracker");
            }
        }

        return result;
    }


    @Override
    public boolean doDeinitTrackers()
    {
        TrackerManager tManager = TrackerManager.getInstance();

        // Indicate if the trackers were deinitialized correctly
        boolean result = tManager.deinitTracker(ObjectTracker.getClassType());
        tManager.deinitTracker(PositionalDeviceTracker.getClassType());

        return result;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        // Process the Gestures
        return ((mSampleAppMenu != null && mSampleAppMenu.processEvent(event))
                || mGestureDetector.onTouchEvent(event));
    }


    boolean isDeviceTrackingActive()
    {
        return mDeviceTracker;
    }

    // Menu options
    private final static int CMD_BACK = -1;
    private final static int CMD_DEVICE_TRACKING = 1;
    private final static int CMD_AUTOFOCUS = 2;
    private final static int CMD_FLASH = 3;
    private final static int CMD_DATASET_START_INDEX = 4;


    private void setSampleAppMenuSettings()
    {
        SampleAppMenuGroup group;

        group = mSampleAppMenu.addGroup("", false);
        group.addTextItem(getString(R.string.menu_back), -1);

        group = mSampleAppMenu.addGroup("", true);
        group.addSelectionItem(getString(R.string.menu_device_tracker),
                CMD_DEVICE_TRACKING, false);

        group = mSampleAppMenu.addGroup(getString(R.string.menu_camera), true);
        mFocusOptionView = group.addSelectionItem(getString(R.string.menu_contAutofocus),
                CMD_AUTOFOCUS, mContAutofocus);
        mFlashOptionView = group.addSelectionItem(
                getString(R.string.menu_flash), CMD_FLASH, false);

        group = mSampleAppMenu
                .addGroup(getString(R.string.menu_datasets), true);
        mStartDatasetsIndex = CMD_DATASET_START_INDEX;
        mDatasetsNumber = mDatasetStrings.size();

        group.addRadioItem("Stones & Chips", mStartDatasetsIndex, true);
        group.addRadioItem("Tarmac", mStartDatasetsIndex + 1, false);
        group.addRadioItem("My Patterns", mStartDatasetsIndex + 2, false);

        group = mSampleAppMenu.addGroup("OBJECTS", true);
        group.addRadioItem("Teapot", 7, true);
        group.addRadioItem("Cube", 8, false);
        group.addRadioItem("MyObj", 9, false);


        mSampleAppMenu.attachMenu();
    }


    private void setMenuToggle(View view, boolean value)
    {
        // OnCheckedChangeListener is called upon changing the checked state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
        {
            ((Switch) view).setChecked(value);
        } else
        {
            ((CheckBox) view).setChecked(value);
        }
    }


    // In this function you can define the desired behavior for each menu option
    // Each case corresponds to a menu option
    @Override
    public boolean menuProcess(int command)
    {
        boolean result = true;

        switch (command)
        {
            case CMD_BACK:
                finish();
                break;

            case CMD_FLASH:
                result = CameraDevice.getInstance().setFlashTorchMode(!mFlash);

                if (result)
                {
                    mFlash = !mFlash;
                } else
                {
                    showToast(getString(mFlash ? R.string.menu_flash_error_off
                            : R.string.menu_flash_error_on));
                    Log.e(LOGTAG,
                            getString(mFlash ? R.string.menu_flash_error_off
                                    : R.string.menu_flash_error_on));
                }
                break;

            case CMD_AUTOFOCUS:

                if (mContAutofocus)
                {
                    result = CameraDevice.getInstance().setFocusMode(
                            CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);

                    if (result)
                    {
                        mContAutofocus = false;
                    } else
                    {
                        showToast(getString(R.string.menu_contAutofocus_error_off));
                        Log.e(LOGTAG,
                                getString(R.string.menu_contAutofocus_error_off));
                    }
                } else
                {
                    result = CameraDevice.getInstance().setFocusMode(
                            CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

                    if (result)
                    {
                        mContAutofocus = true;
                    } else
                    {
                        showToast(getString(R.string.menu_contAutofocus_error_on));
                        Log.e(LOGTAG,
                                getString(R.string.menu_contAutofocus_error_on));
                    }
                }

                break;

            case CMD_DEVICE_TRACKING:

                result = toggleDeviceTracker();

                break;

            case 7:
                ImageTargetRenderer.objectSel = 0;
                break;
            case 8:
                ImageTargetRenderer.objectSel = 1;
                break;

            case 9:
                ImageTargetRenderer.objectSel = 2;
                break;


            default:
                if (command >= mStartDatasetsIndex
                        && command < mStartDatasetsIndex + mDatasetsNumber)
                {
                    mSwitchDatasetAsap = true;
                    mCurrentDatasetSelectionIndex = command
                            - mStartDatasetsIndex;
                }
                break;
        }

        return result;
    }


    private boolean toggleDeviceTracker()
    {
        boolean result = true;
        TrackerManager trackerManager = TrackerManager.getInstance();
        PositionalDeviceTracker deviceTracker = (PositionalDeviceTracker)
                trackerManager.getTracker(PositionalDeviceTracker.getClassType());

        if (deviceTracker != null)
        {
            if (!mDeviceTracker)
            {
                if (!deviceTracker.start())
                {
                    Log.e(LOGTAG,"Failed to start device tracker");
                    result = false;
                }
                else
                {
                    Log.d(LOGTAG,"Successfully started device tracker");
                }
            }
            else
            {
                deviceTracker.stop();
                Log.d(LOGTAG, "Successfully stopped device tracker");

                clearSampleAppMessage();
            }
        }
        else
        {
            Log.e(LOGTAG, "Device tracker is null!");
            result = false;
        }

        if (result)
        {
            mDeviceTracker = !mDeviceTracker;
        }
        else
        {
            clearSampleAppMessage();
        }

        return result;
    }


    public void checkForRelocalization(final int statusInfo)
    {
        if (mCurrentStatusInfo == statusInfo)
        {
            return;
        }

        mCurrentStatusInfo = statusInfo;

        if (mCurrentStatusInfo == TrackableResult.STATUS_INFO.RELOCALIZING)
        {
            // If the status is RELOCALIZING, start the timer
            if (!mStatusDelayTimer.isRunning())
            {
                mStatusDelayTimer.startTimer();
            }
        }
        else
        {
            // If the status is not RELOCALIZING, stop the timers and hide the message
            if (mStatusDelayTimer.isRunning())
            {
                mStatusDelayTimer.stopTimer();
            }

            if (mRelocalizationTimer.isRunning())
            {
                mRelocalizationTimer.stopTimer();
            }

            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    if (mSampleAppMessage != null)
                    {
                        mSampleAppMessage.hide();
                    }
                }
            });
        }
    }


    private void clearSampleAppMessage()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (mSampleAppMessage != null)
                {
                    mSampleAppMessage.hide();
                }
            }
        });
    }


    private void showToast(String text)
    {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }
}