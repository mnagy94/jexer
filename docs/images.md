Next-Gen Multimedia Standard - Proposed Design Document
=======================================================


Purpose
-------

TODO: Crib text from the first message of
https://gitlab.freedesktop.org/terminal-wg/specifications/issues/12 as
to why people want images in their terminals.

The same mechanism that can put raster-based images on the screen is
easily generalizable to other media types such as vector-based images,
animations, and embedded GUI widgets.  This document is thus a
"multimedia" proposal, not just "simple images".


Acknowledgements
----------------

This proposal has been informed from the following prior work:

* DEC VT300 series sixel graphics standard:
  https://vt100.net/docs/vt3xx-gp/chapter14.html

* iTerm2 image protocol:
  https://iterm2.com/documentation-images.html

* Kitty image protocol:


* Jexer Terminal User Interface:
  https://gitlab.com/klamonte/jexer


Design Goals - Core
-------------------

The core ("must-have") design goals are:

* Be easy to implement in existing terminals and applications:

  - Sacrifice "10%" of potential function to eliminate "90%" of
    implementation pain.  "Less is more."

  - Be a strict superset of the existing iTerm2 and DEC sixel image
    solutions.  One should be able to take an existing terminal or
    application that emits/consumes iTerm2 or sixel sequences, and
    only change the control sequence introducer/termination to achieve
    the same effect as a terminal/application that conforms with this
    standard.

* Have no ambiguity.  If two terminal or application developers can
  read this document and reach different conclusions on what should be
  on the screen, then an error exists in this document that must be
  fixed.

  - Every feature should be straightforward to validate via automated
    unit testing.

  - Every conformant terminal should produce the same output (pixels
    on screen) given the same input (terminal font, terminal
    sequences).

  - Every option must have a defined default value.

  - Erroneous sequences must have defined expected results.

  - Every operation must act atomically: either everything worked
    (image is on screen, cursor has moved, etc.) or nothing did.

* Be straightforward to implement in non-"physical" terminals,
  including:

  - Future versions of terminal control libraries such as ncurses and
    termbox.

  - Terminal multiplexers that support "headless" terminals (no
    physical screen) and "multi-head" terminals (many different
    physical screens).

* Be platform-agnostic, and easy to implement on (at the least):
  POSIX, Windows, and web.

  - All features must be available even if the only means of
    communication between the application and terminal is control
    sequences (e.g. no shared disk, no shared memory, no shared DOM,
    etc.).

* Support graceful fallback:

  - Terminal emulators and physical terminals that do not support this
    standard should remain usable with no undefined screen artifacts,
    even when the application blindly emits these sequences to those
    terminals.

  - This standard must able to be versioned for future enhancements.

  - An application must be able to detect that its terminal supports
    this standard, and at what version.

* Support secure programming practices:

  - Applications must not be able to obtain unauthorized data from
    terminal memory, such as: images emitted by other applications
    still present in the terminal's scrollback buffer, terminal or
    system memory limits.

  - Applications must not be able to compromise the terminal through
    denial-of-service such as: excessive memory usage, unterminated
    control sequences.  Similarly, terminals must not be able to
    compromise application through their responses to application
    queries.

  - Applications must not be able to manipulate the terminal into
    performing an insecure operation such as: reading arbitrary shared
    memory regions, reading arbitrary files on disk, deleting
    arbitrary files on disk, etc.  Similarly, terminals must not be
    able to manipulate applications into performing insecure
    operations.

  - This standard must be implementable when the terminal has a fixed
    maximum memory, such as a kernel-level device driver.



Design Goals - Secondary
------------------------

The secondary ("nice-to-have") design goals are:

* Minimal redundant network traffic for on-screen data that is
  repeated: either on screen in multiple places, or in the same place
  but refreshed multiple times.

* Asynchronous notification from terminal to application that the
  screen has been changed by outside or user action.  Examples: font
  change, session detach/attach, user changed image preferences.


Out Of Scope
------------

The following items are out of scope for this standard:

* Bidirectional output.  Applications are expected to generate Tiles
  and place them on screen where they need.  The cursor response to
  image sequences are defined as left-to-right, consistent with
  ECMA-48 / ANSI X3.64 sequences.  An independent BIDI standard is
  free to apply whatever solution will work for ECMA-48 sequences to
  the sequences described in this document.

* Capabilities.  This standard defines a limited number of terminal
  reports.  These are not intended to be used as a general-purpose
  capabilities model.



Definitions
-----------

Terminal - The hardware, or a program that simulates hardware,
           comprising a keyboard, screen, and mouse.

Application - A program that utilizes the terminal for its
              input/output with the user.

Multiplexer - A special case of an application that simulates one or
              more "inner" terminals for other applications to use,
              and composes these inner terminals into a combined
              screen to emit to one or more "outer" terminals that
              obtain input/output from the user.  Multiplexers are
              thus both applications and terminals.

X - The column coordinate of a cell.  This standard is 0-based: the
    left-most column of the screen is numbered 0.

Y - The row coordinate of a cell.  This standard is 0-based: the
    top-most row of the screen is numbered 0.

Z - The layer that text or multimedia is placed on.  This proposal
    uses a right-hand coordinate system with (X, Y, Z) = (0, 0, 0)
    defined as the top-left corner on the default layer: positive Z
    projects "away" from the user and "into" or "behind" the screen.
    Rendering the Cells on the screen must produce the same result as
    painter's algorithm (see Rendering section below).

Cell - A fixed-width-and-height rectangle on the screen.  The cells of
       the screen are arranged in a grid of X columns and Y rows.  A
       Cell has dimensions of cellWidth and cellHeight, which can be
       measured in either pixels or points.  Every Cell has a
       coordinate of (X, Y, Z).

Tile - One or more contiguous Cells with data to be displayed.  The
       data can be text or image data, but not both.  A Tile has width
       of 1, 2, or more, and a coordinate of (X, Y, Z) that is the
       same as its left-most (first) Cell's (X, Y, Z).  In practice,
       Tiles are typically one Cell wide for ASCII and Latin language
       glyphs, and two Cells wide for "fullwidth" glyphs as used in
       Asian langauges, emojis, and symbols.  This standard does not
       preclude Tiles from encompassing entire grapheme clusters.

Layer - A screen-sized grid of Cells that have the same Z coordinate.
        Layers are drawn to the screen in descending Z order.  Layers
        may have optional additional attributes such as transparency.


Rendering
---------

A terminal will display its Cells such that the screen will look as if
it was rendered in the following pseudo-code manner:

```
for each layer Z, in descending order from maxZ to minZ:
  for each row Y, in ascending order from minY to maxY:
    for each column X, in ascending order from minX to maxX:
      draw tile at (X, Y, Z)
      advance X by tile width
    next column
    advance Y by 1
  next row
  decrease Z by 1
next layer
```

A terminal is free to optimize its rendering as it sees fit, so long
as the final screen output looks equivalent to the above method.



Terminal State
--------------



Terminal Reports
----------------



Error Handling
--------------



Cursor Position
---------------




Wire Formats
------------




Optimizations
-------------



Examples
--------


