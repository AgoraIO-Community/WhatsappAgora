package com.example.whatsappagora.activites;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.whatsappagora.R;
import com.example.whatsappagora.layout.GridVideoViewContainer;
import com.example.whatsappagora.layout.SmallVideoViewAdapter;
import com.example.whatsappagora.layout.SmallVideoViewDecoration;
import com.example.whatsappagora.model.User;
import com.example.whatsappagora.model.UserStatusData;
import com.example.whatsappagora.ui.RecyclerItemClickListener;
import com.example.whatsappagora.ui.RtlLinearLayoutManager;
import com.example.whatsappagora.utils.MessageUtil;

import java.util.HashMap;
import java.util.Iterator;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;

import static com.example.whatsappagora.utils.MessageUtil.INTENT_EXTRA_IS_PEER_MODE;

public class VideoActivity extends AppCompatActivity {
    public static final int LAYOUT_TYPE_DEFAULT = 0;
    public static final int LAYOUT_TYPE_SMALL = 1;

    private String channelName;
    private User user;
    private static final String TAG = VideoActivity.class.getName();
    public int mLayoutType = LAYOUT_TYPE_DEFAULT;
    private static final int PERMISSION_REQ_ID = 22;
    RtcEngine mRtcEngine;
    private ImageView mCallBtn, mMuteBtn, mSwitchVoiceBtn;
    private GridVideoViewContainer mGridVideoViewContainer;
    private boolean isCalling = true;
    private boolean isMuted = false;
    private boolean isVoiceChanged = false;
    private boolean mIsLandscape = false;
    private RelativeLayout mSmallVideoViewDock;
    private SmallVideoViewAdapter mSmallVideoViewAdapter;
    private boolean mIsPeerToPeerMode = true;
    private String mActualTarget;

    private final HashMap<Integer, SurfaceView> mUidsList = new HashMap<>();


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
        getExtras();
        initUI();

        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)) {
            initEngineAndJoinChannel();
        }
    }

    private void getExtras() {
        channelName = getIntent().getExtras().getString("Channel");
        user = getIntent().getExtras().getParcelable("User");
        mIsPeerToPeerMode = getIntent().getBooleanExtra(MessageUtil.INTENT_EXTRA_IS_PEER_MODE, true);
        mActualTarget = getIntent().getExtras().getString("Actual Target");
    }

    private void initUI() {
        mCallBtn = findViewById(R.id.start_call_end_call_btn);
        mMuteBtn = findViewById(R.id.audio_mute_audio_unmute_btn);
        mSwitchVoiceBtn = findViewById(R.id.switch_voice_btn);

        mGridVideoViewContainer = findViewById(R.id.grid_video_view_container);
        mGridVideoViewContainer.setItemEventHandler(new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                //can add single click listener logic
            }

            @Override
            public void onItemLongClick(View view, int position) {
                //can add long click listener logic
            }

            @Override
            public void onItemDoubleClick(View view, int position) {
                onBigVideoViewDoubleClicked(view, position);
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
        // Join a channel with a token, token can be null.
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

    private void onBigVideoViewDoubleClicked(View view, int position) {
        if (mUidsList.size() < 2) {
            return;
        }

        UserStatusData user = mGridVideoViewContainer.getItem(position);
        int uid = (user.mUid == 0) ? this.user.getAgoraUid() : user.mUid;

        if (mLayoutType == LAYOUT_TYPE_DEFAULT && mUidsList.size() != 1) {
            switchToSmallVideoView(uid);
        } else {
            switchToDefaultVideoView();
        }
    }

    private void switchToSmallVideoView(int bigBgUid) {
        HashMap<Integer, SurfaceView> slice = new HashMap<>(1);
        slice.put(bigBgUid, mUidsList.get(bigBgUid));
        Iterator<SurfaceView> iterator = mUidsList.values().iterator();
        while (iterator.hasNext()) {
            SurfaceView s = iterator.next();
            s.setZOrderOnTop(true);
            s.setZOrderMediaOverlay(true);
        }

        mUidsList.get(bigBgUid).setZOrderOnTop(false);
        mUidsList.get(bigBgUid).setZOrderMediaOverlay(false);

        mGridVideoViewContainer.initViewContainer(this, bigBgUid, slice, mIsLandscape);

        bindToSmallVideoView(bigBgUid);

        mLayoutType = LAYOUT_TYPE_SMALL;
    }

    private void bindToSmallVideoView(int exceptUid) {
        if (mSmallVideoViewDock == null) {
            ViewStub stub = (ViewStub) findViewById(R.id.small_video_view_dock);
            mSmallVideoViewDock = (RelativeLayout) stub.inflate();
        }

        boolean twoWayVideoCall = mUidsList.size() == 2;

        RecyclerView recycler = (RecyclerView) findViewById(R.id.small_video_view_container);

        boolean create = false;

        if (mSmallVideoViewAdapter == null) {
            create = true;
            mSmallVideoViewAdapter = new SmallVideoViewAdapter(this, this.user.getAgoraUid(), exceptUid, mUidsList);
            mSmallVideoViewAdapter.setHasStableIds(true);
        }
        recycler.setHasFixedSize(true);

        if (twoWayVideoCall) {
            recycler.setLayoutManager(new RtlLinearLayoutManager(getApplicationContext(), RtlLinearLayoutManager.HORIZONTAL, false));
        } else {
            recycler.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
        }
        recycler.addItemDecoration(new SmallVideoViewDecoration());
        recycler.setAdapter(mSmallVideoViewAdapter);
        recycler.addOnItemTouchListener(new RecyclerItemClickListener(getBaseContext(), new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {

            }

            @Override
            public void onItemLongClick(View view, int position) {

            }

            @Override
            public void onItemDoubleClick(View view, int position) {
                onSmallVideoViewDoubleClicked(view, position);
            }
        }));

        recycler.setDrawingCacheEnabled(true);
        recycler.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_AUTO);

        if (!create) {
            mSmallVideoViewAdapter.setLocalUid(this.user.getAgoraUid());
            mSmallVideoViewAdapter.notifyUiChanged(mUidsList, exceptUid, null, null);
        }
        for (Integer tempUid : mUidsList.keySet()) {
            if (this.user.getAgoraUid() != tempUid) {
                if (tempUid == exceptUid) {
                    mRtcEngine.setRemoteUserPriority(tempUid, Constants.USER_PRIORITY_HIGH);
                } else {
                    mRtcEngine.setRemoteUserPriority(tempUid, Constants.USER_PRIORITY_NORANL);
                }
            }
        }
        recycler.setVisibility(View.VISIBLE);
        mSmallVideoViewDock.setVisibility(View.VISIBLE);
    }

    private void onSmallVideoViewDoubleClicked(View view, int position) {
        switchToDefaultVideoView();
    }

    private void onRemoteUserLeft(int uid) {
        removeRemoteVideo(uid);
    }

    private void removeRemoteVideo(final int uid) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
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
                mRtcEngine.setupRemoteVideo(new VideoCanvas(mRemoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid));

                switchToDefaultVideoView();
            }
        });
    }

    private void switchToDefaultVideoView() {

        mGridVideoViewContainer.initViewContainer(VideoActivity.this, user.getAgoraUid(), mUidsList, mIsLandscape);

        boolean setRemoteUserPriorityFlag = false;

        mLayoutType = LAYOUT_TYPE_DEFAULT;

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

    public void onVideoChatClicked(View view) {
        jumpToMessageActivity();
    }

    private void jumpToMessageActivity() {
        Intent intent = new Intent(this, MessageActivity.class);
        intent.putExtra(INTENT_EXTRA_IS_PEER_MODE, mIsPeerToPeerMode);
        if (!mIsPeerToPeerMode) {
            intent.putExtra(MessageUtil.INTENT_EXTRA_TARGET_NAME, channelName);
        }else {
            intent.putExtra(MessageUtil.INTENT_EXTRA_TARGET_NAME, mActualTarget);
        }
        intent.putExtra(MessageUtil.INTENT_EXTRA_USER_ID, user);
        startActivity(intent);
    }
}
