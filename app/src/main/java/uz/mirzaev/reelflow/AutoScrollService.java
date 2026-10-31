package uz.mirzaev.reelflow;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Local-only accessibility service. */
public class AutoScrollService extends AccessibilityService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            tick();
            handler.postDelayed(this, 700);
        }
    };
    private WindowManager windowManager;
    private LinearLayout floatingPanel;
    private TextView playButton;
    private boolean playing = false;
    private boolean gesturePending = false;
    private String activePackage = null;
    private long currentVideoSince = 0;
    private long lastSwipe = 0;
    private long lastProgressAt = 0;
    private double lastProgress = -1;
    private int forwardSamples = 0;

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        String pkg = event.getPackageName().toString();
        if (getPackageName().equals(pkg)) return;
        int type = event.getEventType();
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || type == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            if (Config.allowed(this, pkg) && !pkg.equals(activePackage)) {
                pause();
                activePackage = pkg;
            } else if (!Config.allowed(this, pkg) && activePackage != null) {
                pause();
                activePackage = null;
            }
        } else if (type == AccessibilityEvent.TYPE_VIEW_SCROLLED && playing
                && pkg.equals(activePackage) && SystemClock.elapsedRealtime() - lastSwipe > 1100) {
            resetTimer();
        }
    }

    @Override public void onInterrupt() { pause(); }

    @Override public void onDestroy() {
        pause();
        handler.removeCallbacks(ticker);
        hidePanel();
        super.onDestroy();
    }

    private void tick() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null && !pm.isInteractive()) {
            pause();
            hidePanel();
            return;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            pause();
            hidePanel();
            return;
        }
        try {
            CharSequence packageName = root.getPackageName();
            String pkg = packageName == null ? null : packageName.toString();
            if (!Config.allowed(this, pkg)) {
                if (activePackage != null) pause();
                activePackage = null;
                hidePanel();
                return;
            }
            if (!pkg.equals(activePackage)) {
                pause();
                activePackage = pkg;
            }
            showPanel();
            if (!playing || gesturePending) return;
            long now = SystemClock.elapsedRealtime();
            if (now - lastSwipe < 1800) return;
            if (Config.smart(this)) {
                double progress = ProgressDetector.probe(root, getResources().getDisplayMetrics().widthPixels);
                if (progress >= 0) {
                    boolean increasing = lastProgress >= 0 && progress > lastProgress + .008;
                    if (increasing && now - lastProgressAt > 300) forwardSamples++;
                    if (forwardSamples >= 2 && now - currentVideoSince > 3000) {
                        if (progress >= .985 || (lastProgress >= .90 && progress <= .12)) {
                            swipeNext();
                            return;
                        }
                    }
                    lastProgress = progress;
                    lastProgressAt = now;
                }
            }
            if (now - currentVideoSince >= Config.seconds(this) * 1000L) swipeNext();
        } finally {
            root.recycle();
        }
    }

    private void resetTimer() {
        currentVideoSince = SystemClock.elapsedRealtime();
        lastProgressAt = 0;
        lastProgress = -1;
        forwardSamples = 0;
    }

    private void pause() {
        playing = false;
        resetTimer();
        setPlayIcon();
    }

    private void toggle() {
        if (playing) {
            pause();
        } else {
            if (!Config.allowed(this, activePackage)) return;
            playing = true;
            resetTimer();
            setPlayIcon();
        }
    }

    private void swipeNext() {
        if (!Config.allowed(this, activePackage) || gesturePending) return;
        int w = getResources().getDisplayMetrics().widthPixels;
        int h = getResources().getDisplayMetrics().heightPixels;
        Path path = new Path();
        path.moveTo(w * .50f, h * .78f);
        path.lineTo(w * .50f, h * .28f);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 320));
        gesturePending = true;
        lastSwipe = SystemClock.elapsedRealtime();
        resetTimer();
        boolean sent = dispatchGesture(builder.build(), new GestureResultCallback() {
            @Override public void onCompleted(GestureDescription gesture) { gesturePending = false; }
            @Override public void onCancelled(GestureDescription gesture) { gesturePending = false; }
        }, handler);
        if (!sent) gesturePending = false;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private TextView overlayButton(String label, String description) {
        TextView button = new TextView(this);
        button.setText(label);
        button.setContentDescription(description);
        button.setTextColor(Color.WHITE);
        button.setTextSize(23);
        button.setGravity(Gravity.CENTER);
        button.setClickable(true);
        return button;
    }

    private void setPlayIcon() {
        if (playButton != null) {
            playButton.setText(playing ? "Ⅱ" : "▶");
            playButton.setContentDescription(playing ? "Пауза автолистания" : "Начать автолистание");
        }
    }

    private void showPanel() {
        if (floatingPanel != null) return;
        floatingPanel = new LinearLayout(this);
        floatingPanel.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(235, 31, 40, 69));
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), Color.rgb(91, 99, 150));
        floatingPanel.setBackground(bg);
        floatingPanel.setPadding(dp(2), dp(5), dp(2), dp(5));
        playButton = overlayButton("▶", "Начать автолистание");
        playButton.setOnClickListener(v -> toggle());
        floatingPanel.addView(playButton, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView next = overlayButton("↑", "Следующий ролик");
        next.setOnClickListener(v -> swipeNext());
        floatingPanel.addView(next, new LinearLayout.LayoutParams(dp(44), dp(44)));
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                dp(48), dp(100), WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
        params.x = dp(4);
        params.y = 0;
        params.setTitle("ReelFlow controls");
        try {
            windowManager.addView(floatingPanel, params);
        } catch (RuntimeException error) {
            floatingPanel = null;
            playButton = null;
        }
    }

    private void hidePanel() {
        if (floatingPanel == null) return;
        try { windowManager.removeView(floatingPanel); }
        catch (RuntimeException ignored) {}
        floatingPanel = null;
        playButton = null;
    }
}
