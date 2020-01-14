package com.example.whatsappagora;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class ChannelActivity extends AppCompatActivity {

    EditText editText;
    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel);
        editText = findViewById(R.id.channel_edit_text);

        user = getIntent().getExtras().getParcelable("User");
    }

    public void onJoinChannelClicked(View view) {
        String channel = editText.getText().toString();
        if (channel.equals("")) {
            Toast.makeText(this, "Please Enter Channel Name.", Toast.LENGTH_SHORT).show();
        }else {
            Intent intent = new Intent(ChannelActivity.this, VideoActivity.class);
            intent.putExtra("User", user);
            intent.putExtra("Channel", channel);
            startActivity(intent);
        }
    }
}
