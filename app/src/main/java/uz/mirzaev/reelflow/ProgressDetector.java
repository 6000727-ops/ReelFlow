package uz.mirzaev.reelflow;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Best-effort progress detection. */
final class ProgressDetector {
    private static final Pattern TIMES = Pattern.compile(
        "(?<!\\d)(\\d{1,2}:\\d{2}(?::\\d{2})?)\\s*(?:/|из|of|\\u2014)\\s*(\\d{1,2}:\\d{2}(?::\\d{2})?)(?!\\d)",
        Pattern.CASE_INSENSITIVE);

    static double probe(AccessibilityNodeInfo root, int screenWidth) {
        if (root == null) return -1;
        ArrayDeque<AccessibilityNodeInfo> pending = new ArrayDeque<>();
        pending.add(root);
        int checked = 0;
        double fallback = -1;
        while (!pending.isEmpty() && checked++ < 180) {
            AccessibilityNodeInfo node = pending.removeFirst();
            try {
                CharSequence clazz = node.getClassName();
                String name = clazz == null ? "" : clazz.toString().toLowerCase(java.util.Locale.ROOT);
                if (name.contains("seekbar") || name.contains("progressbar") || name.contains("slider")) {
                    Rect b = new Rect();
                    node.getBoundsInScreen(b);
                    AccessibilityNodeInfo.RangeInfo range = node.getRangeInfo();
                    if (range != null && b.width() >= screenWidth * 0.35f
                            && b.height() <= 150 && range.getMax() > range.getMin()) {
                        double f = (range.getCurrent() - range.getMin()) / (range.getMax() - range.getMin());
                        if (f >= 0 && f <= 1) return f;
                    }
                }
                double t = parse(node.getContentDescription());
                if (t < 0) t = parse(node.getText());
                if (t >= 0 && fallback < 0) fallback = t;
                for (int i = 0; i < node.getChildCount(); i++) {
                    AccessibilityNodeInfo child = node.getChild(i);
                    if (child != null) pending.addLast(child);
                }
            } finally {
                if (node != root) node.recycle();
            }
        }
        while (!pending.isEmpty()) pending.removeFirst().recycle();
        return fallback;
    }

    private static double parse(CharSequence value) {
        if (value == null) return -1;
        Matcher m = TIMES.matcher(value);
        if (!m.find()) return -1;
        int current = asSeconds(m.group(1));
        int total = asSeconds(m.group(2));
        if (total < 3 || total > 10800 || current < 0 || current > total) return -1;
        return (double) current / total;
    }

    private static int asSeconds(String raw) {
        String[] parts = raw.split(":");
        int total = 0;
        for (String p : parts) total = total * 60 + Integer.parseInt(p);
        return total;
    }
}
