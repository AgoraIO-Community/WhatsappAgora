package com.example.whatsappagora;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;

public class VideoActivity extends AppCompatActivity {

    private String channelName;
    private User user;
    private static final String TAG = VideoActivity.class.getName();

    private static final int PERMISSION_REQ_ID = 22;
    RtcEngine mRtcEngine;
    private RelativeLayout mRemoteContainer;
    private SurfaceView mRemoteView;
    private FrameLayout mLocalContainer;
    private SurfaceView mLocalView;
    private ImageView mCallBtn, mMuteBtn, mSwitchVoiceBtn;
    private boolean isCalling = true;
    private boolean isMuted = false;
    private boolean isVoiceChanged = false;

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
                    Log.i("agora","Join channel success, uid: " + (uid & 0xFFFFFFFFL));
                    user.setAgoraUid(uid);
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
                    Log.i("agora","First remote video decoded, uid: " + (uid & 0xFFFFFFFFL));
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
                    Log.i("agora","User offline, uid: " + (uid & 0xFFFFFFFFL));
                    onRemoteUserLeft();
                }
            });
        }

        @Override
        public void onStreamMessage(int uid, int streamId, byte[] data) {
            super.onStreamMessage(uid, streamId, data);
            //do shacking when receive 1
            if (data.length == 1 && data[0] == 1) {
                performAnimation();
            }
        }

        @Override
        public void onStreamMessageError(int uid, int streamId, int error, int missed, int cached) {
            super.onStreamMessageError(uid, streamId, error, missed, cached);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        mLocalContainer = findViewById(R.id.local_video_view_container);
        mRemoteContainer = findViewById(R.id.remote_video_view_container);

        mCallBtn = findViewById(R.id.btn_call);
        mMuteBtn = findViewById(R.id.btn_mute);
        mSwitchVoiceBtn = findViewById(R.id.btn_switch_voice);
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

        // Enable the video module.
        mRtcEngine.enableVideo();

        mRtcEngine.enableInEarMonitoring(true);
        mRtcEngine.setInEarMonitoringVolume(80);

        mLocalView = RtcEngine.CreateRendererView(getBaseContext());
        mLocalView.setZOrderMediaOverlay(true);
        mLocalContainer.addView(mLocalView);
        // Set the local video view.
        VideoCanvas localVideoCanvas = new VideoCanvas(mLocalView, VideoCanvas.RENDER_MODE_HIDDEN, 0);
        mRtcEngine.setupLocalVideo(localVideoCanvas);
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

    private void onRemoteUserLeft() {
        removeRemoteVideo();
    }

    private void removeRemoteVideo() {
        if (mRemoteView != null) {
            mRemoteContainer.removeView(mRemoteView);
        }
        mRemoteView = null;
    }

    private void setupRemoteVideo(int uid) {

        mRemoteView = RtcEngine.CreateRendererView(getBaseContext());
        mRemoteContainer.addView(mRemoteView);
        // Set the remote video view.
        mRtcEngine.setupRemoteVideo(new VideoCanvas(mRemoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
        mRemoteView.setTag(uid);

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
        }else {
            //start the call
            startCalling();
            isCalling = true;
            mCallBtn.setImageResource(R.drawable.btn_endcall);
        }
    }

    private void finishCalling() {
        removeLocalVideo();
        removeRemoteVideo();
        leaveChannel();
    }

    private void removeLocalVideo() {
        if (mLocalView != null) {
            mLocalContainer.removeView(mLocalView);
        }
        mLocalView = null;
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
        }else {
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
