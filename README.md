# Deep Dive Coding Android Utility Classes, v1.0.3

This repository contains utility classes (currently 2 of them) that simplify the use of some important Android features in application code.

## `BaseFluentAsyncTask`

The standard `AsyncTask` class is a powerful tool for managing coordination between UI updates and short-lived background processing tasks (e.g. database updates, web service requests). However, the amount of boilerplate required is rather excessive&mdash;particularly if we're already using Java 8 features (esp. lambdas) in our code. The aim of `edu.cnm.deepdive.android.BaseFluentAsyncTask` is to simplify access to the capabilities of `AsyncTask`, especially for 3 use cases:

* The UI updates to be performed on successful completion of the background processing differ between multiple points of invocation of the same background processing. An example of this would be a common database query or update that is performed from different fragments or activities, with different UI updates required in each case.

* Additional background thread operations need to be "chained" at the end of primary background processing&mdash;especially when such additional operations may differ between invocation points.

* The combination of required background &amp; foreground operations would make the use of a simple `Thread` instance insufficient, but concise enough that a named or anonymous subclass of `AsyncTask` would be excessively verbose.

`BaseFluentAsyncTask` changes the specification of post-background-processing logic from an inheritance-with-override style to a fluent configuration style. In many cases, this allows us to specify even moderately complex combinations of background and UI thread processing in a few lines of code, without the need to declare a subclass (named or anonymous) of `BaseFluentAsyncTask`. One example of this, taken from the [Strategies &amp; Aphorisms project](https://github.com/deep-dive-coding-java-cohort-6/strategies-aphorisms), includes a background database query, along with a UI update on successful completion.

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

`BaseFluentAsyncTask` also divides the background processing portion of its functionality into 2 phases: performance and transformation. This provides additional flexibility, allowing for the chaining of background operations; however, the added flexibility comes at the cost of some additional complexity in specification: whereas `AsyncTask` is a generic class with 3 types (`Param`, `Update`, and `Result`), `BaseFluentAsyncTask` has 4: `Param`, `Update`, `Intermediate`, and `Result`. `Intermediate` is the type of result returned by the performance phase of the background processing, while `Result` is the final type returned by the transformation phase. In many cases, these 2 types will be the same; the default transformer is an _identity transformer_, simply returning the value it receives as input.