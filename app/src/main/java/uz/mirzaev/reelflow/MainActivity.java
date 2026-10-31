package uz.mirzaev.reelflow;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(14, 18, 32);
    private static final int CARD = Color.rgb(27, 34, 54);
    private static final int TEXT = Color.WHITE;
    private static final int MUTED = Color.rgb(177, 187, 211);
    private static final int ACCENT = Color.rgb(130, 140, 255);
    private LinearLayout page;
    private TextView status;
    private TextView intervalText;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        build();
    }

    @Override protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private LinearLayout box() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(18), dp(18), dp(18), dp(18));
        l.setBackground(shape(CARD, 20));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(16);
        page.addView(l, lp);
        return l;
    }

    private Button button(String title, boolean filled) {
        Button b = new Button(this);
        b.setText(title);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTextColor(filled ? Color.WHITE : Color.rgb(214, 219, 255));
        b.setBackground(shape(filled ? ACCENT : Color.rgb(49, 58, 83), 12));
        b.setElevation(dp(0));
        b.setMinHeight(dp(48));
        return b;
    }

    private void addGap(LinearLayout parent, int h) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(1, dp(h)));
    }

    private void build() {
        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setBackgroundColor(BG);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(26), dp(20), dp(28));
        scroller.addView(page);
        setContentView(scroller);

        page.addView(text("↥  ReelFlow", 30, TEXT, true));
        addGap(page, 4);
        page.addView(text("Автолистание Reels и VK Клипов", 15, MUTED, false));
        addGap(page, 24);

        LinearLayout permission = box();
        permission.addView(text("01  ДОСТУП", 13, ACCENT, true));
        addGap(permission, 10);
        status = text("Проверяем службу...", 15, TEXT, true);
        permission.addView(status);
        addGap(permission, 10);
        permission.addView(text("Однократно включи ReelFlow в специальных возможностях Android.", 13, MUTED, false));
        addGap(permission, 16);
        Button enable = button("Открыть специальные возможности", true);
        permission.addView(enable, new LinearLayout.LayoutParams(-1, dp(49)));
        enable.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        LinearLayout apps = box();
        apps.addView(text("02  ГДЕ ЛИСТАТЬ", 13, ACCENT, true));
        addGap(apps, 8);
        addSwitch(apps, "Instagram Reels", Config.INSTAGRAM);
        addSwitch(apps, "VK Клипы", Config.VK_CLIPS);
        addSwitch(apps, "VK Видео", Config.VK_VIDEO);
        addSwitch(apps, "ВКонтакте (клипы внутри VK)", Config.VK);

        LinearLayout settings = box();
        settings.addView(text("03  КОГДА ЛИСТАТЬ", 13, ACCENT, true));
        addGap(settings, 12);
        Switch smart = new Switch(this);
        smart.setText("Умное определение окончания");
        smart.setTextSize(14);
        smart.setTextColor(TEXT);
        smart.setChecked(Config.smart(this));
        smart.setOnCheckedChangeListener((b, checked) ->
                Config.prefs(this).edit().putBoolean(Config.SMART, checked).apply());
        settings.addView(smart);
        addGap(settings, 8);
        settings.addView(text("Если видеоплеер показывает доступный индикатор просмотра, переключение происходит в конце. Иначе используется интервал ниже.", 12, MUTED, false));
        addGap(settings, 18);
        intervalText = text("Интервал: " + Config.seconds(this) + " сек.", 16, TEXT, true);
        settings.addView(intervalText);
        SeekBar seek = new SeekBar(this);
        seek.setMax(34);
        seek.setProgress((Config.seconds(this) - 10) / 5);
        settings.addView(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar v, int progress, boolean fromUser) {
                int seconds = 10 + progress * 5;
                intervalText.setText("Интервал: " + seconds + " сек.");
                if (fromUser) Config.prefs(MainActivity.this).edit().putInt(Config.INTERVAL, seconds).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar v) {}
            @Override public void onStopTrackingTouch(SeekBar v) {}
        });
        settings.addView(text("10 секунд                                         3 минуты", 11, MUTED, false));

        LinearLayout launch = box();
        launch.addView(text("04  ОТКРЫТЬ ВИДЕО", 13, ACCENT, true));
        addGap(launch, 12);
        launch.addView(text("Открой Reels или клипы и нажми ▶ на плавающей панели справа. Там же можно остановить автоматическое перелистывание.", 13, MUTED, false));
        addGap(launch, 14);
        addLaunch(launch, "Открыть Instagram", Config.INSTAGRAM);
        addLaunch(launch, "Открыть VK Клипы", Config.VK_CLIPS);
        addLaunch(launch, "Открыть VK Видео", Config.VK_VIDEO);
        addLaunch(launch, "Открыть ВКонтакте", Config.VK);
        TextView foot = text("By Mirzaev I  •  v0.1", 12, MUTED, false);
        foot.setGravity(Gravity.CENTER);
        page.addView(foot);
    }

    private void addSwitch(LinearLayout parent, String name, String pkg) {
        Switch sw = new Switch(this);
        sw.setText(name);
        sw.setTextSize(14);
        sw.setTextColor(TEXT);
        sw.setPadding(0, dp(7), 0, dp(7));
        sw.setChecked(Config.prefs(this).getBoolean("app_" + pkg, true));
        sw.setOnCheckedChangeListener((button, enabled) ->
                Config.prefs(this).edit().putBoolean("app_" + pkg, enabled).apply());
        parent.addView(sw);
    }

    private void addLaunch(LinearLayout parent, String title, String packageName) {
        Button b = button(title, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(47));
        lp.bottomMargin = dp(8);
        parent.addView(b, lp);
        b.setOnClickListener(v -> {
            if (!isAccessibilityEnabled()) {
                Toast.makeText(this, "Сначала включи службу ReelFlow", Toast.LENGTH_LONG).show();
                return;
            }
            Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent == null) {
                Toast.makeText(this, "Приложение не установлено", Toast.LENGTH_LONG).show();
            } else {
                startActivity(intent);
            }
        });
    }

    private boolean isAccessibilityEnabled() {
        String list = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (list == null) return false;
        String serviceName = new ComponentName(this, AutoScrollService.class).flattenToString();
        for (String entry : list.split(":")) {
            if (entry.equalsIgnoreCase(serviceName)) return true;
        }
        return false;
    }

    private void refreshStatus() {
        if (status == null) return;
        if (isAccessibilityEnabled()) {
            status.setText("● Служба включена");
            status.setTextColor(Color.rgb(111, 224, 167));
        } else {
            status.setText("○ Служба выключена");
            status.setTextColor(Color.rgb(253, 177, 116));
        }
    }
}
