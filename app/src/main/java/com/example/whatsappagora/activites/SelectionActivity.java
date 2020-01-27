package com.example.whatsappagora.activites;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.whatsappagora.R;
import com.example.whatsappagora.model.User;
import com.example.whatsappagora.rtm.AGApplication;
import com.example.whatsappagora.rtm.ChatManager;
import com.example.whatsappagora.utils.MessageUtil;

public class SelectionActivity extends AppCompatActivity {

    private User user;
    private TextView mTitleTextView, mChatButton, mCallButton;
    private EditText mNameEditText;
    private String mTargetName;

    private static final int CHAT_REQUEST_CODE = 1;

    private boolean mIsPeerToPeerMode = true;

    private ChatManager mChatManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection);

        mChatManager = AGApplication.the().getChatManager();

        initUIAndData();
    }

    private void initUIAndData() {
        Intent intent = getIntent();
        user = intent.getParcelableExtra(MessageUtil.INTENT_EXTRA_USER_ID);
        mTitleTextView = findViewById(R.id.selection_title);
        mNameEditText = findViewById(R.id.selection_name);
        mChatButton = findViewById(R.id.selection_chat_btn);
        mCallButton = findViewById(R.id.selection_call_btn);
        RadioGroup modeGroup = findViewById(R.id.mode_radio_group);
        modeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.peer_radio_button:
                        mIsPeerToPeerMode = true;
                        mTitleTextView.setText(getString(R.string.title_peer));
                        mChatButton.setText(getString(R.string.btn_chat));
                        mNameEditText.setHint(getString(R.string.hint_friend));
                        break;
                    case R.id.selection_tab_channel:
                        mIsPeerToPeerMode = false;
                        mTitleTextView.setText(getString(R.string.title_channel));
                        mChatButton.setText(getString(R.string.btn_join));
                        mNameEditText.setHint(getString(R.string.hint_channel));
                        break;
                }
            }
        });

        RadioButton peerMode = findViewById(R.id.peer_radio_button);
        peerMode.setChecked(true);
    }

    public void onClickFinish(View view) {
        finish();
    }

    private void jumpToMessageActivity() {
        Intent intent = new Intent(this, MessageActivity.class);
        intent.putExtra(MessageUtil.INTENT_EXTRA_IS_PEER_MODE, mIsPeerToPeerMode);
        intent.putExtra(MessageUtil.INTENT_EXTRA_TARGET_NAME, mTargetName);
        intent.putExtra(MessageUtil.INTENT_EXTRA_USER_ID, user);
        startActivityForResult(intent, CHAT_REQUEST_CODE);
    }

    public void onClickChat(View view) {
        mTargetName = mNameEditText.getText().toString();
        if (mTargetName.equals("")) {
            showToast(getString(mIsPeerToPeerMode ? R.string.account_empty : R.string.channel_name_empty));
        } else if (mTargetName.length() >= MessageUtil.MAX_INPUT_NAME_LENGTH) {
            showToast(getString(mIsPeerToPeerMode ? R.string.account_too_long : R.string.channel_name_too_long));
        } else if (mTargetName.startsWith(" ")) {
            showToast(getString(mIsPeerToPeerMode ? R.string.account_starts_with_space : R.string.channel_name_starts_with_space));
        } else if (mTargetName.equals("null")) {
            showToast(getString(mIsPeerToPeerMode ? R.string.account_literal_null : R.string.channel_name_literal_null));
        } else if (mIsPeerToPeerMode && mTargetName.equals(user.getFireDisplayName())) {
            showToast(getString(R.string.account_cannot_be_yourself));
        } else {
            mChatButton.setEnabled(false);
            jumpToMessageActivity();
        }
    }

    public void onClickCall(View view) {
        String myName = user.getFireDisplayName();
        String targetName = mNameEditText.getText().toString();
        String channelName = "";
        if (mIsPeerToPeerMode) {
            channelName = myName.compareTo(targetName) < 0 ? myName + targetName : targetName + myName;

        }else {
            channelName = targetName;
        }
        Intent intent = new Intent(this, VideoActivity.class);
        intent.putExtra("User", user);
        intent.putExtra("Channel", channelName);
        intent.putExtra(MessageUtil.INTENT_EXTRA_IS_PEER_MODE, mIsPeerToPeerMode);
        intent.putExtra("Actual Target", targetName);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHAT_REQUEST_CODE) {
            if (resultCode == MessageUtil.ACTIVITY_RESULT_CONN_ABORTED) {
                finish();
            }
        }
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mChatButton.setEnabled(true);
    }
}
