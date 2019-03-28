/*
 *    Copyright 2019 Nicholas Bennett & Deep Dive Coding
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.text.format.DateFormat;
import android.widget.DatePicker;
import android.widget.TimePicker;
import java.util.Calendar;

/**
 * Simple date/time picker, implemented as a {@link DialogFragment} wrapping a {@link
 * DatePickerDialog} or {@link TimePickerDialog} (depending on the selected {@link Mode}). This
 * class is intended not only to simplify the use of the underlying dialogs, but also to encourage a
 * fluent, functional style of use.
 *
 * @author Nicholas Bennett, Deep Dive Coding
 * @version 2.0.0
 */
public class DateTimePickerFragment extends DialogFragment
    implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

  private Mode mode = Mode.DATE;
  private Calendar calendar = Calendar.getInstance();
  private OnChangeListener listener = null;

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    Dialog dialog;
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
    if (listener != null) {
      listener.onChange(updateValue);
    }
  }

  @Override
  public final void onTimeSet(TimePicker view, int hourOfDay, int minute) {
    Calendar updateValue = Calendar.getInstance();
    updateValue.setTimeInMillis(calendar.getTimeInMillis());
    updateValue.set(Calendar.HOUR_OF_DAY, hourOfDay);
    updateValue.set(Calendar.MINUTE, minute);
    if (listener != null) {
      listener.onChange(updateValue);
    }
  }

  /**
   * Returns the configured {@link Mode} (which defaults to {@link Mode#DATE} if not set).
   *
   * @return currently configured {@link Mode}.
   */
  public Mode getMode() {
    return mode;
  }

  /**
   * Sets the configured {@link Mode} (which defaults to {@link Mode#DATE} if not set).
   *
   * @param mode configured {@link Mode} to use when dialog is displayed.
   * @return this {@link DateTimePickerFragment} instance.
   */
  public DateTimePickerFragment setMode(Mode mode) {
    this.mode = mode;
    return this;
  }

  /**
   * Returns the {@link Calendar} instance used for the initially selected current date-time when the dialog is displayed.
   *
   * @return instance of {@link Calendar}.
   */
  public Calendar getCalendar() {
    return calendar;
  }

  /**
   * Sets the {@link Calendar} instance used for the initially selected current date-time when the dialog is displayed.
   *
   * @param calendar instance of {@link Calendar}.
   * @return this {@link DateTimePickerFragment} instance.
   */
  public DateTimePickerFragment setCalendar(Calendar calendar) {
    if (calendar != null) {
      this.calendar.setTimeInMillis(calendar.getTimeInMillis());
    } else {
      this.calendar = Calendar.getInstance();
    }
    return this;
  }

  /**
   * Sets the {@link OnChangeListener} that will be used (invoking {@link OnChangeListener#onChange(Calendar)}) when the user dismisses the dialog by clicking the <strong>OK</strong> button.
   *
   * @param listener callback instance of {@link OnChangeListener} implementation.
   * @return this {@link DateTimePickerFragment} instance.
   */
  public DateTimePickerFragment setOnChangeListener(
      OnChangeListener listener) {
    this.listener = listener;
    return this;
  }

  /**
   * Enumerates the two possible modes of operation of {@link DateTimePickerFragment}.
   */
  public enum Mode {
    DATE, TIME
  }

  /**
   * Event handler for positive dismissal of the {@link DateTimePickerFragment}.
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
