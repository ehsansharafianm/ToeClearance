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

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Locale;


public class UiManager {

    // All UI elements
    private final View root;
    private final ImuManager imuManager;

    public Button scanButton, syncButton, measureButton, disconnectButton,
            stopButton, uploadButton, dataLogButton, homeButton, listImusButton;
    public Button logToggleButton;
    public TextView imu1Status, imu2Status, logContents;
    public TextView imu1Roll, imu2Roll;           // Roll angles
    public TextView imu1Gyro, imu2Gyro;           // Gyro magnitudes
    public TextView imu1Accel, imu2Accel;         // Linear accelerations
    public TextView imu1Index, imu2Index;         // Packet indices
    public TextView imu1Battery, imu2Battery;
    public EditText enterSubjectNumber;
    // Feature detection display fields
    public TextView imu1WindowNumber, imu1TerrainType, imu1BiasValue, imu1MaxHeight, imu1MaxStride;

    public CardView imuListDialog;
    public RecyclerView imuRecyclerView;
    public Button closeImuListButton;

    public UiManager(View rootView, ImuManager imuManager) {
        this.root = rootView;
        this.imuManager = imuManager;
    }

    // Bind all Views from layout
    public void bindLabelingDataViews(View root) {

        enterSubjectNumber = root.findViewById(R.id.enterSubjectNumber);
        scanButton = root.findViewById(R.id.scanButton);
        syncButton = root.findViewById(R.id.syncButton);
        measureButton = root.findViewById(R.id.measureButton);
        disconnectButton = root.findViewById(R.id.disconnectButton);
        stopButton = root.findViewById(R.id.stopButton);
        listImusButton = root.findViewById(R.id.listImusButton);
        uploadButton = root.findViewById(R.id.uploadButton);
        dataLogButton = root.findViewById(R.id.dataLogButton);
        homeButton = root.findViewById(R.id.homeButton);

        logToggleButton = root.findViewById(R.id.logToggleButton);
        // Status fields
        imu1Status = root.findViewById(R.id.imu1Status);
        imu2Status = root.findViewById(R.id.imu2Status);
        logContents = root.findViewById(R.id.logContents);

        // IMU1 data fields
        imu1Roll = root.findViewById(R.id.imu1Roll);
        imu1Gyro = root.findViewById(R.id.imu1Gyro);
        imu1Accel = root.findViewById(R.id.imu1Accel);
        imu1Index = root.findViewById(R.id.imu1Index);
        imu1Battery = root.findViewById(R.id.imu1Battery);

        // IMU2 data fields
        imu2Roll = root.findViewById(R.id.imu2Roll);
        imu2Gyro = root.findViewById(R.id.imu2Gyro);
        imu2Accel = root.findViewById(R.id.imu2Accel);
        imu2Index = root.findViewById(R.id.imu2Index);
        imu2Battery = root.findViewById(R.id.imu2Battery);

        // Feature detection display fields
        imu1WindowNumber = root.findViewById(R.id.imu1WindowNumber);
        imu1TerrainType = root.findViewById(R.id.imu1TerrainType);
        imu1BiasValue = root.findViewById(R.id.imu1BiasValue);
        imu1MaxHeight = root.findViewById(R.id.imu1MaxHeight);
        imu1MaxStride = root.findViewById(R.id.imu1MaxStride);


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

    private boolean isLogVisible = false;

    public void setLogToggleButtonHandler(Button logToggleButton, LogManager logManager) {
        if (logToggleButton == null || logManager == null) return;

        logToggleButton.setOnClickListener(v -> {
            isLogVisible = !isLogVisible;
            logManager.setLogVisible(isLogVisible);

            if (isLogVisible) {
                logToggleButton.setText("Hide Log");
            } else {
                logToggleButton.setText("Show Log");
            }
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
        imu1Roll.setText("");
        imu1Gyro.setText("");
        imu1Accel.setText("");
        imu1Index.setText("");
        imu1Battery.setText("");

        imu2Roll.setText("");
        imu2Gyro.setText("");
        imu2Accel.setText("");
        imu2Index.setText("");
        imu2Battery.setText("");
    }

    // Method to update feature detection display with dynamic colors
    public void updateFeatureDisplay(int windowNum, String terrainType, double biasValue,
                                     double maxHeight, double maxStride) {
        if (imu1WindowNumber != null) {
            imu1WindowNumber.setText(String.valueOf(windowNum));
        }

        if (imu1TerrainType != null) {
            imu1TerrainType.setText(terrainType);

            // Set different background colors for different terrain types
            int backgroundColor;
            switch (terrainType) {
                case "Level_Walk":
                    backgroundColor = Color.parseColor("#4CAF50"); // Green
                    break;
                case "Stair_Ascend":
                    backgroundColor = Color.parseColor("#FF5722"); // Red-Orange
                    break;
                case "Stair_Descend":
                    backgroundColor = Color.parseColor("#F44336"); // Red
                    break;
                case "Ramp_Ascend":
                    backgroundColor = Color.parseColor("#FF9800"); // Orange
                    break;
                case "Ramp_Descend":
                    backgroundColor = Color.parseColor("#FFC107"); // Amber
                    break;
                default:
                    backgroundColor = Color.parseColor("#9E9E9E"); // Gray for unknown
                    break;
            }

            // Create a GradientDrawable to maintain rounded corners (if your rectangle has them)
            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            drawable.setColor(backgroundColor);
            drawable.setCornerRadius(8); // Adjust corner radius as needed, or set to 0 for sharp corners

            imu1TerrainType.setBackground(drawable);
        }

        if (imu1BiasValue != null) {
            imu1BiasValue.setText(String.format(Locale.US, "%.3f", biasValue));
        }
        if (imu1MaxHeight != null) {
            imu1MaxHeight.setText(String.format(Locale.US, "%.3f m", maxHeight));
        }
        if (imu1MaxStride != null) {
            imu1MaxStride.setText(String.format(Locale.US, "%.3f m", maxStride));
        }
    }


    // Add a method to show the dialog
    public void showImuListDialog() {
        if (imuListDialog != null) {
            imuListDialog.setVisibility(View.VISIBLE);
        }
    }
}