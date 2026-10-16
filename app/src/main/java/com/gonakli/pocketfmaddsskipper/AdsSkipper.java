package com.gonakli.pocketfmaddsskipper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class AdsSkipper extends AccessibilityService {
    String[] checkPackages = {
            "com.radio.pocketfm"
    };
    String playStorePackageName = "com.android.vending";

    String[] keywords = {
            "skip", "close", "closeall", "close ad",
            "google play", "unlock 2 more episodes", "close video", "try again",
            "dismiss", "iv_close", "btn_close", "img_close",
            "ad_close", "ad_skip", "cross", "btn_x", "iv_x",
            "innobi", "close_btn", "close_img", "no thanks", "not now"
    };
    private long lastBackPressed = 0;
    private long lastTapPressed = 0;


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

        // logic for exiting google play while watching advertisement and ads redirect to google play
        if (currentPackageName.equalsIgnoreCase(playStorePackageName)) {
            // performing back press action till play store package is active
            if(System.currentTimeMillis() - lastBackPressed > 1500) {
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
            // lastBackPressed used to avoid multiple clicks at a same time
            lastBackPressed = System.currentTimeMillis();
            return;
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
                } else if (text.equalsIgnoreCase("300x300") || description.equalsIgnoreCase("300x300")) {
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

        // logging result and node clickable in the logcat for inspections
        Log.d("result", "is node clickable: : " + node.isClickable() );
        Log.d("result", "result of tapGesture function is: " + result );

    }

    private boolean tapGesture(float x, float y) {
        // if last tap occurs recently, then return with false output as a result
        if(System.currentTimeMillis() - lastTapPressed < 1500) return false;

        // setting the value of last tap pressed for further use
        lastTapPressed = System.currentTimeMillis();

        // creating a path variable and then move to the node coordinates where tap gesture stimulated
        Path path = new Path();
        path.moveTo(x, y);

        // adding stroke or tap on the screen
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, 50L);

        //  build prepared using stroke
        GestureDescription gestureDescription = new GestureDescription.Builder().addStroke(stroke).build();

        // gesture is performed and result is given to the parent calling function, where its called
        return dispatchGesture(gestureDescription, null, null);
    }

    @Override
    public void onInterrupt() {

    }

}
