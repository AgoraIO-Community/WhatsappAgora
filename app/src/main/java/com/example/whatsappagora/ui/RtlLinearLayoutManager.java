package com.example.whatsappagora.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.LinearLayoutManager;

public class RtlLinearLayoutManager extends LinearLayoutManager {

    public RtlLinearLayoutManager(Context context) {
        super(context);
    }

    public RtlLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public RtlLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected boolean isLayoutRTL() {
        return true;
    }
}
