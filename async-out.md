# Viewing Async Output While Working On Node Projects with `shadow-cljs`

When working on nodejs projects with `shadow-cljs` and Calva, async output does not always appear in the Calva output window. To work around this problem, follow these steps:

1. Use the Calva command `Copy Jack-in Command to Clipboard.` Select the `shadow-cljs` option.
2. Paste the copied command into a terminal window (either a Calva terminal or an external window, preferably on a second monitor.)
3. Execute this command to start shadow-cljs in watch mode.
4. In another terminal window, run your node script. For example, if your shadow-cljs.edn builds clause looks like this:

    ```json
    :builds
    {:script
    {:target :node-script
    :main myorg.myapp/-main
    :output-to "out/script/myapp.js"}}
    ```

    &emsp;you would run `node out/script/myapp.js` in the terminal.

5. Back in Calva, jack-in by clicking REPL in the status bar and selecting `Connect to a running REPL in your project.` For `project type` select `shadow-cljs` and for `build` select `:script`, assuming your `shadow-cljs.edn` is as above, or else select whatever build name is appropriate. Do not choose `node-repl`.
6. Evaluating forms in Calva will now cause some output to appear in both the Calva output window and the terminal window in which your node script is running, but async output will appear only in the terminal window.

This workaround fixes issue #1468.
