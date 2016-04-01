package io.github.xhinliang.mdpreference;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

@SuppressWarnings("unused")
public abstract class TwoStatePreference extends Preference {

    private static final String TAG = "TwoStatePreference";

    private CharSequence summaryOn;
    private CharSequence summaryOff;
    private boolean isChecked;
    private boolean isCheckedSet;
    private boolean disableDependentsState;

    public TwoStatePreference(Context context) {
        super(context);
    }

    public TwoStatePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TwoStatePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TwoStatePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @SuppressWarnings("ResourceType")
    protected void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, new int[]{
                android.R.attr.summaryOn, android.R.attr.summaryOff, android.R.attr.disableDependentsState
        }, defStyleAttr, defStyleRes);
        setSummaryOn(typedArray.getString(0));
        setSummaryOff(typedArray.getString(1));
        setDisableDependentsState(typedArray.getBoolean(2, false));
        typedArray.recycle();
    }


    @Override
    protected void onClick() {
        super.onClick();
        boolean newValue = !isChecked();
        if (callChangeListener(newValue)) {
            setChecked(newValue);
        }
    }

    /**
     * Set the checked state and saves it to the {@link SharedPreferences}.
     *
     * @param checked The checked state.
     */
    public void setChecked(boolean checked) {
        // Always persist/notify the first time; don't assume the field's default of false.
        boolean changed = isChecked != checked;
        if (changed || !isCheckedSet) {
            isChecked = checked;
            isCheckedSet = true;
            persistBoolean(checked);
            if (changed) {
                notifyDependencyChange(shouldDisableDependents());
                notifyChanged();
            }
        }
    }


    /**
     * Returns the checked state.
     *
     * @return The checked state.
     */
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public boolean shouldDisableDependents() {
        boolean shouldDisable = disableDependentsState == isChecked;
        return shouldDisable || super.shouldDisableDependents();
    }

    /**
     * Sets the summary to be shown when checked.
     *
     * @param summary The summary to be shown when checked.
     */
    public void setSummaryOn(CharSequence summary) {
        summaryOn = summary;
        if (isChecked()) {
            notifyChanged();
        }
    }

    /**
     * @param summaryResId The summary as a resource.
     * @see #setSummaryOn(CharSequence)
     */
    public void setSummaryOn(int summaryResId) {
        setSummaryOn(getContext().getString(summaryResId));
    }

    /**
     * Returns the summary to be shown when checked.
     *
     * @return The summary.
     */
    public CharSequence getSummaryOn() {
        return summaryOn;
    }

    /**
     * Sets the summary to be shown when unchecked.
     *
     * @param summary The summary to be shown when unchecked.
     */
    public void setSummaryOff(CharSequence summary) {
        summaryOff = summary;
        if (!isChecked()) {
            notifyChanged();
        }
    }

    /**
     * @param summaryResId The summary as a resource.
     * @see #setSummaryOff(CharSequence)
     */
    public void setSummaryOff(int summaryResId) {
        setSummaryOff(getContext().getString(summaryResId));
    }

    /**
     * Returns the summary to be shown when unchecked.
     *
     * @return The summary.
     */
    public CharSequence getSummaryOff() {
        return summaryOff;
    }

    /**
     * Returns whether dependents are disabled when this preference is on ({@code true})
     * or when this preference is off ({@code false}).
     *
     * @return Whether dependents are disabled when this preference is on ({@code true})
     * or when this preference is off ({@code false}).
     */
    public boolean getDisableDependentsState() {
        return disableDependentsState;
    }

    /**
     * Sets whether dependents are disabled when this preference is on ({@code true})
     * or when this preference is off ({@code false}).
     *
     * @param disableDependentsState The preference state that should disable dependents.
     */
    public void setDisableDependentsState(boolean disableDependentsState) {
        this.disableDependentsState = disableDependentsState;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getBoolean(index, false);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setChecked(restoreValue ? getPersistedBoolean(isChecked) : (Boolean) defaultValue);
    }

    /**
     * Sync a summary view contained within view's sub hierarchy with the correct summary text.
     */
    void syncSummaryView() {
        Log.d(TAG, "syncSummaryView");
        // Sync the summary view
        boolean useDefaultSummary = true;
        if (isChecked && !TextUtils.isEmpty(summaryOn)) {
            summaryText.setText(summaryOn);
            useDefaultSummary = false;
        } else if (!isChecked && !TextUtils.isEmpty(summaryOff)) {
            summaryText.setText(summaryOff);
            useDefaultSummary = false;
        }
        if (useDefaultSummary) {
            CharSequence summary = getSummary();
            if (!TextUtils.isEmpty(summary)) {
                summaryText.setText(summary);
                useDefaultSummary = false;
            }
        }
        int newVisibility = View.GONE;
        if (!useDefaultSummary) {
            // Someone has written to it
            newVisibility = View.VISIBLE;
        }
        if (newVisibility != summaryText.getVisibility()) {
            summaryText.setVisibility(newVisibility);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }
        SavedState myState = new SavedState(superState);
        myState.checked = isChecked();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setChecked(myState.checked);
    }

    static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        boolean checked;

        public SavedState(Parcel source) {
            super(source);
            checked = source.readInt() == 1;
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(checked ? 1 : 0);
        }
    }
}

