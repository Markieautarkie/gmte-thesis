package com.thesis.mtbalance;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {

    /* Keys */
    public static final String KEY_PARTICIPANT_NUMBER = "participant_number";
    public static final String KEY_PREFERRED_FEEDBACK = "preferred_feedback";

    public static final String KEY_PLOT_RESOLUTION = "plot_resolution";
    public static final String KEY_THRESHOLD_LENIENCY = "threshold_leniency";

    public static final String KEY_CRANK_LENGTH = "crank_length";
    public static final String KEY_LOWER_LEG_LENGTH = "lower_leg_length";
    public static final String KEY_UPPER_LEG_LENGTH = "upper_leg_length";

    public static final String KEY_OFFSET_DIMENSION = "offset_dimension";
    public static final String KEY_HIP_DIMENSION = "hip_dimension";

    /**
     * Called on activity creation.
     *
     * @param savedInstanceState - state holding saved info from onPause callback.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        /**
         * Called when preferences window is created.
         *
         * @param savedInstanceState - state holding saved info from onPause callback.
         * @param rootKey            - the root key of the sharedPreferences list.
         */
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            // Set the keyboard for dimension settings to numerical only
            setNumericalKeyboard(KEY_PARTICIPANT_NUMBER);

            setNumericalKeyboard(KEY_PLOT_RESOLUTION);

            setNumericalKeyboard(KEY_CRANK_LENGTH);
            setNumericalKeyboard(KEY_LOWER_LEG_LENGTH);
            setNumericalKeyboard(KEY_UPPER_LEG_LENGTH);

            setNumericalKeyboard(KEY_OFFSET_DIMENSION);
            setNumericalKeyboard(KEY_HIP_DIMENSION);
        }

        /**
         * Sets the keyboard of an editText to numerical only.
         *
         * @param key - the key of the value to set to numerical only.
         */
        private void setNumericalKeyboard(String key) {
            EditTextPreference etp = getPreferenceManager().findPreference(key);
            if (etp != null) {
                etp.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
                    @Override
                    public void onBindEditText(@NonNull EditText editText) {
                        editText.setInputType(InputType.TYPE_CLASS_PHONE);
                    }
                });
            }
        }
    }
}
