Clojure API for Arduino.

To install, merely add the following to your 'project.clj'.

```clojure
[clodiuno "0.0.4-SNAPSHOT"]

``` 

For examples, check out the [project
homepage](http://nakkaya.com/clodiuno.html).

# Usage

## Create board

```clojure
(ns clj-arduino
  (:require [clodiuno.core    :refer :all])
  (:require [clodiuno.firmata :refer :all]))

(def board (arduino :firmata "/path/to/port"))
; or
(def board (arduino :firmata "/path/to/port" :baudrate 9600))
; default baudrate is 57600
```


# Issues

If you got NoSuchPortException then you need add your port to env var containing ports.
```clojure
(System/setProperty "gnu.io.rxtx.SerialPorts" "/path/to/your/port")
```
