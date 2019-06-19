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

import android.os.AsyncTask;
import androidx.annotation.Nullable;

/**
 * Declares methods, nested interfaces, and a nested exception class, intended to encourage a more
 * fluent, functional, and service-oriented style of extending and using {@link AsyncTask}.
 *
 * <p>One aim of this approach is to encourage implementation of {@link
 * AsyncTask} subclasses that are loosely coupled with consumer logic, and easily reused. For
 * example, if multiple activities or fragments need to perform a similar database operation, but
 * with slight differences in UI updates and other post-processing logic, the general operation can
 * be implemented in a single subclass of {@link FluentAsyncTask}, with variations specified via
 * lambdas in fluent invocations of {@link #setOnCompletionListener(OnResultListener)}, {@link
 * #setOnProgressUpdateListener(OnProgressUpdateListener)}, and (possibly) {@link
 * #setOnFailureListener(OnResultListener)}.</p>
 *
 * <p>For a simple {@link AsyncTask} implementation, with a single point of consumption, an
 * instance
 * of this class can be created and used, specifying all key lifecycle logic via individual lambdas
 * or anonymous classes.</p>
 *
 * <p>Note that this class overrides {@link AsyncTask#doInBackground(Object[])},
 * splitting background processing into 2 phases: performance and observation of completion.
 * Performance is implemented in the {@link #perform(Object[])} method, which (unless overridden)
 * invokes {@link Performer#perform(Object[])} on the {@link Performer} specified in {@link
 * #setPerformer(Performer)}. Observation of completion is implemented by invoking {@link
 * OnResultListener#handle(Object)} on the {@link OnResultListener} (if any) specified in {@link
 * #setOnCompletionListener(OnResultListener)}. Simple extensions or uses of {@link FluentAsyncTask}
 * will generally only need to implement the performance phase&mdash;either by overriding {@link
 * #perform(Object[]) perform(Params...)}, or by specifying a lambda or instance of an anonymous
 * class in an invocation of {@link #setPerformer(Performer)}. However, splittin background
 * processing into 2 parts gives flexibility for additional background processing, as needed.</p>
 *
 * <p><strong>Important notes:</strong></p>
 * <ul>
 * <li><p>
 * This class overrides and makes <code>final</code> the {@link #doInBackground(Object[])}, {@link
 * #onProgressUpdate(Object[])}, {@link #onPostExecute(Object)}, and {@link #onCancelled(Object)}
 * methods. If an app needs to override these methods, then it should create a subclass of {@link
 * AsyncTask}, rather than a subclass of {@link FluentAsyncTask}.
 * </p></li>
 * <li><p>
 * If a consumer wishes the task to do nothing for one or more of the performance, observation,
 * progress, success, or failure phases, there is no need to clear or replace the corresponding
 * listener.
 * </p></li>
 * </ul>
 *
 * @param <Params> type of input parameters used by the asynchronous task (consumed by {@link
 * #perform(Object[])}), or {@link Void} if no inputs will be used.
 * @param <Progress> type of individual values provided by the asynchronous task for progress
 * updates (consumed by {@link #onProgressUpdate(Object[])}), or (usually) {@link Void} if no
 * progress updates will be made.
 * @param <Result> type of result data produced by the background processing of the asynchronous
 * task (returned by {@link Performer#perform(Object[])} and consumed by {@link
 * #onPostExecute(Object)} and {@link #onCancelled(Object)}, or (usually) {@link Void} if the
 * background processing is not intended to return a result.
 * @author Nicholas Bennett, Deep Dive Coding
 * @version 2.0.0
 */
public class FluentAsyncTask<Params, Progress, Result>
    extends AsyncTask<Params, Progress, Result> {

  private Performer<Params, Result> performer;
  private OnProgressUpdateListener<Progress> progressListener;
  private OnResultListener<Result> completionListener;
  private OnResultListener<Result> successListener;
  private OnResultListener<Result> failureListener;
  private RuntimeException exception;

  /**
   * Executes basic processing of asynchronous task. This method will always be invoked on a
   * background thread; no UI modifications should be attempted from this method, but should instead
   * be performed in the lambdas/interface implementations specified to {@link
   * #setOnProgressUpdateListener(OnProgressUpdateListener)}, {@link #setOnSuccessListener(OnResultListener)},
   * and {@link #setOnFailureListener(OnResultListener)}.
   *
   * @param params input arguments to background processing.
   * @return result. the eventual invocation of {@link #onCancelled(Object)}, and consequently, the
   * lambda or {@link OnResultListener} implementation specified to {@link
   * #setOnFailureListener(OnResultListener)}.
   */
  @SuppressWarnings("unchecked")
  @Nullable
  protected Result perform(Params... params) {
    return (performer != null) ? performer.perform(params) : null;
  }

  /**
   * Implements the <code>abstract</code> {@link AsyncTask#doInBackground(Object[])} to divide
   * background processing into 2 components: performance and observation. Any value (including
   * <code>null</code>) returned by {@link #perform(Object[])} will be passed to the {@link
   * OnResultListener} specified in {@link #setOnCompletionListener(OnResultListener)} (if any).
   * <p> If the code of the performance phase throws a {@link RuntimeException}, or invokes {@link
   * #cancel(boolean)}, the observation phase will be skipped, and the listener set (if any) via
   * {@link #setOnFailureListener(OnResultListener)} will be used, instead of any listener set via
   * {@link #setOnSuccessListener(OnResultListener)}.</p>
   * <p>Subclasses of {@link FluentAsyncTask} cannot override this method (since it is declared
   * <code>final</code>), but can instead override {@link #perform(Object[])}, or provide a lambda
   * or {@link Performer} implementation in an invocation of {@link #setPerformer(Performer)}.</p>
   *
   * @param params input arguments to background processing.
   * @return background processing results.
   */
  @SafeVarargs
  @Nullable
  @Override
  protected final Result doInBackground(Params... params) {
    Result result = null;
    try {
      if (!isCancelled()) {
        result = perform(params);
      }
    } catch (RuntimeException e) {
      exception = e;
      cancel(true);
    } finally {
      if (!isCancelled() && completionListener != null) {
        try {
          completionListener.handle(result);
        } catch (RuntimeException e) {
          // Do nothing.
        }
      }
    }
    return null;
  }

  /**
   * Invokes {@link OnProgressUpdateListener#update(Object[])}, presumably to update the UI to
   * display the progress/status of the background processing. Rather than overriding this method
   * (which is not possible, since the method is <code>final</code>), a listener should be attached
   * by invoking {@link #setOnProgressUpdateListener(OnProgressUpdateListener)} on an instance of
   * this class or a subclass.
   *
   * @param values progress
   */
  @SafeVarargs
  @Override
  protected final void onProgressUpdate(Progress... values) {
    super.onProgressUpdate(values);
    if (progressListener != null) {
      progressListener.update(values);
    }
  }

  /**
   * Invokes {@link OnResultListener#handle(Object)} to perform any UI thread-based post-processing
   * after successful completion of background processing. Rather than overriding this method (which
   * is not possible, since the method is <code>final</code>), a listener should be attached by
   * invoking {@link #setOnSuccessListener(OnResultListener)} on an instance of this class or a
   * subclass.
   *
   * @param result background processing results.
   */
  @Override
  protected final void onPostExecute(@Nullable Result result) {
    super.onPostExecute(result);
    if (successListener != null) {
      successListener.handle(result);
    }
  }

  /**
   * Invokes {@link OnResultListener#handle(Object)} to perform any UI thread-based post-processing
   * after unsuccessful completion of background processing. Rather than overriding this method
   * (which is not possible, since the method is <code>final</code>), a listener should be attached
   * by invoking {@link #setOnFailureListener(OnResultListener)} on an instance of this class or a
   * subclass.
   *
   * @param result background processing results.
   */
  @Override
  protected final void onCancelled(@Nullable Result result) {
    super.onCancelled(result);
    if (failureListener != null) {
      failureListener.handle(result);
    }
  }

  /**
   * Specifies the {@link Performer} implementation instance (usually a lambda) to be used for basic
   * background processing. Consumer code may invoke this method directly on an instance of this
   * class, or override {@link #perform(Object[])} in a subclass.
   *
   * @param performer {@link Performer} callback object or lambda.
   * @return this instance (for fluent-style method chaining).
   */
  public FluentAsyncTask<Params, Progress, Result> setPerformer(
      Performer<Params, Result> performer) {
    this.performer = performer;
    return this;
  }

  /**
   * Specifies the {@link OnProgressUpdateListener} implementation instance (usually a lambda) to be
   * used for progress update processing on the UI thread.
   *
   * @param listener {@link OnProgressUpdateListener} callback object or lambda.
   * @return this instance (for fluent-style method chaining).
   */
  public FluentAsyncTask<Params, Progress, Result> setOnProgressUpdateListener(
      OnProgressUpdateListener<Progress> listener) {
    progressListener = listener;
    return this;
  }

  /**
   * Specifies the {@link OnResultListener} implementation instance (usually a lambda) to be used
   * for additional background processing following successful completion of {@link
   * #perform(Object[])}.
   *
   * @param listener {@link OnResultListener} callback object or lambda.
   * @return this instance (for fluent-style method chaining).
   */
  public FluentAsyncTask<Params, Progress, Result> setOnCompletionListener(
      OnResultListener<Result> listener) {
    completionListener = listener;
    return this;
  }

  /**
   * Specifies the {@link OnResultListener} implementation instance (usually a lambda) to be used
   * for additional UI thread processing following successful completion of background processing.
   *
   * @param listener {@link OnResultListener} callback object or lambda.
   * @return this instance (for fluent-style method chaining).
   */
  public FluentAsyncTask<Params, Progress, Result> setOnSuccessListener(
      OnResultListener<Result> listener) {
    successListener = listener;
    return this;
  }

  /**
   * Specifies the {@link OnResultListener} implementation instance (usually a lambda) to be used
   * for additional UI thread processing following unsuccessful completion of background
   * processing.
   *
   * @param listener {@link OnResultListener} callback object or lambda.
   * @return this instance (for fluent-style method chaining).
   */
  public FluentAsyncTask<Params, Progress, Result> setOnFailureListener(
      OnResultListener<Result> listener) {
    failureListener = listener;
    return this;
  }

  /**
   * Returns the exception thrown (if any) by the {@link #perform(Object[])} method, resulting in
   * automatic marking the task as failed.
   *
   * @return instance of {@link RuntimeException} thrown by {@link #perform(Object[])}, or null if
   * no exception was thrown.
   */
  public RuntimeException getException() {
    return exception;
  }

  /**
   * Declares a {@link Performer#perform(Object[])} method that can be implemented to perform basic
   * background processing in an instance of {@link FluentAsyncTask}.
   *
   * @param <Params> type of input parameters used by the asynchronous task (consumed by {@link
   * #perform(Object[])}), or {@link Void} if no inputs will be used.
   * @param <Result> type of result data produced by the basic background processing of the
   * asynchronous task (returned by {@link #perform(Object[])}, or (usually) {@link Void} if the
   * background processing is not intended to return a result.
   */
  public interface Performer<Params, Result> {

    /**
     * Performs basic background processing in a {@link FluentAsyncTask} instance.
     *
     * @param params input arguments to background processing.
     * @return background processing results.
     */
    @SuppressWarnings("unchecked")
    Result perform(Params... params);

  }

  /**
   * Declares an {@link #update(Object[])} method that can be implemented to provide UI updates (for
   * example) while a background task is ongoing.
   *
   * @param <Progress> type of individual values provided by the asynchronous task for progress
   * updates (consumed by {@link #onProgressUpdate(Object[])}), or (usually) {@link Void} if no
   * progress updates will be made.
   */
  public interface OnProgressUpdateListener<Progress> {

    /**
     * Performs progress updates on the UI, or other progress updates that originate on the UI
     * thread, and are thus asynchronous with background processing.
     *
     * @param values update data.
     */
    @SuppressWarnings("unchecked")
    void update(Progress... values);

  }

  /**
   * Declares a {@link #handle(Object)} method that can be implemented to perform additional
   * processing following completion of processing by {@link #perform(Object[])}.
   *
   * @param <Result> type of result returned by {@link Performer#perform(Object[])}).
   */
  public interface OnResultListener<Result> {

    /**
     * Performs additional background processing (if set using {@link #setOnCompletionListener(OnResultListener)})
     * or UI updates (if set using {@link #setOnSuccessListener(OnResultListener)} or {@link
     * #setOnFailureListener(OnResultListener)}), following completion of {@link
     * #perform(Object[])}.
     *
     * @param result output of backround processing returned from {@link
     * FluentAsyncTask#doInBackground(Object[])}.
     */
    void handle(Result result);

  }

}
