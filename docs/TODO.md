Jexer TODO List
===============


Roadmap
-------

BUG: TTreeView.reflow() doesn't keep the vertical dot within the
     scrollbar.

0.0.6

- TEditor
  - True tokenization and syntax highlighting: Java, C, Clojure, XML
  - Tab character support
  - Cut and Paste

- Finish up multiscreen support:
  - cmAbort to cmScreenDisconnected
  - cmScreenConnected
  - Handle screen resizes

- TSpinner
- TComboBox
- TCalendar

0.0.7

- THelpWindow
  - TText + clickable links
  - Index

0.0.8

- Undo / Redo support

0.1.0: BETA RELEASE and BUG HUNT

- Verify vttest in multiple tterminals.

1.0.0

- Maven artifact.


1.1.0 Wishlist
--------------

- TTerminal
  - Handle resize events (pass to child process)

- Screen
  - Allow complex characters in putCharXY() and detect them in putStringXY().

- Drag and drop
  - TEditor
  - TField
  - TText
  - TTerminal
  - TComboBox



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
