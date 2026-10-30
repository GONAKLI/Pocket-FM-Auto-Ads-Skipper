package com.gonakli.pocketfmaddsskipper;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AdsSkipper extends AccessibilityService {
    // packages where application works
    String[] checkPackages = {
            "com.radio.pocketfm"
    };

    // Google Play Store package name
    String playStorePackageName = "com.android.vending";

    // Unique id for notification
    String notificationId = "Ads_Skipper";

    // keywords that are used to skip the ads in the application
    String[] keywords = {
            "close video", "skip", "close", "closeall", "close ad",
            "google play", "unlock 2 more episodes", "try again",
            "dismiss", "iv_close", "btn_close", "img_close",
            "ad_close", "ad_skip", "cross", "btn_x", "iv_x",
            "innobi", "close_btn", "close_img", "no thanks", "not now",
            "later", "got it", "x", "\u00d7", "\u2715", "\u2716", "\u2717",
            "skip ad", "skip ads", "close ads", "no, thanks", "unlock 1 more episode",
            "बंद करे", "बंद करें", "300x300", "continue watching ads"
    };
    private long lastTapPressed = 0;

    // counter for Total Ads skip count
    private int totalAdSkippedCount = 0;
    private long lastTimePlayStoreClosed = 0;
    WindowManager windowManager;


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // if event is empty return directly
        if (event == null) return;

        // get value of current package Name for which event is triggered
        String currentPackageName = String.valueOf(event.getPackageName());

        // get value of current class names inside a package
        String currentClassName = String.valueOf(event.getClassName());

        // log current class name and package name in the logcat
        Log.d("showPackages", "className: " + currentClassName);
        Log.d("showPackages", "packageName: " + currentPackageName);

        // logic for exiting Google Play while watching advertisement and ads redirect to Google Play
        if (currentPackageName.equalsIgnoreCase(playStorePackageName)) {
            // If else block to Prevent multiple back Press happens
            if ((System.currentTimeMillis() - lastTimePlayStoreClosed) < 500) {
                return;
            } else {
                lastTimePlayStoreClosed = System.currentTimeMillis();
                // performing back press action
                performGlobalAction(GLOBAL_ACTION_BACK);
                return;
            }

        }

        // checking if pocket fm application is active or not
        boolean target = false;
        for (String item : checkPackages) {
            if (item.equalsIgnoreCase(currentPackageName)) {
                // if currentPackageName is in checkPackages list, break the loop instantly
                // for further action
                target = true;
                break;
            }
        }

        // if currentPackageName is not in our list, then simply return
        if (!target) return;

        // to retrieve window content, we get the root node in the currently active window
        AccessibilityNodeInfo root = getRootInActiveWindow();

        // if root is null, then simply return
        if (root == null) return;

        // to perform further action in root node, we make a function dumpTree()
        dumpTree(root);
    }

    private void dumpTree(AccessibilityNodeInfo root) {
        // for safety measures, we first check if root is null or not
        if (root == null) return;

        // starting a loop to find further root child elements
        for (int i = 0; i < root.getChildCount(); i++) {
            // starting finding root child from its parent root
            AccessibilityNodeInfo rootChild = root.getChild(i);

            // if rootChild is null simply skip this iteration, and move to the next iteration
            if (rootChild == null) continue;

            // finding text, description and id from the root child, if any one is null, we set empty string
            String text = rootChild.getText() == null ? "" : rootChild.getText().toString().toLowerCase();
            String description = rootChild.getContentDescription() == null ? "" : rootChild.getContentDescription().toString().toLowerCase();
            String id = rootChild.getViewIdResourceName() == null ? "" : rootChild.getViewIdResourceName().toLowerCase();

            // informationCheckPoint is used for logging purpose, no other use case,
            // just to check text, description and id value
            String informationCheckPoint =
                    "Text is: " + text + "\n description is: " + description + "\n id is: " + id + "\n\n";

            // logging informationCheckPoint on the screen for further operations or finding something else
            Log.d("checkInformation", "dumpTree: " + informationCheckPoint);

            // check if any keywords for skipping ads in our list exists in our
            // text, description or id fields
            for (String item : keywords) {
                if (text.equalsIgnoreCase(item.toLowerCase()) || description.equalsIgnoreCase(item.toLowerCase())) {
                    performClickAction(rootChild);
                    break;
                } else if (id.equalsIgnoreCase(item)) {
                    performClickAction(rootChild);
                    break;
                }
            }

            // again call dumpTree for recursion in root child of its children, to go in deeper levels
            dumpTree(rootChild);
        }


    }

    private void performClickAction(AccessibilityNodeInfo node) {
        // performClickAction function is used as a supporting method, to perform click event in a node

        // area is used to find and draw boundaries of that particular node
        Rect area = new Rect();

        // find boundaries of a node in the screen
        node.getBoundsInScreen(area);

        // result come from tapGesture main function, Which ultimately generate Tap effects on the screen
        boolean result = tapGesture(area.exactCenterX(), area.exactCenterY()); // function calling start from here

        if (!result) {
            //check if node is clickable, if yes, then perform click directly
            if (node.isClickable()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                showTouchOnScreen(area.exactCenterX(), area.exactCenterY());
            }
        }
        // logging result and node clickable in the logcat for inspections
        Log.d("result", "is node clickable: : " + node.isClickable());
        Log.d("result", "result of tapGesture function is: " + result);

        // increasing totalAdSkipped Count by 1
        totalAdSkippedCount++;
        // send a notification if totalAdSkippedCount is a multiple of 10 i.e after every 10 ads
        if (totalAdSkippedCount % 10 == 0) {
            // function called for notification send
            sendNotification();
        }
        //produce vibration to check Service called and executed
        generateVibration();

    }

    private boolean tapGesture(float x, float y) {
        // if last tap occurs recently, then return with false output as a result
        if (System.currentTimeMillis() - lastTapPressed < 500) return false;

        // setting the value of last tap pressed for further use
        lastTapPressed = System.currentTimeMillis();

        // creating a path variable and then move to the node coordinates where tap gesture stimulated
        Path path = new Path();
        path.moveTo(x, y);

        // adding stroke or tap on the screen
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 50L, 80L);

        //  build prepared using stroke
        GestureDescription gestureDescription = new GestureDescription.Builder().addStroke(stroke).build();

        showTouchOnScreen(x, y);
        // gesture is performed and result is given to the parent calling function, where its called
        return dispatchGesture(gestureDescription, null, null);
    }

    private void generateVibration() {
        // used to produce vibration after ads skipped
        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Getting Vibration manager service
            VibratorManager vibratorManager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            // Getting mobile vibrator
            vibrator = vibratorManager.getDefaultVibrator();
        } else {
            // if android version is 11 or lower, it will be used
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }

        // safety check in case vibrator is not present in device or null in worst case
        if (vibrator == null || !vibrator.hasVibrator()) return;


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //  adding vibration effect duration and intensity
            VibrationEffect vibrationEffect = VibrationEffect.createOneShot(50, 50);
            // start vibrating
            vibrator.vibrate(vibrationEffect);
        } else {
            // if android version is 7 or lower, it will be used
            vibrator.vibrate(50);
        }
    }

    private void sendNotification() {
        // used to notify user about Total ads count
        NotificationChannel notificationChannel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(
                    notificationId,
                    "Pocket FM Ads Skipper",
                    NotificationManager.IMPORTANCE_MIN
            );
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, notificationId)
                .setContentTitle("Total Clicks Happens :- " + totalAdSkippedCount)
                .setSmallIcon(R.drawable.pocket_fm_icon)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(1, builder.build());
        }
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public boolean onUnbind(Intent intent) {
        // Called when service is closed by user from accessibility setting
        Toast.makeText(this, "Thanks For using our service", Toast.LENGTH_SHORT).show();
        totalAdSkippedCount = 0;
        return true;
    }

    @Override
    protected void onServiceConnected() {
        // called when service started by the user, it shows a toast
        super.onServiceConnected();
        Toast.makeText(this, "Service started, start enjoying", Toast.LENGTH_SHORT).show();
    }

    private void showTouchOnScreen(float x, float y) {
        if (windowManager == null) {
            windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        }
        View view = new View(this);

        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.OVAL);
        gradientDrawable.setColor(Color.YELLOW);
        gradientDrawable.setStroke(4, Color.GREEN);
        view.setBackground(gradientDrawable);

        int size = 60;
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                size,
                size,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.x = (int) (x - size / 2f);
        layoutParams.y = (int) (y - size / 2f);

        try {
            windowManager.addView(view, layoutParams);
        } catch (Exception e) {
            return;
        }

        view.postDelayed(() -> {
            try {
                windowManager.removeView(view);
            } catch (Exception e) {
                return;
            }
        }, 500);
    }
}
