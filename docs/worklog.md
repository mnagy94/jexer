Jexer Work Log
==============

December 15, 2017

We now have 24-bit RGB colors working with Swing backend.
EMCA48Terminal isn't happy though, let's try to fix that...  still no
dice.  So RGB is there for ECMA48 backend, but it sometimes flickers
or disappears.  I'm not sure yet where the fault lies.  Ah, found it!
Cell.isBlank() wasn't checking RGB.

Well, I do say it is rather pretty now.  Let's get this committed and
uploaded.

December 14, 2017

TComboBox is stubbed in, and it was quite simple: just a TField and
TList, and a teeny bit of glue.  Along the way I renamed TCheckbox to
TCheckBox, which was almost more work than TComboBox.  Heh.  Things
are starting to come together indeed.

TSpinner is in.  Now working on TCalendar...  ...and TCalendar is in!

December 13, 2017

A user noticed that the example code given in the README.md caused the
main window to freeze when clicking close.  Turns out that was due to
the addWindow(new TWindow(...)) line, which led to TWindow appearing
in TApplication's window list twice.  Fixed the README, and then made
TApplication.addWindow a package private function plus a check to
ensure it isn't added twice.

On the home front, my main box is now a Fedora 26 running Plasma
desktop.  That ate a few weekends getting used to.  Current-era Linux
is pretty nice, systemd so far (cross fingers) isn't creating any real
problems, audio and wifi worked out of the box (thanks to Intel
chipsets), and I can finally have all of my books and references on
the same box as dev.  So woohoo!

SwingTerminal is getting the insets wrong, which is a bit aggravating.
So let's add adjustable insets in SwingComponent with a default
2-pixel border around the whole thing, which I can tweak for my
laptop.  Done!

Alright, so where are we?  Well, I will have some time in the evenings
over the next couple weeks to put into projects.  This one will get a
little bit of love, probably a new widget or two; Qodem might get
libssh2 + mdebtls support in Windows if those aren't too involved;
Jermit will get a little more push towards a Kermit implementation.

October 17, 2017

I finally gave up the ghost on using gcj as the default compiler due
to its awesome unused imports messages, and learned how to get PMD to
do that job.  Which promptly created 1000+ warning messages related to
class item order (variables, constructors, methods), nested ifs,
useless checks, and so on.  So now we go on a code sweep to fix those,
and along the way set a new class template.  Since this is so large
and invasive, I will bite the bullet now and get it done before the
next release which will get it out on Maven finally.

August 16, 2017

Holy balls this has gotten so much faster!  It is FINALLY visibly
identical in speed to the original d-tui: on xterm it is glass
smooth.  CPU load is about +/- 10%, idling around 5%.

I had to dramatically rework the event processing order, but now it
makes much more sense.  TApplication.run()'s sole job is to listen for
backend I/O, push it into drainEventQueue, and wake up the consumer
thread.  The consumer thread's run() has the job of dealing with the
event, AND THEN calling doIdles and updating the screen.  That was the
big breakthrough: why bother having main thread do screen updates?  It
just leads to contention everywhere as it tries to tell the consumer
thread to lay off its data structures, when in reality the consumer
thread should have been the real owner of those structures in the
first place!  This was mainly an artifact of the d-tui fiber threading
design.

So now we have nice flow of events:

* I/O enters the backend, backend wakes up main thread.

* Main thread grabs events, wakes up consumer thread.

* Consumer thread does work, updates screen.

* Anyone can call doRepaint() to get a screen update shortly
  thereafter.

* Same flow for TTerminalWindow: ECMA48 gets remote I/O, calls back
  into TTerminalWindow, which then calls doRepaint().  So in this case
  we have a completely external thread asking for a screen update, and
  it is working.

Along the way I also eliminated the Screen.dirty flag and cut out
calls to CellAttribute checks.  Overall we now have about 80% less CPU
being burned and way less latency.  Both HPROF samples and times puts
my code at roughly 5% of the total, all the rest is the
sleeping/locking infrastructure.

August 15, 2017

I cut 0.0.5 just now, and also applied for a Sonatype repository.
It was a reasonable spot: TEditor was working albeit buggy, and a bug
had just come in on the main TApplication run loop.  So we are about
to embark upon some performance work again, it's been probably version
0.0.2 or so since the last cycle.

Code size: 40446 lines.

Now switching head to 0.0.6 and taking a small break.

August 14, 2017

TEditor is basically done.  Mouse movement, keyboard movement,
backspace / delete / enter / etc. are all in.  Things are starting to
look pretty good.

I'm going to prep for a final cut and release tag tomorrow or the next
evening.  I need to take a break and get some meatspace life dealt
with.

August 12, 2017

TEditor is stubbed in about 50% complete now.  I have a Highlighter
class that provides different colors based on Word text values, but it
is a lot too simple to do true syntax highlighting.  I am noodling on
the right design that would let TEditor be both a programmer's editor
(so Highlighter needs to have state and do a lexical scan) and a word
processor (where Word needs to tokenize on whitespace).  I estimate
probably a good 2-4 weeks left to get the editor behavior where I want
it, and then after that will be the 0.0.5 release.

Finding more minor paper cuts and fixing them: the mouse cursor being
ahead of a window drag event, SwingTerminal resetting blink on new
input, prevent TWindow from resizing down into the status bar.

August 8, 2017

Multiscreen is looking really cool!  Demo6 now brings up three
screens, including one that is inside a TWindow of a different
application.

August 7, 2017

Had trouble sleeping, what with a bunch of imaginative thoughts for
this release.  jexer.backend will be the ultimate destination for
jexer.session and most of jexer.io.  TerminalReader will be the
interface for keyboard and mouse events.  cmScreenConnected and
cmScreenDisconnected will be new events to represent a screen
appearing/disappearing, and MultiBackend will be a new backend
multiplexer that goes full XRandR.  Several new demos demonstrating
multi-screen support will be coming along.

August 6, 2017

Time to clean up more API, particularly between Backend and Screen.
Both of these will be interfaces soon, so that one could easily
subclass JComponent and implement both Screen and Backend.  The
original code evolved out of Qodem, where screen.c and input.c were
two different things leading to ECMA48Screen and ECMA48Terminal, but
now there is really no need to keep them separate.  It also
complicates the constructors, as these are basically friend classes
that have used package private access to get around their artificial
separation.

When I get this done it should be a lot easier to do any of:

* Pass a JFrame or JComponent to SwingBackend and have it add itself,
  like any other Swing widget.

* Construct a SwingBackend and add it to any regular JComponent.

* Have multiple TApplications running inside the same Swing
  application, including having actions affect each other.  (Will also
  need to ensure that TWidgets/TWindows are not in different
  TApplication collections.)

* Build a Backend/Screen multiplexer, so that one could have a ECMA48
  TApplication listening on a port and a local Swing monitor for it.

* Build a Backend/Screen manager, so that one could have multiple
  ECMA48 screens acting as a single large screen (e.g. XRandR).

Now I need to decide which package will collect Backend, SessionInfo,
and Screen.  jexer.io has some java.io stuff, so it stays anyway.

July 28, 2017

Got very busy with my meatspace life, now getting a chance to come
back around.

I gave up on TEditor knowing about graphemes, instead pulling back to
simple Cells.  This will be better anyway in the long run, as getting
grapheme support in Screen someday will also get it for me in TEditor
for free.  But it does mean that TEditor will chew through much more
RAM than it needs to for a text file.  Performance optimization will
come someday.  But this means I can also go back to gcj, because I
really like its warnings about unused imports.

I've got a POM stubbed in, and created an account over at sonatype.
If it isn't too hard, I will try to get 0.0.5 released into the maven
universe.  But that is still a bit away, I need TEditor running with
syntax highlighting first.

July 17, 2017

Focus-follows-mouse is in, as is NOCLOSEBOX.

July 15, 2017

I think I have cleaned up most of the window show/hide/activate mess
in TApplication.  Demo4 has some cool interactions between a
background TDesktop and several foreground TWindows, which helped
expose bugs.

July 9, 2017

While working on TWindow.hide/show I decided that I am sick of
TApplication's active window handling.  TApplication makes lots of
assumptions, things are too fragile between modal and not, and one
cannot easily say window.activate().  So I will also be changing that
too. ... Code is still a bit of a mess, but hooks are in place at
least for show/hide/activate.

July 8, 2017

Qodem 1.0.0 released last month, I had a vacation, and a Jexer user
(nikiroo) started opening up pull requests. :-) So back unto the
breach we go!

TButton is now animated so that there is some feedback when selected
via keyboard.  StringJustifier was written which permits TText's to
have left/centered/right and full justification.  TDesktop is now in
too which can act as a permanent max-sized window without borders.

Next up is Viewport, an interface to collect scrollbar API, and then a
cleaner API for scrollable widgets and windows.  After that is more
window API: hide/show/maximize/restore, and unclosable windows.  I am
cherry-picking bits from @nikiroo's PRs, which will likely break them
before it fixes things, but I will find some way to get Niki credited
with those pieces.

March 21, 2017

I am starting to gear up for making Jexer a serious project now.  I've
created its SourceForge project, linked it back to GitHub, have most
of its web page set up (looks like Qodem's), and released 0.0.4.  And
then this morning saw an out-of-bounds exception if you kill the main
demo window.  Glad I marked it Alpha on SourceForge...

Yesterday I was digging around the other Turbo Vision derived projects
while populating the about page, and made a sad/happy-ish realization:
Embarcadero could probably get all of them shut down if it really
wanted to, including Free Vision.  I uncovered some hidden history in
Free Vision, such that it appears that Graphics Vision had some
licensed Borland code in it, so there might be enough mud in the air
that Free Vision could be shut down the same way RHTVision was.  But
even worse is the SCOTUS ruling on Oracle vs Google: if APIs are
copyrighted (regardless of their thoughts on fair use), then any
software that matches the API of a proprietary project might find
itself subject to an infringement case.  So that too could shut down
the other API-compatible TV clones.

Fortunately, Jexer (and D-TUI) is completely new, and has no API
compatibility with Turbo Vision.  Jexer could be a new root to a whole
generation of TUI applications.

March 18, 2017

TStatusBar is working, as is "smart" window placement.  Overall this
is looking quite nice.  Found a lot of other small paper cut items and
fixed them.  It looks absolutely gorgeous on Mac now.

Tomorrow I will get to the public wifi and get this uploaded.

Time to call this 0.0.4 now though.  We are up to 32,123 lines of
code.

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
