Jexer TODO List
===============


Roadmap
-------

0.0.6

- TEditor
  - Horizontal scrollbar integration
  - True tokenization and syntax highlighting: Java, C, Clojure, XML
  - Carat notation for control characters
  - Tab character support
  - Cut/copy/paste
  - Performance: behave smoothly on 100MB text files

0.0.7

- Finish up multiscreen support:
  - cmAbort to cmScreenDisconnected
  - cmScreenConnected
  - Handle screen resizes

- TEditor
  - Word wrap
  - Performance: behave smoothly on 1GB text files

- Additional main color themes:
  - Dark / L33t
  - Green / NoReallyElite
  - Red/brown
  - Monochrome
  - OMGPonies

0.0.8

- THelpWindow
  - TEditor + clickable links
  - Index

- TEditor
  - Expose API:
    - Cursor movement
    - Movement within document
    - Cut/copy/paste

0.0.9

- TEditor:
  - Undo / Redo support

0.1.0: LET'S GET PRETTY

- TChart:
  - Bar chart
  - XY chart
  - Time series chart

0.1.1: BETA RELEASE and BUG HUNT

- Verify vttest in multiple tterminals.

0.2.0:

- Drag and drop
  - TEditor
  - TField
  - TText
  - TTerminal
  - TComboBox

1.0.0

- Publish to the whole wide world


1.1.0 Wishlist
--------------

- Screen
  - Allow complex characters in putCharXY() and detect them in putStringXY().



Regression Checklist
--------------------

  TTerminal
    No hang when closing, whether or not script is running
    No dead script children lying around
    vttest passing



Release Checklist √
-------------------

Eliminate all Eclipse warnings

Fix all marked TODOs in code

Eliminate DEBUG, System.err prints

Update written by date to current year:
    All code headers
    VERSION

Tag github

Upload to SF

Upload to sonatype


Brainstorm Wishlist
-------------------



Bugs Noted In Other Programs
----------------------------
