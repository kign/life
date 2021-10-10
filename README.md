# Convay's Game of Life

This repository is a collection of semi-independent modules related to the implementation of [Game of life]().

This is pretty much work in progress. Some modules are not yet fully (or at all) documented.

Everything is published under GPLv2.

## Demo

[Online demo](https://kign.github.io/life/life-web.html).

![Web UI Screenshot](https://github.com/kign/life/blob/master/etc/Life_Web_UI.png?raw=true "Web UI Screenshot" )

This is implemented directly in [Web Assembly](https://github.com/kign/life/blob/master/wasm/life.wat), so it should have performance comparable to plain C version, but
might not be fully compatible with older browsers (in particular, it requires support for [bulk memory operations](https://github.com/WebAssembly/bulk-memory-operations/blob/master/proposals/bulk-memory-operations/Overview.md)).

## Available modules

<table>
  <tr>
    <td><A href="https://github.com/kign/life/tree/master/lib">lib</A></td>
    <td>Well-optimized C implementation</td>
  </tr>
  <tr>
    <td><A href="https://github.com/kign/life/tree/master/jni">jni</A></td>
    <td>Java Native Interface (JNI) plugin for C library</td>
  </tr>
  <tr>
    <td><A href="https://github.com/kign/life/tree/master/lifefx">lifefx</A></td>
    <td>UI implementation in <A href="https://openjfx.io/">Java FX</A> using JNI plugin</td>
  </tr>
  <tr>
    <td><A href="https://github.com/kign/life/tree/master/pni">pni</A></td>
    <td>Python package with wrappers for C library; published under name <A href="https://pypi.org/project/conway-life/">conway-life</A></td>
  </tr>
  <tr>
    <td><A href="https://github.com/kign/life/tree/master/py-web">py-web</A></td>
    <td>Pure Python Web UI using <code>conway-life</code>; published under name <A href="https://pypi.org/project/life-web-ui/">life-web-ui</A></td>
  </tr>
  <tr>
    <td><A href="https://github.com/kign/life/tree/master/wasm">wasm</A></td>
    <td>Web Assembly implementation written directly in <A href="https://developer.mozilla.org/en-US/docs/WebAssembly/Understanding_the_text_format">Web Assembly Text</A> format (WAT). </td>
  </tr>
  <tr>
    <td><A href="https://github.com/kign/life/tree/master/python">python</A></td>
    <td>Python utilities based on <code>conway-life</code> for testing/research (in progress)</td>
  </tr>
  <tr>
    <td><A href="https://github.com/kign/life/tree/master/test">test</A></td>
    <td>Test suite in C (unfinished)</td>
  </tr>
</table>
