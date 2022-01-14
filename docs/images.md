Terminal Emulator Multimedia Standard - Proposed Design
=======================================================

Version: 1

 

Purpose
-------

Multiple standards exist to incorporate image data in text-based
terminals and terminal emulators.  Few standards have wide adoption
despite frequent user requests for these features and hardware support
for several of the standards.

A group including developers of several widely-used terminal emulators
has been working on defining the needs and limitations for a standard
that can be implemented in current-gen terminal emulators.  The
discussion has been primarily captured here:
https://gitlab.freedesktop.org/terminal-wg/specifications/issues/12

This document collects many of the reported desires and practical
constraints of that discussion into a proposed standard that
encompasses three independent new features:

1. A method to transfer multimedia data for immediate display within
   the screen cell grid ("Direct Multimedia").

2. A method to transfer multimedia data to a terminal-managed cache,
   and later display that data within the screen cell grid ("Cached
   Multimedia").

3. A method to assign cell data to different layers with options for
   both layer and cell transparency ("Layers").

A terminal may implement any combination of these features
independently of each other.  If all features are supported, then all
of the design goals outlined in this document can be met.

The same mechanisms that can put raster-based images on the screen are
also readily generalizable to other media types such as vector-based
images and animations.  This document is thus a "multimedia" proposal
rather than a "simple images" proposal.



Acknowledgements
----------------

This proposal has been informed from the following prior work:

* DEC VT300 series sixel graphics standard:
  https://vt100.net/docs/vt3xx-gp/chapter14.html

* iTerm2 image protocol:
  https://iterm2.com/documentation-images.html

* Kitty image protocol:
  https://sw.kovidgoyal.net/kitty/graphics-protocol.html

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
  corrected.

  - Every feature must be straightforward to validate via automated
    unit testing.

  - Every conformant terminal must produce the same output (pixels on
    screen) given the same input (terminal font, terminal sequences).

  - Every option must have a defined default value.

  - Erroneous sequences must have defined expected results.

  - Every operation must act atomically: either everything worked
    (image is on screen, cursor has moved, terminal state has changed,
    etc.) or nothing did.

* Integrate with existing ECMA-48 / ANSI X3.64 defined sequences:

  - Operations on Tiles/Cells containing text will have the same
    effect when applied to Tiles/Cells containing image data.

  - Existing sequences are given new parameters to cover needed
    features rather than entirely new sequences introduced.

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

The secondary ("nice-to-have") design goals are listed below.  These
might not all be possible, but will kept in mind:

* Minimal redundant network traffic for on-screen data that is
  repeated: either on screen in multiple places, or in the same place
  but refreshed multiple times.

* Asynchronous notification from terminal to application that the
  screen has been changed by outside or user action.  Examples: font
  change, session detach/attach, user changed image preferences.

* The ability for a multiplexer to "pass-thru" the image drawing
  sequence to its "outer" terminal, with some support for limited
  clipping.



Out Of Scope
------------

The following items are out of scope:

* Bidirectional output.  Applications are expected to generate Tiles
  and place them on screen where they need.  The cursor response to
  image sequences are defined as left-to-right-top-to-bottom,
  consistent with ECMA-48 / ANSI X3.64 sequences.  An independent BIDI
  standard is free to apply whatever solution will work for ECMA-48 /
  ANSI X3.64 sequences to the sequences described in this document.

* Capabilities.  This standard defines a limited number of new
  terminal reports and responses.  These are not intended to be used
  as a general-purpose capabilities model.

* Terminal Cache Management.  This standard defines a means for
  applications and terminals to communicate around cached multimedia
  items, but terminals are free to implement whatever cache management
  strategies they deem fit.

* Reliable Transport.  This standard defines a two-way
  command/response protocol that may get out of order on unreliable
  channels such as 3-wire RS232.  Applictions that require reliable
  transport on unreliable links may choose to use one of the many
  successful standards available for this purpose.



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

X - The column coordinate of a cell.  This standard is 1-based (like
    ECMA-48): the left-most column of the screen is numbered 1.

Y - The row coordinate of a cell.  This standard is 1-based (like
    ECMA-48): the top-most row of the screen is numbered 1.

Z - The layer that text or multimedia is placed on.  This proposal
    uses a right-hand coordinate system with (X, Y, Z) = (1, 1, 1)
    defined as the top-left corner on the default layer; positive Z
    projects "away" from the user and "into" or "behind" the screen.
    Rendering the Cells on the screen must produce the same result as
    painter's algorithm (see "Layers - Rendering" section below).

Cell - A fixed-width-and-height rectangle on the screen.  The cells of
       the screen are arranged in a grid of X columns and Y rows.  A
       Cell has dimensions of cellWidth and cellHeight pixels.  Every
       Cell has a coordinate of (X, Y) (or (X, Y, Z) when the terminal
       supports the layers feature).

Tile - One or more contiguous Cells with data to be displayed.  The
       data can be text or image data, but not both.  A Tile has width
       of 1, 2, or more, and a coordinate of (X, Y, Z) that is the
       same as its left-most (first) Cell's (X, Y, Z).  In practice,
       Tiles are typically one Cell wide for ASCII and Latin language
       glyphs, and two Cells wide for "fullwidth" glyphs as used in
       Asian langauges, emojis, and symbols.  This standard does not
       preclude Tiles from encompassing entire grapheme clusters.
       Note that ECMA-48 / ANSI X3.64 operations are performed against
       Tiles, not Cells: if a 2-Cell-wide Tile is deleted via
       backspace, then the cursor will decrement on screen by two
       columns.

Layer - A screen-sized grid of Cells that have the same Z coordinate.
        Layers are drawn to the screen in descending Z order.  Layers
        may have optional additional attributes such as transparency.
        Layer support is an orthogonal (independent) option to
        multimedia support.  It is acceptable for terminals to support
        multimedia without layers and vice versa.



All Features - Detection
------------------------

Applications can detect support for these features using Primary
Device Attributes (DA) and DECID (ESC Z, or 0x9A).

Terminals that support this standard will repond with additional
parameter(s): "224" for direct multimedia, "225" for cached
multimedia, and "226" for layers.  A recap of the parameters xterm
supports is listed below, with these new feature responses included:

| VT220 (and higher) Response | Description                                |
|-----------------------------|--------------------------------------------|
| 1                           | 132-columns                                |
| 2                           | Printer                                    |
| 3                           | ReGIS graphics                             |
| 4                           | Sixel graphics                             |
| 6                           | Selective erase                            |
| 8                           | User-defined keys                          |
| 9                           | National Replacement Character sets        |
| 1 5                         | Technical characters                       |
| 1 6                         | Locator port                               |
| 1 7                         | Terminal state interrogation               |
| 1 8                         | User windows                               |
| 2 1                         | Horizontal scrolling                       |
| 2 2                         | ANSI color, e.g., VT525                    |
| 2 8                         | Rectangular editing                        |
| 2 9                         | ANSI text locator (i.e., DEC Locator mode) |
| 2 2 4                       | Direct Multimedia Version 1                |
| 2 2 5                       | Cached Multimedia Version 1                |
| 2 2 6                       | Layers                                     |



Direct Multimedia - Summary
---------------------------

Non-text data (multimedia) can be sent to the terminal for immediate
display in a rectangular (single-layer) region of text Cells.
Multimedia data is transmitted to the terminal using one of two wire
formats described later in this document.

Setting a Cell to multimedia is a destructive operation: the Cell's
original text is lost.  Multimedia pixels will not overlap rendered
text in the same Cell.  To achieve pixels overlaid on text, the layers
feature can be used.

Setting any part of a multi-Cell Tile to multimedia also "breaks up"
the Tile into a range of single Cells.  In other words, multimedia can
only be carried by a Cell, not a Tile.

The pixels of a multimedia Cell are assigned to the Cell's foreground;
multimedia Cells have no background.  If a terminal supports the
layers feature, setting a multimedia Cell's foreground transparency to
true/enabled causes that Cell to not be displayed at all; setting its
background transparency to either true/enabled or false/disabled has
no visible effect.

The pixels of multimedia Cells can come from two sources:

  1. The application can generate pixels and send them to the terminal
     for display at the current cursor position.

  2. The application can specify a source for the multimedia and the
     terminal will generate the pixels for display at the current
     cursor position.



Direct Multimedia - Required Support For Existing Sequences
-----------------------------------------------------------

A terminal with direct multimedia feature must support the following
defined xterm sequences:

| Sequence       | Description                                         |
|----------------|-----------------------------------------------------|
| CSI 16 t       | Responds with CSI 6 ; cellHeight ; cellWidth t      |
| CSI 18 t       | Responds with CSI 8 ; rows ; columns t              |



Direct Multimedia - New Sequences
---------------------------------

A terminal with direct multimedia feature must support the following
new sequences:

| Sequence                             | Command     | Description             |
|--------------------------------------|-------------|-------------------------|
| OSC 1 3 3 8 ; s i x e l : {data} BEL | SIXEL       | Display sixel at (x, y) |
| OSC 1 3 3 8 ; s i x e l : {data} ST  | SIXEL       | Display sixel at (x, y) |
| OSC 1 3 3 8 ; F i l e = {args} : {data} BEL   | DMDISPLAY   | Display media at (x, y) |
| OSC 1 3 3 8 ; F i l e = {args} : {data} ST    | DMDISPLAY   | Display media at (x, y) |
| CSI ? 3 0 0 0 h               | DECSET 3000 | Enable SCRCHANGE notification  |
| CSI ? 3 0 0 0 l               | DECRST 3000 | Disable SCRCHANGE notification |
| OSC 1 3 3 9 ; Pe ; {args} ST  | DMRESP      | Terminal response to DMDISPLAY |
| CSI ? 3 0 0 1 h               | DECSET 3001 | Enable DMDISPLAY responses     |
| CSI ? 3 0 0 1 l               | DECRST 3001 | Disable DMDISPLAY responses    |



If SCRCHANGE is set/enabled, then the terminal will send the "CSI 6 ;
cellHeight ; cellWidth t" when the font size has changed, and "CSI 8 ;
rows ; columns t" when the number of rows/columns on the screen has
changed.



For the SIXEL command:

* The {data} is a sixel sequence as described in the VT330/340
  Programmer Reference Manual, Chapter 14, available online at:
  http://vt100.net/docs/vt3xx-gp/chapter14.html .  The {data} is the
  "P1 ; P2 ; P3 ; q s..s" portion of the Device Control String, i.e. a
  complete sixel sequence minus the leading DCS and trailing ST.

* The sixel image is processed as shown below.  Note that this
  behavior is equivalent to Sixel Scrolling mode enabled.

  - The sixel active position starts at the upper-left corner of the
    text cursor position.

  - The screen is scrolled up if the image overflows into the bottom
    text row.

  - Pixels that would be drawn to the right of the visible region on
    screen are discarded.

  - The cursor's final position is on the same column as the starting
    cursor position, and on the row immediately below the image.


For the DMDISPLAY command:

* The {args} is a set of key-value pairs (each pair separated by
  semicolon (';')), followed by a colon (':'), followed by a base-64
  encoded string ({data}).

* A key can be any alpha-numeric ASCII string ('0' - '9', 'A' - 'Z',
  'a' - 'z').

* A value is any printable ASCII string not containing whitespace,
  colon, or semicolon ('!' - '9', '<' - '~').

* Any alpha-numeric key may be specified.  A key that is not supported
  by the terminal is ignored without error.

* The multimedia pixels are processed as shown below.

  - The pixel are drawn starting at the upper-left corner of the text
    cursor position.

  - If scroll is specified as 1 (enabled), then:

    a. The screen is scrolled up if the image overflows into the
       bottom text row.

    b. The cursor's final position is on the same column as the
       starting cursor position, and on the row immediately below the
       image.

  - If scroll is omitted or specified as 0 (disabled), then:

    a. The screen is never scrolled.

    b. Pixels that would be drawn below the visible region on screen
       are discarded.

    c. The cursor's final position is at the same column and row as
       the starting cursor position, i.e. the cursor does not move at
       all.

  - Pixels that would be drawn to the right of the visible region on
    screen are discarded.



The keys for the key-value pairs that must be supported by the
terminal are listed below:

| Key          | Default Value | Description                                  |
|--------------|---------------|----------------------------------------------|
| type         | "image/rgb"   | mime-type describing data field              |
| url          | ""            | If set, a location containing the media data |
| width        | 1             | Number of Cells or pixels wide to display in |
| height       | 1             | Number of Cells or pixels high to display in |
| scale        | "none"        | Scale/zoom option, see below                 |
| align        | "nw"          | Align image to edge option, see below        |
| sourceX      | 0             | Media source X position to display           |
| sourceY      | 0             | Media source Y position to display           |
| sourceWidth  | "auto"        | Media width in pixels to display             |
| sourceHeight | "auto"        | Media height in pixels to display            |
| scroll       | 1             | If 1, scroll the display if needed           |

A terminal may support additional keys.  If a key is specified but not
supported by the terminal, then it is ignored without error.



The "type" value is a mime-type string describing the format of the
base64-encoded binary data.  The terminal must support at mimunum these
mime-types:

| Type String   | Description                                                  |
|---------------|--------------------------------------------------------------|
| "image/rgb"   | Big-endian-encoded 24-bit red, green, blue values            |
| "image/rgba"  | Big-endian-encoded 32-bit red, green, blue, alpha values     |
| "image/png"   | PNG file data as described by (reference to PNG format)      |

A terminal may support additional types.  An application can detect
terminal support for a format by: enabling terminal responses (DECSET
3001), sending a DMDISPLAY command, and examining the terminal's
response sequence for success or error.



The "url" value is a RFC-XXXX defined Universal Resource Located,
encoded in RFC-XXXX form as a printable ASCII string not containing:
whitespace, colon (':'), semicolon (';'), or equals ('=').

A terminal is not required to support any URLs.



The "width" and "height" values can take the following forms:

| Value                         | Meaning                   |
|-------------------------------|---------------------------|
| N (a positive integer)        | Number of Cells           |
| Npx (positive integer + "px") | Number of pixels          |
| N% (positive integer + "%")   | Percent of screen width or height |
| "auto"                        | Number of pixels as defined by the multimedia data |



The "scale" value can take the following values:

| Value      | Meaning                                                       |
|------------|---------------------------------------------------------------|
| "none"     | No scaling along either axis.                                 |
| "scale"    | Stretch image, preserving aspect ratio, to maximum size in the target area without cropping |
| "stretch"  | Stretch along both axes, distorting aspect ratio, to fill the target area               |
| "crop"     | Stretch along both axes, preserving aspect ration, to completely fill the target area, cropping pixels that will not fit |



The "align" value can take the following values:

| Value      | Meaning                                                         |
|------------|-----------------------------------------------------------------|
| "nw"       | Media is placed at the top-left corner (northwest)              |
| "n"        | Media is placed on the top and centered horizontally (north)    |
| "ne"       | Media is placed at the top-right corner (northest)              |
| "w"        | Media is placed on the left and centered vertically (west)      |
| "c"        | Media is centered in the target area (center)                   |
| "e"        | Media is placed on the right and centered vertically (east)     |
| "sw"       | Media is placed on the bottom-left corner (southwest)           |
| "s"        | Media is placed on the bottom and centered horizontally (south) |
| "se"       | Media is placed on the bottom-right corner (southeast)          |



"sourceX", "sourceY", "sourceWidth", and "sourceHeight" define the
rectangle of pixels from the media that will be displayed on the
screen.  The ranges for these values is shown below:

| Key          | Minimum Value | Maximum Value                 | Default Value |
|--------------|---------------|-------------------------------|---------------|
| sourceX      | 0             | Media's full width - 1        | 0             |
| sourceY      | 0             | Media's full height - 1       | 0             |
| sourceWidth  | 1             | Media's full width - sourceX  | "auto"        |
| sourceHeight | 1             | Media's full height - sourceY | "auto"        |

If any of these values are specified and outside the range, no image
is displayed, and the cursor does not move.  "sourceWidth" and
"sourceHeight" can be "auto", which means use the maximum available
width/height (given sourceX/sourceY) from the media's inherent
dimensions.



Direct Multimedia - Terminal Responses / Error Handling
-------------------------------------------------------

If DMDISPLAY reponses are enabled, then a terminal will respond to the
DMDISPLAY display with DMRESP.  DMRESP responses must be sent in the
same sequential order as the DMDISPLAY commands they are responses to:
the terminal may not re-order responses.

No provision is made for reliable delivery.  On unreliable links
(example: 3-wire RS232), the DMDISPLAY and DMRESP command/response
sequence may get out of order.



The format of DMRESP is:

* Pe - a non-negative integer error code.

* The {args} is a set of key-value pairs (each pair separated by
  semicolon (';')).

* A key can be any alpha-numeric ASCII string ('0' - '9', 'A' - 'Z',
  'a' - 'z').

* A value is any printable ASCII string not containing whitespace,
  colon, or semicolon ('!' - '9', '<' - '~').



The Pe error codes are defined as:

| Value | Meaning                            | {args} containts         |
|-------|------------------------------------|--------------------------|
| 0     | No error occurred, i.e. success    | nothing                  |
| 1     | Unsupported "type"          | "type" value that was incorrect |
| 2     | Invalid value - no media displayed | "key" that was incorrect |
| 3     | Unsupported key - media displayed  | "key" that unsupported   |
| 4     | Insufficient memory                | nothing                  |
| 5     | Other error - no media displayed   | nothing                  |
| 6     | Other - media displayed            | nothing                  |
| 7     | Conflicting keys - no media displayed | nothing               |
| 8     | RESERVED FOR FUTURE USE            | RESERVED FOR FUTURE USE  |

Additional Pe error codes may be returned; any Pe value except 0, 3,
and 6 must mean that the media was not displayed, and the cursor was
not moved.

If both "type" and "url" are set, no media is diaplyed, the cursor is
not moved, and the DMRESP error code is 7.



Direct Multimedia - Examples
----------------------------



Cached Multimedia - Summary
---------------------------

Non-text data (multimedia) can be sent to the terminal for later
display in a rectangular (single-layer) region of text Cells.
Multimedia data is transmitted to the terminal using the CMCACHE
command described below, and displayed on screen using the CMDISPLAY
command.  A single CMCACHE command can support many CMDISPLAY
commands.

Upon display, setting a Cell to multimedia is a destructive operation:
the Cell's original text is lost.  Multimedia pixels will not overlap
rendered text in the same Cell.  To achieve pixels overlaid on text,
the layers feature can be used.

Setting any part of a multi-Cell Tile to multimedia also "breaks up"
the Tile into a range of single Cells.  In other words, multimedia can
only be carried by a Cell, not a Tile.

The pixels of a multimedia Cell are assigned to the Cell's foreground;
multimedia Cells have no background.  If a terminal supports the
layers feature, setting a multimedia Cell's foreground transparency to
true/enabled causes that Cell to not be displayed at all; setting its
background transparency to either true/enabled or false/disabled has
no visible effect.

The pixels of multimedia Cells can come from two sources:

  1. The application can generate pixels and send them to the terminal
     for display at the current cursor position.

  2. The application can specify a source for the multimedia and the
     terminal will generate the pixels for display at the current
     cursor position.




Cached Multimedia - Cache/Memory Management
-------------------------------------------

The terminal manages a cache of multimedia data on behalf of one or
more applications.  Applications request media be stored in the cache,
and if successful the terminal provides an identification number that
applications must use to request display from the cache to the screen.

The amount of memory and retention/eviction strategy for the cache is
wholly managed by the terminal, with the following restrictions:

* The terminal may not remove items from the cache that have any
  portion being actively displayed on the primary or alternate
  screens.

* The terminal must respond to every CMCACHE command with a new unique
  ID.

The scrollback buffer is permitted, and recommended, to contain only a
few (or zero) multimedia images.  Terminals should consider retaining
only the last 2-5 screens' worth of pixel data in the scrollback
buffer.



Cached Multimedia - Required Support For Existing Sequences
-----------------------------------------------------------

A terminal with cached multimedia feature must support the following
defined xterm sequences:

| Sequence       | Description                                         |
|----------------|-----------------------------------------------------|
| CSI 16 t       | Responds with CSI 6 ; cellHeight ; cellWidth t      |
| CSI 18 t       | Responds with CSI 8 ; rows ; columns t              |



Cached Multimedia - New Sequences
---------------------------------

A terminal with cached multimedia feature must support the following new
sequences:

| Sequence                             | Command   | Description             |
|--------------------------------------|-----------|-------------------------|
| CSI ? 3 0 0 0 h             | DECSET 3000 | Enable SCRCHANGE notification  |
| CSI ? 3 0 0 0 l             | DECRST 3000 | Disable SCRCHANGE notification |
| OSC 1 3 4 0 ; F i l e = {args} : {data} BEL   | CMCACHE   | Display media at (x, y) |
| OSC 1 3 4 1 ; Pi ; {args} ST         | CMDISPLAY | Display media at (x, y) |
| OSC 1 3 4 2 ; Pi ; Pe ; {args} ST | CMCRESP | Terminal response to CMCACHE   |
| OSC 1 3 4 3 ; Pi ; Pe ; {args} ST | CMDRESP | Terminal response to CMDISPLAY |



If SCRCHANGE is set/enabled, then the terminal will send the "CSI 6 ;
cellHeight ; cellWidth t" when the font size has changed, and "CSI 8 ;
rows ; columns t" when the number of rows/columns on the screen
changes.



Cached Multimedia - CMCACHE
---------------------------

For the CMCACHE command:

* The {args} is a set of key-value pairs (each pair separated by
  semicolon (';')), followed by a colon (':'), followed by a base-64
  encoded string ({data}).

* A key can be any alpha-numeric ASCII string ('0' - '9', 'A' - 'Z',
  'a' - 'z').

* A value is any printable ASCII string not containing whitespace,
  colon, or semicolon ('!' - '9', '<' - '~').



The keys for the key-value pairs that must be supported by the
terminal are listed below:

| Key          | Default Value | Description                                  |
|--------------|---------------|----------------------------------------------|
| type         | "image/rgb"   | mime-type describing data field              |
| url          | ""            | If set, a location containing the media data |



The "type" value is a mime-type string describing the format of the
base64-encoded binary data.  The terminal must support at mimunum these
mime-types:

| Type String   | Description                                                  |
|---------------|--------------------------------------------------------------|
| "image/rgb"   | Big-endian-encoded 24-bit red, green, blue values            |
| "image/rgba"  | Big-endian-encoded 32-bit red, green, blue, alpha values     |
| "image/png"   | PNG file data as described by (reference to PNG format)      |

A terminal may support additional types.  An application can detect
terminal support for a format by: sending a CMCACHE command, and
examining the terminal's CMCRESP sequence for success or error.



The "url" value is a RFC-XXXX defined Universal Resource Located,
encoded in RFC-XXXX form as a printable ASCII string not containing:
whitespace, colon (':'), semicolon (';'), or equals ('=').

A terminal is not required to support any URLs.



Cached Multimedia - CMDISPLAY
-----------------------------

For the CMDISPLAY command:

* Pi - a non-negative integer media ID that was returned by a CMCRESP
  response to a previous CMCACHE command.

* The {args} is a set of key-value pairs (each pair separated by
  semicolon (';')), followed by a colon (':'), followed by a base-64
  encoded string.

* A key can be any alpha-numeric ASCII string ('0' - '9', 'A' - 'Z',
  'a' - 'z').

* A value is any printable ASCII string not containing whitespace,
  colon, or semicolon ('!' - '9', '<' - '~').

* Any alpha-numeric key may be specified.  A key that is not supported
  by the terminal is ignored without error.

* The multimedia pixels are processed as shown below.

  - The pixel are drawn starting at the upper-left corner of the text
    cursor position.

  - If scroll is specified as 1 (enabled), then:

    a. The screen is scrolled up if the image overflows into the
       bottom text row.

    b. The cursor's final position is on the same column as the
       starting cursor position, and on the row immediately below the
       image.

  - If scroll is omitted or specified as 0 (disabled), then:

    a. The screen is never scrolled.

    b. Pixels that would be drawn below the visible region on screen
       are discarded.

    c. The cursor's final position is at the same column and row as
       the starting cursor position, i.e. the cursor does not move at
       all.

  - Pixels that would be drawn to the right of the visible region on
    screen are discarded.



The keys for the key-value pairs that must be supported by the
terminal are listed below:

| Key          | Default Value | Description                                  |
|--------------|---------------|----------------------------------------------|
| width        | 1             | Number of Cells or pixels wide to display in |
| height       | 1             | Number of Cells or pixels high to display in |
| scale        | "none"        | Scale/zoom option, see below                 |
| align        | "nw"          | Align image to edge option, see below        |
| sourceX      | 0             | Media source X position to display           |
| sourceY      | 0             | Media source Y position to display           |
| sourceWidth  | "auto"        | Media width in pixels to display             |
| sourceHeight | "auto"        | Media height in pixels to display            |
| scroll       | 1             | If 1, scroll the display if needed           |

A terminal may support additional keys.  If a key is specified but not
supported by the terminal, then it is ignored without error.



The "width" and "height" values can take the following forms:

| Value                         | Meaning                   |
|-------------------------------|---------------------------|
| N (a positive integer)        | Number of Cells           |
| Npx (positive integer + "px") | Number of pixels          |
| N% (positive integer + "%")   | Percent of screen width or height |
| "auto"                        | Number of pixels as defined by the multimedia data |



The "scale" value can take the following values:

| Value      | Meaning                                                       |
|------------|---------------------------------------------------------------|
| "none"     | No scaling along either axis.                                 |
| "scale"    | Stretch image, preserving aspect ratio, to maximum size in the target area without cropping |
| "stretch"  | Stretch along both axes, distorting aspect ratio, to fill the target area               |
| "crop"     | Stretch along both axes, preserving aspect ration, to completely fill the target area, cropping pixels that will not fit |



The "align" value can take the following values:

| Value      | Meaning                                                         |
|------------|-----------------------------------------------------------------|
| "nw"       | Media is placed at the top-left corner (northwest)              |
| "n"        | Media is placed on the top and centered horizontally (north)    |
| "ne"       | Media is placed at the top-right corner (northest)              |
| "w"        | Media is placed on the left and centered vertically (west)      |
| "c"        | Media is centered in the target area (center)                   |
| "e"        | Media is placed on the right and centered vertically (east)     |
| "sw"       | Media is placed on the bottom-left corner (southwest)           |
| "s"        | Media is placed on the bottom and centered horizontally (south) |
| "se"       | Media is placed on the bottom-right corner (southeast)          |



"sourceX", "sourceY", "sourceWidth", and "sourceHeight" define the
rectangle of pixels from the media that will be displayed on the
screen.  The ranges for these values is shown below:

| Key          | Minimum Value | Maximum Value                 | Default Value |
|--------------|---------------|-------------------------------|---------------|
| sourceX      | 0             | Media's full width - 1        | 0             |
| sourceY      | 0             | Media's full height - 1       | 0             |
| sourceWidth  | 1             | Media's full width - sourceX  | "auto"        |
| sourceHeight | 1             | Media's full height - sourceY | "auto"        |

If any of these values are specified and outside the range, no image
is displayed, and the cursor does not move.  "sourceWidth" and
"sourceHeight" can be "auto", which means use the maximum available
width/height (given sourceX/sourceY) from the media's inherent
dimensions.



Cached Multimedia - Error Handling
----------------------------------

A terminal will always respond to the CMCACHE command with CMCRESP,
and to the CMDISPLAY command with CMDRESP.  Responses must be sent in
the same sequential order as the CMCACHE/CMDISPLAY commands they are
responses to: the terminal may not re-order responses.

No provision is made for reliable delivery.  On unreliable links
(example: 3-wire RS232), the command/response sequence may get out of
order.



Cached Multimedia - Error Handling - CMCRESP
--------------------------------------------

The format of CMCRESP is:

* Pi - a non-negative integer media ID.  The terminal will generate a
  new ID for every image successfully loaded into the cache.  The
  application must use this ID for CMDISPLAY commands.

* Pe - a non-negative integer error code.

* The {args} is a set of key-value pairs (each pair separated by
  semicolon (';')).

* A key can be any alpha-numeric ASCII string ('0' - '9', 'A' - 'Z',
  'a' - 'z').

* A value is any printable ASCII string not containing whitespace,
  colon, or semicolon ('!' - '9', '<' - '~').



The Pe error codes are defined as:

| Value | Meaning                                | {args} containts         |
|-------|----------------------------------------|--------------------------|
| 0     | No error occurred, i.e. success        | nothing                  |
| 1     | Unsupported "type"              | "type" value that was incorrect |
| 2     | Invalid value - no media stored        | "key" that was incorrect |
| 3     | Unsupported key - media stored         | "key" that unsupported   |
| 4     | Insufficient memory - no media stored  | nothing                  |
| 5     | Other error - no media stored          | nothing                  |
| 6     | Other - media stored                   | nothing                  |
| 7     | Conflicting keys - no media stored     | nothing                  |
| 8     | RESERVED FOR FUTURE USE                | RESERVED FOR FUTURE USE  |

Additional Pe error codes may be returned; any Pe value except 0, 3,
and 6 must mean that the media was not stored in the cache.

If both "type" and "url" are set, no media is diaplyed, the cursor is
not moved, and the CMCRESP error code is 7.



Cached Multimedia - Error Handling - CMDRESP
--------------------------------------------

The format of CMDRESP is:

* Pi - a non-negative integer media ID.

* Pe - a non-negative integer error code.

* The {args} is a set of key-value pairs (each pair separated by
  semicolon (';')).

* A key can be any alpha-numeric ASCII string ('0' - '9', 'A' - 'Z',
  'a' - 'z').

* A value is any printable ASCII string not containing whitespace,
  colon, or semicolon ('!' - '9', '<' - '~').



The Pe error codes are defined as:

| Value | Meaning                                | {args} containts         |
|-------|----------------------------------------|--------------------------|
| 0     | No error occurred, i.e. success        | nothing                  |
| 1     | RESERVED FOR FUTURE USE                | RESERVED FOR FUTURE USE  |
| 2     | Invalid value - no media displayed     | "key" that was incorrect |
| 3     | Unsupported key - media displayed      | "key" that unsupported   |
| 4     | Insufficient memory - no media displayed | nothing                |
| 5     | Other error - no media displayed       | nothing                  |
| 6     | Other - media displayed                | nothing                  |
| 7     | RESERVED FOR FUTURE USE                | RESERVED FOR FUTURE USE  |
| 8     | Media was evicted - no media displayed | nothing                  |

Additional Pe error codes may be returned; any Pe value except 0, 3,
and 6 must mean that the media was not displayed.



Cached Multimedia - Examples
----------------------------




Layers - Summary
----------------

Layers introduce the concept of a layer "Z" coordinate to the existing
rows ("Y") by columns ("X") grid.  Put another way, the
two-dimensional grid of columns-by-rows becomes a three-dimensional
cube of columns-by-rows-by-layers.  For this document, the column,
row, and layer coordinates are referred to as X, Y, and Z.  This
cartesian coordinate system is right-handed, with the Z axis pointing
"away" from the user "into" the screen.

An application treats the Z coordinate exactly as it does X and Y
(rows and columns) coordinates:

  * If it attemps to set Z to a value less than 1, then Z is set to 1.

  * If it attempts to set Z to a value greater than the number of
    layers, then Z is set to the number of layers.

New sequences are provided to set and query Z, Y, X; to set and query
the screen cube size; and control visibility of Cells in-front-of
other Cells.

Operations that can act on more than one Cell are defined such to act
on all layers simultaneously by default; most of these operations can
also be set to act only on the current layer.



Layers - Number of Layers
-------------------------

A terminal is required to provide between 1 and a finite number of
layers.

The number of layers may be different between the primary and
alternate screens.

An application may request that the terminal allocate additional
layers.  The terminal is free to honor or ignore such requests as it
sees fit.

The scrollback buffer is permitted, and recommended, to contain only a
"flattened" single layer.



Layers - Terminal State
-----------------------

The terminal maintains a complex state at all times.  This state
includes variables such as cursor position, foreground/background
color, attributes to apply to the next displayed character, and so on.
The layers feature adds more variables to the state, and these
variables are required to be stored with DECSC (ESC 7) and restored
with DECRC (ESC 8).  The new variables are listed below:

| Mnemonic | Description                 | Default value  |
|----------|-----------------------------|----------------|
| Z        | Cursor position Z           | 1              |
| MSL      | Manipulate single layer     | off / disabled |
| TFT      | Text foreground transparent | false          |
| TBT      | Text background transparent | false          |



Layers - Required Support For Existing Sequences
------------------------------------------------

A terminal with layers feature must support the standard VT100/VT102
sequences defined in their respective manuals.



Layers - New Sequences
----------------------

A terminal with layer feature must support the following new
sequences:

| Sequence          | Command     | Description                            |
|-------------------|-------------|----------------------------------------|
| CSI ? z ; y ; x H | CUPZ        | Move cursor to (x, y, z)               |
| CSI 2 2 5 ; 1 ; Pa t | SLA      | Set layer alpha                        |
| CSI ? 3 0 0 2 h   | DECSET 3002 | Enable Manupulate Single Layer (MSL)   |
| CSI ? 3 0 0 2 l   | DECRST 3002 | Disable Manupulate Single Layer (MSL)  |
| CSI ? l ; h ; w t | RSZCUBE     | Resize cube to (layers, height, width) |

Default parameters and ranges are listed below:

| Command | Position / Variable | Default Value | Minumum | Maximum   |
|---------|---------------------|---------------|---------|-----------|
| CUPZ    | 1 / z               | 1             | 1       | # layers  |
| CUPZ    | 2 / y               | 1             | 1       | # rows    |
| CUPZ    | 3 / x               | 1             | 1       | # columns |
| SLA     | 1 / alpha           | 255           | 0       | 255       |
| RSZCUBE | 1 / l               | 1             | 1       | varies    |
| RSZCUBE | 2 / h               | 24            | 1       | varies    |
| RSZCUBE | 3 / w               | 80            | 1       | varies    |

The terminal must also support the following new queries:

| Query           | Response              | Description                    |
|-----------------|-----------------------|--------------------------------|
| CSI ? 1 0 0 n   | CSI ? z ; y ; x n     | Report cursor Z, Y, X position |
| CSI ? 1 8 t     | CSI ? 8 ; l ; h ; w t | Report the text area cube layers, height, width |

The terminal must support the following new Set Graphics Rendition
(SGR) character attributes commands:

| SGR Parameter | Description                                 |
|---------------|---------------------------------------------|
| 2 3 0         | Set text foreground color to transparent    |
| 2 3 9         | Set text foreground color to solid (opaque) |
| 2 4 0         | Set text background color to transparent    |
| 2 4 9         | Set text background color to solid (opaque) |



Layers - Error Handling
-----------------------

No additional error reporting is provided for layer feature.



Layers - Rendering
------------------

A terminal with layer feature will display its Cells such that the
screen will appear as if it was rendered in the manner of the
pseudo-code below:

```
for each layer Z, in descending order from maxZ to minZ:

  for each row Y, in ascending order from minY to maxY:

    for each column X, in ascending order from minX to maxX:

      if tile at (X, Y, Z) background color is solid:
        draw rectangle of background color with layer alpha

      if tile at (X, Y, Z) foreground color is solid:
        if tile at (X, Y, Z) is glyph:
          draw glyph with foreground color with layer alpha
        else
          draw pixel data of tile as red/green/blue/alpha pixels with
             layer alpha

      advance X by tile width
    next column

    advance Y by 1
  next row

  decrease Z by 1
next layer
```

A terminal is free to optimize its rendering as it sees fit, so long
as the final screen output looks equivalent to the above method.



Layers - Integration With Existing Sequences
--------------------------------------------

Sequences that insert characters/lines, delete characters/lines, or
modify larger regions are changed to act upon multiple layers as
defined below.  By default, MSL (Manipulate Single Layer) is
off/unset, and Z is 1, so if the application never changes MSL or Z
then these sequences will produce the same visible output as a
terminal without layer support.

A terminal is not required to support all of these sequences; however,
for those sequences it does support, if it supports the layers feature
then the sequences must behave as shown below:

| Sequence   | Command     | Additional behavior                      |
|------------|-------------|------------------------------------------|
| BS  (0x08) | Backspace   | Only current layer affected if MSL=on    |
| DEL (0x7F) | Delete      | Only current layer affected if MSL=on    |
| IND (0x84) | Index       | Only current layer affected if MSL=on    |
| RI  (0x8D  | Reverse Index | Only current layer affected if MSL=on  |
| ESC # 3    | DECDHL      | Cells on all layers always affected      |
| ESC # 4    | DECDHL      | Cells on all layers always affected      |
| ESC # 5    | DECSWL      | Cells on all layers always affected      |
| ESC # 6    | DECDWL      | Cells on all layers always affected      |
| ESC # 8    | DECALN      | All layers > 1 cleared; Z, MSL, TFT, TBT reset to default |
| ESC 7      | DECSC       | Also store Z, MSL, TFT, TBT              |
| ESC 8      | DECRC       | Also restore Z, MSL, TFT, TBT            |
| ESC c      | RIS         | All layers > 1 cleared; Z, MSL, TFT, TBT reset to default |
| CSI @      | ICH         | Only current layer affected if MSL=on    |
| CSI J      | ED          | Only current layer affected if MSL=on    |
| CSI K      | EL          | Only current layer affected if MSL=on    |
| CSI ? K    | DECSEL      | Only current layer affected if MSL=on    |
| CSI L      | IL          | Only current layer affected if MSL=on    |
| CSI M      | DL          | Only current layer affected if MSL=on    |
| CSI X      | ECH         | Only current layer affected if MSL=on    |
| CSI M      | DL          | Only current layer affected if MSL=on    |
| CSI P      | DCH         | Only current layer affected if MSL=on    |
| CSI R      | DECSTBM     | Cells on all layers always affected      |
| CSI $ t    | DECARA      | Only current layer affected if MSL=on    |
| CSI $ v    | DECCRA      | Only current layer affected if MSL=on    |
| CSI x      | DECSACE     | Cells on all layers always affected      |
| CSI $ x    | DECFRA      | Only current layer affected if MSL=on    |
| CSI $ z    | DECERA      | Only current layer affected if MSL=on    |

(( TODO: add many more to the above table... ))

The VT52 sub-mode commands:

| Sequence   | Command     | Additional behavior                      |
|------------|-------------|------------------------------------------|
| ESC J      | ED          | Only current layer affected if MSL=on    |
| ESC K      | EL          | Only current layer affected if MSL=on    |



Layers - Use With Multiplexers
------------------------------

Layers are inteded to provide a means for multiplexers to pass on the
job of multimedia support to the "outer" or host terminal.  The
proposed mechanics of that is outlined in the pseudo-code below:

```
for each inner terminal in descending order from maxZ to minZ:

  emit CUPZ(inner terminal Z, inner terminal Y, inner terminal X)

  draw inner terminal text with standard VT100/VT102/xterm sequences

  for each multimedia sequence emitted by the inner terminal:
    emit CUP(inner terminal Y, inner terminal X)
    emit multimedia sequences to outer terminal
  next multimedia sequence

  decrease Z by 1
next inner terminal
```

The method above may not be effective for complex multi-terminal
screen layouts, but is hoped to work well for many simple cases.



Layers - Examples
-----------------




References
----------

* xterm control sequences:


* ECMA-48:
