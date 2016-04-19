The project follows the concepts described in the article [My Clojure Workflow, Reloaded](http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded) by Sierra Stuart.

For development start with:

    lein repl
    (dev)

Inside the repl you can use the folowing commands:

-   **(go):** to start the application
-   **(stop):** to stop the application
-   **(reset):** reload the sources
-   **(test-all):** run all tests
-   **(ns namespace):** set the actual namespace
-   **(run-tests):** run all the tests of the actual ns
-   **(test-vars [#'function-test]):** run only the the function-test

To see the test front end application connect in a browser to <http://localhost:9500/>

For development I don't use the terminal but the spacemacs editor  with my custom layer clj (see <https://github.com/dpom/dotfiles/tree/master/.spacemacs.d/layers/clj>)
