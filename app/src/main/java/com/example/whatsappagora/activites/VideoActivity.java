package com.example.whatsappagora.activites;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.whatsappagora.R;
import com.example.whatsappagora.layout.GridVideoViewContainer;
import com.example.whatsappagora.model.User;
import com.example.whatsappagora.ui.RecyclerItemClickListener;

import java.util.HashMap;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;

public class VideoActivity extends AppCompatActivity {

    private String channelName;
    private User user;
    private static final String TAG = VideoActivity.class.getName();

    private static final int PERMISSION_REQ_ID = 22;
    RtcEngine mRtcEngine;
    private SurfaceView mLocalView;
    private ImageView mCallBtn, mMuteBtn, mSwitchVoiceBtn;
    private GridVideoViewContainer mGridVideoViewContainer;
    private boolean isCalling = true;
    private boolean isMuted = false;
    private boolean isVoiceChanged = false;
    private boolean mIsLandscape = false;


    private final HashMap<Integer, SurfaceView> mUidsList = new HashMap<>(); // uid = 0 || uid == EngineConfig.mUid


    // Ask for Android device permissions at runtime.
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        // Listen for the onJoinChannelSuccess callback.
        // This callback occurs when the local user successfully joins the channel.
        public void onJoinChannelSuccess(String channel, final int uid, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(VideoActivity.this, "User: " + uid + " join!", Toast.LENGTH_LONG).show();
                    Log.i("agora", "Join channel success, uid: " + (uid & 0xFFFFFFFFL));
                    user.setAgoraUid(uid);
                    SurfaceView localView = mUidsList.remove(0);
                    mUidsList.put(uid, localView);
                }
            });
        }

        @Override
        // Listen for the onFirstRemoteVideoDecoded callback.
        // This callback occurs when the first video frame of a remote user is received and decoded after the remote user successfully joins the channel.
        // You can call the setupRemoteVideo method in this callback to set up the remote video view.
        public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i("agora", "First remote video decoded, uid: " + (uid & 0xFFFFFFFFL));
                    setupRemoteVideo(uid);
                }
            });
        }

        @Override
        // Listen for the onUserOffline callback.
        // This callback occurs when the remote user leaves the channel or drops offline.
        public void onUserOffline(final int uid, int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(VideoActivity.this, "User: " + uid + " left the room.", Toast.LENGTH_LONG).show();
                    Log.i("agora", "User offline, uid: " + (uid & 0xFFFFFFFFL));
                    onRemoteUserLeft(uid);
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.hide();
        }
        setContentView(R.layout.activity_video);


        channelName = getIntent().getExtras().getString("Channel");
        user = getIntent().getExtras().getParcelable("User");

        initUI();

        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)) {
            initEngineAndJoinChannel();
        }
    }

    private void initUI() {
        mCallBtn = findViewById(R.id.btn_call);
        mMuteBtn = findViewById(R.id.btn_mute);
        mSwitchVoiceBtn = findViewById(R.id.btn_switch_voice);

        mGridVideoViewContainer = findViewById(R.id.grid_video_view_container);
        mGridVideoViewContainer.setItemEventHandler(new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                //todo: add click event
            }

            @Override
            public void onItemLongClick(View view, int position) {
                //todo: add long click event
            }

            @Override
            public void onItemDoubleClick(View view, int position) {
                // TODO: 2020-01-20 add double click event
            }
        });
    }

    private void initEngineAndJoinChannel() {
        initializeEngine();
        setupLocalVideo();
        joinChannel();
    }

    private void initializeEngine() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.agora_app_id), mRtcEventHandler);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    private void setupLocalVideo() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRtcEngine.enableVideo();
                mRtcEngine.enableInEarMonitoring(true);
                mRtcEngine.setInEarMonitoringVolume(80);

                SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
                mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
                surfaceView.setZOrderOnTop(false);
                surfaceView.setZOrderMediaOverlay(false);

                mUidsList.put(0, surfaceView);

                mGridVideoViewContainer.initViewContainer(VideoActivity.this, 0, mUidsList, mIsLandscape);
            }
        });
    }

    private void joinChannel() {
        // Join a channel with a token.
        mRtcEngine.joinChannel(null, channelName, "Extra Optional Data", 0);
    }

    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }
        return true;
    }

    private void onRemoteUserLeft(int uid) {
        removeRemoteVideo(uid);
    }

    private void removeRemoteVideo(final int uid) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //todo: remove view
                Object target = mUidsList.remove(uid);
                if (target == null) {
                    return;
                }

                switchToDefaultVideoView();
            }
        });

    }

    private void setupRemoteVideo(final int uid) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SurfaceView mRemoteView = RtcEngine.CreateRendererView(getApplicationContext());

                mUidsList.put(uid, mRemoteView);

                mRemoteView.setZOrderOnTop(true);
                mRemoteView.setZOrderMediaOverlay(true);
                // Set the remote video view.
                mRtcEngine.setupRemoteVideo(new VideoCanvas(mRemoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid));

                switchToDefaultVideoView();
            }
        });
    }

    private void switchToDefaultVideoView() {

        mGridVideoViewContainer.initViewContainer(VideoActivity.this, user.getAgoraUid(), mUidsList, mIsLandscape);

        boolean setRemoteUserPriorityFlag = false;

        int sizeLimit = mUidsList.size();
        if (sizeLimit > 5) {
            sizeLimit = 5;
        }

        for (int i = 0; i < sizeLimit; i++) {
            int uid = mGridVideoViewContainer.getItem(i).mUid;
            if (user.getAgoraUid() != uid) {
                if (!setRemoteUserPriorityFlag) {
                    setRemoteUserPriorityFlag = true;
                    mRtcEngine.setRemoteUserPriority(uid, Constants.USER_PRIORITY_HIGH);
                } else {
                    mRtcEngine.setRemoteUserPriority(uid, Constants.USER_PRIORITY_NORANL);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isCalling) {
            leaveChannel();
        }
        RtcEngine.destroy();
    }

    private void leaveChannel() {
        // Leave the current channel.
        mRtcEngine.leaveChannel();
    }

    public void onCallClicked(View view) {
        if (isCalling) {
            //finish current call
            finishCalling();
            isCalling = false;
            mCallBtn.setImageResource(R.drawable.btn_startcall);
            finish();
//            Intent intent = new Intent(this, SelectionActivity.class);
//            intent.putExtra(MessageUtil.INTENT_EXTRA_USER_ID, user);
//            startActivity(intent);
        } else {
            //start the call
            startCalling();
            isCalling = true;
            mCallBtn.setImageResource(R.drawable.btn_endcall);
        }
    }

    private void finishCalling() {
        leaveChannel();
        mUidsList.clear();
    }

    private void startCalling() {
        setupLocalVideo();
        joinChannel();
    }

    public void onSwitchCameraClicked(View view) {
        mRtcEngine.switchCamera();
    }

    public void onLocalAudioMuteClicked(View view) {
        isMuted = !isMuted;
        mRtcEngine.muteLocalAudioStream(isMuted);
        int res = isMuted ? R.drawable.btn_mute : R.drawable.btn_unmute;
        mMuteBtn.setImageResource(res);
    }

    public void onSwitchVoiceClicked(View view) {
        if (!isVoiceChanged) {
            //start voice change to little girl, can be changed to different voices
            mRtcEngine.setLocalVoiceChanger(3);
            Toast.makeText(this, "Voice changer activate", Toast.LENGTH_SHORT).show();
        } else {
            //disable voice change
            Toast.makeText(this, "Voice back to normal", Toast.LENGTH_SHORT).show();
            mRtcEngine.setLocalVoiceReverbPreset(0);
        }
        int res = !isVoiceChanged ? R.drawable.ic_change_voice_24dp : R.drawable.ic_change_voice_normal_24dp;
        mSwitchVoiceBtn.setImageResource(res);
        isVoiceChanged = !isVoiceChanged;
    }

    public void onRemoteShackClicked(View view) {
        //send message to the other user with data = {1}
        //mRtcEngine.sendStreamMessage(mRtcEngine.createDataStream(true, true), new byte[]{1});
        Toast.makeText(this, "" + user.getFireUid(), Toast.LENGTH_SHORT).show();
    }

    public void performAnimation() {
        //mRemoteContainer.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
    }

}
