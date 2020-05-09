package com.gallery.album.ui;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.preference.SwitchPreference;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.ActionBar;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.TwoStatePreference;
import androidx.appcompat.widget.Toolbar;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.RelativeLayout;

import java.util.Arrays;

import com.gallery.album.R;
import com.gallery.album.themes.Theme;
import com.gallery.album.data.Settings;
import com.gallery.album.preferences.ColumnCountPreference;
import com.gallery.album.preferences.ColumnCountPreferenceDialogFragment;
import com.gallery.album.preferences.StylePreference;
import com.gallery.album.preferences.StylePreferenceDialogFragment;
import com.gallery.album.util.NetworkStateReceiver;
import com.gallery.album.util.Util;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

public class SettingsActivity extends ThemeableActivity implements NetworkStateReceiver.NetworkStateReceiverListener {

    private static boolean recreated = false;
    RelativeLayout rlmAdView;
    NetworkStateReceiver networkStateReceiver;
    AdView mAdView;
    InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        rlmAdView = findViewById(R.id.rlmAdView);
        //setting window insets manually
        final View rootView = findViewById(R.id.root_view);
        final View container = findViewById(R.id.preference_fragment_container);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            rootView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
                public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                    toolbar.setPadding(toolbar.getPaddingStart() /*+ insets.getSystemWindowInsetLeft()*/,
                            toolbar.getPaddingTop() + insets.getSystemWindowInsetTop(),
                            toolbar.getPaddingEnd() /*+ insets.getSystemWindowInsetRight()*/,
                            toolbar.getPaddingBottom());

                    ViewGroup.MarginLayoutParams toolbarParams
                            = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
                    toolbarParams.leftMargin += insets.getSystemWindowInsetLeft();
                    toolbarParams.rightMargin += insets.getSystemWindowInsetRight();
                    toolbar.setLayoutParams(toolbarParams);

                    container.setPadding(container.getPaddingStart() + insets.getSystemWindowInsetLeft(),
                            container.getPaddingTop(),
                            container.getPaddingEnd() + insets.getSystemWindowInsetRight(),
                            container.getPaddingBottom() + insets.getSystemWindowInsetBottom());

                    // clear this listener so insets aren't re-applied
                    rootView.setOnApplyWindowInsetsListener(null);
                    return insets.consumeSystemWindowInsets();
                }
            });
        } else {
            rootView.getViewTreeObserver()
                    .addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    // hacky way of getting window insets on pre-Lollipop
                                    // somewhat works...
                                    int[] screenSize = Util.getScreenSize(SettingsActivity.this);

                                    int[] windowInsets = new int[]{
                                            Math.abs(screenSize[0] - rootView.getLeft()),
                                            Math.abs(screenSize[1] - rootView.getTop()),
                                            Math.abs(screenSize[2] - rootView.getRight()),
                                            Math.abs(screenSize[3] - rootView.getBottom())};

                                    Log.d("MainActivity", "windowInsets: " + Arrays.toString(windowInsets));

                                    toolbar.setPadding(toolbar.getPaddingStart(),
                                            toolbar.getPaddingTop() + windowInsets[1],
                                            toolbar.getPaddingEnd(),
                                            toolbar.getPaddingBottom());

                                    ViewGroup.MarginLayoutParams toolbarParams
                                            = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
                                    toolbarParams.leftMargin += windowInsets[0];
                                    toolbarParams.rightMargin += windowInsets[2];
                                    toolbar.setLayoutParams(toolbarParams);

                                    container.setPadding(container.getPaddingStart() + windowInsets[0],
                                            container.getPaddingTop(),
                                            container.getPaddingEnd() + windowInsets[2],
                                            container.getPaddingBottom() + windowInsets[3]);

                                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                }
                            });
        }

        SettingsFragment fragment = new SettingsFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.preference_fragment_container, fragment)
                .commit();

        fragment.setCallback(new SettingsFragment.OnSettingChangedCallback() {
            @Override
            public void onSettingChanged() {
                setResult(RESULT_OK);
            }
        });

        setSystemUiFlags();


        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(this);
        this.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
        if (Util.isNetworkAvailable(this)) {
            displayInterstitialAd();
        }

    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    void displayInterstitialAd() {

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.admob_Interstitial_Ad));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                mInterstitialAd.show();
                // Code to be executed when an ad finishes loading.
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.
                Log.e("errorCode", "The interstitial wasn't loaded yet." + errorCode);
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when the ad is displayed.
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
            }

            @Override
            public void onAdClosed() {

                // Code to be executed when when the interstitial ad is closed.
            }
        });
    }

    @Override
    public void recreate() {
        recreated = true;
        super.recreate();
    }

    @Override
    public void onBackPressed() {
        if (recreated) {
            setResult(RESULT_OK);
        }
        super.onBackPressed();
    }

    @Override
    public int getDarkThemeRes() {
        return R.style.Gallery_Theme_Settings;
    }

    @Override
    public int getLightThemeRes() {
        return R.style.Gallery_Theme_Light_Settings;
    }

    @Override
    public void onThemeApplied(Theme theme) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(toolbarColor);
        toolbar.setTitleTextColor(textColorPrimary);

        if (theme.darkStatusBarIcons() &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Util.setDarkStatusBarIcons(findViewById(R.id.root_view));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int statusBarColor = getStatusBarColor();
            getWindow().setStatusBarColor(statusBarColor);
        }
    }


    public static class SettingsFragment extends PreferenceFragmentCompat
            implements Preference.OnPreferenceChangeListener {

        private static final String DIALOG_FRAGMENT_TAG
                = "android.support.v7.preference.PreferenceFragment.DIALOG";
        private static final String SHOWN_DIALOG_FRAGMENT = "SHOWN_DIALOG_FRAGMENT";
        private static final int NONE = 0;
        private static final int STYLE_DIALOG_FRAGMENT = 1;
        private static final int COLUMN_COUNT_DIALOG_FRAGMENT = 2;

        private int shownDialogFragment = NONE;
        private OnSettingChangedCallback callback;


        interface OnSettingChangedCallback {
            void onSettingChanged();
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.preferences);

            Settings settings = Settings.getInstance(getContext());

            initExcludedPathsPref();
            initVirtualDirectoriesPref();
            initThemePref(settings.getTheme());
            initStylePref(settings.getStyle(getContext(), false));
            initColumnCountPref(settings.getRealColumnCount());
            initShowVideos(settings.showVideos());
            initMediaRetrieverPref(settings.useStorageRetriever());
            init8BitColorPref(settings.use8BitColor());
            initCameraShortcutPref(settings.getCameraShortcut());
            initAnimationsPref(settings.showAnimations());
            initMaxBrightnessPref(settings.isMaxBrightness());

            if (savedInstanceState != null
                    && savedInstanceState.containsKey(SHOWN_DIALOG_FRAGMENT)) {
                int shownDialogFragment = savedInstanceState.getInt(SHOWN_DIALOG_FRAGMENT);
                Preference preference = null;
                if (shownDialogFragment == STYLE_DIALOG_FRAGMENT) {
                    preference = findPreference(getString(R.string.pref_key_style));
                } else if (shownDialogFragment == COLUMN_COUNT_DIALOG_FRAGMENT) {
                    preference = findPreference(getString(R.string.pref_key_column_count));
                }

                if (preference != null) {
                    onDisplayPreferenceDialog(preference);
                }
            }
        }

        private void initExcludedPathsPref() {
            Preference pref = findPreference(getString(R.string.pref_key_excluded_paths));
            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (callback != null) {
                        callback.onSettingChanged();
                    }
                    Intent intent = new Intent(getContext(), ExcludePathsActivity.class);
                    getContext().startActivity(intent);
                    return false;
                }
            });
        }

        private void initVirtualDirectoriesPref() {
            Preference pref = findPreference(getString(R.string.pref_key_virtual_directories));
            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (callback != null) {
                        callback.onSettingChanged();
                    }
                    Intent intent = new Intent(getContext(), VirtualAlbumsActivity.class);
                    getContext().startActivity(intent);
                    return false;
                }
            });
        }

        private void initThemePref(String theme) {
            ListPreference themePref = (ListPreference)
                    findPreference(getString(R.string.pref_key_theme));

            String theme_name = Settings.Utils.getThemeName(getActivity(), theme);
            themePref.setSummary(theme_name);
            themePref.setOnPreferenceChangeListener(this);
        }

        private void initStylePref(int style) {
            StylePreference stylePref = (StylePreference)
                    findPreference(getString(R.string.pref_key_style));

            String style_name = Settings.Utils.getStyleName(getActivity(), style);
            stylePref.setSummary(style_name);
            stylePref.setOnPreferenceChangeListener(this);
        }

        private void initColumnCountPref(int column_count) {
            ColumnCountPreference columnCountPref = (ColumnCountPreference)
                    findPreference(getString(R.string.pref_key_column_count));

            columnCountPref.setSummary(String.valueOf(column_count));
            columnCountPref.setOnPreferenceChangeListener(this);
        }

        private void initShowVideos(boolean hide) {
            SwitchPreference prefs = (SwitchPreference) findPreference(getString(R.string.pref_key_show_videos));
            prefs.setChecked(hide);
            prefs.setOnPreferenceChangeListener(this);
        }

        private void initMediaRetrieverPref(boolean storageRetriever) {
            TwoStatePreference mediaRetrieverPref =
                    (TwoStatePreference) findPreference(getString(R.string.pref_key_media_retriever));

            mediaRetrieverPref.setChecked(storageRetriever);
            mediaRetrieverPref.setOnPreferenceChangeListener(this);
        }

        private void init8BitColorPref(boolean use8BitColor) {
            TwoStatePreference use8BitColorPref =
                    (TwoStatePreference) findPreference(getString(R.string.pref_key_8_bit_color));

            use8BitColorPref.setChecked(use8BitColor);
            use8BitColorPref.setOnPreferenceChangeListener(this);
        }

        private void initCameraShortcutPref(boolean cameraShortcut) {
            TwoStatePreference cameraShortcutPref =
                    (TwoStatePreference) findPreference(getString(R.string.pref_key_camera_shortcut));

            cameraShortcutPref.setChecked(cameraShortcut);
            cameraShortcutPref.setOnPreferenceChangeListener(this);
        }

        private void initAnimationsPref(boolean showAnimations) {
            TwoStatePreference animationsPref =
                    (TwoStatePreference) findPreference(getString(R.string.pref_key_animations));

            animationsPref.setChecked(showAnimations);
            animationsPref.setOnPreferenceChangeListener(this);
        }

        private void initMaxBrightnessPref(boolean maxBrightness) {
            TwoStatePreference animationsPref =
                    (TwoStatePreference) findPreference(getString(R.string.pref_key_max_brightness));

            animationsPref.setChecked(maxBrightness);
            animationsPref.setOnPreferenceChangeListener(this);
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            if (callback != null) {
                callback.onSettingChanged();
            }

            DialogFragment dialogFragment = null;
            if (preference instanceof StylePreference) {
                dialogFragment
                        = StylePreferenceDialogFragment
                        .newInstance(preference);
            } else if (preference instanceof ColumnCountPreference) {
                dialogFragment
                        = ColumnCountPreferenceDialogFragment
                        .newInstance(preference);
            }

            if (dialogFragment != null) {
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(this.getFragmentManager(), DIALOG_FRAGMENT_TAG);
                return;
            }

            super.onDisplayPreferenceDialog(preference);
        }

        @Override
        public void onPause() {
            super.onPause();

            if (getActivity().isChangingConfigurations()) {
                Fragment fragment =
                        getFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG);
                if (fragment != null && fragment instanceof DialogFragment) {
                    /*if (fragment instanceof StylePreferenceDialogFragment) {
                        shownDialogFragment = STYLE_DIALOG_FRAGMENT;
                    } else if (fragment instanceof ColumnCountPreferenceDialogFragment) {
                        shownDialogFragment = COLUMN_COUNT_DIALOG_FRAGMENT;
                    }*/

                    ((DialogFragment) fragment).dismiss();
                }
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt(SHOWN_DIALOG_FRAGMENT, shownDialogFragment);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            Log.d("SettingsActivity", "onPreferenceChange() called with: preference = [" + preference + "], o = [" + o + "]");
            if (callback != null) {
                callback.onSettingChanged();
            }

            Settings settings = Settings.getInstance(getActivity());
            if (preference.getKey().equals(getString(R.string.pref_key_theme))) {
                String themeValue = (String) o;
                settings.setTheme(themeValue);

                String theme_name = Settings.Utils.getThemeName(getActivity(), themeValue);
                preference.setSummary(theme_name);

                //update Activities
                getActivity().recreate();
            } else if (preference.getKey().equals(getString(R.string.pref_key_style))) {
                settings.setStyle((int) o);
                String style_name = Settings.Utils.getStyleName(getActivity(), (int) o);
                preference.setSummary(style_name);

            } else if (preference.getKey().equals(getString(R.string.pref_key_column_count))) {
                settings.setColumnCount((int) o);
                preference.setSummary(String.valueOf(o));
            } else if (preference.getKey().equals(getString(R.string.pref_key_media_retriever))) {
                settings.useStorageRetriever((boolean) o);
            } else if (preference.getKey().equals(getString(R.string.pref_key_8_bit_color))) {
                settings.use8BitColor((boolean) o);
            } else if (preference.getKey().equals(getString(R.string.pref_key_camera_shortcut))) {
                settings.setCameraShortcut((boolean) o);
            } else if (preference.getKey().equals(getString(R.string.pref_key_animations))) {
                settings.showAnimations((boolean) o);
            } else if (preference.getKey().equals(getString(R.string.pref_key_show_videos))) {
                settings.showVideos((boolean) o);
            } else if (preference.getKey().equals(getString(R.string.pref_key_max_brightness))) {
                settings.setMaxBrightness((boolean) o);
            }
            return true;
        }

        void setCallback(OnSettingChangedCallback callback) {
            this.callback = callback;
        }
    }

    @Override
    public void networkAvailable() {
        admobBanner();
    }

    @Override
    public void networkUnavailable() {
        rlmAdView.removeAllViews();
    }

    void admobBanner() {

        mAdView = new AdView(this);
        mAdView.setAdUnitId(getString(R.string.admob_banner_id));
        rlmAdView.addView(mAdView);
        rlmAdView.setVisibility(View.VISIBLE);
        loadBanner();
    }

    private void loadBanner() {
        AdRequest adRequest = new AdRequest.Builder().build();
        AdSize adSize = getAdSize();
        mAdView.setAdSize(adSize);
        mAdView.loadAd(adRequest);
    }

    private AdSize getAdSize() {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;
        int adWidth = (int) (widthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }
}
