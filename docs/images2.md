Terminal Emulator Images Standard - Proposed Design - Simplified
================================================================

Version: 1



Purpose
-------

See the [original proposal](images.md) for purpose, design goals, and
definitions.

This document is an updated proposal to address feedback on the first
proposal, which included: "overengineered", "hopelessly
overengineered", and "unnecessarily complex."

I perceive this feedback as a positive: it is far easier to imagine a
feature and remove it, than to fail to picture it and need it later.
The original proposal was a superset of every image format referenced,
and generalized beyond to multimedia.  This proposal is sharply
reduced from that to: "put this pixel rectangle from the image, into
that cell-based rectangle with specific scaling policy".  It is mostly
a subset of the iTerm2 protocol, with specifications for what happens
to the cursor, and more precise definitions of the
"preserveAspectRatio" equivalent options.



Tradeoffs
---------

Simplifying the original proposal will significantly reduce
complexity, but also eliminates features.  The major tradeoffs offered
in this revised proposal are:

1. Elimination of the layers feature, and with it the ability to place
   images behind text.  In this proposal, a Cell on the screen will
   show either a (part of a) visible image, or a (part of a) text
   glyph, but never both.

2. Elimination of the "url" option, and with it the ability for an
   application to specify a filename or other method for the terminal
   to find the file data on the local machine.  Image data must always
   be passed inline with the sequences.

3. Elimination of response codes, and with it:

   - The ability for multiplexers to blindly pass on the sequences to
     their host terminal.

   - The ability for applications to reliably detect success or
     failure of image display operations.

4. Elimination of pixel-oriented image placement operations, and with
   it the ability of applications to pass on image calculations to the
   terminal.  An application which requires pixel-perfect rendering
   must generate the pixels it needs, aligned such to be displayed at
   the top-left corner of the text Cell rectangle.



Summary
-------

This revised document proposes two independent new features:

1. A method to transfer image data for immediate display within the
   screen Cell grid ("Direct Images").

2. A method to transfer image data to a terminal-managed cache, and
   later display that data within the screen Cell grid ("Cached
   Images").

The only difference between the first and second feature is the
presence of an ID key.  Direct images do not use an ID key, while
cached images use a store operation with ID key followed by one or
more display operations with ID key.

Images are applied to text Cells, and once set handled the same way
text Cells are handled: erasing a line erases the image Cells on that
line, inserting a character will shift image Cells on that row over,
scrolling will shift the image up, and so on.  Therefore, terminals
will need to be prepared for the scenario that every Cell on the
display is a separate image, with a separate display scaling option
that will need to be re-applied automatically if font metrics change.



All Features - Detection
------------------------

Applications can detect support for these features using Primary
Device Attributes (DA) and DECID (ESC Z, or 0x9A).

Terminals that support this standard will repond with additional
parameter(s): "224" for direct images and "225" for cached images.  A
recap of the parameters xterm supports is listed below, with these new
feature responses included:

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
| 2 2 4                       | Direct Images Version 1                    |
| 2 2 5                       | Cached Images Version 1                    |



Direct Images - Summary
-----------------------

Non-text data (images) can be sent to the terminal for immediate
display in a rectangular region of text Cells.  Image data is
transmitted to the terminal using a wire format described later in
this document.

Setting a Cell to image is a destructive operation: the Cell's
original text is lost.  Similarly, setting a Cell (or multiple Cells
for fullwidth glyphs or grapheme clusters) to text is a destructive
operation: the image in the Cell(s) is lost.

Setting any part of a multi-Cell Tile to image also "breaks up" the
Tile into a range of single Cells.  In other words, image data can
only be carried by a Cell, not a Tile.



Direct Images - New Sequences
-----------------------------

A terminal with direct images feature must support the following new
sequences:

| Sequence                             | Description             |
|--------------------------------------|-------------------------|
| OSC 1 3 3 8 ; F i l e = {args} : {data} BEL | Display image at (x, y) |
| OSC 1 3 3 8 ; F i l e = {args} : {data} ST  | Display image at (x, y) |



For the OSC 1 3 3 8 sequence:

* The {args} is a set of key-value pairs (each pair separated by
  semicolon (';')), followed by a colon (':'), followed by a base-64
  encoded string ({data}).

* A key can be any alpha-numeric ASCII string ('0' - '9', 'A' - 'Z',
  'a' - 'z').

* A value is any printable ASCII string not containing whitespace,
  colon, or semicolon ('!' - '9', '<' - '~').

* Any alpha-numeric key may be specified.  A key that is not supported
  by the terminal is ignored without error.

* The image is processed as shown below:

  - The pixels are drawn starting at the upper-left corner of the text
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
| width        | 1             | Number of Cells or pixels wide to display in |
| height       | 1             | Number of Cells or pixels high to display in |
| scale        | "none"        | Scale/zoom option, see below                 |
| sourceX      | 0             | Media source X position to display           |
| sourceY      | 0             | Media source Y position to display           |
| sourceWidth  | "auto"        | Media width in pixels to display             |
| sourceHeight | "auto"        | Media height in pixels to display            |
| scroll       | 0             | If 0, scroll the display if needed           |

A terminal may support additional keys.  If a key is specified but not
supported by the terminal, then it is ignored without error.



The "type" value is a mime-type string describing the format of the
base64-encoded binary data.  The terminal must support at minimum these
mime-types:

| Type String   | Description                                                  |
|---------------|--------------------------------------------------------------|
| "image/rgb"   | Big-endian-encoded 24-bit red, green, blue values            |
| "image/rgba"  | Big-endian-encoded 32-bit red, green, blue, alpha values     |
| "image/png"   | PNG file data as described by (reference to PNG format)      |

A terminal may support additional types.  An application can detect
terminal support for a format by:

  1. Attempt to draw image, with "scroll" set to 1.

  2. Check cursor position DSR 6.

  3. If cursor has moved, then the terminal supports this image type.



The "width" and "height" values are positive integers describing the
number of Cells the image will be placed in.



The "scale" value can take the following values:

| Value      | Meaning                                                       |
|------------|---------------------------------------------------------------|
| "none"     | No scaling along either axis.                                 |
| "scale"    | Stretch image, preserving aspect ratio, to maximum size in the target area without cropping |
| "stretch"  | Stretch along both axes, distorting aspect ratio, to fill the target area               |
| "crop"     | Stretch along both axes, preserving aspect ration, to completely fill the target area, cropping pixels that will not fit |



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



Cached Images - Summary
-----------------------

Non-text data (image) can be sent to the terminal for later display in
a rectangular region of text Cells.  Image data is transmitted to the
terminal using the CSTORE command described below, and displayed on
screen using the CDISPLAY command.  A single CSTORE command can
support many CDISPLAY commands.

Upon display, setting a Cell to image is a destructive operation: the
Cell's original text is lost.  Similarly, setting a Cell (or multiple
Cells for fullwidth glyphs or grapheme clusters) to text is a
destructive operation: the image in the Cell(s) is lost.

Setting any part of a multi-Cell Tile to image also "breaks up" the
Tile into a range of single Cells.  In other words, image data can
only be carried by a Cell, not a Tile.



Cached Images - Cache/Memory Management
---------------------------------------

The terminal manages a cache of multimedia data on behalf of the
application.  The application requests media be stored in the cache
and provides an ID.  This ID is later used to request display on the
screen.

The amount of memory and retention/eviction strategy for the cache is
wholly managed by the terminal, with the following restrictions:

* The terminal may not remove items from the cache that have any
  portion being actively displayed on the primary or alternate
  screens.

The scrollback buffer is permitted, and recommended, to contain only a
few (or zero) multimedia images.  Terminals should consider retaining
only the last 2-5 screens' worth of pixel data in the scrollback
buffer.

Applications have no control over when images are removed from the
cache, and no provision is made to generate/ensure unique IDs.

A terminal multiplexer that passes all CSTORE/CDISPLAY commands to the
host terminal will need to parse the CSTORE and CDISPLAY sequences for
the "id" field and rewrite it to be unique for all of its inner
terminals.



Cached Images - New Sequences
-----------------------------

A terminal with cached images feature must support the following new
sequences:

| Sequence                             | Command   | Description             |
|--------------------------------------|-----------|-------------------------|
| OSC 1 3 4 0 ; F i l e = {args} : {data} BEL | CSTORE | Display media at (x, y) |
| OSC 1 3 4 1 ; Pi ; {args} ST         | CDISPLAY  | Display media at (x, y) |



Cached Images - CSTORE
----------------------

For the CSTORE command:

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
| id           | 0             | ID to refer to the image                     |
| type         | "image/rgb"   | mime-type describing data field              |



The "id" value is a non-negative integer between 0 and 999999.



The "type" value is a mime-type string describing the format of the
base64-encoded binary data.  The terminal must support at mimunum these
mime-types:

| Type String   | Description                                                  |
|---------------|--------------------------------------------------------------|
| "image/rgb"   | Big-endian-encoded 24-bit red, green, blue values            |
| "image/rgba"  | Big-endian-encoded 32-bit red, green, blue, alpha values     |
| "image/png"   | PNG file data as described by (reference to PNG format)      |

A terminal may support additional types.  An application can detect
terminal support for a format by:

  1. Store image in cache.

  2. Attempt to draw image, with "scroll" set to 1.

  3. Check cursor position DSR 6.

  4. If cursor has moved, then the terminal supports this image type.



Cached Images - CDISPLAY
------------------------

For the CDISPLAY command:

* Pi - a non-negative integer ID that was used in a previous CSTORE
  command.

* The {args} is a set of key-value pairs (each pair separated by
  semicolon (';')), followed by a colon (':'), followed by a base-64
  encoded string.

* A key can be any alpha-numeric ASCII string ('0' - '9', 'A' - 'Z',
  'a' - 'z').

* A value is any printable ASCII string not containing whitespace,
  colon, or semicolon ('!' - '9', '<' - '~').

* Any alpha-numeric key may be specified.  A key that is not supported
  by the terminal is ignored without error.

* The image pixels are processed as shown below.

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
| id           | 0             | ID to refer to the image                     |
| width        | 1             | Number of Cells or pixels wide to display in |
| height       | 1             | Number of Cells or pixels high to display in |
| scale        | "none"        | Scale/zoom option, see below                 |
| sourceX      | 0             | Media source X position to display           |
| sourceY      | 0             | Media source Y position to display           |
| sourceWidth  | "auto"        | Media width in pixels to display             |
| sourceHeight | "auto"        | Media height in pixels to display            |
| scroll       | 0             | If 1, scroll the display if needed           |

A terminal may support additional keys.  If a key is specified but not
supported by the terminal, then it is ignored without error.



The "width" and "height" values are positive integers describing the
number of Cells the image will be placed in.



The "scale" value can take the following values:

| Value      | Meaning                                                       |
|------------|---------------------------------------------------------------|
| "none"     | No scaling along either axis.                                 |
| "scale"    | Stretch image, preserving aspect ratio, to maximum size in the target area without cropping |
| "stretch"  | Stretch along both axes, distorting aspect ratio, to fill the target area               |
| "crop"     | Stretch along both axes, preserving aspect ration, to completely fill the target area, cropping pixels that will not fit |



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
