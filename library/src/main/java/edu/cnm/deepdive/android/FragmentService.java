/*
 *  Copyright 2019 Nicholas Bennett & Deep Dive Coding
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.cnm.deepdive.android;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

/**
 * Provides app-wide fragment management services, intended primarily for use in apps without
 * extensive back stack requirements, where some navigation view (e.g. a navigation drawer, or a
 * bottom navigation view), available app-wide, is used to move between the primary UI fragments,
 * and where those fragments should not be re-instantiated every time the user navigates between
 * them.
 * <p>The singleton pattern is implemented by this class, exposing its capabilities via a single
 * instance returned by the {@link #getInstance()} method.</p>
 *
 * @author Nicholas Bennett, Deep Dive Coding
 * @version 2.0.0
 */
public class FragmentService {

  private FragmentService() {
  }

  /**
   * Returns the single instance of {@link FragmentService}.
   *
   * @return instance.
   */
  public static FragmentService getInstance() {
    return InstanceHolder.INSTANCE;
  }

  /**
   * Attaches the specified fragment to the specified activity and container, with a specified tag,
   * in an initially visible state, and returns the not-yet-committed transaction.
   *
   * @param activity host of fragment.
   * @param containerId id of {@link android.view.ViewGroup} to which the fragment will be
   * attached.
   * @param fragment fragment to be loaded.
   * @param tag <code>String</code> identifier of fragment.
   * @return uncommitted transaction.
   */
  public FragmentTransaction getLoadFragmentTransaction(
      FragmentActivity activity, int containerId, Fragment fragment, String tag) {
    return activity.getSupportFragmentManager().beginTransaction()
        .add(containerId, fragment, tag);
  }

  /**
   * Attaches the specified fragment to the specified activity and container, with a specified tag,
   * in an initially visible or hidden state. Note that while {@link #showFragment(FragmentActivity,
   * int, Fragment)} ensures that only one fragment attached to a single container (at most) is
   * visible at a time, this method performs no such checks.
   *
   * @param activity host of fragment.
   * @param containerId id of {@link android.view.ViewGroup} to which the fragment will be
   * attached.
   * @param fragment fragment to be loaded.
   * @param tag <code>String</code> identifier of fragment.
   * @param visible initial visible state of activity.
   */
  public void loadFragment(
      FragmentActivity activity, int containerId, Fragment fragment, String tag, boolean visible) {
    FragmentTransaction transaction =
        getLoadFragmentTransaction(activity, containerId, fragment, tag);
    if (!visible) {
      transaction.hide(fragment);
    }
    transaction.commit();
  }

  /**
   * Begins, but does not commit, a transaction to make the specified fragment visible, if it is
   * attached to the specified activity and container. Any other fragments attached to the specified
   * activity and container are set to be hidden in the transaction.
   *
   * @param activity host activity.
   * @param containerId host container.
   * @param fragment fragment to show.
   * @return uncommitted transaction.
   */
  public FragmentTransaction getShowFragmentTransaction(
      FragmentActivity activity, int containerId, Fragment fragment) {
    FragmentManager manager = activity.getSupportFragmentManager();
    FragmentTransaction transaction = manager.beginTransaction();
    for (Fragment frag : manager.getFragments()) {
      if (frag.getId() == containerId) {
        if (frag == fragment && !frag.isVisible()) {
          transaction.show(frag);
        } else if (frag != fragment && frag.isVisible()) {
          transaction.hide(frag);
        }
      }
    }
    return transaction;
  }

  /**
   * Begins, but does not commit, a transaction to make the specified fragment visible, if it is
   * attached to the specified activity and container. Any other fragments attached to the specified
   * activity and container are set to be hidden in the transaction.
   *
   * @param activity host activity.
   * @param containerId host container.
   * @param tag <code>String</code> identifier of fragment to show.
   * @return uncommitted transaction.
   */
  public FragmentTransaction getShowFragmentTransaction(
      FragmentActivity activity, int containerId, String tag) {
    return getShowFragmentTransaction(activity, containerId,
        findFragment(activity, containerId, tag));
  }

  /**
   * Makes the specified fragment visible, if it is attached to the specified activity and
   * container. Any other fragments attached to the specified activity and container are hidden. In
   * any event, no visibility changes will be made to fragments not attached to both the specified
   * host activity and container.
   *
   * @param activity host activity.
   * @param containerId host container.
   * @param fragment fragment to show.
   */
  public void showFragment(FragmentActivity activity, int containerId, Fragment fragment) {
    getShowFragmentTransaction(activity, containerId, fragment).commit();
  }

  /**
   * Makes the specified fragment visible, if it is attached to the specified activity and
   * container. Any other fragments attached to the specified activity and container are hidden. In
   * any event, no visibility changes will be made to fragments not attached to both the specified
   * host activity and container.
   *
   * @param activity host activity.
   * @param containerId host container.
   * @param tag <code>String</code> identifier of fragment to show.
   */
  public void showFragment(FragmentActivity activity, int containerId, String tag) {
    getShowFragmentTransaction(activity, containerId, tag).commit();
  }

  /**
   * Returns a reference to a fragment with the specified tag, if it is attached to the specified
   * activity and container. If a fragment meeting those criteria is not found, <code>null</code> is
   * returned.
   *
   * @param activity host activity.
   * @param containerId host container.
   * @param tag <code>String</code> identifier of fragment.
   * @return fragment (null if not found).
   */
  public Fragment findFragment(FragmentActivity activity, int containerId, String tag) {
    FragmentManager manager = activity.getSupportFragmentManager();
    Fragment fragment = manager.findFragmentByTag(tag);
    return (fragment != null && fragment.getId() == containerId) ? fragment : null;
  }

  private static class InstanceHolder {

    private static final FragmentService INSTANCE = new FragmentService();

  }

}
