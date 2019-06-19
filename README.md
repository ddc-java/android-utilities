This repository contains utility classes (currently 2 of them) that simplify the use of some important Android features in application code.

## `FluentAsyncTask`

The standard `AsyncTask` class is a powerful tool for managing coordination between UI updates and short-lived background processing tasks (e.g. database updates, web service requests). However, the amount of boilerplate required is rather excessive&mdash;particularly if we're already using Java 8 features (esp. lambdas) in our code. The aim of `edu.cnm.deepdive.android.FluentAsyncTask` is to simplify access to the capabilities of `AsyncTask`, especially for 3 use cases:

* The UI updates to be performed on successful completion of the background processing differ between multiple points of invocation of the same background processing. An example of this would be a common database query or update that is performed from different fragments or activities, with different UI updates required in each case.

* Additional background thread operations need to be "chained" at the end of primary background processing&mdash;especially when such additional operations may differ between invocation points.

* The combination of required background &amp; foreground operations would make the use of a simple `Thread` instance insufficient, but concise enough that a named or anonymous subclass of `AsyncTask` would be excessively verbose.

`FluentAsyncTask` changes the specification of post-background-processing logic from an inheritance-with-override style to a fluent configuration style. In many cases, this allows us to specify even moderately complex combinations of background and UI thread processing in a few lines of code, without the need to declare a subclass (named or anonymous) of `FluentAsyncTask`. One example of this, taken from the [Strategies &amp; Aphorisms project](https://github.com/deep-dive-coding-java-cohort-6/strategies-aphorisms), includes a background database query, along with a UI update on successful completion. (Note that this uses the `BaseFluentAsyncTask`, which was the v1.x name of the class that is now called `FluentAsyncTask`.)

```java
private void changeAnswer() {
  new BaseFluentAsyncTask<Void, Void, SayingWithSource, SayingWithSource>()
      .setPerformer((ignore) -> 
          StratAphorDatabase.getInstance().getSayingDao().findFirstRandom())
      .setSuccessListener((sayingWithSource) -> {
        sayingText.setText(sayingWithSource.getSaying());
        sourceName.setText(sayingWithSource.getSource());
        fadeTogether(sayingText, sourceName);
      })
      .execute();
}
```

`FluentAsyncTask` also divides the background processing portion of its functionality into 2 phases: primary performance and additional processing. This provides additional flexibility, allowing for the chaining of background operations.

For more information, see the [Javadoc](docs/api/).

## License

Copyright 2019 Nicholas Bennett & Deep Dive Coding

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
