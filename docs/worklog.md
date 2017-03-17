Jexer Work Log
==============

March 17, 2017

Jexer is coming back to active development status.  I had a lot of
other projects ahead of it in the queue, mostly Qodem but also Jermit
and of course lots of actual day job work keeping me too tired for
afterhours stuff.  But here we are now, and I want to get Jexer to its
1.0.0 release before the end of 2018.  After that it will be a
critical bit of function for IWP and NIB, if I ever get those going.
I need to re-organize the demo app a bit so that it fits within 80x25,
and then get to TStatusBar.

A status bar will be an optional part of TWindow.  If it exists, then
it will be drawn last by TApplication and get events routed to it from
TWindow's event handlers.  This will have the nice effect that the
status bar can change depending on which window is active, without any
real extra work on TApplication's part.

Putting together a proper TODO now, with release and regression
checklists.  I think I will see if jexer is available at SourceForge,
and if so grab it.  Perhaps I can put together some good Turbo Vision
resources too.  At the very least direct people to the Borland-derived
C++ releases and Free Vision.

