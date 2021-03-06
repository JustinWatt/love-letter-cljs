[![Circle CI](https://circleci.com/gh/JustinWatt/love-letter-cljs/tree/dev.svg?style=svg)](https://circleci.com/gh/JustinWatt/love-letter-cljs/tree/dev)
# love-letter-cljs

Implementation of the game Love Letter in clojurescript using re-frame.

## Development Mode

### Run application:

```
lein clean
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then type
```
(start)
(cljs-repl)
```
to start the figwheel server and repl.

Finally, navigate to [http://localhost:3449](http://localhost:3449).

### Run tests:

```
lein clean
lein doo phantom test once
```

The above command assumes that you have [phantomjs](https://www.npmjs.com/package/phantomjs) installed. However, please note that [doo](https://github.com/bensu/doo) can be configured to run cljs.test in many other JS environments (chrome, ie, safari, opera, slimer, node, rhino, or nashorn).

## Production Build

```
lein clean
lein cljsbuild once min
```
