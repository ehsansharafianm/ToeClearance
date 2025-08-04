package com.tmsimple.ToeClearance;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;



public class UiManager {

    // All UI elements
    private final View root;
    private final ImuManager imuManager;

    public Button scanButton, syncButton, measureButton, disconnectButton,
            stopButton, uploadButton, dataLogButton, homeButton;
    public Switch logSwitch, ImuSwitch;
    public TextView thighScanStatus, footScanStatus, logContents;
    public TextView ValueF1, ValueF2, ValueF3, ValueF4;
    public TextView ValueT1, ValueT2, ValueT3, ValueT4;
    public EditText enterSubjectNumber;

    public UiManager(View rootView, ImuManager imuManager) {
        this.root = rootView;
        this.imuManager = imuManager;
    }

    // Bind all Views from layout
    public void bindLabelingDataViews(View root) {
        scanButton = root.findViewById(R.id.scanButton);
        syncButton = root.findViewById(R.id.syncButton);
        measureButton = root.findViewById(R.id.measureButton);
        disconnectButton = root.findViewById(R.id.disconnectButton);
        stopButton = root.findViewById(R.id.stopButton);
        uploadButton = root.findViewById(R.id.uploadButton);
        dataLogButton = root.findViewById(R.id.dataLogButton);
        homeButton = root.findViewById(R.id.homeButton);

        logSwitch = root.findViewById(R.id.logSwitch);
        ImuSwitch = root.findViewById(R.id.ImuSwitch);

        thighScanStatus = root.findViewById(R.id.thighStatusView);
        footScanStatus = root.findViewById(R.id.footStatusView);
        logContents = root.findViewById(R.id.logContents);

        ValueF1 = root.findViewById(R.id.valueF1);
        ValueF2 = root.findViewById(R.id.valueF2);
        ValueF3 = root.findViewById(R.id.valueF3);
        ValueF4 = root.findViewById(R.id.valueF4);

        ValueT1 = root.findViewById(R.id.valueT1);
        ValueT2 = root.findViewById(R.id.valueT2);
        ValueT3 = root.findViewById(R.id.valueT3);
        ValueT4 = root.findViewById(R.id.valueT4);

        enterSubjectNumber = root.findViewById(R.id.enterSubjectNumber);
    }



    //
    // ---------- CONFIGURATION METHODS ----------
    //

    // Configure Button (text, color, enabled)
    public void setButton(Button button, String text, String colorHex, Boolean enabled) {
        if (button == null) return;
        if (text != null) button.setText(text);
        if (colorHex != null) button.setBackgroundColor(Color.parseColor(colorHex));
        if (enabled != null) button.setEnabled(enabled);
    }

    // Configure TextView (text only)
    public void setTextView(TextView textView, String text, String colorHex, Boolean enabled)  {
        if (textView == null) return;
        if (text != null) textView.setText(text);
        if (colorHex != null) textView.setBackgroundColor(Color.parseColor(colorHex));
        if (enabled != null) textView.setEnabled(enabled);
    }

    // Configure visibility for logContents
    public void setLogVisible(boolean visible) {
        if (logContents != null) {
            logContents.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }
    public void setDataLogButtonHandler(Button button, LogManager logManager, ImuManager imuManager) {
        button.setOnClickListener(new View.OnClickListener() {
            int index = 0;

            @Override
            public void onClick(View v) {
                index++;
                if (index % 2 == 1) {
                    imuManager.setLoggingData(true);
                    button.setBackgroundColor(Color.parseColor("#05edbb"));
                    button.setText("Data Logging ...");
                    logManager.log(" ---- Data is Logging -----");
                } else if (index % 2 == 0 && index > 1) {
                    imuManager.setLoggingData(false);
                    button.setBackgroundColor(Color.parseColor("#4DBDDF"));
                    button.setText("Data Logging Stopped");
                    logManager.log("---- Data Logging Stopped -----");
                }
            }
        });
    }
    public void setLogSwitchHandler(Switch logSwitch, LogManager logManager) {
        if (logSwitch == null || logManager == null) return;

        logSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            logManager.setLogVisible(isChecked);
        });
    }

    public interface OnSubjectNumberEnteredListener {
        void onSubjectNumberEntered(int subjectNumber);
    }

    public void setEnterSubjectNumberHandler(EditText editText, OnSubjectNumberEnteredListener listener) {
        if (editText == null || listener == null) return;

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable string) {
                if (!TextUtils.isEmpty(string)) {
                    try {
                        int number = Integer.parseInt(string.toString());
                        listener.onSubjectNumberEntered(number);
                        setButton(scanButton, null, null, true);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }

                }
            }
        });
    }

    public interface OnImuSwitchChangedListener {
        void onLeftSideSelected();
        void onRightSideSelected();
    }
    public void setImuSwitchHandler(Switch imuSwitch, LogManager logManager, OnImuSwitchChangedListener listener) {
        if (imuSwitch == null || listener == null) return;

        imuSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                logManager.log("IMU switch is checked for left side");
                listener.onLeftSideSelected();
            } else {
                logManager.log("IMU switch is checked for right side");
                listener.onRightSideSelected();
            }
        });
    }

    public void setHomeButtonHandler(Button button, Runnable onHomePressed) {
        button.setOnClickListener(v -> {
            if (onHomePressed != null) {
                onHomePressed.run();
            }
        });
    }

    // single listener that reads its offset (and colors) out of the view’s tag
    private final View.OnTouchListener labelTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // each button has a PacketOffsetItem in its tag
            PacketOffsetItem info = (PacketOffsetItem) v.getTag();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    imuManager.setPacketCounterOffset(info.offset);
                    v.setBackgroundColor(info.activeColor);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    imuManager.setPacketCounterOffset(0);
                    v.setBackgroundColor(info.defaultColor);
                    break;
            }
            return true;
        }
    };
    /** Call this once after you inflate your labeling_data layout */
    public void bindLabelButtons() {
        // configure each of your six buttons here:
        PacketOffsetItem[] configs = new PacketOffsetItem[] {
                new PacketOffsetItem(R.id.labelButton1, 1000000,
                        Color.parseColor("#05fff8"),
                        Color.parseColor("#4CAF50")),
                new PacketOffsetItem(R.id.labelButton2, 2000000,
                        Color.parseColor("#05fff8"),
                        Color.parseColor("#4CAF50")),
                new PacketOffsetItem(R.id.labelButton3, 3000000,
                        Color.parseColor("#05fff8"),
                        Color.parseColor("#4CAF50")),
                new PacketOffsetItem(R.id.labelButton4, 4000000,
                        Color.parseColor("#05fff8"),
                        Color.parseColor("#4CAF50")),
                new PacketOffsetItem(R.id.labelButton5, 5000000,
                        Color.parseColor("#05fff8"),
                        Color.parseColor("#4CAF50")),
                new PacketOffsetItem(R.id.labelButton6, 6000000,
                        Color.parseColor("#05fff8"),
                        Color.parseColor("#4CAF50")),
                // …and so on for buttons 3–6…
        };

        for (PacketOffsetItem cfg : configs) {
            Button btn = root.findViewById(cfg.buttonId);
            btn.setTag(cfg);
            btn.setOnTouchListener(labelTouchListener);
        }
    }
    /** simple holder for everything our listener needs */
    private static class PacketOffsetItem {
        final int buttonId;
        final int offset;
        final int activeColor;
        final int defaultColor;

        PacketOffsetItem(int buttonId, int offset, int activeColor, int defaultColor) {
            this.buttonId    = buttonId;
            this.offset      = offset;
            this.activeColor = activeColor;
            this.defaultColor= defaultColor;
        }
    }


    // Optional utility: clear all text fields
    public void clearAllValues() {
        ValueF1.setText("");
        ValueF2.setText("");
        ValueF3.setText("");
        ValueF4.setText("");
        ValueT1.setText("");
        ValueT2.setText("");
        ValueT3.setText("");
        ValueT4.setText("");
    }
}