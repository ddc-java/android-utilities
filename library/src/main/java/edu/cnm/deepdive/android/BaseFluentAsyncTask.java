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
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Declares methods, nested interfaces, and a nested exception class, intended to encourage a more
 * fluent, functional, and service-oriented style of extending and using {@link AsyncTask}.
 *
 * <p>One aim of this approach is to encourage implementation of {@link
 * AsyncTask} subclasses that are loosely coupled with consumer logic, and easily reused. For
 * example, if multiple activities or fragments need to perform a similar database operation, but
 * with slight differences in UI updates and other post-processing logic, the general operation can
 * be implemented in a single subclass of {@link BaseFluentAsyncTask}, with variations specified via
 * lambdas in fluent invocations of {@link #setTransformer(Transformer)}, {@link
 * #setProgressListener(ProgressListener)}, and (possibly) {@link #setFailureListener(ResultListener)}.</p>
 *
 * <p>For a simple {@link AsyncTask} implementation, with a single point of
 * consumption, an instance of this class can be created and used, specifying all key lifecycle
 * logic via individual lambdas or anonymous classes.</p>
 *
 * <p>Note that this class overrides {@link AsyncTask#doInBackground(Object[])},
 * splitting background processing into 2 phases: performance and transformation (the latter need
 * not transform the values returned from the former; instead, it might simply be additional
 * background processing that differs between the different consumers). The first phase produces an
 * intermediate value; the second takes the intermediate value, performs some additional processing,
 * and produces a result. Simple extensions or uses of {@link BaseFluentAsyncTask} will generally
 * only need to implement the performance phase&mdash;either by overriding {@link #perform(Object[])
 * perform(Params...)}, or by specifying a lambda or instance of an anonymous class in
 * an invocation of {@link #setPerformer(Performer)}.</p>
 *
 * <p><strong>Important notes:</strong></p>
 * <ul>
 * <li><p>
 * This class overrides and makes <code>final</code> the {@link #doInBackground(Object[])},
 * {@link #onProgressUpdate(Object[])}, {@link #onPostExecute(Object)}, and {@link
 * #onCancelled(Object)} methods. If an app needs to override these methods, then it should create a
 * subclass of {@link AsyncTask}, rather than a subclass of {@link BaseFluentAsyncTask}.
 * </p></li>
 * <li><p>
 * If the <code>Intermediate</code> and <code>Result</code> types
 * specified for an instance of this class (or a subclass) are different, then {@link
 * #setTransformer(Transformer)} <strong>must</strong> be invoked to configure a mapping object
 * between values of those 2 types; otherwise, a {@link ClassCastException} is likely to be thrown.
 * </p></li>
 * <li><p>
 * Passing a <code>null</code> argument to {@link #setPerformer(Performer)}, {@link
 * #setTransformer(Transformer)}, {@link #setProgressListener(ProgressListener)}, {@link
 * #setSuccessListener(ResultListener)}, or {@link #setSuccessListener(ResultListener)} will
 * result in {@link NullPointerException} being thrown at the corresponding moment in the execution
 * lifecycle of a <code>BaseFluentAsyncTask</code> instance. However, if any of those methods is
 * simply not invoked, then a default instance of the relevant listener is used. Thus, if a consumer
 * wishes the task to do nothing for one or more of the performance, transformation, progress,
 * success, or failure phases, there is no need to clear or replace the corresponding listener.
 * </p></li>
 * </ul>
 *
 * @param <Params> type of input parameters used by the asynchronous task (consumed by {@link
 * #perform(Object[])}), or {@link Void} if no inputs will be used.
 * @param <Progress> type of individual values provided by the asynchronous task for progress
 * updates (consumed by {@link #onProgressUpdate(Object[])}), or (usually) {@link Void} if no
 * progress updates will be made.
 * @param <Intermediate> type of result data produced by the basic background processing of the
 * asynchronous task (returned by {@link #perform(Object[])} and consumed by {@link
 * Transformer#transform(Object)}, or (usually) {@link Void} if the background processing is not
 * intended to return a result.
 * @param <Result> type of result data produced by the transform processing of the asynchronous task
 * (returned by {@link Transformer#transform(Object)} and consumed by {@link #onPostExecute(Object)}
 * and {@link #onCancelled(Object)}, or (usually) {@link Void} if the background processing is not
 * intended to return a result.
 *
 * @author Nicholas Bennett, Deep Dive Coding
 * @version 1.0.3
 */
public class BaseFluentAsyncTask<Params, Progress, Intermediate, Result>
    extends AsyncTask<Params, Progress, Result> {

  private Performer<Params, Intermediate> performer = new Performer<Params, Intermediate>() {
    @Override
    public Intermediate perform(Params... params) throws TaskException {
      return null;
    }
  };
  private ProgressListener<Progress> progressListener = new ProgressListener<Progress>() {
    @Override
    public void update(Progress... values) {
    }
  };
  private Transformer<Intermediate, Result> transformer = new Transformer<Intermediate, Result>() {
    @Override
    public Result transform(Intermediate intermediate) throws TaskException {
      return (Result) intermediate;
    }
  };
  private ResultListener<Result> successListener = new ResultListener<Result>() {
    @Override
    public void handle(Result result) {
    }
  };
  private ResultListener<Result> failureListener = new ResultListener<Result>() {
    @Override
    public void handle(Result result) {
    }
  };

  /**
   * Executes basic processing of asynchronous task. This method will always be invoked on a
   * background thread; no UI modifications should be attempted from this method, but should instead
   * be performed in the lambdas/interface implementations specified to {@link
   * #setProgressListener(ProgressListener)}, {@link #setSuccessListener(ResultListener)}, and
   * {@link #setFailureListener(ResultListener)}.
   *
   * @param params input arguments to background processing.
   * @return intermediate results.
   * @throws TaskException to indicate that {@link #cancel(boolean)} should be invoked, resulting in
   * the eventual invocation of {@link #onCancelled(Object)}, and consequently, the lambda or {@link
   * ResultListener} implementation specified to {@link #setFailureListener(ResultListener)}.
   */
  @Nullable
  protected Intermediate perform(Params... params) throws TaskException {
    return performer.perform(params);
  }

  /**
   * Implements the <code>abstract</code> {@link AsyncTask#doInBackground(Object[])} to divide
   * background processing into 2 components: performance and transformation. Any value (including
   * <code>null</code>) returned by {@link #perform(Object[])} will be passed in the invocation of
   * {@link Transformer#transform(Object)}.
   * <p> If the code of the performance phase throws a {@link TaskException} (or any other {@link
   * Exception}), the transformation phase will not be executed. If such an exception is thrown in
   * either the performance phase or the transformation phase, the listener set (if any) via {@link
   * #setFailureListener(ResultListener)} will be used, instead of any listener set via {@link
   * #setSuccessListener(ResultListener)}.</p>
   * <p>Subclasses of {@link BaseFluentAsyncTask} cannot override this method (since it is declared
   * <code>final</code>), but can instead override {@link #perform(Object[])}, or provide a lambda
   * or {@link Performer} implementation in an invocation of {@link #setPerformer(Performer)}.</p>
   *
   * @param params input arguments to background processing.
   * @return background processing results.
   */
  @Nullable
  @Override
  protected final Result doInBackground(Params... params) {
    try {
      return transformer.transform(perform(params));
    } catch (TaskException e) {
      cancel(true);
    } catch (Exception e) {
      Log.e(getClass().getSimpleName(), e.getMessage(), e);
      cancel(true);
    }
    return null;
  }

  /**
   * Invokes {@link ProgressListener#update(Object[])}, presumably to update the UI to display the
   * progress/status of the background processing. Rather than overriding this method
   * (which is not possible, since the method is <code>final</code>), a listener should be attached
   * by invoking {@link #setProgressListener(ProgressListener)} on an instance of this class or a
   * subclass.
   *
   * @param values progress
   */
  @Override
  protected final void onProgressUpdate(Progress... values) {
    super.onProgressUpdate(values);
    progressListener.update(values);
  }

  /**
   * Invokes {@link ResultListener#handle(Object)} to perform any UI thread-based post-processing
   * after successful completion of background processing. Rather than overriding this method
   * (which is not possible, since the method is <code>final</code>), a listener should be attached
   * by invoking {@link #setSuccessListener(ResultListener)} on an instance of this class or a
   * subclass.
   *
   * @param result background processing results.
   */
  @Override
  protected final void onPostExecute(@Nullable Result result) {
    super.onPostExecute(result);
    successListener.handle(result);
  }

  /**
   * Invokes {@link ResultListener#handle(Object)} to perform any UI thread-based post-processing
   * after unsuccessful completion of background processing. Rather than overriding this method
   * (which is not possible, since the method is <code>final</code>), a listener should be attached
   * by invoking {@link #setFailureListener(ResultListener)} on an instance of this class or a
   * subclass.
   *
   * @param result background processing results.
   */
  @Override
  protected final void onCancelled(@Nullable Result result) {
    super.onCancelled(result);
    failureListener.handle(result);
  }

  /**
   * Specifies the {@link Performer} implementation instance (usually a lambda) to be used for basic
   * background processing. Consumer code may invoke this method directly on an instance of this
   * class, or override {@link #perform(Object[])} in a subclass. In either case, {@link
   * TaskException} may be thrown to signal unsuccessful completion (aka failure) of the background
   * processing.
   *
   * @param performer {@link Performer} callback object or lambda.
   * @return this instance (for fluent-style method chaining).
   */
  public BaseFluentAsyncTask<Params, Progress, Intermediate, Result> setPerformer(
      Performer<Params, Intermediate> performer) {
    this.performer = performer;
    return this;
  }

  /**
   * Specifies the {@link ProgressListener} implementation instance (usually a lambda) to be used
   * for progress updating on the UI thread.
   *
   * @param listener {@link ProgressListener} callback object or lambda.
   * @return this instance (for fluent-style method chaining).
   */
  public BaseFluentAsyncTask<Params, Progress, Intermediate, Result> setProgressListener(
      ProgressListener<Progress> listener) {
    this.progressListener = listener;
    return this;
  }

  /**
   * Specifies the {@link Transformer} implementation instance (usually a lambda) to be used for
   * additional background processing.
   *
   * @param transformer {@link Transformer} callback object or lambda.
   * @return this instance (for fluent-style method chaining).
   */
  public BaseFluentAsyncTask<Params, Progress, Intermediate, Result> setTransformer(
      Transformer<Intermediate, Result> transformer) {
    this.transformer = transformer;
    return this;
  }

  /**
   * Specifies the {@link ResultListener} implementation instance (usually a lambda) to be used for
   * additional UI thread processing following successful completion of backgrounnd processing.
   *
   * @param listener {@link ResultListener} callback object or lambda.
   * @return this instance (for fluent-style method chaining).
   */
  public BaseFluentAsyncTask<Params, Progress, Intermediate, Result> setSuccessListener(
      ResultListener<Result> listener) {
    this.successListener = listener;
    return this;
  }

  /**
   * Specifies the {@link ResultListener} implementation instance (usually a lambda) to be used for
   * additional UI thread processing following unsuccessful completion of background processing.
   *
   * @param listener {@link ResultListener} callback object or lambda.
   * @return this instance (for fluent-style method chaining).
   */
  public BaseFluentAsyncTask<Params, Progress, Intermediate, Result> setFailureListener(
      ResultListener<Result> listener) {
    this.failureListener = listener;
    return this;
  }

  /**
   * Declares a {@link Performer#perform(Object[])} method that can be implemented to perform basic
   * background processing in an instance of {@link BaseFluentAsyncTask}.
   *
   * @param <Params> type of input parameters used by the asynchronous task (consumed by {@link
   * #perform(Object[])}), or {@link Void} if no inputs will be used.
   * @param <Intermediate> type of result data produced by the basic background processing of the
   * asynchronous task (returned by {@link #perform(Object[])} and consumed by {@link
   * Transformer#transform(Object)}, or (usually) {@link Void} if the background processing is not
   * intended to return a result.
   */
  public interface Performer<Params, Intermediate> {

    /**
     * Performs basic background processing in a {@link BaseFluentAsyncTask} instance.
     *
     * @param params input arguments to background processing.
     * @return background processing results.
     * @throws TaskException to indicate unsuccessful completion.
     */
    Intermediate perform(Params... params) throws TaskException;

  }

  /**
   * Declares an {@link #update(Object[])} method that can be implemented to provide UI updates (for
   * example) while a background task is ongoing.
   *
   * @param <Progress> type of individual values provided by the asynchronous task for progress
   * updates (consumed by {@link #onProgressUpdate(Object[])}), or (usually) {@link Void} if no
   * progress updates will be made.
   */
  public interface ProgressListener<Progress> {

    /**
     * Performs progress updates on the UI, or other progress updates that originate on the UI
     * thread, and are thus asynchronous with background processing.
     *
     * @param values update data.
     */
    void update(Progress... values);

  }

  /**
   * Declares a {@link #transform(Object)} method that can be implemented to perform additional
   * background processing following successful completion of the {@link
   * BaseFluentAsyncTask#perform(Object[])} method.
   *
   * @param <Intermediate> type of result data produced by the basic background processing of the
   * asynchronous task (returned by {@link #perform(Object[])} and consumed by {@link
   * Transformer#transform(Object)}, or (usually) {@link Void} if the background processing is not
   * intended to return a result.
   * @param <Result> type of result produced by the transform processing of the asynchronous task
   * (returned by {@link Transformer#transform(Object)} and consumed by {@link
   * #onPostExecute(Object)} and {@link #onCancelled(Object)}, or (usually) {@link Void} if the
   * background processing is not intended to return a result.
   */
  public interface Transformer<Intermediate, Result> {

    /**
     * Transforms the intermediate data to a final result. (These may be of different types,
     * depending on the instance declaration or subclass definition.)
     *
     * @param intermediate intermediate results, returned by {@link BaseFluentAsyncTask#perform(Object[])}.
     * @return final results, passed to {@link BaseFluentAsyncTask#onPostExecute(Object)} or {@link
     * BaseFluentAsyncTask#onCancelled(Object)}.
     * @throws TaskException to indicate unsuccessful completion.
     */
    Result transform(Intermediate intermediate) throws TaskException;

  }

  /**
   * Declares a {@link #handle(Object)} method that can be implemented to perform additional
   * processing on the UI thread following completion of background processing.
   *
   * @param <Result> type of result produced by background processing (returned by {@link
   * Transformer#transform(Object)}).
   */
  public interface ResultListener<Result> {

    /**
     * Performs UI display updates, or other updates originating on the UI thread, following
     * completion of UI processing.
     *
     * @param result output of backround processing returned from {@link
     * BaseFluentAsyncTask#doInBackground(Object[])}.
     */
    void handle(Result result);

  }

  /**
   * Thrown by background task methods ({@link BaseFluentAsyncTask#perform(Object[])}, {@link
   * Performer#perform(Object[])}, and {@link Transformer#transform(Object)}) to indicate
   * unsuccessful (but not necessarily problematic) completion of background processing.
   */
  public static class TaskException extends RuntimeException {

    /**
     * Initializes exception with no cause or message specified.
     */
    public TaskException() {
      super();
    }

    /**
     * Initializes exception with the specified message.
     *
     * @param message description of this exception.
     */
    public TaskException(String message) {
      super(message);
    }

    /**
     * Initializes exception with the specified message and cause.
     *
     * @param message description of this exception.
     * @param cause exception that caused <code>this</code> exception to be raised.
     */
    public TaskException(String message, Throwable cause) {
      super(message, cause);
    }

    /**
     * Initializes exception with the specified cause.
     *
     * @param cause exception that caused <code>this</code> exception to be raised.
     */
    public TaskException(Throwable cause) {
      super(cause);
    }

  }

}
