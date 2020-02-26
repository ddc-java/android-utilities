/*
 *    Copyright 2020 Deep Dive Coding/CNM Ingenuity, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package edu.cnm.deepdive.android;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.DatePicker;
import android.widget.TimePicker;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import java.util.Calendar;

/**
 * Simple date/time picker, implemented as a {@link DialogFragment} wrapping a {@link
 * DatePickerDialog} or {@link TimePickerDialog} (depending on the selected {@link Mode}). This
 * class is intended primarily to simplify &amp; unify the use of the underlying dialogs, and to
 * demonstrate dialog-to-host interaction.
 *
 * @author Nicholas Bennett, Deep Dive Coding
 * @version 3.0.0
 */
public class DateTimePickerFragment extends DialogFragment
    implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

  private static final String CALENDAR_KEY = "calendar";
  private static final String MODE_KEY = "mode";

  private Mode mode;
  private Calendar calendar;
  private OnChangeListener listener;

  /**
   * Creates and returns an instance of {@link DateTimePickerFragment}, configured for the specified
   * selection {@link Mode}, using {@code calendar} as the starting date/time.
   *
   * @param mode {@link Mode#DATE} or {@link Mode#TIME} selection mode.
   * @param calendar initially selected date/time.
   * @return prepared instance of {@link DateTimePickerFragment}, ready for invocation of the {@link
   * #show(FragmentManager, String)} method.
   */
  public DateTimePickerFragment createInstance(Mode mode, Calendar calendar) {
    DateTimePickerFragment fragment = new DateTimePickerFragment();
    Bundle args = new Bundle();
    args.putSerializable(MODE_KEY, mode);
    args.putSerializable(CALENDAR_KEY, calendar);
    fragment.setArguments(args);
    return fragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    Dialog dialog;
    readArguments();
    setupListener();
    if (mode == Mode.DATE) {
      dialog = new DatePickerDialog(getActivity(), this, calendar.get(Calendar.YEAR),
          calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
    } else {
      dialog = new TimePickerDialog(getActivity(), this, calendar.get(Calendar.HOUR_OF_DAY),
          calendar.get(Calendar.MINUTE), DateFormat.is24HourFormat(getActivity()));
    }
    return dialog;
  }

  @Override
  public final void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
    Calendar updateValue = Calendar.getInstance();
    updateValue.setTimeInMillis(calendar.getTimeInMillis());
    updateValue.set(Calendar.YEAR, year);
    updateValue.set(Calendar.MONTH, month);
    updateValue.set(Calendar.DAY_OF_MONTH, dayOfMonth);
    listener.onChange(updateValue);
  }

  @Override
  public final void onTimeSet(TimePicker view, int hourOfDay, int minute) {
    Calendar updateValue = Calendar.getInstance();
    updateValue.setTimeInMillis(calendar.getTimeInMillis());
    updateValue.set(Calendar.HOUR_OF_DAY, hourOfDay);
    updateValue.set(Calendar.MINUTE, minute);
    listener.onChange(updateValue);
  }

  private void readArguments() {
    Bundle args = getArguments();
    if (args == null
        || !args.containsKey(MODE_KEY)
        || (mode = (Mode) args.getSerializable(MODE_KEY)) == null) {
      mode = Mode.DATE;
    }
    if (args == null
        || !args.containsKey(CALENDAR_KEY)
        || (calendar = (Calendar) args.getSerializable(CALENDAR_KEY)) == null) {
      calendar = Calendar.getInstance();
    }
  }

  private void setupListener() {
    Fragment parentFragment = getParentFragment();
    FragmentActivity hostActivity = getActivity();
    if (parentFragment instanceof OnChangeListener) {
      listener = (OnChangeListener) parentFragment;
    } else if (hostActivity instanceof OnChangeListener) {
      listener = (OnChangeListener) hostActivity;
    } else {
      listener = new OnChangeListener() {
        @Override
        public void onChange(Calendar calendar) {}
      };
    }
  }

  /**
   * Enumerates the two possible modes of operation of {@link DateTimePickerFragment}.
   */
  public enum Mode {
    DATE, TIME
  }

  /**
   * Event handler for positive dismissal of the {@link DateTimePickerFragment}. In order to receive
   * the updated date/time value, the parent fragment or host activity must implement this
   * interface.
   */
  public interface OnChangeListener {

    /**
     * Handles the user-selected date-time.
     *
     * @param calendar user-selected date-time.
     */
    void onChange(Calendar calendar);

  }

}
